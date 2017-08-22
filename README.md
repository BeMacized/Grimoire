
[![Grimoire Logo](https://img.bemacized.net/Heo6WJMr08yK5d74bmOTeMtLwhZDHXfb.png)](https://grimoire.bemacized.net/)

[![](https://images.microbadger.com/badges/version/bemacized/grimoire.svg)](https://microbadger.com/images/bemacized/grimoire)

**Mac's Grimoire** is a Discord bot that brings many **Magic The Gathering** related tools straight into your discord server. It can perform tasks such as card-, price- or rule lookups, and even more.

Supported features include, but are not limited to:

* Card Fetching
* Price Fetching (MagicCardMarket & TCGPlayer)
* Rule Fetching (Comprehensive- & Tournament Rules)
* Token Fetching
* Infraction Procedures
* Keyword definition lookups
* Currency Conversion
* Non-English Card Support
* More on the [Website](https://grimoire.bemacized.net)

Invite the bot to your Guild via this link: [Invite Grimoire](https://grimoire.bemacized.net/invite)

You can join our support server via here: [Support Server](https://grimoire.bemacized.net/support)

### Preference Dashboard
You can change your Guild preferences over at the [Dashboard](https://grimoire.bemacized.net/dashboard). Much of the bots functionality can be tailored to your preferences via this panel.
![Dashboard Screenshot](https://grimoire.bemacized.net/img/screenshots/Dashboard.png)

### Command Reference

#### Inline references

You can use inline shortcuts to quickly reference multiple cards within your message. You are limited to a max of 3 inline references per message.

| Shortcut                                                      | Command   | Examples                                                                         |
|---------------------------------------------------------------|-----------|----------------------------------------------------------------------------------|
| `<<card>>`<br>`<<card \| set>>`<br>`[[card]]`<br>`[[card \| set]]`    | `g!card`    | `<<Mighty Leap>>`<br>`<<Mighty Leap \| ORI>>`<br>`[[Mighty Leap \| Magic Origins]]`    |
| `<<$card>>`<br>`<<$card \| set>>`<br>`[[$card]]`<br>`[[card \| set]]` | `g!pricing` | `<<$Mighty Leap>>`<br>`<<$Might Leap \| ORI>>`<br>`[[$Mighty Leap \| Magic Origins]]`  |

#### Commands

All commands are prefixed using `g!` by default. You can change this behaviour via the Dashboard. For more in-depth information, you can visit the [Command Reference](https://grimoire.bemacized.net/reference) on the website! Alternatively, you can browse the commands via the bot by sending "commands" in a private message.

| Command                                                                 | Description                                                    | Aliases                                | Examples<br>                                                                                                              |
|-------------------------------------------------------------------------|----------------------------------------------------------------|----------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| g!art <card\|set><br>g!art <card>                                       | Fetch the full art of a card                                   | cardart                                | g!art Mighty Leap \| ORI<br>g!art Mighty Leap \| Magic Origins`<br>`g!art Mighty Leap                                   |
| g!card <card\|set><br>g!card <card>                                     | Fetch information for a card                                   | c                                      | g!card Mighty Leap \| ORI<br>g!card Mighty Leap \| Magic Origins<br>g!card Mighty Leap<br>                                |
| g!comprules <paragraph nr>                                              | Retrieve a paragraph from the comprehensive rules              | crules comprehensiverulescr            | g!comprules 702<br>g!comprules 702.5c<br>g!comprules 702.5<br>g!comprules 7<br>g!comprules<br>                            |
| g!define <keyword>                                                      | Looks up the definition for the specified keyword              | keyword definition                     | g!define vigilance<br>g!define prowess<br>g!define enchant<br>                                                            |
| g!flavor <card\|set><br>g!flavor <card>                                 | Retrieves the flavor text of a card.                           | flavortext                             | g!flavor Mighty Leap \| ORI<br>g!flavor Mighty Leap \| Magic Origins<br>g!flavor Mighty Leap<br>                          |
| g!help                                                                  | Shows the help text, containing all of the command references. |                                        | <br>                                                                                                                      |
| g!infractionprocedure <paragraph> [topic]                               | Retrieve a paragraph from the tournament rules.                | ipguide ipg                            | g!infractionprocedure 2.5<br>g!infractionprocedure 2.5 philosophy<br>g!infractionprocedure 2<br>g!infractionprocedure<br> |
| g!legality <card\|set><br>g!legality <card>                             | Checks the legality of a card, for every known format          | format legalities formatsillegal legal | g!legality Mighty Leap \| ORI<br>g!legality Mighty Leap \| Magic Origins<br>g!legality Mighty Leap<br>                    |
| g!names <card\|set><br>g!names <card>                                   | Retrieves all known foreign names for a card                   | foreign named abroad                   | g!names Mighty Leap \| ORI<br>g!names Mighty Leap \| Magic Origins<br>g!names Mighty Leap<br>                             |
| g!oracle <card\|set><br>g!oracle <card>                                 | Retrieves the oracle text of a card.                           | cardtext                               | g!oracle Mighty Leap \| ORI<br>g!oracle Mighty Leap \| Magic Origins<br>g!oracle Mighty Leap<br>                          |
| g!pricing <card\|set><br>g!pricing <card>                               | Retrieves the current pricing for a card.                      | dollarydoos price                      | g!pricing Mighty Leap \| ORI<br>g!pricing Mighty Leap \| Magic Origins<br>g!pricing Mighty Leap<br>                       |
| g!prints <card\|set><br>g!prints <card>                                 | Retrieves all sets that a card was printed in.                 | versions printings sets                | g!prints Mighty Leap \| ORI<br>g!prints Mighty Leap<br>g!prints Mighty Leap \| Magic Origins<br>                          |
| g!random [supertype] [type] [subtype] [rarity] [set] [setcode] [layout] | Show a random card of a certain type.                          | rng rand                               | g!random C17 mythic<br>g!random rare artifact<br>g!random legendary creature<br>g!random<br>                              |
| g!reloadpreferences                                                     | Reload preferences immediately for your guild                  | reloadprefs                            | <br>                                                                                                                      |
| g!rulings <card\|set><br>g!rulings <card>                               | Retrieves the current rulings of the specified card.           | ruling rules                           | g!rulings Mighty Leap \| ORI<br>g!rulings Mighty Leap \| Magic Origins<br>g!rulings Mighty Leap<br>                       |
| g!set <set>                                                             | Fetch information for a set                                    | s                                      | g!set Magic Origins<br>g!set ORI<br>                                                                                      |
| g!standard                                                              | See what sets are currently in standard rotation               | wis whatsinstandard                    | <br>                                                                                                                      |
| g!token <token_name> [choice]                                           | Retrieve the art of a token.                                   |                                        | g!token angel 3<br>g!token angel<br>                                                                                      |
| g!tournamentrules <paragraph nr>                                        | Retrieve a paragraph from the tournament rules                 | mtr trmagictournamentrules             | g!tournamentrules<br>g!tournamentrules 3<br>g!tournamentrules 3.10<br>                                                    |

### Selfhosting
Instructions for hosting this bot by yourself are coming soon!

### Magic & Chill
In case you are interested, feel free to come join our partner server, [Discord & Chill](https://discord.gg/vqsFzgJ). We talk about Magic all day and love new people joining in!
### Disclaimer

The literal and graphical information presented in this repository, and by the Discord bot about Magic: The Gathering, including card images, the mana symbols, and Oracle text, is copyright Wizards of the Coast, LLC, a subsidiary of Hasbro, Inc. This project is not produced by, endorsed by, supported by, or affiliated with Wizards of the Coast.

Card prices represent daily averages and/or market values provided by our affiliates. Absolutely no guarantee is made for any price information. See stores for final prices and availability.

The Discord logo and Discord API are copyright Discord, Grimoire is not created by, affiliated with, or supported by Discord.

The Scryfall logo and Scryfall API are copyright Some Assembly, LLC, DBA Scryfall. Grimoire is not created by, affiliated with, or supported by Scryfall.

