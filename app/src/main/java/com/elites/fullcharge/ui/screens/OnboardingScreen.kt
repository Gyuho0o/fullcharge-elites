package com.elites.fullcharge.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elites.fullcharge.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String
)

private val onboardingPages = listOf(
    OnboardingPage(
        emoji = "🔋",
        title = "완충 전우회에 오신 걸\n환영합니다",
        description = "배터리 100%인 사람만\n입장할 수 있는 특별한 채팅방이에요"
    ),
    OnboardingPage(
        emoji = "⚡",
        title = "입장 조건",
        description = "배터리가 100%일 때만\n입장 버튼이 활성화돼요\n\n충전 중이든 아니든 상관없어요"
    ),
    OnboardingPage(
        emoji = "⏱️",
        title = "생존 규칙",
        description = "채팅 중 배터리가 99%로 떨어지면\n10초 카운트다운이 시작돼요\n\n10초 안에 충전하지 않으면 퇴장!"
    ),
    OnboardingPage(
        emoji = "🎖️",
        title = "계급 시스템",
        description = "채팅방에 오래 머물수록\n계급이 올라가요"
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWhite)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 여백 (건너뛰기 버튼 제거됨)
            Spacer(modifier = Modifier.height(48.dp))

            // 페이지 콘텐츠 (스와이프 가능)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(
                    page = onboardingPages[page]
                )
            }

            // 페이지 인디케이터
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 32.dp)
            ) {
                onboardingPages.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (index == pagerState.currentPage) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) TossBlue
                                else DividerGray
                            )
                    )
                }
            }

            // 다음/시작하기 버튼
            Button(
                onClick = {
                    if (isLastPage) {
                        onComplete()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TossBlue
                )
            ) {
                Text(
                    text = if (isLastPage) "시작하기" else "다음",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 건너뛰기 버튼 (다음 버튼 아래)
            TextButton(
                onClick = onComplete,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "건너뛰기",
                    color = TextTertiary,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 이모지
        Text(
            text = page.emoji,
            fontSize = 80.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 제목
        Text(
            text = page.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 설명
        Text(
            text = page.description,
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}
