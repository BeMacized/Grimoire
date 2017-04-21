// @flow

// Dependencies
import { describe, it } from 'mocha';
import { assert } from 'chai';
import ChatProcessor from '../src/ChatProcessor';
import { defaultConfig } from '../src/Utils/Config';

describe('ChatProcessor', () => {
  describe('Card reference extraction', () => {
    it('properly extracts an inline card name.', (done) => {
      const extractedNames: Array<string> = [];
      // Instantiate chatProcessor
      const commons: any = { sendFile: async () => ({}), sendMessage: async () => ({}), config: defaultConfig, mtg: {}, appState: ({ setLastMentioned: () => {} }: any) };
      const chatProcessor: ChatProcessor = new ChatProcessor(commons, ({ processMessage: () => {} }: any));
      // Mock the showInlineCard function
      chatProcessor.showInlineCard = async (cardName) => { extractedNames.push(cardName); return {}; };
      // Make the call
      chatProcessor.process('I like playing <<Negate>>!', '0', '0', '0');
      // Check output
      assert.deepEqual(extractedNames, ['Negate'], 'Unexpected result from cardname extraction!');
      done();
    });

    it('properly extracts multiple inline card names.', (done) => {
      const extractedNames: Array<string> = [];
      // Instantiate chatProcessor
      const commons: any = { sendFile: async () => ({}), sendMessage: async () => ({}), config: defaultConfig, mtg: {}, appState: ({ setLastMentioned: () => {} }: any) };
      const chatProcessor: ChatProcessor = new ChatProcessor(commons, ({ processMessage: () => {} }: any));
      // Mock the showInlineCard function
      chatProcessor.showInlineCard = async (cardName) => { extractedNames.push(cardName); return {}; };
      // Make the call
      chatProcessor.process('I like playing <<Negate>>, but <<Choking Restraints>> as well! <<Deadlock Trap>> is pretty sweet too.', '0', '0', '0');
      // Check output
      assert.deepEqual(extractedNames, ['Negate', 'Choking Restraints', 'Deadlock Trap'], 'Unexpected result from cardname extraction!');
      done();
    });
  });

  describe('Logging', () => {
    it('sets the last card as the last mentioned card for the channel', (done) => {
      // Mock Commons, App State & MTG SDK
      const commons: any = {
        sendFile: async () => ({}),
        sendMessage: async () => {
          done('Unexpected call to sendMessage method');
          return null;
        },
        config: defaultConfig,
        mtg: {
          card: {
            where: async (): Promise<Array<Object>> => [{ name: 'example card', imageUrl: 'example url', setName: 'example set', set: 'set id', id: 'example id' }]
          }
        },
        appState: ({ setLastMentioned: (channelId: string, card: Object) => {
          assert.equal(channelId, 'channel id', 'Given channel id is not the one we expected');
          assert.equal(card.id, 'example id', 'Given card id is not the one we expected');
          assert.equal(card.set, 'set id', 'Given set id is not the one we expected');
          done();
        } }: any)
      };
      // Instantiate chatProcessor
      const chatProcessor: ChatProcessor = new ChatProcessor(commons, ({ processMessage: () => {} }: any));
      chatProcessor.process('<<example card|set id>>', 'user id', 'channel id', 'guild id');
    });
  });

  describe('Library interaction', () => {
    it('attempts uploading card art for inline cards', (done) => {
      // Mock MTG SDK & Commons
      const commons: any = {
        sendFile: async (imageUrl) => {
          assert.equal(imageUrl, 'example url', 'Attempted upload for unknown image url');
          done();
          return {};
        },
        sendMessage: async () => {
          done('Unexpected call to sendMessage method');
          return null;
        },
        config: defaultConfig,
        mtg: {
          card: {
            where: async (query: Object): Promise<Array<Object>> => {
              assert.deepEqual(query, { name: 'example card name' }, 'Executed query did not match expectations.');
              return [{ name: 'example card', imageUrl: 'example url' }];
            }
          }
        },
        appState: ({ setLastMentioned: () => {} }: any)
      };
      // Instantiate chatProcessor
      const chatProcessor: ChatProcessor = new ChatProcessor(commons, ({ processMessage: () => {} }: any));
      chatProcessor.showInlineCard('example card name', '0', '0', '0');
    });

    it('includes the specified set code in its queries for inline card names', (done) => {
      // Mock MTG SDK & Commons
      const commons: any = {
        sendFile: async () => ({}),
        sendMessage: async () => ({}),
        config: defaultConfig,
        mtg: {
          card: {
            where: async (query: Object): Promise<Array<Object>> => {
              assert.deepEqual(query, { name: 'example card name', set: 'SET' }, 'Executed query did not match expectations.');
              done();
              return [];
            }
          }
        },
        appState: ({ setLastMentioned: () => {} }: any)
      };
      // Instantiate chatProcessor
      const chatProcessor: ChatProcessor = new ChatProcessor(commons, ({ processMessage: () => {} }: any));
      chatProcessor.showInlineCard('example card name', '0', '0', '0', 'SET');
    });
  });

  describe('Feedback to invalid input', () => {
    it('shows the correct feedback when there are no results', (done) => {
      // Mock MTG SDK & Commons
      const commons: any = {
        sendFile: async () => {
          done('Unexpected call to sendFile method');
          return {};
        },
        sendMessage: async (message) => {
          assert.equal(message, "<@0>, I could not find any results for **'example card name'**!", 'Received unexpected feedback!');
          done();
          return null;
        },
        config: defaultConfig,
        mtg: {
          card: {
            where: async (query: Object): Promise<Array<Object>> => {
              assert.deepEqual(query, { name: 'example card name' }, 'Executed query did not match expectations.');
              return [];
            }
          }
        },
        appState: ({ setLastMentioned: () => {} }: any)
      };
      // Instantiate chatProcessor
      const chatProcessor: ChatProcessor = new ChatProcessor(commons, ({ processMessage: () => {} }: any));
      chatProcessor.showInlineCard('example card name', '0', '0', '0');
    });

    it('shows the correct feedback when the search failed', (done) => {
      // Mock MTG SDK & Commons
      const commons: any = {
        sendFile: async () => {
          done('Unexpected call to sendFile method');
          return {};
        },
        sendMessage: async (message) => {
          assert.match(message, /^<@0>, I ran into some problems when trying to retrieve data for \*\*'example card name'\*\*.*/, 'Received unexpected feedback!');
          done();
          return null;
        },
        config: defaultConfig,
        mtg: {
          card: {
            where: async (query: Object): Promise<Array<Object>> => {
              assert.deepEqual(query, { name: 'example card name' }, 'Executed query did not match expectations.');
              throw Error('Some Random Error');
            }
          }
        },
        appState: ({ setLastMentioned: () => {} }: any)
      };
      // Instantiate chatProcessor
      const chatProcessor: ChatProcessor = new ChatProcessor(commons, ({ processMessage: () => {} }: any));
      chatProcessor.showInlineCard('example card name', '0', '0', '0');
    });

    it('shows the correct feedback when there are multiple results', (done) => {
      // Mock MTG SDK & Commons
      const commons: any = {
        sendFile: async () => {
          done('Unexpected call to sendFile method');
          return {};
        },
        sendMessage: async (message) => {
          assert.equal(message, '<@0>, There were too many results for **\'example card name\'**. Did you perhaps mean to pick any of the following?\n\n - example card 2\n - example card 1\n', 'Received unexpected feedback!');
          done();
          return null;
        },
        mtg: {
          card: {
            where: async (query: Object): Promise<Array<Object>> => {
              assert.deepEqual(query, { name: 'example card name' }, 'Executed query did not match expectations.');
              return [{ name: 'example card 1' }, { name: 'example card 2' }];
            }
          }
        },
        config: defaultConfig,
        appState: ({ setLastMentioned: () => {} }: any)
      };

      // Instantiate chatProcessor
      const chatProcessor: ChatProcessor = new ChatProcessor(commons, ({ processMessage: () => {} }: any));
      chatProcessor.showInlineCard('example card name', '0', '0', '0');
    });

    it('shows the correct feedback when no card image is available', (done) => {
      // Mock MTG SDK & Commons
      const commons: any = {
        sendFile: async () => {
          done('Unexpected call to sendFile method');
          return {};
        },
        sendMessage: async (message) => {
          assert.equal(message, '<@0>, There is no card image available for **\'example card name\'**!', 'Received unexpected feedback!');
          done();
          return null;
        },
        mtg: {
          card: {
            where: async (query: Object): Promise<Array<Object>> => {
              assert.deepEqual(query, { name: 'example card name' }, 'Executed query did not match expectations.');
              return [{ name: 'example card' }];
            }
          }
        },
        config: defaultConfig,
        appState: ({ setLastMentioned: () => {} }: any)
      };
      // Instantiate chatProcessor
      const chatProcessor: ChatProcessor = new ChatProcessor(commons, ({ processMessage: () => {} }: any));
      chatProcessor.showInlineCard('example card name', '0', '0', '0');
    });
  });

  describe('Card reference limits', () => {
    it('allows references in fewer or equal amount to the threshold', (done) => {
      let error;
      // Mock MTG SDK & Commons
      const commons: any = {
        sendFile: async () => ({}),
        sendMessage: async (message) => {
          try {
            assert.notMatch(message, /<@0>, It is not permitted to use more than [0-9]+ card references per message\./);
          } catch (e) {
            error = 'ChatProcessor did not permit given amount of card references.';
            throw e;
          }
        },
        mtg: {},
        config: defaultConfig,
        appState: ({ setLastMentioned: () => {} }: any)
      };
      // Instantiate chatProcessor
      const chatProcessor: ChatProcessor = new ChatProcessor(commons, ({ processMessage: () => {} }: any));

      // Process references
      let references = '';
      for (let i = 1; i < defaultConfig.maxInlineCardReferences; i++) references += '<<card>>';
      chatProcessor.process(references, '0', '0', '0');
      chatProcessor.process(`${references}<<card>>`, '0', '0', '0');
      setTimeout(() => done(error), 100);
    });

    it('declines more references than the threshold', (done) => {
      // Mock MTG SDK & Commons
      const commons: any = {
        sendFile: async () => ({}),
        sendMessage: async (message) => {
          assert.match(message, /<@0>, It is not permitted to use more than [0-9]+ card references per message\./);
          done();
        },
        mtg: {},
        config: defaultConfig,
        appState: ({ setLastMentioned: () => {} }: any)
      };
      // Instantiate chatProcessor
      const chatProcessor: ChatProcessor = new ChatProcessor(commons, ({ processMessage: () => {} }: any));

      // Process references
      let references = '';
      for (let i = 0; i <= defaultConfig.maxInlineCardReferences; i++) references += '<<card>>';
      chatProcessor.process(references, '0', '0', '0');
    });
  });
});
