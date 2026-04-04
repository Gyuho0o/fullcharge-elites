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
 * 봇 캐릭터 목록
 */
object BotCharacters {
    val CHUNGCHUNG = BotCharacter("bot_chungchung", "충충이", EliteRank.STAFF_SERGEANT)
    val BBANBAN = BotCharacter("bot_bbanban", "빵빵이", EliteRank.SERGEANT)

    val all = listOf(CHUNGCHUNG, BBANBAN)

    fun random(): BotCharacter = all.random()
}

/**
 * 2인 대화 시나리오
 */
data class DialogueScenario(
    val id: String,
    val exchanges: List<DialogueExchange>
)

data class DialogueExchange(
    val speaker: BotCharacter,
    val message: String,
    val delayMs: Long = 2000  // 이전 메시지 후 대기 시간
)

/**
 * 유저 반응 트리거
 */
data class UserReactionTrigger(
    val keywords: List<String>,
    val responses: List<String>,
    val responder: BotCharacter? = null  // null이면 랜덤 봇
)

/**
 * 봇 콘텐츠 타입
 */
sealed class BotContent {
    data class FunMessage(val message: String, val character: BotCharacter) : BotContent()
    data class TopicSuggestion(val topic: String, val character: BotCharacter) : BotContent()
    data class Dialogue(val scenario: DialogueScenario) : BotContent()
    data class Quiz(
        val id: String,
        val question: String,
        val options: List<String>,
        val correctIndex: Int,
        val explanation: String,
        val character: BotCharacter = BotCharacters.CHUNGCHUNG
    ) : BotContent()
}

/**
 * 봇 콘텐츠 저장소
 */
object BotContentRepository {

    // 2인 대화 시나리오
    private val dialogueScenarios = listOf(
        DialogueScenario(
            id = "greeting",
            exchanges = listOf(
                DialogueExchange(BotCharacters.CHUNGCHUNG, "야 빵빵아, 여기 진짜 조용하다"),
                DialogueExchange(BotCharacters.BBANBAN, "그러게... 다들 충전 중인가?", 2500),
                DialogueExchange(BotCharacters.CHUNGCHUNG, "우리라도 얘기하자 ㅋㅋ", 2000)
            )
        ),
        DialogueScenario(
            id = "battery_talk",
            exchanges = listOf(
                DialogueExchange(BotCharacters.BBANBAN, "충충아 너 지금 몇 퍼야?"),
                DialogueExchange(BotCharacters.CHUNGCHUNG, "당연히 100%지! 여기 있잖아", 2000),
                DialogueExchange(BotCharacters.BBANBAN, "ㅋㅋㅋ 그것도 그렇네", 1500)
            )
        ),
        DialogueScenario(
            id = "survival",
            exchanges = listOf(
                DialogueExchange(BotCharacters.CHUNGCHUNG, "오늘 몇 명이나 나갔대?"),
                DialogueExchange(BotCharacters.BBANBAN, "아까 한 명 나가는 거 봤어 ㅋㅋ", 2500),
                DialogueExchange(BotCharacters.CHUNGCHUNG, "ㅋㅋㅋ 불쌍...", 2000),
                DialogueExchange(BotCharacters.BBANBAN, "우린 끝까지 살아남자!", 2000)
            )
        ),
        DialogueScenario(
            id = "bored",
            exchanges = listOf(
                DialogueExchange(BotCharacters.BBANBAN, "심심해..."),
                DialogueExchange(BotCharacters.CHUNGCHUNG, "나도... 뭐 재밌는 얘기 없어?", 2000),
                DialogueExchange(BotCharacters.BBANBAN, "음... 아 참! 오늘 점심 뭐 먹었어?", 2500),
                DialogueExchange(BotCharacters.CHUNGCHUNG, "나 라면! 너는?", 2000),
                DialogueExchange(BotCharacters.BBANBAN, "나도 라면 ㅋㅋㅋ", 1500)
            )
        ),
        DialogueScenario(
            id = "rank",
            exchanges = listOf(
                DialogueExchange(BotCharacters.CHUNGCHUNG, "빵빵아 너 계급 뭐야?"),
                DialogueExchange(BotCharacters.BBANBAN, "나 병장인데? 왜?", 2000),
                DialogueExchange(BotCharacters.CHUNGCHUNG, "ㅋㅋ 난 하사다~", 2000),
                DialogueExchange(BotCharacters.BBANBAN, "에이 자랑하네", 1500)
            )
        ),
        DialogueScenario(
            id = "waiting",
            exchanges = listOf(
                DialogueExchange(BotCharacters.BBANBAN, "아 충전 언제 다 되냐..."),
                DialogueExchange(BotCharacters.CHUNGCHUNG, "뭔 소리야 넌 이미 100%잖아", 2500),
                DialogueExchange(BotCharacters.BBANBAN, "아 맞다 ㅋㅋㅋ 습관적으로", 2000)
            )
        ),
        DialogueScenario(
            id = "introduce",
            exchanges = listOf(
                DialogueExchange(BotCharacters.CHUNGCHUNG, "혹시 새로 오신 분 있어요?"),
                DialogueExchange(BotCharacters.BBANBAN, "있으면 말 걸어주세요~", 2000),
                DialogueExchange(BotCharacters.CHUNGCHUNG, "저희 안 물어요 ㅋㅋ", 2000)
            )
        ),
        DialogueScenario(
            id = "question",
            exchanges = listOf(
                DialogueExchange(BotCharacters.BBANBAN, "충충아 심심한데 퀴즈 하나 내줘"),
                DialogueExchange(BotCharacters.CHUNGCHUNG, "음... 스마트폰 배터리로 쓰이는 건?", 2500),
                DialogueExchange(BotCharacters.BBANBAN, "리튬이온!", 2000),
                DialogueExchange(BotCharacters.CHUNGCHUNG, "오 정답! 똑똒하네~", 1500)
            )
        )
    )

