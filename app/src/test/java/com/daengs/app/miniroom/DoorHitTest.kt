package com.daengs.app.miniroom

import com.daengs.app.miniroom.art.DoorSpec
import com.daengs.app.miniroom.art.leftWallPoint
import com.daengs.app.miniroom.art.rightWallPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 문 그림과 누르는 자리가 어긋나면 화면만 봐서는 원인이 안 보인다.
 * 둘 다 DoorSpec 을 읽는지 여기서 고정한다.
 */
class DoorHitTest {

    private val g = RoomGeometry.of(411f)

    @Test
    fun `벽 평면 변환은 왕복된다`() {
        val pts = listOf(0f to 0f, 1.5f to 20f, 3.5f to 60f, 5f to 10f, 6f to 100f)
        pts.forEach { (u, h) ->
            val (u2, h2) = g.toLeftWall(g.leftWallPoint(u, h))
            assertEquals("u at ($u,$h)", u, u2, 0.001f)
            assertEquals("h at ($u,$h)", h, h2, 0.001f)
        }
    }

    @Test
    fun `문 안쪽은 눌린다`() {
        val h1 = DoorSpec.heightPx(g)
        val mid = (DoorSpec.U0 + DoorSpec.U1) / 2f
        listOf(
            mid to h1 * 0.5f,                       // 한가운데
            DoorSpec.U0 + 0.05f to h1 * 0.1f,       // 경첩 쪽 아래
            DoorSpec.U1 - 0.05f to h1 * 0.1f,       // 손잡이 쪽 아래
            mid to h1 * 0.95f,                      // 위쪽
            mid to 1f,                              // 문턱 바로 위
        ).forEach { (u, h) ->
            assertTrue("($u, $h) 은 문이어야 한다", DoorSpec.contains(g, g.leftWallPoint(u, h)))
        }
    }

    @Test
    fun `문 바깥은 안 눌린다`() {
        val h1 = DoorSpec.heightPx(g)
        val mid = (DoorSpec.U0 + DoorSpec.U1) / 2f
        listOf(
            mid to h1 * 1.15f,                      // 문 위 벽
            mid to -8f,                             // 문턱 아래 (바닥)
            DoorSpec.U0 - 0.4f to h1 * 0.5f,        // 모서리 쪽 옆
            DoorSpec.U1 + 0.4f to h1 * 0.5f,        // 반대쪽 옆
        ).forEach { (u, h) ->
            assertFalse("($u, $h) 은 문이 아니어야 한다", DoorSpec.contains(g, g.leftWallPoint(u, h)))
        }
    }

    @Test
    fun `창문 자리를 문으로 오판하지 않는다`() {
        // 창문은 u 0.7~2.5, 벽 높이의 30~72% 에 있다
        listOf(0.7f, 1.6f, 2.5f).forEach { u ->
            listOf(0.35f, 0.55f, 0.70f).forEach { frac ->
                val p = g.leftWallPoint(u, g.wallPx * frac)
                assertFalse("창문 자리 (u=$u) 가 문으로 잡혔다", DoorSpec.contains(g, p))
            }
        }
    }

    @Test
    fun `오른쪽 벽이나 바닥은 문이 아니다`() {
        assertFalse(DoorSpec.contains(g, g.rightWallPoint(4f, g.wallPx * 0.4f)))
        assertFalse("바닥 한가운데", DoorSpec.contains(g, g.footprintCenter(2, 2, androidx.compose.ui.unit.IntSize(1, 1))))
    }

    @Test
    fun `문 규격은 화면 크기가 달라져도 같은 비율을 유지한다`() {
        listOf(320f, 411f, 600f).forEach { w ->
            val geom = RoomGeometry.of(w)
            assertEquals(DoorSpec.H_FRAC, DoorSpec.heightPx(geom) / geom.wallPx, 0.0001f)
            // 어느 화면에서든 문 한가운데는 눌려야 한다
            val mid = (DoorSpec.U0 + DoorSpec.U1) / 2f
            assertTrue("width $w", DoorSpec.contains(geom, geom.leftWallPoint(mid, DoorSpec.heightPx(geom) * 0.5f)))
        }
    }
}
