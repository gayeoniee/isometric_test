package com.daengs.app.ui.dex.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------
// 포일 11종 — `vendor/cards-css/*.css` 한 장씩
//
// 레이어 순서 · 블렌드 모드 · 필터 계수는 그쪽 값을 그대로 옮겼다. 픽셀 단위까지
// 같지는 않지만 **무엇으로 만들어진 무늬인지**는 같다 — 12장이 서로 달라 보이는 게
// 이 티어 체계의 요점이고, 저쪽 README 도 "기법이 겹치면 시험할 게 없어진다"고 적었다.
//
// 부품(그라디언트 · 레이어 · 필터)은 [Foil] 파일에 있다.
// ---------------------------------------------------------------------------

/** 포인터에 따라 무늬를 미는 양. 저쪽 `calc(((50% - var(--background-x)) * K) + 50%)`. */
internal fun DrawScope.shift(p: Offset, kx: Float, ky: Float) = Offset(
    (0.5f - p.x) * kx * size.width + size.width * 0.5f,
    (0.5f - p.y) * ky * size.height + size.height * 0.5f,
)

internal fun DrawScope.at(p: Offset) = Offset(p.x * size.width, p.y * size.height)

/**
 * 카드 그림 위에 포일을 얹는다.
 *
 * **오프스크린 레이어 안에서 불러야 한다.** 블렌드 모드가 "아래 있는 것"과 섞는
 * 것이라, 카드 그림과 같은 레이어가 아니면 앱 배경과 섞여 버린다.
 */
fun DrawScope.drawFoil(foil: Foil, input: FoilInput, tune: FoilTune) {
    if (input.intensity <= 0.001f) return
    val shine = tune.shineOpacity * input.intensity
    when (foil) {
        Foil.Prism -> prism(input, tune, shine)
        Foil.Crystal -> crystal(input, tune, shine)
        Foil.Gold -> gold(input, tune, shine)
        Foil.Oilslick -> oilslick(input, tune, shine)
        Foil.Sunburst -> sunburst(input, tune, shine)
        Foil.Holo -> holo(input, tune, shine)
        Foil.Reverse -> reverse(input, tune, shine)
        Foil.Aurora -> aurora(input, tune, shine)
        Foil.Cosmos -> cosmos(input, tune, shine)
        Foil.Mosaic -> mosaic(input, tune, shine)
        Foil.Metal -> metal(input, tune, shine)
    }
    glare(input, tune)
}

/** 어느 포일에나 붙는 하이라이트. 손가락을 따라오는 둥근 빛. */
private fun DrawScope.glare(i: FoilInput, t: FoilTune) {
    val c = at(i.p)
    foilLayer(
        BlendMode.Overlay,
        t.glareOpacity * 0.9f * i.intensity,
        filterOf(0.85f * t.brightness, 1.7f * t.contrast, t.saturate),
    ) {
        drawRect(
            radialAt(
                c, size,
                0.10f to white(0.8f),
                0.40f to gray(0.65f, 0.35f),
                1f to Color(0f, 0f, 0f, 0.6f),
            ),
        )
    }
}

/** No.01·02. 무지개가 각도로 쪼개지는 분광. */
private fun DrawScope.prism(i: FoilInput, t: FoilTune, shine: Float) {
    val c = at(i.p)
    foilLayer(
        BlendMode.ColorDodge, shine * 0.9f,
        filterOf(0.75f * t.brightness, 1.7f * t.contrast, 1.05f * t.saturate),
    ) {
        blendBrushes(
            repeatingSweep(SUNPILLAR, 60f, c, (i.p.x - 0.5f) * 100f),
            // 가운데만 살리고 바깥은 눌러야 한다. 반투명이면 multiply 가 안 먹어서
            // 카드 전체가 밝아진다.
            radialAt(c, size, 0.05f to gray(0.95f), 0.45f to gray(0.35f), 1f to Color.Black),
            BlendMode.Multiply,
        )
    }
    // 얇은 흑백 살 — 각도가 조금만 바뀌어도 튄다
    foilLayer(BlendMode.Hardlight, (0.4f + i.fromCenter * 0.5f) * shine, filterOf(1.1f, 1.4f, 1f)) {
        drawRect(
            repeatingSweep(
                listOf(
                    white(0f), white(0f), white(0.55f), white(0.55f),
                    Color(0f, 0f, 0f, 0.45f), Color(0f, 0f, 0f, 0.45f), white(0f),
                ),
                24f, c, (i.p.x - 0.5f) * -60f,
            ),
        )
    }
    foilLayer(BlendMode.Luminosity, shine * 0.45f, filterOf(0.62f, 3f, 1f)) {
        drawRect(radialAt(c, size, 0f to gray(0.92f, 0.7f), 0.25f to gray(0.76f, 0.1f), 0.9f to gray(0.06f)))
    }
}

