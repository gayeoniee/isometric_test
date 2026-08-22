package com.daengs.app.miniroom.art

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.daengs.app.miniroom.RoomSpec
import com.daengs.app.ui.theme.RoomPalette

// ---------------------------------------------------------------------------
// 가구·소품 아트.
//
// 전부 **기본 단위**로만 그린다 (타일 = 64x32).
// 화면 크기·scale·RoomGeometry 를 여기서 참조하지 않는다 — 바깥의 transform 이
// 이미 잡아놨다. 그래서 이 파일은 PNG 로 교체될 때 통째로 버려도 된다.
//
// 좌표계: ArtBox.size 상자의 왼쪽 위가 (0,0).
//
// **접지 규칙 (중요)**: ArtBox.anchor 는 물건이 바닥에 닿는 점이고,
// [floorShadow] 는 그 점을 **중심으로** 그린다.
// - 바닥에 눕는 물건(방석·밥그릇·뼈다귀)은 밑면 타원의 **중심**이 anchor 다.
//   맨 아래 모서리를 anchor 로 잡으면 그림자가 물건 밑으로 삐져나와 공중에 뜬 것처럼 보인다.
// - 서 있는 물건(화분·공)은 바닥에 닿는 **맨 아랫점**이 anchor 다.
// ---------------------------------------------------------------------------

/** 색을 밝게/어둡게. 그라데이션용 색을 팔레트에 계속 추가하지 않으려고 둔다. */
private fun lighten(c: Color, t: Float) = lerp(c, Color.White, t)
private fun darken(c: Color, t: Float) = lerp(c, Color(0xFF6B4A3E), t)

/** 바닥 그림자. (cx,cy) 가 타원의 중심이자 접지점. 납작한 정도는 타일 비율을 따른다. */
private fun DrawScope.floorShadow(cx: Float, cy: Float, w: Float) {
    val h = w / RoomSpec.TILE_RATIO
    drawOval(RoomPalette.Shadow, Offset(cx - w / 2f, cy - h / 2f), Size(w, h))
}

/** 러그 — flat 아트. 칸을 점유하지 않아서 그 위에 강아지가 앉는다. */
fun DrawScope.drawRug() {
    drawOval(RoomPalette.RugRim, Offset(0f, 0f), Size(112f, 60f))
    drawOval(RoomPalette.RugFill, Offset(4f, 3f), Size(104f, 54f))
    drawOval(
        RoomPalette.RugRim.copy(alpha = 0.7f),
        Offset(14f, 10f),
        Size(84f, 40f),
        style = Stroke(width = 1.6f),
    )
}

/** 공 — 서 있는(구르는) 물건. 접지점은 맨 아랫점. anchor = (12, 23) */
fun DrawScope.drawBall() {
    floorShadow(12f, 23f, 23f)
    // 단색 원 대신 방사형 그라데이션 -> 도형 하나로도 구처럼 보인다.
    // 빛은 왼쪽 위에서 온다고 가정한다 (방 전체가 같은 방향이어야 통일감이 생긴다).
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                lighten(RoomPalette.BallFill, 0.45f),
                RoomPalette.BallFill,
                darken(RoomPalette.BallFill, 0.18f),
            ),
            center = Offset(8.2f, 8.0f),
            radius = 15f,
        ),
        radius = 11f,
        center = Offset(12f, 12f),
    )
    // 바닥에서 올라온 반사광 — 아래쪽 가장자리를 살짝 밝게
    drawArc(
        color = lighten(RoomPalette.BallFill, 0.25f).copy(alpha = 0.55f),
        startAngle = 35f, sweepAngle = 110f, useCenter = false,
        topLeft = Offset(2.2f, 2.2f), size = Size(19.6f, 19.6f),
        style = Stroke(width = 1.3f),
    )
    drawCircle(Color.White.copy(alpha = 0.8f), 2.5f, Offset(8.0f, 7.6f))
}

/**
 * 밥그릇 — 도형 구성은 처음 그대로 두고, 접지점만 고쳤다.
 * 밑면 타원(38x18)의 중심이 바닥에 닿는 점이다. anchor = (20, 17)
 */
fun DrawScope.drawBowl() = drawBowlLike(RoomPalette.BowlFill, RoomPalette.RugFill)

