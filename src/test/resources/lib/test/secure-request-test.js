/* global require, exports */
var assert = require('/lib/xp/testing');
var http = require('/lib/http-client');

exports.withCertificatesGetRequest = function (serverUrl, providedCertificates) {

    const result = http.request({
        url: serverUrl,
        certificates: providedCertificates
    });

    var expectedJson = {
        "status": 200,
        "message": "OK",
        "body": "GET request",
        "contentType": "text/plain",
        "headers": {
            "Content-Length": "11",
            "content-type": "text/plain"
        },
        "cookies": []
    };

    assert.assertJsonEquals(expectedJson, result, 'http.request result not equals');

};
