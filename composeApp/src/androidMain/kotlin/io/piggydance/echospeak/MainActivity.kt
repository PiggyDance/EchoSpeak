package io.piggydance.echospeak

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.piggydance.echospeak.audio.AudioSessionRunner
import io.piggydance.echospeak.audio.VadType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Shared across Activity recreation and permission-gate re-entry, so a new host
// also waits for the preceding host's native resources to finish closing.
private val voiceSessionRunner = AudioSessionRunner<Pair<Context, VadType>> { (context, vad) ->
    object : AudioSessionRunner.Session {
        private var controller: VoiceEchoController? = null

        override suspend fun start() = withContext(Dispatchers.IO) {
            controller = VoiceEchoController(context, vadType = vad)
            if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                try {
                    controller!!.start()
                } catch (e: SecurityException) {
                    // Re-throw to the effect's handler after the session finally block
                    // closes the just-created models. Covers a grant-revocation race.
                    throw e
                }
            } else {
                throw SecurityException("Microphone permission is not granted")
            }
        }

        override suspend fun close() = withContext(Dispatchers.IO) {
            controller?.release()
            controller = null
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The app keeps its original dark visual style even on a light system theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("echospeak_main", Context.MODE_PRIVATE)
        setContent {
            var selectedVadName by rememberSaveable {
                mutableStateOf(prefs.getString("vad_engine", VadType.SILERO.name)!!)
            }
            val selectedVad = VadType.entries.firstOrNull { it.name == selectedVadName }
                ?: VadType.SILERO
            var showSettings by rememberSaveable { mutableStateOf(false) }
            EchoSpeakTheme {
                OnboardingPermissionHandler {
                    // Keep this effect composed while settings are open. Back and
                    // selecting the current engine must not interrupt recording.
                    VoiceEchoEffect(selectedVad)
                    if (showSettings) {
                        EchoSettingsScreen(
                            selectedVad = selectedVad,
                            onVadSelected = { vad ->
                                if (vad != selectedVad) {
                                    prefs.edit().putString("vad_engine", vad.name).apply()
                                    selectedVadName = vad.name
                                }
                            },
                            onBack = { showSettings = false },
                        )
                    } else {
                        App {
                            EchoHomeHeader(
                                onSettings = { showSettings = true },
                                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun VoiceEchoEffect(vadType: VadType) {
        val context = applicationContext
        LaunchedEffect(vadType) {
            try {
                voiceSessionRunner.run(context to vadType)
            } catch (e: SecurityException) {
                // Permission revocation may race with the permission wrapper.
                android.util.Log.w("EchoSpeak", "Microphone permission was revoked", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
