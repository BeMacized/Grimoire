// @flow
import fs from 'fs';
import _ from 'lodash';
import request from 'superagent';
import * as xml2js from 'xml2js';
import Promise from 'bluebird';
import type { MTGSDK } from 'mtgsdk';
import AppState from './AppState';
import SetDictionary from '../ApiUtils/SetDictionary';

const parseXML = Promise.promisify(xml2js.parseString);

export default class CardUtils {

  appState: AppState;
  mtg: MTGSDK;
  tokens: Array<Object>;
  setDictionary: SetDictionary;

  constructor(appState: AppState, mtg: MTGSDK, tokenFile: string, setDictionary: SetDictionary) {
    // Initialize Fields
    this.appState = appState;
    this.mtg = mtg;
    this.setDictionary = setDictionary;
    this.tokens = [];
    const parseTokensAsync = (async () => { this.tokens = await this.parseTokens(tokenFile); });
    if (setDictionary.ready) parseTokensAsync();
    else setDictionary.on('ready', parseTokensAsync);

    // Bind function(s)
    this.obtainSpecifiedCard = this.obtainSpecifiedCard.bind(this);
    this.obtainRecentOrSpecifiedCard = this.obtainRecentOrSpecifiedCard.bind(this);
  }

  parseTokens: (tokenFile: string) => Promise<Array<Object>>;
  async parseTokens(tokenFile: string) {
    // Obtain necessary data from API
    let types: Array<string>;
    let supertypes: Array<string>;
    let subtypes: Array<string>;
    try {
      types = (await request.get('https://api.magicthegathering.io/v1/types')).body.types || [];
      supertypes = (await request.get('https://api.magicthegathering.io/v1/supertypes')).body.supertypes || [];
      subtypes = (await request.get('https://api.magicthegathering.io/v1/subtypes')).body.subtypes || [];
    } catch (e) {
      console.error(e);
      throw { errType: 'TOKEN_TYPE_FETCH_ERROR' };
    }

    // Load content
    const xml = fs.readFileSync(tokenFile, 'utf8');
    let tokenData;
    try {
      // Parse XML
      tokenData = await parseXML(xml);
      // Access data
      tokenData = tokenData.cockatrice_carddatabase.cards[0].card;
    } catch (e) {
      console.error(e);
      throw { errType: 'TOKEN_PARSE_ERROR' };
    }

    // Clean up data
    try {
      tokenData = tokenData.reduce((result, token) => result.concat(token.set.map(set => ({
        name: token.name[0],
        names: token.name,
        set: set._,
        imageUrl: set.$ ? set.$.picURL : undefined,
        cmc: 0,
        colors: token.color ? token.color.map(c => {
          switch (c) {
            case 'R': return 'Red';
            case 'W': return 'White';
            case 'B': return 'Black';
            case 'U': return 'Blue';
            case 'G': return 'Green';
            default: return c;
          }
        }) : [],
        colorIdentity: token.color,
        text: token.text ? token.text.join('\n') : '',
        power: token.pt ? token.pt[0].split('/')[0] : undefined,
        toughness: token.pt ? token.pt[0].split('/')[1] : undefined,
        printings: _.uniq(token.set.map(s => s._)),
        originalText: token.text ? token.text.join('\n') : '',
        type: token.type[0],
        originalType: token.type[0],
        setName: this.setDictionary.setCodeToName(set._) || '',
        supertypes: supertypes.filter(type => token.type.indexOf(type) > -1),
        types: types.filter(type => token.type.indexOf(type) > -1),
        subtypes: subtypes.filter(type => token.type.indexOf(type) > -1),
        token: true
      })), []), []);
      console.log('Initialized tokens.');
    } catch (e) {
      console.error(e);
      throw { errType: 'TOKEN_CLEAN_ERROR' };
    }

    // Remove duplicate arts
    tokenData = tokenData.reduce((result, token) => {
      if (result.find(t => t.name === token.name && t.set === token.set)) return result;
      return result.concat([token]);
    }, []);

    // Return data (reversed)
    return tokenData.reverse();
  }

  obtainSpecifiedCard: (name: string, setCode: ?string) => Promise<Object>
  async obtainSpecifiedCard(name: string, setCode: ?string) {
    let matches = [];

    // First check tokens
    matches = this.tokens.filter(token => token.name.match(new RegExp(`.*${name}.*`, 'gi')));
    if (setCode && matches.length) matches = matches.filter(token => token.set.toLowerCase() === (setCode || '').toLowerCase());

    // Check for cards
    const query = setCode ? { set: setCode, name } : { name };
    try {
      matches = matches.concat(await this.mtg.card.where(query));
    } catch (e) {
      console.error(e);
      // Let the user know if we encountered an error.
      throw { e: 'RETRIEVE_ERROR', error: `\n\`\`\`\n${e}\n${e.stack}\n\`\`\`\n` };
    }

    // Filter out duplicate results
    matches = _.uniqWith(matches.reverse(), (a, b) => a.name === b.name);
    // If there is a direct match, use that one.
    const exactMatch = matches.find(m => (m.name: any).toLowerCase() === (name: any).toLowerCase());
    if (exactMatch) matches = [exactMatch];

    // Take action based on the amount of matches
    switch (matches.length) {
      // No results
      case 0: {
        throw { e: 'NO_RESULTS' };
      }
      // 1 Result
      case 1: {
        return matches[0];
      }
      // Multiple results
      default: {
        throw { e: 'MANY_RESULTS', cards: matches };
      }
    }
  }

  obtainRecentOrSpecifiedCard: (name: ?string, setCode: ?string, channelId: string) => Promise<Object>
  async obtainRecentOrSpecifiedCard(name: ?string, setCode: ?string, channelId: string) {
    let card;
    // Obtain card from last mentioned
    if (!name) {
      const lastMentioned = this.appState.lastMentioned.find(md => md.channelId === channelId);
      // If none was mentioned, stop here and inform the user.
      if (!lastMentioned) throw { e: 'NON_MENTIONED' };
      card = lastMentioned.card;
    } else card = await this.obtainSpecifiedCard(name, setCode); // Otherwise obtain specified card
    return card;
  }
}
