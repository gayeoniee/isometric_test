package com.daengs.app.miniroom

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.daengs.app.miniroom.art.ArtBox
import com.daengs.app.miniroom.art.ItemArt
import com.daengs.app.miniroom.art.ItemCatalog
import com.daengs.app.miniroom.art.footprintFacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 다칸 아이템을 돌리면 **발자국도 같이 돈다.**
 *
 * 아이소메트릭에서 좌우 반전은 두 격자 축을 맞바꾸는 것과 같아서, 2x1 침대를 돌리면
 * 그림은 세로로 눕는다. 발자국을 안 돌리면 격자는 여전히 가로 2칸을 먹어서
 * **보이는 것과 점유가 어긋난다** — 실기기에서 바로 걸렸던 버그라 여기서 고정한다.
 */
class FootprintFacingTest {

    private val wide = ArtBox(footprint = IntSize(2, 1), size = Size(104f, 98f), anchor = Offset(52f, 66f))
    private val one = ArtBox(size = Size(40f, 30f), anchor = Offset(20f, 17f))
    private val rug = ArtBox(size = Size(112f, 60f), anchor = Offset(56f, 30f), flat = true)

    private val catalog = ItemCatalog(
        mapOf(
            "humanbed" to ItemArt.Shapes(wide, movable = true) {},
            "bowl" to ItemArt.Shapes(one, movable = true) {},
            "rug" to ItemArt.Shapes(rug, movable = true) {},
        )
    )

    @Test
    fun `돌리면 가로세로가 맞바뀐다`() {
        assertEquals(IntSize(2, 1), wide.footprintFacing(0))
        assertEquals(IntSize(1, 2), wide.footprintFacing(1))
    }

    @Test
    fun `정사각은 돌려도 그대로`() {
        assertEquals(IntSize(1, 1), one.footprintFacing(0))
        assertEquals(IntSize(1, 1), one.footprintFacing(1))
    }

    @Test
    fun `돌린 침대는 세로 두 칸을 먹는다`() {
        val state = MiniRoomState(listOf(PlacedItem(1L, "humanbed", 1, 1, facing = 1)))
        val taken = state.occupiedCells(exclude = null, catalog = catalog)
        assertEquals(setOf(IntOffset(1, 1), IntOffset(1, 2)), taken)
    }

    @Test
    fun `돌린 침대 옆칸은 비어 있다`() {
        val state = MiniRoomState(listOf(PlacedItem(1L, "humanbed", 1, 1, facing = 1)))
        // 예전에는 (2,1) 이 막혀 있고 (1,2) 가 비어 있었다 — 정확히 반대였다
        assertTrue(state.canPlace("bowl", 2, 1, null, catalog))
        assertFalse(state.canPlace("bowl", 1, 2, null, catalog))
    }

    @Test
    fun `가장자리에서 돌리면 안쪽으로 밀어 넣는다`() {
        // 마지막 행에 가로로 누운 침대. 그대로 돌리면 세로 2칸이 격자를 넘는다.
        val state = MiniRoomState(listOf(PlacedItem(1L, "humanbed", 0, RoomSpec.GRID - 1, facing = 0)))
        assertTrue(state.rotate(1L, catalog))
        val bed = state.items.first()
        assertEquals(1, bed.facing)
        assertEquals(RoomSpec.GRID - 2, bed.row)
    }

    @Test
    fun `돌린 자리에 다른 물건이 있으면 돌지 않는다`() {
        val state = MiniRoomState(
            listOf(
                PlacedItem(1L, "humanbed", 1, 1, facing = 0),
                PlacedItem(2L, "bowl", 1, 2),
            )
        )
        assertFalse(state.rotate(1L, catalog))
        assertEquals(0, state.items.first().facing)
    }

    @Test
    fun `바닥에 깔린 아트는 돌려도 칸을 안 먹는다`() {
        val state = MiniRoomState(listOf(PlacedItem(1L, "rug", 2, 2, facing = 1)))
        assertTrue(state.occupiedCells(null, catalog).isEmpty())
    }
}
