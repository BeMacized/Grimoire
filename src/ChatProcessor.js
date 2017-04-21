// @flow

// Dependencies
import _ from 'lodash';
import type { Message } from 'discord.js';
import type { MTGSDK } from 'mtgsdk';
import type { ConfigType } from './Utils/Config';
import AppState from './AppState';
import CommandDispatcher from './Command/CommandDispatcher';

// Types
export type ChatTools = {
  sendFile: (url: string, text: string, userId: string, channelId?: ?string, guildId?: ?string) => Promise<Message>,
  sendMessage: (text: string, userId: string, channelId?: ?string, guildId?: ?string) => Promise<Message>
};

// Class
export default class ChatProcessor {

  chatTools: ChatTools;
  mtg: MTGSDK;
  config: ConfigType;
  appState: AppState;
  commandDispatcher: CommandDispatcher;

  constructor(chatTools: ChatTools, mtg: MTGSDK, config: ConfigType, appState: AppState, commandDispatcher: CommandDispatcher) {
    // Initialize field(s)
    this.chatTools = chatTools;
    this.mtg = mtg;
    this.config = config;
    this.appState = appState;
    this.commandDispatcher = commandDispatcher;
    // Bind method(s)
    this.process = this.process.bind(this);
    this.showInlineCard = this.showInlineCard.bind(this);
  }

  process: (msg: string, userId: string, channelId: string, guildId?: ?string) => void;
  process(msg: string, userId: string, channelId: string, guildId?: ?string) {
    // Extract inline card names
    const cardNames: Array<string> = (msg.match(/(?=(<<)).+?(?=(>>))/g) || []).map(n => n.substring(2, n.length));
    if (cardNames.length > this.config.maxInlineCardReferences) {
      this.chatTools.sendMessage(
      `<@${userId}>, It is not permitted to use more than ${this.config.maxInlineCardReferences} card references per message.`,
      userId,
      channelId).catch(e => { throw e; });
      return;
    }
    if (cardNames.length > 0) {
      cardNames.forEach(async (name, index) => {
      // Extract set code
        let card;
        if (name.indexOf('|') > -1) {
          const split = name.split('|');
          name = split[0];
          const code = split[1];
          card = await this.showInlineCard(name, userId, channelId, guildId, code);
        } else card = await this.showInlineCard(name, userId, channelId, guildId); // Just the most recent one if not specified
      // Save card as last mentioned to state
        if (index === cardNames.length - 1 && card) this.appState.setLastMentioned(channelId, card.id, card.set);
      });
      return;
    }

    // Process command
    if (msg.substring(0, 1) === '!' || msg.substring(0, 1) === '/') { this.commandDispatcher.processMessage(msg, userId, channelId, guildId); }
  }

  showInlineCard: (cardName: string, userId: string, channelId?: ?string, guildId?: ?string, setCode?: string) => Promise<?Object>;
  async showInlineCard(cardName: string, userId: string, channelId?: ?string, guildId?: ?string, setCode?: string = '') {
    // Ensure setcode validity
    if (!guildId && !setCode && channelId) setCode = channelId;

    // Construct query
    const query: Object = { name: cardName };
    if (setCode) query.set = setCode;

    // Retrieve search results
    let matches;
    try { matches = await this.mtg.card.where(query); } catch (e) {
      // Let the user know if we encountered an error.
      const error: string = `\n\`\`\`\n${e}\n${e.stack}\n\`\`\`\n`;
      this.chatTools.sendMessage(
        `<@${userId}>, I ran into some problems when trying to retrieve data for **'${cardName}'**!${error}`,
        userId,
        channelId).catch(e => { throw e; }); // eslint-disable-line no-shadow
      return null;
    }

    // Filter out duplicate results
    matches = _.uniqWith(matches.reverse(), (a, b) => a.name === b.name);

    switch (matches.length) {
      // No results
      case 0: {
        this.chatTools.sendMessage(
          `<@${userId}>, I could not find any results for **'${cardName}'**${setCode ? ` with set code ${setCode}!` : '!'}`,
          userId,
          channelId).catch(e => { throw e; });
        return null;
      }
      // 1 Result
      case 1: {
        // Inform the user if there's no card image available
        if (!matches[0].imageUrl) {
          this.chatTools.sendMessage(
            `<@${userId}>, There is no card image available for **'${cardName}'**${setCode ? ` with set code ${setCode}!` : '!'}`,
            userId,
            channelId).catch(e => { throw e; });
          return null;
        }
        // Show the user the card art
        this.chatTools.sendFile(
          matches[0].imageUrl,
            `**${matches[0].name}**\n${matches[0].setName} (${matches[0].set})`,
            userId,
            channelId).catch(e => { throw e; });
        return matches[0];
      }
      // Multiple results
      default: {
        const cardList: string = matches.map(card => ` - ${card.name}`).reduce((total, value) => `${total + value}\n`, '');
        this.chatTools.sendMessage(
        `<@${userId}>, There were too many results for **'${cardName}'**${setCode ? ` with set code ${setCode}.` : '.'} Did you perhaps mean to pick any of the following?\n\n${cardList}`,
          userId,
          channelId).catch(e => { throw e; });
        return null;
      }
    }
  }

}
