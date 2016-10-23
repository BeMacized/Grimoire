//Imports
const Discord = require('discord.js');
const config = require('./config.js');
const mtg = require('mtgsdk');

//Global initializations
const bot = new Discord.Client();
var lastCard = {};

//Notify when ready for use
bot.on('ready', () => {
    console.log('Ready.');
});

//Handle message receive event
bot.on('message', message => {

    //Don't respond to bot users
    if (message.author.bot) return;

    //Handle commands
    if (message.content.startsWith("!")) {
        var split = message.content.trim().split(/\s+/);
        var cmd = split[0].substr(1, split[0].length);
        var args = split.splice(1, split.length);

        switch (cmd) {
            case "rulings":
                if (!lastCard.hasOwnProperty(message.channel.id)) {
                    message.reply("I don't remember the last card mentioned!");
                    return;
                }
                const cardShell = lastCard[message.channel.id];
                mtg.card.where({name: cardShell.name})
                    .then(cards => {
                        if (cards.length == 0) {
                            message.reply("I wasn't able to retrieve information about '" + cardShell.name + "'");
                            return;
                        }
                        var card = null;
                        for (var c of cards) {
                            if (c.id == cardShell.id) {
                                card = c;
                                break;
                            }
                        }
                        if (card == null) {
                            message.reply("I wasn't able to retrieve information about '" + cardShell.name + "'");
                            return;
                        }
                        if (!card.hasOwnProperty("rulings")) {
                            message.reply("'" + cardShell.name + "' does not seem to have any specified rulings!");
                            return;
                        }
                        var response = "**Rulings for '" + card.name + "':**\n\n";
                        for (ruling of card.rulings) {
                            response += "**" + ruling.date + "**\n" + ruling.text + "\n\n";
                        }
                        message.reply(response);
                    });
                break;
        }
    }

    //Otherwise attempt finding card names
    else {
        //Extract names from message
        var names = message.content.match(/<<[^<>]+>>/g);
        if (names == null || names.length == 0) return;

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
                            if (set != null) {
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
                                var card = cards[cards.length - 1];
                                if (!card.hasOwnProperty("imageUrl")) {
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
                        for (var img of images) {
                            message.channel.sendFile(img, "card.png");
                        }
                    }
                });
        }
    }
});

//Login to discord
bot.login(config.botToken);