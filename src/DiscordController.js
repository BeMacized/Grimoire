// @flow
import Discord, { Client, Message } from 'discord.js';
import ChatProcessor from './ChatProcessor';

export default class DiscordController {

  discord: Discord;
  client: Client;
  botToken: string;
  username: string;
  chatProcessor: ?ChatProcessor;

  constructor(discord: Discord, botToken: string, username: string) {
    // Instantiate fields
    this.discord = discord;
    this.botToken = botToken;
    this.username = username;
    this.client = new this.discord.Client();

    // Setup event listeners
    // Log on ready
    this.client.on('ready', () => {
      this.client.user.setAvatar('./src/Resources/Avatar.png').catch(e => { // eslint-disable-line global-require
        if (e) console.error('Warning: DiscordController could not assert Avatar.');
      });
      this.client.user.setUsername(this.username).catch(e => {
        if (e) console.error('Warning: DiscordController could not assert username.');
      });
      console.log('Connected to Discord.');
    });

    // Log reconnects
    this.client.on('reconnecting', () => {
      console.error('An unexpected disconnect occurred. Reconnecting to Discord.');
    });

    // Log errors
    this.client.on('error', (e) => {
      console.error(`Discord.JS encountered a severe error:\n${e}\n${e.stack}`);
    });

    // Pass on messages
    this.client.on('message', message => {
      // Don't serve bots
      if (message.author.bot) return;
      // Pass message onto processor
      if (this.chatProcessor) this.chatProcessor.process(message.content, message.author.id, message.channel.id, message.guild ? message.guild.id : null);
    });
  }

  login: Function;
  login() {
    if (!this.client) throw Error('Attempted login before client initialisation');
    this.client.login(this.botToken).catch(() => {});
  }

  getChatTools: () => {
    sendFile: (url: string, text: string, userId: string, channelId?: ?string) => Promise<Message>,
    sendMessage: (text: string, userId: string, channelId?: ?string) => Promise<Message>
  }
  getChatTools() {
    return {
      sendFile: async (url: string, text: string, userId: string, channelId?: ?string) => {
        // Obtain destination
        let destination = this.client.channels.get(channelId);
        if (!destination) {
          destination = await this.client.fetchUser(userId);
          if (!destination) throw Error(`DiscordController could not obtain channel object for Channel ID '${channelId || 'Not Supplied'}' or User ID'${userId}'`);
        }
        // Send file
        return destination.sendFile(url, 'card.png', text, { split: true });
      },
      sendMessage: async (text: string, userId: string, channelId?: ?string) => {
        // Obtain destination
        let destination = this.client.channels.get(channelId);
        if (!destination) {
          destination = await this.client.fetchUser(userId);
          if (!destination) throw Error(`DiscordController could not obtain channel object for Channel ID '${channelId || 'Not Supplied'}' or User ID'${userId}'`);
        }
        // Send message
        return destination.sendMessage(text, { split: true });
      }
    };
  }


}
