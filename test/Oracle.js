// @flow

// Dependencies
import { describe, it } from 'mocha';
import { assert } from 'chai';

import Oracle from '../src/Command/All/Oracle';

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
    const oracle = new Oracle(commons);
    // Execute command
    oracle.exec(['test', 'card'], 'user id', 'channel id');
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
    const oracle = new Oracle(commons);
    // Execute command
    oracle.exec([], 'user id', 'channel id');
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
    const oracle = new Oracle(commons);
    // Execute command
    oracle.exec([], 'user id', 'channel id');
  });
  it('gives appropriate feedback when no oracle is available', (done) => {
    // Mock Commons
    const commons: any = {
      appState: ({}: any),
      mtg: ({}: any),
      config: ({}: any),
      sendFile: async () => ({}),
      sendMessage: async (msg: string, userId: string, channelId: string) => {
        assert.equal(msg, '<@user id>, There is no known oracle text for **\'TEST CARD\'**', 'Error was not what we expected');
        assert.equal(userId, 'user id', 'User ID was not what we expected');
        assert.equal(channelId, 'channel id', 'Channel ID was not what we expected');
        done();
      }
    };
    // Inject mock of obtainRecentOrSpecifiedCard
    commons.obtainRecentOrSpecifiedCard = async () => ({ originalText: '', name: 'TEST CARD' });
    // Initialize Prints
    const oracle = new Oracle(commons);
    // Execute command
    oracle.exec([], 'user id', 'channel id');
  });
  it('fetches all oracle text correctly', (done) => {
    // Mock Commons
    const commons: any = {
      appState: ({}: any),
      mtg: ({}: any),
      config: ({}: any),
      sendFile: async () => ({}),
      sendMessage: async (msg: string, userId: string, channelId: string) => {
        assert.equal(msg, '<@user id>, Here is the oracle text for **\'TEST CARD\'**:\n\nTest text', 'Message was not what we expected');
        assert.equal(userId, 'user id', 'User ID was not what we expected');
        assert.equal(channelId, 'channel id', 'Channel ID was not what we expected');
        done();
      }
    };
    // Inject mock of obtainRecentOrSpecifiedCard
    commons.obtainRecentOrSpecifiedCard = async () => ({ originalText: 'Test text',
      name: 'TEST CARD' });
    // Initialize Prints
    const oracle = new Oracle(commons);
    // Execute command
    oracle.exec([], 'user id', 'channel id');
  });
  // TODO: ALSO TEST EMOJI MAP
});
