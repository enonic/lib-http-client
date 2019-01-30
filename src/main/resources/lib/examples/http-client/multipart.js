var httpClientLib = require('/lib/http-client');

function getServerHost() {
    return testInstance.getServerHost();
}
var myImageStream = testInstance.getImageStream();

// BEGIN
var response = httpClientLib.request({
    url: 'http://' + getServerHost() + '/uploadMedia',
    method: 'POST',
    contentType: 'multipart/mixed',
    multipart: [
        {
            name: 'media',
            fileName: 'logo.png',
            contentType: 'image/png',
            value: myImageStream
        },
        {
            name: 'category',
            value: 'images'
        }
    ]
});
// END
