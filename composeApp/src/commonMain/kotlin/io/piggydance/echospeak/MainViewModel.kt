package io.piggydance.echospeak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.piggydance.echospeak.speechecho.EchoConfig
import io.piggydance.echospeak.speechecho.EchoMode
import io.piggydance.echospeak.speechecho.SpeechEcho
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    fun switchToDelayMode() {
        // 这里不够优雅, 需要增加一个配置的builder
        viewModelScope.launch(Dispatchers.IO) {
            SpeechEcho.instance.turnOn(EchoConfig(mode = EchoMode.Delay))
        }
    }

    fun switchToSentenceMode() {
        // 这里不够优雅, 需要增加一个配置的builder
        viewModelScope.launch(Dispatchers.IO) {
            SpeechEcho.instance.turnOn(EchoConfig(mode = EchoMode.Sentence))
        }
    }

    fun switchToOffMode() {
        viewModelScope.launch(Dispatchers.IO) {
            SpeechEcho.instance.turnOff()
        }
    }
}
