package com.elites.fullcharge.data

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val messagesRef: DatabaseReference = database.getReference("messages")
    private val usersRef: DatabaseReference = database.getReference("online_users")
    private val reportsRef: DatabaseReference = database.getReference("reports")
    private val allTimeRecordsRef: DatabaseReference = database.getReference("all_time_records")

    // 최근 50개 메시지만 유지
    private val messageLimit = 50

    // 입장 시간 (이 시간 이후의 메시지만 표시)
    private var joinedAt: Long = 0L

    fun getMessages(): Flow<List<ChatMessage>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<ChatMessage>()

                for (child in snapshot.children) {
                    try {
                        val id = child.key ?: continue
                        val oderId = child.child("userId").getValue(String::class.java) ?: ""
                        val nickname = child.child("nickname").getValue(String::class.java) ?: ""
                        val msgText = child.child("message").getValue(String::class.java) ?: ""
                        val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                        val rank = child.child("rank").getValue(String::class.java) ?: EliteRank.TRAINEE.name
                        val isSystemMessage = child.child("isSystemMessage").value == true
                        val isBotMessage = child.child("isBotMessage").value == true

                        // readBy
                        val readBy = child.child("readBy").children
                            .mapNotNull { it.getValue(String::class.java) }

                        // 답장 관련
                        val replyToId = child.child("replyToId").getValue(String::class.java)
                        val replyToNickname = child.child("replyToNickname").getValue(String::class.java)
                        val replyToMessage = child.child("replyToMessage").getValue(String::class.java)

                        // reactions
                        val reactions = mutableMapOf<String, List<String>>()
                        child.child("reactions").children.forEach { emojiChild ->
                            val userIds = emojiChild.children.mapNotNull { it.getValue(String::class.java) }
                            if (userIds.isNotEmpty()) {
                                reactions[emojiChild.key ?: ""] = userIds
                            }
                        }

                        // 투표 관련
                        val isPoll = child.child("isPoll").value == true
                        val pollQuestion = child.child("pollQuestion").getValue(String::class.java)
                        val pollOptions = child.child("pollOptions").children
                            .mapNotNull { it.getValue(String::class.java) }
                        val pollVotes = mutableMapOf<String, List<String>>()
                        child.child("pollVotes").children.forEach { optionChild ->
                            val key = optionChild.key ?: return@forEach
                            val userIds = optionChild.children
                                .mapNotNull { it.getValue(String::class.java) }
                            pollVotes[key] = userIds
                        }
                        val pollEndTime = child.child("pollEndTime").getValue(Long::class.java) ?: 0L

                        // mentions
                        val mentions = child.child("mentions").children
                            .mapNotNull { it.getValue(String::class.java) }

                        // warning
                        val warning = child.child("warning").getValue(String::class.java)

                        val chatMessage = ChatMessage(
                            id = id,
                            userId = oderId,
                            nickname = nickname,
                            message = msgText,
                            timestamp = timestamp,
                            rank = rank,
                            isSystemMessage = isSystemMessage,
                            isBotMessage = isBotMessage,
                            readBy = readBy,
                            replyToId = replyToId,
                            replyToNickname = replyToNickname,
                            replyToMessage = replyToMessage,
                            reactions = reactions,
                            isPoll = isPoll,
                            pollQuestion = pollQuestion,
                            pollOptions = pollOptions,
                            pollVotes = pollVotes,
                            pollEndTime = pollEndTime,
                            mentions = mentions,
                            warning = warning
                        )
                        messages.add(chatMessage)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // 파싱 실패한 메시지는 건너뜀
                    }
                }

                val filteredMessages = messages
                    .filter { it.timestamp >= joinedAt }
                    .sortedBy { it.timestamp }
                trySend(filteredMessages)
            }

            override fun onCancelled(error: DatabaseError) {
                // 에러 처리
            }
        }

        messagesRef.orderByChild("timestamp").limitToLast(messageLimit)
            .addValueEventListener(listener)

        awaitClose {
            messagesRef.removeEventListener(listener)
        }
    }

    fun setJoinedTime(time: Long) {
        joinedAt = time
    }

    fun getNewMessages(): Flow<ChatMessage> = callbackFlow {
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue(ChatMessage::class.java)?.let { message ->
                    trySend(message.copy(id = snapshot.key ?: ""))
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        messagesRef.orderByChild("timestamp").limitToLast(1)
            .addChildEventListener(listener)

        awaitClose {
            messagesRef.removeEventListener(listener)
        }
    }

    suspend fun sendMessage(message: ChatMessage) {
        val key = messagesRef.push().key ?: UUID.randomUUID().toString()
        val messageWithId = message.copy(id = key)
        messagesRef.child(key).setValue(messageWithId.toMap()).await()
    }

    suspend fun sendSystemMessage(text: String) {
        val key = messagesRef.push().key ?: UUID.randomUUID().toString()
        val message = ChatMessage(
            id = key,
            userId = "SYSTEM",
            nickname = "시스템",
            message = text,
            timestamp = System.currentTimeMillis(),
            rank = EliteRank.TRAINEE.name,
            isSystemMessage = true
        )
        messagesRef.child(key).setValue(message.toMap()).await()
    }

    /**
     * 봇 메시지 전송 (랜덤 캐릭터 사용)
     */
    suspend fun sendBotMessage(text: String) {
        val character = BotCharacters.random()
        sendBotMessageWithCharacter(character, text)
    }

    /**
     * 봇 캐릭터별 메시지 전송
     */
    suspend fun sendBotMessageWithCharacter(character: BotCharacter, text: String) {
        val key = messagesRef.push().key ?: UUID.randomUUID().toString()
        val message = ChatMessage(
            id = key,
            userId = character.id,
            nickname = character.nickname,
            message = text,
            timestamp = System.currentTimeMillis(),
            rank = character.rank.name
        )
        messagesRef.child(key).setValue(message.toMap()).await()
    }

    /**
     * 봇 퀴즈를 투표로 전송 (랜덤 캐릭터 사용, 3분 제한)
     */
    suspend fun sendBotQuizAsPoll(question: String, options: List<String>) {
        val character = BotCharacters.random()
        val key = messagesRef.push().key ?: UUID.randomUUID().toString()
        val currentTime = System.currentTimeMillis()
        val message = ChatMessage(
            id = key,
            userId = character.id,
            nickname = character.nickname,
            message = question,
            timestamp = currentTime,
            rank = character.rank.name,
            isPoll = true,
            pollQuestion = question,
            pollOptions = options,
            pollVotes = options.indices.associate { it.toString() to emptyList() },
            pollEndTime = currentTime + (3 * 60 * 1000L)  // 3분 후 종료
        )
        messagesRef.child(key).setValue(message.toMap()).await()
    }

    // 1분(60초) 이내 활동한 사용자만 온라인으로 간주
    private val onlineThresholdMs = 60_000L

    fun getOnlineUsers(): Flow<List<EliteUser>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentTime = System.currentTimeMillis()
                val users = snapshot.children.mapNotNull { child ->
                    try {
                        child.getValue(EliteUser::class.java)
                    } catch (e: Exception) {
                        // 잘못된 형식의 데이터는 무시
                        null
                    }
                }.filter { user ->
                    // isOnline이고 1분 이내 활동한 사용자만
                    user.isOnline && (currentTime - user.lastActiveTime) < onlineThresholdMs
                }
                trySend(users)
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        usersRef.addValueEventListener(listener)

        awaitClose {
            usersRef.removeEventListener(listener)
        }
    }

    suspend fun joinChat(userId: String, nickname: String) {
        val user = EliteUser(
            userId = userId,
            nickname = nickname,
            sessionStartTime = System.currentTimeMillis(),
            lastActiveTime = System.currentTimeMillis(),
            isOnline = true
        )
        usersRef.child(userId).setValue(user).await()
    }

    suspend fun leaveChat(userId: String) {
        usersRef.child(userId).child("isOnline").setValue(false).await()
    }

    suspend fun updateUserActivity(userId: String) {
        usersRef.child(userId).child("lastActiveTime")
            .setValue(System.currentTimeMillis()).await()
    }

    suspend fun updateUserNickname(userId: String, nickname: String) {
        usersRef.child(userId).child("nickname")
            .setValue(nickname).await()
    }

    suspend fun markMessagesAsRead(messageIds: List<String>, userId: String) {
        messageIds.forEach { messageId ->
            try {
                val messageRef = messagesRef.child(messageId)
                val snapshot = messageRef.get().await()
                val currentReadBy = snapshot.child("readBy").children
                    .mapNotNull { it.getValue(String::class.java) }
                    .toMutableList()

                if (!currentReadBy.contains(userId)) {
                    currentReadBy.add(userId)
                    messageRef.child("readBy").setValue(currentReadBy).await()
                }
            } catch (e: Exception) {
                // 에러 무시 (메시지가 삭제되었을 수 있음)
            }
        }
    }

    fun getOnlineUserCount(): Flow<Int> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentTime = System.currentTimeMillis()
                val count = snapshot.children.count { child ->
                    try {
                        val user = child.getValue(EliteUser::class.java)
                        user != null && user.isOnline && (currentTime - user.lastActiveTime) < onlineThresholdMs
                    } catch (e: Exception) {
                        // 잘못된 형식의 데이터는 무시
                        false
                    }
                }
                trySend(count)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(0)
            }
        }

        usersRef.addValueEventListener(listener)

        awaitClose {
            usersRef.removeEventListener(listener)
        }
    }

    /**
     * 메시지 신고
     */
    suspend fun reportMessage(report: Report): Boolean {
        return try {
            val key = reportsRef.push().key ?: return false
            val reportWithId = report.copy(id = key)
            reportsRef.child(key).setValue(reportWithId.toMap()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 이미 신고했는지 체크
     */
    suspend fun hasAlreadyReported(messageId: String, reporterUserId: String): Boolean {
        return try {
            val snapshot = reportsRef
                .orderByChild("messageId")
                .equalTo(messageId)
                .get()
                .await()

            snapshot.children.any { child ->
                child.child("reporterUserId").getValue(String::class.java) == reporterUserId
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 이모지 리액션 토글 (추가/제거)
     */
    suspend fun toggleReaction(messageId: String, emoji: String, userId: String) {
        try {
            val reactionRef = messagesRef.child(messageId).child("reactions").child(emoji)
            val snapshot = reactionRef.get().await()
            val currentUsers = snapshot.children
                .mapNotNull { it.getValue(String::class.java) }
                .toMutableList()

            if (currentUsers.contains(userId)) {
                currentUsers.remove(userId)
            } else {
                currentUsers.add(userId)
            }

            if (currentUsers.isEmpty()) {
                reactionRef.removeValue().await()
            } else {
                reactionRef.setValue(currentUsers).await()
            }
        } catch (e: Exception) {
            // 에러 무시
        }
    }

    /**
     * 투표하기
     */
    suspend fun votePoll(messageId: String, optionIndex: Int, userId: String) {
        try {
            val pollVotesRef = messagesRef.child(messageId).child("pollVotes")
            val snapshot = pollVotesRef.get().await()

            // 현재 투표 상태를 Map으로 변환
            val currentVotes = mutableMapOf<String, MutableList<String>>()
            snapshot.children.forEach { optionChild ->
                val key = optionChild.key ?: return@forEach
                val userIds = optionChild.children
                    .mapNotNull { it.getValue(String::class.java) }
                    .toMutableList()
                currentVotes[key] = userIds
            }

            // 기존 투표 제거 (다른 옵션에 투표했었다면)
            currentVotes.forEach { (_, userIds) ->
                userIds.remove(userId)
            }

            // 새 투표 추가
            val optionKey = optionIndex.toString()
            if (!currentVotes.containsKey(optionKey)) {
                currentVotes[optionKey] = mutableListOf()
            }
            currentVotes[optionKey]?.add(userId)

            // List로 변환하여 저장 (Firebase 호환성)
            val votesToSave = currentVotes.mapValues { it.value.toList() }
            pollVotesRef.setValue(votesToSave).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 투표 메시지 전송
     * @param durationMinutes 투표 지속 시간 (분). 0이면 무제한
     */
    suspend fun sendPollMessage(
        userId: String,
        nickname: String,
        rank: String,
        question: String,
        options: List<String>,
        durationMinutes: Int = 5
    ) {
        val key = messagesRef.push().key ?: UUID.randomUUID().toString()
        val currentTime = System.currentTimeMillis()
        val endTime = if (durationMinutes > 0) {
            currentTime + (durationMinutes * 60 * 1000L)
        } else 0L

        val message = ChatMessage(
            id = key,
            userId = userId,
            nickname = nickname,
            message = question,
            timestamp = currentTime,
            rank = rank,
            isPoll = true,
            pollQuestion = question,
            pollOptions = options,
            pollVotes = options.indices.associate { it.toString() to emptyList() },
            pollEndTime = endTime
        )
        messagesRef.child(key).setValue(message.toMap()).await()
    }

    /**
     * 역대 랭킹 조회 (상위 10명)
     */
    fun getAllTimeRecords(): Flow<List<AllTimeRecord>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val records = snapshot.children.mapNotNull { child ->
                    try {
                        val oderId = child.child("userId").getValue(String::class.java) ?: ""
                        val nickname = child.child("nickname").getValue(String::class.java) ?: ""
                        val durationMillis = child.child("durationMillis").getValue(Long::class.java) ?: 0L
                        val achievedAt = child.child("achievedAt").getValue(Long::class.java) ?: 0L
                        AllTimeRecord(oderId, nickname, durationMillis, achievedAt)
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.durationMillis }
                    .take(10)
                trySend(records)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }

        allTimeRecordsRef.addValueEventListener(listener)

        awaitClose {
            allTimeRecordsRef.removeEventListener(listener)
        }
    }

    /**
     * 역대 기록 저장/업데이트
     * 사용자의 기존 기록보다 높으면 업데이트
     */
    suspend fun updateAllTimeRecord(userId: String, nickname: String, durationMillis: Long) {
        try {
            val snapshot = allTimeRecordsRef.child(userId).get().await()
            val existingDuration = snapshot.child("durationMillis").getValue(Long::class.java) ?: 0L

            if (durationMillis > existingDuration) {
                val record = AllTimeRecord(
                    oderId = userId,
                    nickname = nickname,
                    durationMillis = durationMillis,
                    achievedAt = System.currentTimeMillis()
                )
                allTimeRecordsRef.child(userId).setValue(record.toMap()).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ========== 관리자 기능 ==========

    /**
     * 관리자: 사용자 강퇴
     */
    suspend fun kickUser(userId: String) {
        try {
            usersRef.child(userId).child("isOnline").setValue(false).await()
            usersRef.child(userId).child("kicked").setValue(true).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 관리자: 사용자 계급 변경 (세션 시작 시간 조정)
     */
    suspend fun changeUserRank(userId: String, newRank: EliteRank) {
        try {
            // 해당 계급에 맞는 세션 시작 시간 계산
            val requiredDuration = newRank.minMinutes * 60 * 1000L
            val newStartTime = System.currentTimeMillis() - requiredDuration
            usersRef.child(userId).child("sessionStartTime").setValue(newStartTime).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
