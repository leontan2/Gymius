import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const outputPath = resolve(scriptDir, '../src/environments/environment.prod.ts');
const apiUrl = (process.env.FRONTEND_API_URL || 'http://localhost:8080').replace(/\/$/, '');

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
