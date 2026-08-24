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

    // 411dp 폭 기기 기준
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
        // 칸 폭은 원근 때문에 자리마다 다르다. 평균값의 1/5 이면 어느 칸에서도 안쪽이다.
        val nudges = listOf(
            Offset(g.cell / 5f, 0f),
            Offset(-g.cell / 5f, 0f),
            Offset(0f, g.cell / 10f),
            Offset(0f, -g.cell / 10f),
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

    /**
     * 격자 네 꼭짓점이 **방 그림의 바닥 네 귀퉁이**에 놓이는가.
     *
     * 예전에는 좌우 대칭 마름모라 "폭 = GRID x tw" 같은 식으로 잴 수 있었다.
     * 지금은 원근이 들어간 사각형이라 대칭이 아니고, 기준은 [FloorQuad] 백분율뿐이다.
     */
    @Test
    fun `격자 꼭짓점이 방 그림의 바닥 귀퉁이에 있다`() {
        val n = RoomSpec.GRID.toFloat()
        val corners = listOf(
            FloorQuad.back to g.toScreenF(0f, 0f),
            FloorQuad.right to g.toScreenF(n, 0f),
            FloorQuad.front to g.toScreenF(n, n),
            FloorQuad.left to g.toScreenF(0f, n),
        )
        corners.forEach { (pctPoint, screen) ->
            val wantX = g.stage.left + pctPoint.x / 100f * g.stage.width
            val wantY = g.stage.top + pctPoint.y / 100f * g.stage.height
            assertEquals("x of $pctPoint", wantX, screen.x, 0.01f)
            assertEquals("y of $pctPoint", wantY, screen.y, 0.01f)
        }
    }

    /**
     * 바닥은 **앞쪽이 더 넓다.** 원근 사각형이라는 사실 자체를 고정한다 —
     * 누가 실수로 대칭 마름모로 되돌리면 여기서 걸린다.
     */
    @Test
    fun `앞쪽 칸이 뒤쪽 칸보다 넓다`() {
        val n = RoomSpec.GRID.toFloat()
        val backWidth = g.toScreenF(1f, 0f).x - g.toScreenF(0f, 0f).x
        val frontWidth = g.toScreenF(1f, n).x - g.toScreenF(0f, n).x
        assertTrue("뒤 $backWidth / 앞 $frontWidth", frontWidth > backWidth)
    }

    @Test
    fun `방 크기가 달라져도 격자는 같은 비율에 놓인다`() {
        listOf(320f, 360f, 411f, 480f, 600f).forEach { w ->
            val geom = RoomGeometry.of(w)
            val p = geom.toScreenF(RoomSpec.GRID / 2f, RoomSpec.GRID / 2f)
            val xPct = (p.x - geom.stage.left) / geom.stage.width * 100f
            val yPct = (p.y - geom.stage.top) / geom.stage.height * 100f
            assertEquals("x at width $w", 49.725f, xPct, 0.05f)
            assertEquals("y at width $w", 71.875f, yPct, 0.05f)
        }
    }

    /**
     * 격자 바깥은 **음수 칸**으로 나와야 한다.
     *
     * 옛 `toGrid` 는 `.toInt()` 를 써서 0 방향으로 잘랐다. col 이 -0.4 인 지점(격자
     * 왼쪽 바깥)이 0 으로 잘려서, 밖으로 끌어낸 아이템이 조용히 (0,0) 에 붙었다.
     * 지금은 floor 라 -1 이 나온다 — 그래도 범위 검사는 실수값으로 하는 게 안전하다.
     */
    @Test
    fun `격자 바깥은 바깥으로 나온다`() {
        val outside = g.toScreenF(-0.4f, 2f)

        val (cInt, _) = g.toGrid(outside)
        assertEquals("floor 라 -1 이다", -1, cInt)

        val (cF, rF) = g.toGridF(outside)
        assertTrue("실수값도 음수다", cF < 0f)
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
        // (5,0) 과 (0,5) 는 방의 정반대편인데 화면 y 는 비슷하다 — y 로 정렬하면
        // 둘의 앞뒤가 뒤죽박죽이 된다. 깊이 키는 둘 다 5 로 같아야 맞다.
        val a = g.toScreen(5, 0)
        val d = g.toScreen(0, 5)
        assertTrue("화면 y 가 비슷해서 y 정렬은 못 쓴다", kotlin.math.abs(a.y - d.y) < g.cell)
        assertTrue("그런데 x 는 반대편이다", a.x > d.x)
    }
}
