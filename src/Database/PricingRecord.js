// @flow
import mongoose from 'mongoose';

// Define schema
export default mongoose.model('PricingRecord', new mongoose.Schema({
  cardName: { type: String, required: true },
  setCode: { type: String, required: true },
  pricings: {
    type: [{
      key: { type: String, required: true },
      currency: { type: String, required: true },
      value: { type: Number, required: true }
    }],
    required: true
  },
  storeId: { type: String, required: true },
  storeName: { type: String, required: true },
  storeUrl: { type: String, required: true },
  lastUpdated: { type: Number, required: true }
}));
