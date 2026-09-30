package io.piggydance.echospeak

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.HelpCircle
import compose.icons.feathericons.Settings
import io.piggydance.echospeak.audio.VadType

internal val EchoCyan = Color(0xFF00C8FF)
internal val EchoBackground = Color(0xFF080C1A)
private val Panel = Color(0xFF101A30)

@Composable
internal fun EchoHomeHeader(onSettings: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("EchoSpeak", fontSize = 23.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
        IconButton(
            onClick = onSettings,
            modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.06f), CircleShape),
        ) {
            Icon(FeatherIcons.Settings, stringResource(R.string.settings_title), tint = EchoCyan)
        }
    }
}

@Composable
internal fun EchoSettingsScreen(
    selectedVad: VadType,
    onVadSelected: (VadType) -> Unit,
    onBack: () -> Unit,
) {
    var showGuide by rememberSaveable { mutableStateOf(false) }
    BackHandler { onBack() }
    Column(Modifier.fillMaxSize().background(EchoBackground).safeDrawingPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(FeatherIcons.ArrowLeft, stringResource(R.string.action_back), tint = EchoCyan)
            }
            Text(
                stringResource(R.string.settings_title),
                modifier = Modifier.padding(start = 8.dp),
                fontFamily = FontFamily.Default,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsLabel(stringResource(R.string.settings_voice_detection))
                Surface(color = Panel, shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.selectableGroup()) {
                        VadType.entries.forEachIndexed { index, vad ->
                            val selected = selectedVad == vad
                            Row(
                                Modifier.fillMaxWidth()
                                    .selectable(selected, onClick = { onVadSelected(vad) }, role = Role.RadioButton)
                                    .padding(horizontal = 20.dp, vertical = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = EchoCyan,
                                        unselectedColor = Color(0xFF70809A),
                                    ),
                                )
                                Text(
                                    when (vad) {
                                        VadType.SILERO -> "Silero"
                                        VadType.WEBRTC -> "WebRTC"
                                        VadType.YAMNET -> "YAMNet"
                                    },
                                    fontFamily = FontFamily.Default,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 18.sp,
                                    color = if (selected) Color.White else Color(0xFFB1BDD1),
                                )
                            }
                            if (index != VadType.entries.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    color = Color.White.copy(alpha = 0.06f),
                                )
                            }
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = { showGuide = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EchoCyan),
            ) {
                Icon(FeatherIcons.HelpCircle, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.guide_title), fontFamily = FontFamily.Default, fontSize = 16.sp)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (showGuide) EchoGuideDialog(onDismiss = { showGuide = false })
}

@Composable
private fun SettingsLabel(text: String) {
    Text(text, color = Color(0xFF91A2BE), fontFamily = FontFamily.Default, fontSize = 16.sp)
}

/** Longer explanations live here, never below the live recording spectrum. */
@Composable
internal fun EchoGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        titleContentColor = Color.White,
        textContentColor = Color(0xFFB8C6DC),
        icon = { Icon(FeatherIcons.HelpCircle, null, tint = EchoCyan) },
        title = { Text(stringResource(R.string.guide_title), fontFamily = FontFamily.Default) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                GuideSection(R.string.guide_speaking_title, R.string.guide_speaking_body)
                GuideSection(R.string.settings_voice_detection, R.string.guide_detection_body)
                GuideSection(R.string.settings_noise_reduction, R.string.guide_noise_body)
                GuideSection(R.string.guide_privacy_title, R.string.guide_privacy_body)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = EchoCyan)) {
                Text(stringResource(R.string.action_done), fontFamily = FontFamily.Default)
            }
        },
    )
}

@Composable
private fun GuideSection(title: Int, body: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(title), color = Color.White, fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold)
        Text(stringResource(body), fontFamily = FontFamily.Default, fontSize = 16.sp, lineHeight = 23.sp)
    }
}
