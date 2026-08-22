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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daengs.app.miniroom.RoomDefaults
import com.daengs.app.miniroom.art.ItemArtSpec
import com.daengs.app.miniroom.art.ItemLabels
import com.daengs.app.miniroom.art.ItemSpecs
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
    available: (String) -> Int,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                Text("아이템", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.width(6.dp))
                Text("톡 누르면 놓이고, 방에서 톡 누르면 돌아와요", color = TextMuted, fontSize = 9.sp)
            }
            Spacer(Modifier.height(5.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RoomDefaults.INVENTORY_ORDER.forEach { id ->
                    val spec = ItemSpecs[id]
                    if (spec != null) {
                        InventorySlot(
                            id = id,
                            spec = spec,
                            count = available(id),
                            onClick = { onPick(id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventorySlot(
    id: String,
    spec: ItemArtSpec,
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
        ItemThumb(spec, Modifier.size(46.dp).alpha(if (enabled) 1f else 0.3f))
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
 * 방에서 쓰는 것과 **같은 도형 코드**를 그대로 호출한다. 아이콘을 따로 만들면
 * 나중에 PNG 로 갈아끼울 때 두 군데를 고쳐야 하고, 실제 모습과 어긋나기 쉽다.
 */
@Composable
fun ItemThumb(spec: ItemArtSpec, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val box = spec.box
        val z = minOf(size.width / box.size.width, size.height / box.size.height) * 0.86f
        translate(
            (size.width - box.size.width * z) / 2f,
            (size.height - box.size.height * z) / 2f,
        ) {
            scale(z, z, pivot = Offset.Zero) {
                when (spec) {
                    is ItemArtSpec.Shapes -> spec.draw(this, 2)
                    is ItemArtSpec.Sheet -> spec.fallback(this, 2)
                    is ItemArtSpec.Res -> Unit
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
        InventoryPanel(available = { if (it == "rug") 0 else 2 }, onPick = {})
    }
}
