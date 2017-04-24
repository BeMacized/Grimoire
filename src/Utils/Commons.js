// @flow

import _ from 'lodash';
import type { MTGSDK } from 'mtgsdk';
import type { Message, Emoji } from 'discord.js';
import type { ConfigType } from './Config';
import AppState from './AppState';
import SetDictionary from '../ApiUtils/SetDictionary';

export default class Commons {

  appState: AppState;
  mtg: MTGSDK;
  sendFile: (url: string, text: string, userId: string, channelId?: ?string, guildId?: ?string) => Promise<Message>;
  sendMessage: (text: string, userId: string, channelId?: ?string, guildId?: ?string) => Promise<Message>;
  config: ConfigType;
  setDictionary: SetDictionary;
  getEmoji: (name: string, guildId: string) => ?Emoji;

  constructor(appState: AppState, mtg: MTGSDK, config: ConfigType, setDictionary: SetDictionary,
    sendFile: (url: string, text: string, userId: string, channelId?: ?string, guildId?: ?string) => Promise<Message>,
    sendMessage: (text: string, userId: string, channelId?: ?string, guildId?: ?string) => Promise<Message>,
    getEmoji: (name: string, guildId: string) => ?Emoji) {
    // Initialize fields
    this.mtg = mtg;
    this.appState = appState;
    this.sendMessage = sendMessage;
    this.sendFile = sendFile;
    this.config = config;
    this.getEmoji = getEmoji;
    this.setDictionary = setDictionary;

    // Bind function(s)
    this.obtainRecentOrSpecifiedCard = this.obtainRecentOrSpecifiedCard.bind(this);
    this.obtainSpecifiedCard = this.obtainSpecifiedCard.bind(this);
  }

  obtainSpecifiedCard: (name: string, setCode: ?string) => Promise<Object>
  async obtainSpecifiedCard(name: string, setCode: ?string) {
    const query = setCode ? { set: setCode, name } : { name };
    let matches;
    try {
      matches = await this.mtg.card.where(query);
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
