# Deploying social push notifications

These Cloud Functions trigger FCM sends when new social docs are created
(friend requests, streak battle invites, nutrition duel invites, accountability
partner requests) in Firestore.

## One-time setup

1. **Upgrade the Firebase project to the Blaze plan**
   - Cloud Functions requires pay-as-you-go. You stay in the free tier
     (2M invocations/month) unless you scale a lot.
   - https://console.firebase.google.com/project/gym-workout-2660f/usage/details
   - Set a **budget alert** + **spending cap** in the linked Google Cloud
     billing account if you want a hard ceiling.

2. **Install the Firebase CLI** (once, globally)

   ```bash
   npm install -g firebase-tools
   firebase login
   ```

3. **Install function dependencies**

   ```bash
   cd functions
   npm install
   ```

## Deploy

From the repo root:

```bash
firebase deploy --only functions
```

First deploy takes 2–4 minutes. Subsequent deploys only push changed functions.

## Verify

Open the Functions log:

```bash
firebase functions:log
```

Then from the app, send a friend request to a test account. You should see a
log line like `Notification sent { uid: 'xyz', title: 'New friend request' }`
and the recipient device should receive a system notification.

## Functions in this codebase

| Trigger                              | Notifies                  |
| ------------------------------------ | ------------------------- |
| `friendships/{id}` create            | recipient (user2 / user1) |
| `streak_battles/{id}` create         | `opponentId`              |
| `nutrition_duels/{id}` create        | `opponentId`              |
| `accountability_partners/{id}` create| `user2Id`                 |

All four only send when `status == "pending"`.

## Firestore security note

FCM tokens live on `users/{uid}.fcmToken`. Your security rules should let the
owner write this field and Cloud Functions (which use the Admin SDK and bypass
rules) read it. No client should be able to read other users' tokens.
