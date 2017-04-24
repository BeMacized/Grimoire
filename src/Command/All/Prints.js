// @flow

import BaseCommand from '../BaseCommand';
import Commons from '../../Utils/Commons';

export default class Prints extends BaseCommand {

  constructor(commons: Commons) {
    super(
      commons,
      'prints',
      '[card name]',
      'Retrieves all sets that a card was printed in. Lists for the last shown card if no card name supplied.',
      ['sets', 'versions']
    );
    // Bind method(s)
    this.exec = this.exec.bind(this);
  }

  exec: (args: Array<string>, userId: string, channelId: string, guildId?: ?string) => Promise<void>;
  async exec(args: Array<string>, userId: string, channelId: string) {
    let card;
    try {
      card = await this.commons.obtainRecentOrSpecifiedCard(args.join(' '), null, channelId);
    } catch (err) {
      switch (err.e) {
        case 'RETRIEVE_ERROR': this.commons.sendMessage(`<@${userId}>, I ran into some problems when trying to retrieve data for **'${args.join(' ')}'**!${err.error}`, userId, channelId); break;
        case 'NO_RESULTS': this.commons.sendMessage(`<@${userId}>, I could not find any results for **'${args.join(' ')}'**!`, userId, channelId); break;
        case 'MANY_RESULTS': {
          const cardList: string = err.cards.map(c => ` - ${c.name}`).reduce((total, value) => `${total + value}\n`, '');
          this.commons.sendMessage(`<@${userId}>, There were too many results for **'${args.join(' ')}'**. Did you perhaps mean to pick any of the following?\n\n${cardList}`, userId, channelId);
          break;
        }
        case 'NON_MENTIONED': this.commons.sendMessage(`<@${userId}>, Please either specify a card name, or make sure to mention a card using an inline reference beforehand.`, userId, channelId); break;
        default: { console.error(err); this.commons.sendMessage(`<@${userId}>, An unknown error occurred.`, userId, channelId); break; }
      }
      return;
    }

    // Obtain sets
    const sets = await Promise.all(card.printings.map(async (setCode) => {
      const set = await this.commons.mtg.set.find(setCode);
      return { code: setCode, name: set.set.name };
    }));

    // Construct message
    let message: string = `The card **'${card.name}'** was printed in the following sets:\n`;
    sets.forEach(set => { message += `\n - ${set.name} (**${set.code}**)`; });

    // Send message
    this.commons.sendMessage(message, userId, channelId);
  }

}
