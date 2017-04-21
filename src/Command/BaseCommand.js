// @flow
import type { MTGSDK } from 'mtgsdk';
import type { Message } from 'discord.js';
import type { ConfigType } from '../Utils/Config';
import AppState from '../AppState';


export default class BaseCommand {
  name: string;
  usage: string;
  description: string;
  aliases: Array<string>;
  tools: CommandTools;

  constructor(commandTools: CommandTools, name: string, usage: string, description: string, aliases?: Array<string> = []) {
    // Verify input
    if (!name) throw Error('Command name cannot be empty');
    // Initialize fields
    this.name = name;
    this.usage = usage;
    this.description = description;
    this.aliases = aliases;
    this.tools = commandTools;
  }

  exec(/* args: Array<string>, userId: string, channelId?: ?string, guildId?: ?string*/) {
    // override me
  }

}

export type CommandTools = {
  sendFile: (url: string, text: string, userId: string, channelId?: ?string, guildId?: ?string) => Promise<Message>,
  sendMessage: (text: string, userId: string, channelId?: ?string, guildId?: ?string) => Promise<Message>,
  mtg: MTGSDK,
  config: ConfigType,
  appState: AppState
};