/**
 * 그릇 공통 형태. 입체감은 세 곳에서 나온다.
 *  1) 몸통 세로 그라데이션 (위가 밝고 아래가 어둡다)
 *  2) 안쪽 그늘 — 뒤가 어둡고 앞이 밝아야 파여 보인다
 *  3) 테두리 왼쪽 위 하이라이트 한 줄
 */
private fun DrawScope.drawBowlLike(body: Color, inner: Color) {
    floorShadow(20f, 17f, 42f)
    drawOval(
        brush = Brush.verticalGradient(
            listOf(lighten(body, 0.20f), body, darken(body, 0.22f)),
            startY = 8f, endY = 26f,
        ),
        topLeft = Offset(1f, 8f), size = Size(38f, 18f),
    )
    drawOval(lighten(body, 0.12f), Offset(4f, 4f), Size(32f, 14f))
    drawOval(
        brush = Brush.verticalGradient(
            listOf(darken(inner, 0.30f), inner),
            startY = 5f, endY = 16f,
        ),
        topLeft = Offset(8f, 6f), size = Size(24f, 10f),
    )
    drawArc(
        color = lighten(body, 0.55f).copy(alpha = 0.8f),
        startAngle = 168f, sweepAngle = 115f, useCenter = false,
        topLeft = Offset(4f, 4f), size = Size(32f, 14f),
        style = Stroke(width = 1.3f),
    )
}

/**
 * 강아지 침대 — 테두리가 올라오고 가운데가 파인 형태. anchor = (39, 30)
 * 밑면 타원의 중심이 바닥에 닿는 점이다.
 */
fun DrawScope.drawBed() {
    floorShadow(39f, 30f, 82f)
    // 바깥 테두리 (쿠션 링)
    drawOval(
        brush = Brush.verticalGradient(
            listOf(lighten(RoomPalette.BedFill, 0.22f), RoomPalette.BedFill, darken(RoomPalette.BedFill, 0.18f)),
            startY = 6f, endY = 46f,
        ),
        topLeft = Offset(3f, 8f), size = Size(72f, 38f),
    )
    // 안쪽 파인 부분 — 뒤가 어둡고 앞이 밝아야 파여 보인다
    drawOval(
        brush = Brush.verticalGradient(
            listOf(darken(RoomPalette.RugFill, 0.18f), RoomPalette.RugFill),
            startY = 13f, endY = 36f,
        ),
        topLeft = Offset(13f, 14f), size = Size(52f, 24f),
    )
    // 테두리 하이라이트
    drawArc(
        color = Color.White.copy(alpha = 0.55f),
        startAngle = 170f, sweepAngle = 110f, useCenter = false,
        topLeft = Offset(5f, 10f), size = Size(68f, 34f),
        style = Stroke(width = 1.6f),
    )
}

/** 화분 — 서 있는 물건. 접지점은 화분 바닥. anchor = (22, 88) */
fun DrawScope.drawPlant() {
    floorShadow(22f, 88f, 38f)
    // 화분 — 위가 넓은 사다리꼴
    drawPath(
        Path().apply {
            moveTo(7f, 62f)
            lineTo(37f, 62f)
            lineTo(32f, 88f)
            lineTo(12f, 88f)
            close()
        },
        RoomPalette.PlantPot,
    )
    drawOval(RoomPalette.PlantPot.copy(alpha = 0.6f), Offset(6f, 57f), Size(32f, 11f))
    listOf(
        Triple(22f, 22f, 15f),
        Triple(11f, 38f, 12f),
        Triple(33f, 38f, 12f),
        Triple(22f, 47f, 11f),
    ).forEach { (x, y, r) -> drawCircle(RoomPalette.PlantLeaf, r, Offset(x, y)) }
    drawCircle(RoomPalette.PlantLeaf.copy(alpha = 0.55f), 7f, Offset(18f, 28f))
}

