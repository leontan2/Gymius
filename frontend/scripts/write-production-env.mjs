import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const outputPath = resolve(scriptDir, '../src/environments/environment.generated.ts');
const rawApiUrl = process.env.FRONTEND_API_URL?.trim();

if (!rawApiUrl) {
  throw new Error('FRONTEND_API_URL is required for production builds.');
}

const apiUrl = rawApiUrl
  .replace(/^FRONTEND_API_URL=/, '')
  .replace(/\/$/, '');

let parsedUrl;
try {
  parsedUrl = new URL(apiUrl);
} catch {
  throw new Error(`FRONTEND_API_URL must be a valid URL. Received: ${rawApiUrl}`);
}

const isLocal = ['localhost', '127.0.0.1', '::1'].includes(parsedUrl.hostname);
if (parsedUrl.protocol !== 'https:' && !(isLocal && parsedUrl.protocol === 'http:')) {
  throw new Error('FRONTEND_API_URL must use HTTPS unless it targets localhost.');
}

if (parsedUrl.username || parsedUrl.password || parsedUrl.search || parsedUrl.hash) {
  throw new Error('FRONTEND_API_URL must not include credentials, query parameters, or a fragment.');
}

mkdirSync(dirname(outputPath), { recursive: true });
writeFileSync(
  outputPath,
  `export const environment = {
  production: true,
  apiUrl: ${JSON.stringify(apiUrl)},
  devAuthBypassEnabled: false
};
`
);

console.log(`Using production API URL: ${apiUrl}`);
