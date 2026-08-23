package com.daengs.app.miniroom

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset

/**
 * 방에 놓인 아이템 하나.
 *
 * CONTEXT.md 4번 섹션대로 **격자 좌표만** 저장한다.
 * 화면 좌표는 매번 [RoomGeometry.footprintCenter] 로 계산하고,
 * 앞뒤 순서는 [depth] 로 결정되므로 저장하지 않는다.
 */
@Immutable
data class PlacedItem(
    val instanceId: Long,
    val itemId: String,
    val col: Int,
    val row: Int,
    /**
     * 바라보는 방향. 0 = 기본, 1 = 좌우 반전.
     *
     * 2D 아이소메트릭에서 "회전"은 이미지를 돌리는 게 아니라 **방향별 그림을
     * 따로 두는 것**이다. 화면에서 그냥 돌리면 바닥 평면을 벗어나 기울어 보인다.
     * 지금은 도형 아트라 좌우 반전(2방향)까지만 공짜로 된다.
     * 나중에 3D 프리렌더 PNG 를 쓰면 90도씩 4장을 뽑아 0..3 으로 늘리면 된다.
     */
    val facing: Int = 0,
) {
    /** 앞뒤 정렬 키. 화면 y 좌표가 아니라 col + row 다. */
    val depth: Int get() = col + row

    companion object {
        /** 지금 표현 가능한 방향 수. 도형 아트라 좌우 반전 2방향. */
        const val FACINGS = 2
    }
}

/** 드래그 중인 한 아이템의 임시 상태. 손을 떼면 사라진다. */
@Immutable
data class DragState(
    val instanceId: Long,
    val startCol: Int,
    val startRow: Int,
    val startPointer: Offset,
    val pointer: Offset,
    val targetCol: Int,
    val targetRow: Int,
    val valid: Boolean,
) {
    /** 손가락이 움직인 만큼. 스냅 전 자유 좌표를 그릴 때 쓴다. */
    val visualDelta: Offset get() = pointer - startPointer

    /** 드래그 중에는 "놓일 자리"의 깊이로 정렬해야 앞뒤가 실시간으로 바뀐다. */
    val targetDepth: Int get() = targetCol + targetRow
}

/**
 * 앞뒤 정렬 키. 1x1 이면 CONTEXT.md 4번대로 col + row 다.
 *
 * 다칸 아이템은 **가장 앞 칸** 기준이어야 한다. 2x1 침대를 col+row 로 재면
 * 자기 앞칸에 있는 물건보다 뒤로 밀려서 침대가 그 물건에 가려진다.
 * fw = fh = 1 이면 col + row 로 그대로 줄어들어 기존 동작은 안 바뀐다.
 */
fun depthKey(col: Int, row: Int, fw: Int, fh: Int): Int = (col + fw - 1) + (row + fh - 1)

object RoomDefaults {
    // 강아지 아트 키는 여기 없다 — 털색마다 하나씩이라
    // [com.daengs.app.miniroom.art.DogCoat.ALL] 이 유일한 출처다.

    /**
     * 보유 개수. 인벤토리에 남은 수 = 여기 수 - 방에 놓인 수 로 **계산**한다.
     * 별도 카운터를 들고 있으면 방 상태와 어긋날 여지가 생긴다.
     */
    val OWNED: Map<String, Int> = mapOf(
        "rug" to 1,
        "bed" to 1,
        "humanbed" to 1,
        "desk" to 1,
        "house" to 1,
        "bowl" to 1,
        "waterbowl" to 1,
        "plant" to 2,
        "lantern" to 2,
    )

    /** 인벤토리에 보여줄 순서. 강아지는 아이템이 아니므로 빠진다. */
    val INVENTORY_ORDER: List<String> = OWNED.keys.toList()

    /** 시안 배치를 대충 흉내 낸 초기 상태. 전부 하드코딩 — 백엔드 없음. */
    /** 가구만. 강아지는 격자를 안 쓰므로 [DogHerd] 가 따로 관리한다. */
    val STARTER_ROOM: List<PlacedItem> = listOf(
        PlacedItem(1L, "plant", 5, 0),
        PlacedItem(2L, "desk", 4, 1),
        PlacedItem(3L, "rug", 2, 2),
        PlacedItem(4L, "bed", 2, 2),
        PlacedItem(5L, "bowl", 4, 3),
        PlacedItem(6L, "waterbowl", 5, 3),
        PlacedItem(7L, "lantern", 0, 4),
        PlacedItem(8L, "humanbed", 0, 1),
    )

    /**
     * 방에 돌아다니는 강아지 수. 여기만 바꾸면 된다.
     * 지금은 견종 4종을 다 보려고 4마리 — 넘으면 앞에서부터 다시 쓴다.
     */
    const val DOG_COUNT = 4
}
