// @flow
import Commons from '../Utils/Commons';

export default class BaseCommand {
  name: string;
  usage: string;
  description: string;
  aliases: Array<string>;
  commons: Commons;

  constructor(commons: Commons, name: string, usage: string, description: string, aliases?: Array<string> = []) {
    // Verify input
    if (!name) throw Error('Command name cannot be empty');
    // Initialize fields
    this.name = name;
    this.usage = usage;
    this.description = description;
    this.aliases = aliases;
    this.commons = commons;
  }

  // eslint-disable-next-line no-unused-vars
  async exec(args: Array<string>, userId: string, channelId: string, guildId?: ?string) {
    // override me
  }

}
