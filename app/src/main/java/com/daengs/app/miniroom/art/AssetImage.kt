package com.daengs.app.miniroom.art

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/**
 * `assets/` 안의 그림을 읽는다. 리소스가 아니라 **에셋**이라 [android.content.res.AssetManager]
 * 를 거쳐야 한다.
 *
 * 도감 웹 데모의 그림을 방 안 액자에도 쓰려고 만들었다. 같은 파일을 리소스로 한 벌 더
 * 넣으면 APK 만 커지고 저쪽이 그림을 갱신할 때 두 곳을 고쳐야 한다.
 *
 * 파일이 없거나 못 읽으면 **null 을 준다.** 액자는 그림 없이도 그려지므로 앱이 죽는
 * 것보다 낫다.
 *
 * @param sample 2 면 절반 크기로 읽는다. 액자는 작게 그리므로 원본을 다 들 이유가 없다.
 */
@Composable
fun rememberAssetImage(path: String, sample: Int = 1): ImageBitmap? {
    val context = LocalContext.current
    return remember(path, sample) {
        runCatching {
            context.assets.open(path).use { stream ->
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
            }
        }.getOrNull()
    }
}
