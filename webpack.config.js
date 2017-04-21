'use strict';

//Dependencies
var webpack = require('webpack');
var path = require('path');
var fs = require('fs');

//Build externals flag for excluding node_modules
var nodeModules = {};
fs.readdirSync('node_modules').filter(function(x) {
    return ['.bin'].indexOf(x) === -1;
}).forEach(function(mod) {
    nodeModules[mod] = 'commonjs ' + mod;
});

module.exports = {
    name: 'My Application',
    entry: [
        'babel-polyfill', './src/Grimoire.js'
    ],
    output: {
        path: path.join(__dirname, 'dist'),
        filename: 'Grimoire.js'
    },
    plugins: [
        new webpack.BannerPlugin('require("source-map-support").install();', {
            raw: true,
            entryOnly: false
        }),
        new webpack.DefinePlugin({
            'process.env': {
                'NODE_ENV': JSON.stringify("development")
            }
        })
    ],
    target: 'node',
    node: {
        __dirname: false,
        __filename: false
    },
    devtool: 'sourcemap',
    module: {
        loaders: [
          {
            test: /\.js?$/,
            exclude: /node_modules/,
            loaders: ["babel-loader"]
          }, {
            test: /.*$/,
            exclude: /node_modules/,
            loader: 'file'
          }
        ]
    },
    externals: nodeModules
};
