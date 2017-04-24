// @flow
import request from 'superagent';
import randomString from 'randomstring';
import crypto from 'crypto';
import didYouMean from 'didyoumean';
import moment from 'moment';
import rawurlencode from 'locutus/php/url/rawurlencode';
import SetDictionary from './SetDictionary';
import type { ConfigType } from './Config';

export default class MCMAPI {

  config: ConfigType;
  setDictionary: SetDictionary;

  constructor(config: ConfigType, setDictionary: SetDictionary) {
    // Initialize fields
    this.config = config;
    this.setDictionary = setDictionary;
    // Bind function(s)
    this.getPricing = this.getPricing.bind(this);
    this.genAuthHeader = this.genAuthHeader.bind(this);
    this.updateSetDictionary = this.updateSetDictionary.bind(this);
  }

  updateSetDictionary: Function;
  async updateSetDictionary() {
    // Make request safely
    const endpoint = `https://${this.config.mcmApiHost}/ws/v1.1/output.json/expansion/1`;
    const method = 'GET';
    let result;
    try {
      result = await request.get(endpoint).set('Authorization', this.genAuthHeader(method, endpoint));
      if (!result.body) {
        console.error('MCM API Issue: Body not present.');
        throw { errType: 'MISC_ERROR', result };
      }
      result = result.body;
    } catch (e) {
      if (e.errType === 'MISC_ERROR') throw e;
      switch (e.status) {
        case 401: {
          console.error('MCM API Issue: 401 Unauthorized.');
          throw { errType: 'UNAUTHORIZED' };
        }
        case 403: {
          console.error('MCM API Issue: 403 Forbidden.');
          throw { errType: 'FORBIDDEN' };
        }
        case 404: {
          console.error('MCM API Issue: 404 Not Found.');
          throw { errType: 'CLIENT_ERROR' };
        }
        case 500: {
          console.error('MCM API Issue: 500 Internal Server Error.');
          throw { errType: 'SERVER_ERROR' };
        }
        default: {
          console.error(`MCM API Issue: Unknown Status: ${e.status}.`, e);
          throw { errType: 'REQUEST_ERROR' };
        }
      }
    }
    // Verify response
    if (!result.expansion) {
      console.error(`MCM API Issue: No 'expansion' field in result of request to${endpoint}`);
      throw { errType: 'RESPONSE_ERROR' };
    }
    // Process response
    result.expansion.forEach(expansion => {
      // Map expansion name to known set name
      const setName = didYouMean(expansion.name, this.setDictionary.dictionary.map(item => item.setName));
      // If we don't know the expansion, discard it.
      if (!setName) return;
      // Obtain set code (which definitely exists because we just obtained its name)
      const setCode : string = (this.setDictionary.dictionary.find(item => item.setName === setName): any).setCode;
      // Register the MCM name
      this.setDictionary.setMcmName(setCode, expansion.name);
    });
  }

  getPricing: (cardName: string, setCode?: ?string) => Promise<{ SELL: number, LOW: number, LOWEX: number, LOWFOIL: number, AVG: number }>;
  async getPricing(cardName: string, setCode?: ?string) {
    // Obtain MCM name from set code if specified
    const mcmSet = setCode ? this.setDictionary.setCodeToMcmName(setCode) : '';
    // Stop if we don't know the specified set
    if (setCode && !mcmSet) throw { errType: 'SET_NOT_AVAILABLE' };
    // Construct the request
    const endpoint = `https://${this.config.mcmApiHost}/ws/v1.1/output.json/products/${rawurlencode(cardName)}/1/1/true`;
    const method = 'GET';
    // Make request safely
    let result;
    try {
      result = await request.get(endpoint).set('Authorization', this.genAuthHeader(method, endpoint));
      if (!result.body) {
        console.error('MCM API Issue: Body not present.');
        throw { errType: 'MISC_ERROR', result };
      }
      result = result.body;
    } catch (e) {
      if (e.errType === 'MISC_ERROR') throw e;
      switch (e.status) {
        case 401: {
          console.error('MCM API Issue: 401 Unauthorized.');
          throw { errType: 'UNAUTHORIZED' };
        }
        case 403: {
          console.error('MCM API Issue: 403 Forbidden.');
          throw { errType: 'FORBIDDEN' };
        }
        case 404: {
          console.error('MCM API Issue: 404 Not Found.');
          throw { errType: 'CLIENT_ERROR' };
        }
        case 500: {
          console.error('MCM API Issue: 500 Internal Server Error.');
          throw { errType: 'SERVER_ERROR' };
        }
        default: {
          console.error(`MCM API Issue: Unknown Status: ${e.status}.`, e);
          throw { errType: 'REQUEST_ERROR' };
        }
      }
    }
    // Verify response
    if (!result.product) {
      console.error(`MCM API Issue: No 'product' field in result of request to${endpoint}`);
      throw { errType: 'RESPONSE_ERROR' };
    }
    // Filter out all incorrect sets
    if (mcmSet) result.product = result.product.filter(product => product.expansion === mcmSet);
    // Quit if we did not find a matching product
    if (result.product.length === 0) throw { errType: 'NOT_FOUND' };
    // Extract the product
    const product = result.product[0];
    // Return the price guide
    return Object.assign({}, product.priceGuide, { url: `https://www.magiccardmarket.eu${product.website}` });
  }

  genAuthHeader: (httpMethod: string, realm: string) => string;
  genAuthHeader(httpMethod: string, realm: string) {
    // Define main parameters
    const oauthVersion = '1.0';
    const oauthConsumerKey = this.config.mcmToken;
    const oauthToken = '';
    const oauthSignatureMethod = 'HMAC-SHA1';
    const oauthTimestamp = moment().unix().toString();
    const oauthNonce = randomString.generate({ length: 12, charset: 'alphabetic' });

    // Construct & set signature
    let baseString = `${httpMethod.toUpperCase()}&${rawurlencode(realm)}&`;
    const paramString = [
      `oauth_consumer_key=${rawurlencode(oauthConsumerKey)}&`,
      `oauth_nonce=${rawurlencode(oauthNonce)}&`,
      `oauth_signature_method=${rawurlencode(oauthSignatureMethod)}&`,
      `oauth_timestamp=${rawurlencode(oauthTimestamp)}&`,
      `oauth_token=${rawurlencode(oauthToken)}&`,
      `oauth_version=${rawurlencode(oauthVersion)}`
    ].join('');
    baseString += rawurlencode(paramString);
    const signingKey = `${rawurlencode(this.config.mcmSecret)}&`;
    const oauthSignature = crypto.createHmac('sha1', signingKey).update(baseString).digest('base64');

    // Construct and return header authorization value
    return ['OAuth ',
      `realm="${realm}", `,
      `oauth_version="${oauthVersion}", `,
      `oauth_timestamp="${oauthTimestamp}", `,
      `oauth_nonce="${oauthNonce}", `,
      `oauth_consumer_key="${oauthConsumerKey}", `,
      `oauth_token="${oauthToken}", `,
      `oauth_signature_method="${oauthSignatureMethod}", `,
      `oauth_signature="${oauthSignature}"`
    ].join('');
  }
}
