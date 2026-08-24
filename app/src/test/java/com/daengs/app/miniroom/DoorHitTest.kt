package com.daengs.app.miniroom

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.daengs.app.miniroom.art.DoorSpec
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 문 터치 판정.
 *
 * 문은 방 그림(modular_empty_room_v1.png)에 구워져 있고, [DoorSpec] 은 그 그림에서
 * 자로 재서 뽑은 **백분율 사각형**이다. 그리는 쪽과 누르는 쪽이 같은 값을 읽는지,
 * 그리고 창문·바닥 같은 이웃을 문으로 오인하지 않는지를 여기서 고정한다.
 *
 * 예전 판본은 벽 평면 좌표계(u, h)로 문을 정의했다. 벽을 코드로 그리던 시절의
 * 규격이라 그림으로 바뀌면서 통째로 갈렸다.
 */
class DoorHitTest {

    private val g = RoomGeometry.of(900f)

    /** 방 그림 안의 백분율 위치 → 화면 좌표. 테스트가 눈금을 직접 읽게 해준다. */
    private fun at(xPct: Float, yPct: Float) = Offset(
        g.stage.left + xPct / 100f * g.stage.width,
        g.stage.top + yPct / 100f * g.stage.height,
    )

    @Test
    fun `문짝 한가운데는 문이다`() {
        val cx = (DoorSpec.leaf.left + DoorSpec.leaf.right) / 2f
        val cy = (DoorSpec.leaf.top + DoorSpec.leaf.bottom) / 2f
        assertTrue(DoorSpec.contains(g, at(cx, cy)))
    }

    @Test
    fun `문짝 네 귀퉁이 안쪽도 문이다`() {
        val inset = 0.6f
        val corners = listOf(
            DoorSpec.leaf.left + inset to DoorSpec.leaf.top + inset,
            DoorSpec.leaf.right - inset to DoorSpec.leaf.top + inset,
            DoorSpec.leaf.left + inset to DoorSpec.leaf.bottom - inset,
            DoorSpec.leaf.right - inset to DoorSpec.leaf.bottom - inset,
        )
        corners.forEach { (x, y) ->
            assertTrue("($x%, $y%) 은 문이어야 한다", DoorSpec.contains(g, at(x, y)))
        }
    }

    @Test
    fun `문틀 바깥은 문이 아니다`() {
        val out = 2.5f
        val outside = listOf(
            DoorSpec.frame.left - out to 50f,      // 문 왼쪽 벽
            DoorSpec.frame.right + out to 50f,     // 문 오른쪽 벽
            12f to DoorSpec.frame.top - out,       // 문 위 벽
            12f to DoorSpec.frame.bottom + out,    // 문 아래 걸레받이
        )
        outside.forEach { (x, y) ->
            assertFalse("($x%, $y%) 은 문이 아니어야 한다", DoorSpec.contains(g, at(x, y)))
        }
    }

    /**
     * 창문은 같은 벽에 있고 문보다 크다. 둘의 가로 범위가 겹치면 창문을 눌러도
     * 산책 게임이 뜨는 사고가 난다 — 실제로 벽 평면 시절에 한 번 겪은 자리다.
     */
    @Test
    fun `같은 벽의 창문은 문이 아니다`() {
        listOf(30f, 38f, 46f, 52f).forEach { x ->
            listOf(20f, 32f, 44f).forEach { y ->
                assertFalse("창문 자리 ($x%, $y%) 가 문으로 잡혔다", DoorSpec.contains(g, at(x, y)))
            }
        }
    }

    @Test
    fun `바닥과 오른쪽 벽은 문이 아니다`() {
        assertFalse("바닥 한가운데", DoorSpec.contains(g, g.footprintCenter(6, 6, IntSize(1, 1))))
        assertFalse("오른쪽 벽", DoorSpec.contains(g, at(80f, 40f)))
    }

    /** 백분율이라 화면 크기가 변해도 같은 자리를 가리켜야 한다. */
    @Test
    fun `방 크기가 달라져도 같은 자리다`() {
        val cx = (DoorSpec.leaf.left + DoorSpec.leaf.right) / 2f
        val cy = (DoorSpec.leaf.top + DoorSpec.leaf.bottom) / 2f
        listOf(320f, 640f, 1200f).forEach { w ->
            val geom = RoomGeometry.of(w)
            val p = Offset(
                geom.stage.left + cx / 100f * geom.stage.width,
                geom.stage.top + cy / 100f * geom.stage.height,
            )
            assertTrue("width $w", DoorSpec.contains(geom, p))
        }
    }
}
