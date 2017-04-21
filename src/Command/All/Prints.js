// @flow

import BaseCommand from '../BaseCommand';
import type { CommandTools } from '../BaseCommand';

export default class Prints extends BaseCommand {

  constructor(commandTools: CommandTools) {
    super(
      commandTools,
      'prints',
      '[card name]',
      'Retrieve all sets that a card was printed in. Lists the last shown card if no card name supplied.',
      ['sets', 'versions']
    );
    // Bind method(s)
    this.exec = this.exec.bind(this);
  }

  exec: (args: Array<string>, userId: string, channelId?: ?string, guildId?: ?string) => void;
  exec(args: Array<string>, userId: string, channelId?: ?string) {
    this.tools.sendMessage('Test', userId, channelId);
  }

}
