package com.daengs.app.miniroom

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.floor

/**
 * CONTEXT.md 4번 섹션의 좌표 변환이 실제로 맞게 도는지.
 * 여기가 틀리면 화면에서 아이템이 엉뚱한 칸에 붙고 앞뒤가 뒤집힌다.
 */
class IsoMathTest {

    // 411dp 폭 기기 기준. tw = 411/6 = 68.5
    private val g = RoomGeometry.of(411f)

    @Test
    fun `타일 중심은 같은 칸으로 되돌아온다`() {
        for (col in 0 until RoomSpec.GRID) {
            for (row in 0 until RoomSpec.GRID) {
                val center = g.footprintCenter(col, row, IntSize(1, 1))
                val (c, r) = g.toGrid(center)
                assertEquals("col at ($col,$row)", col, c)
                assertEquals("row at ($col,$row)", row, r)
            }
        }
    }

    @Test
    fun `칸 중심에서 조금 벗어나도 같은 칸이다`() {
        val nudges = listOf(
            Offset(g.tw / 5f, 0f),
            Offset(-g.tw / 5f, 0f),
            Offset(0f, g.th / 5f),
            Offset(0f, -g.th / 5f),
        )
        for (col in 0 until RoomSpec.GRID) {
            for (row in 0 until RoomSpec.GRID) {
                val center = g.footprintCenter(col, row, IntSize(1, 1))
                nudges.forEach { n ->
                    val (c, r) = g.toGrid(center + n)
                    assertEquals("col at ($col,$row) + $n", col, c)
                    assertEquals("row at ($col,$row) + $n", row, r)
                }
            }
        }
    }

    @Test
    fun `격자 꼭짓점이 예상 위치에 있다`() {
        val n = RoomSpec.GRID.toFloat()
        val top = g.toScreenF(0f, 0f)
        val right = g.toScreenF(n, 0f)
        val bottom = g.toScreenF(n, n)
        val left = g.toScreenF(0f, n)

        // 다이아몬드 폭 = 6 * tw, 높이 = 6 * th
        assertEquals(6f * g.tw, right.x - left.x, 0.01f)
        assertEquals(6f * g.th, bottom.y - top.y, 0.01f)
        // 위/아래 꼭짓점은 가로 중앙에 있다
        assertEquals(top.x, bottom.x, 0.01f)
        assertEquals(411f / 2f, top.x, 0.01f)
    }

    @Test
    fun `타일 비율은 화면 크기와 무관하게 일정하다`() {
        listOf(320f, 360f, 411f, 480f, 600f).forEach { w ->
            val geom = RoomGeometry.of(w)
            assertEquals("width $w", RoomSpec.TILE_RATIO, geom.tw / geom.th, 0.001f)
        }
    }

    /**
     * toGrid 의 `.toInt()` 는 floor 가 아니라 0 방향 잘림이라
     * 격자 왼쪽/위쪽 바깥의 -0.x 지점을 0 으로 만들어 버린다.
     * 그래서 범위 검사는 실수값(toGridF)으로 해야 한다.
     */
    @Test
    fun `격자 바깥은 실수값으로만 걸러낼 수 있다`() {
        val outside = g.toScreenF(-0.4f, 2f)

        val (cInt, _) = g.toGrid(outside)
        assertEquals("잘림 때문에 0 으로 보인다", 0, cInt)

        val (cF, rF) = g.toGridF(outside)
        assertTrue("실수값은 음수로 나온다", cF < 0f)
        assertFalse("따라서 바깥으로 판정된다", g.isInside(cF, rF))
    }

    @Test
    fun `격자 안쪽은 안쪽으로 판정된다`() {
        for (col in 0 until RoomSpec.GRID) {
            for (row in 0 until RoomSpec.GRID) {
                val (cF, rF) = g.toGridF(g.footprintCenter(col, row, IntSize(1, 1)))
                assertTrue("($col,$row)", g.isInside(cF, rF))
            }
        }
    }

    @Test
    fun `칸 이동량은 floor 로 계산해야 원점 근처에서도 맞다`() {
        // (0,2) 칸을 잡고 왼쪽으로 한 칸 끌면 격자 밖(-1)이 나와야 한다.
        val start = g.footprintCenter(0, 2, IntSize(1, 1))
        val moved = start + (g.toScreenF(0f, 1f) - g.toScreenF(1f, 1f))

        val (c0, _) = g.toGridF(start)
        val (c1, _) = g.toGridF(moved)
        assertEquals(-1, floor(c1).toInt() - floor(c0).toInt())
    }

    @Test
    fun `다칸 아이템은 가장 앞 칸으로 정렬된다`() {
        // 1x1 은 기존과 동일해야 한다 (회귀 방지)
        assertEquals(4, depthKey(2, 2, 1, 1))
        assertEquals(0, depthKey(0, 0, 1, 1))

        // 2x1 침대가 (0,1) 에 있으면 앞칸은 (1,1) 이라 깊이 2
        assertEquals(2, depthKey(0, 1, 2, 1))
        // col+row 로 재면 1 이라, (1,1) 에 있는 1x1 물건(깊이 2)보다 뒤로 밀린다
        assertTrue("앞칸 기준이 더 커야 한다", depthKey(0, 1, 2, 1) > 0 + 1)

        // 2x2 는 양쪽 다 +1
        assertEquals(6, depthKey(2, 2, 2, 2))
    }

    @Test
    fun `앞뒤 정렬 키는 col 더하기 row 다`() {
        val items = listOf(
            PlacedItem(1, "a", 5, 0), // depth 5
            PlacedItem(2, "b", 0, 0), // depth 0
            PlacedItem(3, "c", 2, 2), // depth 4
            PlacedItem(4, "d", 0, 5), // depth 5
        )
        val order = items.sortedBy { it.depth }.map { it.instanceId }
        assertEquals(listOf(2L, 3L, 1L, 4L), order)

        // 화면 y 로 정렬하면 안 된다는 것도 확인한다.
        // (5,0) 과 (0,5) 는 화면 y 가 같지만 x 가 반대편이다.
        val a = g.toScreen(5, 0)
        val d = g.toScreen(0, 5)
        assertEquals(a.y, d.y, 0.01f)
        assertTrue(a.x > d.x)
    }
}
