const express = require('express');
const awsServerlessExpress = require('aws-serverless-express');
const awsServerlessExpressMiddleware = require('aws-serverless-express/middleware');
const path = require('path');

const app = express();

app.use(awsServerlessExpressMiddleware.eventContext())
app.get('/config', (req, res) => res.json({
    'someConfig': `hello ${new Date()}`
}));

const pathToStatic = path.join(__dirname, '/static');
app.use(express.static(pathToStatic));
app.get('*', (req, res) => {
    res.sendFile(path.join(pathToStatic, 'index.html'));
});

const server = awsServerlessExpress.createServer(app, null, [
    'application/javascript',
    'application/json',
    'application/octet-stream',
    'application/xml',
    'font/eot',
    'font/opentype',
    'font/otf',
    'image/jpeg',
    'image/png',
    'image/svg+xml',
    'text/comma-separated-values',
    'text/css',
    'text/html',
    'text/javascript',
    'text/plain',
    'text/text',
    'text/xml'
]);
exports.handler = (event, context) => awsServerlessExpress.proxy(server, event, context);
