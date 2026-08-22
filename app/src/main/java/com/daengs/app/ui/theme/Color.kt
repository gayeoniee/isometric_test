package com.daengs.app.ui.theme

import androidx.compose.ui.graphics.Color

// 시안(design/home-screen.png)에서 뽑은 파스텔 팔레트.
// 앱 크롬(탭바·카드·칩)에서 쓰는 색.
val CreamBg = Color(0xFFFDF1EC)
val CardWhite = Color(0xFFFFFFFF)
val DaengPink = Color(0xFFF0A0A0)
val DaengPinkDeep = Color(0xFFE08585)
val PinkSoft = Color(0xFFFBE4E0)
val PinkFaint = Color(0xFFF7ECE8)
val TextDark = Color(0xFF4A3B36)
val TextMuted = Color(0xFFA79089)

/**
 * 미니룸 아트 전용 팔레트.
 *
 * MaterialTheme.colorScheme 에서 가져오지 않는다 — 아트 색은 브랜드 에셋이지
 * 테마 표면이 아니다. colorScheme 를 타면 기기 배경화면(dynamic color)이나
 * 다크 모드에 따라 방 색이 제멋대로 바뀐다.
 */
object RoomPalette {
    val WallLeft = Color(0xFFF3D8D2)
    val WallRight = Color(0xFFEBC9C2)
    val WallTrim = Color(0xFFFDF6F3)
    val WallShadow = Color(0xFFE2B9B1)

    val FloorLight = Color(0xFFEEDCC4)
    val FloorDark = Color(0xFFE4CDAF)
    val FloorPlank = Color(0xFFD8BE9C)
    val FloorEdge = Color(0xFFF6EDE2)

    val SkyTop = Color(0xFFBFE3F5)
    val SkyBottom = Color(0xFFE8F5FB)
    val Cloud = Color(0xFFFFFFFF)
    val Blossom = Color(0xFFF7C0CE)
    val BlossomDeep = Color(0xFFEFA3B8)
    val Branch = Color(0xFFC49A86)

    val DoorFill = Color(0xFFF2B3B3)
    val DoorTrim = Color(0xFFFDF6F3)
    val DoorKnob = Color(0xFFEB9A9A)

    val FenceFill = Color(0xFFF6C7C4)
    val FenceTrim = Color(0xFFFFF3F0)

    val RugFill = Color(0xFFFAF3EA)
    val RugRim = Color(0xFFEFE2D2)
    val BallFill = Color(0xFFF3A8A8)
    val BowlFill = Color(0xFFEFB0A6)
    val BedFill = Color(0xFFF6D3CE)
    val PlantPot = Color(0xFFE9A896)
    val PlantLeaf = Color(0xFF9FC49A)
    val BoneFill = Color(0xFFF7EBDD)

    // 추가 아이템
    val HouseWall = Color(0xFFF6DCD2)
    val HouseWallSide = Color(0xFFEBC7BA)
    val HouseRoof = Color(0xFFEE9F9F)
    val HouseRoofSide = Color(0xFFDC8A8A)
    val HouseDoor = Color(0xFF7E5C50)
    val BoxTop = Color(0xFFF3C9A8)
    val BoxLeft = Color(0xFFDDAE8B)
    val BoxRight = Color(0xFFE9BE9B)
    val WaterRim = Color(0xFFA9C9E4)
    val WaterFill = Color(0xFF7FB2DA)
    val CushionTop = Color(0xFFCBB7E0)
    val CushionSide = Color(0xFFB49ECD)
    val VaseFill = Color(0xFFBFD9CE)
    val VaseSide = Color(0xFFA3C4B7)
    val FlowerA = Color(0xFFF3A6BE)
    val FlowerB = Color(0xFFF6CE7E)
    val BlanketFill = Color(0xFFDCD3EC)
    val BlanketRim = Color(0xFFC5B9DC)

    val DogBody = Color(0xFFE8C9A0)
    val DogBodyDark = Color(0xFFD3AE81)
    val DogFace = Color(0xFF5B4438)
    val DogEar = Color(0xFF6B4F40)
    val DogNose = Color(0xFF3A2C24)

    val Shadow = Color(0x1A5B4438)
    val GhostValid = Color(0xFF7FC98F)
    val GhostInvalid = Color(0xFFE87F7F)
}
