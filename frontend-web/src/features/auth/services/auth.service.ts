import { GoogleAuthProvider, signInWithCredential, signInWithPopup, signOut } from "firebase/auth";
import { authMode, requireFirebaseAuth } from "../../../shared/lib/firebase";
import { testAccounts } from "../model/test-accounts";

export function signInWithGoogle() {
  const provider = new GoogleAuthProvider();
  provider.setCustomParameters({ prompt: "select_account" });
  return signInWithPopup(requireFirebaseAuth(), provider);
}

export function signOutUser() {
  return signOut(requireFirebaseAuth());
}

export function signInAsTestClimber(id: string) {
  if (authMode !== "emulator") throw new Error("Local test accounts require the authentication emulator.");
  const account = testAccounts.find(account => account.id === id);
  if (!account) throw new Error("Unknown test account.");
  // The emulator accepts synthetic provider credentials. Real Firebase rejects these tokens.
  const credential = GoogleAuthProvider.credential(JSON.stringify({
    sub: account.id, email: `${account.id}@example.test`, email_verified: true, name: account.name,
  }));
  return signInWithCredential(requireFirebaseAuth(), credential);
}

export function signInError(error: unknown) {
  const code = typeof error === "object" && error !== null && "code" in error ? String(error.code) : "";
  if (code === "auth/popup-closed-by-user" || code === "auth/cancelled-popup-request") return "Sign-in was cancelled. You can try again.";
  if (code === "auth/popup-blocked") return "Allow the sign-in popup in your browser and try again.";
  if (code === "auth/network-request-failed") return "Check your connection and try signing in again.";
  return "Could not sign in. Please try again.";
}
