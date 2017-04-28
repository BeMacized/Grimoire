// @flow
import EventEmitter from 'events';
import MTGSDK from 'mtgsdk';
import SetDictionaryRecord from '../Database/SetDictionaryRecord';

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
  Record: SetDictionaryRecord;

  constructor(mtg: MTGSDK, Record: SetDictionaryRecord) {
    super();
    // Initialize field(s)
    this.dictionary = [];
    this.mtg = mtg;
    this.Record = Record;
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
    this.save(setCode);
  }

  setTcgName: (setCode: string, mcmName: string) => void;
  setTcgName(setCode: string, tcgName: string) {
    this.dictionary = this.dictionary.map(de => (de.setCode === setCode ? Object.assign({}, de, { tcgName }) : de));
    this.save(setCode);
  }

  save: (string) => void;
  async save(setCode: string) {
    const entry = this.dictionary.find(e => e.setCode === setCode);
    if (!entry) throw Error('CANNOT SAVE FOR SETCODE NOT EXIST IN SET DICTIONARY');
    try {
      let record = await this.Record.findOne({ setCode });
      if (!record) record = new this.Record(entry);
      else {
        record.mcmName = entry.mcmName;
        record.tcgName = entry.tcgName;
      }
      await record.save();
    } catch (e) {
      throw Error('Encountered error with database', e);
    }
  }

  load: Function;
  async load() {
    // Load from mtgsdk
    const all = this.mtg.set.all();
    all.on('data', set => this.dictionary.push({ setCode: set.code, setName: set.name }));
    all.on('error', (err) => console.error(err));
    all.on('end', async () => {
      // Load from database
      try {
        const records = await this.Record.find({}).lean();
        records.forEach(r => { this.dictionary = this.dictionary.filter(e => e.setCode !== r.setCode).concat([r]); });
        this.ready = true;
        this.emit('ready');
        console.log('Initialized Set Dictionary.');
      } catch (e) {
        throw Error('Encountered error with database', e);
      }
    });
  }

}
