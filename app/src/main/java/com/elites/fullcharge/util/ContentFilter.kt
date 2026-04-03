package com.elites.fullcharge.util

/**
 * 콘텐츠 필터링 유틸리티
 * - 광고/스팸 경고
 * - 링크 허용 도메인 체크
 */
object ContentFilter {

    // 음란 단어 (차단)
    private val obsceneWords = listOf(
        // 성기/성행위 관련
        "섹스", "sex", "보지", "자지", "좆", "씹", "fuck", "pussy", "dick", "cock",
        "음경", "음부", "성기", "자위", "딸딸", "오르가즘", "orgasm",
        "떡치", "박아", "따먹", "씹떡", "존슨", "꼬추", "잠지", "페니스", "클리",
        // 음란물 관련
        "야동", "porn", "av녀", "야사", "야설", "섹파", "원나잇", "노팬티", "노브라",
        "빨아", "핥아", "질내", "사정", "정액", "애액", "69자세", "체위",
        // 성매매 관련
        "조건만남", "원조교제", "성매매", "매춘", "창녀", "걸레년",
        // 영어 추가
        "blowjob", "handjob", "cumshot", "creampie", "milf", "dildo", "vibrator",
        "nipple", "boob", "tit", "ass", "anal", "orgasm", "masturbat", "erotic",
        "hentai", "xxx", "nsfw",
        // 원색적 욕설
        "씨발", "시발", "ㅅㅂ", "ㅆㅂ", "씨빨", "시빨", "씨바", "시바",
        "병신", "ㅂㅅ", "븅신", "빙신",
        "지랄", "ㅈㄹ", "짓랄",
        "개새끼", "개색끼", "개세끼", "ㄱㅅㄲ",
        "니미", "느금마", "니애미", "느금",
        "엠창", "애미", "에미",
        "ㅈ같", "좃같", "존나", "ㅈㄴ", "졸라"
    )

    // 광고/스팸 의심 키워드 (차단 X, 경고만)
    private val spamWarningWords = listOf(
        // 광고성
        "오픈채팅", "단톡방", "돈벌", "부업", "재택", "투잡",
        "비트코인", "선물거래", "마진",
        // 도박
        "카지노", "바카라", "슬롯", "토토", "배팅"
    )

    // 개인정보 패턴 (경고만)
    private val privateInfoPatterns = listOf(
        Regex("010[-\\s]?\\d{4}[-\\s]?\\d{4}"),  // 전화번호
        Regex("[a-zA-Z0-9]+@[a-zA-Z0-9]+\\.[a-zA-Z]+"),  // 이메일
    )

    // 허용된 도메인 (링크 미리보기용)
    private val allowedDomains = listOf(
        "youtube.com", "youtu.be",
        "naver.com", "blog.naver.com", "news.naver.com",
        "daum.net", "kakao.com",
        "google.com", "github.com",
        "instagram.com", "twitter.com", "x.com",
        "tiktok.com",
        "wikipedia.org",
        "namu.wiki"
    )

    data class FilterResult(
        val isAllowed: Boolean,
        val reason: String? = null,
        val filteredMessage: String? = null,
        val warning: String? = null  // 경고 메시지 (차단 X, 표시만)
    )

    /**
     * 메시지 필터링
     * @return FilterResult - 허용 여부와 이유/경고
     *
     * 정책:
     * - 욕설: 허용 (차단 X)
     * - 광고/스팸 키워드: 허용하되 경고 표시
     * - 개인정보: 허용하되 경고 표시
     * - 허용되지 않은 링크: 차단
     */
    fun filterMessage(message: String): FilterResult {
        val lowerMessage = message.lowercase()
        val warnings = mutableListOf<String>()

        // 1. 음란 단어 체크 (차단)
        for (word in obsceneWords) {
            if (lowerMessage.contains(word.lowercase())) {
                return FilterResult(
                    isAllowed = false,
                    reason = "부적절한 표현은 사용할 수 없어요"
                )
            }
        }

        // 2. 광고/스팸 키워드 체크 (차단 X, 경고만)
        for (word in spamWarningWords) {
            if (lowerMessage.contains(word.lowercase())) {
                warnings.add("광고/스팸")
                break
            }
        }

        // 3. 개인정보 패턴 체크 (차단 X, 경고만)
        for (pattern in privateInfoPatterns) {
            if (pattern.containsMatchIn(message)) {
                warnings.add("개인정보")
                break
            }
        }

        // 4. 링크 체크 - 허용되지 않은 도메인만 차단
        val urlCheckResult = checkUrls(message)
        if (!urlCheckResult.isAllowed) {
            return urlCheckResult
        }

        // 경고가 있으면 경고 메시지와 함께 허용
        val warningMessage = if (warnings.isNotEmpty()) {
            "${warnings.joinToString(", ")} 의심 콘텐츠입니다. 주의하세요."
        } else null

        return FilterResult(isAllowed = true, warning = warningMessage)
    }

    /**
     * URL 체크 - 허용된 도메인만 통과
     */
    private fun checkUrls(message: String): FilterResult {
        val urlPattern = Regex("https?://[^\\s]+")
        val urls = urlPattern.findAll(message)

        for (urlMatch in urls) {
            val url = urlMatch.value.lowercase()
            val isAllowed = allowedDomains.any { domain ->
                url.contains(domain)
            }

            if (!isAllowed) {
                return FilterResult(
                    isAllowed = false,
                    reason = "허용되지 않은 링크예요"
                )
            }
        }

        return FilterResult(isAllowed = true)
    }

    /**
     * 링크가 허용된 도메인인지 체크
     */
    fun isAllowedUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return allowedDomains.any { domain ->
            lowerUrl.contains(domain)
        }
    }

    /**
     * 허용된 도메인 목록 반환 (UI 표시용)
     */
    fun getAllowedDomains(): List<String> = allowedDomains

    /**
     * 닉네임 필터링
     * - 음란 단어만 체크 (차단)
     * - 스팸/링크는 닉네임에서 체크 안 함
     */
    fun filterNickname(nickname: String): FilterResult {
        val lowerNickname = nickname.lowercase()

        // 음란 단어 체크
        for (word in obsceneWords) {
            if (lowerNickname.contains(word.lowercase())) {
                return FilterResult(
                    isAllowed = false,
                    reason = "부적절한 닉네임이에요"
                )
            }
        }

        return FilterResult(isAllowed = true)
    }
}
