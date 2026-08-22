package com.daengs.app.miniroom.sprite

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.tooling.preview.Preview

/**
 * 프레임 시계. 시트 하나당 하나씩 만들 필요 없이, 이 raw 시간에서
 * 여러 애니메이션(강아지 대기, 화분 흔들림 ...)의 프레임 인덱스를 각자 뽑아 쓴다.
 *
 * 반드시 **draw 람다 안에서** 읽을 것. 그래야 recomposition 없이
 * draw 단계만 무효화돼서 드래그 중에도 60fps 가 나온다.
 */
@Composable
fun rememberFrameClock(): State<Long> = produceState(0L) {
    while (true) {
        withInfiniteAnimationFrameMillis { value = it }
    }
}

/**
 * 방 Canvas 밖에서 쓰는 독립 스프라이트 컴포넌트 (아이템 고르기, 프로필 아바타 등).
 *
 * @param sheet null 이면 아직 에셋이 없다는 뜻 → [fallback] 이 같은 프레임 인덱스를 받아 그린다.
 * @param frameTimeMs null 이 아니면 그 시각으로 프레임을 고정한다.
 *   @Preview 와 스크린샷 테스트에서 무한 애니메이션이 멈춰 프레임 0 에 얼어붙는 걸 피하기 위한 것.
 */
@Composable
fun SpriteAnimation(
    sheet: SpriteSheet?,
    frameSize: Size,
    modifier: Modifier = Modifier,
    frameCount: Int = sheet?.frameCount ?: 1,
    fps: Int = sheet?.fps ?: 8,
    flipHorizontal: Boolean = false,
    frameTimeMs: Long? = null,
    fallback: DrawScope.(frame: Int) -> Unit = {},
) {
    val clock = rememberFrameClock()
    Canvas(modifier) {
        val t = frameTimeMs ?: clock.value
        val frame = frameIndexAt(t, frameCount, fps)
        val s = if (frameSize.width > 0f) size.width / frameSize.width else 1f
        withTransform({ scale(s, s, pivot = androidx.compose.ui.geometry.Offset.Zero) }) {
            if (sheet != null) {
                drawSpriteFrame(sheet, frame, frameSize, flipHorizontal)
            } else {
                fallback(frame)
            }
        }
    }
}

@Preview(widthDp = 96, heightDp = 96, showBackground = true)
@Composable
private fun SpriteAnimationFallbackPreview() {
    SpriteAnimation(
        sheet = null,
        frameSize = Size(64f, 72f),
        modifier = Modifier,
        frameCount = 4,
        frameTimeMs = 500L,
        fallback = { frame ->
            drawCircle(
                color = androidx.compose.ui.graphics.Color(0xFFE8C9A0),
                radius = 20f + frame * 2f,
                center = androidx.compose.ui.geometry.Offset(32f, 36f),
            )
        },
    )
}
