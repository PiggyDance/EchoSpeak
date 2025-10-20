package io.piggydance.echospeak

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.piggydance.echospeak.speechecho.EchoConfig
import io.piggydance.echospeak.speechecho.EchoMode
import io.piggydance.echospeak.speechecho.SpeechEcho
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

@Stable
data class MainState(
    val counting: Int,
)

class MainViewModel : ViewModel(), KoinComponent {

    private val _state = MutableStateFlow(MainState(0))
    val state: StateFlow<MainState> = _state.asStateFlow()

    fun switchToDelayMode() {
        _state.value = _state.value.copy(counting = _state.value.counting + 1)
        // 这里不够优雅, 需要增加一个配置的builder
        viewModelScope.launch(Dispatchers.IO) {
            SpeechEcho.instance.turnOn(EchoConfig(mode = EchoMode.Delay))
        }
    }

    fun switchToSentenceMode() {
        _state.value = _state.value.copy(counting = _state.value.counting + 1)
        // 这里不够优雅, 需要增加一个配置的builder
        viewModelScope.launch(Dispatchers.IO) {
            SpeechEcho.instance.turnOn(EchoConfig(mode = EchoMode.Sentence))
        }
    }

    fun switchToOffMode() {
        _state.value = _state.value.copy(counting = _state.value.counting + 1)
        viewModelScope.launch(Dispatchers.IO) {
            SpeechEcho.instance.turnOff()
        }
    }
}
