package com.elites.fullcharge.data

data class ChatMessage(
    val id: String = "",
    val userId: String = "",
    val nickname: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val rank: String = EliteRank.TRAINEE.name,
    val isSystemMessage: Boolean = false,
    val readBy: List<String> = emptyList(),  // 읽은 사용자 ID 목록
    // 답장 관련 필드
    val replyToId: String? = null,           // 답장 대상 메시지 ID
    val replyToNickname: String? = null,     // 답장 대상 닉네임
    val replyToMessage: String? = null,      // 답장 대상 메시지 내용 (미리보기)
    // 이모지 리액션 (이모지 -> 반응한 userId 목록)
    val reactions: Map<String, List<String>> = emptyMap(),
    // 투표 관련 필드
    val isPoll: Boolean = false,
    val pollQuestion: String? = null,
    val pollOptions: List<String> = emptyList(),
    val pollVotes: Map<String, List<String>> = emptyMap(),  // 옵션 -> 투표한 userId 목록
    // 멘션된 사용자 ID 목록
    val mentions: List<String> = emptyList(),
    // 콘텐츠 경고 메시지 (스팸/광고 의심 등)
    val warning: String? = null
) {
    // Firebase를 위한 빈 생성자
    constructor() : this("", "", "", "", 0L, EliteRank.TRAINEE.name, false, emptyList(), null, null, null, emptyMap(), false, null, emptyList(), emptyMap(), emptyList(), null)

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "nickname" to nickname,
        "message" to message,
        "timestamp" to timestamp,
        "rank" to rank,
        "isSystemMessage" to isSystemMessage,
        "readBy" to readBy,
        "replyToId" to replyToId,
        "replyToNickname" to replyToNickname,
        "replyToMessage" to replyToMessage,
        "reactions" to reactions,
        "isPoll" to isPoll,
        "pollQuestion" to pollQuestion,
        "pollOptions" to pollOptions,
        "pollVotes" to pollVotes,
        "mentions" to mentions,
        "warning" to warning
    )

    // 안 읽은 사람 수 계산 (온라인 사용자 수 기준)
    fun getUnreadCount(onlineUserCount: Int): Int {
        // 본인은 제외하고 계산
        val readCount = readBy.size
        return maxOf(0, onlineUserCount - readCount - 1)  // -1은 본인
    }
}
