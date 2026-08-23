package com.daengs.app.miniroom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import com.daengs.app.miniroom.art.DogCoat
import kotlin.math.abs
import kotlin.math.floor
import kotlin.random.Random

/**
 * 방을 돌아다니는 강아지 한 마리.
 *
 * **격자를 쓰지 않는다.** 위치가 실수 좌표(예: 2.7, 3.4)라 칸 개념이 없고,
 * 그래서 러그 위에 걸치거나 여러 마리가 서로 안 부딪힌다.
 * 격자는 **막힌 칸을 물어볼 때만** 쓴다 ([DogHerd.update] 의 `blocked`).
 *
 * 필드가 Compose state 가 아닌 이유: 캔버스는 강아지 애니메이션 때문에
 * 어차피 매 프레임 다시 그린다. 위치까지 state 로 두면 10마리 x 60fps 만큼
 * 쓸데없는 스냅샷 쓰기가 생긴다. 그리는 순간에 최신 값만 읽으면 된다.
 */
class DogActor(
    val id: Int,
    /**
     * 이 강아지의 아트 키. 털색마다 카탈로그 항목이 따로 있어서
     * ([com.daengs.app.miniroom.art.DogCoat]) 렌더러는 색을 몰라도 된다.
     */
    val artId: String,
    /**
     * 덩치. **전부 1f — 마리 구분은 덩치가 아니라 털색으로 한다.**
     * 덩치를 흔들었더니 같은 견종인데 원근이 깨진 것처럼 보였다.
     * 필드를 남겨두는 건 히트 판정·그리기가 이미 이걸 타고 있어서,
     * 나중에 새끼 강아지 같은 걸 넣을 때 여기만 건드리면 되기 때문이다.
     */
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
class DogHerd(count: Int, seed: Int = 7) {

    private val rnd = Random(seed)
    private var lastMs = 0L

    var dogs: List<DogActor> = emptyList()
        private set

    /** 손으로 잡고 있는 강아지. 잡힌 동안은 스스로 안 움직인다. */
    var draggingId: Int? = null

    init {
        dogs = List(count) { newDog(it) }
    }

    /**
     * 털색은 **뽑지 않고 차례대로 돌린다.** 무작위로 뽑으면 세 마리가 다 크림색으로
     * 나오는 판이 나와서, 여러 마리라는 게 눈에 안 들어온다.
     */
    private fun newDog(i: Int): DogActor {
        val d = DogActor(
            id = i,
            artId = DogCoat.ALL[i % DogCoat.ALL.size].id,
            sizeScale = 1f,
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

    fun setCount(n: Int) {
        if (n == dogs.size) return
        dogs = if (n < dogs.size) {
            dogs.take(n)
        } else {
            dogs + List(n - dogs.size) { newDog(dogs.size + it) }
        }
    }

    fun byId(id: Int): DogActor? = dogs.firstOrNull { it.id == id }

    /** 격자 밖으로 못 나가게. */
    fun clampToFloor(p: Offset): Offset {
        val lo = 0.15f
        val hi = RoomSpec.GRID - 0.15f
        return Offset(p.x.coerceIn(lo, hi), p.y.coerceIn(lo, hi))
    }

    // -- 가구 막기 --------------------------------------------------------------
    //
    // 막힌 칸은 [MiniRoomState.occupiedCells] 가 준다. 거기서 `flat` 아트(러그·강아지
    // 침대)는 이미 빠져 있으므로 **바닥에 깔린 것 위로는 지나가고, 서 있는 가구는
    // 통과하지 못한다** — 규칙을 여기서 새로 정의하지 않는다. 아이템 하나를 flat 으로
    // 바꾸면 배치 점유와 강아지 통행이 같이 따라온다.

    /**
     * 강아지가 차지하는 반경(격자 단위).
     *
     * 발끝 한 점만 검사하면 몸통이 책상에 반쯤 파묻힌 채로 멈춘다.
     * 그렇다고 크게 잡으면 가구 사이를 못 지나간다 — 통로가 2*이 값 보다 넓어야 한다.
     */
    private val bodyRadius = 0.22f

    private fun blockedAt(x: Float, y: Float, blocked: Set<IntOffset>): Boolean {
        if (blocked.isEmpty()) return false
        // 네 모서리. 가운데 한 점만 보면 모서리가 가구를 파고든다.
        for (dx in -1..1 step 2) {
            for (dy in -1..1 step 2) {
                val c = floor(x + dx * bodyRadius).toInt()
                val r = floor(y + dy * bodyRadius).toInt()
                if (IntOffset(c, r) in blocked) return true
            }
        }
        return false
    }

    /**
     * [from] 에서 [to] 로 가되 막힌 곳은 안 밟는다.
     *
     * 축을 하나씩 따로 시도하는 게 핵심이다. 둘을 한꺼번에 판정하면 가구 모서리에
     * 비스듬히 닿는 순간 완전히 멈춰서, 벽에 코를 박고 떠는 것처럼 보인다.
     * 축을 나누면 막힌 축만 버리고 **나머지 축으로 미끄러져** 가구를 타고 돌아간다.
     */
    private fun slide(from: Offset, to: Offset, blocked: Set<IntOffset>): Offset {
        var x = from.x
        var y = from.y
        if (!blockedAt(to.x, y, blocked)) x = to.x
        if (!blockedAt(x, to.y, blocked)) y = to.y
        return Offset(x, y)
    }

    /** 막히지 않은 무작위 목표. 방이 꽉 차 있으면 그냥 아무 데나 (탈출 로직이 꺼내준다). */
    private fun freeSpot(blocked: Set<IntOffset>): Offset {
        repeat(12) {
            val p = randomSpot()
            if (!blockedAt(p.x, p.y, blocked)) return p
        }
        return randomSpot()
    }

    /** 가장 가까운 빈 칸의 한가운데. 가구 밑에 깔렸을 때 걸어 나갈 방향이 된다. */
    private fun nearestFree(from: Offset, blocked: Set<IntOffset>): Offset {
        var best = from
        var bestD = Float.MAX_VALUE
        for (c in 0 until RoomSpec.GRID) {
            for (r in 0 until RoomSpec.GRID) {
                if (IntOffset(c, r) in blocked) continue
                val p = Offset(c + 0.5f, r + 0.5f)
                val d = (p - from).getDistanceSquared()
                if (d < bestD) {
                    bestD = d
                    best = p
                }
            }
        }
        return best
    }

    /**
     * 손으로 끌 때의 이동. 자율 이동과 **같은 규칙**을 탄다 —
     * 손가락으로도 책상을 뚫고 지나갈 수 없고, 모서리에서는 미끄러진다.
     */
    fun dragTo(dog: DogActor, desired: Offset, blocked: Set<IntOffset>) {
        dog.pos = slide(dog.pos, clampToFloor(desired), blocked)
    }

    /**
     * 매 프레임 호출. 목표까지 걸어가고, 도착하면 잠깐 쉬었다가 새 목표를 고른다.
     *
     * @param nowMs 프레임 시계 값
     * @param blocked 밟을 수 없는 칸 ([MiniRoomState.occupiedCells])
     */
    fun update(nowMs: Long, blocked: Set<IntOffset> = emptySet()) {
        if (lastMs == 0L) {
            lastMs = nowMs
            return
        }
        // 앱이 잠깐 멈췄다 돌아오면 dt 가 커져서 순간이동한다. 상한을 둔다.
        val dt = ((nowMs - lastMs) / 1000f).coerceIn(0f, 0.05f)
        lastMs = nowMs

        for (d in dogs) {
            if (d.id == draggingId) {
                d.moving = false
                continue
            }

            // 처음 자리가 나빴거나, 서 있는 자리에 가구가 놓였다. 충돌을 무시하고
            // 가까운 빈 칸으로 걸어 나온다 — 순간이동시키면 눈에 띄게 튄다.
            val trapped = blockedAt(d.pos.x, d.pos.y, blocked)
            if (trapped) {
                d.target = nearestFree(d.pos, blocked)
                d.restUntil = 0L
            } else if (nowMs < d.restUntil) {
                d.moving = false
                continue
            }

            val delta = d.target - d.pos
            val dist = delta.getDistance()
            if (dist < 0.06f) {
                d.restUntil = nowMs + 700L + rnd.nextLong(2200)
                d.target = freeSpot(blocked)
                d.moving = false
                continue
            }
            val step = d.speed * dt
            val want = clampToFloor(d.pos + delta * (step / dist).coerceAtMost(1f))
            // 갇힌 동안에는 막힘 판정을 끈다. 안 그러면 가구 밑에서 영영 못 나온다.
            val next = if (trapped) want else slide(d.pos, want, blocked)

            val gained = (next - d.pos).getDistance()
            if (gained < step * 0.2f) {
                // 미끄러질 여지도 없이 막혔다. 떠는 대신 다른 목표를 고른다.
                d.target = freeSpot(blocked)
                d.restUntil = nowMs + 250L
                d.moving = false
                continue
            }

            // 화면상 가로 이동 방향 = (col - row). 아이소메트릭이라 격자 축과 다르다.
            // 목표 방향이 아니라 **실제로 간 방향**으로 판정해야 한다 — 가구를 타고
            // 미끄러지는 동안에는 둘이 다르고, 그때 몸이 엉뚱한 쪽을 본다.
            val movedBy = next - d.pos
            val screenDx = movedBy.x - movedBy.y
            if (abs(screenDx) > 0.0005f) d.mirrored = screenDx < 0f

            d.pos = next
            d.moving = true
            d.phase += gained * 9f
        }
    }

    /**
     * 뒤에 있는 강아지부터. 가구와 깊이로 섞으려면 [DogActor.depthCell] 순이어야 한다.
     * 같은 칸에 둘이 겹치면 그때만 실수 좌표로 가른다.
     */
    fun sortedByDepth(): List<DogActor> =
        dogs.sortedWith(compareBy({ it.depthCell }, { it.pos.x + it.pos.y }))
}

/**
 * 강아지의 앞뒤 정렬 키 — **발끝이 딛고 있는 칸**으로 잰다.
 *
 * 실수 좌표를 그대로 `x + y` 로 쓰면 안 된다. 가구는 칸을 통째로 차지해서 정렬 키가
 * 정수인데, 강아지만 연속값이면 **자가 서로 다르다.** 예를 들어
 *
 * ```
 * 책상 (4,1)        -> 정렬 키 5
 * 강아지 (3.7, 1.5) -> 3.7 + 1.5 = 5.2  > 5  ->  "앞"
 * ```
 *
 * 강아지의 col 은 3 이라 책상보다 **한 칸 뒤**인데 앞으로 판정된다. 뒤에 있는 강아지가
 * 책상 위에 그려지니 몸이 책상을 뚫고 지나가는 것처럼 보인다.
 * 칸으로 내리면 `3 + 1 = 4 < 5` 라 제대로 뒤로 간다.
 *
 * 칸 단위라 값이 툭툭 끊기는 것도 이득이다 — 가구 옆에서 조금 흔들려도 앞뒤가
 * 깜빡이지 않는다. 게다가 [DogHerd] 의 몸 반경 덕에 막힌 칸 경계에 딱 붙어 서지도 못한다.
 */
val DogActor.depthCell: Int get() = floor(pos.x).toInt() + floor(pos.y).toInt()

@Composable
fun rememberDogHerd(count: Int): DogHerd {
    val herd = remember { DogHerd(count) }
    remember(count) { herd.setCount(count); count }
    return herd
}
