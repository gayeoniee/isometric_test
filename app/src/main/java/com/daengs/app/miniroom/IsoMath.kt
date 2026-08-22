package com.daengs.app.miniroom

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min

// ---------------------------------------------------------------------------
// CONTEXT.md 4번 섹션의 좌표 변환 수식 — 글자 그대로 옮긴 것. 고치지 말 것.
// ---------------------------------------------------------------------------

/** 격자 → 화면 */
fun toScreen(col: Int, row: Int, tw: Float, th: Float, origin: Offset) = Offset(
    x = origin.x + (col - row) * tw / 2f,
    y = origin.y + (col + row) * th / 2f,
)

/** 화면 → 격자 (터치 판정) */
fun toGrid(pos: Offset, tw: Float, th: Float, origin: Offset): Pair<Int, Int> {
    val dx = pos.x - origin.x
    val dy = pos.y - origin.y
    val col = ((dx / (tw / 2f) + dy / (th / 2f)) / 2f).toInt()
    val row = ((dy / (th / 2f) - dx / (tw / 2f)) / 2f).toInt()
    return col to row
}

// ---------------------------------------------------------------------------
// 위 수식을 안전하게 쓰기 위한 보조 함수들
// ---------------------------------------------------------------------------

/**
 * toGrid 의 잘림(truncation) 이전 실수값.
 *
 * 왜 필요한가: [toGrid] 의 `.toInt()` 는 floor 가 아니라 **0 방향 잘림**이다.
 * col 이 -0.4 인 지점(격자 왼쪽 바깥)도 0 으로 잘려서, 격자 밖으로 끌어낸
 * 아이템이 조용히 (0,0) 에 붙어버린다. 범위 검사는 반드시 이 실수값으로 한다.
 */
fun toGridF(pos: Offset, tw: Float, th: Float, origin: Offset): Pair<Float, Float> {
    val dx = pos.x - origin.x
    val dy = pos.y - origin.y
    val col = (dx / (tw / 2f) + dy / (th / 2f)) / 2f
    val row = (dy / (th / 2f) - dx / (tw / 2f)) / 2f
    return col to row
}

/** 격자 → 화면, 연속 좌표판. 아이템 중심·드래그 프리뷰에 쓴다. */
fun toScreenF(col: Float, row: Float, tw: Float, th: Float, origin: Offset) = Offset(
    x = origin.x + (col - row) * tw / 2f,
    y = origin.y + (col + row) * th / 2f,
)

/** 방 규격. 아트는 전부 이 기본 단위(타일 64x32)로 그린다. */
object RoomSpec {
    const val GRID = 6

    /** 아트 저작 기준 타일 크기. 화면 크기와 무관한 "기본 단위". */
    const val BASE_TILE_W = 64f
    const val BASE_TILE_H = 32f

    /**
     * 벽 높이 = 5 타일높이, 벽 위 여백 = 0.5 타일높이.
     *
     * 세로 공간이 고정(스크롤 없음)이라 벽을 높이면 바닥이 좁아진다.
     * 그래서 여백을 1 → 0.5 로 줄여 상쇄했다: 벽은 25% 높아지고
     * 방 전체 높이는 11 → 11.5 라 바닥은 4% 만 줄어든다.
     */
    const val WALL_TILES = 5f
    const val HEADROOM_TILES = 0.5f

    /** 방 전체 높이를 th 단위로. 여백 + 벽 + 격자 = 11.5 */
    const val ROOM_TILES_H = HEADROOM_TILES + WALL_TILES + GRID

    /**
     * 방의 가로:세로 비율.
     * 가로 = 6*tw = 12*th, 세로 = 11*th  →  12:11
     */
    const val ASPECT = (GRID * 2f) / ROOM_TILES_H
}

/**
 * 화면 폭 하나에서 파생된 방의 기하 정보 전부.
 *
 * tw = W/6, th = tw/2 로 **구성상** 2:1 비율이 깨질 수 없다.
 * [scale] 은 기본 단위 1 이 화면 px 로 몇인지를 나타내는 유일한 환산 계수다.
 */
@Immutable
data class RoomGeometry(
    val tw: Float,
    val th: Float,
    val wallPx: Float,
    val origin: Offset,
    val scale: Float,
) {
    fun toScreen(col: Int, row: Int): Offset = toScreen(col, row, tw, th, origin)

    fun toScreenF(col: Float, row: Float): Offset = toScreenF(col, row, tw, th, origin)

    fun toGrid(pos: Offset): Pair<Int, Int> = toGrid(pos, tw, th, origin)

    fun toGridF(pos: Offset): Pair<Float, Float> = toGridF(pos, tw, th, origin)

    /** 발자국(footprint)이 차지하는 바닥 영역의 중심. 1x1 이면 타일 중심이다. */
    fun footprintCenter(col: Int, row: Int, footprint: IntSize): Offset =
        toScreenF(col + footprint.width / 2f, row + footprint.height / 2f)

    /** 격자 밖으로 나갔는지 — 반드시 실수값으로 검사한다. */
    fun isInside(colF: Float, rowF: Float): Boolean =
        colF >= 0f && rowF >= 0f && colF < RoomSpec.GRID && rowF < RoomSpec.GRID

    companion object {
        /**
         * 주어진 상자 안에 방을 통째로 넣는다.
         *
         * 가로·세로 중 **더 빡빡한 쪽**으로 타일 크기를 정하므로, 화면 높이가
         * 모자라면 방이 알아서 작아진다 (스크롤 없이 한 화면에 담기 위해 필요).
         * tw:th = 2:1 은 여기서도 구성상 깨지지 않는다.
         */
        fun of(widthPx: Float, heightPx: Float): RoomGeometry {
            val tw = min(widthPx / RoomSpec.GRID, heightPx / (RoomSpec.ROOM_TILES_H / 2f))
            val th = tw / 2f
            val wall = RoomSpec.WALL_TILES * th
            val head = RoomSpec.HEADROOM_TILES * th
            // 남는 세로 공간은 위아래로 나눠 방을 가운데 둔다
            val top = ((heightPx - RoomSpec.ROOM_TILES_H * th) / 2f).coerceAtLeast(0f)
            return RoomGeometry(
                tw = tw,
                th = th,
                wallPx = wall,
                origin = Offset(widthPx / 2f, top + head + wall),
                scale = tw / RoomSpec.BASE_TILE_W,
            )
        }

        /** 가로만 아는 경우 — 방 비율대로 세로를 잡는다. */
        fun of(widthPx: Float): RoomGeometry = of(widthPx, widthPx / RoomSpec.ASPECT)
    }
}
