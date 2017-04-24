// @flow
import mongoose from 'mongoose';

// Define schema
export default mongoose.model('SetDictionaryRecord', new mongoose.Schema({
  setCode: { type: String, required: true },
  setName: { type: String, required: true },
  mcmName: { type: String, required: false },
  tcgName: { type: String, required: false }
}));
