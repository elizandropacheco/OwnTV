const fs = require('node:fs');
const path = require('node:path');
const acorn = require('acorn');
for (const directory of ['app', 'service']) {
  const root = path.join(__dirname, '..', directory);
  for (const file of fs.readdirSync(root).filter(name => name.endsWith('.js'))) {
    acorn.parse(fs.readFileSync(path.join(root, file), 'utf8'), { ecmaVersion: 5 });
  }
}
console.log('All app and service JavaScript parses as ES5.');
