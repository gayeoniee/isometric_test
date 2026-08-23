package com.daengs.app.miniroom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import com.daengs.app.miniroom.art.ItemCatalog
import kotlin.math.floor

/** 화면 좌표를 아이템 아트 상자 안의 로컬 좌표(기본 단위)로 되돌린다. */
fun RoomGeometry.toArtLocal(p: Offset, item: PlacedItem, catalog: ItemCatalog): Offset? {
    val box = catalog[item.itemId]?.box ?: return null
    val c = footprintCenter(item.col, item.row, box.footprint)
    val left = c.x - box.anchor.x * scale
    val top = c.y - box.anchor.y * scale
    val local = Offset((p.x - left) / scale, (p.y - top) / scale)
    // 좌우 반전된 아이템은 터치 판정도 같이 뒤집어야 그림과 맞는다.
    return if (item.facing == 1) {
        Offset(2f * box.anchor.x - local.x, local.y)
    } else {
        local
    }
}

/**
 * 손가락 아래에서 **가장 앞에 있는** 아이템을 고른다.
 *
 * [toGrid] 로 발밑 타일만 보면 안 된다 — 화분처럼 키가 큰 아트는 자기 타일보다
 * 위로 삐져나오므로, 그림을 눌렀는데 뒤쪽 빈 칸이 잡힌다.
 * 그리기 순서의 **역순**으로 훑어서, 눈에 위에 있는 것이 먼저 잡히게 한다.
 */
fun List<PlacedItem>.pickTopmost(
    p: Offset,
    g: RoomGeometry,
    catalog: ItemCatalog,
): PlacedItem? = asReversed().firstOrNull { item ->
    val art = catalog[item.itemId] ?: return@firstOrNull false
    if (!art.movable) return@firstOrNull false
    val local = g.toArtLocal(p, item, catalog) ?: return@firstOrNull false
    art.box.touchArea.contains(local)
}

@Stable
class MiniRoomState internal constructor(initial: List<PlacedItem>) {

    val items: SnapshotStateList<PlacedItem> = initial.toMutableStateList()

    var drag: DragState? by mutableStateOf(null)
        private set

    /** 편집 모드에서 선택된 가구. 이 아이템 옆에 돌리기/치우기 버튼이 뜬다. */
    var selectedId: Long? by mutableStateOf(null)
        private set

    fun select(id: Long?) {
        selectedId = id
    }

    private var nextId: Long = (initial.maxOfOrNull { it.instanceId } ?: 0L) + 1L

    // -- 조회 -----------------------------------------------------------------

    /**
     * 그리기 순서. **반드시 draw 람다 안에서 호출할 것.**
     * 바깥에서 미리 계산해 캡처하면 상태 변화가 draw 를 무효화하지 않는다.
     *
     * 레이어를 먼저 가르고, 그 안에서 CONTEXT.md 4번 섹션대로 col + row 로 정렬한다.
     *
     * | 레이어 | 대상 | 이유 |
     * |---|---|---|
     * | 0 | 바닥 장식 (러그) | 두께가 0 이라 바닥에 선 물건을 가릴 수 없다. 앞칸에 있어도 뒤로 간다. |
     * | 1 | 보통 가구 | 여기서만 col+row 정렬이 의미가 있다. |
     * | 2 | 강아지 | 화면의 주인공. 무엇에도 가리지 않는다. |
     *
     * 레이어 0 을 따로 두지 않으면, 앞칸으로 옮긴 러그가 뒤칸의 공이나 강아지를
     * 덮어버린다 — 깊이 계산은 맞지만 눈에는 명백히 틀려 보인다.
     */
    fun drawOrder(catalog: ItemCatalog): List<PlacedItem> {
        val d = drag
        return items.sortedWith(
            compareBy(
                { layerOf(it, catalog) },
                { depthOf(it, catalog, d) },
            )
        )
    }

    private fun depthOf(item: PlacedItem, catalog: ItemCatalog, d: DragState?): Int {
        val fp = catalog[item.itemId]?.box?.footprint
        val col = if (d != null && item.instanceId == d.instanceId) d.targetCol else item.col
        val row = if (d != null && item.instanceId == d.instanceId) d.targetRow else item.row
        return depthKey(col, row, fp?.width ?: 1, fp?.height ?: 1)
    }

