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
    private val effectsRef: DatabaseReference = database.getReference("effects")
    private val tokensRef: DatabaseReference = database.getReference("fcm_tokens")
    private val connectedRef: DatabaseReference = database.getReference(".info/connected")
    private val officerEventsRef: DatabaseReference = database.getReference("officer_events")

    // Firebase에서 가져올 메시지 수 (시스템 메시지 포함이므로 넉넉하게)
    // 실제 표시되는 사용자 메시지는 필터링 후 결정
    private val messageLimit = 150

    // 입장 시간 (이 시간 이후의 메시지만 표시)
    private var joinedAt: Long = 0L

    // 현재 접속 중인 사용자 정보 (재연결 시 핸들러 재설정용)
    private var currentUserId: String? = null
    private var currentNickname: String? = null
    private var isCurrentlyConnected: Boolean = false

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
                        val isSystemMessage = child.child("isSystemMessage").getValue(Boolean::class.java) ?: false
                        val effectId = child.child("effectId").getValue(String::class.java)

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
                        val isPoll = child.child("isPoll").getValue(Boolean::class.java) ?: false
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
                            readBy = readBy,
                            effectId = effectId,
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

                // 메시지 정렬 (timestamp 기준)
                val sortedMessages = messages.sortedBy { it.timestamp }

                // 디버깅: 모든 메시지 표시 (필터링 없이)
                // TODO: 테스트 후 원래 필터링 로직으로 복원
                android.util.Log.d("ChatRepository", "Total messages from Firebase: ${messages.size}, joinedAt: $joinedAt")
                messages.forEach { msg ->
                    android.util.Log.d("ChatRepository", "Message: id=${msg.id}, timestamp=${msg.timestamp}, isSystem=${msg.isSystemMessage}, text=${msg.message.take(30)}")
                }

                // 입장 전 메시지: 합류/퇴장 알림 시스템 메시지만 제외하고 최근 50개
                val messagesBeforeJoin = if (joinedAt <= 0) {
                    emptyList()
                } else {
                    sortedMessages
                        .filter { msg ->
                            msg.timestamp < joinedAt &&
                            // 합류/퇴장 알림 시스템 메시지만 제외 (배신 메시지는 긴급 알림이므로 표시)
                            !(msg.isSystemMessage && (msg.message.contains("합류했습니다") ||
                                                       msg.message.contains("퇴장했습니다") ||
                                                       msg.message.contains("복귀했습니다")))
                        }
                        .takeLast(50)
                }

                // 입장 후 메시지: 모두 표시
                val messagesAfterJoin = sortedMessages.filter { it.timestamp >= joinedAt }

                val filteredMessages = (messagesBeforeJoin + messagesAfterJoin)
                    .distinctBy { it.id }
                    .sortedBy { it.timestamp }

                android.util.Log.d("ChatRepository", "Filtered messages: ${filteredMessages.size} (before: ${messagesBeforeJoin.size}, after: ${messagesAfterJoin.size})")

                trySend(filteredMessages)
            }

            override fun onCancelled(error: DatabaseError) {
                // 에러 처리
            }
        }

        val query = messagesRef.orderByChild("timestamp").limitToLast(messageLimit)
        query.addValueEventListener(listener)

        awaitClose {
            query.removeEventListener(listener)
        }
    }

    fun setJoinedTime(time: Long) {
        joinedAt = time
    }

    /**
     * Firebase 연결 상태 모니터링
     * 네트워크 끊김/재연결 감지하여 onDisconnect 핸들러 재설정
     */
    fun getConnectionState(): Flow<Boolean> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                val wasConnected = isCurrentlyConnected
                isCurrentlyConnected = connected

                // 재연결 감지: 이전에 끊겼다가 다시 연결됨
                if (connected && !wasConnected && currentUserId != null && currentNickname != null) {
                    // 재연결 시 onDisconnect 핸들러 재설정 필요
                }

                trySend(connected)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(false)
            }
        }

        connectedRef.addValueEventListener(listener)

        awaitClose {
            connectedRef.removeEventListener(listener)
        }
    }

    /**
     * 재연결 시 onDisconnect 핸들러 재설정
     * 네트워크 끊김으로 배신 메시지가 전송되었을 수 있으므로 다시 설정
     */
    suspend fun resetDisconnectHandlersOnReconnect() {
        val userId = currentUserId ?: return
        val nickname = currentNickname ?: return

        // 기존 핸들러 취소 (이미 발동되었을 수 있음)
        try {
            disconnectMessageKey?.let { key ->
                messagesRef.child(key)
                    .onDisconnect()
                    .cancel()
                    .await()
            }
            usersRef.child(userId).child("isOnline")
                .onDisconnect()
                .cancel()
                .await()
        } catch (e: Exception) {
            // 이미 발동된 경우 무시
        }

        // 온라인 상태 복구
        usersRef.child(userId).child("isOnline").setValue(true).await()

        // 새로운 핸들러 설정
        setupDisconnectHandlers(userId, nickname)
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

    fun getOnlineUsers(): Flow<List<EliteUser>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = snapshot.children.mapNotNull { child ->
                    try {
                        val oderId = child.child("userId").getValue(String::class.java) ?: ""
                        val nickname = child.child("nickname").getValue(String::class.java) ?: ""
                        val sessionStartTime = child.child("sessionStartTime").getValue(Long::class.java) ?: 0L
                        val lastActiveTime = child.child("lastActiveTime").getValue(Long::class.java) ?: 0L
                        val isOnline = child.child("isOnline").getValue(Boolean::class.java) ?: false
                        val isAdmin = child.child("isAdmin").getValue(Boolean::class.java) ?: false
                        EliteUser(
                            userId = oderId,
                            nickname = nickname,
                            sessionStartTime = sessionStartTime,
                            lastActiveTime = lastActiveTime,
                            isOnline = isOnline,
                            isAdmin = isAdmin
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.filter { user ->
                    // isOnline인 사용자만 (관리자 제외) - onDisconnect로 오프라인 상태 자동 관리됨
                    // 닉네임 "전우회장"도 관리자로 간주하여 제외
                    val isAdminByNickname = user.nickname == "전우회장"
                    user.isOnline && !user.isAdmin && !isAdminByNickname
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

    // 연결 끊김 시 전송할 퇴장 메시지 키 (취소용)
    private var disconnectMessageKey: String? = null

    /**
     * 채팅방 입장
     * @param forceSessionStartTime 세션 시작 시간을 강제로 설정 (복구 모드용)
     * @return 실제 적용된 sessionStartTime
     */
    suspend fun joinChat(
        userId: String,
        nickname: String,
        isAdmin: Boolean = false,
        forceSessionStartTime: Long? = null
    ): Long {
        // 재연결 시 핸들러 재설정을 위해 사용자 정보 저장
        if (!isAdmin) {
            currentUserId = userId
            currentNickname = nickname
        }

        val currentTime = System.currentTimeMillis()

        val sessionStartTime: Long = when {
            // 강제 설정된 시간이 있으면 사용 (복구 모드)
            forceSessionStartTime != null -> forceSessionStartTime

            else -> {
                // 기존 사용자 정보 확인 (세션 유지를 위해)
                val existingSnapshot = usersRef.child(userId).get().await()
                val existingStartTime = existingSnapshot.child("sessionStartTime").getValue(Long::class.java)
                val existingLastActive = existingSnapshot.child("lastActiveTime").getValue(Long::class.java) ?: 0L
                val wasAdmin = existingSnapshot.child("isAdmin").getValue(Boolean::class.java) ?: false

                // 기존 세션이 있고 최근에 활동했다면 (10분 이내) 세션 유지
                // 단, 이전 세션이 관리자 세션이었으면 새 세션 시작 (관리자 계급 유지 방지)
                val isRecentSession = existingStartTime != null &&
                                      existingStartTime > 0 &&
                                      !wasAdmin &&  // 관리자 세션이었으면 새로 시작
                                      (currentTime - existingLastActive) < 10 * 60 * 1000  // 10분 이내

                if (isRecentSession) {
                    // 기존 세션 유지 - sessionStartTime 보존
                    existingStartTime!!
                } else {
                    // 새 세션 시작
                    currentTime
                }
            }
        }

        val user = EliteUser(
            userId = userId,
            nickname = nickname,
            sessionStartTime = sessionStartTime,
            lastActiveTime = currentTime,
            isOnline = true,
            isAdmin = isAdmin
        )
        usersRef.child(userId).setValue(user).await()

        // 재접속 시 최근 1분 이내의 본인 배신 메시지 삭제 (앱 재빌드/재실행 시 onDisconnect가 발동되어 생성된 메시지)
        if (!isAdmin) {
            cleanupRecentBetrayalMessage(nickname, currentTime)
        }

        // 관리자가 아닌 경우에만 연결 끊김 핸들러 설정
        if (!isAdmin) {
            setupDisconnectHandlers(userId, nickname)
        }

        return sessionStartTime
    }

    /**
     * 재접속 시 최근 배신 메시지 정리
     * 앱 재빌드/재실행 시 onDisconnect가 발동되어 배신 메시지가 생성될 수 있음
     * 5분 이내의 본인 배신 메시지를 삭제
     */
    private suspend fun cleanupRecentBetrayalMessage(nickname: String, currentTime: Long) {
        try {
            val fiveMinutesAgo = currentTime - 5 * 60 * 1000
            val recentMessages = messagesRef
                .orderByChild("timestamp")
                .startAt(fiveMinutesAgo.toDouble())
                .get()
                .await()

            recentMessages.children.forEach { snapshot ->
                val message = snapshot.child("message").getValue(String::class.java) ?: return@forEach
                val isSystem = snapshot.child("isSystemMessage").getValue(Boolean::class.java) ?: false

                // 본인의 배신 메시지인 경우 삭제
                if (isSystem && message.contains(nickname) && message.contains("배신했습니다")) {
                    snapshot.ref.removeValue().await()
                }
            }
        } catch (e: Exception) {
            // 실패해도 무시 (치명적이지 않음)
        }
    }

    /**
     * Firebase 연결 끊김 시 자동 실행될 핸들러 설정
     * - 서버 측에서 연결 끊김을 감지하여 실행
     * - 앱 강제 종료, 네트워크 끊김 등 모든 상황에서 동작
     */
    private suspend fun setupDisconnectHandlers(userId: String, nickname: String) {
        // 기존 핸들러가 있으면 먼저 취소 (중복 등록 방지)
        try {
            disconnectMessageKey?.let { existingKey ->
                messagesRef.child(existingKey)
                    .onDisconnect()
                    .cancel()
                    .await()
            }
            usersRef.child(userId).child("isOnline")
                .onDisconnect()
                .cancel()
                .await()
        } catch (e: Exception) {
            // 기존 핸들러가 없거나 이미 발동된 경우 무시
        }

        // 1. 온라인 상태를 false로 설정
        usersRef.child(userId).child("isOnline")
            .onDisconnect()
            .setValue(false)
            .await()

        // 2. 퇴장 메시지 설정 (미리 키 생성)
        val messageKey = messagesRef.push().key ?: return
        disconnectMessageKey = messageKey

        val leaveMessage = mapOf(
            "id" to messageKey,
            "userId" to "SYSTEM",
            "nickname" to "시스템",
            "message" to "${nickname} 전우가 배터리 관리 소홀로 전우회를 배신했습니다",
            "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP,
            "rank" to "TRAINEE",
            "isSystemMessage" to true
        )

        messagesRef.child(messageKey)
            .onDisconnect()
            .setValue(leaveMessage)
            .await()
    }

    /**
     * 정상 종료 시 연결 끊김 핸들러 취소
     */
    suspend fun cancelDisconnectHandlers(userId: String) {
        // onDisconnect 핸들러 취소
        usersRef.child(userId).child("isOnline")
            .onDisconnect()
            .cancel()
            .await()

        disconnectMessageKey?.let { key ->
            messagesRef.child(key)
                .onDisconnect()
                .cancel()
                .await()
        }
        disconnectMessageKey = null
    }

    suspend fun leaveChat(userId: String, wasAdmin: Boolean = false, wasExiled: Boolean = false) {
        // 정상 종료 시 onDisconnect 핸들러 취소
        cancelDisconnectHandlers(userId)

        when {
            wasAdmin -> {
                // 관리자 세션 종료 시 세션 데이터 완전 초기화
                usersRef.child(userId).updateChildren(mapOf(
                    "isOnline" to false,
                    "isAdmin" to false,
                    "sessionStartTime" to 0L
                )).await()
            }
            wasExiled -> {
                // 추방 시 세션 데이터 초기화 (자동 복원 방지 - 광고 시청 후 복구 모달 필요)
                usersRef.child(userId).updateChildren(mapOf(
                    "isOnline" to false,
                    "sessionStartTime" to 0L,
                    "lastActiveTime" to 0L
                )).await()
            }
            else -> {
                // 자발적 퇴장: isOnline만 false (세션 유지하여 10분 내 복귀 시 계속)
                usersRef.child(userId).child("isOnline").setValue(false).await()
            }
        }

        // 사용자 정보 클리어
        currentUserId = null
        currentNickname = null
    }

    suspend fun updateUserActivity(userId: String) {
        val updates = mapOf(
            "lastActiveTime" to System.currentTimeMillis(),
            "isOnline" to true  // 활동 중이면 항상 온라인 상태 보장
        )
        usersRef.child(userId).updateChildren(updates).await()
    }

    suspend fun updateUserNickname(userId: String, nickname: String) {
        usersRef.child(userId).child("nickname")
            .setValue(nickname).await()

        // 현재 닉네임 업데이트 (onDisconnect 핸들러에서 사용)
        if (userId == currentUserId) {
            currentNickname = nickname
            // onDisconnect 핸들러 재설정 (새 닉네임으로)
            cancelDisconnectHandlers(userId)
            setupDisconnectHandlers(userId, nickname)
        }
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
                val count = snapshot.children.count { child ->
                    try {
                        val isOnline = child.child("isOnline").getValue(Boolean::class.java) ?: false
                        val isAdmin = child.child("isAdmin").getValue(Boolean::class.java) ?: false
                        // isOnline인 사용자만 (관리자 제외) - onDisconnect로 오프라인 상태 자동 관리됨
                        isOnline && !isAdmin
                    } catch (e: Exception) {
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

    // ========== FCM 토큰 관리 ==========

    /**
     * FCM 토큰 저장/업데이트
     */
    suspend fun saveFcmToken(userId: String, token: String) {
        try {
            tokensRef.child(userId).setValue(mapOf(
                "token" to token,
                "updatedAt" to System.currentTimeMillis()
            )).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * FCM 토큰 삭제 (로그아웃 시)
     */
    suspend fun removeFcmToken(userId: String) {
        try {
            tokensRef.child(userId).removeValue().await()
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

    /**
     * 관리자: 메시지 삭제
     */
    suspend fun deleteMessage(messageId: String) {
        try {
            messagesRef.child(messageId).removeValue().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 관리자: 신고 목록 조회 (실시간)
     */
    fun getReports(): Flow<List<Report>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reports = mutableListOf<Report>()
                for (child in snapshot.children) {
                    try {
                        val report = Report(
                            id = child.child("id").getValue(String::class.java) ?: "",
                            messageId = child.child("messageId").getValue(String::class.java) ?: "",
                            messageContent = child.child("messageContent").getValue(String::class.java) ?: "",
                            reportedUserId = child.child("reportedUserId").getValue(String::class.java) ?: "",
                            reportedNickname = child.child("reportedNickname").getValue(String::class.java) ?: "",
                            reporterUserId = child.child("reporterUserId").getValue(String::class.java) ?: "",
                            reporterNickname = child.child("reporterNickname").getValue(String::class.java) ?: "",
                            reason = child.child("reason").getValue(String::class.java) ?: "",
                            timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L,
                            status = child.child("status").getValue(String::class.java) ?: ReportStatus.PENDING.name
                        )
                        reports.add(report)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                // 최신 순으로 정렬
                trySend(reports.sortedByDescending { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        reportsRef.addValueEventListener(listener)
        awaitClose { reportsRef.removeEventListener(listener) }
    }

    /**
     * 관리자: 신고 상태 업데이트
     */
    suspend fun updateReportStatus(reportId: String, status: ReportStatus) {
        try {
            reportsRef.child(reportId).child("status").setValue(status.name).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 관리자: 신고 삭제
     */
    suspend fun deleteReport(reportId: String) {
        try {
            reportsRef.child(reportId).removeValue().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ========== 장교 이펙트 이벤트 ==========

    /**
     * 장교 입장 이벤트 발송
     * 장교(소위 이상)가 채팅방에 입장하면 다른 사용자에게 알림
     */
    suspend fun broadcastOfficerEntrance(
        userId: String,
        nickname: String,
        rank: EliteRank
    ) {
        try {
            val eventId = officerEventsRef.push().key ?: return
            val event = mapOf(
                "type" to "OFFICER_ENTERED",
                "userId" to userId,
                "nickname" to nickname,
                "rank" to rank.name,
                "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP
            )
            officerEventsRef.child(eventId).setValue(event).await()

            // 5초 후 자동 삭제 (데이터 누적 방지)
            kotlinx.coroutines.delay(5000)
            try {
                officerEventsRef.child(eventId).removeValue().await()
            } catch (e: Exception) {
                // 삭제 실패는 무시
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 장교 입장 이벤트 수신 Flow
     * 새로운 장교 입장 이벤트를 실시간으로 수신
     */
    fun getOfficerEntranceEvents(currentUserId: String): Flow<ChatEvent.OfficerEntered> = callbackFlow {
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    val type = snapshot.child("type").getValue(String::class.java)
                    if (type != "OFFICER_ENTERED") return

                    val oderId = snapshot.child("userId").getValue(String::class.java) ?: return
                    // 자신의 입장은 무시
                    if (oderId == currentUserId) return

                    val nickname = snapshot.child("nickname").getValue(String::class.java) ?: return
                    val rankName = snapshot.child("rank").getValue(String::class.java) ?: return
                    val rank = try {
                        EliteRank.valueOf(rankName)
                    } catch (e: Exception) {
                        return
                    }

                    trySend(ChatEvent.OfficerEntered(oderId, nickname, rank))
                } catch (e: Exception) {
                    // 파싱 실패 무시
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        // 최근 이벤트만 수신 (5초 이내)
        val recentTime = System.currentTimeMillis() - 5000
        val query = officerEventsRef.orderByChild("timestamp").startAt(recentTime.toDouble())
        query.addChildEventListener(listener)

        awaitClose {
            query.removeEventListener(listener)
        }
    }
}
