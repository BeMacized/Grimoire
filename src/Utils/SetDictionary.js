// @flow

import MTGSDK from 'mtgsdk';

export type DictionaryEntry = {
  setCode: string,
  setName: string,
  mcmName?: string
};

export default class SetDictionary {

  dictionary: Array<DictionaryEntry>;
  mtg: MTGSDK;

  constructor(mtg: MTGSDK) {
    // Initialize field(s)
    this.dictionary = [];
    this.mtg = mtg;
    // Bind functions
    this.setCodeToMcmName = this.setCodeToMcmName.bind(this);
    this.setCodeToName = this.setCodeToName.bind(this);
    this.setMcmName = this.setMcmName.bind(this);
    this.save = this.save.bind(this);
    this.load = this.load.bind(this);
    // Load initial data
    this.load();
  }

  setCodeToMcmName: (setCode: string) => ?string;
  setCodeToMcmName(setCode: string) {
    const result = this.dictionary.find(de => de.setCode === setCode && de.mcmName !== undefined);
    if (result) return result.mcmName;
    return null;
  }

  setCodeToName: (setCode: string) => ?string;
  setCodeToName(setCode: string) {
    const result = this.dictionary.find(de => de.setCode === setCode);
    if (result) return result.setName;
    return null;
  }

  setMcmName: (setCode: string, mcmName: string) => void;
  setMcmName(setCode: string, mcmName: string) {
    this.dictionary = this.dictionary.map(de => (de.setCode === setCode ? Object.assign({}, de, { mcmName }) : de));
    this.save();
  }

  save: Function;
  save() {
    // TODO: Save to DB
  }

  load: Function;
  async load() {
    // Load from mtgsdk
    this.mtg.set.all().on('data', set => this.dictionary.push({ setCode: set.code, setName: set.name }));
    // TODO: Load from DB
  }


}
