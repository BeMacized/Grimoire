# Magister's Grimoire [![Build Status](https://travis-ci.org/BeMacized/Grimoire.svg?branch=master)](https://travis-ci.org/BeMacized/Grimoire)
"Magister's Grimoire" is a Discord Bot developed for the "Magic & Chill" Discord guild. It provides functionality surrounding the trading card game "Magic The Gathering" by Wizards of the Coast.

## Usage
The main use of this bot is pulling up Magic The Gathering card art. You can do this by surrounding card names with << >>.
For example: _<\<Shivan Dragon>> has been reprinted many times!_

You can too specify a specific set if you want to. You can do this by either supplying the set name, or a 3 letter set code.
For example: _<\<Shivan Dragon|Limited Edition Alpha>> is a very old card!_
Or alternatively: _<\<Shivan Dragon|LEA>> is a very old card!_

## Commands
The following commands are currently implemented:

Command | Parameters | Description
------------ | ------------- | -------------
**!help** | _None_ | Pull up the bot documentation
**!rulings** | _[cardname]_ | Pull up the rulings for a card. If no cardname is supplied, it will take the last card mentioned in the channel.
**!stats** | _None_ | Pull up some bot related statistics!
**!prints** | _[cardname]_ | Pull up all the sets a card has been printed in. If no cardname is supplied, it will take the last card mentioned in the channel.
**!oracle** | _[cardname]_ | Pull up the oracle (current) text of a specific card. If no cardname is supplied, it will take the last card mentioned in the channel.
**!pricing** | _[cardname]_ | Pull up pricing data for a specific card. If no cardname is supplied, it will take the last card mentioned in the channel.


The data for the **!pricing** command is graciously provided by [TCGPlayer](http://tcgplayer.com) and [MagicCardMarket.EU](http://magiccardmarket.eu)
