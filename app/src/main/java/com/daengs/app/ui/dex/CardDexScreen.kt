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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
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

/** 확대 뷰가 열렸는지 확인하는 주기 (ms). */
private const val VIEWER_POLL_MS = 500L

/** 자바스크립트 문자열 리터럴로 감싼다. CSS 안의 따옴표·역슬래시를 안전하게 넘긴다. */
private fun quoteJs(text: String): String =
    "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

/** 저쪽 `theme-color`. 로딩 중 흰 화면이 번쩍이지 않게 배경을 미리 맞춘다. */
private val DexBackground = Color(0xFF0E0B05)

// ---------------------------------------------------------------------------
// 우리 쪽 조정
//
// 저쪽 파일을 안 고치고 얹는 것들이다. 폴더를 통째로 갈아끼워도 이 조정은 살아남는다.
// ---------------------------------------------------------------------------

/**
 * 포일 세기. 저쪽이 `rarity.css` 에 만들어 둔 손잡이를 쓴다. 1 이 저쪽 기본값이고
 * 낮출수록 차분해진다. **vendor 포일 11종이 전부 이 두 개를 읽는 것**을 확인했다
 * (`gold.css:41` 처럼 `calc(var(--card-opacity) * 0.9 * var(--hc-shine-opacity))`).
 *
 * **opacity 계열만 건드린다.** 저쪽 README 경고 때문이다 — 무늬가 구조인 포일
 * (`mosaic` 의 격자, `metal` 의 세로 결)은 `brightness` 로 누르면 색이 돌아오는 대신
 * 무늬가 같이 뭉개져서, 그 티어를 고른 이유가 없어진다. `hard-light` 포일
 * (`gold` · `metal`)은 blend 가 곱하기 쪽으로 넘어가 탁해진다.
 *
 * No.01 배추만 예외다. 그 카드는 vendor 포일이 아니라 저쪽 자체 이머시브라 이 값을
 * 안 읽는다 — 조정해도 안 변하는 게 정상이다.
 */
private const val SHINE_OPACITY = "0.72"
private const val GLARE_OPACITY = "0.72"

/**
 * 폰에서 그리드를 **두 칸**으로.
 *
 * 이게 이번 작업에서 제일 크다. 저쪽 기본값은 `minmax(min(100%, 250px), 1fr)` 인데
 * 폰 뷰포트가 400px 남짓이라 **한 줄에 한 장**이 된다. 카드가 화면 폭을 꽉 채우니
 * 블렌드 레이어 4~5장이 전부 그 크기로 잡히고, 그래서
 * `tile memory limits exceeded` 가 난다.
 *
 * **`align-self: start` 가 꼭 필요하다.** 없으면 칸이 그 행에서 제일 큰 칸 높이로
 * 늘어난다. 저쪽은 슬롯을 `1fr auto` 두 줄로 짜고 그림 자리를 4:5 로 잡아 두는데,
 * 칸이 늘어나면 그 4:5 가 깨져 세로로 길어지고, 카드가 **높이 기준**으로 크기를
 * 잡으므로 (`height:100%` + `aspect-ratio: --ar`) 폭까지 같이 넓어져 칸을 넘친다.
 *
 * 실제로 그렇게 잘렸다. 하필 1행이 배추(No.01)와 페퍼인데, 배추만 "꾹 눌러서
 * 들어가기" 버튼이 있어 칸이 길고, 그래서 옆의 페퍼가 넘쳤다. 비율이 넓은 카드
 * (페퍼·가지·당근·단호박·상추 0.80, 나머지 0.72~0.725)일수록 크게 넘친다.
 * 1열일 때는 옆칸이 없어서 안 생기던 문제다.
 *
 * `minmax(0, 1fr)` 도 같이 필요하다. 그냥 `1fr` 은 칸의 최소 폭이 `auto` 라 내용보다
 * 작아지지 못한다.
 *
 * 두 칸으로 만들면 카드 면적이 **4분의 1** 이 된다. 포일을 제대로 보는 건 어차피
 * 확대 뷰이고 (저쪽 README: "이 데모는 카드를 보라고 만든 것이라 화면을 카드에 다
 * 준다"), 그리드는 모아둔 걸 훑는 자리라 두 칸이 도감답기도 하다.
 */
