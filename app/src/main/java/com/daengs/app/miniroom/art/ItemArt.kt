package com.daengs.app.miniroom.art

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize

/**
 * 아트 한 장의 규격. **기본 단위**(타일 = 64x32)로만 적는다 — 화면 px 아님.
 *
 * 렌더러·히트판정·드래그·정렬이 아이템에 대해 아는 것은 이 [ArtBox] 뿐이다.
 * 그래서 도형을 PNG 로 바꿔도 ArtBox 만 같으면 나머지 코드는 손댈 필요가 없다.
 */
@Immutable
data class ArtBox(
    /** 차지하는 타일 수 (width = cols, height = rows) */
    val footprint: IntSize = IntSize(1, 1),
    /** 아트 상자 크기 (기본 단위) */
    val size: Size,
    /** 상자 안에서 바닥 중심에 닿는 점 (기본 단위) */
    val anchor: Offset,
    /**
     * 바닥에 깔리는 아트인가 (러그 등).
     * true 면 칸을 점유하지 않아서 그 위에 다른 것을 놓을 수 있고,
     * 같은 depth 에서 항상 먼저(뒤에) 그려진다.
     */
    val flat: Boolean = false,
    /**
     * col+row 정렬을 무시하고 항상 맨 앞에 그린다.
     *
     * 강아지에만 쓴다. 강아지는 화면의 주인공이라 러그 가장자리나 가구에
     * 조금이라도 가리면 어색하다. 깊이 정렬은 가구끼리만 의미가 있다.
     */
    val alwaysOnTop: Boolean = false,
    /** 더 좁은 터치 판정 영역. null 이면 [size] 전체. */
    val hitRect: Rect? = null,
) {
    val touchArea: Rect get() = hitRect ?: Rect(Offset.Zero, size)
}

/**
 * 아이템의 시각 표현 **선언**. 순수 데이터 — Compose 런타임도, 해석된 리소스도 없다.
 *
 * 에셋이 생기면 [Shapes] → [Res] 로 한 줄만 바꾸면 된다. ArtBox 가 같으면
 * 정렬·히트판정·스냅·점유 계산은 전혀 바뀌지 않는다.
 */
@Immutable
sealed interface ItemArtSpec {
    val box: ArtBox
    val movable: Boolean

    /** 지금 쓰는 것 — Canvas 도형. frame 을 받아 애니메이션도 가능. */
    @Immutable
    data class Shapes(
        override val box: ArtBox,
        override val movable: Boolean = true,
        val draw: DrawScope.(frame: Int) -> Unit,
    ) : ItemArtSpec

    /** 정지 PNG. 에셋이 생기면 이걸로 교체. */
    @Immutable
    data class Res(
        override val box: ArtBox,
        override val movable: Boolean = true,
        @param:DrawableRes val resId: Int,
        /**
         * 축소 보간 방식. 소스를 기본단위의 4배로 저작하므로 화면에서는 **항상 축소**된다.
         * 부드러운 아트에 [FilterQuality.None](최근접 이웃)을 쓰면 계단·깜빡임이 생기고,
         * 반대로 픽셀 아트에 보간을 쓰면 뭉개진다. 그래서 아이템별로 고른다.
         */
        val filterQuality: FilterQuality = FilterQuality.Medium,
    ) : ItemArtSpec

    /** 스프라이트 시트 PNG. resId = 0 이면 아직 에셋이 없다는 뜻 → fallback 사용. */
    @Immutable
    data class Sheet(
        override val box: ArtBox,
        override val movable: Boolean = false,
        @param:DrawableRes val resId: Int,
        val frameWidth: Int,
        val frameHeight: Int,
        val columns: Int,
        val frameCount: Int,
        val fps: Int = 8,
        val filterQuality: FilterQuality = FilterQuality.Medium,
        val fallback: DrawScope.(frame: Int) -> Unit,
    ) : ItemArtSpec
}

/** 합성 시점에 리소스까지 해석된 형태. 그리기 단계가 소비하는 것. */
@Immutable
sealed interface ItemArt {
    val box: ArtBox
    val movable: Boolean

    @Immutable
    data class Shapes(
        override val box: ArtBox,
        override val movable: Boolean,
        val draw: DrawScope.(frame: Int) -> Unit,
    ) : ItemArt

    @Immutable
    data class Bitmap(
        override val box: ArtBox,
        override val movable: Boolean,
        val image: ImageBitmap,
        val filterQuality: FilterQuality = FilterQuality.Medium,
    ) : ItemArt

    @Immutable
    data class Sheet(
        override val box: ArtBox,
        override val movable: Boolean,
        /** null 이면 아직 에셋이 없다 → [fallback] 이 그린다. */
        val sheet: com.daengs.app.miniroom.sprite.SpriteSheet?,
        /** 시트가 없어도 폴백이 같은 타임라인을 타야 하므로 여기에도 들고 있는다. */
        val frameCount: Int,
        val fps: Int,
        val fallback: DrawScope.(frame: Int) -> Unit,
    ) : ItemArt
}
