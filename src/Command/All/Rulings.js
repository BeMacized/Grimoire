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
      card = await this.commons.obtainRecentOrSpecifiedCard(args.join(' '), channelId);
    } catch (err) {
      this.commons.sendMessage(`<@${userId}>, ${err}`, userId, channelId);
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
