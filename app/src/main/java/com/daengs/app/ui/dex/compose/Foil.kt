package com.daengs.app.ui.dex.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

// ---------------------------------------------------------------------------
// 홀로그램 포일 — `vendor/cards-css/*.css` 를 Compose 로 옮긴 것
//
// 웹판은 카드 그림 위에 레이어를 여러 장 얹고 CSS 블렌드 모드로 섞는다. 한 장이
// 대략 이렇게 생겼다.
//
//     background-image: <그라디언트 A>, <그라디언트 B>;
//     background-blend-mode: multiply;      <- A 와 B 를 자기들끼리 섞고
//     filter: brightness() contrast() saturate();
//     mix-blend-mode: color-dodge;          <- 그 결과를 카드 그림에 섞는다
//     opacity: ...;
//
// Compose 에도 같은 부품이 다 있다.
//
//   background-image      -> Brush (linear / radial / sweep)
//   background-blend-mode -> saveLayer 안에서 두 번째 brush 를 blendMode 로 그리기
//   filter                -> ColorFilter.colorMatrix
//   mix-blend-mode        -> saveLayer 의 Paint 에 blendMode
//
// **딱 하나 없는 게 `repeating-conic-gradient` 다.** Compose 의 sweepGradient 는
// 한 바퀴만 돈다. 그래서 주기를 손으로 반복해 넣는다 ([repeatingSweep]) — 60도짜리
// 무늬면 색을 여섯 번 이어 붙여 360도를 채우는 식이고, 결과는 완전히 같다.
//
// ## 카드 전체가 한 레이어여야 한다
//
// `mix-blend-mode` 는 "아래 있는 것과 섞는다"는 뜻이라, 카드 그림과 포일이 같은
// 오프스크린 레이어 안에 있어야 한다. 밖에 있으면 앱 배경과 섞여 버린다.
// [HoloCard] 가 `CompositingStrategy.Offscreen` 으로 그 레이어를 만든다.
// ---------------------------------------------------------------------------

/** 카드 12장이 쓰는 포일. 이름은 저쪽 `rarity` 값 그대로다. */
enum class Foil { Prism, Crystal, Gold, Oilslick, Sunburst, Holo, Reverse, Aurora, Cosmos, Mosaic, Metal }

/**
 * 손가락(또는 자이로)이 만든 입력. 전부 0~1 이다.
 *
 * @param p 카드 안에서의 위치. 저쪽 `--pointer-x/y`
 * @param fromCenter 가운데에서 얼마나 멀리 있나. **반지름 0.5 를 1 로 본다** —
 *   저쪽 주석대로 `reverse` 가 이걸로 가운데를 죽이고 가장자리를 살리므로 정규화를
 *   바꾸면 그 티어가 무너진다
 * @param intensity 포일 세기. 손을 떼면 0 으로 잦아든다
 */
data class FoilInput(val p: Offset, val fromCenter: Float, val intensity: Float) {
    companion object {
        val Idle = FoilInput(Offset(0.5f, 0.5f), 0f, 0f)

        fun of(p: Offset, intensity: Float) = FoilInput(
            p = p,
            fromCenter = min(hypot(p.x - 0.5f, p.y - 0.5f) / 0.5f, 1f),
            intensity = intensity,
        )
    }
}

/**
 * 세기 손잡이. 저쪽 `--hc-*` 와 같은 자리다.
 *
 * 웹판에서 정한 값을 그대로 쓴다 — 원화가 밝고 주인공이 흰 강아지라 포일을 그대로
 * 얹으면 얼굴이 날아간다는 게 저쪽 README 의 요지다.
 */
data class FoilTune(
    val brightness: Float = 1f,
    val contrast: Float = 1f,
    val saturate: Float = 1f,
    /**
     * 포일 세기. **낮게 시작한다.**
     *
     * 처음에 0.72 로 뒀더니 얼굴이 통째로 날아갔다. 저쪽 README 가 경고한 그대로다 —
     * 포일 대부분이 `color-dodge` 라 밝은 원화에 얹으면 흰색으로 클리핑되고, 제일
     * 먼저 사라지는 게 흰 강아지 얼굴이다. 웹판은 vendor CSS 안에 이미 눌러 둔 값이
     * 들어 있지만 우리는 그 값을 다시 잡아야 한다.
     */
    val shineOpacity: Float = 0.30f,
    val glareOpacity: Float = 0.35f,
)

/** 저쪽 `--sunpillar-1..6`. 무지개 한 주기를 이루는 여섯 색이다. */
internal val SUNPILLAR = listOf(
    hsl(2f, 1f, 0.73f),
    hsl(53f, 1f, 0.69f),
    hsl(93f, 1f, 0.69f),
    hsl(176f, 1f, 0.76f),
    hsl(228f, 1f, 0.74f),
    hsl(283f, 1f, 0.73f),
)

// -- 부품 -------------------------------------------------------------------

/**
 * `repeating-conic-gradient` 흉내.
 *
 * Compose 의 sweepGradient 는 0~1 을 한 바퀴로 본다. [periodDeg] 짜리 무늬를
 * 360/period 번 이어 붙여 채운다. [rotateDeg] 만큼 돌린다.
 */
