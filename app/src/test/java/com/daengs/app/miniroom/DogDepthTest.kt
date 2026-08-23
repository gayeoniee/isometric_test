package com.daengs.app.miniroom

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 강아지와 가구의 앞뒤.
 *
 * **가구와 강아지는 같은 자로 재야 한다.** 가구는 칸을 통째로 차지해서 정렬 키가
 * 정수인데 강아지만 실수 좌표를 그대로 쓰면, 뒤에 있는 강아지가 가구 위에 그려져서
 * 몸이 가구를 뚫고 지나가는 것처럼 보인다. 실기기에서 "유령처럼 통과한다"고 걸린 것.
 */
class DogDepthTest {

    private fun dogAt(x: Float, y: Float): DogActor {
        val herd = DogHerd(count = 1)
        val d = herd.dogs.first()
        d.pos = Offset(x, y)
        return d
    }

    /** 책상이 (4,1) 에 1x1 로 놓인 경우의 정렬 키. */
    private val deskDepth = depthKey(4, 1, 1, 1)

    @Test
    fun `한 칸 뒤에 선 강아지는 가구보다 뒤로 간다`() {
        val dog = dogAt(3.7f, 1.5f)
        assertEquals(4, dog.depthCell)
        assertTrue("책상보다 뒤여야 한다", dog.depthCell < deskDepth)
    }

    @Test
    fun `실수 좌표를 그대로 쓰면 틀린다 - 회귀 기록`() {
        // 이게 뚫고 지나가 보이던 원인이다. col 3 (= 책상보다 한 칸 뒤) 인데
        // 합이 5.2 라 5 보다 커서 "앞" 으로 판정됐다.
        val dog = dogAt(3.7f, 1.5f)
        assertTrue(dog.pos.x + dog.pos.y > deskDepth)
        assertTrue(dog.depthCell < deskDepth)
    }

    @Test
    fun `앞 칸에 서면 가구보다 앞으로 온다`() {
        assertTrue(dogAt(4.5f, 2.5f).depthCell > deskDepth) // row +1
        assertTrue(dogAt(5.5f, 1.5f).depthCell > deskDepth) // col +1
    }

    @Test
    fun `뒤 칸에 서면 가구보다 뒤로 간다`() {
        assertTrue(dogAt(4.5f, 0.5f).depthCell < deskDepth) // row -1
        assertTrue(dogAt(3.5f, 1.5f).depthCell < deskDepth) // col -1
    }

    @Test
    fun `다칸 가구는 가장 앞 칸이 기준이다`() {
        // (2,2) 에 놓인 2x1 침대는 (2,2)(3,2) 를 먹는다 → 앞 칸은 (3,2)
        val bedDepth = depthKey(2, 2, 2, 1)
        assertEquals(5, bedDepth)
        // 침대 바로 앞칸(2,3) 의 강아지는 동률 → 같은 깊이면 강아지가 앞에 그려진다
        assertEquals(5, dogAt(2.5f, 3.5f).depthCell)
        // 한 칸 더 앞이면 확실히 앞
        assertTrue(dogAt(3.5f, 3.5f).depthCell > bedDepth)
        // 침대 뒤쪽은 뒤
        assertTrue(dogAt(2.5f, 1.5f).depthCell < bedDepth)
    }

    @Test
    fun `무리는 뒤에서 앞 순서로 정렬된다`() {
        val herd = DogHerd(count = 3)
        herd.dogs[0].pos = Offset(4.5f, 4.5f)
        herd.dogs[1].pos = Offset(0.5f, 0.5f)
        herd.dogs[2].pos = Offset(2.5f, 2.5f)
        assertEquals(listOf(1, 2, 0), herd.sortedByDepth().map { it.id })
    }
}
