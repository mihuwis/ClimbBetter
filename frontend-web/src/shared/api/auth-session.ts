let generation = 0;
let controller = new AbortController();

export function advanceAuthSession() {
  controller.abort();
  controller = new AbortController();
  generation += 1;
}

export function authSession() {
  return { generation, signal: controller.signal };
}

export function assertAuthSession(expected: number) {
  if (generation !== expected) throw new DOMException("The signed-in account changed.", "AbortError");
}
