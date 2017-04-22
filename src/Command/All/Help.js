// @flow

import BaseCommand from '../BaseCommand';
import Commons from '../../Utils/Commons';

export default class Help extends BaseCommand {

  getCommands: () => Array<BaseCommand>;

  constructor(commons: Commons, getCommands: () => Array<BaseCommand>) {
    super(
      commons,
      'help',
      '',
      'Shows the help text, containing all of the command references.',
      []
    );
    this.getCommands = getCommands;

    // Bind method(s)
    this.exec = this.exec.bind(this);
  }

  exec: (args: Array<string>, userId: string, channelId: string, guildId?: ?string) => Promise<void>;
  async exec(args: Array<string>, userId: string, channelId: string) {
    // Construct message
    // Add header
    let message = [
      "Hi there! This is the help page for Mac's Grimoire.",
      'Below, you will find all usable commands with what they do and how to use them.',
      'You can use both `/` or `!` as the command prefix.',
      ''
    ];

    // Add command data
    this.getCommands().forEach(cmd => {
      message.push(`**!${cmd.name}** ${cmd.usage}`);
      if (cmd.aliases.length > 0) message.push(`**Aliases:** _${cmd.aliases.join(', ')}_`);
      message.push(cmd.description);
      message.push('');
    });

    // Add footer
    message = message.concat([
      "Mac's Grimoire is being developed by BeMacized (<https://bemacized.net/>)",
      '',
      'You can find the source code over at GitHub: <https://github.com/BeMacized/Grimoire>',
      'Contributors are always welcome!',
      'For feature suggestions or bug reports, please submit an issue with the issue tracker: <https://github.com/BeMacized/Grimoire/issues>',
      '',
      "In case you have any questions regarding Mac's Grimoire, feel free to search for contact",
      'via Twitter: <https://twitter.com/BeMacized>',
      'or via e-mail: info@bemacized.net'
    ]);

    // Send message
    this.commons.sendMessage(message.join('\n'), userId, channelId);
  }

}
