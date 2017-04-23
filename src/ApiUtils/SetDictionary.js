// @flow
import EventEmitter from 'events';
import MTGSDK from 'mtgsdk';

export type DictionaryEntry = {
  setCode: string,
  setName: string,
  mcmName?: string,
  tcgName?: string
};

export default class SetDictionary extends EventEmitter {

  dictionary: Array<DictionaryEntry>;
  mtg: MTGSDK;
  ready: boolean = false;

  constructor(mtg: MTGSDK) {
    super();
    // Initialize field(s)
    this.dictionary = [];
    this.mtg = mtg;
    // Bind functions
    this.setCodeToMcmName = this.setCodeToMcmName.bind(this);
    this.setCodeToTcgName = this.setCodeToTcgName.bind(this);
    this.setCodeToName = this.setCodeToName.bind(this);
    this.setMcmName = this.setMcmName.bind(this);
    this.setTcgName = this.setTcgName.bind(this);
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

  setCodeToTcgName: (setCode: string) => ?string;
  setCodeToTcgName(setCode: string) {
    const result = this.dictionary.find(de => de.setCode === setCode && de.tcgName !== undefined);
    if (result) return result.tcgName;
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

  setTcgName: (setCode: string, mcmName: string) => void;
  setTcgName(setCode: string, tcgName: string) {
    this.dictionary = this.dictionary.map(de => (de.setCode === setCode ? Object.assign({}, de, { tcgName }) : de));
    this.save();
  }

  save: Function;
  save() {
    // TODO: Save to DB
  }

  load: Function;
  async load() {
    // Load from mtgsdk
    const all = this.mtg.set.all();
    all.on('data', set => this.dictionary.push({ setCode: set.code, setName: set.name }));
    all.on('error', (err) => console.error(err));
    all.on('end', () => {
      this.ready = true;
      this.emit('ready');
    });
    // TODO: Load from DB
  }


}
