var mongoose = require('mongoose');
const config = require('./../data/config.js');
const moment = require("moment");
var schedule = require('node-schedule');

//connect to database
mongoose.connect(config.database);

//Define schemas
var CardData = mongoose.model('CardData', new mongoose.Schema({
        name: {type: String, required: true},
        setCode: {type: String, required: true},
        marketplaces: {
            mcm: {
                price: {
                    low: {type: Number},
                    lowFoil: {type: Number},
                    avg: {type: Number},
                    trend: {type: Number}
                },
                url: {type: String},
                lastUpdated: {type: Number}
            },
            tcg: {
                price: {
                    low: {type: Number},
                    avg: {type: Number},
                    high: {type: Number},
                    avgFoil: {type: Number}
                },
                url: {type: String},
                lastUpdated: {type: Number}
            }
        }
    })
);

var RetrievalRecord = mongoose.model('RetrievalRecord', new mongoose.Schema({
        cardName: {type: String, required: true},
        setCode: {type: String, required: true},
        provider: {type: String, required: true},
        timestamp: {type: Number, required: true}
    })
);

//Define functions
var cleanup = function () {
    CardData.remove({lastUpdated: {$lte: (moment().unix() - config.maxCardAge)}}, function (err) {
        if (err) console.log(err);
    });
};

//Run cleanup at start
cleanup();

//Schedule daily cleanup
schedule.scheduleJob('00 00 12 * * 1-7', function () {
    cleanup();
});

//Export module
module.exports = {"CardData": CardData, "RetrievalRecord": RetrievalRecord};