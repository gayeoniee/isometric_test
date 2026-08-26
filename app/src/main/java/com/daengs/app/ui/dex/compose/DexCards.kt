package com.daengs.app.ui.dex.compose

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// 카드 12장 — `assets/neo-hologram/cards.mjs` 를 옮긴 것
//
// **여기 필드가 적은 이유**: 카드 그림(webp)에 프레임·제목·기술명·수치가 전부 구워져
// 있다. 웹판도 그림 위에 포일만 얹지 글자를 그리지 않는다. 그래서 우리가 들고 있어야
// 할 건 그리드 아래 설명과, 어떤 포일을 쓸지뿐이다.
//
// 저쪽이 카드를 늘리면 여기에 줄만 더한다. `cards.mjs` 가 원본이다.
// ---------------------------------------------------------------------------

@Immutable
data class DexCard(
    val no: Int,
    val id: String,
    /** 그리드 설명에 쓰는 이름 */
    val name: String,
    /** 예: `CRUNCH 820` 의 앞부분 */
    val statLabel: String,
    val stat: Int,
    val foil: Foil,
    /** 카드 뒤 글로우 색. 저쪽 `accent` */
    val accent: Color,
) {
    /** `assets/` 안의 그림 경로 */
    val art: String get() = "neo-hologram/art/$id.webp"

    /** 그리드 설명 두 번째 줄. 저쪽은 `statLabel` 이 빈 카드가 하나 있다(토마토). */
    val statLine: String get() = if (statLabel.isBlank()) "$stat" else "$statLabel $stat"
}

/**
 * 카드 순서 = 도감 순서다. No.01 부터.
 *
 * No.01 배추만 저쪽에서 `immersive`(꾹 누르면 카드 안으로 들어가는 별개 뷰)인데,
 * 그건 CSS 723줄 + JS 524줄짜리 다른 물건이라 여기서는 [Foil.Prism] 으로 대신 둔다.
 * 옮길지 말지는 나머지가 자리를 잡은 뒤에 정한다.
 */
val DEX_CARDS: List<DexCard> = listOf(
    DexCard(1, "cabbage", "Cabbage Neo", "CRUNCH", 820, Foil.Prism, Color(0xFF8FD94A)),
    DexCard(2, "pepper", "Pepper Neo", "CRISP", 860, Foil.Prism, Color(0xFFFFD838)),
    DexCard(3, "eggplant", "Eggplant Neo", "GLOSS", 900, Foil.Crystal, Color(0xFFA86BFF)),
    DexCard(4, "carrot", "Carrot Neo", "SNAP", 830, Foil.Gold, Color(0xFFFF8A2B)),
    DexCard(5, "danhobak", "Danhobak Neo", "CRUNCH", 840, Foil.Oilslick, Color(0xFF7D9B46)),
    DexCard(6, "mushroom", "Mushroom Neo", "MYCELIUM MASH", 820, Foil.Sunburst, Color(0xFFCBB08A)),
    DexCard(7, "broccoli", "Broccoli Neo", "MYCELIUM MASH", 850, Foil.Holo, Color(0xFF7BBF3A)),
    DexCard(8, "cucumber", "Cucumber Neo", "MYCELIUM MASH", 810, Foil.Reverse, Color(0xFF4FAE52)),
    DexCard(9, "spinach", "Spinach Neo", "MYCELIUM MASH", 860, Foil.Aurora, Color(0xFF3F8F3F)),
    DexCard(10, "sweet-potato", "Sweet Potato Neo", "MYCELIUM MASH", 830, Foil.Cosmos, Color(0xFFA0656F)),
    DexCard(11, "tomato", "Tomato Neo", "", 840, Foil.Mosaic, Color(0xFFCC351A)),
    DexCard(12, "lettuce", "Lettuce Neo", "FRESH FLUTTER", 800, Foil.Metal, Color(0xFFB2D121)),
)
