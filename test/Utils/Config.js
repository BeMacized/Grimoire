// @flow

// Dependencies
import { describe, it } from 'mocha';
import { assert } from 'chai';
import Config, { defaultConfig } from '../../src/Utils/Config';

describe('Config', () => {
  it('attempts generating a default config when none exists', (done) => {
    // Mock FS-Promise
    const fsp = {
      existsSync: (path) => {
        assert.equal(path, './config.json', 'The path given was not the one we expected.');
        return false;
      },
      writeFile: async (path: string, text: string) => {
        assert.equal(path, './config.json', 'The path given was not the one we expected.');
        assert.equal(text, JSON.stringify(defaultConfig, null, 2), 'The text to be written was not the one we expected.');
        done();
      },
      readFile: async (path: string, encoding: string) => {
        assert.equal(path, './config.json', 'The path given was not the one we expected.');
        assert.equal(encoding, 'utf8', 'The encoding given was not the one we expected.');
        return '{ "notEmpty": true }';
      }
    };
    // Create new config instance
    const config = new Config('./config.json', fsp);
    // Initialize it
    config.initialize();
  });
});
