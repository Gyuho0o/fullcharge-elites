package com.elites.fullcharge.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elites.fullcharge.ui.theme.*

/**
 * 실시간 합류/퇴장 카운트를 표시하는 플로팅 인디케이터
 * 프로그레스바 하단에 표시되며, 애니메이션과 함께 나타났다 사라짐
 */
@Composable
fun JoinLeaveIndicator(
    joinCount: Int,
    leaveCount: Int,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible && (joinCount > 0 || leaveCount > 0),
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300)
        ),
        exit = fadeOut(animationSpec = tween(500)) + slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(500)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            CardBlack.copy(alpha = 0.95f),
                            SurfaceBlack.copy(alpha = 0.95f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 합류 카운트
            if (joinCount > 0) {
                JoinCountBadge(count = joinCount)
            }

            // 퇴장 카운트
            if (leaveCount > 0) {
                LeaveCountBadge(count = leaveCount)
            }
        }
    }
}

@Composable
private fun JoinCountBadge(count: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "join_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "join_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.alpha(alpha)
    ) {
        Text(
            text = "+$count",
            color = EliteGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "합류",
            color = EliteGreen.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LeaveCountBadge(count: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "leave_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "leave_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.alpha(alpha)
    ) {
        Text(
            text = "-$count",
            color = CrisisRed,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "퇴장",
            color = CrisisRed.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
