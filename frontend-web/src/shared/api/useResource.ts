import { useEffect, useState } from 'react';
import { request } from './http';

export function useResource<T>(path: string | null) {
  const [revision, setRevision] = useState(0);
  const key = `${path}:${revision}`;
  const [result, setResult] = useState<{ key: string; data: T | null; error: unknown } | null>(null);
  useEffect(() => {
    if (!path) return;
    const controller = new AbortController();
    let active = true;
    request<T>(path, { signal: controller.signal })
      .then(data => { if (active) setResult({ key, data, error: null }); })
      .catch(error => { if (active) setResult({ key, data: null, error }); });
    return () => { active = false; controller.abort(); };
  }, [path, key]);
  const current = result?.key === key ? result : null;
  return {
    data: current?.data ?? null,
    error: current?.error ?? null,
    loading: !!path && !current,
    refresh: () => setRevision(value => value + 1),
  };
}
