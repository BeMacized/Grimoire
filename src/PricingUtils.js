// @flow
import moment from 'moment';
import _MCMAPI from './ApiUtils/MCMAPI';
import _TCGAPI from './ApiUtils/TCGAPI';
import Commons from './Utils/Commons';
import _PricingRecord from './Database/PricingRecord';
import _RateLimiter from './Utils/RateLimiter';

export type PricingRecordType = {
  cardName: string,
  setCode: string,
  pricings: Array<{
    key: string,
    currency: string,
    value: number
  }>,
  storeId: string,
  storeName: string,
  storeUrl: string,
  lastUpdated: number
};

export default class PricingUtils {

  commons: Commons;
  mcmApi: _MCMAPI;
  tcgApi: _TCGAPI;
  PricingRecord: _PricingRecord;
  RateLimiter: Class<_RateLimiter>;
  rateLimiters: Array<{storeId: string, limiter: _RateLimiter}>;

  constructor(MCMAPI: Class<_MCMAPI>, TCGAPI: Class<_TCGAPI>, commons: Commons, PricingRecord: _PricingRecord, RateLimiter: Class<_RateLimiter>) {
    // Initialize fields
    this.mcmApi = new MCMAPI(commons.config, commons.setDictionary);
    this.tcgApi = new TCGAPI(commons.config, commons.setDictionary);
    this.commons = commons;
    this.PricingRecord = PricingRecord;
    this.RateLimiter = RateLimiter;
    this.rateLimiters = [];

    // Update set dictionary when ready
    const updateSetDictionary = () => {
      this.mcmApi.updateSetDictionary().catch(err => {
        if (err) console.log(err.errType, 'Could not initialize sets for MagicCardMarket.eu');
      });
      this.tcgApi.updateSetDictionary().catch(err => {
        if (err) console.log(err.errType, 'Could not initialize sets for TCGPlayer.com');
      });
    };
    if (commons.setDictionary.ready) updateSetDictionary();
    else commons.setDictionary.on('ready', updateSetDictionary);

    // Bind functions
    this.getPricing = this.getPricing.bind(this);
    this.assertStoreRecord = this.assertStoreRecord.bind(this);
  }

  getPricing: (cardName: string, setCode?: ?string) => Promise<Array<PricingRecordType>>;
  async getPricing(cardName: string, setCode?: ?string) {
    // Obtain card reference
    const card = await this.commons.cardUtils.obtainSpecifiedCard(cardName, setCode);
    // Assert MCM & TCG records
    await this.assertStoreRecord(card.name, card.set, 'MCM');
    await this.assertStoreRecord(card.name, card.set, 'TCG');
    // Obtain records
    let records;
    try {
      // Remove outdated records
      await this.PricingRecord.remove({ lastUpdated: { $lte: moment().unix() - this.commons.config.pricingDataTimeout } });
      // Get all relevant pricing records
      records = await this.PricingRecord.find({ cardName: card.name, setCode: card.set }).lean();
    } catch (e) {
      console.error(e);
      throw 'DATABASE_ERROR';
    }
    return records;
  }

  assertStoreRecord: (cardName: string, setCode: string, storeId: string) => Promise<void>;
  async assertStoreRecord(cardName: string, setCode: string, storeId: string) {
    let record;
    try {
      record = await this.PricingRecord.findOne({ cardName, setCode, storeId, lastUpdated: { $gt: moment().unix() - this.commons.config.pricingDataTimeout } });
      // Stop here if a record exists;
      if (record) return;
      // Delete all known relevant records
      await this.PricingRecord.remove({ cardName, setCode, storeId });
    } catch (e) {
      console.error(e);
      throw { errType: 'DATABASE_ERROR' };
    }
    // Get rate limiter for storeId
    const limiter = (() => {
      let entry = this.rateLimiters.find(e => e.storeId === storeId);
      if (!entry) {
        entry = { storeId, limiter: new this.RateLimiter(200, 3600) };
        this.rateLimiters.push(entry);
      }
      return entry.limiter;
    })();

    if (!limiter.checkPermission()) {
      console.error(`Surpassed artificial rate limit for ${storeId}`);
      throw { errType: 'ARTIFICIAL_RATE_LIMIT' };
    }

    console.log('RETRIEVING FROM API', storeId);
    // Get pricing from store API
    let pricing: {
      url: string,
      pricing: Object,
      currency: string,
      storeName: string
    };
    try {
      switch (storeId) {
        case 'MCM': {
          pricing = await this.mcmApi.getPricing(cardName, setCode);
          break;
        }
        case 'TCG': {
          pricing = await this.tcgApi.getPricing(cardName, setCode);
          break;
        }
        default: {
          console.log('UNKNOWN STORE', storeId);
          return; }
      }
    } catch (e) {
      switch (e.errType) {
        case 'RESPONSE_ERROR': { console.log(storeId, 'yielded a RESPONSE_ERROR. I suggest checking it out.'); break; }
        case 'UNAUTHORIZED': { console.log(storeId, 'yielded a UNAUTHORIZED. I suggest checking it out.'); break; }
        case 'FORBIDDEN': { console.log(storeId, 'yielded a FORBIDDEN. I suggest checking it out.'); break; }
        case 'CLIENT_ERROR': { console.log(storeId, 'yielded a CLIENT_ERROR. I suggest checking it out.'); break; }
        case 'SERVER_ERROR': { console.log(storeId, 'yielded a SERVER_ERROR. Best to wait it out.'); break; }
        case 'REQUEST_ERROR': { console.log(storeId, 'yielded a REQUEST_ERROR. I suggest checking it out.'); break; }
        case 'NOT_FOUND': break;
        case 'SET_NOT_AVAILABLE': { console.log(storeId, 'does not currently support', cardName, setCode); break; }
        default: { console.log(storeId, 'yielded an unknown error.', JSON.stringify(e, null, 2)); break; }
      }
      return;
    }
    // Construct pricing data
    const pricingData = {
      cardName,
      setCode,
      pricings: Object.keys(pricing.pricing).map(key => ({ key, currency: pricing.currency, value: pricing.pricing[key] })),
      storeId,
      storeName: pricing.storeName,
      storeUrl: pricing.url,
      lastUpdated: moment().unix()
    };
    // Save pricing data
    try {
      await (new this.PricingRecord(pricingData)).save();
    } catch (e) {
      console.error(e);
      throw { errType: 'DATABASE_ERROR' };
    }
  }

}
