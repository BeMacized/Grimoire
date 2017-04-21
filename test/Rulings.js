// @flow

// Dependencies
import { describe, it } from 'mocha';
import { assert } from 'chai';

import Rulings from '../src/Command/All/Rulings';

describe('Command: Rulings', () => {
  it('attempts retrieving a card based on arguments', (done) => {
    // Mock Commons
    const commons: any = { appState: ({}: any), mtg: ({}: any), config: ({}: any), sendFile: async () => ({}), sendMessage: async () => ({}) };
    // Inject mock of obtainRecentOrSpecifiedCard
    commons.obtainRecentOrSpecifiedCard = async (name: string, channelId: string) => {
      assert.equal(name, 'test card', 'Card name was not what we expected.');
      assert.equal(channelId, 'channel id', 'Channel ID was not what we expected');
      done();
      return { printings: [], name: 'card name' };
    };
    // Initialize Prints
    const rulings = new Rulings(commons);
    // Execute command
    rulings.exec(['test', 'card'], 'user id', 'channel id');
  });
  it('attempts retrieving a card without arguments', (done) => {
    // Mock Commons
    const commons: any = { appState: ({}: any), mtg: ({}: any), config: ({}: any), sendFile: async () => ({}), sendMessage: async () => ({}) };
    // Inject mock of obtainRecentOrSpecifiedCard
    commons.obtainRecentOrSpecifiedCard = async (name: string, channelId: string) => {
      assert.equal(name, '', 'Card name was not what we expected.');
      assert.equal(channelId, 'channel id', 'Channel ID was not what we expected');
      done();
      return { printings: [], name: 'card name' };
    };
    // Initialize Prints
    const rulings = new Rulings(commons);
    // Execute command
    rulings.exec([], 'user id', 'channel id');
  });
  it('relays any errors that occur during fetching', (done) => {
    // Mock Commons
    const commons: any = {
      appState: ({}: any),
      mtg: ({}: any),
      config: ({}: any),
      sendFile: async () => ({}),
      sendMessage: async (msg: string, userId: string, channelId: string) => {
        assert.equal(msg, '<@user id>, TEST', 'Error was not what we expected');
        assert.equal(userId, 'user id', 'User ID was not what we expected');
        assert.equal(channelId, 'channel id', 'Channel ID was not what we expected');
        done();
      }
    };
    // Inject mock of obtainRecentOrSpecifiedCard
    commons.obtainRecentOrSpecifiedCard = async () => {
      throw 'TEST';
    };
    // Initialize Prints
    const rulings = new Rulings(commons);
    // Execute command
    rulings.exec([], 'user id', 'channel id');
  });
  it('gives appropriate feedback when no rulings are available', (done) => {
    // Mock Commons
    const commons: any = {
      appState: ({}: any),
      mtg: ({}: any),
      config: ({}: any),
      sendFile: async () => ({}),
      sendMessage: async (msg: string, userId: string, channelId: string) => {
        assert.equal(msg, '<@user id>, There are no known rulings for **\'TEST CARD\'**', 'Error was not what we expected');
        assert.equal(userId, 'user id', 'User ID was not what we expected');
        assert.equal(channelId, 'channel id', 'Channel ID was not what we expected');
        done();
      }
    };
    // Inject mock of obtainRecentOrSpecifiedCard
    commons.obtainRecentOrSpecifiedCard = async () => ({ rulings: [], name: 'TEST CARD' });
    // Initialize Prints
    const rulings = new Rulings(commons);
    // Execute command
    rulings.exec([], 'user id', 'channel id');
  });
  it('fetches all rulings correctly', (done) => {
    // Mock Commons
    const commons: any = {
      appState: ({}: any),
      mtg: ({}: any),
      config: ({}: any),
      sendFile: async () => ({}),
      sendMessage: async (msg: string, userId: string, channelId: string) => {
        assert.equal(msg, 'The following ruling(s) were released for **\'TEST CARD\'**:\n\n**date1**:\n - ruling1\n - ruling2\n\n**date2**:\n - ruling3', 'Error was not what we expected');
        assert.equal(userId, 'user id', 'User ID was not what we expected');
        assert.equal(channelId, 'channel id', 'Channel ID was not what we expected');
        done();
      }
    };
    // Inject mock of obtainRecentOrSpecifiedCard
    commons.obtainRecentOrSpecifiedCard = async () => ({ rulings: [
      { date: 'date1', text: 'ruling1' },
      { date: 'date1', text: 'ruling2' },
      { date: 'date2', text: 'ruling3' },
    ],
      name: 'TEST CARD' });
    // Initialize Prints
    const rulings = new Rulings(commons);
    // Execute command
    rulings.exec([], 'user id', 'channel id');
  });
});
