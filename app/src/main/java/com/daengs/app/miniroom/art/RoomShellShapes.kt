package com.daengs.app.miniroom.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.daengs.app.miniroom.FloorQuad
import com.daengs.app.miniroom.RoomGeometry
import com.daengs.app.miniroom.RoomSpec
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ---------------------------------------------------------------------------
// 방 껍데기 — **그림 한 장**
//
// 예전에는 벽·바닥·창문·문·울타리를 전부 코드로 그렸다(500줄). 이제 그 전부가
// modular_empty_room_v1.png 안에 구워져 있어서 그림을 한 번 그리면 끝난다.
//
// 딱 하나 남은 예외가 **문**이다. 문은 눌러서 여는 물건이라 그림에 구워두면
// 열리지 않는다. 그렇다고 문만 벡터로 새로 그리면 픽셀 아트 방에 매끈한 도형이
// 얹혀 화풍이 깨진다.
//
// 그래서 **그림에서 문만 오려내 다시 그린다.** 같은 PNG 의 문 영역을 소스로 삼아
// 경첩 쪽으로 눌러 그리면, 새 아트 없이 원래 화풍 그대로 열리는 문이 된다.
// ---------------------------------------------------------------------------

/** 방 그림을 [RoomGeometry.stage] 에 채운다. 픽셀 아트라 보간을 끈다. */
fun DrawScope.drawRoomBackground(g: RoomGeometry, room: ImageBitmap) {
    drawImage(
        image = room,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(room.width, room.height),
        dstOffset = IntOffset(g.stage.left.roundToInt(), g.stage.top.roundToInt()),
        dstSize = IntSize(g.stage.width.roundToInt(), g.stage.height.roundToInt()),
        // 픽셀 아트를 확대할 때 기본값(Medium)이면 뿌옇게 번진다
        filterQuality = FilterQuality.None,
    )
}

/**
 * 문 규격 — **방 그림에 자로 재서 뽑은 값**이다.
 *
 * 예전엔 벽 평면 좌표계(u, h)로 문을 정의했다. 벽을 코드로 그리던 시절엔 그게 맞았지만
 * 이제 벽이 그림이라, 그림 안에서 문이 실제로 있는 자리를 백분율로 잡는 게 맞다.
 *
 * 값은 1122x1402 원본에서 올리브색 문짝의 경계를 찾아 재고 백분율로 환산했다.
 */
object DoorSpec {
    /**
     * 문짝을 감싸는 사각형. 여닫는 대상이자 터치 판정 영역이다.
     *
     * **사각형은 문짝보다 크다.** 문이 아치라 위 모서리가 남고, 밑변이 비스듬해
     * 오른쪽 아래도 남는다. 남는 자리는 벽이므로 그릴 때는 [ARCH_RISE] · [SHEAR] 를
     * 반영한 실루엣으로 잘라내야 한다.
     *
     * top 은 **기울이기 전** 아치 꼭대기다. 그림에서 재면 꼭대기가 39.02% 지만 그건
     * 이미 기울어진 값이고 여기에 [SHEAR] 를 다시 얹으므로, 41.02% 를 넣는다.
     */
    val leaf = Rect(left = 6.77f, top = 41.02f, right = 17.83f, bottom = 66.05f)

    /** 문틀까지 포함한 범위. 터치를 조금 너그럽게 받으려고 쓴다. */
    val frame = Rect(left = 5.2f, top = 37.0f, right = 19.3f, bottom = 67.5f)

    /**
     * 문이 붙은 벽이 물러나면서 **오른쪽이 올라간 정도**. 문짝 높이 대비 비율.
     *
     * 그림에서 문 밑변을 재면 왼쪽 66.05%, 오른쪽 62.77% 로 3.28%p 차이가 난다.
     * 아치 윗변도 같은 만큼 기울어 있다 — 대칭 아치를 세로로 밀어놓은 모양이다.
     */
    const val SHEAR = -0.1307f

    /**
     * 아치가 어깨에서 꼭대기까지 솟은 높이. 문짝 높이 대비 비율.
     *
     * 반원(폭의 절반)으로 그렸더니 **문보다 볼록해서** 양옆으로 문 위까지 열렸다.
     * 실제로는 폭 124px 에 솟음 49px 인 납작한 타원이다.
     */
    const val ARCH_RISE = 0.123f

    /**
     * 문 너머로 보여줄 바깥. **창유리에서 오려 쓴다.**
     *
     * 문간을 어둡게 칠했더니 나가는 문이 아니라 검은 구멍으로 읽혔다. 밖이 보여야
     * 나가는 문이다. 새로 그리면 화풍이 깨지므로 같은 그림의 창유리를 쓴다 —
     * 창살이 안 걸리는 오른쪽 아래 한 칸이다.
     */
    val outside = Rect(left = 42.87f, top = 30.53f, right = 48.84f, bottom = 43.37f)

    /** 경첩은 오른쪽. 그림에서 손잡이가 왼쪽에 있다. 열릴 때 이쪽으로 눌린다. */
    const val HINGE_RIGHT = true

