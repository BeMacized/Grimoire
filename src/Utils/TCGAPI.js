// @flow
import request from 'superagent';
import Promise from 'bluebird';
import didYouMean from 'didyoumean';
import * as xml2js from 'xml2js';
import SetDictionary from './SetDictionary';
import type { ConfigType } from './Config';

const parseXML = Promise.promisify(xml2js.parseString);

export default class TCGAPI {

  config: ConfigType;
  setDictionary: SetDictionary;

  constructor(config: ConfigType, setDictionary: SetDictionary) {
    // Initialize fields
    this.config = config;
    this.setDictionary = setDictionary;
    // Bind function(s)
    this.getPricing = this.getPricing.bind(this);
    this.updateSetDictionary = this.updateSetDictionary.bind(this);
  }

  updateSetDictionary: Function;
  async updateSetDictionary() {
    // TODO: Scrape this from somewhere?
    const sets = [
      '10th Edition',
      '7th Edition',
      '8th Edition',
      '9th Edition',
      'Aether Revolt',
      'Alara Reborn',
      'Alliances',
      'Alpha Edition',
      'Amonkhet',
      'Anthologies',
      'Antiquities',
      'APAC Lands',
      'Apocalypse',
      'Arabian Nights',
      'Archenemy',
      'Archenemy: Nicol Bolas',
      'Arena Promos',
      'Astral',
      'Avacyn Restored',
      'Battle for Zendikar',
      'Battle Royale Box Set',
      'Beatdown Box Set',
      'Beta Edition',
      'Betrayers of Kamigawa',
      'Born of the Gods',
      'Box Sets',
      'Champions of Kamigawa',
      'Champs Promos',
      'Chronicles',
      'Classic Sixth Edition',
      'Coldsnap',
      'Coldsnap Theme Deck Reprints',
      'Collector\'s Edition',
      'Commander',
      'Commander 2013',
      'Commander 2014',
      'Commander 2015',
      'Commander 2016',
      'Commander\'s Arsenal',
      'Conflux',
      'Conspiracy',
      'Conspiracy: Take the Crown',
      'Dark Ascension',
      'Darksteel',
      'Deckmasters Garfield vs Finkel',
      'Dissension',
      'Dragon\'s Maze',
      'Dragons of Tarkir',
      'Duel Decks',
      'Duel Decks: Ajani vs. Nicol Bolas',
      'Duel Decks: Anthology',
      'Duel Decks: Blessed vs. Cursed',
      'Duel Decks: Divine vs. Demonic',
      'Duel Decks: Elspeth vs. Kiora',
      'Duel Decks: Elspeth vs. Tezzeret',
      'Duel Decks: Elves vs. Goblins',
      'Duel Decks: Garruk vs. Liliana',
      'Duel Decks: Heroes vs. Monsters',
      'Duel Decks: Izzet vs. Golgari',
      'Duel Decks: Jace vs. Chandra',
      'Duel Decks: Jace vs. Vraska',
      'Duel Decks: Knights vs. Dragons',
      'Duel Decks: Mind vs. Might',
      'Duel Decks: Nissa vs. Ob Nixilis',
      'Duel Decks: Phyrexia vs. the Coalition',
      'Duel Decks: Sorin vs. Tibalt',
      'Duel Decks: Speed vs. Cunning',
      'Duel Decks: Venser vs. Koth',
      'Duel Decks: Zendikar vs. Eldrazi',
      'Duels of the Planeswalkers',
      'Eldritch Moon',
      'Eternal Masters',
      'European Lands',
      'Eventide',
      'Exodus',
      'Fallen Empires',
      'Fate Reforged',
      'Fifth Dawn',
      'Fifth Edition',
      'FNM Promos',
      'Fourth Edition',
      'Fourth Edition (Foreign Black Border)',
      'Fourth Edition (Foreign White Border)',
      'From the Vault: Angels',
      'From the Vault: Annihilation',
      'From the Vault: Dragons',
      'From the Vault: Exiled',
      'From the Vault: Legends',
      'From the Vault: Lore',
      'From the Vault: Realms',
      'From the Vault: Relics',
      'From the Vault: Twenty',
      'Future Sight',
      'Game Day Promos',
      'Gatecrash',
      'Grand Prix Promos',
      'Guildpact',
      'Guru Lands',
      'Hero\'s Path Promos',
      'JSS/MSS Promos',
      'Judge Promos',
      'Judgment',
      'Kaladesh',
      'Khans of Tarkir',
      'Launch Party &amp; Release Event Promos',
      'Legends',
      'Legions',
      'Lorwyn',
      'Magic 2010 (M10)',
      'Magic 2011 (M11)',
      'Magic 2012 (M12)',
      'Magic 2013 (M13)',
      'Magic 2014 (M14)',
      'Magic 2015 (M15)',
      'Magic Modern Event Deck',
      'Magic Origins',
      'Magic Player Rewards',
      'Magic Premiere Shop',
      'Masterpiece Series: Amonkhet Invocations',
      'Masterpiece Series: Kaladesh Inventions',
      'Media Promos',
      'Mercadian Masques',
      'Mirage',
      'Mirrodin',
      'Mirrodin Besieged',
      'Modern Masters',
      'Modern Masters 2015',
      'Modern Masters 2017',
      'Morningtide',
      'Nemesis',
      'New Phyrexia',
      'Oath of the Gatewatch',
      'Odyssey',
      'Onslaught',
      'Oversize Cards',
      'Planar Chaos',
      'Planechase',
      'Planechase 2012',
      'Planechase Anthology',
      'Planeshift',
      'Portal',
      'Portal Second Age',
      'Portal Three Kingdoms',
      'Premium Deck Series: Fire and Lightning',
      'Premium Deck Series: Graveborn',
      'Premium Deck Series: Slivers',
      'Prerelease Cards',
      'Pro Tour Promos',
      'Prophecy',
      'Ravnica',
      'Return to Ravnica',
      'Revised Edition',
      'Revised Edition (Foreign Black Border)',
      'Revised Edition (Foreign White Border)',
      'Rise of the Eldrazi',
      'Saviors of Kamigawa',
      'Scars of Mirrodin',
      'Scourge',
      'Shadowmoor',
      'Shadows over Innistrad',
      'Shards of Alara',
      'Special Occasion',
      'Starter 1999',
      'Starter 2000',
      'Stronghold',
      'Tarkir Dragonfury Promos',
      'Tempest',
      'The Dark',
      'Theros',
      'Time Spiral',
      'Timeshifted',
      'Torment',
      'Ugin\'s Fate Promos',
      'Unglued',
      'Unhinged',
      'Unique and Miscellaneous Promos',
      'Unlimited Edition',
      'Urza\'s Destiny',
      'Urza\'s Legacy',
      'Urza\'s Saga',
      'Vanguard',
      'Visions',
      'Weatherlight',
      'Welcome Deck 2016',
      'WMCQ Promo Cards',
      'Worldwake',
      'WPN &amp; Gateway Promos',
      'Zendikar',
      'Zendikar Expeditions'
    ];
    // Process response
    sets.forEach(set => {
      // Map expansion name to known set name
      const setName = didYouMean(set, this.setDictionary.dictionary.map(item => item.setName));
      // If we don't know the expansion, discard it.
      if (!setName) return;
      // Obtain set code (which definitely exists because we just obtained its name)
      const setCode : string = (this.setDictionary.dictionary.find(item => item.setName === setName): any).setCode;
      // Register the MCM name
      this.setDictionary.setTcgName(setCode, set);
    });
  }

