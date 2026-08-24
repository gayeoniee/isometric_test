package com.daengs.app.miniroom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 저장 형식이 깨지면 사용자가 꾸민 방이 통째로 날아간다.
 * 되돌리기가 안 되는 종류의 버그라 여기서 고정해둔다.
 */
class RoomCodecTest {

    private val sample = listOf(
        PlacedItem(1L, "rug", 2, 2, 0),
        PlacedItem(2L, "humanbed", 0, 1, 1),
    )

    @Test
    fun `쓰고 읽으면 그대로 돌아온다`() {
        assertEquals(sample, RoomCodec.decode(RoomCodec.encode(sample)))
    }

    @Test
    fun `빈 방도 왕복된다`() {
        assertEquals(emptyList<PlacedItem>(), RoomCodec.decode(RoomCodec.encode(emptyList())))
    }

    @Test
    fun `저장된 게 없으면 null`() {
        assertNull(RoomCodec.decode(null))
        assertNull(RoomCodec.decode(""))
        assertNull(RoomCodec.decode("   "))
    }

    @Test
    fun `버전이 다르면 통째로 버린다`() {
        val v1 = RoomCodec.encode(sample)
        assertNull("옛 형식을 새 코드로 잘못 해석하면 안 된다", RoomCodec.decode(v1.replace("v3", "v2")))
        assertNull(RoomCodec.decode("1,rug,2,2,0"))
    }

    @Test
    fun `망가진 줄이 하나라도 있으면 전부 버린다`() {
        assertNull("필드 수가 모자람", RoomCodec.decode("v3;1,rug,2,2"))
        assertNull("숫자가 아님", RoomCodec.decode("v3;x,rug,2,2,0"))
        assertNull("itemId 가 빔", RoomCodec.decode("v3;1,,2,2,0"))
    }

    @Test
    fun `격자 밖 좌표는 버린다`() {
        // 격자 크기를 줄이는 변경이 있었을 때 옛 데이터가 밖에 남는 경우 (GRID=12)
        assertNull(RoomCodec.decode("v3;1,rug,12,2,0"))
        assertNull(RoomCodec.decode("v3;1,rug,2,-1,0"))
    }

    @Test
    fun `방향 값이 범위를 넘으면 잘라 넣는다`() {
        // 방향 수가 줄어든 경우. 이건 좌표와 달리 방을 못 쓰게 만들지 않으므로 살린다.
        val r = RoomCodec.decode("v3;1,rug,2,2,7")!!
        assertEquals(PlacedItem.FACINGS - 1, r.single().facing)
    }

    @Test
    fun `아이템 id 에 구분자가 없어야 한다`() {
        // 카탈로그 id 규칙(소문자·숫자·밑줄)이 지켜지는지 확인 — 깨지면 저장이 망가진다
        val bad = com.daengs.app.miniroom.art.ItemSpecs.keys.filter { it.contains(',') || it.contains(';') }
        assertEquals(emptyList<String>(), bad)
    }
}