    private fun layerOf(item: PlacedItem, catalog: ItemCatalog): Int {
        val box = catalog[item.itemId]?.box ?: return LAYER_ITEM
        return when {
            box.alwaysOnTop -> LAYER_TOP
            box.flat -> LAYER_FLOOR
            else -> LAYER_ITEM
        }
    }

    /** 점유된 칸. 바닥에 깔린 아트(러그)는 칸을 막지 않는다. */
    fun occupiedCells(exclude: Long?, catalog: ItemCatalog): Set<IntOffset> {
        val out = HashSet<IntOffset>()
        for (item in items) {
            if (item.instanceId == exclude) continue
            val box = catalog[item.itemId]?.box ?: continue
            if (box.flat) continue
            for (dc in 0 until box.footprint.width) {
                for (dr in 0 until box.footprint.height) {
                    out += IntOffset(item.col + dc, item.row + dr)
                }
            }
        }
        return out
    }

    fun canPlace(itemId: String, col: Int, row: Int, ignore: Long?, catalog: ItemCatalog): Boolean {
        val box = catalog[itemId]?.box ?: return false
        if (col < 0 || row < 0) return false
        if (col + box.footprint.width > RoomSpec.GRID) return false
        if (row + box.footprint.height > RoomSpec.GRID) return false
        if (box.flat) return true
        val taken = occupiedCells(ignore, catalog)
        for (dc in 0 until box.footprint.width) {
            for (dr in 0 until box.footprint.height) {
                if (IntOffset(col + dc, row + dr) in taken) return false
            }
        }
        return true
    }

    // -- 드래그 ---------------------------------------------------------------

    fun beginDrag(pointer: Offset, g: RoomGeometry, catalog: ItemCatalog): Boolean {
        val hit = drawOrder(catalog).pickTopmost(pointer, g, catalog) ?: return false
        drag = DragState(
            instanceId = hit.instanceId,
            startCol = hit.col,
            startRow = hit.row,
            startPointer = pointer,
            pointer = pointer,
            targetCol = hit.col,
            targetRow = hit.row,
            valid = true,
        )
        return true
    }

    fun updateDrag(pointer: Offset, g: RoomGeometry, catalog: ItemCatalog) {
        val d = drag ?: return
        val item = items.firstOrNull { it.instanceId == d.instanceId } ?: return
        val box = catalog[item.itemId]?.box ?: return

        // 칸 단위 이동량으로 계산한다. 아이템 중심을 toGrid 에 넣는 방식보다
        // 발자국 크기·기준점에 휘둘리지 않고, 잡은 지점이 그대로 유지된다.
        val (c0, r0) = g.toGridF(d.startPointer)
        val (c1, r1) = g.toGridF(pointer)
        val dCol = floor(c1).toInt() - floor(c0).toInt()
        val dRow = floor(r1).toInt() - floor(r0).toInt()

        // 격자 밖으로 나가면 가장자리에 붙인다 — 유령 표시가 항상 "실제로 놓일 칸"이
        // 되므로, 손을 떼기 전에 결과가 눈에 보인다.
        val maxCol = RoomSpec.GRID - box.footprint.width
        val maxRow = RoomSpec.GRID - box.footprint.height
        val targetCol = (d.startCol + dCol).coerceIn(0, maxCol)
        val targetRow = (d.startRow + dRow).coerceIn(0, maxRow)

        drag = d.copy(
            pointer = pointer,
            targetCol = targetCol,
            targetRow = targetRow,
            valid = canPlace(item.itemId, targetCol, targetRow, item.instanceId, catalog),
        )
    }

    /** 놓기. 유효하면 스냅, 이미 찬 칸이면 원래 자리로 되돌린다. */
    fun endDrag(catalog: ItemCatalog) {
        val d = drag
        drag = null
        if (d == null) return
        if (!d.valid) return
        val i = items.indexOfFirst { it.instanceId == d.instanceId }
        if (i < 0) return
        // copy 로 통째 교체해야 SnapshotStateList 가 쓰기를 감지한다.
        items[i] = items[i].copy(col = d.targetCol, row = d.targetRow)
    }

