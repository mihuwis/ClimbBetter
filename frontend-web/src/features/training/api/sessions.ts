import { request } from '../../../shared/api/http';
import type { SessionCommand, SessionDetails } from '../model/session.types';
export const createSession = (command: SessionCommand) => request<{ sessionId: string }>('/training/sessions', { method: 'POST', body: JSON.stringify(command) });
export const updateSession = (id: string, version: number, reason: string, command: SessionCommand) =>
  request<SessionDetails>(`/training/sessions/${id}`, { method: 'PUT', body: JSON.stringify({ expectedVersion: version, changeReason: reason, session: command }) });
export const deleteSession = (id: string, version: number) => request<void>(`/training/sessions/${id}?expectedVersion=${version}`, { method: 'DELETE' });
