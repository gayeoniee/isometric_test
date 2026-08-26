package com.daengs.app.ui.dex.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// 카드 한 장
//
// 그림 위에 포일을 얹고, 손가락 위치에 따라 3D 로 기울인다. 웹판과 같은 구조다.
//
// **오프스크린 레이어가 핵심이다.** 포일은 `mix-blend-mode` 로 "아래 있는 것"과
// 섞이는데, 그 아래가 카드 그림이어야지 앱 배경이면 안 된다. 그래서 그림과 포일을
// 같은 레이어 안에서 그린다.
// ---------------------------------------------------------------------------

/** 기울기. 저쪽 `--rotate-x/y` 와 같은 계수다. */
private const val ROTATE_X = 22f
private const val ROTATE_Y = 25f

/**
 * 홀로그램 카드.
 *
 * @param art 카드 그림. 프레임·제목·수치가 전부 구워져 있다
 * @param input 손가락/자이로. [FoilInput.Idle] 이면 포일이 잠잠하다
 * @param tilt 3D 로 기울일지. 그리드에서는 끄고 확대 뷰에서만 켠다
 */
@Composable
fun HoloCard(
    art: ImageBitmap?,
    foil: Foil,
    input: FoilInput,
    modifier: Modifier = Modifier,
    tune: FoilTune = FoilTune(),
    tilt: Boolean = true,
) {
    val ratio = if (art != null) art.width.toFloat() / art.height else 0.8f
    Canvas(
        modifier
            .aspectRatio(ratio)
            .graphicsLayer {
                // 포일이 카드 그림과 섞이려면 둘이 같은 레이어에 있어야 한다.
                compositingStrategy = CompositingStrategy.Offscreen
                if (tilt) {
                    rotationX = (0.5f - input.p.y) * ROTATE_X * input.intensity
                    rotationY = (input.p.x - 0.5f) * ROTATE_Y * input.intensity
                    // 원근이 없으면 회전이 그냥 찌그러짐으로 보인다
                    cameraDistance = 14f * density
                }
            },
    ) {
        if (art != null) {
            drawImage(
                image = art,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.High,
            )
        }
        drawFoil(foil, input, tune)
    }
}

/**
 * 카드를 문질러 포일을 보는 손짓.
 *
 * 저쪽이 정한 규칙을 그대로 따른다 — **카드 안쪽 드래그는 전부 포일 구경**이다.
 * "느리면 구경, 빠르면 넘기기"로 속도를 재서 가르는 건 저쪽이 이미 해보고 버렸다
 * (README: 포일을 구경하다 보면 손이 저절로 빨라져서 어떤 문턱도 안 먹는다).
 *
 * @param onTap 움직이지 않고 뗐을 때. 확대 열기·설명 열기에 쓴다
 */
@Composable
fun rememberRubState(onTap: (() -> Unit)? = null): RubState {
    val state = remember { RubState() }
    state.onTap = onTap
    return state
}

class RubState {
    var input by mutableStateOf(FoilInput.Idle)
        internal set

    internal var onTap: (() -> Unit)? = null

    internal fun move(p: Offset, size: androidx.compose.ui.geometry.Size) {
        val n = Offset((p.x / size.width).coerceIn(0f, 1f), (p.y / size.height).coerceIn(0f, 1f))
        input = FoilInput.of(n, 1f)
    }

    internal fun release() {
        // 손을 떼면 포일이 잦아든다. 위치는 그대로 둬서 홱 튀지 않게 한다.
        input = input.copy(intensity = 0f)
    }
}

/** [RubState] 를 붙인다. 카드 자체에 걸어야 좌표가 카드 기준이 된다. */
fun Modifier.rubbable(state: RubState): Modifier = this.pointerInput(state) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        state.move(down.position, size.toSize())
        var moved = false
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) moved = true
            if (moved) {
                state.move(change.position, size.toSize())
                change.consume()
            }
            if (!change.pressed) break
        }
        state.release()
        if (!moved) state.onTap?.invoke()
    }
}

private fun androidx.compose.ui.unit.IntSize.toSize() =
    androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat())
