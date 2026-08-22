package com.daengs.app.miniroom.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.daengs.app.ui.theme.RoomPalette

/**
 * 강아지 대기 동작 — 스프라이트 시트가 아직 없을 때 쓰는 폴백.
 *
 * 중요한 건 이게 **시트와 똑같은 프레임 인덱스를 받는다**는 점이다.
 * 나중에 `ItemArtSpec.Sheet.resId` 에 진짜 시트를 꽂으면 이 함수는 호출되지 않고,
 * 크기·기준점·앞뒤 정렬은 ArtBox 에서 오므로 그대로 맞는다.
 *
 * 좌표계: ArtBox 상자 56x78, 바닥에 닿는 점은 (28, 74).
 */
fun DrawScope.drawDog(frame: Int) {
    val f = frame % 8

    // 숨쉬기 — 몸이 위아래로 아주 조금
    val bob = floatArrayOf(0f, -0.6f, -1.2f, -0.6f, 0f, -0.6f, -1.2f, -0.6f)[f]
    // 꼬리 흔들기
    val tail = floatArrayOf(-16f, -4f, 8f, 16f, 8f, -4f, -16f, -22f)[f]
    // 귀 까딱 (가끔)
    val ear = if (f == 2 || f == 3) -5f else 0f

    drawOval(RoomPalette.Shadow, Offset(6f, 66f), Size(44f, 16f))

    translate(0f, bob) {
        // 꼬리 — 몸통보다 먼저 그려서 뒤로 간다
        rotate(tail, Offset(44f, 54f)) {
            drawRoundRect(
                RoomPalette.DogBodyDark,
                Offset(42f, 34f),
                Size(7f, 22f),
                androidx.compose.ui.geometry.CornerRadius(3.5f, 3.5f),
            )
        }

        // 앉은 자세 몸통
        drawOval(RoomPalette.DogBody, Offset(9f, 38f), Size(38f, 36f))
        drawOval(RoomPalette.DogBodyDark.copy(alpha = 0.35f), Offset(13f, 52f), Size(30f, 20f))

        // 앞다리
        drawRoundRect(
            RoomPalette.DogBody,
            Offset(17f, 58f),
            Size(8f, 16f),
            androidx.compose.ui.geometry.CornerRadius(4f, 4f),
        )
        drawRoundRect(
            RoomPalette.DogBody,
            Offset(31f, 58f),
            Size(8f, 16f),
            androidx.compose.ui.geometry.CornerRadius(4f, 4f),
        )

        // 귀 — 얼굴보다 먼저
        rotate(ear, Offset(28f, 26f)) {
            drawOval(RoomPalette.DogEar, Offset(6f, 2f), Size(14f, 20f))
            drawOval(RoomPalette.DogEar, Offset(36f, 2f), Size(14f, 20f))
        }

        // 머리
        drawCircle(RoomPalette.DogBody, 17f, Offset(28f, 26f))
        // 시안 강아지 특징 — 어두운 얼굴 마스크
        drawOval(RoomPalette.DogFace, Offset(15f, 18f), Size(26f, 22f))
        drawOval(RoomPalette.DogBody.copy(alpha = 0.9f), Offset(22f, 30f), Size(12f, 9f))

        // 눈
        drawCircle(RoomPalette.DogNose, 2.6f, Offset(22f, 25f))
        drawCircle(RoomPalette.DogNose, 2.6f, Offset(34f, 25f))
        drawCircle(RoomPalette.RugFill, 1f, Offset(22.8f, 24f))
        drawCircle(RoomPalette.RugFill, 1f, Offset(34.8f, 24f))

        // 코
        drawOval(RoomPalette.DogNose, Offset(25.5f, 31f), Size(5f, 3.6f))
    }
}
