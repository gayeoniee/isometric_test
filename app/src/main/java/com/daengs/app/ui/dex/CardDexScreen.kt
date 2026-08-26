package com.daengs.app.ui.dex

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

// ---------------------------------------------------------------------------
// 네오 채소 도감 — 저쪽 웹 데모를 그대로 실는다
//
// 카드 12장의 홀로그램 포일은 CSS 블렌드 모드(color-dodge · hard-light)와 다중
// 그라디언트를 포인터 위치로 굴리는 것이다. 네이티브로 옮기면 옮기는 게 아니라
// **다시 유도하는** 일이 되고, 저쪽이 원화에 맞춰 오래 조정해 둔 세기 값을 처음부터
// 다시 맞춰야 한다. 그래서 폴더째 실었다.
//
// **저쪽 파일은 한 글자도 고치지 않는다.** 저쪽이 갱신하면 통째로 갈아끼울 수 있어야
// 한다. 필요한 조정은 전부 이 파일에서 한다.
//
// ## 서버가 아니다
//
// [WebViewAssetLoader] 는 APK 안의 `assets/` 를 https 주소로 얹어 주는 **로더**다.
// `appassets.androidplatform.net` 은 실제로 없는 주소이고, 안드로이드가 네트워크로
// 나가기 전에 가로챈다. 개발 서버도 포트도 인터넷 권한도 없다 — 비행기 모드에서도
// 똑같이 돈다.
//
// 그런데 왜 굳이 https 인가. **`file://` 에서는 ES 모듈이 CORS 로 막히기 때문이다.**
// 저쪽 index.html 이 `<script type="module">` 을 쓰므로, file:// 로 열면 카드가 한
// 장도 안 뜬다.
// ---------------------------------------------------------------------------

/** 저쪽 `theme-color`. 로딩 중 흰 화면이 번쩍이지 않게 배경을 미리 맞춘다. */
private val DexBackground = Color(0xFF0E0B05)

private const val ASSET_ORIGIN = "https://appassets.androidplatform.net"
private const val DEX_URL = "$ASSET_ORIGIN/assets/neo-hologram/index.html"

/**
 * 도감 화면. [onClose] 는 방으로 돌아갈 때 불린다.
 *
 * 닫는 길이 셋이다 — 안드로이드 뒤로가기 · 페이지 왼쪽 위 `← DAENGS` · 확대 뷰의 ✕.
 * 앞의 둘을 여기서 받는다.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CardDexScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val loader = remember(context) {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }

    // 뒤로가기·자이로가 이 인스턴스를 잡고 있어야 해서 밖에 둔다.
    val webView = remember(context) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(AndroidColor.parseColor("#0E0B05"))
            settings.javaScriptEnabled = true
            // 저쪽이 본 카드를 기억하는 데 쓴다. 없으면 조용히 실패한다.
            settings.domStorageEnabled = true
            // 저쪽이 자기 뷰포트를 직접 잡는다. 여기서 겹쳐 잡으면 카드가 작아진다.
            settings.loadWithOverviewMode = false
            settings.useWideViewPort = false
            isVerticalScrollBarEnabled = false

            webViewClient = object : WebViewClientCompat() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? = loader.shouldInterceptRequest(request.url)

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    // 저쪽 껍데기의 `<a class="back" href="/">← DAENGS</a>`.
                    // 웹에서는 서비스 첫 화면으로 가는 링크인데, 앱에는 그런 페이지가
                    // 없어서 그대로 두면 빈 화면이 뜬다. 방으로 돌려보낸다.
                    val url = request.url.toString()
                    if (url == "$ASSET_ORIGIN/" || url == ASSET_ORIGIN) {
                        onClose()
                        return true
                    }
                    return false
                }
            }
            loadUrl(DEX_URL)
        }
    }

    // 확대 뷰가 열려 있으면 그것만 닫고, 아니면 방으로 나간다.
    //
    // 저쪽은 확대 뷰를 `<dialog>` 로 만든다. 안드로이드 뒤로가기는 Esc 로 안 가므로
    // 저절로 닫히지 않는다. **저쪽 파일을 고치지 않고** 밖에서 닫는다.
    BackHandler {
        webView.evaluateJavascript(
            "(function(){var d=document.querySelector('dialog[open]');" +
                "if(d){d.close();return true}return false})()",
        ) { result -> if (result != "true") onClose() }
    }

    // 폰을 기울이면 카드가 따라 기운다. 저쪽이 확대한 한 장에만 적용한다.
    DeviceTilt { beta, gamma ->
        webView.evaluateJavascript("window.__neoTilt&&window.__neoTilt($beta,$gamma)", null)
    }

    Box(modifier.fillMaxSize().background(DexBackground)) {
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
    }
}