/** 뼈다귀 장난감 — 바닥에 눕는다. 접지점은 자기 중심. anchor = (18, 13) */
fun DrawScope.drawBone() {
    floorShadow(18f, 13f, 38f)
    val g = Brush.verticalGradient(
        listOf(
            lighten(RoomPalette.BoneFill, 0.28f),
            RoomPalette.BoneFill,
            darken(RoomPalette.BoneFill, 0.14f),
        ),
        startY = 4f, endY = 21f,
    )
    drawRoundRect(g, Offset(7f, 9f), Size(22f, 7f), CornerRadius(3.5f, 3.5f))
    listOf(6f to 9f, 6f to 16f, 30f to 9f, 30f to 16f).forEach { (x, y) ->
        drawCircle(g, 4.4f, Offset(x, y))
    }
    listOf(6f to 9f, 30f to 9f).forEach { (x, y) ->
        drawArc(
            color = Color.White.copy(alpha = 0.7f),
            startAngle = 186f, sweepAngle = 92f, useCenter = false,
            topLeft = Offset(x - 4.4f, y - 4.4f), size = Size(8.8f, 8.8f),
            style = Stroke(width = 1.0f),
        )
    }
    drawCircle(darken(RoomPalette.BoneFill, 0.12f), 1.6f, Offset(18f, 12.5f))
}

// ---------------------------------------------------------------------------
// 추가 아이템
// ---------------------------------------------------------------------------

private fun poly(points: List<Offset>): Path = Path().apply {
    moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    close()
}

/**
 * 아이소메트릭 상자. (cx, cyBase) 는 **밑면 마름모의 중심** = 접지점.
 * 상자 모양 아이템은 전부 이걸로 만든다.
 */
internal fun DrawScope.isoBox(
    cx: Float,
    cyBase: Float,
    w: Float,
    h: Float,
    top: Color,
    left: Color,
    right: Color,
) {
    val hw = w / 2f
    val qh = w / 4f
    drawPath(
        poly(
            listOf(
                Offset(cx - hw, cyBase - h),
                Offset(cx, cyBase - h + qh),
                Offset(cx, cyBase + qh),
                Offset(cx - hw, cyBase),
            )
        ),
        left,
    )
    drawPath(
        poly(
            listOf(
                Offset(cx + hw, cyBase - h),
                Offset(cx, cyBase - h + qh),
                Offset(cx, cyBase + qh),
                Offset(cx + hw, cyBase),
            )
        ),
        right,
    )
    drawPath(
        poly(
            listOf(
                Offset(cx, cyBase - h - qh),
                Offset(cx + hw, cyBase - h),
                Offset(cx, cyBase - h + qh),
                Offset(cx - hw, cyBase - h),
            )
        ),
        top,
    )
}

/** 강아지집 — 상자 + 모임지붕 + 입구. anchor = (36, 62) */
fun DrawScope.drawHouse() {
    floorShadow(36f, 62f, 60f)
    isoBox(36f, 62f, 52f, 24f, RoomPalette.HouseWall, RoomPalette.HouseWallSide, RoomPalette.HouseWall)

    // 모임지붕 — 보이는 두 면만
    val apex = Offset(36f, 9f)
    val w = Offset(10f, 38f)
    val s = Offset(36f, 51f)
    val e = Offset(62f, 38f)
    drawPath(poly(listOf(w, s, apex)), RoomPalette.HouseRoofSide)
    drawPath(poly(listOf(e, s, apex)), RoomPalette.HouseRoof)

    // 입구 — 오른쪽 앞면에 아치
    drawOval(RoomPalette.HouseDoor, Offset(40f, 48f), Size(16f, 18f))
    drawRect(RoomPalette.HouseDoor, Offset(40f, 56f), Size(16f, 10f))
}

/** 장난감 상자 — anchor = (26, 40) */
fun DrawScope.drawToybox() {
    floorShadow(26f, 40f, 52f)
    isoBox(26f, 40f, 46f, 22f, RoomPalette.BoxTop, RoomPalette.BoxLeft, RoomPalette.BoxRight)
    // 뚜껑 테두리
    drawPath(
        poly(
            listOf(
                Offset(26f, 6.5f),
                Offset(49f, 18f),
                Offset(26f, 29.5f),
                Offset(3f, 18f),
            )
        ),
        RoomPalette.BoneFill.copy(alpha = 0.5f),
    )
    drawPawStamp(Offset(15f, 32f), 5f, RoomPalette.BoneFill.copy(alpha = 0.8f))
}

