// @flow

// Dependencies
import { describe, it } from 'mocha';
import { assert } from 'chai';
import Commons from '../src/Utils/Commons';

describe('Commons', () => {
  describe('obtainRecentOrSpecifiedCard', () => {
    it('finds the most recent card if none is supplied', async () => {
      // Mock App State
      const appState : any = {
        lastMentioned: [
          {
            channelId: 'channel id',
            card: { cardObject: true }
          }
        ]
      };
      // Initialize commons
      const commons = new Commons(appState, ({}: any), ({}: any), ({}: any), async () => ({}), async () => ({}), () => ({}));
      // Invoke function
      const result = await commons.obtainRecentOrSpecifiedCard('', '', 'channel id');
      // Test
      assert.deepEqual(result, { cardObject: true }, 'The result is not what we expected');
    });

    it('gives appropriate feedback when no recent card is known and no name is provided', async () => {
      // Mock App State
      const appState : any = {
        lastMentioned: [
          {
            channelId: 'channel id',
            card: { cardObject: true }
          }
        ]
      };
      // Initialize commons
      const commons = new Commons(appState, ({}: any), ({}: any), ({}: any), async () => ({}), async () => ({}), () => ({}));
      // Test
      try {
        await commons.obtainRecentOrSpecifiedCard('', '', 'channel id2');
      } catch (e) {
        assert.deepEqual(e, { e: 'NON_MENTIONED' }, 'The error message we obtained was not the one we expected');
        return;
      }
      assert.fail();
    });

    it('looks up the card with the MTG API when a card name is provided', async (done) => {
      // Mock App State
      const appState : any = {
        lastMentioned: []
      };
      // Mock MTG SDK
      const mtg : any = {
        card: {
          where: (query: Object) => {
            assert.deepEqual(query, { name: 'card name' }, 'The query is not what we expected.');
            done();
            return [{ name: 'card name' }];
          }
        }
      };
      // Initialize commons
      const commons = new Commons(appState, mtg, ({}: any), ({}: any), async () => ({}), async () => ({}), () => ({}));
      // Test
      await commons.obtainRecentOrSpecifiedCard('card name', null, 'channel id');
    });

    it('gives appropriate feedback when the provided name does not yield results', async () => {
      // Mock App State
      const appState : any = {
        lastMentioned: []
      };
      // Mock MTG SDK
      const mtg : any = {
        card: {
          where: (query: Object) => {
            assert.deepEqual(query, { name: 'card name' }, 'The query is not what we expected.');
            return [];
          }
        }
      };
      // Initialize commons
      const commons = new Commons(appState, mtg, ({}: any), ({}: any), async () => ({}), async () => ({}), () => ({}));
      // Test
      try {
        await commons.obtainRecentOrSpecifiedCard('card name', null, 'channel id');
      } catch (e) {
        assert.deepEqual(e, { e: 'NO_RESULTS' }, 'The error message we obtained was not the one we expected');
        return;
      }
      assert.fail();
    });

    it('gives appropriate feedback when the provided name yields multiple results', async () => {
      // Mock App State
      const appState : any = {
        lastMentioned: []
      };
      // Mock MTG SDK
      const mtg : any = {
        card: {
          where: (query: Object) => {
            assert.deepEqual(query, { name: 'card name' }, 'The query is not what we expected.');
            return [{ name: 'card 1' }, { name: 'card 2' }];
          }
        }
      };
      // Initialize commons
      const commons = new Commons(appState, mtg, ({}: any), ({}: any), async () => ({}), async () => ({}), () => ({}));
      // Test
      try {
        await commons.obtainRecentOrSpecifiedCard('card name', null, 'channel id');
      } catch (e) {
        assert.deepEqual(e, { e: 'MANY_RESULTS', cards: [{ name: 'card 2' }, { name: 'card 1' }] }, 'The error message we obtained was not the one we expected');
        return;
      }
      assert.fail();
    });
  });
  // TODO: ADD TESTS FOR SPECIFIED SET
});
