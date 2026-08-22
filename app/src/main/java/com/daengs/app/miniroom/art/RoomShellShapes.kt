package com.daengs.app.miniroom.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import com.daengs.app.miniroom.RoomGeometry
import com.daengs.app.miniroom.RoomSpec
import com.daengs.app.ui.theme.RoomPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------
// 벽 평면 좌표계
//   u = 벽을 따라간 거리 (격자 단위 0..6, 0 이 안쪽 모서리)
//   h = 바닥에서 올라간 높이 (px, 0..wallPx)
// 벽에 붙는 것(창문·문)은 전부 (u,h) 로 그리고 이 함수로 화면에 매핑한다.
// ---------------------------------------------------------------------------

fun RoomGeometry.leftWallPoint(u: Float, h: Float) =
    Offset(origin.x - u * tw / 2f, origin.y + u * th / 2f - h)

fun RoomGeometry.rightWallPoint(u: Float, h: Float) =
    Offset(origin.x + u * tw / 2f, origin.y + u * th / 2f - h)

/** 앞쪽 오른쪽 모서리(col = 6 선) 위의 점. 울타리가 여기 선다. */
fun RoomGeometry.frontRailPoint(v: Float, h: Float) =
    Offset(origin.x + (6f - v) * tw / 2f, origin.y + (6f + v) * th / 2f - h)

private fun poly(points: List<Offset>): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    close()
}

/**
 * 아치형 개구부의 (u,h) 윤곽선. 아래는 직선, 위는 반타원.
 * 벽 평면에서 만든 뒤 매핑하므로 아이소메트릭으로 자연스럽게 기울어진다.
 */
private fun archOutline(
    u0: Float,
    u1: Float,
    h0: Float,
    h1: Float,
    archH: Float,
    steps: Int = 14,
): List<Pair<Float, Float>> {
    val mid = (u0 + u1) / 2f
    val ru = (u1 - u0) / 2f
    val shoulder = h1 - archH
    val pts = mutableListOf(u0 to h0, u1 to h0, u1 to shoulder)
    for (i in 0..steps) {
        val t = PI * i / steps
        pts += (mid + ru * cos(t).toFloat()) to (shoulder + archH * sin(t).toFloat())
    }
    pts += u0 to shoulder
    return pts
}

// ---------------------------------------------------------------------------

fun DrawScope.drawRoomShell(g: RoomGeometry) {
    drawWalls(g)
    drawWindow(g)
    drawDoor(g)
    drawFloor(g)
}

private fun DrawScope.drawWalls(g: RoomGeometry) {
    val corner = g.origin
    val n = RoomSpec.GRID.toFloat()
    val leftBase = g.leftWallPoint(n, 0f)
    val rightBase = g.rightWallPoint(n, 0f)
    val w = g.wallPx

    // 왼쪽 벽 (문 + 창문이 붙는 면)
    drawPath(
        poly(
            listOf(
                Offset(corner.x, corner.y - w),
                Offset(leftBase.x, leftBase.y - w),
                leftBase,
                corner,
            )
        ),
        RoomPalette.WallLeft,
    )
    // 오른쪽 벽 — 시안대로 민무늬. 살짝 어둡게 해야 두 면이 갈라져 보인다.
    drawPath(
        poly(
            listOf(
                Offset(corner.x, corner.y - w),
                Offset(rightBase.x, rightBase.y - w),
                rightBase,
                corner,
            )
        ),
        RoomPalette.WallRight,
    )

    // 안쪽 모서리 이음새 — 이게 없으면 두 벽이 한 면으로 보인다
    drawLine(
        RoomPalette.WallShadow,
        Offset(corner.x, corner.y - w),
        corner,
        strokeWidth = 1.5f * g.scale,
    )

    // 벽 위 몰딩 (시안의 흰 테두리)
    val trim = 5f * g.scale
    drawPath(
        poly(
            listOf(
                Offset(corner.x, corner.y - w),
                Offset(leftBase.x, leftBase.y - w),
                Offset(leftBase.x, leftBase.y - w + trim),
                Offset(corner.x, corner.y - w + trim),
            )
        ),
        RoomPalette.WallTrim,
    )
    drawPath(
        poly(
            listOf(
                Offset(corner.x, corner.y - w),
                Offset(rightBase.x, rightBase.y - w),
                Offset(rightBase.x, rightBase.y - w + trim),
                Offset(corner.x, corner.y - w + trim),
            )
        ),
        RoomPalette.WallTrim,
    )

    // 걸레받이
    val base = 4f * g.scale
    drawPath(
        poly(
            listOf(
                corner,
                leftBase,
                Offset(leftBase.x, leftBase.y - base),
                Offset(corner.x, corner.y - base),
            )
        ),
        RoomPalette.WallTrim,
    )
    drawPath(
        poly(
            listOf(
                corner,
                rightBase,
                Offset(rightBase.x, rightBase.y - base),
                Offset(corner.x, corner.y - base),
            )
        ),
        RoomPalette.WallTrim,
    )
}

