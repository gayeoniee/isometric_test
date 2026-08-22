package com.daengs.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.daengs.app.ui.theme.PinkFaint
import com.daengs.app.ui.theme.RoomPalette

/**
 * 강아지 얼굴 아바타.
 *
 * 미니룸 강아지와 같은 색을 쓴다 — 나중에 아바타 스튜디오가 생기면
 * 이 자리에 생성된 이미지를 끼운다.
 */
@Composable
fun DogAvatar(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension / 40f
        scale(s, s, pivot = Offset.Zero) {
            drawCircle(PinkFaint, 20f, Offset(20f, 20f))

            // 귀
            drawOval(RoomPalette.DogEar, Offset(3.5f, 6f), Size(12f, 17f))
            drawOval(RoomPalette.DogEar, Offset(24.5f, 6f), Size(12f, 17f))

            // 머리
            drawCircle(RoomPalette.DogBody, 14.5f, Offset(20f, 21f))
            // 어두운 얼굴 마스크
            drawOval(RoomPalette.DogFace, Offset(9f, 12.5f), Size(22f, 19f))
            drawOval(RoomPalette.DogBody.copy(alpha = 0.92f), Offset(15f, 22.5f), Size(10f, 8f))

            // 눈
            drawCircle(RoomPalette.DogNose, 2.3f, Offset(15.2f, 19.5f))
            drawCircle(RoomPalette.DogNose, 2.3f, Offset(24.8f, 19.5f))
            drawCircle(RoomPalette.RugFill, 0.9f, Offset(15.9f, 18.7f))
            drawCircle(RoomPalette.RugFill, 0.9f, Offset(25.5f, 18.7f))

            // 코
            drawOval(RoomPalette.DogNose, Offset(17.8f, 23.4f), Size(4.4f, 3.2f))
        }
    }
}

@Preview
@Composable
private fun DogAvatarPreview() {
    DogAvatar(Modifier.size(56.dp))
}
