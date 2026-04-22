"use strict";

const {
  onDocumentCreated,
  onDocumentUpdated,
} = require("firebase-functions/v2/firestore");
const {onSchedule} = require("firebase-functions/v2/scheduler");
const {setGlobalOptions} = require("firebase-functions/v2");
const {logger} = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
setGlobalOptions({region: "us-central1", maxInstances: 10});

const db = admin.firestore();
const messaging = admin.messaging();

async function sendToUser(uid, title, body, route) {
  if (!uid) return;
  const snap = await db.collection("users").doc(uid).get();
  const token = snap.exists ? snap.get("fcmToken") : null;
  if (!token) {
    logger.info("No FCM token for user", {uid});
    return;
  }
  try {
    await messaging.send({
      token,
      notification: {title, body},
      data: route ? {route} : {},
      android: {priority: "high"},
    });
    logger.info("Notification sent", {uid, title});
  } catch (err) {
    logger.warn("Send failed", {uid, error: err.message});
    // If the token is invalid, clear it so we don't keep retrying.
    if (
      err.code === "messaging/registration-token-not-registered" ||
      err.code === "messaging/invalid-registration-token"
    ) {
      await db.collection("users").doc(uid).update({fcmToken: ""});
    }
  }
}

exports.onFriendshipCreated = onDocumentCreated(
    "friendships/{id}",
    async (event) => {
      const data = event.data && event.data.data();
      if (!data || data.status !== "pending") return;
      const requester = data.requestedBy;
      const target = requester === data.user1Id ? data.user2Id : data.user1Id;
      const reqSnap = await db.collection("users").doc(requester).get();
      const reqName = reqSnap.exists ?
          reqSnap.get("displayName") || "Someone" :
          "Someone";
      await sendToUser(
          target,
          "New friend request",
          `${reqName} wants to be friends`,
          "social/friends",
      );
    },
);

exports.onBattleCreated = onDocumentCreated(
    "streak_battles/{id}",
    async (event) => {
      const data = event.data && event.data.data();
      if (!data || data.status !== "pending") return;
      await sendToUser(
          data.opponentId,
          "Streak Battle challenge",
          `${data.creatorName || "A friend"} challenged you to a ` +
          `${data.category || ""} streak battle`,
          "social/battles",
      );
    },
);

exports.onDuelCreated = onDocumentCreated(
    "nutrition_duels/{id}",
    async (event) => {
      const data = event.data && event.data.data();
      if (!data || data.status !== "pending") return;
      await sendToUser(
          data.opponentId,
          "Nutrition Duel",
          `${data.challengerName || "A friend"} challenged you to a ` +
          `${data.category || ""} duel`,
          "social/duels",
      );
    },
);

exports.onPartnershipCreated = onDocumentCreated(
    "accountability_partners/{id}",
    async (event) => {
      const data = event.data && event.data.data();
      if (!data || data.status !== "pending") return;
      await sendToUser(
          data.user2Id,
          "Accountability partner request",
          `${data.user1Name || "A friend"} wants to be your accountability ` +
          "partner",
          "social/accountability",
      );
    },
);

// ── Accepted transitions ──────────────────────────────────────────

exports.onFriendshipUpdated = onDocumentUpdated(
    "friendships/{id}",
    async (event) => {
      const before = event.data && event.data.before.data();
      const after = event.data && event.data.after.data();
      if (!before || !after) return;
      if (before.status === "pending" && after.status === "accepted") {
        // Notify the original requester that the other side accepted
        const accepter =
          after.requestedBy === after.user1Id ? after.user2Id : after.user1Id;
        const accSnap = await db.collection("users").doc(accepter).get();
        const accName = accSnap.exists ?
            accSnap.get("displayName") || "Your friend" :
            "Your friend";
        await sendToUser(
            after.requestedBy,
            "Friend request accepted",
            `${accName} accepted your friend request`,
            "social/friends",
        );
      }
    },
);

exports.onBattleUpdated = onDocumentUpdated(
    "streak_battles/{id}",
    async (event) => {
      const before = event.data && event.data.before.data();
      const after = event.data && event.data.after.data();
      if (!before || !after) return;

      // Opponent accepted the challenge
      if (before.status === "pending" && after.status === "active") {
        await sendToUser(
            after.creatorId,
            "Battle accepted!",
            `${after.opponentName || "Your opponent"} accepted the ` +
            `${after.category || ""} streak battle`,
            "social/battles",
        );
        return;
      }

      // Battle ended — notify both sides
      if (before.status !== "completed" && after.status === "completed") {
        const cat = after.category || "";
        const winner = after.winnerId;
        if (winner === "tie") {
          await sendToUser(
              after.creatorId,
              "Streak Battle ended in a tie",
              `Your ${cat} battle with ${after.opponentName || "your opponent"}` +
              " finished tied",
              "social/battles",
          );
          await sendToUser(
              after.opponentId,
              "Streak Battle ended in a tie",
              `Your ${cat} battle with ${after.creatorName || "your opponent"}` +
              " finished tied",
              "social/battles",
          );
        } else {
          const winnerIsCreator = winner === after.creatorId;
          const winnerUid = winnerIsCreator ? after.creatorId : after.opponentId;
          const loserUid = winnerIsCreator ? after.opponentId : after.creatorId;
          const loserName = winnerIsCreator ?
              after.opponentName :
              after.creatorName;
          const winnerName = winnerIsCreator ?
              after.creatorName :
              after.opponentName;
          await sendToUser(
              winnerUid,
              "You won the Streak Battle!",
              `You beat ${loserName || "your opponent"} in the ${cat} battle`,
              "social/battles",
          );
          await sendToUser(
              loserUid,
              "Streak Battle lost",
              `${winnerName || "Your opponent"} won the ${cat} battle`,
              "social/battles",
          );
        }
      }
    },
);

