// @flow
import moment from 'moment';

export default class RateLimiter {

  count: number;
  max: number;
  timeframe: number;
  sincePeriod: number;

  constructor(max: number, timeframe: number) {
    // Initialize fields
    this.max = max;
    this.timeframe = timeframe;
    this.count = 0;
    this.sincePeriod = moment().unix();
    // Bind methods
    this.checkPermission = this.checkPermission.bind(this);
  }

  checkPermission: (register?: boolean) => boolean;
  checkPermission(register?: boolean = true) {
    if (moment().unix() - this.sincePeriod >= this.timeframe) {
      this.sincePeriod = moment().unix();
      this.count = 0;
    }
    const allowed = this.count < this.max;
    if (register && allowed) this.count += 1;
    return allowed;
  }
}