    /** 활짝 열렸을 때 문짝이 남기는 폭의 비율. 0 이면 완전히 사라져 어색하다. */
    const val OPEN_MIN_W = 0.16f

    /** 백분율 → 화면 px */
    fun rectOf(g: RoomGeometry, r: Rect) = Rect(
        g.stage.left + r.left / 100f * g.stage.width,
        g.stage.top + r.top / 100f * g.stage.height,
        g.stage.left + r.right / 100f * g.stage.width,
        g.stage.top + r.bottom / 100f * g.stage.height,
    )

    /** 문을 눌렀는가. 문틀까지 받아준다 — 손가락은 정확하지 않다. */
    fun contains(g: RoomGeometry, pos: Offset): Boolean = rectOf(g, frame).contains(pos)

    /** 백분율 → 원본 PNG 픽셀 영역. 오려 그릴 때 소스가 된다. */
    fun sourcePx(room: ImageBitmap, r: Rect): IntRect = IntRect(
        (r.left / 100f * room.width).roundToInt(),
        (r.top / 100f * room.height).roundToInt(),
        (r.right / 100f * room.width).roundToInt(),
        (r.bottom / 100f * room.height).roundToInt(),
    )
}

/**
 * 문이 열리는 그림. [open] 0 이면 닫힘, 1 이면 활짝.
 *
 * 방 그림을 깔아둔 **뒤에**, 소품과 강아지보다 **앞서** 부른다. 문으로 든 볕이
 * 소품 밑으로 깔려야 하기 때문이다.
 *
 *  1. 문 너머 바깥 — 창유리를 오려 늘린다
 *  2. 문지방 볕과 인방 그늘 — 평평한 스티커로 안 보이게
 *  3. 눌린 문짝 — 같은 그림에서 오려 경첩 쪽으로 누른다
 *  4. 바닥에 번지는 볕 — 이게 "나간다"를 만든다
 */
fun DrawScope.drawDoorOpening(g: RoomGeometry, room: ImageBitmap, open: Float) {
    if (open <= 0.001f) return

    val dst = DoorSpec.rectOf(g, DoorSpec.leaf)
    val lift = DoorSpec.SHEAR * dst.height

    clipPath(doorPath(dst)) {
        // 1) 바깥. 창유리 한 칸을 문 비율로 늘린다
        val view = DoorSpec.sourcePx(room, DoorSpec.outside)
        drawImage(
            image = room,
            srcOffset = IntOffset(view.left, view.top),
            srcSize = IntSize(view.width, view.height),
            dstOffset = IntOffset(dst.left.roundToInt(), dst.top.roundToInt()),
            dstSize = IntSize(dst.width.roundToInt(), dst.height.roundToInt()),
            filterQuality = FilterQuality.None,
        )

        // 2) 나갈 땅. **이게 없으면 창문으로 보인다.**
        //
        // 창유리를 그대로 쓰면 아래쪽이 나뭇잎이라 문이 아니라 바닥까지 내려온
        // 창처럼 읽힌다. 아래를 잔디와 흙길로 덮어야 나갈 데가 생긴다.
        // 밑변이 기울어 있으므로 지평선도 같이 기운다.
        fun band(v0: Float, v1: Float, color: Color) {
            val y0 = dst.top + dst.height * v0
            val y1 = dst.top + dst.height * v1
            drawPath(
                Path().apply {
                    moveTo(dst.left, y0)
                    lineTo(dst.right, y0 + lift)
                    lineTo(dst.right, y1 + lift)
                    lineTo(dst.left, y1)
                    close()
                },
                color,
            )
        }
        band(HORIZON, HORIZON + 0.10f, GrassFar)
        band(HORIZON + 0.10f, HORIZON + 0.19f, GrassNear)
        band(HORIZON + 0.19f, 1.04f, PathSun)
        band(HORIZON + 0.19f, HORIZON + 0.215f, PathEdge)
    }

    // 3) 눌린 문짝. 잘라내기도 이미지와 **똑같이** 눌러야 한다. 눌린 사각형에
    // 아치를 새로 그리면 어깨 높이까지 따라 올라가 실제 문짝보다 위까지 열린다.
    val squeeze = 1f - open * (1f - DoorSpec.OPEN_MIN_W)
    val w = dst.width * squeeze
    val left = if (DoorSpec.HINGE_RIGHT) dst.right - w else dst.left
    val leafSrc = DoorSpec.sourcePx(room, DoorSpec.leaf)
    clipPath(doorPath(dst, squeeze)) {
        drawImage(
            image = room,
            srcOffset = IntOffset(leafSrc.left, leafSrc.top),
            srcSize = IntSize(leafSrc.width, leafSrc.height),
            dstOffset = IntOffset(left.roundToInt(), dst.top.roundToInt()),
            dstSize = IntSize(w.roundToInt().coerceAtLeast(1), dst.height.roundToInt()),
            filterQuality = FilterQuality.None,
        )
    }

    // 4) 바닥에 번지는 볕. 문간만 밝으면 스티커고, 빛이 바닥에 닿아야 바깥이 된다.
    // 바닥 윤곽으로 잘라야 벽이나 방 밖으로 새지 않는다.
    val threshold = Offset(dst.left + dst.width * 0.5f, dst.bottom + lift * 0.5f)
    val reach = g.stage.width * 0.3f
    clipPath(floorPath(g)) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Sunbeam.copy(alpha = 0.42f * open), Color.Transparent),
                center = threshold,
                radius = reach,
            ),
            radius = reach,
            center = threshold,
        )
    }
}

