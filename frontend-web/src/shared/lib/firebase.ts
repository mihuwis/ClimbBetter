import { getApps, initializeApp } from "firebase/app";
import { browserLocalPersistence, browserPopupRedirectResolver, connectAuthEmulator, getAuth, initializeAuth, type Auth } from "firebase/auth";

export const authMode = import.meta.env.VITE_AUTH_MODE ?? "firebase";
export const existingAccountOnly = authMode === "firebase" && import.meta.env.VITE_AUTH_ACCOUNT_MODE !== "register";

function configure(): { auth: Auth | null; error: string | null } {
  try {
    if (!["firebase", "emulator", "stub"].includes(authMode)) throw new Error("Unknown sign-in configuration.");
    const local = import.meta.env.DEV && ["localhost", "127.0.0.1"].includes(window.location.hostname);
    if (authMode !== "firebase" && !local) throw new Error("Test sign-in is available only on local development servers.");
    if (authMode === "stub") return { auth: null, error: null };
    const emulator = authMode === "emulator";
    const config = {
      apiKey: import.meta.env.VITE_FIREBASE_API_KEY || (emulator ? "local-emulator" : ""),
      authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || (emulator ? "localhost" : ""),
      projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || (emulator ? "demo-climbbetter" : ""),
      appId: import.meta.env.VITE_FIREBASE_APP_ID || (emulator ? "local-climbbetter" : ""),
    };
    if (Object.values(config).some(value => !value)) throw new Error("Sign-in has not been configured for this application yet.");
    if (emulator && !config.projectId.startsWith("demo-")) throw new Error("Local test accounts require a demo project.");
    const existing = getApps().find(app => app.name === "climbbetter");
    const app = existing ?? initializeApp(config, "climbbetter");
    const auth = existing ? getAuth(app) : initializeAuth(app, {
      persistence: browserLocalPersistence,
      popupRedirectResolver: browserPopupRedirectResolver,
    });
    if (emulator && !auth.emulatorConfig) {
      const endpoint = new URL(import.meta.env.VITE_FIREBASE_AUTH_EMULATOR_URL ?? "http://127.0.0.1:9099");
      if (endpoint.protocol !== "http:" || !["localhost", "127.0.0.1"].includes(endpoint.hostname)
        || endpoint.username || endpoint.password || endpoint.pathname !== "/" || endpoint.search || endpoint.hash) {
        throw new Error("Local test accounts require a local authentication emulator.");
      }
      connectAuthEmulator(auth, endpoint.origin, { disableWarnings: true });
    }
    return { auth, error: null };
  } catch (error) {
    return { auth: null, error: error instanceof Error ? error.message : "Sign-in configuration is unavailable." };
  }
}

const configured = configure();
export const firebaseAuth = configured.auth;
export const firebaseSetupError = configured.error;

export function requireFirebaseAuth(): Auth {
  if (!firebaseAuth) throw new Error(firebaseSetupError ?? "Sign-in is unavailable.");
  return firebaseAuth;
}
