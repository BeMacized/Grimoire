// @flow

// Dependencies
import { describe, it } from 'mocha';
import { assert } from 'chai';
import BaseCommand from '../../src/Command/BaseCommand';

describe('BaseCommand', () => {
  it('does not allow instantiation with an empty name', (done) => {
    assert.throws(() => {
      new BaseCommand(({}: any), '', 'usage', 'description'); // eslint-disable-line no-new
    }, 'Command name cannot be empty');
    done();
  });
});