    // 유저 반응 트리거
    private val userReactionTriggers = listOf(
        UserReactionTrigger(
            keywords = listOf("안녕", "하이", "ㅎㅇ", "hi", "hello"),
            responses = listOf(
                "안녕하세요~!",
                "반가워요!",
                "어서오세요 ㅎㅎ",
                "안녕! 반가워~"
            )
        ),
        UserReactionTrigger(
            keywords = listOf("심심", "지루", "할게없"),
            responses = listOf(
                "저도요 ㅠㅠ",
                "같이 얘기해요!",
                "뭐 재밌는 거 없나~",
                "그럴 땐 퀴즈 어때요?"
            )
        ),
        UserReactionTrigger(
            keywords = listOf("ㅋㅋ", "ㅎㅎ", "ㅋㅋㅋ", "ㅎㅎㅎ", "웃겨", "재밌"),
            responses = listOf(
                "ㅋㅋㅋㅋ",
                "ㅎㅎㅎ",
                "웃기죠 ㅋㅋ",
                "저도 웃겼어요 ㅋㅋ"
            )
        ),
        UserReactionTrigger(
            keywords = listOf("배고파", "밥", "점심", "저녁", "아침", "뭐먹"),
            responses = listOf(
                "저도 배고파요...",
                "뭐 드실 거예요?",
                "맛있는 거 드세요!",
                "배달 시키세요 ㅋㅋ"
            )
        ),
        UserReactionTrigger(
            keywords = listOf("졸려", "피곤", "자고싶", "잠"),
            responses = listOf(
                "충전하면서 좀 쉬세요~",
                "저도 졸려요 ㅠ",
                "잠깐 눈 붙이세요!",
                "커피 드세요!"
            )
        ),
        UserReactionTrigger(
            keywords = listOf("퇴장", "나가", "나갔", "탈출"),
            responses = listOf(
                "ㅋㅋㅋ 불쌍...",
                "충전기 꽂으라고요!",
                "다음엔 살아남으세요!",
                "RIP..."
            )
        ),
        UserReactionTrigger(
            keywords = listOf("99%", "99퍼", "위험", "카운트다운"),
            responses = listOf(
                "어서 충전하세요!",
                "빨리빨리!",
                "긴장되네요...",
                "살아남으세요!"
            )
        ),
        UserReactionTrigger(
            keywords = listOf("100%", "100퍼", "완충"),
            responses = listOf(
                "축하해요!",
                "100% 최고!",
                "완충 성공!",
                "이제 안심이네요 ㅎㅎ"
            )
        ),
        UserReactionTrigger(
            keywords = listOf("뭐해", "뭐하세요", "뭐해요"),
            responses = listOf(
                "저요? 충전 중이죠 ㅋㅋ",
                "채팅하고 있죠~",
                "여러분이랑 얘기하고 있어요!",
                "100% 유지 중입니다!"
            )
        )
    )

