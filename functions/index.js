const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const ADMIN_UID = "D2N37vcj9tgGKdhw0oXKOhetK3q1";
const TOKEN_FIELD = "fcmTokens";
const CHUNK_SIZE = 500;

exports.notifyStaffOnNewOrder = onDocumentCreated(
  { document: "orders/{orderId}", region: "us-central1" },
  async (event) => {
    if (!event.data) return;
    const order = event.data.data() || {};
    const orderId = event.params.orderId;

    const [adminDoc, managers] = await Promise.all([
      admin.firestore().collection("users").doc(ADMIN_UID).get(),
      admin.firestore().collection("users").where("role", "==", "manager").get()
    ]);

    const staffDocs = new Map();
    if (adminDoc.exists) staffDocs.set(adminDoc.id, adminDoc);
    managers.forEach((doc) => staffDocs.set(doc.id, doc));

    const tokenOwners = new Map();
    for (const doc of staffDocs.values()) {
      const tokens = Array.isArray(doc.get(TOKEN_FIELD)) ? doc.get(TOKEN_FIELD) : [];
      for (const token of tokens) {
        if (typeof token === "string" && token.length > 20) {
          if (!tokenOwners.has(token)) tokenOwners.set(token, []);
          tokenOwners.get(token).push(doc.ref);
        }
      }
    }
    const tokens = [...tokenOwners.keys()];
    if (!tokens.length) {
      logger.info("New order created; no registered staff FCM tokens", { orderId });
      return;
    }

    const customer = typeof order.customerName === "string" && order.customerName.trim()
      ? order.customerName.trim() : "მომხმარებელი";
    const payload = {
      notification: {
        title: "ახალი შეკვეთა",
        body: customer + " — ახალი შეკვეთა მიღებულია"
      },
      data: { orderId: String(orderId), screen: "orders" },
      android: {
        priority: "high",
        notification: { channelId: "makasia_orders" }
      }
    };

    for (let offset = 0; offset < tokens.length; offset += CHUNK_SIZE) {
      const chunk = tokens.slice(offset, offset + CHUNK_SIZE);
      const result = await admin.messaging().sendEachForMulticast({ ...payload, tokens: chunk });
      const removals = [];
      result.responses.forEach((response, index) => {
        if (!response.success) {
          const code = response.error && response.error.code;
          if (code === "messaging/registration-token-not-registered" ||
              code === "messaging/invalid-registration-token") {
            const token = chunk[index];
            for (const ref of tokenOwners.get(token) || []) {
              removals.push(ref.update({
                [TOKEN_FIELD]: admin.firestore.FieldValue.arrayRemove(token)
              }).catch((error) => logger.warn("Could not prune stale FCM token", { uid: ref.id, error: String(error) })));
            }
          } else {
            logger.warn("FCM delivery failed", { orderId, code, message: response.error && response.error.message });
          }
        }
      });
      await Promise.all(removals);
      logger.info("Staff push batch processed", {
        orderId, successCount: result.successCount, failureCount: result.failureCount
      });
    }
  }
);
