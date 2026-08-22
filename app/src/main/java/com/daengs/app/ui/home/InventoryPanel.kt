package com.daengs.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daengs.app.miniroom.RoomDefaults
import com.daengs.app.miniroom.RoomTheme
import com.daengs.app.miniroom.art.ItemArt
import com.daengs.app.miniroom.art.ItemCatalog
import com.daengs.app.miniroom.art.ItemLabels
import com.daengs.app.miniroom.sprite.drawSpriteFrame
import com.daengs.app.ui.theme.CardWhite
import com.daengs.app.ui.theme.DaengPink
import com.daengs.app.ui.theme.DaengPinkDeep
import com.daengs.app.ui.theme.DaengsTheme
import com.daengs.app.ui.theme.PinkFaint
import com.daengs.app.ui.theme.PinkSoft
import com.daengs.app.ui.theme.TextDark
import com.daengs.app.ui.theme.TextMuted

/**
 * 인벤토리 패널.
 *
 * 방 위에 겹쳐서 뜬다 — 한 화면 안에 다 넣어야 해서 아래에 자리를 새로 낼 수 없다.
 * 열려 있는 동안이 곧 **편집 모드**다: 방의 아이템을 톡 누르면 인벤토리로 돌아가고,
 * 여기 칸을 누르면 방의 빈 자리에 놓인다. (끄는 동작은 열려 있든 아니든 그대로 된다.)
 *
 * @param available itemId → 남은 개수
 */
