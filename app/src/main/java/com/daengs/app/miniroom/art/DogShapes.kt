package com.daengs.app.miniroom.art

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.daengs.app.R

// ---------------------------------------------------------------------------
// 강아지 — **PNG 스프라이트 시트**
//
// 이 파일은 세 판본을 거쳤다. V1 파스텔 벡터, V2 도트(정수 격자에 칸 찍기),
// V3 잉크 만화. 셋 다 도형을 코드로 그렸고, 그래서 매번 화풍을 통째로 다시 짜야 했다.
//
// 이제 그리지 않는다. frankie516c/dog-training-rag 의 워크 시트를 그대로 쓴다.
// 남은 것은 **어느 시트를 쓸지 고르는 표** 하나뿐이다.
//
// 견종 축(털결·귀·꼬리)은 없앴다. 도형을 조립할 때나 필요하던 것이고, 시트를
// 쓰는 지금은 견종 = 시트 한 장이다. 저쪽에서 견종이 늘어나면 아래 표에 줄을
// 하나 추가하면 된다.
// ---------------------------------------------------------------------------

/**
 * 강아지 털색 한 벌.
 *
 * 시트가 이미 색까지 그려져 있어서 지금은 아무 데도 안 쓴다. 남겨둔 이유는
 * **사진에서 뽑은 색을 입히는 기능**이 CONTEXT.md 11번에 남아 있기 때문이다 —
 * 닮아 보이는 데 제일 크게 기여하는 게 털색이고, 털색은 모델이 필요 없다.
 * 그때 시트에 색조를 얹는 입구가 이 타입이 된다.
 */
@Immutable
data class DogCoat(
    val label: String,
    val body: Color,
) {
    companion object {
        val CREAM = DogCoat("크림", Color(0xFFF7E7CC))
        val APRICOT = DogCoat("애프리콧", Color(0xFFEFC098))
        val CHOCO = DogCoat("초코", Color(0xFFB5876A))
        val WHITE = DogCoat("화이트", Color(0xFFFFFCF8))

        val ALL: List<DogCoat> = listOf(CREAM, APRICOT, CHOCO, WHITE)
    }
}

/**
 * 견종 하나 = 워크 시트 한 장.
 *
 * [id] 가 그대로 카탈로그 키다 ([ItemCatalog] 가 `DogBreed.ALL` 로 자동 등록한다).
 * 저쪽에서 시트가 오는 대로 줄만 추가하면 된다 — 시트 규격(가로 4프레임)은 같다.
 */
@Immutable
enum class DogBreed(
    val id: String,
    val label: String,
    @DrawableRes val sheetRes: Int,
    val coat: DogCoat,
) {
    TOY_POODLE(
        "dog_toy_poodle",
        "크림 토이푸들",
        R.drawable.modular_dog_poodle_walk_stable_v2,
        DogCoat.CREAM,
    );

    companion object {
        val ALL: List<DogBreed> = entries

        private val index = entries.associateBy { it.id }

        /** 아트 키 → 견종. 렌더러가 `DogActor.breed.id` 로 되찾을 때 쓴다. */
        fun byId(id: String): DogBreed? = index[id]
    }
}

/**
 * 강아지 한 마리의 **지금 자세**. 리깅에 넘기는 유일한 입력이다.
 *
 * 시트를 쓰든 도형을 그리든 이 값만 받으면 되므로, 아트를 갈아끼워도
 * 움직이는 코드는 그대로다.
 */
@Immutable
data class DogPose(
    /** 걸음 위상(라디안). 이동 거리에 비례해 늘어나므로 속도와 자동으로 맞는다. */
    val phase: Float = 0f,
    /**
     * 0 = 서 있음, 1 = 걷는 중.
     *
     * **불리언이 아니라 실수인 이유**: 걷기 시작·멈춤이 툭 끊기면 몸이 순간이동한다.
     * [com.daengs.app.miniroom.DogHerd] 가 목표값으로 부드럽게 몰아준다.
     */
    val stand: Float = 0f,
) {
    companion object {
        /** 가만히 있는 자세. 미리보기·인벤토리 아이콘이 쓴다. */
        val Idle = DogPose()
    }
}

/**
 * 시트를 못 읽었을 때의 폴백.
 *
 * 리소스가 정상이면 절대 안 불린다. 불렸다는 건 시트가 빠졌다는 뜻이므로
 * **눈에 띄는 분홍 상자**를 그린다 — 조용히 아무것도 안 그리면 원인을 못 찾는다.
 * ([ItemCatalog] 의 무드등 폴백과 같은 방식이다.)
 */
fun DrawScope.drawDogBreed(
    breed: DogBreed,
    frame: Int,
    coat: DogCoat = breed.coat,
    pose: DogPose = DogPose.Idle,
) {
    drawRect(com.daengs.app.ui.theme.RoomPalette.GhostInvalid, Offset.Zero, Size(151.5f, 147.8f))
}
