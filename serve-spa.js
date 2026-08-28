const http = require('http');
const fs = require('fs');
const path = require('path');

const DIST = path.join(__dirname, 'bibliotheque-frontend/dist/bibliotheque-frontend/browser');
const PORT = 4200;

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript',
  '.css': 'text/css',
  '.ico': 'image/x-icon',
  '.json': 'application/json',
};

const indexHtml = fs.readFileSync(path.join(DIST, 'index.html'));

const server = http.createServer((req, res) => {
  const url = req.url.split('?')[0];
  const filePath = path.join(DIST, url === '/' ? 'index.html' : url);
  const ext = path.extname(filePath);

  if (ext && fs.existsSync(filePath)) {
    res.writeHead(200, { 'Content-Type': MIME[ext] || 'application/octet-stream' });
    fs.createReadStream(filePath).pipe(res);
  } else {
    // SPA fallback: return index.html for all routes
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(indexHtml);
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`SPA server running at http://localhost:${PORT}`);
});
