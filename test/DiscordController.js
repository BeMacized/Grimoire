// @flow

// Dependencies
import { describe, it } from 'mocha';
import { assert } from 'chai';
import EventEmitter from 'events';
import DiscordController from '../src/DiscordController';

describe('DiscordController', () => {
  describe('Construction & Login', () => {
    it('constructs correctly', (done) => {
      let stages = 0;
    // Mock DJS
      const DJS = {
        Client: class Client extends EventEmitter {
          user: Object;
          mockClient: boolean = true;
          constructor() {
            super();
            setTimeout(() => this.emit('ready'), 50);
            this.user = {
              setAvatar: async (path: string) => {
                assert.equal(path, './src/Resources/Avatar.png', 'The given avatar file path was not what we expected.');
                stages++;
                return {};
              },
              setUsername: async (name: string) => {
                assert.equal(name, 'username', 'The given username was not what we expected');
                stages++;
                return {};
              },
              setGame: async (text: string) => {
                assert.equal(text, 'Magic: The Gathering', 'The given game text was not what we expected');
                stages++;
                return {};
              }
            };
          }
        }
      };
    // Queue final Check
      setTimeout(() => { assert.equal(stages, 4, 'Not enough stages activated for the result to be correct!'); done(); }, 150);
    // Instantiate controller
      const controller: DiscordController = new DiscordController(DJS, 'token', 'username');
      assert.equal(controller.botToken, 'token', 'Set token was not what we expected');
      assert.equal(controller.username, 'username', 'Set username was not what we expected');
      assert.isTrue(controller.client.mockClient, 'Set client was not the mock client we expected');
      assert.deepEqual(controller.discord, DJS, 'Set library reference was not what we expected');
      stages++;
    });

    it('logs in correctly', (done) => {
    // Mock DJS
      const DJS = {
        Client: class Client extends EventEmitter {
          user: Object;
          mockClient: boolean = true;
          constructor() {
            super();
            this.user = { setAvatar: async () => ({}), setUsername: async () => ({}) };
          }
          login(token: string) {
            assert.equal(token, 'token', 'Set token was not what we expected');
            done();
          }
      }
      };
    // Instantiate controller
      const controller: DiscordController = new DiscordController(DJS, 'token', 'username');
    // Assert we have a mock client
      assert.isTrue(controller.client.mockClient, 'Set client was not the mock client we expected');
    // Log in
      controller.login();
    });
  });

  describe('Passing through messages', () => {
    it('passes through non-bot messages', (done) => {
      // Mock DJS
      const DJS = {
        Client: class Client extends EventEmitter {
          user: Object;
          mockClient: boolean = true;
          constructor() {
            super();
            this.user = { setAvatar: async () => ({}), setUsername: async () => ({}) };
            // Emit mocked message
            setTimeout(() => {
              this.emit('message', {
                content: 'test content',
                author: { id: 'test user id', bot: false },
                guild: { id: 'test guild id' },
                channel: { id: 'test channel id' }
              });
            }, 50);
          }
        }
      };
      // Mock Chat processor
      const CP: any = {
        process: (message: string, userId: string, channelId: ?string, guildId: ?string) => {
          assert.equal(message, 'test content', 'The given message content was not what we expected');
          assert.equal(userId, 'test user id', 'The given author id was not what we expected');
          assert.equal(channelId, 'test channel id', 'The given channel id was not what we expected');
          assert.equal(guildId, 'test guild id', 'The given guild id was not what we expected');
          done();
        },
        mtg: {},
        config: {},
        chatTools: {
          sendFile: async () => ({}),
          sendMessage: async () => ({})
        },
        showInlineCard: async () => {}
      };
      // Instantiate controller
      const controller: DiscordController = new DiscordController(DJS, 'token', 'username');
      // Assert we have a mock client
      assert.isTrue(controller.client.mockClient, 'Set client was not the mock client we expected');
      // Inject mock chat processor
      controller.chatProcessor = CP;
    });
    it('discards bot messages', (done) => {
      let error = false;
      // Mock DJS
      const DJS = {
        Client: class Client extends EventEmitter {
          user: Object;
          mockClient: boolean = true;
          constructor() {
            super();
            this.user = { setAvatar: async () => ({}), setUsername: async () => ({}) };
            // Emit mocked message
            setTimeout(() => {
              this.emit('message', {
                content: 'test content',
                author: { id: 'test user id', bot: true },
                guild: { id: 'test guild id' },
                channel: { id: 'test channel id' }
              });
            }, 50);
          }
        }
      };
      // Mock Chat processor
      const CP: any = {
        process: () => { error = true; },
        mtg: {},
        config: {},
        chatTools: {
          sendFile: async () => ({}),
          sendMessage: async () => ({})
        },
        showInlineCard: async () => {}
      };
      // Instantiate controller
      const controller: DiscordController = new DiscordController(DJS, 'token', 'username');
      // Assert we have a mock client
      assert.isTrue(controller.client.mockClient, 'Set client was not the mock client we expected');
      // Inject mock chat processor
      controller.chatProcessor = CP;
      // Set timeout for final Check
      setTimeout(() => { assert.isFalse(error, 'The message was passed through when it was not supposed to'); done(); }, 100);
    });
  });
});
