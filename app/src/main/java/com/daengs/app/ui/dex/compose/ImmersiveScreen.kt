package com.daengs.app.ui.dex.compose

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daengs.app.miniroom.art.rememberAssetImage
import com.daengs.app.ui.dex.DeviceTilt
import kotlin.math.roundToInt
import kotlin.math.sin

// ---------------------------------------------------------------------------
// 이머시브 무대 그리기
//
// 평면 일곱 장을 시차를 두고 겹친다. 구조와 숫자는 [Immersive] 에 적어 뒀다.
//
// 진입 연출이 이 뷰의 요점이다 — 카드가 화면만 하게 커지고, **틀이 녹으면서**
// 그 안의 배추가 그대로 남아 무대가 된다. 캐릭터가 한 픽셀도 안 움직여야 "안으로
// 들어갔다"로 읽힌다. 그래서 누끼를 카드 안 제자리([ImmersiveScene.fit])에 놓고
// 시작해서, 카드가 사라지는 동안 서서히 무대 크기로 키운다.
// ---------------------------------------------------------------------------

private const val ENTER_MS = 2100

@Composable
fun ImmersiveScreen(
    scene: ImmersiveScene = CABBAGE_SCENE,
    onClose: () -> Unit,
) {
    val back = rememberAssetImage(scene.back)
    val subject = rememberAssetImage(scene.subject)
    val card = rememberAssetImage(scene.card)
    val parts = remember(scene) { buildScene(scene, seedOf("cabbage")) }

    BackHandler(onBack = onClose)

    // 진입. 0 = 카드 그대로, 1 = 무대
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val enter by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(ENTER_MS, easing = LinearEasing),
        label = "immersive-enter",
    )

    // 시선. 손가락이 없으면 자이로가 맡는다.
    val rub = rememberRubState()
    val tracker = remember { TiltTracker() }
    DeviceTilt { b, g -> tracker.feed(b, g) }
    val aim = if (rub.input.intensity > 0f) rub.input.p else (tracker.input?.p ?: Offset(0.5f, 0.5f))

    // 먼지·잎이 떠다니는 시계
    val clock = rememberInfiniteTransition(label = "immersive-clock")
    val t by clock.animateFloat(
        initialValue = 0f,
        targetValue = 6000f,
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart),
        label = "t",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1408))
            .rubbable(rub)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClose,
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawStage(scene, parts, back, subject, card, aim, enter, t.toLong())
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("CABBAGE NEO", color = Color(0xFFEFFBE2), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(scene.place, color = scene.accent2.copy(alpha = 0.8f), fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Text("아무 데나 누르면 나갑니다", color = Color(0x88FFFFFF), fontSize = 11.sp)
        }
    }
}

