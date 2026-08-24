package com.daengs.app.miniroom.art

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import com.daengs.app.ui.theme.RoomPalette
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------
// V3 — 잉크 만화 (굵은 윤곽선 + 긴 주둥이 + 플랫 컬러)
//
// 강아지 아트는 **축 두 개의 조합**이다.
//
//   견종([DogBreed]) = 형태   ·   털색([DogCoat]) = 색
//
// 견종마다 그림을 통째로 짜지 않는다. 털결·귀·꼬리·머리장식을 축으로 나눠두고
// 견종은 그 축의 값을 고르기만 한다. 그래서 견종 하나 추가가 **표에 한 줄**이다.
//
// ## 세 판본
//
// | | V1 (main) | V2 (도트) | V3 (여기) |
// |---|---|---|---|
// | 그리는 법 | 파스텔 벡터 | 정수 격자에 칸 찍기 | 벡터 + 굵은 잉크선 |
// | 윤곽선 | 없음 | 균일한 1칸 | **두께가 변하는 선** |
// | 주둥이 | 짧고 낮게 | 짧게 | **길게 내민다** |
// | 명암 | 있음 | 없음 | 없음 (플랫) |
//
// ## 두께가 변하는 선을 만드는 법
//
// 이 화풍의 8할이 윤곽선인데, Compose 의 Path 에는 **가변 두께 선이 없다.**
// 굵기를 하나 정하면 처음부터 끝까지 같은 굵기다. 그래서 선을 긋지 않고 **면으로**
// 만든다.
//
//   1. 실루엣 전체를 잉크색으로, [InkGrow] 만큼 부풀려 그린다
//   2. 같은 실루엣을 털색으로, 부풀리지 않고 **왼쪽 위로 밀어서** 덮는다
//
// 그러면 밀어낸 방향의 반대쪽(오른쪽 아래)에 선이 두껍게 남고 밀어낸 쪽은 얇아진다.
// 붓을 눌러 그은 것처럼 무게중심이 생긴다 — 균일한 테두리와 인상이 완전히 다르다.
//
// V2 에서 배운 "실루엣을 두 번 그린다"를 그대로 쓴다. 덩어리마다 테두리를 두르면
// 머리·몸·다리가 겹치는 자리마다 선이 보여 몸통 한가운데 배꼽 선이 생기기 때문이다.
// 다른 건 두 번째 패스를 **밀어서** 그린다는 것뿐이다.
//
// 좌표계: ArtBox 상자 56x78, 바닥에 닿는 점은 (28, 74).
// 머리 y 10..52 (42) · 목 아래~발끝 52..74 (22) → **2 : 1**
// ---------------------------------------------------------------------------

/**
 * 잉크 실루엣을 이만큼 부풀린다. 이게 선의 기본 굵기다.
 *
 * 강아지가 화면에서 130px 남짓이라 아트 1단위가 대략 1.7px 다. 2.6 으로 뒀더니
 * 두꺼운 쪽이 6px 가까워져서 **선이 그림을 먹었다.** 1.6 이면 3px 언저리다.
 */
private const val InkGrow = 1.6f

/**
 * 털색 패스를 미는 양. 이 값이 선의 **두께 차**를 만든다 (0 이면 균일한 테두리).
 *
 * 1.0 을 줬더니 두꺼운 쪽과 얇은 쪽이 두 배 넘게 차이 나서, 붓이 아니라 **인쇄가
 * 밀린 것**처럼 보였다. 0.45 면 차이가 눈에 띄되 어색하지 않다.
 */
private const val InkShiftX = -0.45f
private const val InkShiftY = -0.55f

