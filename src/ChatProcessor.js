// @flow

// Dependencies
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
    const loadMsg = await this.commons.sendMessage('```\nLoading card...\n```', userId, channelId).catch(e => { throw e; });

    // Construct query
    const query: Object = { name: cardName };
    if (setCode) query.set = setCode;

    let card;
    try {
      card = await this.commons.cardUtils.obtainSpecifiedCard(cardName, setCode, channelId);
    } catch (err) {
      switch (err.e) {
        case 'RETRIEVE_ERROR': this.commons.sendMessage(`<@${userId}>, I ran into some problems when trying to retrieve data for **'${cardName}'**!${err.error}`, userId, channelId); break;
        case 'NO_RESULTS': this.commons.sendMessage(`<@${userId}>, I could not find any results for **'${cardName}'**${setCode ? ` with set code ${setCode}!` : '!'}`, userId, channelId); break;
        case 'MANY_RESULTS': {
          const cardList: string = err.cards.map(c => ` - ${c.token ? '_[TOKEN]_ ' : ''}${c.name}`).reduce((total, value) => `${total + value}\n`, '');
          this.commons.sendMessage(`<@${userId}>, There were too many results for **'${cardName}'**${setCode ? ` with set code ${setCode}.` : '.'} Did you perhaps mean to pick any of the following?\n\n${cardList}`, userId, channelId);
          break;
        }
        case 'NON_MENTIONED': this.commons.sendMessage(`<@${userId}>, Please either specify a card name, or make sure to mention a card using an inline reference beforehand.`, userId, channelId); break;
        default: { console.error(err); this.commons.sendMessage(`<@${userId}>, An unknown error occurred.`, userId, channelId); break; }
      }
      loadMsg.delete().catch(() => {});
      return;
    }

    if (!card.imageUrl) {
      this.commons.sendMessage(
        `<@${userId}>, There is no card image available for **'${cardName}'**${setCode ? ` with set code ${setCode}!` : '!'}`,
        userId,
        channelId).catch(e => { throw e; });
      loadMsg.delete().catch(() => {});
      return;
    }

    // Show the user the card art
    loadMsg.edit(`\`\`\`\nLoading '${card.name}' from set '${card.setName}'\n\`\`\``).catch(() => {});
    await this.commons.sendFile(
      card.imageUrl,
        `**${card.name}**${card.token ? ' _[TOKEN]_' : ''}\n${card.setName} (${card.set})`,
        userId,
        channelId).catch(e => {
          console.error(e);
          loadMsg.edit(`<@${userId}>, I was not able to finish uploading the card art!`).catch(() => {});
        });
    loadMsg.delete().catch(() => {});
  }

}
