package com.daengs.app.ui.dex

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.abs

// ---------------------------------------------------------------------------
// 자이로 → 웹 카드
//
// 저쪽 도감(`assets/neo-hologram/main.js`)이 **네이티브 앱용 문을 이미 만들어 뒀다.**
//
//     window.__neoTilt = feedOrientation;   // (beta, gamma) 도 단위
//
// 그래서 우리가 할 일은 센서 값을 그 규약에 맞춰 넣는 것뿐이다. 아래는 저쪽이
// 알아서 하므로 여기서 다시 하지 않는다.
//
//   - 확대한 카드 한 장에만 적용 (그리드는 안 건드린다)
//   - 손가락이 올라가 있으면 포인터가 이긴다
//   - 처음 들어온 값을 '정면'으로 삼는다 (들자마자 홱 안 돈다)
//   - 화면 회전 보정
//   - requestAnimationFrame 으로 프레임당 한 번으로 묶기
//
// **웹의 `deviceorientation` 이벤트를 쓰지 않는다.** 그쪽은 secure context 를 타는데,
// 이 브릿지는 웹 API 를 안 거치므로 그 제약이 없다.
// ---------------------------------------------------------------------------

/**
 * 폰 기울기를 [onTilt] 로 흘린다. 단위는 **도**, `deviceorientation` 규약이다.
 *
 * 화면이 보일 때만 센서를 문다. 도감을 닫아 두고 배터리를 쓰면 안 된다.
 *
 * 회전 벡터 센서가 없는 기기에서는 **아무 일도 일어나지 않는다** — 손가락으로
 * 문지르는 길이 그대로 남으므로 카드는 여전히 볼 수 있다.
 *
 * ## 왜 걸러 내나
 *
 * [onTilt] 한 번이 곧 `evaluateJavascript` 한 번이고, 그건 렌더러 프로세스를 넘는
 * 일이다. 센서는 초당 50번 뛰므로 그대로 흘리면 **폰을 책상에 놓아둬도** 초당 50번
 * 프로세스를 넘나든다. 실제로 입력 지연이 눈에 띄게 올라갔다.
 *
 * 저쪽도 `requestAnimationFrame` 으로 묶지만 그건 **JS 안으로 들어온 뒤** 이야기라
 * 넘어가는 비용은 그대로 든다. 그래서 넘기기 전에 두 겹으로 거른다.
 *
 *   1. 직전에 보낸 각도와 [MIN_DEGREES] 미만 차이면 안 보낸다
 *   2. 마지막으로 보낸 지 [MIN_INTERVAL_MS] 가 안 지났으면 안 보낸다
 *
 * @param enabled false 면 센서를 아예 안 문다. **확대 뷰가 닫혀 있을 때** 이걸로 끈다 —
 *   그리드에서는 저쪽이 기울기를 그냥 버리므로 보내 봐야 낭비다.
 */
@Composable
fun DeviceTilt(enabled: Boolean = true, onTilt: (beta: Float, gamma: Float) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val callback by rememberUpdatedState(onTilt)

    DisposableEffect(context, owner, enabled) {
        if (!enabled) return@DisposableEffect onDispose { }

        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (manager == null || sensor == null) return@DisposableEffect onDispose { }

        // 재사용한다. 센서 콜백은 초당 수십 번 오므로 매번 새로 잡으면 쓰레기가 쌓인다.
        val rotation = FloatArray(9)
        val angles = FloatArray(3)

        // 마지막으로 **내보낸** 값. 센서가 준 값이 아니다.
        var lastBeta = Float.NaN
        var lastGamma = Float.NaN
        var lastSentAt = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, angles)
                // angles = [azimuth, pitch, roll] (라디안)
                //
                // deviceorientation 규약으로 옮긴다. beta 는 앞뒤, gamma 는 좌우다.
                // **부호는 실기기에서 맞췄다.** 저쪽이 절대 각도가 아니라 '처음 자세에서
                // 얼마나 움직였는지'를 쓰므로, 부호가 틀리면 카드가 반대로 기운다.
                val beta = -angles[1] * RAD_TO_DEG
                val gamma = angles[2] * RAD_TO_DEG

                // 안 움직였으면 보내지 않는다. 손에 들고 있어도 미세한 떨림은
                // 카드에서 안 보이므로 버리는 편이 낫다.
                val moved = lastBeta.isNaN() ||
                    abs(beta - lastBeta) >= MIN_DEGREES ||
                    abs(gamma - lastGamma) >= MIN_DEGREES
                if (!moved) return

                // 화면이 그리는 것보다 자주 보낼 이유가 없다.
                val now = SystemClock.uptimeMillis()
                if (now - lastSentAt < MIN_INTERVAL_MS) return

                lastBeta = beta
                lastGamma = gamma
                lastSentAt = now
                callback(beta, gamma)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        // 화면이 보일 때만. onStop 에서 떼지 않으면 도감을 닫아도 센서가 계속 돈다.
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START ->
                    manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
                Lifecycle.Event.ON_STOP -> manager.unregisterListener(listener)
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            owner.lifecycle.removeObserver(observer)
            manager.unregisterListener(listener)
        }
    }
}

private const val RAD_TO_DEG = 57.29578f

/**
 * 이만큼은 움직여야 내보낸다 (도).
 *
 * 저쪽은 20도를 기울이면 카드가 끝까지 돈다(`TILT_RANGE`). 0.2도면 그 100분의 1 이라
 * 눈에 안 보이고, 대신 가만히 든 손의 떨림을 전부 걸러 준다.
 */
private const val MIN_DEGREES = 0.2f

/** 이 간격보다 자주는 안 보낸다 (ms). 화면이 그리는 주기보다 촘촘할 이유가 없다. */
private const val MIN_INTERVAL_MS = 16L
