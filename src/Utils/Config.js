// @flow
import FSPromise from 'fs-promise';

export const defaultConfig = {
  maxInlineCardReferences: 6,
  botToken: '',
  botUsername: 'Mac\'s Grimoire',
  mcmApiHost: 'sandbox.mkmapi.eu',
  mcmToken: '',
  mcmSecret: '',
  tcgApiHost: '',
  tcgKey: '',
  mongoURL: '',
  pricingDataTimeout: 21600
};

export type ConfigType = {
  maxInlineCardReferences: number,
  botToken: string,
  botUsername: string,
  mcmApiHost: string,
  mcmToken: string,
  mcmSecret: string,
  tcgApiHost: string,
  tcgKey: string,
  mongoURL: string,
  pricingDataTimeout: number
};

export default class Config {
  values: ConfigType;
  configPath: string;
  fsp: FSPromise;

  constructor(configPath: string, fsp: FSPromise) {
    // Initialize fields
    this.configPath = configPath;
    this.fsp = fsp;
    this.values = defaultConfig;
  }

  initialize: Function;
  async initialize() {
    // If config does not exist at path, write it.
    if (!this.fsp.existsSync(this.configPath)) {
      try {
        await this.fsp.writeFile(this.configPath, JSON.stringify(defaultConfig, null, 2));
      } catch (e) {
        console.error('Could not save default config file:', e);
      }
    }

    // Load the config
    let data: string;
    try {
      data = await this.fsp.readFile(this.configPath, 'utf8');
    } catch (e) {
      console.error('Could not load from existing config file:', e);
      return;
    }

    // Parse the config
    let parsedData: Object;
    try {
      parsedData = JSON.parse(data);
      if (!parsedData) throw Error();
    } catch (e) {
      console.error('Could not load from existing config file because of a syntax error:', e);
      return;
    }

    // Store in field
    this.values = Object.assign({}, defaultConfig, parsedData);
  }
}
