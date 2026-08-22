package com.daengs.app.miniroom

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * 방의 색 테마.
 *
 * 벽·바닥·포인트 **세 가지 색만** 정하면 나머지(그늘·몰딩·걸레받이·문·울타리)는
 * 여기서 파생시킨다. 테마마다 15개씩 색을 손으로 고르면 톤이 어긋나기 쉽고,
 * 새 테마를 추가할 때도 번거롭다.
 *
 * 창밖 하늘·벚꽃은 테마를 따르지 않는다 — 바깥 풍경이라 방 색이 바뀌어도 그대로다.
 */
@Immutable
class RoomTheme(
    val id: String,
    val label: String,
    val wall: Color,
    val floor: Color,
    val accent: Color,
) {
    val wallLeft: Color = wall
    val wallRight: Color = darken(wall, 0.09f)
    val wallShadow: Color = darken(wall, 0.24f)
    val wallTrim: Color = lighten(wall, 0.74f)

    val floorLight: Color = floor
    val floorDark: Color = darken(floor, 0.16f)
    val floorPlank: Color = darken(floor, 0.28f)
    val floorEdge: Color = lighten(floor, 0.62f)

    val doorFill: Color = accent
    val doorTrim: Color = lighten(accent, 0.78f)
    val doorKnob: Color = darken(accent, 0.14f)

    val fenceFill: Color = lighten(accent, 0.22f)
    val fenceTrim: Color = lighten(accent, 0.80f)

    companion object {
        private fun lighten(c: Color, t: Float) = lerp(c, Color.White, t)
        private fun darken(c: Color, t: Float) = lerp(c, Color(0xFF6B4A3E), t)

        val Blossom = RoomTheme(
            id = "blossom",
            label = "벚꽃",
            wall = Color(0xFFF3D8D2),
            floor = Color(0xFFEAD5BE),
            accent = Color(0xFFF2B3B3),
        )
        val Mint = RoomTheme(
            id = "mint",
            label = "민트",
            wall = Color(0xFFD6E8DF),
            floor = Color(0xFFE7DCC8),
            accent = Color(0xFF9FCBB6),
        )
        val Lavender = RoomTheme(
            id = "lavender",
            label = "라벤더",
            wall = Color(0xFFE0D8EE),
            floor = Color(0xFFE9DFD2),
            accent = Color(0xFFB9A7D8),
        )
        val Sky = RoomTheme(
            id = "sky",
            label = "하늘",
            wall = Color(0xFFD4E3F0),
            floor = Color(0xFFE6DED0),
            accent = Color(0xFF9FC0DC),
        )
        val Butter = RoomTheme(
            id = "butter",
            label = "버터",
            wall = Color(0xFFF2E4C8),
            floor = Color(0xFFE8D6B6),
            accent = Color(0xFFEFC97E),
        )

        val ALL = listOf(Blossom, Mint, Lavender, Sky, Butter)
        val DEFAULT = Blossom

        fun byId(id: String): RoomTheme = ALL.firstOrNull { it.id == id } ?: DEFAULT
    }
}