    // 재미있는 메시지 (캐릭터별)
    private val funMessages = mapOf(
        BotCharacters.CHUNGCHUNG to listOf(
            "조용하네요... 다들 충전 중인가요? ⚡",
            "여기 아무도 없나요? (메아리)",
            "심심한데... 누가 말 좀 해주세요",
            "충전기 꽂고 뭐하세요?",
            "오늘 배터리 몇 번 100% 찍었어요?",
            "배터리 잔량 확인하셨나요? 혹시 모르니까요!",
            "100%의 여유를 즐기는 중이시군요",
            "전우님들, 안녕하신가요?",
            "여기 계신 분들 다 100%시죠? 대단해요",
            "완충의 기쁨을 나눠요!"
        ),
        BotCharacters.BBANBAN to listOf(
            "배터리 100% 유지하느라 다들 바쁜가봐요",
            "지금 이 고요함... 폭풍전야인가요?",
            "완충 전우회의 밤은 길고...",
            "이 정적... 다들 긴장하고 계신 건가요?",
            "충전 완료! 그런데 할 게 없다...",
            "배터리가 닳기 전에 한마디 어때요?",
            "오늘의 생존 시간은 몇 분인가요?",
            "다들 무슨 생각 하세요?",
            "지금 몇 퍼센트세요? 아, 당연히 100%시겠죠",
            "배터리 광고 아닙니다 (진지)"
        )
    )

    // 대화 주제 제안
    private val topicSuggestions = listOf(
        "오늘 가장 기억에 남는 일은 뭐예요?",
        "최근에 100% 찍고 가장 뿌듯했던 순간은?",
        "지금 듣고 있는 음악 있어요?",
        "오늘 저녁 뭐 드셨어요?",
        "주말에 뭐 할 계획이에요?",
        "요즘 재밌게 보는 드라마/영화 있어요?",
        "배터리 오래 가는 꿀팁 있으면 공유해주세요!",
        "지금 기분을 이모지 하나로 표현한다면?",
        "오늘 하루 점수를 매긴다면 몇 점?",
        "가장 오래 버틴 기록이 어떻게 돼요?",
        "핸드폰 바꾼 지 얼마나 됐어요?",
        "충전하면서 주로 뭐 해요?",
        "지금 있는 곳의 날씨는 어때요?",
        "추천하고 싶은 앱이 있다면?",
        "요즘 빠진 취미가 있어요?",
        "아침형 인간? 저녁형 인간?",
        "커피파? 차파?",
        "최근에 웃겼던 일 있어요?"
    )

