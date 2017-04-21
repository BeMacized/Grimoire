// @flow
import BaseCommand from './BaseCommand';
import Prints from './All/Prints';
import type { CommandTools } from './BaseCommand';

export default class CommandDispatcher {

  commands: Array<BaseCommand>;

  constructor(commandTools: CommandTools) {
    // Initialize fields
    this.commands = [
      new Prints(commandTools)
    ];

    // Bind functions
    this.processMessage = this.processMessage.bind(this);
  }

  processMessage: (message: string, userId: string, channelId?: ?string, guildId?: ?string) => boolean;
  processMessage(message: string, userId: string, channelId?: string, guildId?: string): boolean {
    // Strip off mention & obtain split data
    const args = message.replace(new RegExp('(^<@[0-9]*> )|(^!)|(^/)', 'gi'), '').split(/\s+/gi);

    // If no command was supplied quit here
    if (args.length === 0) { return false; }

    // Extract command name and remove it from args
    const cmd = args.shift();

    // Attempt finding a relevant command class
    const cmdObj: ?BaseCommand = this.commands.find(c => c.name.toLowerCase() === cmd.toLowerCase() || c.aliases.indexOf(cmd.toLowerCase()) > -1);

    // If none found, stop here
    if (!cmdObj) { return false; }

    // Call the found command
    try {
      if (channelId) cmdObj.exec(args, userId, channelId, guildId);
      else cmdObj.exec(args, userId);
    } catch (e) {
      console.error('CommandDispatcher could not execute command: ', e);
    }

    return true;
  }

}
