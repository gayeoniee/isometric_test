package com.daengs.app.miniroom.art

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntSize
import com.daengs.app.R
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

    // 소품 8종 — frankie516c/dog-training-rag 의 픽셀 아트 PNG 다.
    //
    // 크기는 저쪽 목업의 값에서 환산했다. 저쪽은 `width` 를 **스테이지 폭의 %** 로
    // 적고 높이를 `aspectRatio` 로 유도하는데, 우리 기본 단위가 방 PNG 픽셀이므로
    //   size.width  = width% x 1122
    //   size.height = size.width / aspectRatio
    //   anchor      = artAnchor% x size
    // 로 곧장 옮겨진다.
    //
    // footprint 는 저쪽 16 격자 기준이라 우리 12 에 맞춰 x0.75 했다.

    "rug-cream" to res(
        R.drawable.modular_rug_v1_final,
        w = 437.6f, h = 234.9f, anchorX = 0.50f, anchorY = 0.50f,
        cols = 5, rows = 5, flat = true,
    ),

    "rug-sage" to res(
        R.drawable.modular_rug_sage_v1_final,
        w = 437.6f, h = 235.5f, anchorX = 0.50f, anchorY = 0.50f,
        cols = 5, rows = 5, flat = true,
    ),

    "plant-tall" to res(
        R.drawable.modular_plant_v1_final,
        w = 101.0f, h = 162.8f, anchorX = 0.50f, anchorY = 0.93f,
        cols = 1, rows = 2,
    ),

    "doghouse-sage" to res(
        R.drawable.modular_doghouse_v1_final,
        w = 218.8f, h = 224.7f, anchorX = 0.50f, anchorY = 0.78f,
        cols = 2, rows = 3,
    ),

    "ball-sage" to res(
        R.drawable.modular_ball_v1_final,
        w = 53.9f, h = 55.5f, anchorX = 0.50f, anchorY = 0.94f,
        cols = 1, rows = 1,
    ),

    "cabinet-sage" to res(
        R.drawable.modular_cabinet_v1_final,
        w = 246.8f, h = 242.4f, anchorX = 0.50f, anchorY = 0.77f,
        cols = 4, rows = 2,
    ),

    "toy-basket" to res(
        R.drawable.modular_toy_basket_v1_final,
        w = 101.0f, h = 87.0f, anchorX = 0.50f, anchorY = 0.78f,
        cols = 2, rows = 2,
    ),

    "feeding-bowls" to res(
        R.drawable.modular_feeding_bowls_v1_final,
        w = 101.0f, h = 51.6f, anchorX = 0.50f, anchorY = 0.58f,
        cols = 2, rows = 1,
    ),

) + DogBreed.ALL.associate { it.id to dogSpec(it) }

/**
 * 소품 한 줄을 만든다.
 *
 * 값이 전부 PNG 에서 나오므로 인자가 곧 그림의 규격이다.
 * [anchorX]/[anchorY] 는 그림 안에서 **바닥에 닿는 점**의 비율(0..1)이다 —
 * 세워두는 물건은 아래쪽(0.8 언저리), 바닥에 눕는 러그는 한가운데(0.5)다.
 */
private fun res(
    @DrawableRes resId: Int,
    w: Float,
    h: Float,
    anchorX: Float,
    anchorY: Float,
    cols: Int,
    rows: Int,
    flat: Boolean = false,
) = ItemArtSpec.Res(
    box = ArtBox(
        footprint = IntSize(cols, rows),
        size = Size(w, h),
        anchor = Offset(w * anchorX, h * anchorY),
        flat = flat,
    ),
    resId = resId,
    // 픽셀 아트라 보간을 끈다. 기본값(Medium)이면 확대할 때 뿌옇게 번진다.
    filterQuality = FilterQuality.None,
)

/**
 * 강아지. 저쪽 4프레임 워크 시트를 그대로 쓴다.
 *
 * 시트는 2328x568 짜리 가로 4칸이라 한 프레임이 582x568 이다. 화면에서는 저쪽
 * `visualWidth 13.5%` 를 따라 1122 x 0.135 = 151.5 px 폭으로 그린다.
 *
 * 견종별 시트가 더 생기면 [DogBreed] 표에 줄을 추가하고 여기서 시트만 갈아끼우면 된다.
 */
private fun dogSpec(breed: DogBreed) = ItemArtSpec.Sheet(
    box = ArtBox(
        size = Size(DOG_W, DOG_H),
        anchor = Offset(DOG_W * 0.5f, DOG_H * 0.94f),
    ),
    movable = false,
    resId = breed.sheetRes,
    frameWidth = 582,
    frameHeight = 568,
    columns = 4,
    frameCount = 4,
    fps = 5,
    filterQuality = FilterQuality.None,
) { frame -> drawDogBreed(breed, frame) }

private const val DOG_W = 151.5f
private const val DOG_H = 147.8f

/** 사람이 읽는 이름. 나중에 아이템 목록 UI 에서 쓴다. */
val ItemLabels: Map<String, String> = mapOf(
    "rug-cream" to "크림 러그",
    "rug-sage" to "세이지 러그",
    "plant-tall" to "큰 화분",
    "doghouse-sage" to "강아지 집",
    "ball-sage" to "초록 공",
    "cabinet-sage" to "세이지 수납장",
    "toy-basket" to "장난감 바구니",
    "feeding-bowls" to "밥그릇 세트",
) + DogBreed.ALL.associate { it.id to it.label }

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
