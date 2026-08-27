package com.daengs.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daengs.app.ui.DaengsIcon
import com.daengs.app.ui.DaengsIconView
import com.daengs.app.miniroom.art.DogBreed
import com.daengs.app.ui.DogAvatar
import com.daengs.app.ui.theme.CardWhite
import com.daengs.app.ui.theme.DaengPink
import com.daengs.app.ui.theme.DaengPinkDeep
import com.daengs.app.ui.theme.DaengsTheme
import com.daengs.app.ui.theme.PinkFaint
import com.daengs.app.ui.theme.PinkSoft
import com.daengs.app.ui.theme.TextDark
import com.daengs.app.ui.theme.TextMuted

/**
 * AI 챗봇 카드.
 *
 * 입력은 로컬 state 에만 반영되고 전송은 동작하지 않는다 (백엔드 없음).
 * 칩을 누르면 그 문장이 입력창에 채워지는 것까지만 한다 — 눌러봤을 때
 * 반응이 있어야 화면이 죽어 보이지 않는다.
 */
@Composable
fun ChatbotCard(
    modifier: Modifier = Modifier,
    avatar: DogBreed = HomeDemoData.DOG_BREED,
) {
    var text by rememberSaveable { mutableStateOf("") }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = CardWhite,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 11.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    HomeDemoData.CHAT_TITLE,
                    color = TextDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
                Spacer(Modifier.width(6.dp))
                DaengsIconView(DaengsIcon.Paw, Modifier.size(15.dp), tint = DaengPink)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(HomeDemoData.CHAT_MORE, color = TextMuted, fontSize = 12.sp)
                    DaengsIconView(DaengsIcon.ChevronRight, Modifier.size(15.dp), tint = TextMuted)
                }
            }

            Spacer(Modifier.height(9.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                DogAvatar(avatar, Modifier.size(40.dp))
                Spacer(Modifier.width(10.dp))

                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(PinkFaint, RoundedCornerShape(23.dp))
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (text.isEmpty()) {
                        Text(HomeDemoData.CHAT_PLACEHOLDER, color = TextMuted, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        textStyle = TextStyle(color = TextDark, fontSize = 14.sp),
                        cursorBrush = SolidColor(DaengPink),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.width(9.dp))
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(50))
                        .background(DaengPink)
                        .clickable { /* 백엔드 없음 — 자리만 잡아둔다 */ },
                    contentAlignment = Alignment.Center,
                ) {
                    DaengsIconView(DaengsIcon.Send, Modifier.size(21.dp), tint = CardWhite)
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                HomeDemoData.SUGGESTIONS.forEach { s ->
                    SuggestionChip(s) { text = s }
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = PinkFaint,
        border = BorderStroke(1.dp, PinkSoft),
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick),
    ) {
        Text(
            label,
            color = DaengPinkDeep,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
        )
    }
}

@Preview(widthDp = 411, showBackground = true, backgroundColor = 0xFFFDF1EC)
@Composable
private fun ChatbotCardPreview() {
    DaengsTheme {
        ChatbotCard(Modifier.padding(14.dp))
    }
}
