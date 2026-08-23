package com.daengs.app.miniroom.art

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import com.daengs.app.ui.theme.RoomPalette
import kotlin.math.cos
import kotlin.math.sin

/**
 * 강아지 털색 한 벌.
 *
 * 마리마다 **덩치가 아니라 색으로** 구분한다. 덩치를 흔들면 같은 견종인데
 * 어떤 놈은 크고 어떤 놈은 작아 보여서 원근이 깨진 것처럼 읽힌다.
 *
 * 두 색([body], [shade])만 정하면 밝은 부분은 흰색 쪽으로 섞어 파생시킨다.
 * 그래서 색을 추가할 때 명암 관계가 어긋날 일이 없다 — [RoomTheme] 와 같은 원칙.
 *
 * [id] 는 그대로 카탈로그 키가 된다. `DogActor.artId` 가 이걸 가리키므로
 * 나중에 견종별 PNG 시트가 생기면 [ItemSpecs] 에서 해당 줄만 갈아끼우면 된다.
 */
@Immutable
data class DogCoat(
    val id: String,
    val label: String,
    val body: Color,
    val shade: Color,
) {
    /** 주둥이·가슴털·발목 폼폼처럼 빛을 받는 부분. */
    val light: Color = lerp(body, Color.White, 0.40f)

    companion object {
        /**
         * 파스텔 방에 얹어도 뜨지 않는 범위에서 고른 6색.
         * [com.daengs.app.miniroom.RoomDefaults.DOG_COUNT] 가 6을 넘으면 앞에서부터 다시 쓴다.
         */
        val ALL: List<DogCoat> = listOf(
            DogCoat("dog", "크림", Color(0xFFF2E0C4), Color(0xFFDCC29D)),
            DogCoat("dog_apricot", "애프리콧", Color(0xFFEEBE93), Color(0xFFD69C6C)),
            DogCoat("dog_choco", "초코", Color(0xFFB08468), Color(0xFF906A50)),
            DogCoat("dog_silver", "실버", Color(0xFFD2CDD6), Color(0xFFB3ADBB)),
            DogCoat("dog_white", "화이트", Color(0xFFFBF5EF), Color(0xFFE3D7CC)),
            DogCoat("dog_charcoal", "차콜", Color(0xFF7C7280), Color(0xFF635A68)),
        )

        val DEFAULT: DogCoat = ALL.first()
    }
}

/** 어느 털색이든 같은 코. 색까지 따라 밝히면 이목구비가 흐려진다. */
private val Nose = Color(0xFF3A2C24)

/**
 * 뽀글뽀글한 덩어리 하나.
 *
 * 타원을 하나 깔고 **테두리를 따라 작은 원을 둘러** 곱슬털을 만든다.
 * 푸들처럼 보이게 하는 건 사실상 이 실루엣 하나다 — 형태는 그대로 두고
 * 윤곽만 울퉁불퉁하게 만들면 짧은 털 강아지가 곱슬 강아지가 된다.
 */
private fun DrawScope.curly(
    color: Color,
    center: Offset,
    rx: Float,
    ry: Float,
    bumps: Int,
    bump: Float,
    phase: Float = 0f,
) {
    drawOval(color, Offset(center.x - rx, center.y - ry), Size(rx * 2f, ry * 2f))
    val step = (2f * Math.PI.toFloat()) / bumps
    for (i in 0 until bumps) {
        val t = phase + i * step
        drawCircle(color, bump, Offset(center.x + rx * cos(t), center.y + ry * sin(t)))
    }
}

/** 폼폼 (머리 위 상투·꼬리 끝·발목). 곱슬 덩어리의 동그란 버전. */
private fun DrawScope.pom(color: Color, center: Offset, r: Float) =
    curly(color, center, r, r, 7, r * 0.42f, 0.3f)

/**
 * 강아지 대기 동작 — 스프라이트 시트가 아직 없을 때 쓰는 폴백.
 *
 * 중요한 건 이게 **시트와 똑같은 프레임 인덱스를 받는다**는 점이다.
 * 나중에 `ItemArtSpec.Sheet.resId` 에 진짜 시트를 꽂으면 이 함수는 호출되지 않고,
 * 크기·기준점·앞뒤 정렬은 ArtBox 에서 오므로 그대로 맞는다.
 *
 * 좌표계: ArtBox 상자 56x78, 바닥에 닿는 점은 (28, 74).
 *
 * 푸들 특징을 형태로 박아둔 것 (견종이 바뀌면 여기만 손대면 된다):
 *  - 곱슬 실루엣 — 몸통·머리 테두리의 [curly]
 *  - 머리 위 상투 폼폼, 꼬리 끝 폼폼, 발목 폼폼
 *  - 길게 늘어진 복슬 귀
 *  - 좁고 긴 주둥이 (시안 강아지의 넓적한 얼굴 마스크는 뺐다)
 */
