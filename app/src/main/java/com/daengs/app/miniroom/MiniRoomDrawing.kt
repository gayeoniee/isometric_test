package com.daengs.app.miniroom

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntSize
import com.daengs.app.miniroom.art.ItemArt
import com.daengs.app.miniroom.sprite.drawSpriteFrame
import com.daengs.app.miniroom.sprite.frameIndexAt
import com.daengs.app.ui.theme.RoomPalette

/** 기본 단위 아트를 화면에 올리는 **유일한** 변환 지점. */
private inline fun DrawScope.inArtSpace(
    art: ItemArt,
    item: PlacedItem,
    g: RoomGeometry,
    dragOffset: Offset,
    lift: Float,
    block: DrawScope.() -> Unit,
) {
    val c = g.footprintCenter(item.col, item.row, art.box.footprint)
    withTransform({
        translate(
            c.x - art.box.anchor.x * g.scale + dragOffset.x,
            c.y - art.box.anchor.y * g.scale + dragOffset.y - lift,
        )
        scale(g.scale, g.scale, pivot = Offset.Zero)
        // 방향 1 = 좌우 반전. 기준점을 축으로 뒤집어야 발밑이 안 움직인다.
        if (item.facing == 1) scale(-1f, 1f, pivot = Offset(art.box.anchor.x, 0f))
    }) {
        block()
    }
}

/**
 * 아이템 하나 그리기.
 *
 * 도형이든 PNG 든 시트든 여기서 갈라진다. 바깥(정렬·터치·스냅)은 어느 쪽인지 모른다.
 */
fun DrawScope.drawItem(
    art: ItemArt,
    item: PlacedItem,
    g: RoomGeometry,
    timeMs: Long,
    dragOffset: Offset = Offset.Zero,
    lift: Float = 0f,
) {
    inArtSpace(art, item, g, dragOffset, lift) {
        when (art) {
            is ItemArt.Shapes -> art.draw(this, frameIndexAt(timeMs, 8, 6))
            is ItemArt.Bitmap -> drawImage(
                image = art.image,
                dstOffset = androidx.compose.ui.unit.IntOffset.Zero,
                dstSize = IntSize(art.box.size.width.toInt(), art.box.size.height.toInt()),
                filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
            )

            is ItemArt.Sheet -> {
                val frame = frameIndexAt(timeMs, art.frameCount, art.fps)
                if (art.sheet != null) {
                    drawSpriteFrame(art.sheet, frame, art.box.size)
                } else {
                    art.fallback(this, frame)
                }
            }
        }
    }
}

/** 방 밖으로 끌어냈을 때 뜨는 "치우기" 표시. */
fun DrawScope.drawRemoveHint(center: Offset, g: RoomGeometry) {
    val r = 17f * g.scale
    drawCircle(RoomPalette.GhostInvalid.copy(alpha = 0.9f), r, center)
    val a = r * 0.42f
    drawLine(Color.White, center + Offset(-a, -a), center + Offset(a, a), strokeWidth = 3f * g.scale)
    drawLine(Color.White, center + Offset(a, -a), center + Offset(-a, a), strokeWidth = 3f * g.scale)
}

/** 들어올린 아이템 아래 바닥에 지는 그림자. */
fun DrawScope.drawLiftShadow(g: RoomGeometry, col: Int, row: Int, footprint: IntSize) {
    val c = g.footprintCenter(col, row, footprint)
    val w = g.tw * 0.62f
    drawOval(
        RoomPalette.Shadow.copy(alpha = 0.28f),
        Offset(c.x - w / 2f, c.y - w / 4f),
        Size(w, w / 2f),
    )
}

/**
 * 놓일 칸 표시.
 *
 * 초록이면 그대로 놓이고, 빨강이면 원래 자리로 돌아간다.
 * 격자 밖으로 끌면 이미 가장자리로 붙여둔 좌표가 표시되므로,
 * 손을 떼기 전에 결과가 항상 눈에 보인다.
 */
fun DrawScope.drawCellGhost(
    g: RoomGeometry,
    col: Int,
    row: Int,
    footprint: IntSize,
    valid: Boolean,
) {
    val fw = footprint.width.toFloat()
    val fh = footprint.height.toFloat()
    val p = Path().apply {
        val a = g.toScreenF(col.toFloat(), row.toFloat())
        val b = g.toScreenF(col + fw, row.toFloat())
        val c = g.toScreenF(col + fw, row + fh)
        val d = g.toScreenF(col.toFloat(), row + fh)
        moveTo(a.x, a.y)
        lineTo(b.x, b.y)
        lineTo(c.x, c.y)
        lineTo(d.x, d.y)
        close()
    }
    val color = if (valid) RoomPalette.GhostValid else RoomPalette.GhostInvalid
    drawPath(p, color.copy(alpha = 0.32f))
    drawPath(p, color.copy(alpha = 0.85f), style = Stroke(width = 2f * g.scale))
}