/** 물그릇 — 밥그릇과 형태는 같고 색만 다르다. anchor = (20, 17) */
fun DrawScope.drawWaterBowl() {
    drawBowlLike(RoomPalette.WaterRim, RoomPalette.WaterFill)
    // 수면 반짝임
    drawOval(Color.White.copy(alpha = 0.55f), Offset(13f, 8.5f), Size(7f, 2.6f))
}

/** 쿠션 — 낮은 사각 방석. anchor = (24, 30) */
fun DrawScope.drawCushion() {
    floorShadow(24f, 30f, 50f)
    isoBox(24f, 30f, 46f, 11f, RoomPalette.CushionTop, RoomPalette.CushionSide, RoomPalette.CushionSide)
    // 가운데 눌린 자국
    drawOval(RoomPalette.CushionSide.copy(alpha = 0.5f), Offset(15f, 14f), Size(18f, 9f))
}

/** 꽃병 — anchor = (18, 40) */
fun DrawScope.drawVase() {
    floorShadow(18f, 40f, 30f)
    drawPath(
        poly(
            listOf(
                Offset(10f, 20f),
                Offset(26f, 20f),
                Offset(24f, 40f),
                Offset(12f, 40f),
            )
        ),
        RoomPalette.VaseFill,
    )
    drawOval(RoomPalette.VaseSide, Offset(11f, 36f), Size(14f, 7f))
    drawOval(RoomPalette.VaseSide.copy(alpha = 0.7f), Offset(9f, 16f), Size(18f, 8f))
    // 꽃 세 송이
    drawLine(RoomPalette.PlantLeaf, Offset(18f, 20f), Offset(11f, 9f), strokeWidth = 1.6f)
    drawLine(RoomPalette.PlantLeaf, Offset(18f, 20f), Offset(18f, 5f), strokeWidth = 1.6f)
    drawLine(RoomPalette.PlantLeaf, Offset(18f, 20f), Offset(25f, 9f), strokeWidth = 1.6f)
    drawCircle(RoomPalette.FlowerA, 5f, Offset(11f, 8f))
    drawCircle(RoomPalette.FlowerB, 5.5f, Offset(18f, 4f))
    drawCircle(RoomPalette.FlowerA, 4.5f, Offset(25f, 8f))
}

/** 담요 — 바닥에 깔린다(flat). anchor = (36, 20) */
fun DrawScope.drawBlanket() {
    drawPath(
        poly(
            listOf(
                Offset(36f, 2f),
                Offset(70f, 20f),
                Offset(36f, 38f),
                Offset(2f, 20f),
            )
        ),
        RoomPalette.BlanketRim,
    )
    drawPath(
        poly(
            listOf(
                Offset(36f, 6f),
                Offset(63f, 20f),
                Offset(36f, 34f),
                Offset(9f, 20f),
            )
        ),
        RoomPalette.BlanketFill,
    )
    drawLine(RoomPalette.BlanketRim, Offset(20f, 13f), Offset(52f, 27f), strokeWidth = 1.6f)
    drawLine(RoomPalette.BlanketRim, Offset(20f, 27f), Offset(52f, 13f), strokeWidth = 1.6f)
}

/** 책상 — 상판 + 다리 4개. anchor = (36, 46) */
fun DrawScope.drawDesk() {
    floorShadow(36f, 46f, 68f)
    val legTop = 26f
    // 뒤쪽 다리 먼저
    listOf(Offset(36f, 26f), Offset(8f, 40f)).forEach { p ->
        drawRect(darken(RoomPalette.BoxLeft, 0.10f), Offset(p.x - 2f, p.y), Size(4f, 20f))
    }
    // 상판 (아이소메트릭 판)
    isoBox(36f, 34f, 62f, 5f, RoomPalette.BoxTop, RoomPalette.BoxLeft, RoomPalette.BoxRight)
    // 앞쪽 다리
    listOf(Offset(64f, 40f), Offset(36f, 54f)).forEach { p ->
        drawRect(RoomPalette.BoxLeft, Offset(p.x - 2f, p.y - 6f), Size(4f, 18f))
    }
    // 상판 앞 모서리 하이라이트
    drawLine(
        Color.White.copy(alpha = 0.5f),
        Offset(5f, 34f), Offset(36f, 49.5f),
        strokeWidth = 1.2f,
    )
    if (legTop > 0f) Unit
}