/** 평면 일곱 장. 뒤에서 앞으로. */
private fun DrawScope.drawStage(
    scene: ImmersiveScene,
    parts: SceneParts,
    back: ImageBitmap?,
    subject: ImageBitmap?,
    card: ImageBitmap?,
    aim: Offset,
    enter: Float,
    timeMs: Long,
) {
    // 1. 하늘 — 해 뜨기 직전의 텃밭
    drawRect(
        Brush.verticalGradient(
            0f to Color(0xFF16240F),
            0.55f to Color(0xFF24380F),
            1f to Color(0xFF3C4E14),
        ),
    )

    // 2. 색 환경 — 같은 배경 그림을 크게 흐리게 깐 것.
    //    저쪽도 진짜 블러가 아니라 "크게 늘려 색만 남긴" 층이다.
    if (back != null) {
        val d = parallax(aim, Par.AMBIENT)
        translate(d.x, d.y) {
            val over = 1.35f
            val w = size.width * over
            val h = w * back.height / back.width
            drawImage(
                image = back,
                dstOffset = androidx.compose.ui.unit.IntOffset(
                    ((size.width - w) / 2f).roundToInt(),
                    ((size.height - h) / 2f).roundToInt(),
                ),
                dstSize = androidx.compose.ui.unit.IntSize(w.roundToInt(), h.roundToInt()),
                alpha = 0.55f,
                filterQuality = FilterQuality.Low,
            )
        }
    }

    // 3. 빛줄기 — 위에서 비스듬히 내려온다
    run {
        val d = parallax(aim, Par.RAYS)
        translate(d.x, d.y) {
            drawRect(
                Brush.linearGradient(
                    0f to Color(0x33FFF6C4),
                    0.35f to Color(0x11FFF6C4),
                    1f to Color.Transparent,
                    start = Offset(size.width * 0.75f, -size.height * 0.1f),
                    end = Offset(size.width * 0.1f, size.height),
                ),
                blendMode = BlendMode.Screen,
            )
        }
    }

    // 4. 먼지 — 카드 뒤에서 느리게 떠다닌다
    run {
        val d = parallax(aim, Par.MOTES)
        translate(d.x, d.y) {
            parts.motes.forEach { m ->
                val p = m.drift(timeMs, size)
                drawCircle(scene.accent.copy(alpha = m.alpha * 0.8f), m.r, p, blendMode = BlendMode.Screen)
            }
        }
    }

    // 5. 주인공 — 카드 안 제자리에서 시작해 무대 크기로 자란다.
    //    **여기가 진입 연출의 핵심이다.** 틀이 녹는 동안 캐릭터가 안 움직여야 한다.
    if (subject != null) {
        val d = parallax(aim, Par.SUBJECT)
        translate(d.x, d.y) {
            val r = subjectRect(scene, subject, size, enter)
            drawImage(
                image = subject,
                dstOffset = androidx.compose.ui.unit.IntOffset(r.first.x.roundToInt(), r.first.y.roundToInt()),
                dstSize = androidx.compose.ui.unit.IntSize(r.second.width.roundToInt(), r.second.height.roundToInt()),
                filterQuality = FilterQuality.High,
            )
        }
    }

    // 6. 카드 틀 — 진입하는 동안만 보인다. 녹듯이 사라진다.
    if (card != null && enter < 1f) {
        val alpha = (1f - enter * 1.6f).coerceIn(0f, 1f)
        if (alpha > 0.001f) {
            val (pos, sz) = cardRect(card, size, enter)
            drawImage(
                image = card,
                dstOffset = androidx.compose.ui.unit.IntOffset(pos.x.roundToInt(), pos.y.roundToInt()),
                dstSize = androidx.compose.ui.unit.IntSize(sz.width.roundToInt(), sz.height.roundToInt()),
                alpha = alpha,
                filterQuality = FilterQuality.High,
            )
        }
    }

    // 7. 앞잎사귀 — 크고 흐리게. 초점이 안쪽에 맞은 것처럼 보이게 하는 층이다
    run {
        val d = parallax(aim, Par.FORE)
        translate(d.x, d.y) {
            parts.leaves.forEach { l ->
                val sway = sin(timeMs / 1400f + l.phase) * 6f
                val w = size.width * 0.42f * l.scale
                val h = w * 0.62f
                drawOval(
                    color = Color(0xFF2E4A12).copy(alpha = l.alpha),
                    topLeft = Offset(l.at.x * size.width - w / 2f + sway, l.at.y * size.height - h / 2f),
                    size = Size(w, h),
                )
            }
        }
    }

    // 8. 이슬 — 카메라 유리에 맺힌 방울. **이 층만 시차가 0 이다.**
    parts.dew.forEach { dw ->
        val run = if (dw.runs) ((timeMs / 30f) % (size.height * 1.2f)) else 0f
        val p = Offset(dw.at.x * size.width, dw.at.y * size.height + run)
        drawCircle(Color.White.copy(alpha = dw.alpha * 0.5f), dw.r, p, blendMode = BlendMode.Screen)
        drawCircle(Color.White.copy(alpha = dw.alpha), dw.r * 0.35f, p - Offset(dw.r * 0.3f, dw.r * 0.3f))
    }

    // 가장자리 어둡게 — 무대에 초점이 모인다
    drawRect(
        Brush.radialGradient(
            0.55f to Color.Transparent,
            1f to Color(0xAA000000),
            center = Offset(size.width / 2f, size.height * 0.45f),
            radius = size.width * 0.9f,
        ),
    )
}

/**
 * 누끼가 놓일 자리. [enter] 0 이면 **카드 안 제자리**, 1 이면 무대 가득.
 *
 * 카드 안 제자리는 [ImmersiveScene.fit] 이 준다 — 원본 카드 그림에서 누끼가
 * 차지하던 사각형이다. 여기서 출발해야 틀이 녹는 동안 캐릭터가 안 움직인다.
 */
private fun subjectRect(
    scene: ImmersiveScene,
    subject: ImageBitmap,
    stage: Size,
    enter: Float,
): Pair<Offset, Size> {
    val (cardPos, cardSize) = cardRect(subject, stage, enter)
    // 카드 안에서의 자리 (카드 크기 대비 %)
    val from = Offset(
        cardPos.x + cardSize.width * scene.fit.x / 100f,
        cardPos.y + cardSize.height * scene.fit.y / 100f,
    )
    val fromSize = Size(cardSize.width * scene.fit.w / 100f, cardSize.height * scene.fit.h / 100f)

    // 무대 가득. 가로를 채우고 아래쪽에 앉힌다.
    val toW = stage.width * 1.05f
    val toH = toW * subject.height / subject.width
    val to = Offset((stage.width - toW) / 2f, stage.height * 0.52f - toH / 2f)
    val toSize = Size(toW, toH)

    val e = enter.coerceIn(0f, 1f)
    return Offset(
        from.x + (to.x - from.x) * e,
        from.y + (to.y - from.y) * e,
    ) to Size(
        fromSize.width + (toSize.width - fromSize.width) * e,
        fromSize.height + (toSize.height - fromSize.height) * e,
    )
}

/** 카드가 놓일 자리. 들어오는 동안 화면만 하게 커진다. */
private fun cardRect(card: ImageBitmap, stage: Size, enter: Float): Pair<Offset, Size> {
    val ratio = card.width.toFloat() / card.height
    // 시작은 확대 뷰와 같은 크기, 끝은 화면보다 살짝 크게
    val startW = stage.width * 0.92f
    val endW = stage.width * 1.25f
    val w = startW + (endW - startW) * enter.coerceIn(0f, 1f)
    val h = w / ratio
    return Offset((stage.width - w) / 2f, (stage.height - h) / 2f) to Size(w, h)
}
