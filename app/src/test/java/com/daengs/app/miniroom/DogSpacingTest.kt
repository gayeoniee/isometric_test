package com.daengs.app.miniroom

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 강아지끼리 **목적지를 벌린다.**
 *
 * 예전엔 목적지를 완전 무작위로 뽑아서 둘이 우연히 같은 자리를 고르면 겹친 채로
 * 오래 서 있었다. 머리를 키운 뒤(등신 2.2 → 1.7) 겹치는 면적이 넓어져 눈에 띄게 됐다.
 *
 * 지나가다 잠깐 스치는 건 그대로 둔다 — 그건 오히려 자연스럽다.
 * 여기서 고정하는 건 **오래 머무는 자리(목적지)가 서로 떨어져 있는가** 하나다.
 *
 * 방이 좁거나 가구가 많아 자리가 안 나올 수 있으므로 간격은 **보장이 아니라 노력**이다.
 * 그래서 "항상 떨어진다"가 아니라 "겹친 채 오래 서 있는 판이 확 줄어든다"를 검사한다.
 */
class DogSpacingTest {

    /** 목적지에 도착해 쉬고 있는(=움직이지 않는) 강아지 쌍의 최소 거리. */
    private fun restingPairMinDistance(herd: DogHerd): Float {
        var min = Float.MAX_VALUE
        val resting = herd.dogs.filter { !it.moving }
        for (i in resting.indices) {
            for (j in i + 1 until resting.size) {
                val d = (resting[i].pos - resting[j].pos).getDistance()
                if (d < min) min = d
            }
        }
        return min
    }

    @Test
    fun `목적지는 서로 떨어져서 뽑힌다`() {
        val herd = DogHerd(count = 4)
        var t = 0L
        herd.update(t)

        // 목적지를 여러 번 새로 뽑게 오래 돌린다
        var tooClose = 0
        var samples = 0
        repeat(4000) {
            t += 16L
            herd.update(t)
            val d = restingPairMinDistance(herd)
            if (d != Float.MAX_VALUE) {
                samples++
                if (d < DogHerd.MIN_DOG_GAP * 0.5f) tooClose++
            }
        }

        assertTrue("쉬는 강아지 쌍 표본이 없다 — 테스트가 아무것도 안 봤다", samples > 100)
        val ratio = tooClose.toFloat() / samples
        assertTrue(
            "겹쳐 선 비율 ${(ratio * 100).toInt()}% — 목적지가 안 벌어지고 있다",
            ratio < 0.10f,
        )
    }

    @Test
    fun `방이 가구로 꽉 차도 멈추지 않는다`() {
        // 한 칸만 남기고 전부 막는다. 간격을 지킬 자리가 없는 극단 상황
        val blocked: Set<IntOffset> = buildSet {
            for (c in 0 until RoomSpec.GRID) {
                for (r in 0 until RoomSpec.GRID) {
                    if (c != 0 || r != 0) add(IntOffset(c, r))
                }
            }
        }
        val herd = DogHerd(count = 4)
        for (d in herd.dogs) d.pos = Offset(0.5f, 0.5f)

        var t = 0L
        herd.update(t, blocked)
        // 간격을 못 지킨다고 목적지 고르기가 무한루프에 빠지거나 멈추면 안 된다
        repeat(600) {
            t += 16L
            herd.update(t, blocked)
        }
        for (d in herd.dogs) {
            assertTrue("x=${d.pos.x} 가 격자 밖", d.pos.x in 0f..RoomSpec.GRID.toFloat())
            assertTrue("y=${d.pos.y} 가 격자 밖", d.pos.y in 0f..RoomSpec.GRID.toFloat())
        }
    }
}