internal fun repeatingSweep(
    colors: List<Color>,
    periodDeg: Float,
    center: Offset,
    rotateDeg: Float,
): Brush {
    val cycles = max(1, (360f / periodDeg).toInt())
    val stops = ArrayList<Pair<Float, Color>>(cycles * colors.size + 1)
    val shift = ((rotateDeg / 360f) % 1f + 1f) % 1f
    for (c in 0 until cycles) {
        for ((i, color) in colors.withIndex()) {
            val at = (c + i.toFloat() / colors.size) / cycles
            stops += (((at + shift) % 1f) to color)
        }
    }
    stops.sortBy { it.first }
    // 시작과 끝을 이어 준다. 안 그러면 0 도 자리에 이음매가 보인다.
    val first = stops.first()
    val closed = stops + (1f to first.second)
    return Brush.sweepGradient(colorStops = closed.toTypedArray(), center = center)
}

/** `radial-gradient(farthest-corner circle at ...)`. 반지름은 제일 먼 모서리까지다. */
internal fun radialAt(
    center: Offset,
    size: Size,
    vararg stops: Pair<Float, Color>,
): Brush {
    val r = max(
        max(hypot(center.x, center.y), hypot(size.width - center.x, center.y)),
        max(hypot(center.x, size.height - center.y), hypot(size.width - center.x, size.height - center.y)),
    )
    return Brush.radialGradient(colorStops = stops, center = center, radius = max(r, 1f))
}

/** 반복 선형 그라디언트. `repeating-linear-gradient` 자리. */
internal fun repeatingLinear(
    colors: List<Color>,
    from: Offset,
    to: Offset,
): Brush = Brush.linearGradient(colors = colors, start = from, end = to, tileMode = TileMode.Repeated)

/**
 * `filter: brightness() contrast() saturate()` 를 행렬 하나로.
 *
 * CSS 는 왼쪽부터 차례로 먹이지만, 행렬 곱으로 합치면 한 번에 끝난다.
 * 채도 -> 대비 -> 밝기 순으로 곱한다 (CSS 와 같은 순서).
 */
internal fun filterOf(brightness: Float, contrast: Float, saturate: Float): ColorFilter {
    val m = ColorMatrix().apply { setToSaturation(saturate) }
    // 대비: 가운데(0.5)를 축으로 벌린다
    val t = (1f - contrast) * 0.5f * 255f
    val c = floatArrayOf(
        contrast, 0f, 0f, 0f, t,
        0f, contrast, 0f, 0f, t,
        0f, 0f, contrast, 0f, t,
        0f, 0f, 0f, 1f, 0f,
    )
    val out = ColorMatrix(c).apply { timesAssign(m) }
    // 밝기: 곱하기
    val b = ColorMatrix(
        floatArrayOf(
            brightness, 0f, 0f, 0f, 0f,
            0f, brightness, 0f, 0f, 0f,
            0f, 0f, brightness, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
    b.timesAssign(out)
    return ColorFilter.colorMatrix(b)
}

internal fun hsl(h: Float, s: Float, l: Float, a: Float = 1f): Color {
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val hp = (h % 360f) / 60f
    val x = c * (1f - kotlin.math.abs(hp % 2f - 1f))
    val (r, g, b) = when (hp.toInt()) {
        0 -> Triple(c, x, 0f); 1 -> Triple(x, c, 0f); 2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c); 4 -> Triple(x, 0f, c); else -> Triple(c, 0f, x)
    }
    val m = l - c / 2f
    return Color(r + m, g + m, b + m, a)
}

internal fun white(alpha: Float) = Color(1f, 1f, 1f, alpha)
internal fun gray(level: Float, alpha: Float = 1f) = Color(level, level, level, alpha)

/**
 * 레이어 한 장. `mix-blend-mode` + `filter` + `opacity` 를 한 덩어리로 적용한다.
 *
 * `saveLayer` 를 쓰는 이유: `filter` 는 그라디언트를 섞기 **전에** 걸려야 하고,
 * `mix-blend-mode` 는 섞은 결과를 아래와 합칠 때 걸려야 한다. 한 번의 drawRect 로는
 * 두 시점을 나눌 수 없다.
 */
internal inline fun DrawScope.foilLayer(
    blend: BlendMode,
    alpha: Float,
    filter: ColorFilter?,
    body: DrawScope.() -> Unit,
) {
    if (alpha <= 0.001f) return
    val paint = Paint().apply {
        this.blendMode = blend
        this.alpha = alpha.coerceIn(0f, 1f)
        this.colorFilter = filter
    }
    val canvas = drawContext.canvas
    canvas.saveLayer(Rect(Offset.Zero, size), paint)
    body()
    canvas.restore()
}

/** 레이어 안에서 배경 두 장을 `background-blend-mode` 로 섞는다. */
internal fun DrawScope.blendBrushes(a: Brush, b: Brush, mode: BlendMode) {
    drawRect(a)
    drawRect(b, blendMode = mode)
}
