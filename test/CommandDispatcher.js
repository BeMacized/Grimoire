// @flow

// Dependencies
import { describe, it } from 'mocha';
import { assert } from 'chai';
import CommandDispatcher from '../src/Command/CommandDispatcher';

describe('CommandDispatcher', () => {
  it('returns false when supplying no command', (done) => {
    // Instantiate command dispatcher
    const dispatcher = new CommandDispatcher(({}: any));
    // Dispatch invalid command
    const result = dispatcher.processMessage('!', 'user id', 'channel id', 'guild id');
    // Test
    assert.isFalse(result, 'A different result came back than expected');
    done();
  });

  it('returns false when supplying an unknown', (done) => {
    // Instantiate command dispatcher
    const dispatcher = new CommandDispatcher(({}: any));
    // Remove all registered commands
    dispatcher.commands = [];
    // Dispatch unknown command
    const result = dispatcher.processMessage('!cmd', 'user id', 'channel id', 'guild id');
    // Test
    assert.isFalse(result, 'A different result came back than expected');
    done();
  });

  it('It executes a known command with arguments (and returns true)', (done) => {
    let executed = false;
    // Instantiate command dispatcher
    const dispatcher = new CommandDispatcher(({}: any));
    // Replace all registered commands
    dispatcher.commands = ([{
      name: 'cmd',
      aliases: [],
      exec: (args: Array<string>, userId: string, channelId?: ?string, guildId?: ?string) => {
        assert.deepEqual(args, ['arg1', 'arg2'], 'The given arguments were not the ones we expected');
        assert.equal(userId, 'user id', 'The given user ID was not the one we expected');
        assert.equal(channelId, 'channel id', 'The given channel ID was not the one we expected');
        assert.equal(guildId, 'guild id', 'The given guild ID was not the one we expected');
        executed = true;
      }
    }]: any);
    // Dispatch unknown command
    const result = dispatcher.processMessage('!cmd arg1 arg2', 'user id', 'channel id', 'guild id');
    // Test
    assert.isTrue(result, 'A different result came back than expected');
    assert.isTrue(executed, 'The command was not executed correctly');
    done();
  });
});
