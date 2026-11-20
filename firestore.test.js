const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_user_1";
const BOB_UID = "bob_user_2";
const CAROL_UID = "carol_user_3";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 5000) {
  return new Date(Date.now() - msOffset);
}

function validUserAccount(uid) {
  const ts = pastDate(10000);
  return {
    userId: uid,
    email: `${uid}@example.com`,
    age: 27,
    dobYear: 1999,
    dobMonth: 5,
    dobDay: 14,
    termsAccepted: true,
    safetyConsent: true,
    browseOnly: false,
    selfieVerified: false,
    country: "KE",
    locale: "en",
    whoCanMessageFirst: "verified_inbox",
    allowRoses: true,
    standoutsMuted: false,
    liteMode: false,
    rosesBalance: 1,
    rosesSentWeek: 0,
    strikes: 0,
    suspensionUntilMs: 0,
    onboardingComplete: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validPublicProfile(uid) {
  const ts = pastDate(10000);
  return {
    userId: uid,
    firstName: "Amara",
    age: 27,
    gender: "woman",
    interestedIn: ["men"],
    area: "Kilimani",
    lat: -1.2921,
    lng: 36.7856,
    lookingFor: "long_term",
    bio: "Software dev who loves hiking Ngong Hills and coffee.",
    promptQuestion: "My ideal Sunday",
    promptAnswer: "Early trail walk followed by espresso in Kilimani.",
    interests: ["Hiking", "Coffee", "Design"],
    languages: ["en", "sw"],
    work: "Product Designer",
    selfieVerified: true,
    trustScore: 88,
    prefAgeMin: 24,
    prefAgeMax: 35,
    distMaxKm: 25,
    verifiedOnly: true,
    showInNearby: true,
    paused: false,
    incognito: false,
    status: "active",
    createdAt: ts,
    updatedAt: ts,
  };
}

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

test("Unauthenticated requests are denied across all collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("publicProfiles").get());
  await assertFails(unauth.collection("connections").get());
  await assertFails(unauth.collection("messageRequests").get());
  await assertFails(unauth.collection("reports").get());
});

test("UserAccount: owner create/update succeeds; under-18, shadow field, immutable age/createdAt, and cross-user access fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Valid create
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserAccount(ALICE_UID))
  );

  // Under-18 age rejected
  const under18 = { ...validUserAccount(BOB_UID), age: 17 };
  await assertFails(bobDb.collection("users").doc(BOB_UID).set(under18));

  // Future timestamp rejected
  const futureTs = new Date(Date.now() + 60000);
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...validUserAccount(BOB_UID),
      createdAt: futureTs,
      updatedAt: futureTs,
    })
  );

  // Shadow update (ghost field) rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(500),
    })
  );

  // Valid partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      selfieVerified: true,
      rosesBalance: 2,
      updatedAt: pastDate(500),
    })
  );

  // Immutable age mutation rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      age: 30,
      updatedAt: pastDate(400),
    })
  );

  // Cross-user read & write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      strikes: 5,
      updatedAt: pastDate(400),
    })
  );
});

test("PrivatePrefs: strictly owner-only isolation (other users cannot read or write)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const prefs = {
    userId: ALICE_UID,
    consented: true,
    consentVersion: 1,
    pace: "gradual",
    paceDealbreaker: false,
    exclusivity: "exclusive",
    exclusivityDealbreaker: true,
    chemWeight: "medium",
    discretion: "selective",
    flirtLevel: "sweet",
    place: "own_place",
    updatedAt: pastDate(2000),
  };

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("privatePrefs")
      .doc("compatibility")
      .set(prefs)
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("privatePrefs")
      .doc("compatibility")
      .get()
  );

  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("privatePrefs")
      .doc("compatibility")
      .get()
  );

  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("privatePrefs")
      .get()
  );
});

