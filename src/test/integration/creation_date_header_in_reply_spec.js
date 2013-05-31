require('./integration_config.js');

var channelName = utils.randomChannelName();
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "there's a snake in my boot!";

utils.configureFrisby();

utils.runInTestChannel(channelName, function () {
    frisby.create('Inserting a value into a channel.')
        .post(thisChannelResource, null, { body: messageText})
        .addHeader("Content-Type", "text/plain")
        .expectStatus(201)
        .afterJSON(function (result) {
            var valueUrl = result['_links']['self']['href'];
            frisby.create('Fetching value in order to check creation date.')
                .get(valueUrl)
                .expectStatus(200)
                // Wishing frisby allowed callbacks for header validation too...but it doesn't yet.
                .expectHeaderContains('creation-date', 'T')
                .toss();
        })
        .toss();
});