  getPricing: (cardName: string, setCode?: ?string) => Promise<{ SELL: number, LOW: number, LOWEX: number, LOWFOIL: number, AVG: number }>;
  async getPricing(cardName: string, setCode?: ?string) {
    // Obtain TCG name from set code if specified
    const tcgSet = setCode ? this.setDictionary.setCodeToTcgName(setCode) : '';
    // Stop if we don't know the specified set
    if (setCode && !tcgSet) throw { errType: 'SET_NOT_AVAILABLE' };
    // Make request safely
    let result;
    try {
      result = await request.get(this.config.tcgApiHost).query({ pk: this.config.tcgKey, s: tcgSet, p: cardName });
      console.log('XML', result.text);
      if (!result.text) {
        console.error('TCG API Issue: Text not present.');
        throw { errType: 'MISC_ERROR', result };
      }
      result = result.text;
    } catch (e) {
      if (e.errType === 'MISC_ERROR') throw e;
      switch (e.status) {
        case 401: {
          console.error('TCG API Issue: 401 Unauthorized.');
          throw { errType: 'UNAUTHORIZED' };
        }
        case 403: {
          console.error('TCG API Issue: 403 Forbidden.');
          throw { errType: 'FORBIDDEN' };
        }
        case 404: {
          console.error('TCG API Issue: 404 Not Found.');
          throw { errType: 'CLIENT_ERROR' };
        }
        case 500: {
          console.error('TCG API Issue: 500 Internal Server Error.');
          throw { errType: 'SERVER_ERROR' };
        }
        default: {
          console.error(`TCG API Issue: Unknown Status: ${e.status}.`, e);
          throw { errType: 'REQUEST_ERROR' };
        }
      }
    }

    // Check if product exists
    if (result.trim() === 'Product not found.') throw { errType: 'NOT_FOUND' };

    // Parse XML
    let tcgData;
    try {
      tcgData = await parseXML(result);
      console.log('TCGDATA', JSON.stringify(tcgData, null, 2));
    } catch (e) {
      throw { errType: 'RESPONSE_ERROR' };
    }

    // Construct data
    const tcg = {
      url: tcgData.products.product[0].link[0],
      LOW: tcgData.products.product[0].lowprice[0],
      AVG: tcgData.products.product[0].avgprice[0],
      HIGH: tcgData.products.product[0].hiprice[0],
      AVG_FOIL: tcgData.products.product[0].foilavgprice[0]
    };

    return tcg;
  }
}
