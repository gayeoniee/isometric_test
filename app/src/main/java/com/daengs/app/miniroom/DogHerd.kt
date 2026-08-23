package com.daengs.app.miniroom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.random.Random

/**
 * 방을 돌아다니는 강아지 한 마리.
 *
 * **격자를 쓰지 않는다.** 위치가 실수 좌표(예: 2.7, 3.4)라 칸 개념이 없고,
 * 그래서 가구와 겹쳐도 되고 여러 마리가 서로 안 부딪힌다.
 *
 * 필드가 Compose state 가 아닌 이유: 캔버스는 강아지 애니메이션 때문에
 * 어차피 매 프레임 다시 그린다. 위치까지 state 로 두면 10마리 x 60fps 만큼
 * 쓸데없는 스냅샷 쓰기가 생긴다. 그리는 순간에 최신 값만 읽으면 된다.
 */
class DogActor(
    val id: Int,
    val artId: String,
    /** 마리마다 살짝 다른 덩치. 같은 그림이어도 여러 마리로 보이게 한다. */
    val sizeScale: Float,
    /** 격자 단위/초 */
    val speed: Float,
) {
    var pos: Offset = Offset.Zero
    var target: Offset = Offset.Zero
    var restUntil: Long = 0L
    var moving: Boolean = false

    /** 진행 방향이 화면 왼쪽이면 좌우 반전. 3/4 앞모습 스프라이트를 대비한 것. */
    var mirrored: Boolean = false

    /** 걸음 위상. 이동 거리에 비례해 늘어나서 속도와 자동으로 맞는다. */
    var phase: Float = 0f
}

/**
 * 강아지 무리. 알아서 돌아다니고, 손으로 잡아 옮길 수 있다.
 *
 * 위치는 저장하지 않는다 — 어차피 계속 돌아다녀서 저장해도 의미가 없다.
 */
@Stable
class DogHerd(count: Int, artId: String = RoomDefaults.DOG_ID, seed: Int = 7) {

    private val rnd = Random(seed)
    private var lastMs = 0L

    var dogs: List<DogActor> = emptyList()
        private set

    /** 손으로 잡고 있는 강아지. 잡힌 동안은 스스로 안 움직인다. */
    var draggingId: Int? = null

    init {
        dogs = List(count) { newDog(it, artId) }
    }

    private fun newDog(i: Int, artId: String): DogActor {
        val d = DogActor(
            id = i,
            artId = artId,
            sizeScale = 0.88f + rnd.nextFloat() * 0.26f,
            speed = 0.55f + rnd.nextFloat() * 0.5f,
        )
        d.pos = randomSpot()
        d.target = randomSpot()
        d.restUntil = rnd.nextLong(0, 1500)
        return d
    }

    /** 벽에 딱 붙지 않게 가장자리를 조금 남긴다. */
    private fun randomSpot(): Offset {
        val lo = 0.4f
        val hi = RoomSpec.GRID - 0.4f
        return Offset(lo + rnd.nextFloat() * (hi - lo), lo + rnd.nextFloat() * (hi - lo))
    }

    fun setCount(n: Int, artId: String = RoomDefaults.DOG_ID) {
        if (n == dogs.size) return
        dogs = if (n < dogs.size) {
            dogs.take(n)
        } else {
            dogs + List(n - dogs.size) { newDog(dogs.size + it, artId) }
        }
    }

    fun byId(id: Int): DogActor? = dogs.firstOrNull { it.id == id }

    /** 격자 밖으로 못 나가게. 드래그로 옮길 때도 쓴다. */
    fun clampToFloor(p: Offset): Offset {
        val lo = 0.15f
        val hi = RoomSpec.GRID - 0.15f
        return Offset(p.x.coerceIn(lo, hi), p.y.coerceIn(lo, hi))
    }

    /**
     * 매 프레임 호출. 목표까지 걸어가고, 도착하면 잠깐 쉬었다가 새 목표를 고른다.
     *
     * @param nowMs 프레임 시계 값
     */
    fun update(nowMs: Long) {
        if (lastMs == 0L) {
            lastMs = nowMs
            return
        }
        // 앱이 잠깐 멈췄다 돌아오면 dt 가 커져서 순간이동한다. 상한을 둔다.
        val dt = ((nowMs - lastMs) / 1000f).coerceIn(0f, 0.05f)
        lastMs = nowMs

        for (d in dogs) {
            if (d.id == draggingId || nowMs < d.restUntil) {
                d.moving = false
                continue
            }
            val delta = d.target - d.pos
            val dist = delta.getDistance()
            if (dist < 0.06f) {
                d.restUntil = nowMs + 700L + rnd.nextLong(2200)
                d.target = randomSpot()
                d.moving = false
                continue
            }
            val step = d.speed * dt
            d.pos += delta * (step / dist).coerceAtMost(1f)
            d.moving = true
            d.phase += step * 9f

            // 화면상 가로 이동 방향 = (col - row). 아이소메트릭이라 격자 축과 다르다.
            val screenDx = delta.x - delta.y
            if (abs(screenDx) > 0.01f) d.mirrored = screenDx < 0f
        }
    }

    /** 앞에 있는 강아지부터 그린다 (깊이 = col + row). */
    fun sortedByDepth(): List<DogActor> = dogs.sortedBy { it.pos.x + it.pos.y }
}

@Composable
fun rememberDogHerd(count: Int): DogHerd {
    val herd = remember { DogHerd(count) }
    remember(count) { herd.setCount(count); count }
    return herd
}
