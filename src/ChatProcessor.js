// @flow

// Dependencies
import _ from 'lodash';
import CommandDispatcher from './Command/CommandDispatcher';
import Commons from './Utils/Commons';

// Class
export default class ChatProcessor {

  commons: Commons;
  commandDispatcher: CommandDispatcher;

  constructor(commons: Commons, commandDispatcher: CommandDispatcher) {
    // Initialize field(s)
    this.commons = commons;
    this.commandDispatcher = commandDispatcher;
    // Bind method(s)
    this.process = this.process.bind(this);
    this.showInlineCard = this.showInlineCard.bind(this);
  }

  process: (msg: string, userId: string, channelId: string, guildId?: ?string) => void;
  process(msg: string, userId: string, channelId: string, guildId?: ?string) {
    // Extract inline card names
    const cardNames: Array<string> = (msg.match(/(?=(<<)).+?(?=(>>))/g) || []).map(n => n.substring(2, n.length));
    if (cardNames.length > this.commons.config.maxInlineCardReferences) {
      this.commons.sendMessage(
      `<@${userId}>, It is not permitted to use more than ${this.commons.config.maxInlineCardReferences} card references per message.`,
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
        if (index === cardNames.length - 1 && card) this.commons.appState.setLastMentioned(channelId, card);
      });
      return;
    }

    // Process command
    if (msg.substring(0, 1) === '!' || msg.substring(0, 1) === '/') { this.commandDispatcher.processMessage(msg, userId, channelId, guildId); }
  }

  showInlineCard: (cardName: string, userId: string, channelId: string, guildId: ?string, setCode?: string) => Promise<?Object>;
  async showInlineCard(cardName: string, userId: string, channelId: string, guildId: ?string, setCode?: string = '') {
    console.log('SHOWING INLINE CARD', cardName, setCode);

    const loadMsg = await this.commons.sendMessage('```\nLoading card...\n```', userId, channelId).catch(e => { throw e; });

    // Construct query
    const query: Object = { name: cardName };
    if (setCode) query.set = setCode;

    console.log('QUERY', query);

    // Retrieve search results
    let matches;
    try { matches = await this.commons.mtg.card.where(query); } catch (e) {
      // Let the user know if we encountered an error.
      const error: string = `\n\`\`\`\n${e}\n${e.stack}\n\`\`\`\n`;
      this.commons.sendMessage(
        `<@${userId}>, I ran into some problems when trying to retrieve data for **'${cardName}'**!${error}`,
        userId,
        channelId).catch(e => { throw e; }); // eslint-disable-line no-shadow
      loadMsg.delete().catch(() => {});
      return null;
    }

    // Filter out duplicate results
    matches = _.uniqWith(matches.reverse(), (a, b) => a.name === b.name);
    // If there is a direct match, use that one.
    const exactMatch = matches.find(m => m.name.toLowerCase() === cardName.toLowerCase());
    if (exactMatch) matches = [exactMatch];

    switch (matches.length) {
      // No results
      case 0: {
        this.commons.sendMessage(
          `<@${userId}>, I could not find any results for **'${cardName}'**${setCode ? ` with set code ${setCode}!` : '!'}`,
          userId,
          channelId).catch(e => { throw e; });
        loadMsg.delete().catch(() => {});
        return null;
      }
      // 1 Result
      case 1: {
        // Inform the user if there's no card image available
        if (!matches[0].imageUrl) {
          this.commons.sendMessage(
            `<@${userId}>, There is no card image available for **'${cardName}'**${setCode ? ` with set code ${setCode}!` : '!'}`,
            userId,
            channelId).catch(e => { throw e; });
          loadMsg.delete().catch(() => {});
          return null;
        }
        // Show the user the card art
        loadMsg.edit(`\`\`\`\nLoading '${matches[0].name}' from set '${matches[0].setName}'\n\`\`\``).catch(() => {});
        await this.commons.sendFile(
          matches[0].imageUrl,
            `**${matches[0].name}**\n${matches[0].setName} (${matches[0].set})`,
            userId,
            channelId).catch(e => {
              console.error(e);
              loadMsg.edit(`<@${userId}>, I was not able to finish uploading the card art!`).catch(() => {});
            });
        loadMsg.delete().catch(() => {});
        return matches[0];
      }
      // Multiple results
      default: {
        const cardList: string = matches.map(card => ` - ${card.name}`).reduce((total, value) => `${total + value}\n`, '');
        this.commons.sendMessage(
        `<@${userId}>, There were too many results for **'${cardName}'**${setCode ? ` with set code ${setCode}.` : '.'} Did you perhaps mean to pick any of the following?\n\n${cardList}`,
          userId,
          channelId).catch(e => { throw e; });
        loadMsg.delete().catch(() => {});
        return null;
      }
    }
  }

}
