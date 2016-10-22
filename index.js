//Imports
const Discord = require('discord.js');
const config = require('./config.js');
const mtg = require('mtgsdk');

//Global initializations
const bot = new Discord.Client();

//Notify when ready for use
bot.on('ready', () => {
    console.log('Ready.');
});

//Handle message receive event
bot.on('message', message => {

    //Don't respond to bot users
    if (message.author.bot) return;

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
        const cname = name;
        mtg.card.where({name: cname})
            .then(cards => {
                //We received no results
                if (cards.length == 0) {
                    response += "No results found for '" + cname + "'\n\n";
                }

                //We did receive results
                else {

                    //Single out the one card we need
                    var card;
                    {
                        //If we have a 1:1 match, remove all non matching results.
                        var tmpCards = cards.filter(function (e) {
                            return e.name.toLowerCase() == cname.toLowerCase()
                        });
                        if (tmpCards.length > 0) cards = tmpCards;

                        //If we don't, we preselect the last (most recent) result.
                        else card = tmpCards[tmpCards.length - 1];
                    }

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

                    //Queue the image for upload if we have a definitive result
                    else images.push(card.imageUrl);
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
});

//Login to discord
bot.login(config.botToken);