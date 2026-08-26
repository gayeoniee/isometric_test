package com.daengs.app.ui.dex.compose

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daengs.app.miniroom.art.rememberAssetImage
import com.daengs.app.ui.dex.DeviceTilt
import com.daengs.app.ui.theme.CreamBg
import com.daengs.app.ui.theme.DaengPink
import com.daengs.app.ui.theme.PinkFaint
import com.daengs.app.ui.theme.TextDark
import com.daengs.app.ui.theme.TextMuted

// ---------------------------------------------------------------------------
// 네오 채소 도감 — Compose 판
//
// 웹판(`ui/dex/CardDexScreen.kt`)과 같은 것을 네이티브로 만든 것이다. 카드 효과는
// 최대한 비슷하게 옮기되, **둘러싼 화면은 앱 톤으로** 간다 — 웹판은 어두운 배경
// (#0E0B05)이라 방에서 넘어올 때 분위기가 확 바뀌는데, 네이티브로 오는 이유가
// 그걸 없애는 것이기도 하다.
//
// 카드 그림은 웹판과 **같은 에셋**을 쓴다 (`assets/neo-hologram/art/`). 같은 파일을
// 두 벌 두면 저쪽이 그림을 갱신할 때 두 곳을 고쳐야 한다.
// ---------------------------------------------------------------------------

/** 도감 배경. 앱 크림색보다 살짝 가라앉혀 카드가 떠 보이게 한다. */
private val DexBg = Color(0xFFF6E9E3)

/** 카드 칸의 세로 비율. 웹판 `.slot .frame { aspect-ratio: 4/5 }` 와 같다. */
private const val SLOT_RATIO = 1.25f

/**
 * 꾹 누르는 동안 차오르는 테두리.
 *
 * **카드 모서리를 따라가야 한다.** 처음에 `drawArc` 로 그렸더니 사각형에 내접한
 * 타원이 나왔다 — 카드와 아무 상관 없는 선이라 "카드가 반응한다"로 안 읽힌다.
 * 저쪽은 `border-radius: 5% / 3.6%` 짜리 둥근 사각형 테두리를 conic-gradient 로
 * 채운다. 여기서는 같은 모양의 경로를 만들어 앞에서부터 [progress] 만큼 잘라 그린다.
 *
 * 위 가운데에서 시작한다. 경로를 그냥 재면 모서리 어딘가에서 시작해 어색하다.
 */
private fun DrawScope.drawHoldRing(progress: Float, color: Color) {
    val rx = size.width * 0.05f
    val ry = size.height * 0.036f
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(Offset.Zero, size),
                topLeft = CornerRadius(rx, ry),
                topRight = CornerRadius(rx, ry),
                bottomRight = CornerRadius(rx, ry),
                bottomLeft = CornerRadius(rx, ry),
            ),
        )
    }
    val measure = PathMeasure().apply { setPath(path, false) }
    val total = measure.length
    if (total <= 0f) return

    // 위 가운데가 경로의 어디쯤인지. addRoundRect 는 오른쪽 위 모서리 근처에서
    // 시작하므로, 한 바퀴의 7/8 지점이 대략 위 가운데다.
    val start = total * 0.875f
    val want = total * progress.coerceIn(0f, 1f)

    val seg = Path()
    val first = minOf(want, total - start)
    measure.getSegment(start, start + first, seg, true)
    if (want > first) {
        // 한 바퀴를 넘어가면 앞쪽에서 이어 붙인다
        measure.getSegment(0f, want - first, seg, true)
    }
    drawPath(seg, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
}

@Composable
fun ComposeDexScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
    var opened by remember { mutableStateOf<Int?>(null) }
    var immersive by remember { mutableStateOf(false) }

    BackHandler {
        when {
            immersive -> immersive = false
            opened != null -> opened = null
            else -> onClose()
        }
    }

    if (immersive) {
        ImmersiveScreen(onClose = { immersive = false })
        return
    }

    Box(modifier.fillMaxSize().background(DexBg)) {
        DexGrid(
            onOpen = { opened = it },
            onClose = onClose,
            onImmersive = { immersive = true },
        )

        AnimatedVisibility(
            visible = opened != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            val start = opened ?: 0
            CardViewer(startIndex = start, onClose = { opened = null })
        }
    }
}

// -- 그리드 -----------------------------------------------------------------

@Composable
private fun DexGrid(onOpen: (Int) -> Unit, onClose: () -> Unit, onImmersive: () -> Unit) {
    LazyVerticalGrid(
        // 두 칸. 웹판에서 한 칸이면 카드가 화면을 꽉 채워 무거웠다.
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            DexHeader(onClose = onClose)
        }
        items(DEX_CARDS) { card ->
            GridCard(
                card = card,
                onOpen = { onOpen(DEX_CARDS.indexOf(card)) },
                // No.01 배추만 이머시브다. 저쪽도 지금은 한 장뿐이다.
                onImmersive = if (card.no == 1) onImmersive else null,
            )
        }
    }
}

