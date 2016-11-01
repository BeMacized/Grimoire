const config = require('./../data/config.js');
const randomstring = require("randomstring");
const rawurlencode = require('locutus/php/url/rawurlencode');
const crypto = require('crypto');
const request = require("request");
const moment = require("moment");
const dbmgr = require("./dbmgr.js");
const stathat = require('stathat');


const generateHeader = function (httpMethod, realm) {
    //Define main parameters
    var realm = realm;
    var oauth_version = "1.0";
    var oauth_consumer_key = config.MCM_ID;
    var oauth_token = "";
    var oauth_signature_method = "HMAC-SHA1";
    var oauth_timestamp = moment().unix().toString();
    var oauth_nonce = randomstring.generate({length: 12, charset: 'alphabetic'});

    //Construct & set signature
    var baseString = httpMethod.toUpperCase() + "&" + rawurlencode(realm) + "&";
    var paramString = "oauth_consumer_key=" + rawurlencode(oauth_consumer_key) + "&" +
        "oauth_nonce=" + rawurlencode(oauth_nonce) + "&" +
        "oauth_signature_method=" + rawurlencode(oauth_signature_method) + "&" +
        "oauth_timestamp=" + rawurlencode(oauth_timestamp) + "&" +
        "oauth_token=" + rawurlencode(oauth_token) + "&" +
        "oauth_version=" + rawurlencode(oauth_version);
    baseString += rawurlencode(paramString);
    var signingKey = rawurlencode(config.MCM_SECRET) + "&";
    var oauth_signature = crypto.createHmac('sha1', signingKey).update(baseString).digest('base64');

    //Construct and return header authorization value
    return "OAuth " +
        "realm=\"" + realm + "\", " +
        "oauth_version=\"" + oauth_version + "\", " +
        "oauth_timestamp=\"" + oauth_timestamp + "\", " +
        "oauth_nonce=\"" + oauth_nonce + "\", " +
        "oauth_consumer_key=\"" + oauth_consumer_key + "\", " +
        "oauth_token=\"" + oauth_token + "\", " +
        "oauth_signature_method=\"" + oauth_signature_method + "\", " +
        "oauth_signature=\"" + oauth_signature + "\"";
};

const getCardPricing = function (name, setName, callback) {

    //Map MTG API sets to MCM sets
    const setMap = {
        "limited edition alpha": "alpha"
    };
    if (setName.toLowerCase() in setMap)
        setName = setMap[setName.toLowerCase()];

    //First we look it up in our database
    dbmgr.CardData.find({name: name, setName: setName}, function (err, models) {
        //If an error occurred, we let the callback know
        if (err) {
            console.log(error);
            callback(false, "DB_ERROR");
            return;
        }

        //If we find a record, we check if its still "fresh". If it is, use this as data.
        if (models.length > 0 && moment().unix() - models[0].lastUpdated < config.cardRefreshInterval) {
            callback(true, {
                price: models[0].price,
                lastUpdated: models[0].lastUpdated,
                url: models[0].url
            });
            return;
        }

        //Let's check if we still have enough requests we're allowed to make.
        dbmgr.RetrievalRecord.find({
            timestamp: {$gte: (moment().unix() - 3600)},
            provider: "MCM"
        }, function (err, docs) {
            //If an error occurred, we let the callback know
            if (err) {
                console.log(error);
                callback(false, "DB_ERROR");
                return;
            }

            //We let the callback know that we're rate limited.
            if (docs.length >= config.MCM_RATE_LIMIT) {
                callback(false, "RATE_LIMIT");
                return;
            }

            //We're in the clear, let's contact MagicCardMarket!
            //First log that we made the request with stathat and our db
            stathat.trackEZCount(config.statHatEZKey, "MagicCardMarket Data Retrieval", 1, function (status, json) {
            });
            new dbmgr.RetrievalRecord({
                cardName: name,
                setName: setName,
                provider: "MCM",
                timestamp: moment().unix()
            }).save(function (err) {
                if (err) {
                    //We could not save a retrieval record. In order to prevent accidently sending too many requests, we just stop here.
                    console.log(err);
                    callback(false, "DB_ERROR");
                    return;
                }

                var endpoint = "https://" + config.MCM_HOST + "/ws/v1.1/output.json/products/" + rawurlencode(name) + "/1/1/false";
                var method = "GET";
                request({
                    uri: endpoint,
                    method: method,
                    timeout: 5000,
                    followRedirect: true,
                    maxRedirects: 10,
                    headers: {
                        'Authorization': generateHeader(method, endpoint)
                    }
                }, function (error, response, body) {
                    //If an error occurred, let the callback know
                    if (error || response.statusCode != 200) {
                        console.log({
                            error: error,
                            response: response,
                            body: body
                        });
                        callback(false, "MCM_UNHANDLED_RESPONSE");
                        return;
                    }

                    try {
                        var resp = JSON.parse(body);

                        //Let's remove all non magic single products (deckboxes, playmats, etc)
                        resp.product = resp.product.filter(function (obj) {
                            return obj.category.idCategory == 1;
                        });

                        //We found no data!
                        if (resp.product.length == 0) {
                            callback(false, "NO_DATA_AVAILABLE");
                            return;
                        }

                        //First of all, let's save & update all new data we found! We can immediately find the card of the correct set.
                        var setCard = null;
                        for (const card of resp.product) {

                            //Remove old data
                            dbmgr.CardData.remove({
                                name: card.name["1"].productName,
                                setName: card.expansion
                            }, function (err) {
                            });

                            //Insert new data
                            const cardData = new dbmgr.CardData({
                                name: card.name["1"].productName,
                                setName: card.expansion,
                                lastUpdated: moment().unix(),
                                url: "https://www.magiccardmarket.eu" + card.website,
                                price: {
                                    low: card.priceGuide.LOWEX,
                                    lowFoil: card.priceGuide.LOWFOIL,
                                    avg: card.priceGuide.AVG,
                                    trend: card.priceGuide.TREND
                                }
                            });
                            cardData.save(function (err) {
                                //We can still continue if the caching fails. Rate limiting acts as a safety net.
                                if (err)
                                    console.log(err);
                            });

                            //Check if this is the card we were looking for
                            if (card.expansion.toLowerCase() == setName.toLowerCase())
                                setCard = {
                                    price: cardData.price,
                                    lastUpdated: cardData.lastUpdated,
                                    url: cardData.url
                                };
                        }

                        //If we did not find a result, notify the callback
                        if (setCard == null) {
                            callback(false, "NO_DATA_AVAILABLE_SET");
                            return;
                        }

                        callback(true, setCard);
                    } catch (err) {
                        console.log({
                            stackTrace: err,
                            error: error,
                            statusCode: response.statusCode,
                            body: body
                        });
                        callback(false, "MCM_UNHANDLED_BODY");
                    }
                });
            });
        });
    });
};

module.exports = {"getCardPricing": getCardPricing};