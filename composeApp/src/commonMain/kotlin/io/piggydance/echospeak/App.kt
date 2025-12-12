package io.piggydance.echospeak

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import compose.icons.FeatherIcons
import compose.icons.feathericons.Settings
import echospeak.composeapp.generated.resources.Res
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun App() {
    // 状态管理
    var isDarkMode by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = if (isDarkMode) MaterialTheme.colorScheme else MaterialTheme.colorScheme
    ) {
        val mainViewModel = koinViewModel<MainViewModel>()
        val state by mainViewModel.state.collectAsStateWithLifecycle()

        Scaffold(
            topBar = {
                androidx.compose.material3.TopAppBar(
                    title = { Text("EchoSpeak") },
                    actions = {
                        IconButton(onClick = { /* 打开设置 */ }) {
                            Icon(FeatherIcons.Settings, contentDescription = "设置")
                        }
                    }
                )
            }
        ) { innerPadding ->
            LottieAnimation()
        }
    }
}

@Composable
fun LottieAnimation() {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.DotLottie(
            archive = Res.readBytes("files/voice.lottie")
        )
    }

    Box(
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = rememberLottiePainter(
                composition = composition,
                iterations = Compottie.IterateForever,
            ),
            contentDescription = "LottieFiles animation",
        )
    }
}