test("ProtectedPins, Swipes, Blocks, and SafeDates: owner-only CRUD & bounds enforced", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(3000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("pins").doc("pin_1").set({
      userId: ALICE_UID,
      label: "Office Westlands",
      areaName: "Westlands",
      lat: -1.267,
      lng: 36.811,
      radiusM: 2000,
      active: true,
      pausedUntilMs: 0,
      createdAt: ts,
    })
  );

  // Invalid radius rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("pins").doc("pin_2").set({
      userId: ALICE_UID,
      label: "Home",
      areaName: "Kilimani",
      lat: -1.29,
      lng: 36.78,
      radiusM: 500,
      active: true,
      pausedUntilMs: 0,
      createdAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("swipes").doc(BOB_UID).set({
      actorId: ALICE_UID,
      targetId: BOB_UID,
      targetName: "David",
      targetArea: "Westlands",
      action: "like_note",
      note: "Love Ngong Hills trails too!",
      roseAttached: false,
      reintroduced: false,
      ladderUsed: ["note"],
      createdAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("blocks").doc(BOB_UID).set({
      blockerId: ALICE_UID,
      blockedUid: BOB_UID,
      blockedName: "Bob",
      reasonCategory: "scam",
      evidenceSnapshot: "Asked for fare via M-Pesa",
      evidenceRetainedUntilMs: Date.now() + 90 * 86400000,
      createdAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("safeDates").doc("sd_1").set({
      userId: ALICE_UID,
      connectionId: "conn_1",
      partnerName: "David",
      place: "Artcaffe Junction Mall",
      meetAtLabel: "Sat 4:00 PM",
      trustedContact: "+254712345678",
      status: "scheduled",
      createdAt: ts,
    })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("blocks").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("pins").get()
  );
});

test("PublicProfiles: signed-in discovery read allowed; owner-only write with @DocumentId exclusion", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("publicProfiles").doc(ALICE_UID).set(validPublicProfile(ALICE_UID))
  );

  // @DocumentId `id` field forbidden in payload
  await assertFails(
    bobDb.collection("publicProfiles").doc(BOB_UID).set({
      id: BOB_UID,
      ...validPublicProfile(BOB_UID),
    })
  );

  // Bob can read Alice's public profile, cannot mutate it
  await assertSucceeds(bobDb.collection("publicProfiles").get());
  await assertFails(
    bobDb.collection("publicProfiles").doc(ALICE_UID).update({
      bio: "Hacked",
      updatedAt: pastDate(500),
    })
  );
});

test("MessageRequests, Connections, Messages & Reports: member-scoped queries succeed, unfiltered fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const carolDb = testEnv.authenticatedContext(CAROL_UID).firestore();
  const ts = pastDate(5000);

  // Message request from Alice to Bob
  await assertSucceeds(
    aliceDb.collection("messageRequests").doc("req_1").set({
      senderId: ALICE_UID,
      senderName: "Amara",
      senderArea: "Kilimani",
      senderVerified: true,
      receiverId: BOB_UID,
      receiverName: "David",
      memberUids: [ALICE_UID, BOB_UID],
      body: "Hi David! Which Ngong Hills trail is your favorite?",
      status: "pending",
      roseAttached: false,
      sensitive: false,
      effortReasons: ["verified", "note_on_prompt"],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Carol cannot read or list Alice/Bob's requests
  await assertFails(carolDb.collection("messageRequests").doc("req_1").get());
  await assertFails(carolDb.collection("messageRequests").get());
  await assertSucceeds(
    bobDb.collection("messageRequests").where("memberUids", "array-contains", BOB_UID).get()
  );

  // Connection & Message
  await assertSucceeds(
    aliceDb.collection("connections").doc("conn_1").set({
      memberUids: [ALICE_UID, BOB_UID],
      userAId: ALICE_UID,
      userAName: "Amara",
      userBId: BOB_UID,
      userBName: "David",
      userBArea: "Westlands",
      userBVerified: true,
      lastMessagePreview: "Hey!",
      nudgeCount: 0,
      nudgeBanner: "",
      status: "active",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("connections")
      .doc("conn_1")
      .collection("messages")
      .doc("msg_1")
      .set({
        connectionId: "conn_1",
        memberUids: [ALICE_UID, BOB_UID],
        senderId: ALICE_UID,
        body: "Would you like coffee at Junction Mall this weekend?",
        status: "sent",
        roseAttached: false,
        createdAt: pastDate(1000),
      })
  );

  // Carol cannot inject a message into Alice & Bob's connection
  await assertFails(
    carolDb
      .collection("connections")
      .doc("conn_1")
      .collection("messages")
      .doc("msg_bad")
      .set({
        connectionId: "conn_1",
        memberUids: [ALICE_UID, BOB_UID],
        senderId: CAROL_UID,
        body: "Spam",
        status: "sent",
        roseAttached: false,
        createdAt: pastDate(500),
      })
  );

  // Filtered message query succeeds for member Bob; unfiltered & direct non-member read fail
  await assertSucceeds(
    bobDb
      .collection("connections")
      .doc("conn_1")
      .collection("messages")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );
  await assertFails(
    carolDb
      .collection("connections")
      .doc("conn_1")
      .collection("messages")
      .get()
  );
  await assertFails(
    carolDb
      .collection("connections")
      .doc("conn_1")
      .collection("messages")
      .doc("msg_1")
      .get()
  );

  // Reports scoped to reporterId
  await assertSucceeds(
    aliceDb.collection("reports").doc("rep_1").set({
      reporterId: ALICE_UID,
      targetUid: BOB_UID,
      targetName: "David",
      category: "scam",
      details: "Asked for airtime",
      filedAfterBlock: true,
      bulk: false,
      evidenceSnippet: "Send vocha",
      outcome: "reviewing",
      createdAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("reports").where("reporterId", "==", ALICE_UID).get()
  );
  await assertFails(bobDb.collection("reports").doc("rep_1").get());
  await assertFails(aliceDb.collection("reports").get());
});
