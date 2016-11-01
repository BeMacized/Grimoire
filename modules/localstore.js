var mod = {};
var fs = require('fs');

mod.options = {
    nextFact: 0
};

mod.save = function () {
    fs.writeFile('./data/localstore.json', JSON.stringify(mod.options), function (err) {
        if (err) {
            console.log('An error occurred saving the localstore');
            console.log(err.message);
            return;
        }
    });
};

mod.load = function () {

    if (!fs.exists("./data/localstore.json")) {
        mod.save();
        return;
    }

    var data = fs.readFileSync('./data/localstore.json');

    try {
        mod.options = JSON.parse(data);
    }
    catch (err) {
        console.log('An error occurred loading the localstore');
        console.log(err);
    }
};

module.exports = mod;


