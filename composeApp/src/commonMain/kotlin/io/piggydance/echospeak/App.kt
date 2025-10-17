package io.piggydance.echospeak

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Mic
import compose.icons.feathericons.MicOff
import compose.icons.feathericons.Settings
import org.jetbrains.compose.ui.tooling.preview.Preview

// 回声模式枚举
enum class EchoMode {
    DIRECT, // 直接回响
    DELAYED // 说完后复读
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun App() {
    // 状态管理
    var isRecording by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf(EchoMode.DIRECT) }
    var isDarkMode by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = if (isDarkMode) MaterialTheme.colorScheme else MaterialTheme.colorScheme
    ) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // 应用标题
                Text(
                    text = "回声说话",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 模式选择卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "选择回声模式",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // 直接回声模式
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMode == EchoMode.DIRECT,
                                onClick = { selectedMode = EchoMode.DIRECT }
                            )
                            Text(
                                text = "回声模式",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        // 延迟回声模式
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMode == EchoMode.DELAYED,
                                onClick = { selectedMode = EchoMode.DELAYED }
                            )
                            Text(
                                text = "断句模式",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 录音状态显示
                Card(
                    modifier = Modifier
                        .size(200.dp)
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRecording) Color(0xFFEF5350) else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isRecording) FeatherIcons.Mic else FeatherIcons.MicOff,
                            contentDescription = if (isRecording) "停止录音" else "开始录音",
                            tint = Color.White,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 录音状态文本
                Text(
                    text = if (isRecording) "正在录音..." else "点击麦克风开始录音",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 开始/停止按钮
                Button(
                    onClick = { isRecording = !isRecording },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = if (isRecording) "停止录音" else "开始录音",
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // 当前模式显示
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = "当前模式: ${if (selectedMode == EchoMode.DIRECT) "回声模式" else "断句模式"}",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}