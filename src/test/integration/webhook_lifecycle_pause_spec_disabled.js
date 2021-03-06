// bc -- this test does not run reliably due to what appears to be timing issues.

require('./integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var webhookName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
var port = utils.getPort();
var callbackUrl = callbackDomain + ':' + port + '/';
var webhookConfig = {
    callbackUrl: callbackUrl,
    channelUrl: channelResource,
    paused: false
};

var webhookConfigPaused = {
    callbackUrl: callbackUrl,
    channelUrl: channelResource,
    paused: true
};
/**
 *
 * This should:
 *
 * 1 - create a channel
 * 2 - create a webhook on that channel
 * 3 - start a server at the endpoint
 * 4 - post items into the channel
 * 5 - verify that the records are returned within delta time
 * 6 - pause the webhook
 * 7 - post items into the channel
 * 8 - verify that no records are returned within delta time
 * 9 - un-pause the webhook
 * 10 - verify that the records are returned within delta time
 */
describe(testName, function () {

    var callbackItems = [];
    var postedItems = [];

    function postedItem(value, post) {
        postedItems.push(value.body._links.self.href);
        console.log('value.body._links.self.href', value.body._links.self.href)
        if (post) {
            return utils.postItemQ(channelResource);
        }
    }

    var channel = utils.createChannel(channelName);

    var webhook = utils.putWebhook(webhookName, webhookConfig, 201, testName);

    it('runs callback server and posts ' + webhookName, function (done) {
        runs(function () {
            utils.startServer(port, function (string) {
                callbackItems.push(string);
                console.log(callbackItems.length, 'called back', string);
            });

            // adding two items
            utils.postItemQ(channelResource)
                .then(function (value) {
                    return postedItem(value, true);
                }).then(function (value) {
                postedItem(value, false);
                done();
            });
        });
        waitsFor(function () {
            console.log(postedItems.length, callbackItems.length);

            return callbackItems.length > 0;
        }, "2 callbacks collected", 15 * 1000);

    }, 15 * 1000);
    utils.timeout(2000);
    it('expects 2 items collected', function () {
        expect(callbackItems.length).toBe(2);
    });

    console.log("###### pausing web hook");
    webhook = utils.putWebhook(webhookName, webhookConfigPaused, 200, testName);

    it('posts items to paused ' + webhookName, function (done) {
        utils.sleepQ(2000).then(function () {
            utils.postItemQ(channelResource)
                .then(function (value) {
                    return postedItem(value, true);
                }).then(function (value) {
                postedItem(value, false);
                utils.sleep(500);
                done();
            });
            });


    }, 3000);

    // we added another 2 to a paused web hook.  should still be 2
    it('verfies number ' + webhookName, function () {
        expect(callbackItems.length).toBe(2);
    });


    console.log("###### resuming web hook");
    webhook = utils.putWebhook(webhookName, webhookConfig, 200, testName);
    utils.timeout(2000);
    it('waits for items ' + webhookName, function () {
        utils.sleepQ(2000).then(function () {
            utils.closeServer(function () {
                expect(callbackItems.length).toBe(4);
                expect(postedItems.length).toBe(4);
                // for (var i = 0; i < callbackItems.length; i++) {
                //     var parse = JSON.parse(callbackItems[i]);
                //     expect(parse.uris[0]).toBe(postedItems[i]);
                //     expect(parse.name).toBe(webhookName);
                // }
                utils.deleteWebhook(webhookName);
            }, testName);
        });
    });

});

