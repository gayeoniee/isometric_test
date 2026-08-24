package com.daengs.app.miniroom

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.daengs.app.R

/**
 * 방 테마 — **그림 묶음 한 벌**이다.
 *
 * 예전에는 벽·바닥·문 색 세 개를 들고 있고 거기서 열두 색을 파생시켰다. 방을 코드로
 * 그리던 시절이라 색만 바꾸면 됐다.
 *
 * 이제 방과 소품이 전부 PNG 라서 색을 덧칠할 수 없다. 대신 저쪽에서
 * **테마마다 방·소품을 통째로 리컬러한 팩**을 만들어줬다. 나무결과 픽셀 셰이딩을
 * 살린 채 재질별로 칠한 것이라, 색을 곱하는 것보다 훨씬 낫다.
 *
 * 그래서 테마 = 그림 아홉 장의 묶음이다. 크기와 실루엣은 전부 원본과 같아서
 * 배치·충돌 정보([ArtBox])는 테마가 바뀌어도 그대로다.
 *
 * 테마를 추가하려면 그림 아홉 장을 받아 [ALL] 에 한 줄 넣으면 된다.
 */
@Immutable
data class RoomTheme(
    val id: String,
    val label: String,
    @DrawableRes val room: Int,
    @DrawableRes val rug: Int,
    @DrawableRes val rugCream: Int,
    @DrawableRes val plant: Int,
    @DrawableRes val doghouse: Int,
    @DrawableRes val ball: Int,
    @DrawableRes val cabinet: Int,
    @DrawableRes val basket: Int,
    @DrawableRes val bowls: Int,
    /** 인벤토리 미리보기용 색 세 개. 방 그림을 축소해 보여주는 것보다 알아보기 쉽다. */
    val swatchWall: Color,
    val swatchFloor: Color,
    val swatchAccent: Color,
) {
    /**
     * 아이템 id → 이 테마의 그림.
     *
     * 아이템 id 는 테마와 무관한 **물건의 이름**이다(`rug`, `doghouse`...). 색이
     * 들어간 이름을 쓰면 테마가 바뀔 때마다 id 가 달라져서 저장된 방이 깨진다.
     */
    @DrawableRes
    fun art(itemId: String): Int = when (itemId) {
        ItemIds.RUG -> rug
        ItemIds.RUG_CREAM -> rugCream
        ItemIds.PLANT -> plant
        ItemIds.DOGHOUSE -> doghouse
        ItemIds.BALL -> ball
        ItemIds.CABINET -> cabinet
        ItemIds.BASKET -> basket
        ItemIds.BOWLS -> bowls
        else -> 0
    }

    companion object {
        val CherryBlossom = RoomTheme(
            id = "cherry-blossom",
            label = "벚꽃",
            room = R.drawable.theme_cherry_blossom_room,
            rug = R.drawable.theme_cherry_blossom_rug,
            rugCream = R.drawable.theme_cherry_blossom_rug_cream,
            plant = R.drawable.theme_cherry_blossom_plant,
            doghouse = R.drawable.theme_cherry_blossom_doghouse,
            ball = R.drawable.theme_cherry_blossom_ball,
            cabinet = R.drawable.theme_cherry_blossom_cabinet,
            basket = R.drawable.theme_cherry_blossom_basket,
            bowls = R.drawable.theme_cherry_blossom_bowls,
            swatchWall = Color(0xFFF4D7DF),
            swatchFloor = Color(0xFFC9958E),
            swatchAccent = Color(0xFFA95E76),
        )

        val Mint = RoomTheme(
            id = "mint",
            label = "민트",
            room = R.drawable.theme_mint_room,
            rug = R.drawable.theme_mint_rug,
            rugCream = R.drawable.theme_mint_rug_cream,
            plant = R.drawable.theme_mint_plant,
            doghouse = R.drawable.theme_mint_doghouse,
            ball = R.drawable.theme_mint_ball,
            cabinet = R.drawable.theme_mint_cabinet,
            basket = R.drawable.theme_mint_basket,
            bowls = R.drawable.theme_mint_bowls,
            swatchWall = Color(0xFFD8EFE5),
            swatchFloor = Color(0xFFC4B69A),
            swatchAccent = Color(0xFF5F9D85),
        )

        val Lavender = RoomTheme(
            id = "lavender",
            label = "라벤더",
            room = R.drawable.theme_lavender_room,
            rug = R.drawable.theme_lavender_rug,
            rugCream = R.drawable.theme_lavender_rug_cream,
            plant = R.drawable.theme_lavender_plant,
            doghouse = R.drawable.theme_lavender_doghouse,
            ball = R.drawable.theme_lavender_ball,
            cabinet = R.drawable.theme_lavender_cabinet,
            basket = R.drawable.theme_lavender_basket,
            bowls = R.drawable.theme_lavender_bowls,
            swatchWall = Color(0xFFE7DCF2),
            swatchFloor = Color(0xFFB9A6B8),
            swatchAccent = Color(0xFF79629A),
        )

        val SkyBlue = RoomTheme(
            id = "sky-blue",
            label = "하늘",
            room = R.drawable.theme_sky_blue_room,
            rug = R.drawable.theme_sky_blue_rug,
            rugCream = R.drawable.theme_sky_blue_rug_cream,
            plant = R.drawable.theme_sky_blue_plant,
            doghouse = R.drawable.theme_sky_blue_doghouse,
            ball = R.drawable.theme_sky_blue_ball,
            cabinet = R.drawable.theme_sky_blue_cabinet,
            basket = R.drawable.theme_sky_blue_basket,
            bowls = R.drawable.theme_sky_blue_bowls,
            swatchWall = Color(0xFFDCEEF7),
            swatchFloor = Color(0xFFB6B8B1),
            swatchAccent = Color(0xFF4F83AA),
        )

        val Butter = RoomTheme(
            id = "butter",
            label = "버터",
            room = R.drawable.theme_butter_room,
            rug = R.drawable.theme_butter_rug,
            rugCream = R.drawable.theme_butter_rug_cream,
            plant = R.drawable.theme_butter_plant,
            doghouse = R.drawable.theme_butter_doghouse,
            ball = R.drawable.theme_butter_ball,
            cabinet = R.drawable.theme_butter_cabinet,
            basket = R.drawable.theme_butter_basket,
            bowls = R.drawable.theme_butter_bowls,
            swatchWall = Color(0xFFFFF0BD),
            swatchFloor = Color(0xFFD3A866),
            swatchAccent = Color(0xFFB68737),
        )

        val ALL: List<RoomTheme> = listOf(CherryBlossom, Mint, Lavender, SkyBlue, Butter)

        val DEFAULT: RoomTheme = CherryBlossom

        /** 저장된 id 로 되찾는다. 모르는 id 면 기본값 — 옛 저장값이 남아 있어도 안 깨진다. */
        fun byId(id: String): RoomTheme = ALL.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/**
 * 아이템 id.
 *
 * **색이 들어간 이름을 쓰지 않는다.** 예전에 `rug-cream`, `doghouse-sage` 처럼
 * 색을 붙였다가 테마를 넣으면서 이름과 실제 색이 어긋났다. id 는 저장 파일에
 * 그대로 들어가므로 물건의 이름이어야 한다.
 */
object ItemIds {
    const val RUG = "rug"
    const val RUG_CREAM = "rug_cream"
    const val PLANT = "plant"
    const val DOGHOUSE = "doghouse"
    const val BALL = "ball"
    const val CABINET = "cabinet"
    const val BASKET = "basket"
    const val BOWLS = "bowls"

    /** 인벤토리에 보여줄 순서. */
    val ALL = listOf(RUG, RUG_CREAM, DOGHOUSE, CABINET, BASKET, BOWLS, PLANT, BALL)
}