private fun DrawScope.drawWindow(g: RoomGeometry) {
    val u0 = 0.7f
    val u1 = 2.5f
    // 벽 높이의 30% ~ 72% — 가운데쯤에 오게. 예전엔 0.95 라 천장에 붙어 있었다.
    val h0 = g.wallPx * 0.30f
    val h1 = g.wallPx * 0.72f
    val archH = g.wallPx * 0.20f
    val frame = 0.18f

    val outer = poly(
        archOutline(u0 - frame, u1 + frame, h0 - 4f * g.scale, h1 + 4f * g.scale, archH)
            .map { g.leftWallPoint(it.first, it.second) }
    )
    val inner = poly(
        archOutline(u0, u1, h0, h1, archH).map { g.leftWallPoint(it.first, it.second) }
    )

    drawPath(outer, RoomPalette.WallTrim)

    // 창밖 — 하늘 그라데이션 + 구름 + 벚나무
    clipPath(inner) {
        val topP = g.leftWallPoint(u1, h1)
        val botP = g.leftWallPoint(u0, h0)
        drawRect(
            brush = Brush.verticalGradient(
                listOf(RoomPalette.SkyTop, RoomPalette.SkyBottom),
                startY = topP.y - archH,
                endY = botP.y,
            ),
            topLeft = Offset(topP.x - g.tw * 2f, topP.y - archH * 2f),
            size = Size(botP.x - topP.x + g.tw * 4f, botP.y - topP.y + archH * 4f),
        )

        val c1 = g.leftWallPoint(2.05f, h1 - archH * 0.45f)
        drawOval(
            RoomPalette.Cloud,
            Offset(c1.x - 11f * g.scale, c1.y - 5f * g.scale),
            Size(24f * g.scale, 10f * g.scale),
        )
        val c2 = g.leftWallPoint(2.35f, h1 - archH * 0.78f)
        drawOval(
            RoomPalette.Cloud,
            Offset(c2.x - 7f * g.scale, c2.y - 4f * g.scale),
            Size(16f * g.scale, 8f * g.scale),
        )

        // 벚나무 — 가지 + 꽃뭉치
        val trunk = g.leftWallPoint(u0 + 0.12f, h0)
        val branch = g.leftWallPoint(u0 + 0.8f, h0 + archH * 0.8f)
        drawLine(RoomPalette.Branch, trunk, branch, strokeWidth = 2.4f * g.scale)
        val blossoms = listOf(
            Triple(0.5f, 0.5f, 9f),
            Triple(0.95f, 0.82f, 11f),
            Triple(1.4f, 0.52f, 8f),
            Triple(0.72f, 1.1f, 7f),
            Triple(1.18f, 0.3f, 6f),
        )
        blossoms.forEach { (du, dh, r) ->
            val p = g.leftWallPoint(u0 + du, h0 + archH * dh)
            drawCircle(RoomPalette.Blossom, r * g.scale, p)
            drawCircle(
                RoomPalette.BlossomDeep,
                r * 0.42f * g.scale,
                p + Offset(r * 0.24f * g.scale, r * 0.2f * g.scale),
            )
        }
    }

    // 창틀 격자 (십자)
    drawLine(
        RoomPalette.WallTrim,
        g.leftWallPoint((u0 + u1) / 2f, h1),
        g.leftWallPoint((u0 + u1) / 2f, h0),
        strokeWidth = 2.8f * g.scale,
    )
    drawLine(
        RoomPalette.WallTrim,
        g.leftWallPoint(u0, h0 + (h1 - h0) * 0.42f),
        g.leftWallPoint(u1, h0 + (h1 - h0) * 0.42f),
        strokeWidth = 2.8f * g.scale,
    )
    drawPath(inner, RoomPalette.WallTrim, style = Stroke(width = 3.2f * g.scale))

    // 창턱
    val sL = g.leftWallPoint(u0 - frame * 1.7f, h0 - 4f * g.scale)
    val sR = g.leftWallPoint(u1 + frame * 1.7f, h0 - 4f * g.scale)
    drawPath(
        poly(
            listOf(
                sL,
                sR,
                sR + Offset(0f, 4.5f * g.scale),
                sL + Offset(0f, 4.5f * g.scale),
            )
        ),
        RoomPalette.WallTrim,
    )
}

