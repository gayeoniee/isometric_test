package com.daengs.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.daengs.app.ui.dex.CardDexScreen
import com.daengs.app.ui.home.HomeScreen
import com.daengs.app.ui.theme.DaengsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DaengsTheme {
                // 화면이 둘뿐이라 네비게이션 라이브러리를 넣지 않는다. 산책 게임까지
                // 생겨서 셋을 넘어가면 그때 넣는 게 맞다.
                var dexOpen by rememberSaveable { mutableStateOf(false) }

                if (dexOpen) {
                    CardDexScreen(onClose = { dexOpen = false })
                } else {
                    HomeScreen(onOpenDex = { dexOpen = true })
                }
            }
        }
    }
}
