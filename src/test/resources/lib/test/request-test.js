var assert = require('/lib/xp/testing');
var http = require('/lib/http-client');

exports.simpleGetRequest = function (mockServer) {

    var result = http.request({
        url: 'http://' + mockServer + '/my/url'
    });

    var expectedJson = {
        "status": 200,
        "message": "OK",
        "body": "GET request",
        "contentType": "text/plain",
        "headers": {
            "content-length": "11",
            "content-type": "text/plain"
        },
        "cookies": []
    };

    assert.assertJsonEquals(expectedJson, result, 'http.request result not equals');

};

exports.simplePostRequest = function (mockServer) {

    var result = http.request({
        url: 'http://' + mockServer + '/my/url',
        method: 'post',
        body: 'POST body'
    });

    var expectedJson = {
        "status": 200,
        "message": "OK",
        "body": "POST request",
        "contentType": "text/plain",
        "headers": {
            "content-length": "12",
            "content-type": "text/plain"
        },
        "cookies": []
    };

    assert.assertJsonEquals(expectedJson, result, 'http.request result not equals');

};

exports.simplePatchRequest = function (mockServer) {

    var result = http.request({
        url: 'http://' + mockServer + '/my/url',
        method: 'patch',
        body: 'PATCH body'
    });

    var expectedJson = {
        "status": 200,
        "message": "OK",
        "body": "PATCH request",
        "contentType": "text/plain",
        "headers": {
            "content-length": "13",
            "content-type": "text/plain"
        },
        "cookies": []
    };

    assert.assertJsonEquals(expectedJson, result, 'http.request result not equals');
};

exports.simpleHeadRequest = function (mockServer) {

    var result = http.request({
        url: 'http://' + mockServer + '/my/url',
        method: 'head'
    });

    var expectedJson = {
        "status": 200,
        "message": "OK",
        "body": "",
        "contentType": "text/plain",
        "headers": {
            "content-length": "11",
            "content-type": "text/plain"
        },
        "cookies": []
    };

    assert.assertJsonEquals(expectedJson, result, 'http.request result not equals');

};

exports.getRequestWithParams = function (mockServer) {

    var result = http.request({
        url: 'http://' + mockServer + '/my/url',
        method: 'get',
        params: {
            'a': 123,
            'b': 456,
            'c': null
        }
    });

    var expectedJson = {
        "status": 200,
        "message": "OK",
        "body": "GET request",
        "contentType": "text/plain",
        "headers": {
            "content-length": "11",
            "content-type": "text/plain"
        },
        "cookies": []
    };

    assert.assertJsonEquals(expectedJson, result, 'http.request result not equals');
    assert.assertNotNull(result.bodyStream, 'http.request stream body null');

};

exports.postRequestWithParams = function (mockServer) {

    var result = http.request({
        url: 'http://' + mockServer + '/my/url',
        method: 'post',
        params: {
            'a': 123,
            'b': 456,
            'c': null
        },
        queryParams: {
            p1: 123,
            p2: 'something'
        }
    });

    var expectedJson = {
        "status": 200,
        "message": "OK",
        "body": "POST request",
        "contentType": "text/plain",
        "headers": {
            "content-length": "12",
            "content-type": "text/plain"
        },
        "cookies": []
    };

    assert.assertJsonEquals(expectedJson, result, 'http.request result not equals');

};

exports.postJsonRequest = function (mockServer) {

    var result = http.request({
        url: 'http://' + mockServer + '/my/url',
        method: 'post',
        contentType: 'application/json; charset=utf-8',
        body: JSON.stringify({'a': 123, 'b': 456})
    });


    var expectedJson = {
        "status": 200,
        "message": "OK",
        "body": "POST request",
        "contentType": "text/plain",
        "headers": {
            "content-length": "12",
            "content-type": "text/plain"
        },
        "cookies": []
    };

    assert.assertJsonEquals(expectedJson, result, 'http.request result not equals');

};

