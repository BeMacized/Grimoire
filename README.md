# Mac's Grimoire
"Mac's Grimoire" is a Discord Bot developed for the "Magic & Chill" Discord guild. It provides functionality surrounding the trading card game "Magic The Gathering" by Wizards of the Coast.

## Usage
The main use of this bot is pulling up Magic The Gathering card art. You can do this by surrounding card names with << >>.
For example: _<\<Shivan Dragon>> has been reprinted many times!_

You can too specify a specific set if you want to. You can do this by supplying a 3 letter set code.
For example: _<\<Shivan Dragon|LEA>> is a very old card!_

## Commands
The following commands are currently implemented:

Command | Parameters | Description
------------ | ------------- | -------------
**!help** | _None_ | Shows the help text, containing all of the command references.
**!rulings** | _[cardname]_ | Retrieves the current rulings of the specified card. Lists for the last shown card if no card name supplied.
**!prints** | _[cardname]_ | Retrieves all sets that a card was printed in. Lists for the last shown card if no card name supplied.
**!oracle** | _[cardname]_ | Retrieves the oracle text of a card. Lists for the last shown card if no card name supplied.
**!pricing** | _[cardname]_ | Retrieves the current pricing for a card. Lists for the last shown card if no card name supplied.

The data for the **!pricing** command is graciously provided by [TCGPlayer](http://tcgplayer.com) and [MagicCardMarket.EU](http://magiccardmarket.eu)

## Config
A config file will be automatically generated if none is present. It's located in the working directory with the name `config.json`.

It contains the following fields:

```javascript
{
  "maxInlineCardReferences": 6, // The maximum amount of inline references per message
  "botToken": "", // The Discord bot token
  "botUsername": "Mac's Grimoire", // The username for the bot
  "mcmApiHost": "sandbox.mkmapi.eu", // The API hostname for the MagicCardMarket API
  "mcmToken": "", // The MagicCardMarket Application Token
  "mcmSecret": "", // The MagicCardMarket Application Secret
  "tcgApiHost": "partner.tcgplayer.com", // The hostname for the TCGPlayer API
  "tcgKey": "", // The key to use for the TCGPlayer API
  "mongoURL": "mongodb://User:Password@Host:Port/Database" // The connection details for the mongo database.
}
```