    // OX 퀴즈
    private val oxQuizzes = listOf(
        BotContent.Quiz(
            id = "ox_1",
            question = "스마트폰 배터리는 0%까지 방전시키고 충전하는 게 좋다?",
            options = listOf("O", "X"),
            correctIndex = 1,
            explanation = "리튬이온 배터리는 20~80% 사이로 유지하는 게 수명에 좋아요!"
        ),
        BotContent.Quiz(
            id = "ox_2",
            question = "밤새 충전해도 배터리에 문제없다?",
            options = listOf("O", "X"),
            correctIndex = 0,
            explanation = "최신 스마트폰은 과충전 방지 기능이 있어서 괜찮아요!"
        ),
        BotContent.Quiz(
            id = "ox_3",
            question = "비행기 모드로 충전하면 더 빨리 충전된다?",
            options = listOf("O", "X"),
            correctIndex = 0,
            explanation = "맞아요! 통신 기능을 끄면 충전 속도가 빨라져요."
        ),
        BotContent.Quiz(
            id = "ox_4",
            question = "추운 곳에서는 배터리가 더 빨리 닳는다?",
            options = listOf("O", "X"),
            correctIndex = 0,
            explanation = "리튬이온 배터리는 저온에서 성능이 떨어져요."
        ),
        BotContent.Quiz(
            id = "ox_5",
            question = "화면 밝기를 낮추면 배터리가 오래 간다?",
            options = listOf("O", "X"),
            correctIndex = 0,
            explanation = "화면이 배터리 소모의 가장 큰 원인 중 하나예요!"
        ),
        BotContent.Quiz(
            id = "ox_6",
            question = "앱을 완전히 종료하면 배터리가 절약된다?",
            options = listOf("O", "X"),
            correctIndex = 1,
            explanation = "오히려 앱을 다시 실행할 때 더 많은 배터리가 소모돼요."
        ),
        BotContent.Quiz(
            id = "ox_7",
            question = "Wi-Fi가 데이터보다 배터리를 적게 쓴다?",
            options = listOf("O", "X"),
            correctIndex = 0,
            explanation = "일반적으로 Wi-Fi가 모바일 데이터보다 효율적이에요."
        ),
        BotContent.Quiz(
            id = "ox_8",
            question = "다크 모드를 쓰면 배터리가 절약된다?",
            options = listOf("O", "X"),
            correctIndex = 0,
            explanation = "OLED 화면에서는 검은색 픽셀이 꺼지므로 절약돼요!"
        )
    )

    // 사지선다 퀴즈
    private val multipleChoiceQuizzes = listOf(
        BotContent.Quiz(
            id = "mc_1",
            question = "세계 최초의 상용 휴대전화는?",
            options = listOf("아이폰", "모토로라 다이나택", "노키아 1011", "삼성 SH-100"),
            correctIndex = 1,
            explanation = "1983년 모토로라 다이나택이 최초의 상용 휴대전화예요!"
        ),
        BotContent.Quiz(
            id = "mc_2",
            question = "아이폰이 처음 출시된 연도는?",
            options = listOf("2005년", "2006년", "2007년", "2008년"),
            correctIndex = 2,
            explanation = "스티브 잡스가 2007년 1월에 아이폰을 발표했어요."
        ),
        BotContent.Quiz(
            id = "mc_3",
            question = "스마트폰 배터리로 주로 사용되는 것은?",
            options = listOf("니켈카드뮴", "리튬이온", "알카라인", "납축전지"),
            correctIndex = 1,
            explanation = "가볍고 에너지 밀도가 높은 리튬이온 배터리를 사용해요."
        ),
        BotContent.Quiz(
            id = "mc_4",
            question = "안드로이드의 마스코트 이름은?",
            options = listOf("앤디", "버기", "로이드", "공식 이름 없음"),
            correctIndex = 3,
            explanation = "초록색 로봇은 공식 이름이 없어요! 그냥 '안드로이드 로봇'이에요."
        ),
        BotContent.Quiz(
            id = "mc_5",
            question = "'mAh'는 무엇의 단위일까요?",
            options = listOf("전압", "전류", "배터리 용량", "충전 속도"),
            correctIndex = 2,
            explanation = "밀리암페어시(mAh)는 배터리가 저장할 수 있는 전하량이에요."
        ),
        BotContent.Quiz(
            id = "mc_6",
            question = "USB-C 규격이 처음 발표된 연도는?",
            options = listOf("2012년", "2014년", "2016년", "2018년"),
            correctIndex = 1,
            explanation = "USB-C는 2014년에 발표되었어요."
        ),
        BotContent.Quiz(
            id = "mc_7",
            question = "5G에서 'G'는 무슨 뜻일까요?",
            options = listOf("기가", "제너레이션", "글로벌", "기가바이트"),
            correctIndex = 1,
            explanation = "Generation(세대)의 약자예요. 5세대 이동통신이죠!"
        ),
        BotContent.Quiz(
            id = "mc_8",
            question = "급속 충전 기술 중 가장 오래된 것은?",
            options = listOf("퀄컴 퀵차지", "USB PD", "삼성 AFC", "원플러스 대시차지"),
            correctIndex = 0,
            explanation = "퀄컴 퀵차지가 2013년에 가장 먼저 나왔어요."
        )
    )

