const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.database();
const messaging = admin.messaging();

/**
 * 새 메시지가 추가될 때 푸시 알림 전송
 */
exports.sendChatNotification = functions
  .region("asia-northeast3") // 서울 리전
  .database.ref("/messages/{messageId}")
  .onCreate(async (snapshot, context) => {
    const message = snapshot.val();

    // 시스템 메시지는 알림 안 함
    if (message.isSystemMessage) {
      console.log("Skipping notification for system message");
      return null;
    }

    const senderId = message.userId;
    const senderNickname = message.nickname;
    const messageText = message.message;

    // 모든 FCM 토큰 가져오기
    const tokensSnapshot = await db.ref("/fcm_tokens").once("value");
    const tokensData = tokensSnapshot.val();

    if (!tokensData) {
      console.log("No FCM tokens found");
      return null;
    }

    // 발신자를 제외한 토큰 목록 생성
    const tokens = [];
    const tokenUserMap = {}; // 토큰 -> userId 매핑 (실패 시 삭제용)

    Object.entries(tokensData).forEach(([oderId, data]) => {
      // 발신자 제외
      if (oderId !== senderId && data.token) {
        tokens.push(data.token);
        tokenUserMap[data.token] = oderId;
      }
    });

    if (tokens.length === 0) {
      console.log("No recipients to send notification");
      return null;
    }

    // 메시지 내용 (너무 길면 자르기)
    const truncatedMessage = messageText.length > 100
      ? messageText.substring(0, 100) + "..."
      : messageText;

    // 푸시 알림 페이로드
    const payload = {
      notification: {
        title: `${senderNickname}`,
        body: truncatedMessage,
      },
      data: {
        title: senderNickname,
        body: truncatedMessage,
        senderNickname: senderNickname,
        senderId: senderId,
        messageId: context.params.messageId,
        click_action: "FLUTTER_NOTIFICATION_CLICK",
      },
      android: {
        priority: "high",
        notification: {
          channelId: "elite_chat_channel",
          priority: "high",
          defaultSound: true,
        },
      },
    };

    try {
      // 일괄 전송
      const response = await messaging.sendEachForMulticast({
        tokens: tokens,
        ...payload,
      });

      console.log(`Sent ${response.successCount}/${tokens.length} notifications`);

      // 실패한 토큰 처리 (만료/무효 토큰 삭제)
      if (response.failureCount > 0) {
        const failedTokens = [];
        response.responses.forEach((resp, idx) => {
          if (!resp.success) {
            const errorCode = resp.error?.code;
            // 토큰이 만료되었거나 무효한 경우
            if (
              errorCode === "messaging/invalid-registration-token" ||
              errorCode === "messaging/registration-token-not-registered"
            ) {
              failedTokens.push(tokens[idx]);
            }
          }
        });

        // 실패한 토큰 삭제
        const deletePromises = failedTokens.map((token) => {
          const oderId = tokenUserMap[token];
          if (oderId) {
            console.log(`Removing invalid token for user: ${oderId}`);
            return db.ref(`/fcm_tokens/${oderId}`).remove();
          }
          return Promise.resolve();
        });

        await Promise.all(deletePromises);
      }

      return null;
    } catch (error) {
      console.error("Error sending notifications:", error);
      return null;
    }
  });
