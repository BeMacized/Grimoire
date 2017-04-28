// @flow

import BaseCommand from '../BaseCommand';
import Commons from '../../Utils/Commons';

export default class Prints extends BaseCommand {

  constructor(commons: Commons) {
    super(
      commons,
      'rulings',
      '[card name]',
      'Retrieves the current rulings of the specified card. Lists for the last shown card if no card name supplied.',
      ['rules', 'ruling']
    );
    // Bind method(s)
    this.exec = this.exec.bind(this);
  }

  exec: (args: Array<string>, userId: string, channelId: string, guildId?: ?string) => Promise<void>;
  async exec(args: Array<string>, userId: string, channelId: string) {
    let card;
    try {
      card = await this.commons.cardUtils.obtainRecentOrSpecifiedCard(args.join(' '), null, channelId);
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

    // Let the user know if there are no known rulings
    if (!card.rulings || card.rulings.length === 0) {
      this.commons.sendMessage(`<@${userId}>, There are no known rulings for **'${card.name}'**`, userId, channelId);
      return;
    }

    // Construct message
    let message: string = `The following ruling(s) were released for **'${card.name}'**:`;
    let lastDate = '';
    card.rulings.forEach(ruling => {
      if (ruling.date !== lastDate) {
        lastDate = ruling.date;
        message += `\n\n**${ruling.date}**:`;
      }
      message += `\n - ${ruling.text}`;
    });

    // Send message
    this.commons.sendMessage(message, userId, channelId);
  }

}
