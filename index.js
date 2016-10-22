//Imports
const Discord = require('discord.js');
const config = require('./config.js');
const mtg = require('mtgsdk');

//Global initializations
const bot = new Discord.Client();

bot.on('ready', () => {
    console.log('Ready.');
});

bot.on('message', message => {
    if (message.author.bot) return;

    //Extract names from message
    var names = message.content.match(/<<[^<>]+>>/g);
    if (names == null || names.length == 0) return;
    var retmsg = "";
    var images = [];
    names = names.slice(0, config.maxCardsPerMessage);

    //Evaluate every cardname
    var callsRemaining = names.length;
    for (var name of names) {
        //Remove <<*>>
        name = name.substr(2, name.length - 4);

        //Start making requests
        const cname = name;
        mtg.card.where({name: cname})
            .then(cards => {
                if (cards.length == 0) {
                    retmsg += "No results found for '" + cname + "'\n\n";
                }
                else {
                    //Check for multiple results
                    var matchesAny = false;
                    var uniqueCards = [];

                    for (var card of cards) {
                        if (cname.toLowerCase() == card.name.toLowerCase()) matchesAny = true;
                        if (uniqueCards.indexOf(card.name) <= -1) uniqueCards.push(card.name);
                    }

                    if (!matchesAny && uniqueCards.length > 1) {
                        retmsg += "There are multiple results for '" + cname + "'. Did you mean one of the following?\n";
                        for (var card of uniqueCards) {
                            retmsg += " - " + card + "\n";
                        }
                        retmsg += "\n";
                    }

                    //Queue the image for upload
                    else {
                        images.push(cards[cards.length - 1].imageUrl);
                    }
                }

                //Upload the images and send the message
                callsRemaining--;
                if (callsRemaining == 0) {
                    if (retmsg.length > 0) message.channel.sendMessage(retmsg);
                    for (var img of images) {
                        message.channel.sendFile(img, "card.png");
                    }
                }
            });
    }
});

//Login to discord
bot.login(config.botToken);