private const val GRID_CSS =
    "#dex{grid-template-columns:repeat(2,minmax(0,1fr))!important}" +
        "#dex > li{align-self:start}"

/**
 * 화면 밖 카드는 아예 그리지 않는다.
 *
 * `contain-intrinsic-size` 를 같이 줘야 한다 — 없으면 높이가 0 으로 잡혀 스크롤이 튄다.
 */
private const val OFFSCREEN_CSS =
    "#dex > li{content-visibility:auto;contain-intrinsic-size:auto 320px}"

/**
 * 확대 뷰가 열려 있는 동안에는 **뒤의 그리드를 아예 안 그린다.**
 *
 * 카드를 확대해 문지를 때가 제일 무겁다 (튐 10.9%, 그리드 스크롤은 1.8%). 화면을 꽉
 * 채운 카드의 블렌드 레이어 4~5장을 손가락이 움직일 때마다 전부 다시 칠하기 때문이다.
 * 그 뒤에 그리드까지 살아 있을 이유가 없다 — 다이얼로그가 덮고 있어 보이지도 않는데
 * 브라우저는 여전히 합성 대상으로 들고 있다.
 */
private const val VIEWER_CSS =
    "body:has(dialog[open]) #dex{content-visibility:hidden!important}"

/** 위 조정을 한 장으로 묶은 것. 페이지가 뜬 뒤 넣는다. */
private val TUNING_CSS =
    ".stage[data-effect]{--hc-shine-opacity:$SHINE_OPACITY;--hc-glare-opacity:$GLARE_OPACITY}" +
        GRID_CSS + OFFSCREEN_CSS + VIEWER_CSS

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

                override fun onPageFinished(view: WebView, url: String) {
                    // 저쪽 파일을 고치는 대신 스타일 한 장을 얹는다.
                    // 마지막에 넣으므로 특정도가 같아도 우리 값이 이긴다.
                    view.evaluateJavascript(
                        "(function(){var s=document.getElementById('daengs-tuning')||" +
                            "document.createElement('style');s.id='daengs-tuning';" +
                            "s.textContent=${quoteJs(TUNING_CSS)};" +
                            "document.head.appendChild(s)})()",
                        null,
                    )
                }

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

    // 확대 뷰가 열려 있는가. **자이로를 켤지 끌지가 여기 달렸다.**
    //
    // 저쪽은 기울기를 확대한 한 장에만 쓰고 그리드에서는 버린다. 그런데 우리가
    // 보내는 비용(프로세스를 넘는 evaluateJavascript)은 버려지든 말든 그대로 든다.
    // 그래서 열려 있을 때만 센서를 문다.
    //
    // 저쪽 파일에 신호를 심는 대신 밖에서 확인한다 — 뒤로가기에서 쓰는 것과 같은 수법.
    // 0.5초면 충분하다. 확대를 연 직후 반 박자 늦게 켜지지만, 그 사이에 폰을 기울이는
    // 사람은 없다.
    var viewerOpen by remember { mutableStateOf(false) }
    LaunchedEffect(webView) {
        while (true) {
            webView.evaluateJavascript(
                "!!document.querySelector('dialog[open]')",
            ) { result -> viewerOpen = result == "true" }
            delay(VIEWER_POLL_MS)
        }
    }

    // 폰을 기울이면 카드가 따라 기운다. 저쪽이 확대한 한 장에만 적용한다.
    DeviceTilt(enabled = viewerOpen) { beta, gamma ->
        webView.evaluateJavascript("window.__neoTilt&&window.__neoTilt($beta,$gamma)", null)
    }

    Box(modifier.fillMaxSize().background(DexBackground)) {
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
    }
}