/**
 * 강아지 털색 한 벌.
 *
 * 잉크 화풍은 플랫이라 [shade] 를 명암에 쓰지 않는다. 귀·발끝처럼 **몸에서 한 톤
 * 떨어져야 하는 부분**에만 쓴다 — 흰 강아지에서 이게 없으면 귀가 머리에 먹힌다.
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
    /** 주둥이·가슴·발처럼 밝은 부분. */
    val light: Color = lerp(body, Color.White, 0.45f)

    /** 귀처럼 몸에서 **살짝만** 떨어져 보이면 되는 부분. */
    val soft: Color = lerp(body, shade, 0.55f)

    companion object {
        val CREAM = DogCoat("크림", Color(0xFFF7E7CC), Color(0xFFE0C49C))
        val APRICOT = DogCoat("애프리콧", Color(0xFFEFC098), Color(0xFFD79E70))
        val CHOCO = DogCoat("초코", Color(0xFFB5876A), Color(0xFF8E6850))
        val SILVER = DogCoat("실버", Color(0xFFD6D1DA), Color(0xFFB6B0BE))
        val WHITE = DogCoat("화이트", Color(0xFFFFFCF8), Color(0xFFE6DCD2))
        val CHARCOAL = DogCoat("차콜", Color(0xFF837988), Color(0xFF6A616F))

        val ALL: List<DogCoat> = listOf(CREAM, APRICOT, CHOCO, SILVER, WHITE, CHARCOAL)
    }
}

/** 털결 — 실루엣의 성격. 견종을 **가장 멀리서부터** 구분해주는 축. */
enum class Fur { CURLY, SMOOTH, FLUFFY }

/** 귀. **가까이서 견종을 가르는 건 사실상 이것 하나다.** [FLOP] 은 옆으로 뻗은 짧은 귀. */
enum class Ear { DROP_ROUND, DROP_LONG, FLOP, PRICK }

/** 꼬리. [CURL] 은 등 위로 짧게 말린 꼬리. */
enum class Tail { POM, UP_TIP, PLUME, CURL }

/** 머리 장식. */
enum class Crown { RUFF, NONE }

/**
 * 견종 하나 = 축의 조합.
 *
 * [id] 가 그대로 카탈로그 키다 ([ItemCatalog] 가 DogBreed.ALL 로 자동 등록한다).
 */
