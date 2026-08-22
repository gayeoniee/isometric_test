package com.daengs.app.miniroom.art

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import com.daengs.app.R
import com.daengs.app.miniroom.RoomDefaults
import com.daengs.app.miniroom.sprite.SpriteSheet
import com.daengs.app.ui.theme.RoomPalette

@Immutable
class ItemCatalog(private val map: Map<String, ItemArt>) {
    operator fun get(itemId: String): ItemArt? = map[itemId]
    val ids: Set<String> get() = map.keys
}

/**
 * 아이템 카탈로그.
 *
 * **PNG 로 교체하는 법**: 아래에서 해당 줄의 [ItemArtSpec.Shapes] 를
 * [ItemArtSpec.Res] 로 바꾸고 resId 를 넣기만 하면 된다. ArtBox 만 그대로면
 * 정렬·터치판정·스냅·점유 계산은 한 줄도 안 바뀐다.
 *
 * ```
 * "plant" to ItemArtSpec.Shapes(PlantBox) { drawPlant() }
 * // →
 * "plant" to ItemArtSpec.Res(PlantBox, resId = R.drawable.plant_01)
 * ```
 *
 * 강아지는 이미 [ItemArtSpec.Sheet] 라서 `resId = 0` 을 진짜 시트로만 바꾸면 된다.
 */
val ItemSpecs: Map<String, ItemArtSpec> = mapOf(
    "rug" to ItemArtSpec.Shapes(
        ArtBox(size = Size(112f, 60f), anchor = Offset(56f, 30f), flat = true),
    ) { drawRug() },

    // anchor 는 "바닥에 닿는 점". 눕는 물건은 밑면 타원의 중심, 서는 물건은 맨 아랫점.
    "ball" to ItemArtSpec.Shapes(
        ArtBox(size = Size(24f, 30f), anchor = Offset(12f, 23f)),
    ) { drawBall() },

    "bowl" to ItemArtSpec.Shapes(
        ArtBox(size = Size(40f, 30f), anchor = Offset(20f, 17f)),
    ) { drawBowl() },

    "bed" to ItemArtSpec.Shapes(
        ArtBox(size = Size(78f, 48f), anchor = Offset(39f, 30f)),
    ) { drawBed() },

    "plant" to ItemArtSpec.Shapes(
        ArtBox(size = Size(44f, 96f), anchor = Offset(22f, 88f)),
    ) { drawPlant() },

    "bone" to ItemArtSpec.Shapes(
        ArtBox(size = Size(36f, 24f), anchor = Offset(18f, 13f)),
    ) { drawBone() },

    "house" to ItemArtSpec.Shapes(
        ArtBox(size = Size(72f, 80f), anchor = Offset(36f, 62f)),
    ) { drawHouse() },

    "toybox" to ItemArtSpec.Shapes(
        ArtBox(size = Size(52f, 56f), anchor = Offset(26f, 40f)),
    ) { drawToybox() },

    "waterbowl" to ItemArtSpec.Shapes(
        ArtBox(size = Size(40f, 30f), anchor = Offset(20f, 17f)),
    ) { drawWaterBowl() },

    "cushion" to ItemArtSpec.Shapes(
        ArtBox(size = Size(48f, 46f), anchor = Offset(24f, 30f)),
    ) { drawCushion() },

    "vase" to ItemArtSpec.Shapes(
        ArtBox(size = Size(36f, 46f), anchor = Offset(18f, 40f)),
    ) { drawVase() },

    "blanket" to ItemArtSpec.Shapes(
        ArtBox(size = Size(72f, 40f), anchor = Offset(36f, 20f), flat = true),
    ) { drawBlanket() },

    // --- 에셋 파이프라인 검증용 (실제 PNG 를 쓰는 유일한 아이템들) ---
    // crate 는 toybox 와 ArtBox 가 **완전히 동일**하다. 나란히 놓고 발밑이 어긋나면
    // PNG 경로에 문제가 있다는 뜻이다.
    "crate" to ItemArtSpec.Res(
        ArtBox(size = Size(52f, 56f), anchor = Offset(26f, 40f)),
        resId = R.drawable.crate_01,
    ),

    "lantern" to ItemArtSpec.Sheet(
        box = ArtBox(size = Size(36f, 48f), anchor = Offset(18f, 44f)),
        movable = true,
        resId = R.drawable.lantern_sheet,
        frameWidth = 144,
        frameHeight = 192,
        columns = 4,
        frameCount = 4,
        fps = 4,
    ) { _ ->
        // 시트가 정상이면 이 폴백은 절대 호출되지 않는다.
        // 호출되면(= 분홍 네모가 보이면) 리소스 해석이 실패한 것.
        drawRect(RoomPalette.GhostInvalid, size = Size(36f, 48f))
    },

    RoomDefaults.DOG_ID to ItemArtSpec.Sheet(
        box = ArtBox(size = Size(56f, 78f), anchor = Offset(28f, 74f), alwaysOnTop = true),
        movable = false,
        resId = 0, // 아직 에셋 없음 → fallback 사용
        frameWidth = 56,
        frameHeight = 78,
        columns = 4,
        frameCount = 8,
        fps = 6,
    ) { frame -> drawDog(frame) },
)

/** 사람이 읽는 이름. 나중에 아이템 목록 UI 에서 쓴다. */
val ItemLabels: Map<String, String> = mapOf(
    "rug" to "러그",
    "ball" to "공",
    "bowl" to "밥그릇",
    "bed" to "방석",
    "plant" to "화분",
    "bone" to "뼈다귀",
    "house" to "강아지집",
    "toybox" to "장난감함",
    "waterbowl" to "물그릇",
    "cushion" to "쿠션",
    "vase" to "꽃병",
    "blanket" to "담요",
    "crate" to "상자(PNG)",
    "lantern" to "가로등(시트)",
    RoomDefaults.DOG_ID to "강아지",
)

/**
 * 선언(spec)을 그리기 가능한 형태(art)로 해석한다.
 * 리소스 해석이 @Composable 이라 이 단계만 합성 안에 있고, 그리기 단계는 순수하다.
 */
@Composable
fun rememberItemCatalog(specs: Map<String, ItemArtSpec> = ItemSpecs): ItemCatalog {
    val resolved = LinkedHashMap<String, ItemArt>(specs.size)
    for ((id, spec) in specs) {
        resolved[id] = when (spec) {
            is ItemArtSpec.Shapes ->
                ItemArt.Shapes(spec.box, spec.movable, spec.draw)

            is ItemArtSpec.Res ->
                if (spec.resId != 0) {
                    ItemArt.Bitmap(
                        spec.box,
                        spec.movable,
                        ImageBitmap.imageResource(spec.resId),
                        spec.filterQuality,
                    )
                } else {
                    ItemArt.Shapes(spec.box, spec.movable) { }
                }

            is ItemArtSpec.Sheet ->
                ItemArt.Sheet(
                    box = spec.box,
                    movable = spec.movable,
                    sheet = if (spec.resId != 0) {
                        SpriteSheet(
                            image = ImageBitmap.imageResource(spec.resId),
                            frameWidth = spec.frameWidth,
                            frameHeight = spec.frameHeight,
                            columns = spec.columns,
                            frameCount = spec.frameCount,
                            fps = spec.fps,
                            filterQuality = spec.filterQuality,
                        )
                    } else {
                        null
                    },
                    frameCount = spec.frameCount,
                    fps = spec.fps,
                    fallback = spec.fallback,
                )
        }
    }
    return remember(specs) { ItemCatalog(resolved) }
}
