package com.daengs.app.miniroom

import androidx.compose.ui.geometry.Offset
import com.daengs.app.miniroom.art.DoorSpec
import com.daengs.app.miniroom.art.FrameSpec
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 벽에 건 액자 터치 판정. 누르면 네오 채소 도감이 열린다.
 *
 * 제일 중요한 건 **문과 안 겹치는 것**이다. 겹치면 산책하러 문을 누르다가 도감이
 * 열린다. 그래서 액자는 문·창문이 없는 오른쪽 빈 벽에 건다.
 */
class FrameHitTest {

    private val g = RoomGeometry.of(900f)

    /** 방 그림 안의 백분율 위치 -> 화면 좌표. */
    private fun at(xPct: Float, yPct: Float) = Offset(
        g.stage.left + xPct / 100f * g.stage.width,
        g.stage.top + yPct / 100f * g.stage.height,
    )

    @Test
    fun `액자 한가운데는 액자다`() {
        val cx = (FrameSpec.LEFT + FrameSpec.RIGHT) / 2f
        val cy = FrameSpec.TOP + FrameSpec.drop(cx) + FrameSpec.HEIGHT / 2f
        assertTrue(FrameSpec.contains(g, at(cx, cy)))
    }

    /**
     * 문과 액자가 겹치면 안 된다. 겹치면 문을 누르려다 도감이 열린다.
     *
     * 화면 좌표로 직접 재는 이유: 둘이 서로 다른 방식으로 범위를 만들기 때문이다
     * (문은 사각형, 액자는 기울어진 것의 외접 사각형). 백분율끼리 비교하면 놓친다.
     */
    @Test
    fun `문과 액자는 겹치지 않는다`() {
        val door = DoorSpec.rectOf(g, DoorSpec.frame)
        val frame = FrameSpec.bounds(g)
        assertTrue(
            "문 $door 과 액자 $frame 가 겹친다",
            door.right < frame.left || frame.right < door.left ||
                door.bottom < frame.top || frame.bottom < door.top,
        )
    }

    @Test
    fun `문을 눌러도 액자가 안 잡힌다`() {
        val cx = (DoorSpec.leaf.left + DoorSpec.leaf.right) / 2f
        val cy = (DoorSpec.leaf.top + DoorSpec.leaf.bottom) / 2f
        assertFalse(FrameSpec.contains(g, at(cx, cy)))
    }

    /** 창문은 같은 벽 아래쪽에 있다. 액자와 가로 범위가 겹치면 안 된다. */
    @Test
    fun `창문 자리는 액자가 아니다`() {
        listOf(33f, 40f, 46f).forEach { x ->
            listOf(20f, 30f, 42f).forEach { y ->
                assertFalse("창문 자리 ($x%, $y%)", FrameSpec.contains(g, at(x, y)))
            }
        }
    }

    @Test
    fun `바닥과 천장은 액자가 아니다`() {
        assertFalse("바닥 한가운데", FrameSpec.contains(g, g.footprintCenter(6, 6, androidx.compose.ui.unit.IntSize(1, 1))))
        assertFalse("액자 위쪽 벽", FrameSpec.contains(g, at(80f, 12f)))
        assertFalse("액자 아래쪽 벽", FrameSpec.contains(g, at(80f, 62f)))
    }

    /**
     * 액자가 벽을 벗어나면 허공에 뜬다. 오른쪽 빈 벽으로 재둔 범위 안에 있어야 한다.
     * (그림에서 잰 빈 벽: 가로 67.4~96.6%, 세로 22.0~51.9%)
     */
    @Test
    fun `액자는 빈 벽 범위 안에 있다`() {
        assertTrue("왼쪽이 벽 밖", FrameSpec.LEFT >= 67.4f)
        assertTrue("오른쪽이 벽 밖", FrameSpec.RIGHT <= 96.6f)
        assertTrue("위가 벽 밖", FrameSpec.TOP >= 22.0f)
        val lowest = FrameSpec.TOP + FrameSpec.HEIGHT + FrameSpec.drop(FrameSpec.RIGHT)
        assertTrue("아래가 벽 밖 ($lowest%)", lowest <= 51.9f)
    }

    /** 백분율이라 화면 크기가 변해도 같은 자리를 가리켜야 한다. */
    @Test
    fun `방 크기가 달라져도 같은 자리다`() {
        val cx = (FrameSpec.LEFT + FrameSpec.RIGHT) / 2f
        val cy = FrameSpec.TOP + FrameSpec.drop(cx) + FrameSpec.HEIGHT / 2f
        listOf(320f, 640f, 1200f).forEach { w ->
            val geom = RoomGeometry.of(w)
            val p = Offset(
                geom.stage.left + cx / 100f * geom.stage.width,
                geom.stage.top + cy / 100f * geom.stage.height,
            )
            assertTrue("width $w", FrameSpec.contains(geom, p))
        }
    }
}