exports.onDuelUpdated = onDocumentUpdated(
    "nutrition_duels/{id}",
    async (event) => {
      const before = event.data && event.data.before.data();
      const after = event.data && event.data.after.data();
      if (!before || !after) return;

      // Opponent accepted the duel
      if (before.status === "pending" && after.status === "active") {
        await sendToUser(
            after.challengerId,
            "Duel accepted!",
            `${after.opponentName || "Your opponent"} accepted the ` +
            `${after.category || ""} duel`,
            "social/duels",
        );
        return;
      }

      // Duel ended — notify both sides
      if (before.status !== "completed" && after.status === "completed") {
        const cat = after.category || "";
        const winner = after.winnerId;
        if (winner === "tie") {
          await sendToUser(
              after.challengerId,
              "Duel ended in a tie",
              `Your ${cat} duel with ${after.opponentName || "your opponent"}` +
              " finished tied",
              "social/duels",
          );
          await sendToUser(
              after.opponentId,
              "Duel ended in a tie",
              `Your ${cat} duel with ${after.challengerName || "your opponent"}` +
              " finished tied",
              "social/duels",
          );
        } else {
          const winnerIsChallenger = winner === after.challengerId;
          const winnerUid = winnerIsChallenger ?
              after.challengerId :
              after.opponentId;
          const loserUid = winnerIsChallenger ?
              after.opponentId :
              after.challengerId;
          const loserName = winnerIsChallenger ?
              after.opponentName :
              after.challengerName;
          const winnerName = winnerIsChallenger ?
              after.challengerName :
              after.opponentName;
          await sendToUser(
              winnerUid,
              "You won the Duel!",
              `You beat ${loserName || "your opponent"} in the ${cat} duel`,
              "social/duels",
          );
          await sendToUser(
              loserUid,
              "Duel lost",
              `${winnerName || "Your opponent"} won the ${cat} duel`,
              "social/duels",
          );
        }
      }
    },
);

exports.onPartnershipUpdated = onDocumentUpdated(
    "accountability_partners/{id}",
    async (event) => {
      const before = event.data && event.data.before.data();
      const after = event.data && event.data.after.data();
      if (!before || !after) return;
      if (before.status === "pending" && after.status === "active") {
        await sendToUser(
            after.user1Id,
            "Partnership accepted",
            `${after.user2Name || "Your partner"} accepted your ` +
            "accountability partner request",
            "social/accountability",
        );
      }
    },
);

// ── Scheduled completion ──────────────────────────────────────────
// Runs hourly and closes out any active battle/duel whose endDate has
// passed. Completion writes winnerId + status=completed, which triggers
// onBattleUpdated / onDuelUpdated to push the win/loss/tie notifications.

function todayYmd() {
  // yyyy-MM-dd in UTC. Battle/duel endDates are stored in local-date form,
  // so a one-day slack is fine — we complete when UTC today >= endDate.
  return new Date().toISOString().slice(0, 10);
}

exports.completeDueBattlesAndDuels = onSchedule(
    {schedule: "every 60 minutes", timeZone: "Etc/UTC"},
    async () => {
      const today = todayYmd();

      // ── Battles ──
      const battleSnap = await db.collection("streak_battles")
          .where("status", "==", "active")
          .where("endDate", "<=", today)
          .get();
      logger.info("completion sweep: battles", {count: battleSnap.size});
      for (const doc of battleSnap.docs) {
        const b = doc.data();
        const creatorStreak = b.creatorStreak || 0;
        const opponentStreak = b.opponentStreak || 0;
        let winnerId;
        if (creatorStreak > opponentStreak) winnerId = b.creatorId;
        else if (opponentStreak > creatorStreak) winnerId = b.opponentId;
        else winnerId = "tie";
        await doc.ref.update({status: "completed", winnerId});
      }

      // ── Duels ──
      const duelSnap = await db.collection("nutrition_duels")
          .where("status", "==", "active")
          .where("endDate", "<=", today)
          .get();
      logger.info("completion sweep: duels", {count: duelSnap.size});
      for (const doc of duelSnap.docs) {
        const d = doc.data();
        const cp = d.challengerProgress || 0;
        const op = d.opponentProgress || 0;
        let winnerId;
        if (cp > op) winnerId = d.challengerId;
        else if (op > cp) winnerId = d.opponentId;
        else winnerId = "tie";
        await doc.ref.update({status: "completed", winnerId});
      }
    },
);