@Composable
private fun DexHeader(onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "← 방으로",
                color = TextMuted,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text("채소가 된 네오", color = TextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "${DEX_CARDS.size} / ${DEX_CARDS.size} 수집 · 카드를 눌러 크게 보세요",
            color = TextMuted,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun GridCard(card: DexCard, onOpen: () -> Unit, onImmersive: (() -> Unit)?) {
    // 그리드에서는 작게 그리므로 절반 크기로 읽는다. 12장을 원본으로 들면 55MB 다.
    val art = rememberAssetImage(card.art, sample = 2)
    val rub = rememberRubState(onTap = onOpen, onHold = onImmersive)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // **카드 높이를 칸마다 똑같이 맞춘다.** 그림 비율이 두 종류라(0.72 와 0.80)
        // 폭을 맞추면 높이가 제각각이 되어 줄이 어긋난다. 웹판이 "실물 카드 바인더와
        // 같은 정렬" 이라고 부르는 배치를 그대로 쓴다 — 높이는 같고 폭만 비율만큼
        // 달라지며, 그림은 한 픽셀도 안 잘린다.
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            HoloCard(
                art = art,
                foil = card.foil,
                input = rub.input,
                // 그리드에서는 기울이지 않는다. 열두 장이 한꺼번에 도는 건 산만하다.
                tilt = false,
                // 칸 폭의 4:5. 웹판 `.slot .frame` 과 같은 비율이다.
                modifier = Modifier
                    .height(maxWidth * SLOT_RATIO)
                    // **드래그를 안 먹는다.** 먹으면 카드를 짚고 쓸어내릴 때 목록이
                    // 안 움직인다.
                    .rubbable(rub, consume = false),
            )
            // 꾹 누르는 동안 차오르는 테두리.
            // **없으면 카드가 멈춘 줄 안다** — 꾹 누르기는 눌러보기 전엔 알 수가 없다.
            if (rub.hold > 0f) {
                Canvas(Modifier.matchParentSize()) {
                    drawHoldRing(rub.hold, card.accent)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (onImmersive != null) {
            // 꾹 누르기는 발견해야 아는 손짓이라 유일한 길이면 안 된다 (저쪽 주석).
            Text(
                "★★★ 꾹 눌러서 들어가기",
                color = TextDark,
                fontSize = 10.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(card.accent.copy(alpha = 0.25f))
                    .clickable(onClick = onImmersive)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            Spacer(Modifier.height(4.dp))
        }
        Text("No. %02d".format(card.no), color = TextMuted, fontSize = 10.sp)
        Text(card.name, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(card.statLine, color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

// -- 확대 뷰 ----------------------------------------------------------------

/**
 * 카드를 화면 가득 보는 뷰.
 *
 * 저쪽이 정한 손짓을 그대로 따른다 — **문지르면 포일 구경**, 좌우 버튼으로 넘기기.
 * 쓸어서 넘기기는 저쪽이 해보고 버렸다 (포일을 구경하다 보면 손이 저절로 빨라져서
 * 속도로는 못 가른다).
 */
@Composable
private fun CardViewer(startIndex: Int, onClose: () -> Unit) {
    var index by remember { mutableIntStateOf(startIndex) }
    val card = DEX_CARDS[index]
    // 확대 뷰는 한 장뿐이라 원본 해상도로 읽는다.
    val art = rememberAssetImage(card.art)
    val rub = rememberRubState()

    // 폰을 기울이면 카드가 따라 기운다. **확대 뷰에서만** 켠다 — 그리드에서 열두 장이
    // 한꺼번에 도는 건 산만하고 비싸다.
    val tiltTracker = remember { TiltTracker() }
    DeviceTilt { beta, gamma -> tiltTracker.feed(beta, gamma) }
    // 카드를 넘기면 지금 자세가 다시 정면이 된다
    LaunchedEffect(index) { tiltTracker.reset() }

    // **손가락이 이긴다.** 두 입력이 같은 카드를 두고 매 프레임 싸우면 화면이 떤다.
    val input = if (rub.input.intensity > 0f) rub.input else (tiltTracker.input ?: rub.input)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xE8241C1A))
            // 카드 밖을 누르면 닫힌다
            .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onClose() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.systemBarsPadding().padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HoloCard(
                art = art,
                foil = card.foil,
                input = input,
                tilt = true,
                modifier = Modifier.fillMaxWidth().rubbable(rub),
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NavButton("‹") { index = (index - 1 + DEX_CARDS.size) % DEX_CARDS.size }
                Spacer(Modifier.size(18.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(card.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(card.statLine, color = Color(0xFFD9C9C3), fontSize = 12.sp)
                }
                Spacer(Modifier.size(18.dp))
                NavButton("›") { index = (index + 1) % DEX_CARDS.size }
            }
        }

        Text(
            "✕",
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .systemBarsPadding()
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color(0x33FFFFFF))
                .clickable(onClick = onClose)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun NavButton(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = TextDark,
        fontSize = 20.sp,
        modifier = Modifier
            .clip(CircleShape)
            .background(PinkFaint)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 4.dp),
    )
}

@Suppress("unused")
private val unusedPalette = listOf(CreamBg, DaengPink)
