// @flow
import mtg from 'mtgsdk';
import Discord from 'discord.js';
import FSPromise from 'fs-promise';
import mongoose from 'mongoose';
import MCMAPI from './ApiUtils/MCMAPI';
import TCGAPI from './ApiUtils/TCGAPI';
import SetDictionaryRecord from './Database/SetDictionaryRecord';
import PricingRecord from './Database/PricingRecord';
import PricingUtils from './PricingUtils';
import SetDictionary from './ApiUtils/SetDictionary';
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
  pricingUtils: PricingUtils;
  setDictionary: SetDictionary;
  commons: Commons;

  constructor() {
    // Show stacktraces for unhandled promise rejections
    process.on('unhandledRejection', (err) => {
      console.error(`UNHANDLED_REJECTION: ${JSON.stringify(err, null, 2)}`); // or whatever.
    });

    // Load config
    const config = new Config('./config.json', FSPromise);
    config.initialize().then(() => {
      // Connect to database
      mongoose.Promise = global.Promise;
      mongoose.connect(config.values.mongoURL);
      // Initialize App State
      const appState = new AppState();
      // Instantiate Discord controller
      this.discordController = new DiscordController(Discord, config.values.botToken, config.values.botUsername);
      // Instantiate Set dictionary
      this.setDictionary = new SetDictionary(mtg, SetDictionaryRecord);
      // Initialize commons
      this.commons = new Commons(appState, mtg, config.values, this.setDictionary, this.discordController.getChatTools().sendFile, this.discordController.getChatTools().sendMessage, this.discordController.getChatTools().getEmoji);
      // Instantiate Pricing Utils
      this.pricingUtils = new PricingUtils(MCMAPI, TCGAPI, this.commons, PricingRecord);
      // Instantiate Command Dispatcher
      this.commandDispatcher = new CommandDispatcher(this.commons, this.pricingUtils);
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
