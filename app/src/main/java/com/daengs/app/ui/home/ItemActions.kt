package com.daengs.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daengs.app.ui.theme.CardWhite
import com.daengs.app.ui.theme.DaengPink
import com.daengs.app.ui.theme.DaengPinkDeep
import com.daengs.app.ui.theme.DaengsTheme

/**
 * 선택한 가구 옆에 뜨는 버튼 두 개.
 *
 * 예전에는 "톡 누르면 돌리기 / 방 밖으로 끌어내면 치우기" 였는데,
 * 둘 다 **알려주지 않으면 모르는 조작**이었다. 눈에 보이는 버튼으로 바꿨다.
 */
@Composable
fun ItemActions(
    onRotate: () -> Unit,
    onStore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .shadow(4.dp, RoundedCornerShape(50), clip = false)
            .clip(RoundedCornerShape(50))
            .background(CardWhite),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionButton("↻", DaengPinkDeep, onRotate)
        ActionButton("✕", DaengPink, onStore)
    }
}

@Composable
private fun ActionButton(label: String, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        Modifier.size(34.dp).clip(RoundedCornerShape(50)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = tint, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEAD5BE)
@Composable
private fun ItemActionsPreview() {
    DaengsTheme { ItemActions(onRotate = {}, onStore = {}) }
}
