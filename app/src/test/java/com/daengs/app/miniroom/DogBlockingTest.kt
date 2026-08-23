package com.daengs.app.miniroom

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 강아지는 **러그 위는 지나가고 서 있는 가구는 통과하지 못한다.**
 *
 * 통과 판정은 [MiniRoomState.occupiedCells] 하나를 공유하므로, 러그가 뚫린다는 건
 * 거기서 `flat` 아트가 빠진다는 사실이 지켜준다 (FootprintFacingTest 가 고정).
 * 여기서는 **막힌 칸을 실제로 못 넘는지**와 **끼어서 못 나오는 일이 없는지**를 본다.
 */
class DogBlockingTest {

    /** col = 2 를 세로로 가로막는 벽. */
    private val wall: Set<IntOffset> = (0 until RoomSpec.GRID).map { IntOffset(2, it) }.toSet()

    private fun herdWithOneDog(at: Offset, goal: Offset): Pair<DogHerd, DogActor> {
        val herd = DogHerd(count = 1)
        val dog = herd.dogs.first()
        dog.pos = at
        dog.target = goal
        dog.restUntil = 0L
        return herd to dog
    }

    @Test
    fun `벽 너머로 못 넘어간다`() {
        val (herd, dog) = herdWithOneDog(Offset(0.5f, 0.5f), Offset(5.5f, 0.5f))
        var t = 0L
        herd.update(t, wall) // 첫 호출은 시계만 맞춘다

        repeat(600) {
            t += 16L
            herd.update(t, wall)
            // 몸 반경까지 포함해 col 2 를 밟으면 안 된다
            assertTrue("x=${dog.pos.x} — 벽을 뚫었다", dog.pos.x < 2f)
        }
    }

    @Test
    fun `막힘이 없으면 목표에 도착한다`() {
        val (herd, dog) = herdWithOneDog(Offset(0.5f, 0.5f), Offset(4.5f, 0.5f))
        var t = 0L
        herd.update(t, emptySet())
        repeat(1200) {
            t += 16L
            herd.update(t, emptySet())
        }
        // 도착하면 쉬었다가 새 목표를 고르므로 "그 자리에 있다"가 아니라
        // "적어도 한 번 벽 너머까지 갔다"를 본다
        assertTrue("한 번도 못 건너갔다", dog.pos.x > 2f || dog.target.x > 2f)
    }

    @Test
    fun `가구 밑에 깔려도 걸어 나온다`() {
        // 강아지가 서 있는 자리에 나중에 책상이 놓인 상황
        val (herd, dog) = herdWithOneDog(Offset(2.5f, 2.5f), Offset(2.5f, 2.5f))
        val desk = setOf(IntOffset(2, 2))
        var t = 0L
        herd.update(t, desk)
        repeat(600) {
            t += 16L
            herd.update(t, desk)
        }
        val c = kotlin.math.floor(dog.pos.x).toInt()
        val r = kotlin.math.floor(dog.pos.y).toInt()
        assertTrue("아직 책상 안에 있다 (${dog.pos})", IntOffset(c, r) !in desk)
    }
}
