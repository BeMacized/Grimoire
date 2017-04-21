// @flow
const webpack = require('webpack');
const mainConfig = require('./webpack.config.js');

const productionEnvPlugins = [new webpack.DefinePlugin({
  'process.env': {
    NODE_ENV: JSON.stringify('production')
  }
}),
new webpack.optimize.CommonsChunkPlugin('common.js'),
new webpack.optimize.DedupePlugin(),
new webpack.optimize.UglifyJsPlugin(),
new webpack.optimize.AggressiveMergingPlugin()];

module.exports = [Object.assign({}, mainConfig, { plugins: productionEnvPlugins })];
