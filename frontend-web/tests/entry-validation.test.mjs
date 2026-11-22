import { test } from "node:test";
import assert from "node:assert/strict";
import { completedMoveIssue } from "../src/features/training/model/entry-validation.ts";
import {
  commandFromDraft,
  newDraft,
} from "../src/features/training/model/session-draft.ts";

test("completed climbing requires the full length while attempts retain partial and repeated moves", () => {
  for (const climbType of ["BOULDER", "ROUTE", "CIRCUIT"]) {
    assert.match(
      completedMoveIssue({
        resultType: "ASCENT",
        climbType,
        totalMoves: 8,
        executedMoves: 2,
      }),
      /exactly 8/,
    );
    assert.equal(
      completedMoveIssue({
        resultType: "ASCENT",
        climbType,
        totalMoves: 8,
        executedMoves: 8,
      }),
      null,
    );
    assert.match(
      completedMoveIssue({
        resultType: "ASCENT",
        climbType,
        totalMoves: 8,
        executedMoves: 10,
      }),
      /exactly 8/,
    );
    assert.equal(
      completedMoveIssue({
        resultType: "ATTEMPT",
        climbType,
        totalMoves: 8,
        executedMoves: 2,
      }),
      null,
    );
    assert.equal(
      completedMoveIssue({
        resultType: "ATTEMPT",
        climbType,
        totalMoves: 8,
        executedMoves: 10,
      }),
      null,
    );
  }
  assert.equal(
    completedMoveIssue({ resultType: "WARMUP", executedMoves: 40 }),
    null,
  );
});

test("a restored incomplete completed entry cannot bypass the session save validation", () => {
  const draft = newDraft("user", "manual");
  draft.command.areaId = "area";
  draft.command.entries = [
    {
      clientEntryId: "old",
      entryOrder: 0,
      resultType: "ASCENT",
      totalMoves: 8,
      executedMoves: 2,
    },
  ];
  assert.throws(() => commandFromDraft(draft), /Entry #1:.*exactly 8/);
  draft.command.entries[0].resultType = "ATTEMPT";
  assert.equal(commandFromDraft(draft).entries[0].executedMoves, 2);
});

test("retry preserves the exact pending request even if a legacy completed entry is incomplete", () => {
  const draft = newDraft("user", "manual");
  draft.pendingRequest = {
    ...draft.command,
    areaId: "area",
    entries: [
      {
        clientEntryId: "old",
        entryOrder: 0,
        resultType: "ASCENT",
        totalMoves: 8,
        executedMoves: 2,
      },
    ],
  };
  assert.equal(commandFromDraft(draft), draft.pendingRequest);
});