@Composable
fun InventoryPanel(
    catalog: ItemCatalog,
    available: (String) -> Int,
    onPick: (String) -> Unit,
    currentTheme: RoomTheme,
    onPickTheme: (RoomTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Surface(
        color = CardWhite,
        shape = RoundedCornerShape(22.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        // 슬롯 높이가 고정이므로 내용은 가운데 정렬한다.
        Column(
            Modifier.fillMaxHeight().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TabChip("아이템", tab == 0) { tab = 0 }
                Spacer(Modifier.width(6.dp))
                TabChip("테마", tab == 1) { tab = 1 }
                Spacer(Modifier.width(9.dp))
                Text(
                    if (tab == 0) "톡 누르면 놓이고, 방에서 톡 누르면 돌아와요" else "방 색을 바꿔요",
                    color = TextMuted,
                    fontSize = 9.sp,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (tab == 0) {
                    RoomDefaults.INVENTORY_ORDER.forEach { id ->
                        val art = catalog[id]
                        if (art != null) {
                            InventorySlot(id, art, available(id)) { onPick(id) }
                        }
                    }
                } else {
                    RoomTheme.ALL.forEach { th ->
                        ThemeSwatch(th, th.id == currentTheme.id) { onPickTheme(th) }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventorySlot(
    id: String,
    art: ItemArt,
    count: Int,
    onClick: () -> Unit,
) {
    val enabled = count > 0
    Column(
        Modifier
            .width(60.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .background(if (enabled) PinkFaint else PinkFaint.copy(alpha = 0.4f))
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 개수는 아래 "x0" 하나로 충분하다. 썸네일 위에 0 을 겹쳐 쓰면 중복이다.
        ItemThumb(art, Modifier.size(46.dp).alpha(if (enabled) 1f else 0.3f))
        Spacer(Modifier.height(2.dp))
        Text(
            ItemLabels[id] ?: id,
            color = if (enabled) TextDark else TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            "x$count",
            color = if (enabled) DaengPinkDeep else TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * 인벤토리 썸네일.
 *
 * 방에서 쓰는 것과 **같은 아트**를 그대로 그린다. 아이콘을 따로 만들면 나중에
 * PNG 로 갈아끼울 때 두 군데를 고쳐야 하고 실제 모습과 어긋나기 쉽다.
 *
 * 선언(spec)이 아니라 **해석된 [ItemArt]** 를 받는다. spec 만 보면 리소스가 아직
 * 해석되기 전이라 PNG 아이템을 그릴 수 없어서 빈칸이 나온다 (실제로 그랬다).
 */
@Composable
fun ItemThumb(art: ItemArt, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val box = art.box
        val z = minOf(size.width / box.size.width, size.height / box.size.height) * 0.86f
        translate(
            (size.width - box.size.width * z) / 2f,
            (size.height - box.size.height * z) / 2f,
        ) {
            scale(z, z, pivot = Offset.Zero) {
                when (art) {
                    is ItemArt.Shapes -> art.draw(this, 2)
                    is ItemArt.Bitmap -> drawImage(
                        image = art.image,
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(box.size.width.toInt(), box.size.height.toInt()),
                        filterQuality = art.filterQuality,
                    )

                    is ItemArt.Sheet ->
                        if (art.sheet != null) {
                            drawSpriteFrame(art.sheet, 0, box.size)
                        } else {
                            art.fallback(this, 2)
                        }
                }
            }
        }
    }
}

/** 인벤토리 열기/닫기 버튼. */
@Composable
fun InventoryButton(open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (open) DaengPink else CardWhite.copy(alpha = 0.85f),
        modifier = modifier.size(46.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                if (open) "✕" else "＋",
                color = if (open) CardWhite else DaengPinkDeep,
                fontWeight = FontWeight.Bold,
                fontSize = if (open) 18.sp else 24.sp,
            )
        }
    }
}

@Preview(widthDp = 411, showBackground = true, backgroundColor = 0xFFF3D8D2)
@Composable
private fun InventoryPanelPreview() {
    DaengsTheme {
        InventoryPanel(
            catalog = com.daengs.app.miniroom.art.rememberItemCatalog(),
            available = { if (it == "rug") 0 else 2 },
            onPick = {},
            currentTheme = RoomTheme.DEFAULT,
            onPickTheme = {},
        )
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) DaengPink else PinkFaint,
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick),
    ) {
        Text(
            label,
            color = if (selected) CardWhite else TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp),
        )
    }
}

/**
 * 테마 미리보기.
 *
 * 색 동그라미만 보여주면 벽/바닥/포인트가 실제로 어떻게 어울리는지 알 수 없어서,
 * 방 모양 그대로 축소해 그린다. 방과 **같은 아이소메트릭 비율**을 쓴다.
 */
@Composable
private fun ThemeSwatch(theme: RoomTheme, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .width(60.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(if (selected) PinkSoft else PinkFaint)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(46.dp, 40.dp)) {
            val tw = size.width * 0.88f
            val th = tw / 2f
            val wallH = th * 1.15f
            val cx = size.width / 2f
            val oy = size.height * 0.34f

            fun poly(p: List<Offset>) = Path().apply {
                moveTo(p[0].x, p[0].y)
                p.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }

            val top = Offset(cx, oy)
            val right = Offset(cx + tw / 2f, oy + th / 2f)
            val bottom = Offset(cx, oy + th)
            val left = Offset(cx - tw / 2f, oy + th / 2f)

            drawPath(poly(listOf(top, left, left - Offset(0f, wallH), top - Offset(0f, wallH))), theme.wallLeft)
            drawPath(poly(listOf(top, right, right - Offset(0f, wallH), top - Offset(0f, wallH))), theme.wallRight)
            drawPath(poly(listOf(top, right, bottom, left)), theme.floorLight)
            // 문 — 포인트 색이 어디에 쓰이는지 보이게
            val dx = cx - tw * 0.28f
            drawRoundRect(
                theme.doorFill,
                Offset(dx, oy + th * 0.12f - wallH * 0.72f),
                Size(tw * 0.16f, wallH * 0.62f),
                androidx.compose.ui.geometry.CornerRadius(tw * 0.08f, tw * 0.08f),
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            theme.label,
            color = if (selected) DaengPinkDeep else TextDark,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
