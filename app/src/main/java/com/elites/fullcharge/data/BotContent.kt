package com.elites.fullcharge.data

import kotlin.random.Random

/**
 * 봇 캐릭터 정보
 */
data class BotCharacter(
    val id: String,
    val nickname: String,
    val rank: EliteRank
)

/**
 * 봇 캐릭터 생성기 - 시스템 기본 닉네임 형식과 동일하게 생성
 */
object BotCharacters {
    // 시스템 기본 닉네임 형식과 동일: "형용사 동물"
    private val adjectives = listOf(
        "행복한", "즐거운", "신나는", "편안한", "여유로운",
        "활기찬", "기분좋은", "상쾌한", "따뜻한", "포근한",
        "용감한", "씩씩한", "당당한", "멋진", "귀여운",
        "졸린", "배고픈", "심심한", "설레는", "두근두근"
    )

    private val nouns = listOf(
        "고양이", "강아지", "토끼", "햄스터", "판다",
        "펭귄", "북극곰", "수달", "라쿤", "다람쥐",
        "여우", "코알라", "기린", "사자", "호랑이",
        "돌고래", "물개", "올빼미", "부엉이", "참새"
    )

    /**
     * 시스템 기본 닉네임 형식으로 랜덤 생성
     */
    private fun generateRandomNickname(): String {
        return "${adjectives.random()} ${nouns.random()}"
    }

    /**
     * 랜덤 닉네임과 랜덤 계급으로 새 캐릭터 생성
     */
    fun random(): BotCharacter {
        val nickname = generateRandomNickname()
        // 낮은 계급 위주로 (더 자연스럽게)
        val rank = listOf(
            EliteRank.TRAINEE, EliteRank.TRAINEE,
            EliteRank.PRIVATE_SECOND, EliteRank.PRIVATE_SECOND,
            EliteRank.PRIVATE_FIRST, EliteRank.PRIVATE_FIRST,
            EliteRank.CORPORAL, EliteRank.CORPORAL,
            EliteRank.SERGEANT,
            EliteRank.STAFF_SERGEANT
        ).random()
        return BotCharacter(
            id = "bot_${System.nanoTime()}",
            nickname = nickname,
            rank = rank
        )
    }
}

/**
 * 봇 콘텐츠 타입
 */
sealed class BotContent {
    data class SimpleMessage(val message: String, val character: BotCharacter) : BotContent()
    data class ReplyMessage(val message: String, val character: BotCharacter) : BotContent()
}

/**
 * 봇 콘텐츠 저장소 - 더 자연스러운 버전
 */
object BotContentRepository {

    // 자연스러운 짧은 메시지들 (오타, 줄임말 포함)
    private val casualMessages = listOf(
        // 아주 짧은 메시지
        "ㅎㅇ", "ㅎㅇㅎㅇ", "안뇽", "하잉",
        "ㅋㅋ", "ㅋㅋㅋ", "ㅎㅎ", "ㅎㅎㅎ",
        "오", "오오", "헐", "ㄷㄷ",
        "ㄱㅊ", "ㅇㅇ", "ㄴㄴ", "ㅇㅋ",

        // 짧은 감탄/반응
        "와", "우와", "오호", "헉",
        "대박", "실화?", "진짜?", "레알?",
        "ㅋㅋㅋㅋ", "ㅋㅋㅋㅋㅋ",

        // 일상적인 짧은 말
        "심심", "심심해", "심심하다",
        "배고파", "배고픔", "밥먹고싶다",
        "졸려", "졸림", "자고싶다",
        "ㅠㅠ", "ㅜㅜ", "ㅠ",

        // 약간 긴 메시지 (자연스러운 문장)
        "다들 뭐해", "뭐하냐", "뭐함",
        "나만 심심해?", "나만 심심한거임?",
        "여기 사람있음?", "아무도없나",
        "조용하네", "왜케조용", "너무 조용해",
        "오 사람있네", "반가워",
        "충전중ㅋㅋ", "나도 충전중",
        "100%유지중", "100퍼 유지하는중",
        "아직 100%", "나 100프로임",
        "언제까지 버티나", "오래버티자",
        "화이팅", "파이팅", "ㅎㅇㅌ",

        // 질문형
        "다들 몇분째임?", "얼마나 버팀?",
        "오늘 몇명 나감?", "나간사람 있음?",
        "뭐하면서 기다려", "뭐하고있어",
        "재밌는거 없나", "할거없다",

        // 감정 표현
        "행복", "행복하다", "좋다",
        "ㅋㅋㅋ웃겨", "웃기네", "재밌네",
        "아 지루해", "지루하다", "노잼",

        // 랜덤 감탄사
        "아", "아ㅋㅋ", "음", "흠",
        "오케이", "ㅇㅋㅇㅋ", "굳", "굿",

        // 이모티콘 섞인 것
        "ㅎㅎ:)", "ㅋㅋ^^", "ㅠㅠ..",
        "헐ㅋㅋ", "와ㅋㅋ", "오ㅋ"
    )

