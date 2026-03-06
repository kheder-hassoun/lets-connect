package pro.devapp.walkietalkiek.service.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TalkingStateRepository {
    private val lock = Any()
    private val _isLocalTalking = MutableStateFlow(false)
    val isLocalTalking: StateFlow<Boolean> = _isLocalTalking.asStateFlow()

    private val _isRemoteTalking = MutableStateFlow(false)
    val isRemoteTalking: StateFlow<Boolean> = _isRemoteTalking.asStateFlow()

    private val _isAnyoneTalking = MutableStateFlow(false)
    val isAnyoneTalking: StateFlow<Boolean> = _isAnyoneTalking.asStateFlow()

    fun setLocalTalking(isTalking: Boolean) {
        synchronized(lock) {
            _isLocalTalking.value = isTalking
            _isAnyoneTalking.value = _isLocalTalking.value || _isRemoteTalking.value
        }
    }

    fun setRemoteTalking(isTalking: Boolean) {
        synchronized(lock) {
            _isRemoteTalking.value = isTalking
            _isAnyoneTalking.value = _isLocalTalking.value || _isRemoteTalking.value
        }
    }

    fun clear() {
        synchronized(lock) {
            _isLocalTalking.value = false
            _isRemoteTalking.value = false
            _isAnyoneTalking.value = false
        }
    }
}