fun DrawScope.drawDog(frame: Int, coat: DogCoat = DogCoat.DEFAULT) {
    val f = frame % 8

    // 숨쉬기 — 몸이 위아래로 아주 조금
    val bob = floatArrayOf(0f, -0.6f, -1.2f, -0.6f, 0f, -0.6f, -1.2f, -0.6f)[f]
    // 꼬리 흔들기
    val tail = floatArrayOf(-16f, -4f, 8f, 16f, 8f, -4f, -16f, -22f)[f]
    // 귀 까딱 (가끔)
    val ear = if (f == 2 || f == 3) -5f else 0f

    drawOval(RoomPalette.Shadow, Offset(7f, 65f), Size(42f, 15f))

    translate(0f, bob) {
        // --- 꼬리 — 몸통보다 먼저 그려서 뒤로 간다 -----------------------------
        rotate(tail, Offset(44f, 54f)) {
            drawRoundRect(
                coat.shade,
                Offset(41.5f, 36f),
                Size(6f, 20f),
                CornerRadius(3f, 3f),
            )
            pom(coat.body, Offset(44.5f, 33f), 6.5f)
        }

        // --- 앉은 자세 몸통 ---------------------------------------------------
        curly(coat.body, Offset(28f, 55f), 19f, 17f, 12, 4.6f)
        // 배 밑 그늘. 곱슬 윤곽 안쪽에만 깔아서 실루엣을 흐리지 않는다.
        drawOval(coat.shade.copy(alpha = 0.45f), Offset(14f, 54f), Size(28f, 16f))

        // --- 앞다리 — 푸들 클립: 다리는 짧게 깎고 발목만 폼폼 -----------------
        drawRoundRect(coat.shade, Offset(17f, 56f), Size(7f, 15f), CornerRadius(3.5f, 3.5f))
        drawRoundRect(coat.shade, Offset(32f, 56f), Size(7f, 15f), CornerRadius(3.5f, 3.5f))
        pom(coat.light, Offset(20.5f, 69f), 5.2f)
        pom(coat.light, Offset(35.5f, 69f), 5.2f)

        // --- 가슴털 -----------------------------------------------------------
        curly(coat.light, Offset(28f, 47f), 12f, 10f, 9, 3.4f, 0.4f)

        // --- 귀 — 머리보다 먼저 그려야 머리가 귀 윗동을 덮는다 ----------------
        rotate(ear, Offset(28f, 22f)) {
            for (side in intArrayOf(-1, 1)) {
                val x = 28f + side * 14f
                drawCircle(coat.shade, 7.6f, Offset(x, 26f))
                drawCircle(coat.shade, 7.2f, Offset(x - side * 1.2f, 33f))
                drawCircle(coat.shade, 6.2f, Offset(x - side * 1.6f, 39.5f))
            }
        }

        // --- 머리 -------------------------------------------------------------
        curly(coat.body, Offset(28f, 25f), 13.5f, 13f, 11, 4.2f, 0.2f)
        // 머리 위 상투 — 푸들이라고 바로 읽히는 부분
        pom(coat.body, Offset(28f, 10.5f), 8f)

        // --- 주둥이 — 좁고 길게. 넓적하면 푸들이 아니라 시바가 된다 -----------
        curly(coat.light, Offset(28f, 34.5f), 7.5f, 6.5f, 7, 2.4f, 0.5f)

        // --- 이목구비 ---------------------------------------------------------
        drawCircle(Nose, 2.6f, Offset(22f, 23f))
        drawCircle(Nose, 2.6f, Offset(34f, 23f))
        drawCircle(RoomPalette.RugFill, 1f, Offset(22.8f, 22f))
        drawCircle(RoomPalette.RugFill, 1f, Offset(34.8f, 22f))

        drawOval(Nose, Offset(25.2f, 30f), Size(5.6f, 4f))
    }
}
