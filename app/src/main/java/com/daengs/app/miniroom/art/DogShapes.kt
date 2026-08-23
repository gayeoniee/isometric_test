package com.daengs.app.miniroom.art

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.daengs.app.ui.theme.RoomPalette
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// ---------------------------------------------------------------------------
// 강아지 아트는 **축 두 개의 조합**이다.
//
//   견종([DogBreed]) = 형태   ·   털색([DogCoat]) = 색
//
// 견종마다 그림을 통째로 짜지 않는다. 털결·귀·꼬리·머리장식을 축으로 나눠두고
// 견종은 그 축의 값을 고르기만 한다. 그래서 견종 하나 추가가 **표에 한 줄**이다.
// (CONTEXT.md 11번 "파츠 축" 설계를 코드로 옮긴 것)
//
// ## 귀여움은 비율에서 나온다 (Kindchenschema)
//
// 처음엔 "머리 반경 13.5 / 눈 반경 2.6 / 눈이 얼굴 중앙보다 **위**" 로 그렸다가
// "너무 못생겼다"고 걸렸다. 아기 도식(baby schema)의 정반대였다. 지금 규격:
//
// | | 값 | 왜 |
// |---|---|---|
// | 등신 | **2.2** (머리 지름 29 / 전체 ~64) | 치비 표준 2~2.5 |
// | 눈 크기 | 머리 높이의 **20%** | 교과서는 1/4 이상이지만 실기기에서 부담스러웠다 |
// | 눈 위치 | 머리 위에서 **60%** 지점 (= 아래쪽) | 이게 제일 크게 먹힌다 |
// | 주둥이 | 작게, **낮게** | 길고 높으면 어른 개가 된다 |
// | 볼터치·입 | 있음 | 싸고 확실한 귀여움 |
//
// 좌표계: ArtBox 상자 56x78, 바닥에 닿는 점은 (28, 74).
// ---------------------------------------------------------------------------

/**
 * 강아지 털색 한 벌.
 *
 * 두 색([body], [shade])만 정하면 밝은 부분은 흰색 쪽으로 섞어 파생시킨다.
 * 그래서 색을 추가할 때 명암 관계가 어긋날 일이 없다 — [RoomTheme] 와 같은 원칙.
 *
 * **나중에 사진에서 뽑은 색을 그대로 넣을 자리다** (CONTEXT.md 11번:
 * 닮아 보이는 데 제일 크게 기여하는 건 털색이고, 털색은 모델이 필요 없다).
 */
@Immutable
data class DogCoat(
    val label: String,
    val body: Color,
    val shade: Color,
) {
    /** 주둥이·가슴털처럼 빛을 받는 부분. */
    val light: Color = lerp(body, Color.White, 0.40f)

    /**
     * 귀처럼 **몸에서 살짝만 떨어져 보이면 되는** 부분.
     *
     * [shade] 를 그대로 쓰면 흰 강아지(말티즈)의 귀가 잿빛으로 떠서 지저분해 보인다.
     * 삼색 무늬가 있는 견종(비글)만 예외로 [shade] 를 쓴다 — 거긴 진짜로 색이 다르다.
     */
    val soft: Color = lerp(body, shade, 0.5f)

    companion object {
        val CREAM = DogCoat("크림", Color(0xFFF7E7CC), Color(0xFFE4CBA6))
        val APRICOT = DogCoat("애프리콧", Color(0xFFEFC098), Color(0xFFD79E70))
        val CHOCO = DogCoat("초코", Color(0xFFB5876A), Color(0xFF8E6850))
        val SILVER = DogCoat("실버", Color(0xFFD6D1DA), Color(0xFFB6B0BE))
        val WHITE = DogCoat("화이트", Color(0xFFFDF8F3), Color(0xFFEFE4DA))
        val CHARCOAL = DogCoat("차콜", Color(0xFF837988), Color(0xFF6A616F))

        val ALL: List<DogCoat> = listOf(CREAM, APRICOT, CHOCO, SILVER, WHITE, CHARCOAL)
    }
}

/** 털결 — 실루엣의 성격. 견종을 **가장 멀리서부터** 구분해주는 축. */
enum class Fur { CURLY, SMOOTH, SILKY, FLUFFY }

/** 귀. **가까이서 견종을 가르는 건 사실상 이것 하나다.** */
enum class Ear { DROP_ROUND, DROP_LONG, DROP_SILKY, PRICK }

