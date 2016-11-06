const config = require('./../data/config.js');
const randomstring = require("randomstring");
const rawurlencode = require('locutus/php/url/rawurlencode');
const crypto = require('crypto');
const request = require("request");
const moment = require("moment");
const dbmgr = require("./dbmgr.js");
const stathat = require('stathat');
const setDictionary = require("./setdictionary.js");
const parseXMLToJS = require('xml2js').parseString;

const getCardPricing = function (name, setCode, callback) {

    var setName = (setDictionary.hasOwnProperty(setCode) && setDictionary[setCode].tcg != '') ? setDictionary[setCode].tcg : null;

    if (setName == null) {
        callback(false, (setDictionary.hasOwnProperty(setCode)) ? "TCG_NO_SET_SUPPORT" : "UNKNOWN_SET");
        return;
    }

    //First we look it up in our database
    dbmgr.CardData.find({name: name, setCode: setCode}, function (err, models) {
        //If an error occurred, we let the callback know
        if (err) {
            console.log(error);
            callback(false, "DB_ERROR");
            return;
        }

        //If we find a record, we check if its still "fresh". If it is, use this as data.
        if (models.length > 0 && models[0].marketplaces.hasOwnProperty("tcg") && moment().unix() - models[0].marketplaces.tcg.lastUpdated < config.cardRefreshInterval) {
            callback(true, models[0].marketplaces.tcg);
            return;
        }

        //Let's check if we still have enough requests we're allowed to make.
        dbmgr.RetrievalRecord.find({
            timestamp: {$gte: (moment().unix() - 3600)},
            provider: "TCG"
        }, function (err, docs) {
            //If an error occurred, we let the callback know
            if (err) {
                console.log(error);
                callback(false, "DB_ERROR");
                return;
            }

            //We let the callback know that we're rate limited.
            if (docs.length >= config.TCG_RATE_LIMIT) {
                callback(false, "RATE_LIMIT");
                return;
            }

            //We're in the clear, let's contact MagicCardMarket!
            //First log that we made the request with stathat and our db
            stathat.trackEZCount(config.statHatEZKey, "TCGPlayer Data Retrieval", 1, function (status, json) {
            });

            new dbmgr.RetrievalRecord({
                cardName: name,
                setCode: setCode,
                provider: "TCG",
                timestamp: moment().unix()
            }).save(function (err) {
                if (err) {
                    //We could not save a retrieval record. In order to prevent accidently sending too many requests, we just stop here.
                    console.log(err);
                    callback(false, "DB_ERROR");
                    return;
                }

                var endpoint = config.TCG_ENDPOINT;
                var method = "GET";
                request({
                    uri: endpoint,
                    method: method,
                    timeout: 5000,
                    followRedirect: true,
                    maxRedirects: 10,
                    qs: {
                        pk: config.TCG_PARTNER_KEY,
                        s: setName,
                        p: name
                    }
                }, function (error, response, body) {
                    //Handle errors
                    if (!body) {
                        callback(false, "TCG_UNHANDLED_RESPONSE");
                        return;
                    }
                    parseXMLToJS(body, function (err, result) {
                        //Handle errors
                        if (err) {
                            switch (body.trim()) {
                                case "Product not found.":
                                    callback(false, "NO_DATA_AVAILABLE");
                                    break;
                                default:
                                    console.log("[NOTE] TCG Unhandled Response Body: \n" + body);
                                    callback(false, "TCG_UNHANDLED_BODY");
                                    break;
                            }
                            return;
                        }

                        //Construct the data
                        const tcg = {
                            url: result.products.product[0].link[0],
                            price: {
                                low: result.products.product[0].lowprice[0],
                                avg: result.products.product[0].avgprice[0],
                                high: result.products.product[0].hiprice[0],
                                avgFoil: result.products.product[0].foilavgprice[0]
                            },
                            lastUpdated: moment().unix()
                        };

                        //Let's check if data exists first
                        dbmgr.CardData.find({name: name, setCode: setCode}, function (err, models) {

                            //log error if it exists
                            if (err) {
                                console.log(err);
                                return;
                            }

                            //Data exists, let's update it
                            if (models.length > 0) {
                                for (var model of models) {
                                    model.marketplaces.tcg = tcg;
                                    model.save(function (err) {
                                        //We can still continue if the caching fails. Rate limiting acts as a safety net.
                                        if (err)
                                            console.log(err);
                                    });
                                }
                            }
                            //No data exists, let's insert something new
                            else {
                                const cardData = new dbmgr.CardData({
                                    name: name,
                                    setCode: setCode,
                                    marketplaces: {
                                        tcg: tcg
                                    }
                                });
                                cardData.save(function (err) {
                                    //We can still continue if the caching fails. Rate limiting acts as a safety net.
                                    if (err)
                                        console.log(err);
                                });
                            }
                        });

                        //return obtained data
                        callback(true, tcg);
                        return;
                    });
                });
            });
        });
    });
};

module.exports = {"getCardPricing": getCardPricing};