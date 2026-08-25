package com.daengs.app.miniroom.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
     * 문짝(초록 부분)을 감싸는 사각형. 여닫는 대상이자 터치 판정 영역이다.
     *
     * **사각형은 문짝보다 크다.** 문이 아치라 위 모서리가 남고, 밑변이 비스듬해
     * 오른쪽 아래도 남는다. 남는 자리는 벽이므로 그릴 때는 [SHEAR] 를 반영한
     * 아치로 잘라내야 한다 — 안 자르면 벽 조각이 문짝에 딸려 움직인다.
     */
    val leaf = Rect(left = 6.77f, top = 39.02f, right = 17.83f, bottom = 66.05f)

    /** 문틀까지 포함한 범위. 터치를 조금 너그럽게 받으려고 쓴다. */
    val frame = Rect(left = 5.2f, top = 37.0f, right = 19.3f, bottom = 67.5f)

    /**
     * 문이 붙은 벽이 뒤로 물러나면서 **오른쪽이 올라간 정도**. 문짝 높이 대비 비율.
     *
     * 그림에서 문 밑변을 재보면 왼쪽 66.05%, 오른쪽 62.77% 로 3.28%p 차이가 난다.
     * 아치 윗변도 같은 만큼 기울어 있다 — 즉 대칭 아치를 세로로 밀어놓은 모양이다.
     * 이 값을 빼먹으면 문짝 오른쪽 아래에 벽 삼각형이 딸려나온다.
     */
    const val SHEAR = -0.121f

    /**
     * 경첩이 어느 쪽인가. 그림에서 손잡이가 **왼쪽**에 있으므로 경첩은 오른쪽이다.
     * 열릴 때 문짝이 이쪽으로 눌린다.
     */
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

    /** 원본 PNG 안에서 문짝이 차지하는 픽셀 영역. 오려 그릴 때 소스가 된다. */
    fun leafSourcePx(room: ImageBitmap): Rect = Rect(
        leaf.left / 100f * room.width,
        leaf.top / 100f * room.height,
        leaf.right / 100f * room.width,
        leaf.bottom / 100f * room.height,
    )
}

/**
 * 문이 열리는 그림. [open] 0 이면 닫힘, 1 이면 활짝.
 *
 * 방 그림을 이미 깔아둔 **뒤에** 부른다. 순서는 이렇다.
 *
 *  1. 문짝 자리를 어두운 색으로 덮는다 — 열린 틈으로 보이는 문간이다
 *  2. 같은 PNG 의 문짝 영역을 경첩 쪽으로 눌러서 다시 그린다
 *
 * 새 아트가 없어도 원래 화풍 그대로 열린다. 나중에 저쪽에서 "문 열린 방" PNG 를
 * 받으면 이 함수를 그 그림으로 갈아끼우면 된다.
 */
fun DrawScope.drawDoorOpening(g: RoomGeometry, room: ImageBitmap, open: Float) {
    if (open <= 0.001f) return

    val dst = DoorSpec.rectOf(g, DoorSpec.leaf)
    val src = DoorSpec.leafSourcePx(room)

    // 1) 문간. 안쪽이 비쳐야 문이 "열렸다"로 읽힌다.
    //
    // **문 모양대로** 칠해야 한다. 네모로 칠했더니 문짝 위 모서리의 벽까지 덮여서
    // 검은 사각형이 붙은 꼴이 됐다 — 스티커가 벗겨진 것처럼 보였다.
    drawPath(doorPath(dst), DoorwayDark)

    // 2) 눌린 문짝. 경첩 쪽 가장자리는 제자리에 남는다.
    //
    // 잘라내기도 **이미지와 똑같이** 눌러야 한다. 눌린 사각형에 아치를 새로 그리면
    // 아치가 시작되는 높이가 폭을 따라 올라가버려서, 실제 문짝보다 위까지 열린다.
    // 그 틈으로 벽이 비친다.
    val squeeze = 1f - open * (1f - DoorSpec.OPEN_MIN_W)
    val w = dst.width * squeeze
    val left = if (DoorSpec.HINGE_RIGHT) dst.right - w else dst.left
    clipPath(doorPath(dst, squeeze)) {
        drawImage(
            image = room,
            srcOffset = IntOffset(src.left.roundToInt(), src.top.roundToInt()),
            srcSize = IntSize(src.width.roundToInt(), src.height.roundToInt()),
            dstOffset = IntOffset(left.roundToInt(), dst.top.roundToInt()),
            dstSize = IntSize(w.roundToInt().coerceAtLeast(1), dst.height.roundToInt()),
            filterQuality = FilterQuality.None,
        )
    }
}

/**
 * 문짝 실루엣 — **기울어진 아치**. 반원 천장에 비스듬한 밑변.
 *
 * 문을 네모로 다루면 두 군데서 벽이 딸려온다. 위 모서리(아치 바깥)와 오른쪽
 * 아래(밑변이 비스듬해서). 그 벽 조각이 검게 칠해지거나 문짝과 함께 움직이면
 * 스티커가 벗겨지는 것처럼 보인다.
 *
 * [squeeze] 는 문이 열리며 눌린 가로 비율이다. 모양을 **원래 크기로 그린 다음**
 * 경첩 쪽으로 x 만 줄인다 — 세로는 건드리지 않는다. 이미지도 똑같이 눌리므로
 * 두 실루엣이 정확히 겹친다. 눌린 사각형에 아치를 새로 그리면 아치가 시작되는
 * 높이까지 따라 올라가서 어긋난다.
 */
private fun doorPath(r: Rect, squeeze: Float = 1f): Path {
    val radius = r.width / 2f
    val spring = r.top + radius          // 아치가 시작되는 높이
    // 벽이 물러나며 생긴 세로 밀림. 오른쪽으로 갈수록 올라간다
    fun lift(x: Float) = DoorSpec.SHEAR * r.height * (x - r.left) / r.width
    val hinge = if (DoorSpec.HINGE_RIGHT) r.right else r.left
    fun squeezed(x: Float) = hinge + (x - hinge) * squeeze

    return Path().apply {
        moveTo(squeezed(r.left), r.bottom + lift(r.left))
        lineTo(squeezed(r.left), spring + lift(r.left))
        val steps = 24
        for (i in 0..steps) {
            val a = PI * (1f - i.toFloat() / steps)      // 왼쪽 → 오른쪽
            val x = r.left + radius + radius * cos(a).toFloat()
            lineTo(squeezed(x), spring - radius * sin(a).toFloat() + lift(x))
        }
        lineTo(squeezed(r.right), r.bottom + lift(r.right))
        close()
    }
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

/** 열린 문 너머. 방 그림의 문틀 그늘과 비슷한 톤이라 튀지 않는다. */
private val DoorwayDark = Color(0xFF4A3B2A)

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
