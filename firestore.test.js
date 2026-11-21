const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any Rafik Hafiz collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("juzProgress").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .set({
        userId: ALICE_UID,
        displayName: "Guest",
        dailyRevisionTargetPages: 20,
        zikrFontScale: 1.0,
        totalRevisionsCount: 0,
        totalTasbihCount: 0,
        createdAt: past,
        updatedAt: past,
      })
  );
});

test("Owner can manage UserProfile, juzProgress, tajweedProgress, dailyLogs, reflections; rejects cross-user & ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "عمر الحافظ",
      dailyRevisionTargetPages: 20,
      zikrFontScale: 1.1,
      totalRevisionsCount: 5,
      totalTasbihCount: 33,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      dailyRevisionTargetPages: 30,
      totalRevisionsCount: 6,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // juzProgress
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("juzProgress")
      .doc("juz_30")
      .set({
        userId: ALICE_UID,
        juzNumber: 30,
        masteryLevel: "mastered",
        revisionCount: 4,
        mistakeNote: "مراجعة سورة التكوير آية ١٥",
        lastReviewedEpochMs: Date.now() - 5000,
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("juzProgress")
      .doc("juz_30")
      .update({
        revisionCount: 5,
        masteryLevel: "mastered",
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Invalid juzNumber rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("juzProgress")
      .doc("juz_31")
      .set({
        userId: ALICE_UID,
        juzNumber: 31,
        masteryLevel: "mastered",
        revisionCount: 1,
        mistakeNote: "",
        lastReviewedEpochMs: 0,
        createdAt: created,
        updatedAt: created,
      })
  );

  // tajweedProgress
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tajweedProgress")
      .doc("noon_idgham")
      .set({
        userId: ALICE_UID,
        ruleKey: "noon_idgham",
        mastered: true,
        createdAt: created,
        updatedAt: created,
      })
  );

  // dailyLogs
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("dailyLogs")
      .doc("2026-11-20")
      .set({
        userId: ALICE_UID,
        dateKey: "2026-11-20",
        pagesRevisedToday: 20,
        morningAzkarDone: true,
        eveningAzkarDone: false,
        tasbihDoneToday: 66,
        tajweedQuizCorrect: 3,
        createdAt: created,
        updatedAt: created,
      })
  );

  // reflections
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reflections")
      .doc("note_1")
      .set({
        userId: ALICE_UID,
        title: "ضبط المتشابهات في البقرة",
        body: "التفريق بين وإذ قلنا ادخلوا هذه القرية في البقرة والأعراف",
        moodTag: "mutashabihat",
        dateKey: "2026-11-20",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Bob cannot read or list Alice's data
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("juzProgress").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("juzProgress").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tajweedProgress").get()
  );

  // gamification
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("gamification")
      .doc("summary")
      .set({
        userId: ALICE_UID,
        xp: 120,
        currentStreakDays: 2,
        longestQuizCombo: 4,
        quizzesCompleted: 1,
        perfectQuizzes: 1,
        totalCorrectAnswers: 5,
        surahOpenersSolved: 1,
        surahClosersSolved: 1,
        mutashabihatSolved: 0,
        testedJuzCsv: "30",
        fullQuranQuizDone: false,
        unlockedBadgesCsv: "first_test,perfect_score",
        lastActiveDateKey: "2026-11-20",
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("quizAttempts")
      .doc("attempt_1")
      .set({
        userId: ALICE_UID,
        scopeLabel: "الجزء ٣٠",
        questionTypesCsv: "COMPLETE_AYAH,SURAH_LAST_5",
        score: 5,
        totalQuestions: 5,
        xpEarned: 120,
        bestCombo: 5,
        dateKey: "2026-11-20",
        createdAt: created,
      })
  );

  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("gamification")
      .doc("summary")
      .get()
  );
});
