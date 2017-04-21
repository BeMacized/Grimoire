// @flow

// Dependencies
import { describe, it } from 'mocha';
import { assert } from 'chai';

import Prints from '../src/Command/All/Prints';

describe('Command: Prints', () => {
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
    const prints = new Prints(commons);
    // Execute command
    prints.exec(['test', 'card'], 'user id', 'channel id');
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
    const prints = new Prints(commons);
    // Execute command
    prints.exec([], 'user id', 'channel id');
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
    const prints = new Prints(commons);
    // Execute command
    prints.exec([], 'user id', 'channel id');
  });
  it('fetches all required set names', (done) => {
    const requestedCodes: Array<string> = [];
    // Mock Commons
    const commons: any = {
      appState: ({}: any),
      mtg: {
        set: {
          find: async (setCode: string) => {
            requestedCodes.push(setCode);
            if (setCode === 'SET1') return { set: { name: 'SET 1' } };
            if (setCode === 'SET2') return { set: { name: 'SET 2' } };
            assert.fail();
            return {};
          }
        }
      },
      config: ({}: any),
      sendFile: async () => ({}),
      sendMessage: async (msg: string, userId: string, channelId: string) => {
        assert.deepEqual(requestedCodes, ['SET1', 'SET2'], 'Requested codes were not the ones we expected');
        assert.equal(msg, 'The card **\'TEST CARD\'** was printed in the following sets:\n\n - SET 1 (**SET1**)\n - SET 2 (**SET2**)', 'Error was not what we expected');
        assert.equal(userId, 'user id', 'User ID was not what we expected');
        assert.equal(channelId, 'channel id', 'Channel ID was not what we expected');
        done();
      }
    };
    // Inject mock of obtainRecentOrSpecifiedCard
    commons.obtainRecentOrSpecifiedCard = async () => ({ printings: ['SET1', 'SET2'], name: 'TEST CARD' });
    // Initialize Prints
    const prints = new Prints(commons);
    // Execute command
    prints.exec([], 'user id', 'channel id');
  });
});