/** No.03 가지. 결정 패싯 — 면마다 따로 꺾인다. */
private fun DrawScope.crystal(i: FoilInput, t: FoilTune, shine: Float) {
    val c = at(i.p)
    fun band(color: Color, angle: Float, kx: Float, ky: Float): Brush {
        val o = shift(i.p, kx, ky)
        val d = Offset(cos(angle) * size.width * 0.22f, sin(angle) * size.height * 0.22f)
        return repeatingLinear(listOf(Color.Transparent, color, Color.Transparent), o - d, o + d)
    }
    foilLayer(
        BlendMode.ColorDodge, shine * 0.9f,
        filterOf(0.75f * t.brightness, 1.8f * t.contrast, 1.05f * t.saturate),
    ) {
        drawRect(band(SUNPILLAR[1], 0.9f, 3.2f, 1.8f))
        drawRect(band(SUNPILLAR[3], -1.1f, -2.4f, 2.6f), blendMode = BlendMode.Screen)
        drawRect(band(SUNPILLAR[5], 2.2f, 1.6f, -2.8f), blendMode = BlendMode.Screen)
        drawRect(
            radialAt(c, size, 0f to white(0.6f), 0.5f to gray(0.4f, 0.25f), 1f to Color.Black),
            blendMode = BlendMode.Screen,
        )
    }
}

/** No.04 당근. 금박 — 데모의 GOLD SECRET. */
private fun DrawScope.gold(i: FoilInput, t: FoilTune, shine: Float) {
    val c = at(i.p)
    val g1 = hsl(45f, 0.95f, 0.74f)
    val g2 = hsl(39f, 0.90f, 0.55f)
    val g3 = hsl(33f, 0.85f, 0.42f)
    val g4 = hsl(50f, 1f, 0.86f)
    val o = shift(i.p, 2.8f, 2.2f)
    val d = Offset(size.width * 0.16f, size.height * 0.10f)
    // hard-light 라 밝기를 내리면 곱하기 쪽으로 넘어가 탁해진다 (저쪽 README).
    foilLayer(BlendMode.Hardlight, shine, filterOf(0.95f * t.brightness, 1.15f * t.contrast, t.saturate)) {
        blendBrushes(
            repeatingLinear(listOf(g3, g2, g1, g4, g1, g2, g3), o - d, o + d),
            radialAt(c, size, 0f to white(0.55f), 0.55f to gray(0.35f, 0.3f), 1f to Color.Black),
            BlendMode.Hardlight,
        )
    }
}

/** No.05 단호박. 어두운 기름막 무지개. */
private fun DrawScope.oilslick(i: FoilInput, t: FoilTune, shine: Float) {
    val c = at(i.p)
    val o = shift(i.p, 2.2f, 2.6f)
    val d = Offset(size.width * 0.30f, size.height * 0.22f)
    foilLayer(
        BlendMode.ColorDodge, shine * 0.85f,
        filterOf(0.55f * t.brightness, 1.9f * t.contrast, 1.3f * t.saturate),
    ) {
        blendBrushes(
            repeatingLinear(SUNPILLAR + SUNPILLAR.first(), o - d, o + d),
            radialAt(c, size, 0f to gray(0.5f, 0.6f), 0.6f to gray(0.1f, 0.4f), 1f to Color.Black),
            BlendMode.Multiply,
        )
    }
}

/** No.06 버섯. 중심에서 뻗는 광선. */
private fun DrawScope.sunburst(i: FoilInput, t: FoilTune, shine: Float) {
    val c = at(i.p)
    foilLayer(
        BlendMode.ColorDodge, shine * 0.9f,
        filterOf(0.85f * t.brightness, 1.6f * t.contrast, 1.15f * t.saturate),
    ) {
        blendBrushes(
            repeatingSweep(listOf(SUNPILLAR[0], SUNPILLAR[2], SUNPILLAR[4]), 16f, c, (i.p.x - 0.5f) * 80f),
            radialAt(c, size, 0f to white(0.55f), 0.5f to gray(0.35f, 0.3f), 1f to Color.Black),
            BlendMode.Softlight,
        )
    }
    foilLayer(BlendMode.Overlay, (0.3f + i.fromCenter * 0.4f) * shine, filterOf(1.05f, 1.25f, 1f)) {
        drawRect(repeatingSweep(listOf(white(0f), white(0.5f), white(0f)), 12f, c, 10f - (i.p.x - 0.5f) * 120f))
    }
}

/** No.07 브로콜리. 무지개 밴드 + 세로 스캔라인. */
private fun DrawScope.holo(i: FoilInput, t: FoilTune, shine: Float) {
    val o = shift(i.p, 2.6f, 3.5f)
    val d = Offset(size.width * 0.34f, size.height * 0.12f)
    foilLayer(BlendMode.ColorDodge, shine, filterOf(1.1f * t.brightness, 1.1f * t.contrast, 1.2f * t.saturate)) {
        blendBrushes(
            repeatingLinear(SUNPILLAR + SUNPILLAR.first(), o - d, o + d),
            // 세로 스캔라인. 아주 촘촘해서 무늬라기보다 결로 읽힌다
            repeatingLinear(
                listOf(Color.Black, Color.Black, gray(0.4f), gray(0.4f)),
                Offset.Zero, Offset(4f, 0f),
            ),
            BlendMode.Overlay,
        )
    }
}

