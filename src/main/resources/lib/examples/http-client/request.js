var httpClientLib = require('/lib/http-client');
var assert = require('/lib/xp/testing');

function getServerHost() {
    return testInstance.getServerHost();
}

// BEGIN
var response = httpClientLib.request({
    url: 'http://' + getServerHost() + '/my/service',
    method: 'POST',
    headers: {
        'X-Custom-Header': 'header-value'
    },
    connectionTimeout: 20000,
    readTimeout: 5000,
    body: '{"id": 123}',
    contentType: 'application/json'
});
// END

// BEGIN
// Expected result from request.
var expected = {
    'status': 200,
    'message': 'OK',
    'body': 'POST request',
    'contentType': 'text/plain',
    'headers': {
        'content-length': '12',
        'content-type': 'text/plain'
    },
    "cookies": []
};
// END

assert.assertJsonEquals(expected, response);
