package com.elites.fullcharge.data

/**
 * 메시지 신고 데이터
 */
data class Report(
    val id: String = "",
    val messageId: String = "",
    val messageContent: String = "",
    val reportedUserId: String = "",
    val reportedNickname: String = "",
    val reporterUserId: String = "",
    val reporterNickname: String = "",
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = ReportStatus.PENDING.name
) {
    constructor() : this("", "", "", "", "", "", "", "", 0L, ReportStatus.PENDING.name)

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "messageId" to messageId,
        "messageContent" to messageContent,
        "reportedUserId" to reportedUserId,
        "reportedNickname" to reportedNickname,
        "reporterUserId" to reporterUserId,
        "reporterNickname" to reporterNickname,
        "reason" to reason,
        "timestamp" to timestamp,
        "status" to status
    )
}

enum class ReportStatus {
    PENDING,    // 검토 대기
    REVIEWED,   // 검토 완료
    ACTIONED,   // 조치 완료
    DISMISSED   // 기각
}

enum class ReportReason(val displayName: String) {
    SPAM("스팸/광고"),
    OBSCENE("음란물"),
    ABUSE("욕설/비방"),
    FRAUD("사기/허위정보"),
    OTHER("기타")
}