/** No.08 오이. 가운데를 죽이고 가장자리를 살리는 역전 폴오프. */
private fun DrawScope.reverse(i: FoilInput, t: FoilTune, shine: Float) {
    val c = at(i.p)
    // **가운데일수록 약해진다.** 저쪽이 이 티어를 고른 이유라 정규화를 바꾸면 안 된다.
    val a = ((1.5f - i.fromCenter) * shine).coerceIn(0f, 1f)
    foilLayer(BlendMode.ColorDodge, a, filterOf(0.55f * t.brightness, 1.5f * t.contrast, t.saturate)) {
        blendBrushes(
            radialAt(c, size, 0.05f to Color.White, 0.5f to Color.Black, 0.8f to Color.White),
            Brush.linearGradient(
                colorStops = arrayOf(0.15f to Color.Black, 0.5f to Color.White, 0.85f to Color.Black),
                start = Offset(size.width, 0f),
                end = Offset(0f, size.height),
            ),
            BlendMode.Softlight,
        )
    }
}

/** No.09 시금치. 넓고 부드러운 색 띠. */
private fun DrawScope.aurora(i: FoilInput, t: FoilTune, shine: Float) {
    val o = shift(i.p, 1.8f, 2.4f)
    val d = Offset(size.width * 0.55f, size.height * 0.40f)
    foilLayer(
        BlendMode.ColorDodge, shine * 0.8f,
        filterOf(0.8f * t.brightness, 1.25f * t.contrast, 1.2f * t.saturate),
    ) {
        drawRect(
            repeatingLinear(
                listOf(SUNPILLAR[4], SUNPILLAR[3], SUNPILLAR[2], SUNPILLAR[5], SUNPILLAR[4]),
                o - d, o + d,
            ),
        )
    }
}

/** No.10 고구마. 성운 결. */
private fun DrawScope.cosmos(i: FoilInput, t: FoilTune, shine: Float) {
    val c = at(i.p)
    val o = shift(i.p, 2.0f, 2.0f)
    foilLayer(
        BlendMode.ColorDodge, shine * 0.85f,
        filterOf(0.7f * t.brightness, 1.6f * t.contrast, 1.25f * t.saturate),
    ) {
        blendBrushes(
            radialAt(o, size, 0f to SUNPILLAR[5], 0.35f to SUNPILLAR[4], 0.7f to SUNPILLAR[0], 1f to Color.Black),
            repeatingSweep(
                listOf(white(0.35f), Color.Transparent, gray(0.2f, 0.4f), Color.Transparent),
                90f, c, (i.p.y - 0.5f) * 40f,
            ),
            BlendMode.Overlay,
        )
    }
}

/** No.11 토마토. 격자로 잘린 타일 — 유일하게 무늬가 기하학이다. */
private fun DrawScope.mosaic(i: FoilInput, t: FoilTune, shine: Float) {
    val o = shift(i.p, 1.4f, 1.4f)
    val cell = size.width * 0.09f
    // **밝기로 누르지 않는다.** 격자는 무늬끼리의 대비로 생기는 거라 밝기를 내리면
    // 색이 돌아오는 대신 무늬가 같이 뭉개진다 (저쪽 README).
    foilLayer(BlendMode.ColorDodge, shine * 0.8f, filterOf(t.brightness, 1.35f * t.contrast, 1.15f * t.saturate)) {
        drawRect(
            repeatingLinear(
                SUNPILLAR + SUNPILLAR.first(),
                o, o + Offset(size.width * 0.5f, size.height * 0.5f),
            ),
        )
        drawRect(
            repeatingLinear(
                listOf(white(0.5f), Color.Transparent, Color.Transparent, white(0.5f)),
                Offset.Zero, Offset(cell, 0f),
            ),
            blendMode = BlendMode.Overlay,
        )
        drawRect(
            repeatingLinear(
                listOf(white(0.5f), Color.Transparent, Color.Transparent, white(0.5f)),
                Offset.Zero, Offset(0f, cell),
            ),
            blendMode = BlendMode.Overlay,
        )
    }
}

/** No.12 상추. 세로로 긁힌 브러시드 결. 명도만 오르내리는 이방성. */
private fun DrawScope.metal(i: FoilInput, t: FoilTune, shine: Float) {
    val o = shift(i.p, 1.2f, 2.6f)
    // 여기도 밝기 대신 opacity 로 누른다 — hard-light 는 밝기를 내리면 탁해진다.
    foilLayer(BlendMode.Hardlight, shine * 0.7f, filterOf(t.brightness, 1.2f * t.contrast, 0.2f * t.saturate)) {
        blendBrushes(
            repeatingLinear(listOf(gray(0.85f), gray(0.35f), gray(0.95f), gray(0.45f)), Offset.Zero, Offset(3f, 0f)),
            Brush.linearGradient(
                colorStops = arrayOf(0f to Color.Black, 0.5f to Color.White, 1f to Color.Black),
                start = Offset(o.x - size.width * 0.5f, 0f),
                end = Offset(o.x + size.width * 0.5f, 0f),
            ),
            BlendMode.Overlay,
        )
    }
}
