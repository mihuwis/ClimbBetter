import { request } from "../../../shared/api/http";
import type { CurrentUser } from "../model/profile.types";

export const getProfile = () => request<CurrentUser>("/identity/me");
export const bootstrapProfile = () => request<CurrentUser>("/identity/bootstrap", { method: "POST" });
export const saveProfile = (displayName: string) => request<CurrentUser>("/identity/me", {
  method: "PUT", body: JSON.stringify({ displayName }),
});