private fun DrawScope.drawDoor(g: RoomGeometry) {
    val u0 = 3.5f
    val u1 = 5.0f
    val h1 = g.wallPx * 0.66f
    val archH = g.wallPx * 0.22f
    val frame = 0.2f

    val outer = poly(
        archOutline(u0 - frame, u1 + frame, 0f, h1 + 5f * g.scale, archH)
            .map { g.leftWallPoint(it.first, it.second) }
    )
    val inner = poly(
        archOutline(u0, u1, 0f, h1, archH).map { g.leftWallPoint(it.first, it.second) }
    )

    drawPath(outer, RoomPalette.DoorTrim)
    drawPath(inner, RoomPalette.DoorFill)

    // 문에 새긴 발바닥
    drawPawStamp(
        g.leftWallPoint((u0 + u1) / 2f, h1 * 0.60f),
        8f * g.scale,
        RoomPalette.DoorTrim,
    )
    // 손잡이
    drawCircle(RoomPalette.DoorKnob, 3.4f * g.scale, g.leftWallPoint(u1 - 0.24f, h1 * 0.32f))
}

/** 발바닥 도장 — 문·울타리·아이콘에서 재사용. */
fun DrawScope.drawPawStamp(center: Offset, r: Float, color: Color) {
    drawOval(
        color,
        Offset(center.x - r * 0.62f, center.y - r * 0.12f),
        Size(r * 1.24f, r * 1.02f),
    )
    val toes = listOf(-0.74f to -0.7f, -0.26f to -1.02f, 0.26f to -1.02f, 0.74f to -0.7f)
    toes.forEach { (dx, dy) ->
        drawOval(
            color,
            Offset(center.x + dx * r - r * 0.18f, center.y + dy * r - r * 0.2f),
            Size(r * 0.37f, r * 0.45f),
        )
    }
}

private fun DrawScope.drawFloor(g: RoomGeometry) {
    val n = RoomSpec.GRID.toFloat()
    val top = g.toScreenF(0f, 0f)
    val right = g.toScreenF(n, 0f)
    val bottom = g.toScreenF(n, n)
    val left = g.toScreenF(0f, n)
    val floor = poly(listOf(top, right, bottom, left))

    drawPath(floor, RoomPalette.FloorLight)

    clipPath(floor) {
        // 나뭇결 — row 가 일정한 선을 따라 널을 깐다
        for (r in 0..RoomSpec.GRID) {
            drawLine(
                RoomPalette.FloorPlank.copy(alpha = 0.34f),
                g.toScreenF(0f, r.toFloat()),
                g.toScreenF(n, r.toFloat()),
                strokeWidth = 1.2f * g.scale,
            )
        }
        // 앞쪽으로 갈수록 살짝 어둡게 — 평평해 보이지 않게
        drawPath(
            floor,
            Brush.verticalGradient(
                listOf(
                    RoomPalette.FloorDark.copy(alpha = 0f),
                    RoomPalette.FloorDark.copy(alpha = 0.45f),
                ),
                startY = top.y,
                endY = bottom.y,
            ),
        )
        // 격자를 아주 옅게 — 배치 데모라 한 칸이 어디까진지 보이는 편이 낫다
        for (c in 0..RoomSpec.GRID) {
            drawLine(
                RoomPalette.FloorPlank.copy(alpha = 0.15f),
                g.toScreenF(c.toFloat(), 0f),
                g.toScreenF(c.toFloat(), n),
                strokeWidth = 1f * g.scale,
            )
        }
    }

    // 바닥 앞쪽 테두리 (시안의 흰 띠)
    val lip = 7f * g.scale
    drawPath(
        poly(
            listOf(
                left,
                bottom,
                right,
                right + Offset(0f, lip),
                bottom + Offset(0f, lip),
                left + Offset(0f, lip),
            )
        ),
        RoomPalette.FloorEdge,
    )
    drawPath(floor, RoomPalette.FloorEdge.copy(alpha = 0.9f), style = Stroke(width = 2f * g.scale))
}

/**
 * 울타리. 바닥 앞쪽 모서리에 서므로 아이템보다 **뒤에** 그리면 안 된다 —
 * 방 껍데기가 아니라 아이템을 다 그린 뒤에 호출한다.
 */
fun DrawScope.drawFence(g: RoomGeometry) {
    val postH = 26f * g.scale
    val from = 2.2f
    val to = 5.4f
    val posts = 4

    listOf(0.45f, 0.78f).forEach { frac ->
        drawLine(
            RoomPalette.FenceFill,
            g.frontRailPoint(from, postH * frac),
            g.frontRailPoint(to, postH * frac),
            strokeWidth = 4.5f * g.scale,
        )
    }
    for (i in 0 until posts) {
        val v = from + (to - from) * i / (posts - 1f)
        drawLine(
            RoomPalette.FenceFill,
            g.frontRailPoint(v, 0f),
            g.frontRailPoint(v, postH),
            strokeWidth = 6.5f * g.scale,
        )
        drawCircle(RoomPalette.FenceTrim, 3.4f * g.scale, g.frontRailPoint(v, postH))
    }
    drawPawStamp(
        g.frontRailPoint((from + to) / 2f, postH * 0.6f),
        4.4f * g.scale,
        RoomPalette.FenceTrim,
    )
}
