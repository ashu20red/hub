require('./integration_config.js');

var WebSocket = require('ws');
var url = require('url');
var request = require('request');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;

describe(__filename, function () {

    utils.createChannel(channelName, null, 'websocket testing');

    var itemURLs = [];

    describe('websocket channel endpoint', function () {

        var wsURL = channelResource.replace('http', 'ws') + '/ws';
        var webSocket;
        var receivedMessages = [];

        it('opens websocket', function (done) {
            expect(wsURL).not.toEqual('undefined');

            webSocket = new WebSocket(wsURL);
            webSocket.onmessage = function (message) {
                console.log('received:', message.data);
                receivedMessages.push(message.data);
            };

            webSocket.onclose = function () {
                console.log('closed:', wsURL);
            };

            webSocket.on('open', function () {
                console.log('opened:', wsURL);
                setTimeout(done, 5000);
            });
        });

        it('posts item to channel', function (done) {
            utils.postItemQ(channelResource)
                .then(function (result) {
                    console.log('posted:', result.response.headers.location);
                    itemURLs.push(result.response.headers.location);
                    done();
                });
        });

        it('waits for data', function (done) {
            waitForMessages(receivedMessages, itemURLs, done);
        });

        it('verifies the correct data was received', function () {
            expect(receivedMessages.length).toEqual(itemURLs.length);
            for (var i = 0; i < itemURLs.length; ++i) {
                expect(receivedMessages).toContain(itemURLs[i]);
            }
        });

        it('closes websocket', function () {
            webSocket.close();
        });

    });

    describe('websocket day endpoint', function () {

        var wsURL;
        var webSocket;
        var receivedMessages = [];

        it('builds websocket url', function () {
            expect(itemURLs.length).toEqual(1);
            var itemPath = url.parse(itemURLs[0]).pathname;
            var itemPathComponents = itemPath.split('/');
            var itemYear = itemPathComponents[3];
            var itemMonth = itemPathComponents[4];
            var itemDay = itemPathComponents[5];
            var dayURL = channelResource + '/' + itemYear + '/' + itemMonth + '/' + itemDay;
            wsURL = dayURL.replace('http', 'ws') + '/ws'
        });

        it('opens websocket', function (done) {
            expect(wsURL).not.toEqual('undefined');

            webSocket = new WebSocket(wsURL);
            webSocket.onmessage = function (message) {
                console.log('received:', message.data);
                receivedMessages.push(message.data);
            };

            webSocket.onclose = function () {
                console.log('closed:', wsURL);
            };

            webSocket.on('open', function () {
                console.log('opened:', wsURL);
                setTimeout(done, 5000);
            });
        });

        it('posts item to channel', function (done) {
            utils.postItemQ(channelResource)
                .then(function (result) {
                    console.log('posted:', result.response.headers.location);
                    itemURLs.push(result.response.headers.location);
                    done();
                });
        });

        it('waits for data', function (done) {
            waitForMessages(receivedMessages, itemURLs, done);
        });

        it('verifies the correct data was received', function () {
            expect(receivedMessages.length).toEqual(itemURLs.length);
            for (var i = 0; i < itemURLs.length; ++i) {
                expect(receivedMessages).toContain(itemURLs[i]);
            }
        });

        it('closes websocket', function () {
            webSocket.close();
        });

    });

    describe('websocket hour endpoint', function () {

        var wsURL;
        var webSocket;
        var receivedMessages = [];

        it('builds websocket url', function () {
            expect(itemURLs.length).toEqual(2);
            var itemPath = url.parse(itemURLs[0]).pathname;
            var itemPathComponents = itemPath.split('/');
            var itemYear = itemPathComponents[3];
            var itemMonth = itemPathComponents[4];
            var itemDay = itemPathComponents[5];
            var itemHour = itemPathComponents[6];
            var hourURL = channelResource + '/' + itemYear + '/' + itemMonth + '/' + itemDay + '/' + itemHour;
            wsURL = hourURL.replace('http', 'ws') + '/ws'
        });

        it('opens websocket', function (done) {
            expect(wsURL).not.toEqual('undefined');

            webSocket = new WebSocket(wsURL);
            webSocket.onmessage = function (message) {
                console.log('received:', message.data);
                receivedMessages.push(message.data);
            };

            webSocket.onclose = function () {
                console.log('closed:', wsURL);
            };

            webSocket.on('open', function () {
                console.log('opened:', wsURL);
                setTimeout(done, 5000);
            });
        });

        it('posts item to channel', function (done) {
            utils.postItemQ(channelResource)
                .then(function (result) {
                    console.log('posted:', result.response.headers.location);
                    itemURLs.push(result.response.headers.location);
                    done();
                });
        });

        it('waits for data', function (done) {
            waitForMessages(receivedMessages, itemURLs, done);
        });

        it('verifies the correct data was received', function () {
            expect(receivedMessages.length).toEqual(itemURLs.length);
            for (var i = 0; i < itemURLs.length; ++i) {
                expect(receivedMessages).toContain(itemURLs[i]);
            }
        });

        it('closes websocket', function () {
            webSocket.close();
        });

    });

    describe('websocket minute endpoint', function () {

        var wsURL;
        var webSocket;
        var receivedMessages = [];

        it('builds websocket url', function () {
            expect(itemURLs.length).toEqual(3);
            var itemPath = url.parse(itemURLs[0]).pathname;
            var itemPathComponents = itemPath.split('/');
            var itemYear = itemPathComponents[3];
            var itemMonth = itemPathComponents[4];
            var itemDay = itemPathComponents[5];
            var itemHour = itemPathComponents[6];
            var itemMinute = itemPathComponents[7];
            var minuteURL = channelResource + '/' + itemYear + '/' + itemMonth + '/' + itemDay + '/' + itemHour + '/' + itemMinute;
            wsURL = minuteURL.replace('http', 'ws') + '/ws'
        });

        it('opens websocket', function (done) {
            expect(wsURL).not.toEqual('undefined');

            webSocket = new WebSocket(wsURL);
            webSocket.onmessage = function (message) {
                console.log('received:', message.data);
                receivedMessages.push(message.data);
            };

            webSocket.onclose = function () {
                console.log('closed:', wsURL);
            };

            webSocket.on('open', function () {
                console.log('opened:', wsURL);
                setTimeout(done, 5000);
            });
        });

        it('posts item to channel', function (done) {
            utils.postItemQ(channelResource)
                .then(function (result) {
                    console.log('posted:', result.response.headers.location);
                    itemURLs.push(result.response.headers.location);
                    done();
                });
        });

        it('waits for data', function (done) {
            waitForMessages(receivedMessages, itemURLs, done);
        });

        it('verifies the correct data was received', function () {
            expect(receivedMessages.length).toEqual(itemURLs.length);
            for (var i = 0; i < itemURLs.length; ++i) {
                expect(receivedMessages).toContain(itemURLs[i]);
            }
        });

        it('closes websocket', function () {
            webSocket.close();
        });

    });

    describe('websocket second endpoint', function () {

        var wsURL;
        var webSocket;
        var receivedMessages = [];

        it('builds websocket url', function () {
            expect(itemURLs.length).toEqual(4);
            var itemPath = url.parse(itemURLs[0]).pathname;
            var itemPathComponents = itemPath.split('/');
            var itemYear = itemPathComponents[3];
            var itemMonth = itemPathComponents[4];
            var itemDay = itemPathComponents[5];
            var itemHour = itemPathComponents[6];
            var itemMinute = itemPathComponents[7];
            var itemSecond = itemPathComponents[8];
            var secondURL = channelResource + '/' + itemYear + '/' + itemMonth + '/' + itemDay + '/' + itemHour + '/' + itemMinute + '/' + itemSecond;
            wsURL = secondURL.replace('http', 'ws') + '/ws'
        });

        it('opens websocket', function (done) {
            expect(wsURL).not.toEqual('undefined');

            webSocket = new WebSocket(wsURL);
            webSocket.onmessage = function (message) {
                console.log('received:', message.data);
                receivedMessages.push(message.data);
            };

            webSocket.onclose = function () {
                console.log('closed:', wsURL);
            };

            webSocket.on('open', function () {
                console.log('opened:', wsURL);
                setTimeout(done, 5000);
            });
        });

        it('posts item to channel', function (done) {
            utils.postItemQ(channelResource)
                .then(function (result) {
                    console.log('posted:', result.response.headers.location);
                    itemURLs.push(result.response.headers.location);
                    done();
                });
        });

        it('waits for data', function (done) {
            waitForMessages(receivedMessages, itemURLs, done);
        });

        it('verifies the correct data was received', function () {
            expect(receivedMessages.length).toEqual(itemURLs.length);
            for (var i = 0; i < itemURLs.length; ++i) {
                expect(receivedMessages).toContain(itemURLs[i]);
            }
        });

        it('closes websocket', function () {
            webSocket.close();
        });

    });

    describe('websocket hash endpoint', function () {

        var wsURL;
        var webSocket;
        var receivedMessages = [];

        it('builds websocket url', function () {
            expect(itemURLs.length).toEqual(5);
            wsURL = itemURLs[0].replace('http', 'ws') + '/ws'
        });

        it('opens websocket', function (done) {
            expect(wsURL).not.toEqual('undefined');

            webSocket = new WebSocket(wsURL);
            webSocket.onmessage = function (message) {
                console.log('received:', message.data);
                receivedMessages.push(message.data);
            };

            webSocket.onclose = function () {
                console.log('closed:', wsURL);
            };

            webSocket.on('open', function () {
                console.log('opened:', wsURL);
                setTimeout(done, 5000);
            });
        });

        it('posts item to channel', function (done) {
            utils.postItemQ(channelResource)
                .then(function (result) {
                    console.log('posted:', result.response.headers.location);
                    itemURLs.push(result.response.headers.location);
                    done();
                });
        });

        it('waits for data', function (done) {
            waitForMessages(receivedMessages, itemURLs.slice(1), done);
        });

        it('verifies the correct data was received', function () {
            var exclusiveItemURLs = itemURLs.slice(1);
            expect(receivedMessages.length).toEqual(exclusiveItemURLs.length);
            for (var i = 0; i < exclusiveItemURLs.length; ++i) {
                expect(receivedMessages).toContain(exclusiveItemURLs[i]);
            }
        });

        it('closes websocket', function () {
            webSocket.close();
        });

    });

});

function waitForMessages(actual, expected, done) {
    expect(actual).isPrototypeOf(Array);
    expect(expected).isPrototypeOf(Array);
    setTimeout(function () {
        if (actual.length !== expected.length) {
            waitForMessages(actual, expected, done);
        } else {
            console.log('expected:', expected);
            console.log('actual:', actual);
            done();
        }
    }, 500);
}
