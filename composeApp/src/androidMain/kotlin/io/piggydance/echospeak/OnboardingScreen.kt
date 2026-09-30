package io.piggydance.echospeak

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One permission gate, with optional guidance instead of stacked intro overlays. */
@Composable
fun OnboardingOverlay(
    visible: Boolean,
    onRequestPermission: () -> Unit,
    isPermanentlyDenied: Boolean = false,
) {
    if (!visible) return
    var showGuide by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(EchoBackground).safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("EchoSpeak", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
            // Reuse the original breathing circular spectrum rather than a new style.
            SciFiAudioVisualizer(audioMode = AudioMode.IDLE, spectrum = List(60) { 0f })
        }
        Text(
            stringResource(if (isPermanentlyDenied) R.string.permission_blocked_title else R.string.permission_title),
            fontFamily = FontFamily.Default,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(if (isPermanentlyDenied) R.string.permission_blocked_body else R.string.permission_body),
            fontFamily = FontFamily.Default,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = Color(0xFF91A2BE),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EchoCyan, contentColor = EchoBackground),
        ) {
            Text(
                stringResource(if (isPermanentlyDenied) R.string.permission_open_settings else R.string.permission_allow),
                fontFamily = FontFamily.Default,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { showGuide = true }) {
            Text(stringResource(R.string.guide_title), fontFamily = FontFamily.Default, color = EchoCyan, fontSize = 16.sp)
        }
    }
    if (showGuide) EchoGuideDialog(onDismiss = { showGuide = false })
}

@Composable
fun OnboardingPermissionHandler(content: @Composable () -> Unit) {
    RecordAudioPermissionHandler(autoRequest = false, content = content)
}
