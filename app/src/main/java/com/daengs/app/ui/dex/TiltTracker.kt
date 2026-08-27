package com.daengs.app.ui.dex

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

/**
 * 폰 기울기를 포일 입력으로 바꾼다.
 *
 * 웹판(`assets/neo-hologram/main.js` 의 `feedOrientation`)이 하던 일을 그대로 옮겼다.
 * 그쪽 규칙 중 옮겨야 했던 것들.
 *
 *  - **절대 각도가 아니라 처음 자세에서 얼마나 움직였는지**를 쓴다. 폰을 눕혀 보든
 *    세워서 보든 처음 자세가 정면이 되므로, 들자마자 카드가 홱 돌아가지 않는다
 *  - [RANGE] 만큼 기울이면 카드가 끝까지 돈다
 *  - **손가락이 올라가 있으면 포인터가 이긴다.** 두 입력이 같은 카드를 두고 매
 *    프레임 싸우면 화면이 떤다
 */
class TiltTracker {
    var input by mutableStateOf<FoilInput?>(null)
        private set

    private var baseBeta = Float.NaN
    private var baseGamma = Float.NaN

    /** @param beta 앞뒤, @param gamma 좌우. 둘 다 도 단위 (deviceorientation 규약) */
    fun feed(beta: Float, gamma: Float) {
        if (baseBeta.isNaN()) {
            baseBeta = beta
            baseGamma = gamma
        }
        val dx = gamma - baseGamma
        val dy = beta - baseBeta
        val p = Offset(
            (0.5f + dx / RANGE / 2f).coerceIn(0f, 1f),
            (0.5f + dy / RANGE / 2f).coerceIn(0f, 1f),
        )
        // 가운데에 가까우면 포일도 잠잠하게. 가만히 든 손이 카드를 번쩍이게 하면
        // 눈이 피로하다.
        val away = maxOf(abs(p.x - 0.5f), abs(p.y - 0.5f)) * 2f
        input = FoilInput.of(p, away.coerceIn(0f, 1f))
    }

    /** 카드를 넘기거나 뷰를 닫을 때. 다음에 들 때 그 자세가 다시 정면이 된다. */
    fun reset() {
        baseBeta = Float.NaN
        baseGamma = Float.NaN
        input = null
    }

    private companion object {
        /** 이 각도(도)만큼 기울이면 끝까지 돈다. 저쪽 `TILT_RANGE` 와 같은 값. */
        const val RANGE = 20f
    }
}