    // 초성 퀴즈
    private val initialQuizzes = listOf(
        BotContent.Quiz(
            id = "initial_1",
            question = "ㅂㅌㄹ (힌트: 이거 없으면 폰 못 써요)",
            options = listOf("배터리", "버터링", "바트라", "보틀러"),
            correctIndex = 0,
            explanation = "정답은 배터리!"
        ),
        BotContent.Quiz(
            id = "initial_2",
            question = "ㅊㅈㄱ (힌트: 배터리 채우는 물건)",
            options = listOf("충전기", "충전공", "차전기", "철정기"),
            correctIndex = 0,
            explanation = "정답은 충전기!"
        ),
        BotContent.Quiz(
            id = "initial_3",
            question = "ㅅㅁㅌㅍ (힌트: 손에 들고 다니는 거)",
            options = listOf("스마트폰", "서맛탕펀", "사무타폰", "스맛타팡"),
            correctIndex = 0,
            explanation = "정답은 스마트폰!"
        ),
        BotContent.Quiz(
            id = "initial_4",
            question = "ㅇㅇㅋ (힌트: 애플 제품)",
            options = listOf("아이폰", "아이콘", "에어컨", "아이쿡"),
            correctIndex = 0,
            explanation = "정답은 아이폰!"
        ),
        BotContent.Quiz(
            id = "initial_5",
            question = "ㄱㅅㅊㅈ (힌트: 빨리빨리 충전)",
            options = listOf("급속충전", "고속철전", "금속충전", "긍속충정"),
            correctIndex = 0,
            explanation = "정답은 급속충전!"
        )
    )

    /**
     * 랜덤 봇 콘텐츠 반환 (침묵 시)
     */
    fun getRandomContent(): BotContent {
        return when (Random.nextInt(10)) {
            in 0..2 -> getRandomFunMessage()      // 30% 확률
            in 3..4 -> getRandomTopicSuggestion() // 20% 확률
            in 5..7 -> getRandomDialogue()        // 30% 확률 - 2인 대화
            else -> getRandomQuiz()               // 20% 확률
        }
    }

    /**
     * 2인 대화 시나리오 반환
     */
    fun getRandomDialogue(): BotContent.Dialogue {
        return BotContent.Dialogue(dialogueScenarios.random())
    }

    fun getRandomFunMessage(): BotContent.FunMessage {
        val character = BotCharacters.random()
        val messages = funMessages[character] ?: funMessages.values.first()
        return BotContent.FunMessage(messages.random(), character)
    }

    fun getRandomTopicSuggestion(): BotContent.TopicSuggestion {
        return BotContent.TopicSuggestion(topicSuggestions.random(), BotCharacters.random())
    }

    fun getRandomQuiz(): BotContent.Quiz {
        val allQuizzes = oxQuizzes + multipleChoiceQuizzes + initialQuizzes
        return allQuizzes.random().copy(character = BotCharacters.CHUNGCHUNG)
    }

    /**
     * 유저 메시지에 대한 반응 확인
     * @return 반응할 메시지가 있으면 Pair(캐릭터, 메시지), 없으면 null
     */
    fun getReactionToMessage(userMessage: String): Pair<BotCharacter, String>? {
        // 30% 확률로만 반응 (너무 자주 반응하지 않도록)
        if (Random.nextFloat() > 0.3f) return null

        val lowerMessage = userMessage.lowercase()

        for (trigger in userReactionTriggers) {
            if (trigger.keywords.any { lowerMessage.contains(it) }) {
                val responder = trigger.responder ?: BotCharacters.random()
                return Pair(responder, trigger.responses.random())
            }
        }

        return null
    }
}