/**
 * 문짝 실루엣 — **기울어진 납작 아치**.
 *
 * 문을 네모로 다루면 두 군데서 벽이 딸려온다. 위 모서리(아치 바깥)와 오른쪽
 * 아래(밑변이 비스듬해서). 그 벽 조각이 칠해지거나 문짝과 함께 움직이면
 * 스티커가 벗겨지는 것처럼 보인다.
 *
 * 솟음은 폭의 절반이 아니라 [DoorSpec.ARCH_RISE] 다. 반원으로 그리면 문보다
 * 볼록해서 양옆으로 문 위까지 열린다.
 *
 * [squeeze] 는 열리며 눌린 가로 비율. 모양을 **원래 크기로 그린 다음** 경첩 쪽으로
 * x 만 줄인다 — 세로는 그대로다. 이미지도 그렇게 눌리므로 두 실루엣이 겹친다.
 */
private fun doorPath(r: Rect, squeeze: Float = 1f): Path {
    val rx = r.width / 2f
    val ry = r.height * DoorSpec.ARCH_RISE
    val spring = r.top + ry                       // 아치 어깨 높이
    fun lift(x: Float) = DoorSpec.SHEAR * r.height * (x - r.left) / r.width
    val hinge = if (DoorSpec.HINGE_RIGHT) r.right else r.left
    fun sx(x: Float) = hinge + (x - hinge) * squeeze

    return Path().apply {
        moveTo(sx(r.left), r.bottom + lift(r.left))
        lineTo(sx(r.left), spring + lift(r.left))
        val steps = 24
        for (i in 0..steps) {
            val a = PI * (1f - i.toFloat() / steps)   // 왼쪽 -> 오른쪽
            val x = r.left + rx + rx * cos(a).toFloat()
            lineTo(sx(x), spring - ry * sin(a).toFloat() + lift(x))
        }
        lineTo(sx(r.right), r.bottom + lift(r.right))
        close()
    }
}

/** 칠해진 바닥의 윤곽. 볕이 벽으로 새지 않게 자르는 데 쓴다. */
private fun floorPath(g: RoomGeometry): Path = Path().apply {
    FloorQuad.outline.forEachIndexed { i, p ->
        val x = g.stage.left + p.x / 100f * g.stage.width
        val y = g.stage.top + p.y / 100f * g.stage.height
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/**
 * 닫힌 문을 누를 수 있다는 은은한 표시. [pulse] 0..1.
 *
 * 문짝 위에 옅은 흰빛을 얹는다. 문이 열리기 시작하면 호출부에서 pulse 를 0 으로
 * 줄이므로 여기서 따로 끄지 않는다.
 */
fun DrawScope.drawDoorHint(g: RoomGeometry, pulse: Float) {
    if (pulse <= 0.001f) return
    // 여기도 문 모양이다. 네모로 얹으면 문 위 벽이 같이 밝아져서 티가 난다.
    drawPath(doorPath(DoorSpec.rectOf(g, DoorSpec.leaf)), Color.White.copy(alpha = 0.10f * pulse))
}

/**
 * 문 너머 지평선 위치. 문짝 높이 대비.
 *
 * 이 아래는 창유리 대신 땅을 깐다. 창유리만 쓰면 아래쪽이 나뭇잎이라
 * 바닥까지 내려온 창처럼 보인다.
 */
private const val HORIZON = 0.62f

/** 바깥 땅. 먼 잔디 → 가까운 잔디 → 볕 든 흙길 순으로 깔린다. */
private val GrassFar = Color(0xFF7E9A5C)
private val GrassNear = Color(0xFF93AF66)
private val PathEdge = Color(0xFFB8A176)
private val PathSun = Color(0xFFDCC69A)

/** 문으로 들어온 볕. 바닥에 번진다. */
private val Sunbeam = Color(0xFFFFE9B8)

/**
 * 발자국 도장. 방과 무관하게 아이콘·소품에서도 쓴다.
 *
 * [RoomSpec] 을 안 타므로 방 기하가 바뀌어도 영향이 없다.
 */
fun DrawScope.drawPawStamp(center: Offset, r: Float, color: Color) {
    drawOval(
        color,
        Offset(center.x - r * 0.62f, center.y - r * 0.12f),
        Size(r * 1.24f, r * 1.02f),
    )
    val toes = listOf(-0.74f to -0.7f, -0.26f to -1.02f, 0.26f to -1.02f, 0.74f to -0.7f)
    toes.forEach { (dx, dy) ->
        drawOval(
            color,
            Offset(center.x + dx * r - r * 0.18f, center.y + dy * r - r * 0.2f),
            Size(r * 0.37f, r * 0.45f),
        )
    }
}