    // 유저 메시지에 대한 반응 (키워드 -> 반응들)
    private val reactions = mapOf(
        // 인사
        listOf("안녕", "하이", "ㅎㅇ", "hi", "hello", "안뇽") to listOf(
            "ㅎㅇ", "ㅎㅇㅎㅇ", "안녕", "반가워", "ㅎㅇㅋ", "왔어?", "오", "어 안녕"
        ),
        // 심심/지루
        listOf("심심", "지루", "노잼") to listOf(
            "나도", "나두", "ㅇㅈ", "ㄹㅇ", "진짜", "마자", "ㅠㅠ", "그니까"
        ),
        // 웃음
        listOf("ㅋㅋ", "ㅎㅎ", "웃겨", "ㅋㅋㅋ") to listOf(
            "ㅋㅋㅋ", "ㅋㅋㅋㅋㅋ", "ㅎㅎㅎ", "웃기네", "ㅋㅋㅋㅋ", "ㄹㅇㅋㅋ"
        ),
        // 배고픔/음식
        listOf("배고", "밥", "먹", "치킨", "피자") to listOf(
            "나도", "맛있겠다", "ㅠㅠ배고파", "먹고싶다", "뭐먹어", "굿"
        ),
        // 졸림
        listOf("졸려", "피곤", "잠") to listOf(
            "나도졸려", "ㅠㅠ", "자", "자면안돼", "버텨", "화이팅"
        ),
        // 퇴장 관련
        listOf("나가", "퇴장", "나감", "탈출") to listOf(
            "ㅋㅋㅋ", "불쌍", "ㅠㅠ", "ㅋㅋ앜", "헐", "바이"
        ),
        // 100%
        listOf("100%", "100퍼", "완충", "풀충") to listOf(
            "ㅊㅋ", "굳", "좋겠다", "나도", "ㅇㅇ", "ㅋㅋ"
        ),
        // 99% 위험
        listOf("99%", "위험", "카운트") to listOf(
            "헐", "ㄷㄷ", "충전해", "빨리", "긴장되네", "살아남아"
        ),
        // 동의/긍정
        listOf("맞아", "ㅇㅇ", "인정", "ㄹㅇ") to listOf(
            "ㅇㅈ", "ㄹㅇ", "ㅇㅇ", "그니까", "맞아", "진짜"
        ),
        // 질문에 대한 반응
        listOf("뭐해", "뭐함", "모함") to listOf(
            "충전중", "채팅", "그냥", "너는", "뭐하긴 충전중", "ㅋㅋ뭐하긴"
        ),
        // 감탄
        listOf("대박", "헐", "와", "오") to listOf(
            "ㅇㅈ", "ㄹㅇ", "그니까", "마자", "헐ㅋㅋ", "와ㅋㅋ"
        )
    )

    /**
     * 랜덤 봇 메시지 반환 (침묵 시)
     */
    fun getRandomMessage(): BotContent.SimpleMessage {
        val character = BotCharacters.random()
        val message = casualMessages.random()
        return BotContent.SimpleMessage(message, character)
    }

    /**
     * 유저 메시지에 대한 반응
     * @return 반응이 있으면 BotContent.ReplyMessage, 없으면 null
     */
    fun getReactionToMessage(userMessage: String): BotContent.ReplyMessage? {
        // 50% 확률로 반응 (기존 30%에서 증가)
        if (Random.nextFloat() > 0.5f) return null

        // 메시지가 너무 짧으면 반응하지 않음 (봇끼리 무한루프 방지)
        if (userMessage.length < 2) return null

        val lowerMessage = userMessage.lowercase()

        for ((keywords, responses) in reactions) {
            if (keywords.any { lowerMessage.contains(it) }) {
                val character = BotCharacters.random()
                val response = responses.random()
                return BotContent.ReplyMessage(response, character)
            }
        }

        // 키워드 매칭이 없어도 20% 확률로 일반적인 반응
        if (Random.nextFloat() < 0.2f) {
            val genericResponses = listOf("ㅇㅇ", "ㅋㅋ", "오", "ㅎㅎ", "굳", "ㄱㅊ")
            val character = BotCharacters.random()
            return BotContent.ReplyMessage(genericResponses.random(), character)
        }

        return null
    }
}