/** 꼬리. */
enum class Tail { POM, UP_TIP, PLUME }

/** 머리 장식. */
enum class Crown { TOPKNOT, RUFF, BOW, NONE }

/**
 * 견종 하나 = 축의 조합.
 *
 * [id] 가 그대로 카탈로그 키다. 나중에 견종별 PNG 시트가 생기면
 * [ItemSpecs] 에서 그 줄만 갈아끼우면 된다.
 */
@Immutable
enum class DogBreed(
    val id: String,
    val label: String,
    val fur: Fur,
    val ear: Ear,
    val tail: Tail,
    val crown: Crown,
    /** 주둥이 길이 배수. 작을수록 아기 같다 — 포메가 제일 작다. */
    val muzzle: Float,
    /** 비글식 흰 무늬(주둥이·가슴·발끝·꼬리끝) + 등의 짙은 안장. */
    val marked: Boolean,
    /** 이 견종에 어울리는 기본 털색. */
    val coat: DogCoat,
) {
    POODLE(
        "dog_poodle", "푸들",
        Fur.CURLY, Ear.DROP_ROUND, Tail.POM, Crown.NONE,
        muzzle = 0.95f, marked = false, coat = DogCoat.APRICOT,
    ),

    /** 늘어진 긴 귀 + 삼색 무늬. 둘 다 없으면 비글로 안 읽힌다. */
    BEAGLE(
        "dog_beagle", "비글",
        Fur.SMOOTH, Ear.DROP_LONG, Tail.UP_TIP, Crown.NONE,
        muzzle = 1.1f, marked = true, coat = DogCoat.CHOCO,
    ),

    /** 순백 + 흘러내리는 생머리 + 정수리 리본. 리본이 결정타다. */
    MALTESE(
        "dog_maltese", "말티즈",
        Fur.SILKY, Ear.DROP_SILKY, Tail.PLUME, Crown.BOW,
        muzzle = 0.85f, marked = false, coat = DogCoat.WHITE,
    ),

    /** 뾰족한 선 귀 + 목 갈기 + 아주 짧은 주둥이. 여우 얼굴. */
    POMERANIAN(
        "dog_pom", "포메라니안",
        Fur.FLUFFY, Ear.PRICK, Tail.PLUME, Crown.RUFF,
        muzzle = 0.7f, marked = false, coat = DogCoat.CREAM,
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
 * 파츠가 도형이든 나중에 PNG 든 이 값만 받으면 되므로, 아트를 갈아끼워도
 * 움직이는 코드는 그대로다.
 */
@Immutable
data class DogPose(
    /** 걸음 위상(라디안). 이동 거리에 비례해 늘어나므로 속도와 자동으로 맞는다. */
    val phase: Float = 0f,
    /**
     * 0 = 앉은 정면, 1 = 옆모습 걷기.
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

/** 어느 털색이든 같은 코·눈. 색까지 따라 밝히면 이목구비가 흐려진다. */
private val Ink = Color(0xFF41332C)

/** 비글의 흰 무늬. 털색을 안 탄다 — 어느 비글이든 이 부분은 희다. */
private val Marking = Color(0xFFFCF7F1)

/** 볼터치. 싸고 확실한 귀여움 장치라 전 견종 공통으로 넣는다. */
private val Blush = Color(0xFFF29AA6)

private val Ribbon = Color(0xFFF2909F)

private fun DogBreed.markOr(fallback: Color) = if (marked) Marking else fallback

/** 귀 색. 삼색 견종은 진짜로 귀가 짙고, 나머지는 몸에서 살짝만 떨어진다. */
private fun DogBreed.earColor(coat: DogCoat) = if (marked) coat.shade else coat.soft

// ---------------------------------------------------------------------------
// 축 1 — 털결
// ---------------------------------------------------------------------------

private fun DrawScope.ring(
    color: Color, cx: Float, cy: Float, rx: Float, ry: Float, n: Int, r: Float, phase: Float,
) {
    val step = (2f * Math.PI.toFloat()) / n
    for (i in 0 until n) {
        val t = phase + i * step
        drawCircle(color, r, Offset(cx + rx * cos(t), cy + ry * sin(t)))
    }
}

/**
 * 덩어리 하나를 그 견종의 털결로 그린다.
 *
 * 타원을 깔고 **테두리를 어떻게 처리하느냐**로 털을 표현한다. 형태는 그대로 두고
 * 윤곽만 바꾸면 견종이 바뀐다 — 축을 나눈 이유가 이것이다.
 */
private fun DrawScope.fur(
    style: Fur, color: Color, cx: Float, cy: Float, rx: Float, ry: Float, phase: Float = 0f,
) {
    drawOval(color, Offset(cx - rx, cy - ry), Size(rx * 2f, ry * 2f))
    when (style) {
        // 짧은 털 — 테두리를 건드리지 않는다. 매끈한 게 특징이다
        Fur.SMOOTH -> Unit
        Fur.CURLY -> ring(color, cx, cy, rx, ry, 12, rx * 0.23f, phase)
        Fur.FLUFFY -> ring(color, cx, cy, rx, ry, 11, rx * 0.30f, phase)
        // 흘러내리는 생머리.
        // **가닥을 가늘게 하면 고드름·걸레가 된다** — 실제로 그렇게 나왔었다.
        // 넓게(rx의 60%) 잡고 서로 겹치게 해서 부드러운 치맛단으로 만든다.
        Fur.SILKY -> {
            val n = 5
            for (i in 0 until n) {
                val t = 0.18f + (Math.PI.toFloat() - 0.36f) * i / (n - 1)
                val x = cx + rx * 0.9f * cos(t)
                val y = cy + ry * 0.66f * sin(t)
                drawOval(color, Offset(x - rx * 0.30f, y - ry * 0.1f), Size(rx * 0.60f, ry * 0.9f))
            }
        }
    }
}

/** 폼폼. 푸들 꼬리 끝처럼 **의도적으로 동그랗게 깎은** 부분에만 쓴다. */
private fun DrawScope.pom(color: Color, center: Offset, r: Float) {
    drawCircle(color, r, center)
    ring(color, center.x, center.y, r, r, 7, r * 0.40f, 0.3f)
}

/**
 * 발.
 *
 * **털결을 따라간다.** 전부 폼폼으로 그렸더니 매끈한 비글과 생머리 말티즈까지
 * 푸들 발목 폼폼을 달고 있었다. 뽀글거리는 발은 곱슬·풍성 견종만이다.
 */
private fun DrawScope.paw(fur: Fur, color: Color, center: Offset, r: Float) {
    drawCircle(color, r, center)
    when (fur) {
        Fur.CURLY -> ring(color, center.x, center.y, r, r, 7, r * 0.40f, 0.3f)
        Fur.FLUFFY -> ring(color, center.x, center.y, r, r, 6, r * 0.46f, 0.3f)
        Fur.SMOOTH, Fur.SILKY -> Unit
    }
}

// ---------------------------------------------------------------------------
// 축 2 — 귀
//
// 늘어진 귀는 머리보다 **먼저** (뒤로), 선 귀는 머리보다 **나중에** (앞으로) 그린다.
// 선 귀를 먼저 그렸더니 머리에 통째로 가려져서 포메가 안 보였다.
// ---------------------------------------------------------------------------

private val Ear.inFront: Boolean get() = this == Ear.PRICK

/** 한쪽 귀. [side] 는 -1(왼쪽) / +1(오른쪽). [len] 으로 옆모습에서 조금 줄인다. */
private fun DrawScope.ear(breed: DogBreed, coat: DogCoat, x: Float, top: Float, side: Int, len: Float) {
    val tone = breed.earColor(coat)
    when (breed.ear) {
        // 푸들 — 복슬복슬 뭉친 귀
        Ear.DROP_ROUND -> {
            drawCircle(tone, 8.4f * len, Offset(x, top + 3f))
            drawCircle(tone, 7.8f * len, Offset(x - side * 1.5f, top + 11f * len))
            drawCircle(tone, 6.6f * len, Offset(x - side * 2f, top + 18f * len))
        }
        // 비글 — 길고 낮게 늘어진 매끈한 귀. 턱 아래까지 내려온다
        Ear.DROP_LONG -> {
            drawRoundRect(
                tone,
                Offset(x - 7f, top - 4f),
                Size(14f, 30f * len),
                CornerRadius(7f, 9f),
            )
            drawCircle(tone, 7f, Offset(x, top - 4f + 26f * len))
        }
        // 말티즈 — 얼굴 옆으로 곧게 드리운 생머리
        Ear.DROP_SILKY -> {
            drawRoundRect(
                tone,
                Offset(x - 6f, top - 4f),
                Size(12f, 34f * len),
                CornerRadius(6f, 10f),
            )
            drawOval(tone, Offset(x - 6.5f, top + 20f * len), Size(13f, 14f))
        }
        // 포메 — 뾰족한 선 귀.
        //
        // **크고 바깥으로 빼야 한다.** 작게 그렸더니 머리 실루엣 안에 통째로 묻혀서
        // 포메가 푸들과 구분이 안 됐다. 늘어진 귀는 머리 아래로 나오니 작아도 보이지만,
        // 선 귀는 머리 위·옆으로 삐져나온 부분만 보인다.
        Ear.PRICK -> {
            // **작고 · 끝이 둥글고 · 바깥으로 기울고 · 밑동이 털에 묻혀야 한다.**
            // 길고 뾰족하게 뽑았더니 도깨비 뿔, 똑바로 세웠더니 머리에 붙인 삼각형이 됐다.
            val h = 9f * len
            val w = 7f * len
            val baseY = top + 3f
            // 살짝만 기울인다. 20도로 젖혔더니 끝이 머리 옆으로 6단위나 밀려나서
            // **옆통수에서 솟은 뿔**이 됐다. 8도면 자연스럽고 밖으로 안 튄다.
            rotate(side * 8f, Offset(x, baseY)) {
                // 끝을 **곡선으로** 굴린다. 삼각형 꼭짓점에 원을 얹었더니 그 원이 밖으로
                // 튀어나와 안테나가 됐다. 밑변보다 낮은 뭉툭한 삼각형이라야 귀로 읽힌다.
                drawPath(
                    Path().apply {
                        moveTo(x - w, baseY)
                        lineTo(x - w * 0.3f, baseY - h + 2f)
                        quadraticBezierTo(
                            x + side * 1.2f, baseY - h - 1f,
                            x + w * 0.3f, baseY - h + 2f,
                        )
                        lineTo(x + w, baseY)
                        close()
                    },
                    tone,
                )
                // 귓속은 분홍. 밝은 색으로 채우면 귀가 텅 빈 윤곽선처럼 보인다
                drawPath(
                    Path().apply {
                        moveTo(x - w * 0.45f, baseY - 2f)
                        lineTo(x + w * 0.45f, baseY - 2f)
                        lineTo(x + side * 1.2f, top - h * 0.5f)
                        close()
                    },
                    lerp(coat.light, Blush, 0.42f),
                )
            }
            // 밑동을 머리털로 덮는다. **이게 빠지면 삼각형을 머리에 박아둔 것처럼 보인다.**
            for (k in -1..1) drawCircle(coat.body, 4.5f, Offset(x + k * 4.2f, baseY + 2f))
        }
    }
}

// ---------------------------------------------------------------------------
// 축 3 — 꼬리
// ---------------------------------------------------------------------------

/** 꼬리. [pivot] 이 관절, [dir] 은 꼬리가 뻗는 화면 방향(-1 왼쪽 / +1 오른쪽). */
private fun DrawScope.tail(breed: DogBreed, coat: DogCoat, pivot: Offset, dir: Int, angle: Float) {
    rotate(angle, pivot) {
        when (breed.tail) {
            Tail.POM -> {
                drawRoundRect(
                    coat.shade,
                    Offset(pivot.x - 3f, pivot.y - 17f),
                    Size(6f, 17f),
                    CornerRadius(3f, 3f),
                )
                pom(coat.body, Offset(pivot.x, pivot.y - 19f), 6.6f)
            }
            // 비글 — 곧게 서고 끝이 희다. 멀리서도 눈에 띄는 표식
            Tail.UP_TIP -> {
                drawRoundRect(
                    coat.body,
                    Offset(pivot.x - 3.2f, pivot.y - 21f),
                    Size(6.4f, 22f),
                    CornerRadius(3.2f, 3.2f),
                )
                drawCircle(Marking, 4.2f, Offset(pivot.x, pivot.y - 19f))
            }
            // 등 위로 부채처럼 말리는 꼬리 (말티즈·포메)
            Tail.PLUME -> {
                for (i in 0..4) {
                    val t = i / 4f
                    val a = t * 1.5f
                    drawCircle(
                        if (i == 0) coat.shade else coat.body,
                        7f - t * 1.8f,
                        Offset(
                            pivot.x + dir * (1f + 12f * sin(a)),
                            pivot.y - 2f - 13f * (1f - cos(a)) - t * 7f,
                        ),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 얼굴 — 귀여움이 여기서 결정된다
// ---------------------------------------------------------------------------

/**
 * 눈 하나.
 *
 * 처음엔 반경 4.3 에 흰 점 두 개였는데 "너무 부담스럽다"고 두 번 걸려 2.9 까지 줄였다.
 * 아기 도식은 눈이 커야 한다지만 **화면에서 강아지가 130px 남짓이라 이론값을 그대로
 * 쓰면 얼굴이 눈으로 다 덮인다.** 실기기에서 보고 내린 값이 이론보다 우선이다.
 * (머리 높이의 20% — 교과서의 1/4 보다 작다)
 */
private fun DrawScope.eye(cx: Float, cy: Float, r: Float) {
    drawCircle(Ink, r, Offset(cx, cy))
    drawCircle(Color.White, r * 0.34f, Offset(cx - r * 0.28f, cy - r * 0.32f))
}

/** 코 + 웃는 입(ω). 입은 작고 낮게 — 크면 어른 얼굴이 된다. */
private fun DrawScope.snoutMarks(cx: Float, noseY: Float, w: Float) {
    drawOval(Ink, Offset(cx - w / 2f, noseY - w * 0.36f), Size(w, w * 0.72f))
    // 입(ω)은 **코보다 좁아야** 한다. 코보다 넓으면 얼굴 아래쪽이 입에 먹힌다.
    val m = w * 0.44f
    val stroke = Stroke(width = 1.1f, cap = StrokeCap.Round)
    drawArc(Ink, 0f, 180f, false, Offset(cx - m, noseY + w * 0.38f), Size(m, m * 0.78f), style = stroke)
    drawArc(Ink, 0f, 180f, false, Offset(cx, noseY + w * 0.38f), Size(m, m * 0.78f), style = stroke)
}

private fun DrawScope.blush(cx: Float, cy: Float, w: Float) {
    drawOval(Blush.copy(alpha = 0.32f), Offset(cx - w / 2f, cy - w * 0.31f), Size(w, w * 0.62f))
}

/** 말티즈 정수리 리본. 흰 강아지에 색점 하나가 들어가면 확 산다. */
private fun DrawScope.bow(cx: Float, cy: Float) {
    drawOval(Ribbon, Offset(cx - 8.5f, cy - 4.5f), Size(8f, 9f))
    drawOval(Ribbon, Offset(cx + 0.5f, cy - 4.5f), Size(8f, 9f))
    drawCircle(lerp(Ribbon, Color.White, 0.25f), 2.6f, Offset(cx, cy))
}

// ---------------------------------------------------------------------------
// 정면 — 앉은 자세 (쉴 때 · 미리보기 · 인벤토리)
// ---------------------------------------------------------------------------

private fun DrawScope.dogFront(breed: DogBreed, coat: DogCoat, frame: Int) {
    val f = frame % 8
    val bob = floatArrayOf(0f, -0.6f, -1.2f, -0.6f, 0f, -0.6f, -1.2f, -0.6f)[f]
    val wag = floatArrayOf(-16f, -4f, 8f, 16f, 8f, -4f, -16f, -22f)[f]
    val flick = if (f == 2 || f == 3) -5f else 0f

    // 뒷발 — 옆으로 살짝 비죽. 네발짐승이라는 걸 알린다
    for (x in floatArrayOf(12.5f, 43.5f)) paw(breed.fur, coat.shade, Offset(x, 66f), 4.2f)

    translate(0f, bob) {
        tail(breed, coat, Offset(43f, 58f), dir = 1, angle = wag)

        // 몸통 — **머리보다 작다.** 치비 비율의 핵심
        fur(breed.fur, coat.body, 28f, 58f, 15.5f, 13f)
        drawOval(coat.shade.copy(alpha = 0.35f), Offset(17f, 57f), Size(22f, 12f))
        if (breed.marked) drawOval(coat.shade, Offset(15f, 46f), Size(26f, 12f))

        for (x in floatArrayOf(21f, 35f)) paw(breed.fur, breed.markOr(coat.light), Offset(x, 69f), 5.2f)
        fur(breed.fur, breed.markOr(coat.light), 28f, 55f, 9.5f, 8f, 0.4f)

        // 포메의 목 갈기 — 머리가 몸에 파묻힌 여우 실루엣
        if (breed.crown == Crown.RUFF) fur(Fur.FLUFFY, coat.body, 28f, 49f, 17f, 10.5f, 0.15f)

        rotate(flick, Offset(28f, 24f)) {
            if (!breed.ear.inFront) {
                ear(breed, coat, 12.5f, 24f, -1, 1f)
                ear(breed, coat, 43.5f, 24f, 1, 1f)
            }

            fur(breed.fur, coat.body, 28f, 29f, 16f, 14.5f, 0.2f)

            if (breed.ear.inFront) {
                ear(breed, coat, 19f, 17f, -1, 1f)
                ear(breed, coat, 37f, 17f, 1, 1f)
            }
            when (breed.crown) {
                Crown.TOPKNOT -> pom(coat.body, Offset(28f, 13f), 8f)
                Crown.BOW -> bow(28f, 15f)
                else -> Unit
            }
        }

        // 주둥이는 작고 **낮게**
        fur(Fur.SMOOTH, breed.markOr(coat.light), 28f, 41f, 8f * breed.muzzle, 5.6f)
        blush(15.5f, 37f, 8.5f)
        blush(40.5f, 37f, 8.5f)
        // 눈은 머리 위에서 60% 지점 — 이게 귀여움을 가장 크게 좌우한다
        eye(21.5f, 32f, 2.9f)
        eye(34.5f, 32f, 2.9f)
        snoutMarks(28f, 39.5f, 5.8f)
    }
}

// ---------------------------------------------------------------------------
// 옆모습 — 걸을 때
//
// **네발 걸음은 옆에서 봐야 읽힌다.** 정면 빌보드는 다리가 둘밖에 안 보여서
// 아무리 잘 움직여도 두 발 걷기로 읽힌다.
// 오른쪽을 보고 있는 그림이다. 왼쪽은 `MiniRoomDrawing` 의 `dog.mirrored` 가 뒤집는다.
// ---------------------------------------------------------------------------

/** @param far 반대쪽(먼) 다리인가. 멀면 화면상 위쪽 · 작게 · 어둡게 — 몸통보다 먼저 그린다 */
private fun DrawScope.sideLeg(
    breed: DogBreed, coat: DogCoat, x: Float, lift: Float, dx: Float, far: Boolean,
) {
    val pawR = if (far) 4.2f else 5f
    val pawY = if (far) 67f else 70f
    val w = if (far) 7f else 8.5f
    translate(dx, -lift) {
        drawRoundRect(coat.shade, Offset(x - w / 2f, 54f), Size(w, pawY - 54f), CornerRadius(w / 2f, w / 2f))
        paw(breed.fur, if (far) coat.shade else breed.markOr(coat.light), Offset(x, pawY), pawR)
    }
}

private fun DrawScope.dogSide(breed: DogBreed, coat: DogCoat, frame: Int, pose: DogPose) {
    val f = frame % 8
    val wag = floatArrayOf(-14f, -3f, 8f, 15f, 8f, -3f, -14f, -20f)[f]
    val flick = if (f == 2 || f == 3) -4f else 0f

    // 대각선 걸음(속보) — 가까운 앞발과 **먼 뒷발**이 같이 뜬다. 네발짐승의 걸음이라
    // 이 조합만으로도 두 발 걷기와 확실히 갈린다.
    val s = sin(pose.phase) * pose.stand
    val nearFront = max(0f, s) * 3.4f
    val farHind = nearFront * 0.85f
    val farFront = max(0f, -s) * 3.4f
    val nearHind = farFront * 0.85f

    sideLeg(breed, coat, 16f, farHind, -s * 1.2f, far = true)
    sideLeg(breed, coat, 32f, farFront, s * 1.6f, far = true)

    tail(breed, coat, Offset(9f, 50f), dir = -1, angle = wag)

    fur(breed.fur, coat.body, 22f, 55f, 15.5f, 12.5f)
    fur(breed.fur, coat.body, 11f, 53f, 10f, 10f, 0.3f)
    drawOval(coat.shade.copy(alpha = 0.35f), Offset(11f, 55f), Size(23f, 11f))
    if (breed.marked) drawOval(coat.shade, Offset(10f, 43f), Size(26f, 12f))

    sideLeg(breed, coat, 12f, nearHind, -s * 1.4f, far = false)
    sideLeg(breed, coat, 28f, nearFront, s * 1.8f, far = false)

    fur(breed.fur, breed.markOr(coat.light), 31f, 52f, 8.5f, 8.5f, 0.4f)
    if (breed.crown == Crown.RUFF) fur(Fur.FLUFFY, coat.body, 32f, 45f, 12.5f, 11f, 0.15f)

    fur(breed.fur, coat.body, 36f, 30f, 14.5f, 13.5f, 0.2f)

    // 주둥이는 머리보다 **바깥으로** 나와야 옆모습으로 읽힌다.
    // 머리 안에 파묻히면 물개가 된다 — 실제로 그렇게 나왔었다.
    fur(Fur.SMOOTH, breed.markOr(coat.light), 46.5f, 38f, 6.2f * breed.muzzle, 5.2f)
    blush(43f, 36f, 8f)
    eye(41f, 32f, 2.8f)
    snoutMarks(48f, 36f, 5.2f)

    // 귀는 **머리보다 나중에.** 옆에서 보면 가까운 쪽 귀가 머리 앞으로 온다.
    // 먼저 그렸더니 머리에 통째로 가려져서 비글의 긴 귀가 사라졌다.
    // 눈(x 41)보다 뒤(x 30)에 두어야 얼굴을 안 가린다.
    rotate(flick + s * 3f, Offset(34f, 26f)) {
        if (breed.ear.inFront) {
            ear(breed, coat, 32f, 17f, -1, 0.9f)
        } else {
            ear(breed, coat, 30f, 25f, -1, 0.85f)
        }
        when (breed.crown) {
            Crown.TOPKNOT -> pom(coat.body, Offset(37f, 15f), 7.6f)
            Crown.BOW -> bow(37f, 17f)
            else -> Unit
        }
    }
}

// ---------------------------------------------------------------------------
// 조립
// ---------------------------------------------------------------------------

/**
 * 강아지 한 마리 — 스프라이트 시트가 아직 없을 때 쓰는 폴백.
 *
 * 중요한 건 이게 **시트와 똑같은 프레임 인덱스를 받는다**는 점이다.
 * 나중에 `ItemArtSpec.Sheet.resId` 에 진짜 시트를 꽂으면 이 함수는 호출되지 않고,
 * 크기·기준점·앞뒤 정렬은 ArtBox 에서 오므로 그대로 맞는다.
 *
 * 시계가 둘인 이유:
 *  - [frame] — 대기 동작(숨쉬기·꼬리·귀). 8프레임짜리 **미리 정해둔 값**
 *  - [pose] — 걸음. 이동 거리에 비례하는 연속값이라 속도와 저절로 맞는다
 */
fun DrawScope.drawDogBreed(
    breed: DogBreed,
    frame: Int,
    coat: DogCoat = breed.coat,
    pose: DogPose = DogPose.Idle,
) {
    val t = pose.stand

    // 그림자는 **항상 바닥에** 있어야 한다. 도는 동안 몸이 떠도 따라 뜨면 안 된다.
    drawOval(RoomPalette.Shadow, Offset(8f, 66f), Size(40f, 14f))

    if (t < 0.02f) {
        dogFront(breed, coat, frame)
        return
    }

    // 도는 중 — **살짝 뛰면서 돈다.**
    //
    // 처음엔 가로로 눌러 뒤집었는데 "얇게 플립만 되는 느낌"이 났다. 당연했다 —
    // **입체가 세로축으로 돌면 실루엣 폭이 줄지 않는다.** 앞폭 47, 옆길이 55 인
    // 덩어리를 45도로 돌리면 `47·cos45 + 55·sin45 ≈ 72` 로 오히려 넓어진다.
    // 얇아지는 건 두께가 0 인 카드뿐이라, 누르는 순간 종이가 된다.
    //
    // 그래서 폭은 거의 안 건드리고(0.90) 대신 **뛰어오르게** 한다. 만화가 방향 전환에
    // 늘 쓰는 수법이고, 몸이 뜬 순간에 앞↔옆이 바뀌므로 전환이 자연스럽게 감춰진다.
    val turn = 1f - abs(2f * t - 1f)
    withTransform({
        translate(0f, -7f * turn)
        scale(1f - 0.10f * turn, 1f + 0.08f * turn, pivot = Offset(28f, 74f))
    }) {
        if (t < 0.5f) dogFront(breed, coat, frame) else dogSide(breed, coat, frame, pose)
    }
}
