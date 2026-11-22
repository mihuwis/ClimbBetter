import { existsSync } from 'node:fs';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const state = '.firebase/auth-state';
const args = [fileURLToPath(new URL('node_modules/firebase-tools/lib/bin/firebase.js', import.meta.url)),
  'emulators:start', '--only', 'auth', '--project', 'demo-climbbetter', '--export-on-exit', state];
if (existsSync(`${state}/firebase-export-metadata.json`)) args.push('--import', state);
const child = spawn(process.execPath, args, { stdio: 'inherit', cwd: fileURLToPath(new URL('.', import.meta.url)) });
for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => {
  // On Windows the console signal already reaches the CLI; allow it to export its state.
  if (process.platform !== 'win32') child.kill(signal);
});
child.on('exit', code => { process.exitCode = code ?? 1; });