@Immutable
enum class DogBreed(
    val id: String,
    val label: String,
    val fur: Fur,
    val ear: Ear,
    val tail: Tail,
    val crown: Crown,
    /** 주둥이 길이 배수. 잉크 화풍에서는 이 축이 V1·V2 보다 훨씬 크게 먹는다. */
    val muzzle: Float,
    /** 비글식 흰 무늬(주둥이·가슴·발끝). */
    val marked: Boolean,
    /** 이 견종에 어울리는 기본 털색. */
    val coat: DogCoat,
) {
    POODLE(
        "dog_poodle", "푸들",
        Fur.CURLY, Ear.DROP_ROUND, Tail.POM, Crown.NONE,
        muzzle = 1.0f, marked = false, coat = DogCoat.APRICOT,
    ),

    /** 늘어진 긴 귀 + 삼색 무늬. 둘 다 없으면 비글로 안 읽힌다. */
    BEAGLE(
        "dog_beagle", "비글",
        Fur.SMOOTH, Ear.DROP_LONG, Tail.UP_TIP, Crown.NONE,
        muzzle = 1.25f, marked = true, coat = DogCoat.CHOCO,
    ),

    /** 동글동글 솜뭉치. 푸들과는 **주둥이가 더 짧은** 걸로 갈린다. */
    BICHON(
        "dog_bichon", "비숑",
        Fur.FLUFFY, Ear.DROP_ROUND, Tail.PLUME, Crown.NONE,
        muzzle = 0.8f, marked = false, coat = DogCoat.WHITE,
    ),

    /** 뾰족한 선 귀 + 목 갈기 + 아주 짧은 주둥이. 여우 얼굴. */
    POMERANIAN(
        "dog_pom", "포메라니안",
        Fur.FLUFFY, Ear.PRICK, Tail.PLUME, Crown.RUFF,
        muzzle = 0.75f, marked = false, coat = DogCoat.CREAM,
    ),

    /** 매끈한 흰 믹스. 옆으로 뻗은 처진 귀 + 말린 꼬리 — 만화 강아지의 기본형. */
    MIX(
        "dog_mix", "믹스",
        Fur.SMOOTH, Ear.FLOP, Tail.CURL, Crown.NONE,
        muzzle = 1.05f, marked = false, coat = DogCoat.WHITE,
    );

    companion object {
        val ALL: List<DogBreed> = entries

        private val index = entries.associateBy { it.id }

        /** 아트 키 → 견종. 렌더러가 DogActor.breed.id 로 되찾을 때 쓴다. */
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

/** 윤곽선·눈·코. 털색을 안 탄다 — 색까지 따라 밝히면 잉크선이 흐려진다. */
private val Ink = Color(0xFF2B2422)

/** 비글의 흰 무늬. 어느 비글이든 이 부분은 희다. */
private val Marking = Color(0xFFFDFAF6)

private fun DogBreed.markOr(fallback: Color) = if (marked) Marking else fallback

/** 귀 색. 삼색 견종은 진짜로 귀가 짙고, 나머지는 몸에서 살짝만 떨어진다. */
private fun DogBreed.earColor(coat: DogCoat) = if (marked) coat.shade else coat.soft

/** 잉크 패스에서는 무엇이든 잉크색이다. */
private fun pick(ink: Boolean, c: Color) = if (ink) Ink else c

/** 잉크 패스에서 도형을 부풀리는 양. 채우기 패스에서는 0. */
private fun grow(ink: Boolean) = if (ink) InkGrow else 0f

// ---------------------------------------------------------------------------
// 그리기 원시 함수 — 전부 ink 를 받아 두 패스를 함께 탄다
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
    style: Fur, color: Color, cx: Float, cy: Float, rx: Float, ry: Float,
    phase: Float = 0f, ink: Boolean = false,
) {
    val g = grow(ink)
    drawOval(color, Offset(cx - rx - g, cy - ry - g), Size((rx + g) * 2f, (ry + g) * 2f))
    when (style) {
        // 짧은 털 — 테두리를 건드리지 않는다. 매끈한 게 특징이다
        Fur.SMOOTH -> Unit
        Fur.CURLY -> ring(color, cx, cy, rx, ry, 11, rx * 0.24f + g, phase)
        Fur.FLUFFY -> ring(color, cx, cy, rx, ry, 10, rx * 0.30f + g, phase)
    }
}

/** 폼폼. 푸들 꼬리 끝처럼 **의도적으로 동그랗게 깎은** 부분에만 쓴다. */
private fun DrawScope.pom(color: Color, center: Offset, r: Float, ink: Boolean) {
    val g = grow(ink)
    drawCircle(color, r + g, center)
    ring(color, center.x, center.y, r, r, 7, r * 0.42f + g, 0.3f)
}

/** 다리 한 짝 = 관절에서 발까지의 캡슐. */
private fun DrawScope.limb(color: Color, from: Offset, to: Offset, w: Float, ink: Boolean) {
    drawLine(color, from, to, strokeWidth = w + grow(ink) * 2f, cap = StrokeCap.Round)
}

// ---------------------------------------------------------------------------
// 축 2 — 귀
// ---------------------------------------------------------------------------

/**
 * @param side -1 왼쪽 / +1 오른쪽
 * @param droop 뒤로 젖히는 각도(도). 걸을 때 귀가 날리는 값이 들어온다
 */
private fun DrawScope.ear(
    breed: DogBreed, coat: DogCoat, x: Float, top: Float, side: Int, len: Float,
    ink: Boolean, droop: Float = 0f,
) {
    val tone = pick(ink, breed.earColor(coat))
    val g = grow(ink)
    rotate(side * droop, Offset(x, top)) {
        when (breed.ear) {
            // 푸들·비숑 — 복슬복슬 뭉친 귀
            Ear.DROP_ROUND -> {
                drawCircle(tone, 9f * len + g, Offset(x, top + 3f))
                drawCircle(tone, 8f * len + g, Offset(x - side * 1.5f, top + 12f * len))
                drawCircle(tone, 6.5f * len + g, Offset(x - side * 2f, top + 20f * len))
            }
            // 비글 — 길고 낮게 늘어진 매끈한 귀. 턱 아래까지 내려온다
            Ear.DROP_LONG -> {
                val w = 11f * len
                val h = 30f * len
                drawOval(tone, Offset(x - w / 2f - g, top - g), Size(w + g * 2f, h + g * 2f))
                drawCircle(tone, w / 2f + g, Offset(x, top + h - w / 2f))
            }
            // 믹스 — **옆으로 뻗어** 늘어진 짧은 귀. 만화 강아지의 기본형이다
            Ear.FLOP -> {
                val w = 18f * len
                val h = 11f * len
                rotate(side * 20f, Offset(x, top)) {
                    drawOval(
                        tone,
                        Offset(x + (if (side < 0) -w else 0f) - g, top - h / 2f - g),
                        Size(w + g * 2f, h + g * 2f),
                    )
                }
            }
            // 포메 — 뾰족한 선 귀.
            //
            // **작고 · 끝이 둥글고 · 바깥으로 기울고 · 밑동이 털에 묻혀야 한다.**
            // 길고 뾰족하게 뽑았더니 도깨비 뿔, 똑바로 세웠더니 머리에 붙인 삼각형이 됐다.
            Ear.PRICK -> {
                val h = 12f * len + g
                val w = 8.5f * len + g
                val baseY = top + 3f
                rotate(side * 8f, Offset(x, baseY)) {
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
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 축 3 — 꼬리
// ---------------------------------------------------------------------------

private fun DrawScope.tail(
    breed: DogBreed, coat: DogCoat, pivot: Offset, dir: Int, angle: Float, ink: Boolean,
) {
    val body = pick(ink, coat.body)
    val g = grow(ink)
    rotate(angle, pivot) {
        when (breed.tail) {
            Tail.POM -> {
                limb(body, pivot, Offset(pivot.x, pivot.y - 12f), 5f, ink)
                pom(body, Offset(pivot.x, pivot.y - 15f), 6.5f, ink)
            }
            // 비글 — 곧게 서고 끝이 희다. 멀리서도 눈에 띄는 표식
            Tail.UP_TIP -> {
                limb(body, pivot, Offset(pivot.x, pivot.y - 18f), 6f, ink)
                drawCircle(pick(ink, Marking), 4.5f + g, Offset(pivot.x, pivot.y - 18f))
            }
            Tail.PLUME -> {
                for (i in 0..3) {
                    val t = i / 3f
                    val a = t * 1.5f
                    drawCircle(
                        body,
                        7f - t * 1.8f + g,
                        Offset(
                            pivot.x + dir * (1f + 12f * sin(a)),
                            pivot.y - 2f - 13f * (1f - cos(a)) - t * 6f,
                        ),
                    )
                }
            }
            // 믹스 — 등 위로 짧게 말린 꼬리
            Tail.CURL -> {
                limb(body, pivot, Offset(pivot.x + dir * 9f, pivot.y - 6f), 6f, ink)
                limb(
                    body,
                    Offset(pivot.x + dir * 9f, pivot.y - 6f),
                    Offset(pivot.x + dir * 7f, pivot.y - 15f),
                    5.5f, ink,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 얼굴 — 잉크 패스에는 안 나온다. 채우기 뒤에 한 번만 얹는다
// ---------------------------------------------------------------------------

/** 눈 — 점이다. 세로로 살짝 길어야 멍한 표정이 산다. */
private fun DrawScope.dotEye(cx: Float, cy: Float, r: Float) {
    drawOval(Ink, Offset(cx - r * 0.82f, cy - r), Size(r * 1.64f, r * 2f))
}

/** 코 — 주둥이 끝에 붙는 큼직한 검은 타원. 이 화풍에서 코는 크다. */
private fun DrawScope.nose(cx: Float, cy: Float, w: Float) {
    drawOval(Ink, Offset(cx - w / 2f, cy - w * 0.38f), Size(w, w * 0.76f))
}

/** 입 — 코 밑에서 한 번 꺾이는 짧은 선. */
private fun DrawScope.mouth(cx: Float, cy: Float, w: Float) {
    val p = Path().apply {
        moveTo(cx, cy)
        lineTo(cx, cy + w * 0.28f)
        quadraticBezierTo(cx - w * 0.2f, cy + w * 0.52f, cx - w * 0.45f, cy + w * 0.34f)
        moveTo(cx, cy + w * 0.28f)
        quadraticBezierTo(cx + w * 0.2f, cy + w * 0.52f, cx + w * 0.45f, cy + w * 0.34f)
    }
    drawPath(p, Ink, style = Stroke(width = 2.4f, cap = StrokeCap.Round))
}

// ---------------------------------------------------------------------------
// 앉은 정면
// ---------------------------------------------------------------------------

private fun DrawScope.sitSilhouette(breed: DogBreed, coat: DogCoat, wag: Float, ink: Boolean) {
    val body = pick(ink, coat.body)

    tail(breed, coat, Offset(41f, 62f), dir = 1, angle = wag, ink = ink)

    // 뒷다리 — 몸통 밖으로 삐져나온 엉덩이. 없으면 두 발 짐승이 된다
    for (sx in floatArrayOf(15f, 41f)) {
        fur(breed.fur, pick(ink, coat.shade), sx, 65f, 7f, 6.5f, 0.5f, ink)
        drawOval(pick(ink, breed.markOr(coat.light)), Offset(sx - 4.5f - grow(ink), 68f - grow(ink)),
            Size(9f + grow(ink) * 2f, 6f + grow(ink) * 2f))
    }

    // 몸통 — 머리의 절반. 머리에 파묻혀야 목이 없어 보인다
    fur(breed.fur, body, 28f, 62f, 13f, 10f, ink = ink)

    // 앞다리
    for (sx in floatArrayOf(23f, 33f)) {
        limb(body, Offset(sx, 62f), Offset(sx, 69f), 8f, ink)
        drawOval(pick(ink, breed.markOr(coat.light)), Offset(sx - 5f - grow(ink), 68f - grow(ink)),
            Size(10f + grow(ink) * 2f, 6.5f + grow(ink) * 2f))
    }

    // 포메의 목 갈기 — 머리가 몸에 파묻힌 여우 실루엣
    if (breed.crown == Crown.RUFF) fur(Fur.FLUFFY, body, 28f, 53f, 21f, 7f, 0.15f, ink)

    if (breed.ear != Ear.PRICK) {
        ear(breed, coat, 8f, 26f, -1, 1f, ink)
        ear(breed, coat, 48f, 26f, 1, 1f, ink)
    }

    // 머리 — 실루엣의 대부분
    fur(breed.fur, body, 28f, 31f, 20f, 21f, 0.2f, ink)

    if (breed.ear == Ear.PRICK) {
        ear(breed, coat, 15f, 13f, -1, 1f, ink)
        ear(breed, coat, 41f, 13f, 1, 1f, ink)
    }

    // **주둥이를 길게 내민다.** V1·V2 는 짧고 낮게 뒀는데, 이 화풍은 반대다 —
    // 주둥이가 얼굴 밖으로 나와야 만화 강아지로 읽힌다.
    fur(
        Fur.SMOOTH, pick(ink, breed.markOr(coat.light)),
        28f, 45f, 13f * breed.muzzle, 10f * breed.muzzle, ink = ink,
    )
}

private fun DrawScope.dogSit(breed: DogBreed, coat: DogCoat, frame: Int) {
    val f = frame % 8
    val bob = floatArrayOf(0f, -0.8f, -1.5f, -0.8f, 0f, -0.8f, -1.5f, -0.8f)[f]
    val wag = floatArrayOf(-15f, -5f, 7f, 15f, 7f, -5f, -15f, -21f)[f]

    translate(0f, bob) {
        sitSilhouette(breed, coat, wag, ink = true)
        // 털색 패스를 왼쪽 위로 민다 — 이게 선의 두께를 오른쪽 아래로 몰아준다
        translate(InkShiftX, InkShiftY) {
            sitSilhouette(breed, coat, wag, ink = false)

            // 얼굴은 채우기 뒤에 한 번만. 잉크 패스에 넣으면 눈이 뭉개진다
            dotEye(20f, 30f, 3.4f)
            dotEye(36f, 30f, 3.4f)
            nose(28f, 40f, 9f * breed.muzzle)
            mouth(28f, 44f, 13f)
        }
    }
}

// ---------------------------------------------------------------------------
// 옆모습 걷기 — **다마고치식 두 장**
//
// 자세를 두 장만 두고 딱딱 바꾼다. V2 에서 배운 것이다 — 다리를 각도로 부드럽게
// 돌리면 만화 화풍의 껑충거림이 안 나온다.
//
// 오른쪽을 보고 있는 그림이다. 왼쪽은 MiniRoomDrawing 의 dog.mirrored 가 뒤집는다.
// ---------------------------------------------------------------------------

private fun DrawScope.walkSilhouette(
    breed: DogBreed, coat: DogCoat, step: Int, wag: Float, ink: Boolean,
) {
    val body = pick(ink, coat.body)
    val shade = pick(ink, coat.shade)
    val pawTone = pick(ink, breed.markOr(coat.light))

    // 두 장의 차이는 **어느 짝이 앞에 있나** 하나뿐이다.
    // 다리 x 는 몸통(cx 22, rx 14 -> 8..36) 안에 있어야 한다. 밖에 두면 떨어져 보인다
    val frontX = if (step == 0) 33f else 29f
    val hindX = if (step == 0) 12f else 16f

    tail(breed, coat, Offset(9f, 59f), dir = -1, angle = wag, ink = ink)

    // 먼 쪽 다리 — 몸통보다 먼저. 어둡게 해서 깊이를 낸다
    limb(shade, Offset(frontX - 5f, 61f), Offset(frontX - 5f, 68f), 7f, ink)
    limb(shade, Offset(hindX + 5f, 61f), Offset(hindX + 5f, 68f), 7f, ink)

    // 몸통 — 옆에서는 가로로 길다
    // 몸통 — **정면과 같은 크기여야 한다.** rx 17 로 넓게 뒀더니 머리만큼 커져서
    // 머리가 몸을 압도하지 못했고, 옆모습만 딴 동물처럼 보였다
    fur(breed.fur, body, 22f, 61f, 14f, 9.5f, ink = ink)

    limb(body, Offset(frontX, 61f), Offset(frontX, 70f), 8f, ink)
    drawOval(pawTone, Offset(frontX - 5f - grow(ink), 69f - grow(ink)),
        Size(10f + grow(ink) * 2f, 6f + grow(ink) * 2f))
    limb(body, Offset(hindX, 61f), Offset(hindX, 70f), 8f, ink)
    drawOval(pawTone, Offset(hindX - 5f - grow(ink), 69f - grow(ink)),
        Size(10f + grow(ink) * 2f, 6f + grow(ink) * 2f))

    if (breed.crown == Crown.RUFF) fur(Fur.FLUFFY, body, 28f, 52f, 13f, 7f, 0.15f, ink)

    // 머리
    fur(breed.fur, body, 25f, 31f, 19.5f, 21f, 0.2f, ink)

    // **긴 주둥이.** 옆모습에서 이게 제일 크게 화풍을 만든다.
    // 머리 안에 파묻히면 물개가 된다 — V1 에서 실제로 그렇게 나왔었다.
    // 주둥이는 머리 밖으로 나오되 **동그랗게.** rx 13 으로 길게 뽑았더니 관처럼
    // 뻗어서 정면의 동그란 주둥이와 같은 개로 안 보였다. 머리 가장자리(x 44.5)를
    // 8 쯤만 넘기면 충분하다
    fur(
        Fur.SMOOTH, pick(ink, breed.markOr(coat.light)),
        42f, 43f, 10f * breed.muzzle, 8.5f * breed.muzzle, ink = ink,
    )

    // 귀는 머리보다 **나중에.** 먼저 그리면 머리에 덮여 대머리가 된다 (V2 에서 겪음).
    // 왼쪽 가장자리에 걸쳐야 실루엣 밖으로 나온다.
    if (breed.ear == Ear.PRICK) {
        ear(breed, coat, 19f, 12f, -1, 1f, ink, droop = if (step == 0) 6f else 0f)
    } else {
        ear(breed, coat, 12f, 29f, -1, 1f, ink, droop = if (step == 0) 16f else 6f)
    }
}

private fun DrawScope.dogWalk(breed: DogBreed, coat: DogCoat, frame: Int, pose: DogPose) {
    val f = frame % 8
    val wag = floatArrayOf(-12f, -4f, 6f, 12f, 6f, -4f, -12f, -18f)[f]
    val step = walkStep(pose.phase)
    // 걸을 때 몸이 뜬다. 이게 없으면 미끄러지는 것처럼 보인다
    val hop = if (step == 0) -2f else 0f

    translate(0f, hop) {
        walkSilhouette(breed, coat, step, wag, ink = true)
        translate(InkShiftX, InkShiftY) {
            walkSilhouette(breed, coat, step, wag, ink = false)

            dotEye(32f, 29f, 3.4f)
            nose(50f, 41f, 8.5f * breed.muzzle)
            mouth(48f, 45f, 11f)
        }
    }
}

// ---------------------------------------------------------------------------
// 조립
// ---------------------------------------------------------------------------

/**
 * 걸음 단계 0/1 — **시간이 아니라 이동 거리**로 만든다.
 *
 * 시간으로 하면 빨리 걷든 천천히 걷든 발이 같은 속도로 바뀌어 미끄러져 보인다.
 * phase 는 이동 거리에 비례하므로 이걸 쓰면 속도와 저절로 맞는다.
 */
private fun walkStep(phase: Float): Int = ((phase / 1.4f).toInt() % 2 + 2) % 2

/**
 * 강아지 한 마리 — 스프라이트 시트가 아직 없을 때 쓰는 폴백.
 *
 * 중요한 건 이게 **시트와 똑같은 프레임 인덱스를 받는다**는 점이다.
 * 나중에 ItemArtSpec.Sheet.resId 에 진짜 시트를 꽂으면 이 함수는 호출되지 않고,
 * 크기·기준점·앞뒤 정렬은 ArtBox 에서 오므로 그대로 맞는다.
 */
fun DrawScope.drawDogBreed(
    breed: DogBreed,
    frame: Int,
    coat: DogCoat = breed.coat,
    pose: DogPose = DogPose.Idle,
) {
    // 그림자는 **항상 바닥에** 있어야 한다. 몸이 떠도 따라 뜨면 안 된다
    drawOval(RoomPalette.Shadow, Offset(9f, 67f), Size(38f, 12f))

    if (pose.stand > 0.5f) dogWalk(breed, coat, frame, pose)
    else dogSit(breed, coat, frame)
}