    /**
     * 방향 돌리기. 지금은 좌우 반전 2방향뿐이라 누를 때마다 토글된다.
     * 방향별 그림이 늘어나면 여기 나머지 연산만 바꾸면 된다.
     */
    fun rotate(instanceId: Long) {
        val i = items.indexOfFirst { it.instanceId == instanceId }
        if (i < 0) return
        items[i] = items[i].copy(facing = (items[i].facing + 1) % PlacedItem.FACINGS)
    }

    fun cancelDrag() {
        drag = null
    }

    // -- 인벤토리 -------------------------------------------------------------

    /** 인벤토리에 남은 개수 = 보유 - 방에 놓인 수. 별도 카운터를 두지 않는다. */
    fun availableCount(itemId: String): Int =
        (RoomDefaults.OWNED[itemId] ?: 0) - items.count { it.itemId == itemId }

    /**
     * 새 아이템을 놓을 칸. **방 한가운데에서 가까운 빈 칸**을 고른다.
     *
     * 뒤쪽부터 채우면 (0,0) 이 화면상 강아지 바로 뒤라 새 아이템이 가려지고,
     * 앞쪽부터 채우면 인벤토리 패널에 덮인다. 가운데가 둘 다 피하는 자리다.
     */
    fun firstFreeCell(itemId: String, catalog: ItemCatalog): Pair<Int, Int>? {
        val box = catalog[itemId]?.box ?: return null
        val mid = (RoomSpec.GRID - 1) / 2f
        val cells = ArrayList<Pair<Int, Int>>()
        for (c in 0..(RoomSpec.GRID - box.footprint.width)) {
            for (r in 0..(RoomSpec.GRID - box.footprint.height)) cells += c to r
        }
        return cells
            .sortedWith(
                compareBy(
                    { (it.first - mid) * (it.first - mid) + (it.second - mid) * (it.second - mid) },
                    { it.first + it.second },
                )
            )
            .firstOrNull { canPlace(itemId, it.first, it.second, null, catalog) }
    }

    /** 인벤토리 → 방. 남은 개수가 없거나 빈 칸이 없으면 false. */
    fun placeFromInventory(itemId: String, catalog: ItemCatalog): Boolean {
        if (availableCount(itemId) <= 0) return false
        val (c, r) = firstFreeCell(itemId, catalog) ?: return false
        items += PlacedItem(nextId++, itemId, c, r)
        return true
    }

    /** 방 → 인벤토리. */
    fun returnToInventory(instanceId: Long) {
        items.removeAll { it.instanceId == instanceId }
        if (selectedId == instanceId) selectedId = null
    }

    // -- 편집 -----------------------------------------------------------------

    fun add(itemId: String, col: Int, row: Int, catalog: ItemCatalog): Boolean {
        if (!canPlace(itemId, col, row, null, catalog)) return false
        items += PlacedItem(nextId++, itemId, col, row)
        return true
    }

    fun remove(instanceId: Long) {
        items.removeAll { it.instanceId == instanceId }
    }

    companion object {
        const val LAYER_FLOOR = 0
        const val LAYER_ITEM = 1
        const val LAYER_TOP = 2

        val Saver = listSaver<MiniRoomState, List<Any>>(
            save = { state ->
                state.items.map { listOf(it.instanceId, it.itemId, it.col, it.row, it.facing) }
            },
            restore = { saved ->
                MiniRoomState(
                    saved.map {
                        PlacedItem(
                            it[0] as Long,
                            it[1] as String,
                            it[2] as Int,
                            it[3] as Int,
                            it[4] as Int,
                        )
                    }
                )
            },
        )
    }
}

/**
 * 저장 모델이 (itemId, col, row) 세 개의 원시값뿐이라 회전 시 복원이 사실상 공짜다.
 * 그래서 remember 가 아니라 rememberSaveable 을 쓴다 — 화면 돌렸다고 방이
 * 초기화되면 배치를 검증하는 데모로서 곤란하다.
 */
@Composable
fun rememberMiniRoomState(
    initial: List<PlacedItem> = RoomDefaults.STARTER_ROOM,
): MiniRoomState = rememberSaveable(saver = MiniRoomState.Saver) { MiniRoomState(initial) }
