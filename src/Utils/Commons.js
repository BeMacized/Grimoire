// @flow

import type { MTGSDK } from 'mtgsdk';
import type { Message, Emoji } from 'discord.js';
import type { ConfigType } from './Config';
import AppState from './AppState';
import CardUtils from './CardUtils';
import SetDictionary from '../ApiUtils/SetDictionary';

export default class Commons {

  appState: AppState;
  mtg: MTGSDK;
  sendFile: (url: string, text: string, userId: string, channelId?: ?string, guildId?: ?string) => Promise<Message>;
  sendMessage: (text: string, userId: string, channelId?: ?string, guildId?: ?string) => Promise<Message>;
  config: ConfigType;
  setDictionary: SetDictionary;
  getEmoji: (name: string, guildId: string) => ?Emoji;
  cardUtils: CardUtils;

  constructor(
    appState: AppState,
    mtg: MTGSDK,
    config: ConfigType,
    setDictionary: SetDictionary,
    cardUtils: CardUtils,
    sendFile: (url: string, text: string, userId: string, channelId?: ?string, guildId?: ?string) => Promise<Message>,
    sendMessage: (text: string, userId: string, channelId?: ?string, guildId?: ?string) => Promise<Message>,
    getEmoji: (name: string, guildId: string) => ?Emoji) {
    // Initialize fields
    this.mtg = mtg;
    this.appState = appState;
    this.sendMessage = sendMessage;
    this.sendFile = sendFile;
    this.config = config;
    this.getEmoji = getEmoji;
    this.cardUtils = cardUtils;
    this.setDictionary = setDictionary;
  }

}
