
[![Grimoire Logo](https://i.imgur.com/KlhoTsz.png)](https://grimoirebot.xyz/)

[![](https://images.microbadger.com/badges/version/bemacized/grimoire.svg)](https://microbadger.com/images/bemacized/grimoire)

**Mac's Grimoire** is a Discord bot that brings many **Magic The Gathering** related tools straight into your discord server. It can perform tasks such as card, price and rule lookups to name a few.

Supported features include, but are not limited to:

* Card Fetching
* Price Fetching (MagicCardMarket, TCGPlayer & MTGGoldfish)
* Rule Fetching (Comprehensive- & Tournament Rules)
* Token Fetching
* Infraction Procedures
* Keyword definition lookups
* Currency Conversion
* Non-English Card Support
* Scryfall Syntax Support
* More on the [Website](https://grimoirebot.xyz)

Invite the bot to your Guild via this link: [Invite Grimoire](https://grimoirebot.xyz/invite)

You can join our support server via here: [Support Server](https://grimoirebot.xyz/support)

### Preference Dashboard
You can change your Guild preferences over at the [Dashboard](https://grimoirebot.xyz/dashboard). Much of the bots functionality can be tailored to your preferences via this panel.
![Dashboard Screenshot](https://grimoirebot.xyz/img/screenshots/Dashboard.png)

### Command Reference

#### Inline references

You can use inline shortcuts to quickly reference multiple cards within your message. You are limited to a max of 3 inline references per message.

|Shortcut|Command|Examples|
|:--- |:--- |:--- |
|`<<query>>`|`g!card`|`<<Mighty Leap>>`|
|`<<query \| set>>`|-|`<<Mighty Leap \| ORI>>`|
|`[[query]]`|-|`[[Mighty Leap \| Magic Origins]]`|
||||
|`<<$query>>`|`g!pricing`|`<<$Mighty Leap>>`|
|`<<$query \| set>>`|-|`<<$Mighty Leap \| ORI>>`|
|`[[$query]]`|-|`[[$Mighty Leap \| Magic Origins]]`|



#### Commands

All commands are prefixed using `g!` by default. You can change this behaviour via the Dashboard. For more in-depth information, you can visit the [Command Reference](https://grimoirebot.xyz/reference) on the website! Alternatively, you can browse the commands via the bot by sending "commands" in a private message.

|Command|Description|Aliases|Examples|
|:--- |:--- |:--- |:--- |
|`g!art <query\|set>`|Fetch the full art of a card. This command supports the entire [Scryfall Syntax](https://scryfall.com/docs/reference) for the query parameter.|`cardart`|`g!art Mighty Leap \| ORI`|
|`g!art <query>`|-|-|`g!art Mighty Leap \| Magic Origins`|
|-|-|-|`g!art Mighty Leap`|
||||
|`g!card <query\|set>`|Fetch information for a card This command supports the entire [Scryfall Syntax](https://scryfall.com/docs/reference) for the query parameter.|`c`|`g!card Mighty Leap \| ORI`|
|`g!card <query>`|-|-|`g!card Mighty Leap \| Magic Origins`|
|-|-|-|`g!card Mighty Leap`|
||||
|`g!comprules <paragraph nr>`|Retrieve a paragraph from the comprehensive rules|`crules` `comprehensiverules` `cr`|`g!comprules 702`|
|-|-|-|`g!comprules 702.5c`|
|-|-|-|`g!comprules 702.5`|
|-|-|-|`g!comprules 7`|
|-|-|-|`g!comprules`|
||||
|`g!define <keyword>`|Looks up the definition for the specified keyword|`keyword` `definition`|`g!define vigilance`|
|-|-|-|`g!define prowess`|
|-|-|-|`g!define enchant`|
||||
|`g!flavor <query\|set>`|Retrieves the flavor text of a card. This command supports the entire [Scryfall Syntax](https://scryfall.com/docs/reference) for the query parameter.|`flavortext`|`g!flavor Mighty Leap \| ORI`|
|`g!flavor <query>`|-|-|`g!flavor Mighty Leap \| Magic Origins`|
|-|-|-|`g!flavor Mighty Leap`|
||||
|`g!help`|Shows the help text, containing all of the command references.|-|-|
||||
|`g!infractionprocedure <paragraph> [topic]`|Retrieve a paragraph from the tournament rules.|`ipguide` `ipg`|`g!infractionprocedure 2.5`|
|-|-|-|`g!infractionprocedure 2.5 philosophy`|
|-|-|-|`g!infractionprocedure 2`|
|-|-|-|`g!infractionprocedure`|
||||
|`g!legality <query\|set>`|Checks the legality of a card, for every known format This command supports the entire [Scryfall Syntax](https://scryfall.com/docs/reference) for the query parameter.|`format` `legalities` `formats` `illegal` `legal`|`g!legality Mighty Leap \| ORI`|
|`g!legality <query>`|-|-|`g!legality Mighty Leap \| Magic Origins`|
|-|-|-|`g!legality Mighty Leap`|
||||
|`g!names <query\|set>`|Retrieves all known foreign names for a card. This command supports the entire [Scryfall Syntax](https://scryfall.com/docs/reference) for the query parameter.|`foreign` `named` `abroad`|`g!names Mighty Leap \| ORI`|
|`g!names <query>`|-|-|`g!names Mighty Leap \| Magic Origins`|
|-|-|-|`g!names Mighty Leap`|
||||
|`g!oracle <query\|set>`|Retrieves the oracle text of a card. This command supports the entire [Scryfall Syntax](https://scryfall.com/docs/reference) for the query parameter.|`cardtext`|`g!oracle Mighty Leap \| ORI`|
|`g!oracle <query>`|-|-|`g!oracle Mighty Leap \| Magic Origins`|
|-|-|-|`g!oracle Mighty Leap`|
||||
|`g!pricing <query\|set>`|Retrieves the current pricing for a card. This command supports the entire [Scryfall Syntax](https://scryfall.com/docs/reference) for the query parameter.|`dollarydoos` `price`|`g!pricing Mighty Leap \| ORI`|
|`g!pricing <query>`|-|-|`g!pricing Mighty Leap \| Magic Origins`|
|-|-|-|`g!pricing Mighty Leap`|
||||
|`g!prints <query\|set>`|Retrieves all sets that a card was printed in. This command supports the entire [Scryfall Syntax](https://scryfall.com/docs/reference) for the query parameter.|`versions` `printings` `sets`|`g!prints Mighty Leap \| ORI`|
|`g!prints <query>`|-|-|`g!prints Mighty Leap`|
|-|-|-|`g!prints Mighty Leap \| Magic Origins`|
||||
|`g!random [supertype] [type] [subtype] [rarity] [set] [setcode]`|Show a random card of a certain type.|`rng` `rand`|`g!random C17 mythic`|
|-|-|-|`g!random rare artifact`|
|-|-|-|`g!random legendary creature`|
|-|-|-|`g!random`|
||||
|`g!reloadpreferences`|Reload preferences immediately for your guild|`reloadprefs`|
||||
|`g!rulings <query\|set>`|Retrieves the current rulings of the specified card. This command supports the entire [Scryfall Syntax](https://scryfall.com/docs/reference) for the query parameter.|`ruling` `rules`|`g!rulings Mighty Leap \| ORI`|
|`g!rulings <query>`|-|-|`g!rulings Mighty Leap \| Magic Origins`|
|-|-|-|`g!rulings Mighty Leap`|
||||
|`g!set <set>`|Fetch information for a set|`s`|`g!set Magic Origins`|
|-|-|-|`g!set ORI`|
||||
|`g!standard`|See what sets are currently in standard rotation|`wis` `whatsinstandard`|-|
||||
|`g!statistics`|View statistics for Grimoire.|`stats`|-|
||||
|`g!token <token_name> [choice]`|Retrieve the art of a token.|-|`g!token angel 3`|
|-|-|-|`g!token angel`|
||||
|`g!tournamentrules <paragraph nr>`|Retrieve a paragraph from the tournament rules|`mtr` `tr` `magictournamentrules`|`g!tournamentrules`|
|-|-|-|`g!tournamentrules 3`|
|-|-|-|`g!tournamentrules 3.10`|


### Selfhosting
Instructions for hosting this bot by yourself are coming soon!

### Magic & Chill
In case you are interested, feel free to come join our partner server, [Discord & Chill](https://discord.gg/vqsFzgJ). We talk about Magic all day and love new people joining in!
### Disclaimer

The literal and graphical information presented in this repository, and by the Discord bot about Magic: The Gathering, including card images, the mana symbols, and Oracle text, is copyright Wizards of the Coast, LLC, a subsidiary of Hasbro, Inc. This project is not produced by, endorsed by, supported by, or affiliated with Wizards of the Coast.

Card prices represent daily averages and/or market values provided by our affiliates. Absolutely no guarantee is made for any price information. See stores for final prices and availability.

"Discord" and any associated logos are registered trademarks of Discord, Inc. Grimoire is not created by, affiliated with, or supported by Discord.

The Scryfall logo and Scryfall API are copyright Some Assembly, LLC, DBA Scryfall. Grimoire is not created by, affiliated with, or supported by Scryfall.

