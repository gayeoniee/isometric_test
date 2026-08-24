package com.daengs.app.miniroom.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.daengs.app.miniroom.RoomGeometry
import com.daengs.app.miniroom.RoomSpec
import kotlin.math.roundToInt

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
    /** 문짝(초록 부분). 여닫는 대상이자 터치 판정 영역이다. */
    val leaf = Rect(left = 5.97f, top = 38.4f, right = 18.81f, bottom = 65.9f)

    /** 문틀까지 포함한 범위. 터치를 조금 너그럽게 받으려고 쓴다. */
    val frame = Rect(left = 4.3f, top = 36.4f, right = 20.1f, bottom = 67.7f)

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

    // 1) 문간. 안쪽이 비쳐야 문이 "열렸다"로 읽힌다
    drawRect(DoorwayDark, Offset(dst.left, dst.top), Size(dst.width, dst.height))

    // 2) 눌린 문짝. 경첩 쪽 가장자리는 제자리에 남는다
    val w = dst.width * (1f - open * (1f - DoorSpec.OPEN_MIN_W))
    val left = if (DoorSpec.HINGE_RIGHT) dst.right - w else dst.left
    drawImage(
        image = room,
        srcOffset = IntOffset(src.left.roundToInt(), src.top.roundToInt()),
        srcSize = IntSize(src.width.roundToInt(), src.height.roundToInt()),
        dstOffset = IntOffset(left.roundToInt(), dst.top.roundToInt()),
        dstSize = IntSize(w.roundToInt().coerceAtLeast(1), dst.height.roundToInt()),
        filterQuality = FilterQuality.None,
    )
}

/**
 * 닫힌 문을 누를 수 있다는 은은한 표시. [pulse] 0..1.
 *
 * 문짝 위에 옅은 흰빛을 얹는다. 문이 열리기 시작하면 호출부에서 pulse 를 0 으로
 * 줄이므로 여기서 따로 끄지 않는다.
 */
fun DrawScope.drawDoorHint(g: RoomGeometry, pulse: Float) {
    if (pulse <= 0.001f) return
    val r = DoorSpec.rectOf(g, DoorSpec.leaf)
    drawRect(
        Color.White.copy(alpha = 0.10f * pulse),
        Offset(r.left, r.top),
        Size(r.width, r.height),
    )
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
