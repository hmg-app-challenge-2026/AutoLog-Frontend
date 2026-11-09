package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.AvatarGradients

/** 디자인 언어 5.4: userId 해시로 8색 중 하나. */
fun avatarGradientIndex(userId: String): Int = Math.floorMod(userId.hashCode(), AvatarGradients.size)

/** 한글 = 첫 글자, 라틴 = 앞 두 단어의 첫 글자(대문자). */
fun initialsOf(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "?"
    val first = trimmed.first()
    return if (first.code < 128 && first.isLetter()) {
        trimmed.split(Regex("\\s+")).take(2).joinToString("") { it.take(1).uppercase() }
    } else {
        first.toString()
    }
}

/** 이니셜 크기는 아바타 크기의 40%(토큰이 아니라 비율 — 아바타 크기 자체가 토큰). */
@Composable
fun Avatar(userId: String, name: String, size: Dp, modifier: Modifier = Modifier) {
    val (start, end) = AvatarGradients[avatarGradientIndex(userId)]
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.2f))
            .background(Brush.linearGradient(listOf(start, end), start = Offset.Zero, end = Offset.Infinite)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initialsOf(name),
            style = AutoLogTheme.type.display.copy(
                fontSize = (size.value * 0.4f).sp,
                lineHeight = (size.value * 0.45f).sp,
            ),
            color = Color.White,
        )
    }
}
