package com.elites.fullcharge.data

data class ChatMessage(
    val id: String = "",
    val userId: String = "",
    val nickname: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val rank: String = EliteRank.NEWBIE.name,
    val isSystemMessage: Boolean = false
) {
    // Firebase를 위한 빈 생성자
    constructor() : this("", "", "", "", 0L, EliteRank.NEWBIE.name, false)

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "userId" to userId,
        "nickname" to nickname,
        "message" to message,
        "timestamp" to timestamp,
        "rank" to rank,
        "isSystemMessage" to isSystemMessage
    )
}