exports.postImageRequest = function (mockServer, byteSource) {

    var result = http.request({
        url: 'http://' + mockServer + '/my/url',
        method: 'post',
        contentType: 'image/png',
        body: byteSource
    });


    var expectedJson = {
        "status": 200,
        "message": "OK",
        "contentType": "image/png",
        "headers": {
            "content-length": "10485761",
            "content-type": "image/png"
        },
        "cookies": []
    };

    assert.assertJsonEquals(expectedJson, result, 'http.request result not equals');

};
exports.getWithHeadersRequest = function (mockServer) {

    var result = http.request({
        url: 'http://' + mockServer + '/my/url',
        method: 'get',
        headers: {
            'X-Custom-Header': 'some-value'
        }
    });

    var expectedJson = {
        "status": 200,
        "message": "OK",
        "body": "GET request",
        "contentType": "text/plain",
        "headers": {
            "content-length": "11",
            "content-type": "text/plain"
        },
        "cookies": []
    };

    assert.assertJsonEquals(expectedJson, result, 'http.request result not equals');

};

exports.getWithResponseTimeout = function (mockServer) {

    try {
        http.request({
            url: 'http://' + mockServer + '/my/url',
            method: 'get',
            readTimeout: 1000
        });

        assert.assertTrue(false, 'Expected exception');

    } catch (e) {
        assert.assertEquals("closed", e.message);
    }
};

exports.getWithConnectTimeout = function (mockServer) {

    try {
        http.request({
            url: 'http://' + mockServer + '/my/url',
            method: 'get',
            connectionTimeout: 1000
        });
        assert.assertTrue(false, 'Expected exception');

    } catch (e) {
        assert.assertEquals("couldn't receive headers on time", e.message);
    }

};

exports.requestWithProxy = function (mockServer, proxyHost, proxyPort) {

    var result = http.request({
        url: 'http://' + mockServer + '/my/url',
        method: 'post',
        proxy: {
            host: proxyHost,
            port: proxyPort
        }
    });

    log.info(JSON.stringify(result, null, 2));

    var expectedJson = {
        "status": 200,
        "message": "OK",
        "body": "POST request",
        "contentType": "text/plain",
        "headers": {
            "content-length": "12",
            "content-type": "text/plain"
        },
        "cookies": []
    };

    assert.assertJsonEquals(expectedJson, result, 'http.request result not equals');

};

exports.requestWithProxyAuth = function (mockServer, proxyHost, proxyPort) {

    var result = http.request({
        url: 'http://' + mockServer + '/my/url',
        method: 'post',
        proxy: {
            host: proxyHost,
            port: proxyPort,
            user: 'admin',
            password: 'secret'
        }
    });

    var expectedJson = {
        "status": 200,
        "message": "OK",
        "body": "POST request authenticated",
        "contentType": "text/plain",
        "headers": {
            "content-length": "26",
            "content-type": "text/plain"
        },
        "cookies": []
    };

    assert.assertJsonEquals(expectedJson, result, 'http.request result not equals');

};

exports.cookies = function (mockServer) {

    var result = http.request({
        url: 'http://' + mockServer + '/my/url',
        method: 'GET'
    });

    var expectedJson = {
        'status': 200,
        'message': 'OK',
        'body': 'GET request',
        "contentType": "text/plain",
        'headers': {
            'content-length': '11',
            'content-type': 'text/plain',
            'set-cookie': "a=b; Domain=example.com; Path=/docs; Secure; HttpOnly"
        },
        'cookies': [{
            'name': 'a',
            'value': 'b',
            'path': '/docs',
            'domain': 'example.com',
            'secure': true,
            'httpOnly': true
        }]
    };

    assert.assertJsonEquals(expectedJson, result, 'http.request result not equals');
};

exports.requestWithSoapResponse = function (mockServer) {

    var result = http.request({
        url: 'http://' + mockServer + '/my/url',
        method: 'get',
    });

    var expectedJson = {
        'status': 200,
        'message': 'OK',
        'body': '<?xml version="1.0" encoding="utf-8"?><body/>',
        'contentType': 'application/soap+xml; charset=utf-8',
        'headers': {
            'content-length': '45',
            'content-type': 'application/soap+xml; charset=utf-8'
        },
        'cookies': []
    };

    assert.assertJsonEquals(expectedJson, result, 'http.request result not equals');
    assert.assertNotNull(result.bodyStream, 'http.request stream body null');

};