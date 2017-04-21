// @flow
import mtg from 'mtgsdk';
import Discord from 'discord.js';
import FSPromise from 'fs-promise';
import DiscordController from './DiscordController';
import ChatProcessor from './ChatProcessor';
import Config from './Utils/Config';
import AppState from './Utils/AppState';
import Commons from './Utils/Commons';
import CommandDispatcher from './Command/CommandDispatcher';

class Grimoire {

  discordController: DiscordController;
  chatProcessor: ChatProcessor;
  commandDispatcher: CommandDispatcher;
  commons: Commons;

  constructor() {
    // Show stacktraces for unhandled promise rejections
    process.on('unhandledRejection', (err) => {
      console.error(`UNHANDLED_REJECTION: ${err} ${err.stack}`); // or whatever.
    });

    // Load config
    const config = new Config('./config.json', FSPromise);
    config.initialize().then(() => {
      // Initialize App State
      const appState = new AppState();
      // Instantiate Discord controller
      this.discordController = new DiscordController(Discord, config.values.botToken, config.values.botUsername);
      // Initialize commons
      this.commons = new Commons(appState, mtg, config.values, this.discordController.getChatTools().sendFile, this.discordController.getChatTools().sendMessage);
      // Instantiate Command Dispatcher
      this.commandDispatcher = new CommandDispatcher(this.commons);
      // Instantiate Chat processor
      this.chatProcessor = new ChatProcessor(this.commons, this.commandDispatcher);
      // Connect Chat processor and Discord controller
      this.discordController.chatProcessor = this.chatProcessor;
      // Start Discord controller
      this.discordController.login();
    }).catch(e => { console.error(e); });
  }

}

export default new Grimoire();
