import { useCallback, useEffect, useLayoutEffect, useRef, useState } from "react";
import { useLocation, useNavigationType } from "react-router-dom";
import { useAuth } from "../../auth/hooks/useAuth";
import type { SessionSummary } from "../../training/model/session.types";
import { request } from "../../../shared/api/http";

type Page = { items: SessionSummary[]; nextCursor: string | null };
type Snapshot = Page & { pages: number; scrollY: number };
type Feed = Snapshot & { loading: boolean; error: unknown };
const snapshots = new Map<string, Snapshot>();
let snapshotGeneration = -1;

export function useSessionFeed() {
  const { key, state } = useLocation();
  const navigation = useNavigationType();
  const auth = useAuth();
  const cacheKey = `${auth.generation}:${auth.profile?.userId}:${key}`;
  const [initial] = useState(() => {
    const returnKey = state?.dashboardReturnKey ?? (navigation === "POP" ? key : null);
    return returnKey && snapshotGeneration === auth.generation
      ? snapshots.get(`${auth.generation}:${auth.profile?.userId}:${returnKey}`) : undefined;
  });
  const [feed, setFeed] = useState<Feed>(() => ({
    items: initial?.items ?? [], nextCursor: initial?.nextCursor ?? null,
    pages: initial?.pages ?? 0, scrollY: initial?.scrollY ?? 0, loading: true, error: null,
  }));
  const current = useRef(feed);
  const inFlight = useRef(false);
  const failedAppend = useRef(false);
  const controller = useRef<AbortController | null>(null);

  const load = useCallback(async (append: boolean) => {
    if (inFlight.current) return;
    inFlight.current = true;
    const abort = new AbortController();
    controller.current = abort;
    const previous = current.current;
    const publish = (value: Feed) => { current.current = value; setFeed(value); };
    publish({ ...previous, loading: true, error: null });
    try {
      let cursor = append ? previous.nextCursor : null;
      let items = append ? previous.items : [];
      let pages = append ? previous.pages : 0;
      const count = append ? 1 : Math.max(1, previous.pages);
      for (let index = 0; index < count; index++) {
        const query = new URLSearchParams({ limit: "20" });
        if (cursor) query.set("cursor", cursor);
        const page = await request<Page>(`/dashboard/sessions/page?${query}`, { signal: abort.signal });
        abort.signal.throwIfAborted();
        const known = new Set(items.map(item => item.sessionId));
        items = [...items, ...page.items.filter(item => !known.has(item.sessionId))];
        cursor = page.nextCursor;
        pages += 1;
        if (!cursor) break;
      }
      publish({ items, nextCursor: cursor, pages, scrollY: previous.scrollY, loading: false, error: null });
    } catch (error) {
      if (!abort.signal.aborted) {
        failedAppend.current = append;
        publish({ ...previous, loading: false, error });
      }
    } finally {
      inFlight.current = false;
    }
  }, []);

  useLayoutEffect(() => {
    if (snapshotGeneration !== auth.generation) {
      snapshots.clear();
      snapshotGeneration = auth.generation;
    }
    const previousRestoration = window.history.scrollRestoration;
    window.history.scrollRestoration = "manual";
    window.scrollTo(0, initial?.scrollY ?? 0);
    const rememberScroll = () => { current.current = { ...current.current, scrollY: window.scrollY }; };
    window.addEventListener("scroll", rememberScroll, { passive: true });
    return () => {
      window.removeEventListener("scroll", rememberScroll);
      window.history.scrollRestoration = previousRestoration;
      const value = current.current;
      if (value.pages > 0) snapshots.set(cacheKey, { ...value, scrollY: window.scrollY });
      while (snapshots.size > 10) snapshots.delete(snapshots.keys().next().value!);
    };
  }, [cacheKey, initial, auth.generation]);

  useEffect(() => {
    // StrictMode's first cleanup must cancel before the initial request starts.
    let active = true;
    queueMicrotask(() => { if (active) void load(false); });
    return () => { active = false; controller.current?.abort(); };
  }, [load]);

  const loadMore = useCallback(() => {
    if (current.current.nextCursor && !current.current.error) void load(true);
  }, [load]);
  const retry = useCallback(() => { void load(failedAppend.current); }, [load]);
  return { ...feed, loadMore, retry };
}
