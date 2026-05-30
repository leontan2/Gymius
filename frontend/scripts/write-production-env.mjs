import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const outputPath = resolve(scriptDir, '../src/environments/environment.prod.ts');
const rawApiUrl = process.env.FRONTEND_API_URL || 'http://localhost:8080';
const apiUrl = rawApiUrl
  .replace(/^FRONTEND_API_URL=/, '')
  .replace(/\/$/, '');

if (!/^https?:\/\//.test(apiUrl)) {
  throw new Error(`FRONTEND_API_URL must be an http(s) URL. Received: ${rawApiUrl}`);
}

mkdirSync(dirname(outputPath), { recursive: true });
writeFileSync(
  outputPath,
  `export const environment = {
  production: true,
  apiUrl: ${JSON.stringify(apiUrl)}
};
`
);

console.log(`Using production API URL: ${apiUrl}`);
