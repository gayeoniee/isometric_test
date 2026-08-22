package com.daengs.app.miniroom.sprite

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/** 스프라이트 시트 한 장. 격자로 잘린 프레임들이 왼쪽 위부터 행 우선으로 들어있다. */
@Immutable
data class SpriteSheet(
    val image: ImageBitmap,
    val frameWidth: Int,
    val frameHeight: Int,
    val columns: Int,
    val frameCount: Int,
    val fps: Int = 8,
    /** 축소 보간 방식. 픽셀 아트면 [FilterQuality.None], 부드러운 아트면 보간. */
    val filterQuality: FilterQuality = FilterQuality.Medium,
) {
    fun srcOffset(frame: Int): IntOffset {
        val i = frame.coerceIn(0, frameCount - 1)
        return IntOffset((i % columns) * frameWidth, (i / columns) * frameHeight)
    }
}

/** 시간 → 프레임 인덱스. 시트가 없어도 폴백 아트가 같은 타임라인을 쓸 수 있게 분리해 둔다. */
fun frameIndexAt(timeMs: Long, frameCount: Int, fps: Int): Int {
    if (frameCount <= 0 || fps <= 0) return 0
    return ((timeMs * fps / 1000L) % frameCount).toInt()
}

/**
 * 방 Canvas **안에서** 호출한다 — 강아지도 다른 아이템과 똑같이 col+row 로 정렬돼야 하므로
 * 별도 Composable 이 아니라 DrawScope 확장이어야 한다.
 *
 * 위치는 바깥의 translate 가 이미 잡아준 상태에서 (0,0) 에 그린다.
 * dstOffset 이 IntOffset(정수 px)이라 여기서 위치를 잡으면 드래그가 계단처럼 떨린다.
 */
fun DrawScope.drawSpriteFrame(
    sheet: SpriteSheet,
    frame: Int,
    dstSize: Size,
    flipHorizontal: Boolean = false,
    alpha: Float = 1f,
) {
    withTransform({
        if (flipHorizontal) scale(-1f, 1f, pivot = Offset(dstSize.width / 2f, 0f))
    }) {
        drawImage(
            image = sheet.image,
            srcOffset = sheet.srcOffset(frame),
            srcSize = IntSize(sheet.frameWidth, sheet.frameHeight),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(dstSize.width.roundToInt(), dstSize.height.roundToInt()),
            alpha = alpha,
            // scale 이 1.0 인 경우가 사실상 없으므로 보간 방식이 눈에 그대로 드러난다.
            // 에셋마다 다르므로 SpriteSheet 가 들고 있는 값을 쓴다.
            filterQuality = sheet.filterQuality,
        )
    }
}
