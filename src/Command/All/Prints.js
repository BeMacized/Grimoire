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
      card = await this.commons.obtainRecentOrSpecifiedCard(args.join(' '), channelId);
    } catch (err) {
      this.commons.sendMessage(`<@${userId}>, ${err}`, userId, channelId);
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
