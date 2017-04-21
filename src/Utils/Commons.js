// @flow

import _ from 'lodash';
import type { MTGSDK } from 'mtgsdk';
import type { Message, Emoji } from 'discord.js';
import type { ConfigType } from './Config';
import AppState from './AppState';

export default class Commons {

  appState: AppState;
  mtg: MTGSDK;
  sendFile: (url: string, text: string, userId: string, channelId?: ?string, guildId?: ?string) => Promise<Message>;
  sendMessage: (text: string, userId: string, channelId?: ?string, guildId?: ?string) => Promise<Message>;
  config: ConfigType;
  getEmoji: (name: string, guildId: string) => ?Emoji;

  constructor(appState: AppState, mtg: MTGSDK, config: ConfigType,
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

    // Bind function(s)
    this.obtainRecentOrSpecifiedCard = this.obtainRecentOrSpecifiedCard.bind(this);
  }

  obtainRecentOrSpecifiedCard: (name: ?string, channelId: string) => Promise<Object>
  async obtainRecentOrSpecifiedCard(name: ?string, channelId: string) {
    let card;
    // Obtain card from last mentioned
    if (!name) {
      const lastMentioned = this.appState.lastMentioned.find(md => md.channelId === channelId);
      // If none was mentioned, stop here and inform the user.
      if (!lastMentioned) { throw 'Please either specify a card name, or make sure to mention a card using an inline reference beforehand.'; }
      card = lastMentioned.card;
    } else { // Obtain card from mtg api
      const query = { name };
      let matches;
      try {
        matches = await this.mtg.card.where(query);
      } catch (e) {
        console.error(e);
        // Let the user know if we encountered an error.
        const error: string = `\n\`\`\`\n${e}\n${e.stack}\n\`\`\`\n`;
        throw `I ran into some problems when trying to retrieve data for **'${name}'**!${error}`;
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
          throw `I could not find any results for **'${name}'**!`;
        }
        // 1 Result
        case 1: {
          card = matches[0];
          break;
        }
        // Multiple results
        default: {
          const cardList: string = matches.map(c => ` - ${c.name}`).reduce((total, value) => `${total + value}\n`, '');
          throw `There were too many results for **'${name}'**. Did you perhaps mean to pick any of the following?\n\n${cardList}`;
        }
      }
    }
    return card;
  }
}
