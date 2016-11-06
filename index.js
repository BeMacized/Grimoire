//Imports
const Discord = require('discord.js');
const config = require('./data/config.js');
const mtg = require('mtgsdk');
const facts = require("./modules/facts.js");
const localstore = require("./modules/localstore.js");
const stathat = require('stathat');
const mcm = require('./modules/mcm.js');
const tcg = require('./modules/tcg.js');
const moment = require("moment");

//Load localstore
localstore.load();
//Global initializations
const bot = new Discord.Client();
const factTriggers = ["thopter", "servo", "kaladesh", "fabricate"];
var lastCard = {};
//Notify when ready for use
bot.on('ready', () => {
    console.log('Ready.');
});

var getEmoji = function (name, guild) {
    for (var emoji of guild.emojis) {
        if (emoji[1].name == name) {
            return emoji[1];
        }
    }
    return null;
};

//Handle message receive event
bot.on('message', message => {
    //Don't respond to bot users
    if (message.author.bot) return;
    //Extract names from message
    var names = message.content.match(/<<[^<>]+>>/g);
    //Handle commands
    if (message.content.startsWith("!")) {
        var split = message.content.trim().split(/\s+/);
        var cmd = split[0].substr(1, split[0].length);
        var args = split.splice(1, split.length);
        switch (cmd) {
            case "time": {
                message.reply("My system time is set to " + moment(moment().unix() * 1000).utcOffset("+0000").format('YYYY-MM-DD HH:mm:ss') + " UTC");
                break;
            }
            case "pricing": {
                //Log statistic
                stathat.trackEZCount(config.statHatEZKey, "Command: !pricing", 1, function (status, json) {
                });
                //First, let's check if a card was specified.
                var manual = false;
                var cardShell;
                if (args.length > 0) {
                    cardShell = {name: args.join(" "), id: null};
                    manual = true;
                }
                //Check if we know of any card that was previously mentioned, if not tell the user we don't know what to do
                else if (!lastCard.hasOwnProperty(message.channel.id)) {
                    message.reply("I don't remember the last card mentioned!");
                    return;
                } else {
                    cardShell = lastCard[message.channel.id];
                }
                //Find the card
                mtg.card.where({name: cardShell.name})
                    .then(cards => {
                        //No cards found!
                        if (cards.length == 0) {
                            message.reply("I wasn't able to retrieve information about '" + cardShell.name + "'");
                            return;
                        }
                        //Get the card that has an ID match
                        var card = null;
                        cards.reverse();
                        if (!manual) {
                            for (var c of cards) {
                                if (c.id == cardShell.id) {
                                    card = c;
                                    break;
                                }
                            }
                        } else {
                            for (var c of cards) {
                                if (c.name.toLowerCase() == cardShell.name.toLowerCase()) {
                                    card = c;
                                    break;
                                }
                            }
                        }
                        //We couldn't find a matching card.
                        if (card == null) {
                            message.reply("I wasn't able to retrieve information about '" + cardShell.name + "'");
                            return;
                        }

                        //Save as last card mentioned
                        if (manual) lastCard[message.channel.id] = {id: card.id, name: card.name};

                        //TODO: Make requests async.

                        //Retrieve the pricing for MCM
                        mcm.getCardPricing(card.name, card.set, function (success, mcmData) {
                            var MCM_resp = "";
                            if (!success) {
                                switch (mcmData) {
                                    case "MCM_NO_SET_SUPPORT":
                                        MCM_resp = "Set '" + card.setName + "' is currently unsupported by <http://MagicCardMarket.eu>.";
                                        break;
                                    case "UNKNOWN_SET":
                                        MCM_resp = "Set '" + card.setName + " (" + card.set + ")' has not been implemented for <http://MagicCardMarket.eu>.  Pinging " + message.guild.members.get(config.devId) + " to implement this set for MagicCardMarket. ";
                                        break;
                                    case "RATE_LIMIT":
                                        MCM_resp = "I've asked too much data from <http://MagicCardMarket.eu>. Please try again later!";
                                        break;
                                    case "NO_DATA_AVAILABLE":
                                        MCM_resp = "I couldn't find any results on <http://MagicCardMarket.eu> about '" + card.name + "'.";
                                        break;
                                    case "NO_DATA_AVAILABLE_SET":
                                        MCM_resp = "I couldn't find any results on <http://MagicCardMarket.eu> about '" + card.name + "' in set '" + card.setName + "'.";
                                        break;
                                    case "MCM_UNHANDLED_BODY":
                                        MCM_resp = "Something funky went down and I don't know how to deal with it. Pinging " + message.guild.members.get(config.devId) + " to fix his shit. (" + mcmData + ")";
                                        break;
                                    case "MCM_UNHANDLED_RESPONSE":
                                        MCM_resp = "Something funky went down and I don't know how to deal with it. Pinging " + message.guild.members.get(config.devId) + " to fix his shit. (" + mcmData + ")";
                                        break;
                                    case "DB_ERROR":
                                        message.reply("A problem occurred with my database. Pinging " + message.guild.members.get(config.devId) + " to fix his shit.");
                                        return;
                                    default:
                                        message.reply("I cannot help you right now because " + message.guild.members.get(config.devId) + " messed up big time. Tell him to fix me up! (" + mcmData + ")");
                                        return;
                                }
                            } else {
                                MCM_resp = "Low: €" + mcmData.price.low + " _(Foil: €" + mcmData.price.lowFoil + ")_ **|** Average: €" + mcmData.price.avg + " **|** Trend: €" + mcmData.price.trend + "\n" +
                                    "For more information visit <" + mcmData.url + ">\n"
                                    + "_(Last updated at " + moment(mcmData.lastUpdated * 1000).utcOffset("+0000").format('YYYY-MM-DD HH:mm:ss') + " UTC)_"
                            }

                            //Retrieve the pricing for TCGPlayer
                            tcg.getCardPricing(card.name, card.set, function(success,tcgData){
                                var TCG_resp = "";
                                if (!success) {
                                    switch (tcgData) {
                                        case "TCG_NO_SET_SUPPORT":
                                            TCG_resp = "Set '" + card.setName + "' is currently unsupported by <http://tcgplayer.com/>.";
                                            break;
                                        case "UNKNOWN_SET":
                                            TCG_resp = "Set '" + card.setName + " (" + card.set + ")' has not been implemented for <http://tcgplayer.com/>.  Pinging " + message.guild.members.get(config.devId) + " to implement this set for TCGPlayer. ";
                                            break;
                                        case "RATE_LIMIT":
                                            TCG_resp = "I've asked too much data from <http://tcgplayer.com/>. Please try again later!";
                                            break;
                                        case "NO_DATA_AVAILABLE":
                                            TCG_resp = "I couldn't find any results on <http://tcgplayer.com/> about '" + card.name + "'.";
                                            break;
                                        case "TCG_UNHANDLED_BODY":
                                            TCG_resp = "Something funky went down and I don't know how to deal with it. Pinging " + message.guild.members.get(config.devId) + " to fix his shit. (" + tcgData + ")";
                                            break;
                                        case "TCG_UNHANDLED_RESPONSE":
                                            TCG_resp = "Something funky went down and I don't know how to deal with it. Pinging " + message.guild.members.get(config.devId) + " to fix his shit. (" + tcgData + ")";
                                            break;
                                        case "DB_ERROR":
                                            message.reply("A problem occurred with my database. Pinging " + message.guild.members.get(config.devId) + " to fix his shit.");
                                            return;
                                        default:
                                            message.reply("I cannot help you right now because " + message.guild.members.get(config.devId) + " messed up big time. Tell him to fix me up! (" + tcgData + ")");
                                            return;
                                    }
                                } else {
                                    TCG_resp = "Low: $" + tcgData.price.low + " **|** Average: $" + tcgData.price.avg + " _(Foil: $" + tcgData.price.avgFoil + ")_ **|** High: $" + tcgData.price.high + "\n" +
                                        "For more information visit <" + tcgData.url + ">\n"
                                        + "_(Last updated at " + moment(tcgData.lastUpdated * 1000).utcOffset("+0000").format('YYYY-MM-DD HH:mm:ss') + " UTC)_"
                                }
                                message.reply("\n**Pricing Data for '" + card.name + "' from set '" + card.setName + "':**\n\n"
                                    + "**MagicCardMarket.eu:** " + MCM_resp +"\n\n"
                                    + "**TCGPlayer.com:** " + TCG_resp
                                );
                            });
                        });
                    })
                    .catch(function (err) {
                        message.reply("I wasn't able to retrieve information about '" + cardShell.name + "', as either the card is not available on the Gatherer, or the data source is currently offline.");
                    });
                break;
            }
            case "oracle":
                //Log statistic
                stathat.trackEZCount(config.statHatEZKey, "Command: !oracle", 1, function (status, json) {
                });

                //Define variable mapping:
                var emojiMap = {
                    "W": "manaW",
                    "U": "manaU",
                    "B": "manaB",
                    "R": "manaR",
                    "G": "manaG",
                    "C": "manaC",
                    "W/U": "manaWU",
                    "U/B": "manaUB",
                    "B/R": "manaBR",
                    "R/G": "manaRG",
                    "G/W": "manaGW",
                    "W/B": "manaWB",
                    "U/R": "manaUR",
                    "B/G": "manaBG",
                    "R/W": "manaRW",
                    "G/U": "manaGU",
                    "2/W": "mana2W",
                    "2/U": "mana2U",
                    "2/B": "mana2B",
                    "2/R": "mana2R",
                    "2/G": "mana2G",
                    "WP": "manaWP",
                    "UP": "manaUP",
                    "BP": "manaBP",
                    "RP": "manaRP",
                    "GP": "manaGP",
                    "0": "manaZero",
                    "1": "manaOne",
                    "2": "manaTwo",
                    "3": "manaThree",
                    "4": "manaFour",
                    "5": "manaFive",
                    "6": "manaSix",
                    "7": "manaSeven",
                    "8": "manaEight",
                    "9": "manaNine",
                    "10": "manaTen",
                    "11": "manaEleven",
                    "12": "manaTwelve",
                    "13": "manaThirteen",
                    "14": "manaFourteen",
                    "15": "manaFifteen",
                    "16": "manaSixteen",
                    "20": "manaTwenty",
                    "T": "manaT",
                    "Q": "manaQ",
                    "S": "manaS",
                    "X": "manaX",
                    "E": "manaE"
                };

                //First, let's check if a card was specified.
                var manual = false;
                var cardShell;
                if (args.length > 0) {
                    cardShell = {name: args.join(" "), id: null};
                    manual = true;
                }
                //Check if we know of any card that was previously mentioned, if not tell the user we don't know what to do
                else if (!lastCard.hasOwnProperty(message.channel.id)) {
                    message.reply("I don't remember the last card mentioned!");
                    return;
                } else {
                    cardShell = lastCard[message.channel.id];
                }
                //Find the card
                mtg.card.where({name: cardShell.name})
                    .then(cards => {
                        //No cards found!
                        if (cards.length == 0) {
                            message.reply("I wasn't able to retrieve information about '" + cardShell.name + "'");
                            return;
                        }
                        //Get the card that has an ID match
                        var card = null;
                        if (!manual) {
                            for (var c of cards) {
                                if (c.id == cardShell.id) {
                                    card = c;
                                    break;
                                }
                            }
                        } else {
                            for (var c of cards) {
                                if (c.name.toLowerCase() == cardShell.name.toLowerCase()) {
                                    card = c;
                                    break;
                                }
                            }
                        }
                        //We couldn't find a matching card.
                        if (card == null) {
                            message.reply("I wasn't able to retrieve information about '" + cardShell.name + "'");
                            return;
                        }

                        //Save as last card mentioned
                        if (manual) lastCard[message.channel.id] = {id: card.id, name: card.name};

                        //replacing symbols
                        var oracle = card.text;
                        for (var v in emojiMap) {
                            var emoji = getEmoji(emojiMap[v], message.guild);
                            if (!emoji) continue;
                            oracle = oracle.replace(new RegExp("\\{" + v + "\\}", "g"), emoji.toString());
                        }

                        //reply
                        message.reply("**Oracle text for '" + cardShell.name + "':**\n\n_" + oracle + "_");
                    })
                    .catch(function (err) {
                        console.log(err);
                        message.reply("I wasn't able to retrieve information about '" + cardShell.name + "', as either the card is not available on the Gatherer, or the data source is currently offline.");
                    });
                break;
            case "rulings":

                //Log statistic
                stathat.trackEZCount(config.statHatEZKey, "Command: !rulings", 1, function (status, json) {
                });
                //First, let's check if a card was specified.
                var manual = false;
                var cardShell;
                if (args.length > 0) {
                    cardShell = {name: args.join(" "), id: null};
                    manual = true;
                }
                //Check if we know of any card that was previously mentioned, if not tell the user we don't know what to do
                else if (!lastCard.hasOwnProperty(message.channel.id)) {
                    message.reply("I don't remember the last card mentioned!");
                    return;
                } else {
                    cardShell = lastCard[message.channel.id];
                }
                //Find the card
                mtg.card.where({name: cardShell.name})
                    .then(cards => {
                        //No cards found!
                        if (cards.length == 0) {
                            message.reply("I wasn't able to retrieve information about '" + cardShell.name + "'");
                            return;
                        }
                        //Get the card that has an ID match
                        var card = null;
                        if (!manual) {
                            for (var c of cards) {
                                if (c.id == cardShell.id) {
                                    card = c;
                                    break;
                                }
                            }
                        } else {
                            for (var c of cards) {
                                if (c.name.toLowerCase() == cardShell.name.toLowerCase()) {
                                    card = c;
                                    break;
                                }
                            }
                        }
                        //We couldn't find a matching card.
                        if (card == null) {
                            message.reply("I wasn't able to retrieve information about '" + cardShell.name + "'");
                            return;
                        }

                        //Save as last card mentioned
                        if (manual) lastCard[message.channel.id] = {id: card.id, name: card.name};

                        //check if the card has rulings, if not let the user know
                        if (!card.hasOwnProperty("rulings")) {
                            message.reply("'" + cardShell.name + "' does not seem to have any specified rulings!");
                            return;
                        }
                        //Show the user all the rulings.
                        var response = "**Rulings for '" + card.name + "':**\n\n";
                        for (ruling of card.rulings) {
                            response += "**" + ruling.date + "**\n" + ruling.text + "\n\n";
                        }
                        message.reply(response);
                    })
                    .catch(function (err) {
                        message.reply("I wasn't able to retrieve information about '" + cardShell.name + "', as either the card is not available on the Gatherer, or the data source is currently offline.");
                    });
                break;
            case "prints":

                //Log statistic
                stathat.trackEZCount(config.statHatEZKey, "Command: !prints", 1, function (status, json) {
                });
                //First, let's check if a card was specified.
                var manual = false;
                var cardShell;
                if (args.length > 0) {
                    cardShell = {name: args.join(" "), id: null};
                    manual = true;
                }
                //Check if we know of any card that was previously mentioned, if not tell the user we don't know what to do
                else if (!lastCard.hasOwnProperty(message.channel.id)) {
                    message.reply("I don't remember the last card mentioned!");
                    return;
                } else {
                    cardShell = lastCard[message.channel.id];
                }
                //Find the card
                mtg.card.where({name: cardShell.name})
                    .then(cards => {
                        //No cards found!
                        if (cards.length == 0) {
                            message.reply("I wasn't able to retrieve information about '" + cardShell.name + "'");
                            return;
                        }
                        //Get the card that has an ID match
                        var filtered_cards = [];
                        if (!manual) {
                            for (var c of cards) {
                                if (c.name.toLowerCase() == cardShell.name.toLowerCase())
                                    filtered_cards.push(c);
                            }
                        } else {
                            for (var c of cards) {
                                if (c.name.toLowerCase() == cardShell.name.toLowerCase())
                                    filtered_cards.push(c);
                            }
                        }
                        //We couldn't find a matching card.
                        if (filtered_cards.length == 0) {
                            message.reply("I wasn't able to retrieve information about '" + cardShell.name + "'");
                            return;
                        }

                        //Save as last card mentioned
                        if (manual) lastCard[message.channel.id] = {id: card.id, name: card.name};

                        //Show the user all the sets.
                        var response = "**Card '" + filtered_cards[0].name + "' was printed in the following sets:**\n\n";
                        for (var card of filtered_cards) {
                            response += " - " + card.setName + " (" + card.set + ")\n";
                        }
                        message.reply(response);
                    })
                    .catch(function (err) {
                        message.reply("I wasn't able to retrieve information about '" + cardShell.name + "', as either the card is not available on the Gatherer, or the data source is currently offline.");
                    });
                break;
            case "help":
                try {
                    message.reply(
                        "**You requested help! Here are some instructions:**\n\n" +
                        "My general usage is pulling up card art. You can do this by surrounding card names with << >>.\n" +
                        "For example: _<<Shivan Dragon>> has been reprinted many times!_\n" +
                        "You can too specify a specific set if you want to. You can do this by either supplying the set name, or a 3 letter set code.\n" +
                        "For example: _<<Shivan Dragon|Limited Edition Alpha>> is a very old card!_\n" +
                        "Or alternatively: _<<Shivan Dragon|LEA>> is a very old card!_\n\n" +
                        "I too have some commands you can use:\n" +
                        "**!help** - That is this command you dummy!\n" +
                        "**!rulings** _[cardname]_ - Pull up the rulings for a card. If no cardname is supplied, it will take the last card mentioned in the channel.\n" +
                        "**!stats** - Pull up some bot related statistics!\n" +
                        "**!prints** _[cardname]_ - Pull up all the sets a card has been printed in. If no cardname is supplied, it will take the last card mentioned in the channel.\n" +
                        "**!oracle** _[cardname]_ - Pull up the oracle (current) text of a specific card. If no cardname is supplied, it will take the last card mentioned in the channel.\n" +
                        "**!pricing** _[cardname]_ - Pull up pricing data for a specific card. If no cardname is supplied, it will take the last card mentioned in the channel.\n\n" +
                        "If you have any issues or questions about me, please shoot my developer " + message.guild.members.get(config.devId) + " a PM!"
                    );
                } catch (err) {
                }
                break;
            case "stats":
                message.reply("**Bot Statistics**\n\n**Card Lookups:** https://www.stathat.com/s/WKI6qS4tPTjI");
                break;
        }
    }
    //Otherwise attempt finding card names
    else if (names != null && names.length > 0) {

        //Initialize response variables
        var response = "";
        var images = [];
        //Remove excess names above threshold
        names = names.slice(0, config.maxCardsPerMessage);
        //Evaluate every card name
        var callsRemaining = names.length;
        for (var name of names) {
            //Remove << & >>
            name = name.substr(2, name.length - 4);
            //Start making API requests
            var split = name.split(/\|/);
            const cname = split[0].trim();
            const set = (split.length > 1) ? split[1].trim() : null;
            mtg.card.where({name: cname})
                .then(cards => {
                    //We received no results
                    if (cards.length == 0) {
                        response += "No results found for '" + cname + "'\n\n";
                    }
                    //We did receive results
                    else {
                        //If we have a 1:1 match, remove all non matching results.
                        var tmpCards = cards.filter(function (e) {
                            return e.name.toLowerCase() == cname.toLowerCase()
                        });
                        if (tmpCards.length > 0) cards = tmpCards;
                        //If we still have multiple unique cards, present user with a list of names.
                        var uniqueCards = [];
                        for (var c of cards)
                            if (uniqueCards.indexOf(c.name) <= -1)
                                uniqueCards.push(c.name);
                        if (uniqueCards.length > 1) {
                            response += "There are multiple results for '" + cname + "'. Did you mean one of the following?\n";
                            for (var c of uniqueCards)
                                response += " - " + c + "\n";
                            response += "\n";
                        }
                        else {
                            var sets = [];
                            //If we specified the set, remove all cards with a nonmatching set
                            if (set != null && set !== "") {
                                var tmpCards = [];
                                for (c of cards) {
                                    if (c.set.toLowerCase() == set.toLowerCase() || c.setName.toLowerCase() == set.toLowerCase()) {
                                        tmpCards.push(c);
                                    } else {
                                        sets.push(c.setName + " (" + c.set + ")");
                                    }
                                }
                                cards = tmpCards;
                            }

                            //If we have no more cards left remaining, inform the user we cannot find the card in that set.
                            if (cards.length == 0) {
                                response += "No results found for '" + cname + "' in set '" + set + "'\n" +
                                    "The following sets are available for this card:\n";
                                for (s of sets)
                                    response += " - " + s + "\n";
                            }

                            //Queue the image for upload if we have a definitive result
                            else {
                                //Get last card in array with art
                                var card = null;
                                for (var i = cards.length - 1; i >= 0; i--) {
                                    if (cards[i].hasOwnProperty("imageUrl")) {
                                        card = cards[i];
                                        break;
                                    }
                                }
                                if (card == null) {
                                    response += "There is no card art available for '" + card.name + "' from set '" + card.setName + "' (" + card.set + ")!\n\n";
                                } else {
                                    images.push(card.imageUrl);
                                    lastCard[message.channel.id] = {id: card.id, name: card.name};
                                }
                            }
                        }
                    }
                    //Upload the images and send the message
                    callsRemaining--;
                    if (callsRemaining == 0) {
                        if (response.length > 0) message.channel.sendMessage(response);
                        for (var img of images)
                            message.channel.sendFile(img, "card.png");
                        stathat.trackEZCount(config.statHatEZKey, "Card Lookup", images.length, function (status, json) {
                        });
                    }
                })
                .catch(function (err) {
                    message.reply("I wasn't able to retrieve information about '" + cname + "', as either the card is not available on the Gatherer, or the data source is currently offline.");
                });
        }
    }
    else if (factTriggers.some(function (v) {
            return message.content.toLowerCase().indexOf(v) >= 0;
        }) && localstore.options.nextFact - Math.floor(Date.now() / 1000) < 0 && Math.floor(Math.random() * 5) == 0) {

        //Determine fact store
        var f = message.content.toLowerCase().includes("thopter") ? {
            facts: facts.thopterFacts,
            name: "Thopter"
        } : (message.content.toLowerCase().includes("servo") ? {
            facts: facts.servoFacts,
            name: "Servo"
        } : (Math.floor(Math.random * 2) == 0 ? {facts: facts.servoFacts, name: "Servo"} : {
            facts: facts.thopterFacts,
            name: "Thopter"
        }));
        //Set timeout for next fact
        localstore.options.nextFact = Math.floor(Date.now() / 1000) + Math.floor(Math.random() * (3600 * 3 - 3600) + 3600);
        localstore.save();
        //Send the fact
        message.channel.sendMessage(f.name + " Fact: " + f.facts[Math.floor(Math.random() * f.facts.length)]);
        //Log statistic
        stathat.trackEZCount(config.statHatEZKey, "Show Fact", 1, function (status, json) {
        });

    }
});
//Login to discord
bot.login(config.botToken);