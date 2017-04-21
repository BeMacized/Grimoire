// @flow
import mtg from 'mtgsdk';
import Discord from 'discord.js';
import FSPromise from 'fs-promise';
import DiscordController from './DiscordController';
import ChatProcessor from './ChatProcessor';
import Config from './Utils/Config';
import AppState from './AppState';
import CommandDispatcher from './Command/CommandDispatcher';

class Grimoire {

  discordController: DiscordController;
  chatProcessor: ChatProcessor;
  config: Config;
  appState: AppState;
  commandDispatcher: CommandDispatcher;

  constructor() {
    // Load config
    this.config = new Config('./config.json', FSPromise);
    this.config.initialize().then(() => {
      // Initialize App State
      this.appState = new AppState();
      // Instantiate Discord controller
      this.discordController = new DiscordController(Discord, this.config.values.botToken, this.config.values.botUsername);
      // Instantiate Command Dispatcher
      this.commandDispatcher = new CommandDispatcher(Object.assign({}, this.discordController.getChatTools(), { mtg, config: this.config, appState: this.appState }));
      // Instantiate Chat processor
      this.chatProcessor = new ChatProcessor(this.discordController.getChatTools(), mtg, this.config.values, this.appState, this.commandDispatcher);
      // Connect Chat processor and Discord controller
      this.discordController.chatProcessor = this.chatProcessor;
      // Start Discord controller
      this.discordController.login();
    }).catch(e => { console.error(e); });
  }

}

export default new Grimoire();
