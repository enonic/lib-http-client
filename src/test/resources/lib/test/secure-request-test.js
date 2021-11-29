/* global require, exports */
var assert = require('/lib/xp/testing');
var http = require('/lib/http-client');

exports.withCertificatesGetRequest = function (serverUrl, serverCertificate) {
    const result = http.request({
        url: serverUrl,
        certificates: serverCertificate
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

exports.withNoCertificatesGetRequest = function (serverUrl) {
    const result = http.request({
        url: serverUrl,
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

exports.withClientCertificatesGetRequest = function (serverUrl, serverCertificate, clientCertificate) {
    const result = http.request({
        url: serverUrl,
        certificates: serverCertificate,
        clientCertificate: clientCertificate,
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

exports.withClientCertificateAliasGetRequest = function (serverUrl, serverCertificate, clientCertificate) {
    const result = http.request({
        url: serverUrl,
        certificates: serverCertificate,
        clientCertificate: 'client',
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

exports.withNoClientCertificatesGetRequest = function (serverUrl, serverCertificate) {
    const result = http.request({
        url: serverUrl,
        certificates: serverCertificate,
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
