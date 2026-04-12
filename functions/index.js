const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.database();
const messaging = admin.messaging();

// 알림 간격 제한 (밀리초)
const NOTIFICATION_INTERVAL_MS = 5 * 60 * 1000; // 5분

/**
 * 새 메시지가 추가될 때 푸시 알림 전송
 * - 멘션(@)된 경우: 즉시 알림
 * - 일반 메시지: 5분 간격 제한 (묶음 알림)
 */
exports.sendChatNotification = functions
  .region("asia-northeast3") // 서울 리전
  .database.ref("/messages/{messageId}")
  .onCreate(async (snapshot, context) => {
    const message = snapshot.val();
    const messageText = message.message || "";
    const senderId = message.userId;
    const senderNickname = message.nickname;
    const mentions = message.mentions || [];

    // 시스템 메시지 처리
    if (message.isSystemMessage) {
      // 관리자 공지만 알림 (일반 시스템 메시지는 스킵)
      const isAdminNotice = messageText.startsWith("[공지]") ||
                            messageText.startsWith("[관리자]");

      if (!isAdminNotice) {
        console.log("Skipping notification for system message");
        return null;
      }

      // 관리자 공지는 모든 사용자에게 즉시 발송
      return sendImmediateNotification({
        title: "완충 전우회 공지",
        body: messageText,
        senderId,
        messageId: context.params.messageId,
        excludeSender: false, // 관리자도 받음
      });
    }

    // 모든 FCM 토큰 가져오기
    const tokensSnapshot = await db.ref("/fcm_tokens").once("value");
    const tokensData = tokensSnapshot.val();

    if (!tokensData) {
      console.log("No FCM tokens found");
      return null;
    }

    // 멘션된 사용자에게는 즉시 알림
    if (mentions.length > 0) {
      const mentionedTokens = [];
      const tokenUserMap = {};

      // 멘션된 사용자의 토큰 찾기
      for (const [userId, data] of Object.entries(tokensData)) {
        if (mentions.includes(userId) && data.token && userId !== senderId) {
          mentionedTokens.push(data.token);
          tokenUserMap[data.token] = userId;
        }
      }

      if (mentionedTokens.length > 0) {
        console.log(`Sending immediate notification to ${mentionedTokens.length} mentioned users`);
        await sendNotificationToTokens({
          tokens: mentionedTokens,
          tokenUserMap,
          title: `${senderNickname}님이 회원님을 언급했습니다`,
          body: messageText.length > 100 ? messageText.substring(0, 100) + "..." : messageText,
          senderId,
          senderNickname,
          messageId: context.params.messageId,
        });
      }
    }

    // 일반 메시지: 5분 간격 제한 적용
    const currentTime = Date.now();
    const notificationPromises = [];

    for (const [userId, data] of Object.entries(tokensData)) {
      // 발신자와 이미 멘션 알림 받은 사용자 제외
      if (userId === senderId || mentions.includes(userId) || !data.token) {
        continue;
      }

      // 해당 사용자의 알림 상태 확인
      const stateRef = db.ref(`/notification_state/${userId}`);
      const stateSnapshot = await stateRef.once("value");
      const state = stateSnapshot.val() || { lastNotificationTime: 0, pendingCount: 0 };

      const timeSinceLastNotification = currentTime - (state.lastNotificationTime || 0);

      if (timeSinceLastNotification >= NOTIFICATION_INTERVAL_MS) {
        // 5분 이상 지났으면 알림 발송
        const pendingCount = state.pendingCount || 0;

        let title, body;
        if (pendingCount > 0) {
          // 묶음 알림
          title = "완충 전우회";
          body = `${pendingCount + 1}개의 새 메시지가 있습니다`;
        } else {
          // 일반 알림
          title = senderNickname;
          body = messageText.length > 100 ? messageText.substring(0, 100) + "..." : messageText;
        }

        notificationPromises.push(
          sendNotificationToTokens({
            tokens: [data.token],
            tokenUserMap: { [data.token]: userId },
            title,
            body,
            senderId,
            senderNickname,
            messageId: context.params.messageId,
          }).then(() => {
            // 알림 상태 초기화
            return stateRef.set({
              lastNotificationTime: currentTime,
              pendingCount: 0,
            });
          })
        );
      } else {
        // 5분 이내면 대기 카운터만 증가
        notificationPromises.push(
          stateRef.update({
            pendingCount: (state.pendingCount || 0) + 1,
          })
        );
      }
    }

    await Promise.all(notificationPromises);
    console.log(`Processed notifications for ${notificationPromises.length} users`);

    return null;
  });

/**
 * 즉시 알림 발송 (관리자 공지 등)
 */
async function sendImmediateNotification({ title, body, senderId, messageId, excludeSender = true }) {
  const tokensSnapshot = await db.ref("/fcm_tokens").once("value");
  const tokensData = tokensSnapshot.val();

  if (!tokensData) {
    return null;
  }

  const tokens = [];
  const tokenUserMap = {};

  for (const [userId, data] of Object.entries(tokensData)) {
    if (excludeSender && userId === senderId) continue;
    if (data.token) {
      tokens.push(data.token);
      tokenUserMap[data.token] = userId;
    }
  }

  if (tokens.length === 0) {
    return null;
  }

  return sendNotificationToTokens({
    tokens,
    tokenUserMap,
    title,
    body,
    senderId: senderId || "system",
    senderNickname: "시스템",
    messageId,
  });
}

/**
 * 토큰 목록에 알림 발송
 */
async function sendNotificationToTokens({ tokens, tokenUserMap, title, body, senderId, senderNickname, messageId }) {
  const payload = {
    notification: {
      title,
      body,
    },
    data: {
      title,
      body,
      senderNickname: senderNickname || "",
      senderId: senderId || "",
      messageId: messageId || "",
      click_action: "FLUTTER_NOTIFICATION_CLICK",
    },
    android: {
      priority: "high",
      notification: {
        channelId: "elite_chat_silent_v2",
        priority: "high",
        defaultSound: false,
      },
    },
  };

  try {
    const response = await messaging.sendEachForMulticast({
      tokens,
      ...payload,
    });

    console.log(`Sent ${response.successCount}/${tokens.length} notifications`);

    // 실패한 토큰 처리 (만료/무효 토큰 삭제)
    if (response.failureCount > 0) {
      const deletePromises = [];
      response.responses.forEach((resp, idx) => {
        if (!resp.success) {
          const errorCode = resp.error?.code;
          if (
            errorCode === "messaging/invalid-registration-token" ||
            errorCode === "messaging/registration-token-not-registered"
          ) {
            const userId = tokenUserMap[tokens[idx]];
            if (userId) {
              console.log(`Removing invalid token for user: ${userId}`);
              deletePromises.push(db.ref(`/fcm_tokens/${userId}`).remove());
            }
          }
        }
      });
      await Promise.all(deletePromises);
    }

    return response;
  } catch (error) {
    console.error("Error sending notifications:", error);
    return null;
  }
}
