// @flow

import moment from 'moment';
import BaseCommand from '../BaseCommand';
import Commons from '../../Utils/Commons';
import PricingUtils from '../../PricingUtils';

export default class Pricing extends BaseCommand {

  pricingUtils: PricingUtils;

  constructor(commons: Commons, pricingUtils: PricingUtils) {
    super(
      commons,
      'pricing',
      '[card name]',
      'Retrieves the current pricing for a card. Lists for the last shown card if no card name supplied.',
      ['price', 'dollarydoos']
    );
    // Initialize field
    this.pricingUtils = pricingUtils;
    // Bind method(s)
    this.exec = this.exec.bind(this);
  }

  exec: (args: Array<string>, userId: string, channelId: string, guildId?: ?string) => Promise<void>;
  async exec(args: Array<string>, userId: string, channelId: string) {
    let card;
    try {
      card = await this.commons.cardUtils.obtainRecentOrSpecifiedCard(args.join(' '), null, channelId);
    } catch (err) {
      switch (err.e) {
        case 'RETRIEVE_ERROR': this.commons.sendMessage(`<@${userId}>, I ran into some problems when trying to retrieve data for **'${args.join(' ')}'**!${err.error}`, userId, channelId); break;
        case 'NO_RESULTS': this.commons.sendMessage(`<@${userId}>, I could not find any results for **'${args.join(' ')}'**!`, userId, channelId); break;
        case 'MANY_RESULTS': {
          const cardList: string = err.cards.map(c => ` - ${c.name}`).reduce((total, value) => `${total + value}\n`, '');
          this.commons.sendMessage(`<@${userId}>, There were too many results for **'${args.join(' ')}'**. Did you perhaps mean to pick any of the following?\n\n${cardList}`, userId, channelId);
          break;
        }
        case 'NON_MENTIONED': this.commons.sendMessage(`<@${userId}>, Please either specify a card name, or make sure to mention a card using an inline reference beforehand.`, userId, channelId); break;
        default: { console.error(err); this.commons.sendMessage(`<@${userId}>, An unknown error occurred.`, userId, channelId); break; }
      }
      return;
    }

    // Obtain pricing
    let pricing;
    try {
      pricing = await this.pricingUtils.getPricing(card.name, card.set);
    } catch (e) {
      switch (e.errType) {
        case 'DATABASE_ERROR': {
          this.commons.sendMessage(`<@${userId}>, An error occurred with my database. Please try again later.`, userId, channelId);
          return;
        }
        case 'ARTIFICIAL_RATE_LIMIT': {
          this.commons.sendMessage(`<@${userId}>, We have reached our own rate limit for retrieving card pricing. We do this in order to prevent abuse. If this happens quite often, please contact my developer to raise the limit.`, userId, channelId);
          return;
        }
        default: {
          console.log(e);
          this.commons.sendMessage(`<@${userId}>, An unknown error occurred. Please try again later.`, userId, channelId);
          return;
        }
      }
    }

    // Construct Message
    let message = `<@${userId}>, There is no pricing data available for **'${card.name}'** in set **'${card.setName}'**.`;
    if (pricing.length > 0) {
      message = `<@${userId}>, I found the following pricing data for **'${card.name}'** in set **'${card.setName}'**:`;
      pricing.forEach(price => {
        message += '\n\n';
        message += `**${price.storeName}**: `;
        message += `${price.pricings.map(priceType => `${priceType.key}: ${priceType.currency}${priceType.value}`).join('** | **')}\n`;
        message += `For more information visit ${price.storeUrl}\n`;
        message += `_(Last updated at ${moment(price.lastUpdated * 1000).utcOffset('+0000').format('YYYY-MM-DD HH:mm:ss')} UTC)_`;
      });
    }

    // Send message
    this.commons.sendMessage(message, userId, channelId);
  }

}
