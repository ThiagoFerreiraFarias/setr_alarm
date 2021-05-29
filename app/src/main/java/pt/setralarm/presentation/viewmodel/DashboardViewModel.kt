package pt.setralarm.presentation.viewmodel

import android.os.CountDownTimer
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import pt.setralarm.MyApplication
import pt.setralarm.util.AlarmMode
import pt.setralarm.util.Event

class DashboardViewModel : ViewModel() {

    companion object {
        const val TAG = "DashboardViewModel"
        private val COUNT_DOWN = 10000L
        private val TIME_FOR_UPDATE = 1000L
    }

    private var countDownTimer: CountDownTimer? = null

    var alarmStatus = ObservableField<AlarmMode>().apply { set(AlarmMode.DISARMED) }

    private val _alarmModeType = MutableLiveData<AlarmMode>(AlarmMode.DISARMED)
    val alarmModeType: LiveData<AlarmMode> = _alarmModeType

    private val _timerText = MutableLiveData<String>()
    val timerText: LiveData<String> = _timerText

    val displayTypedPin = ObservableField<String>().apply { set("") }

    private val _isSystemBlocked = MutableLiveData<Event<Boolean>>()
    val isSystemBlocked: LiveData<Event<Boolean>> = _isSystemBlocked

    private val _instructionMsg = MutableLiveData<Event<String>>()
    val instructionMsg: LiveData<Event<String>> = _instructionMsg

    private var isSystemBlockedFlag = true
    private var pinAttempet = 3
    private var cachedPin = ""
    private var typedPin = ""
    private var cachedRequest = ""


    init {
        countDownTimer = object : CountDownTimer(COUNT_DOWN, TIME_FOR_UPDATE) {
            override fun onTick(millisUntilFinished: Long) {
                _timerText.value = ((millisUntilFinished / 1000).toFloat() + 1).toInt().toString()
            }

            override fun onFinish() {

            }
        }
    }


    fun selectedMode(alarmMode: AlarmMode, isExternal: Boolean): Boolean {
        if (alarmMode == alarmStatus.get()) return false
        if (!isExternal) {
            cachedRequest = alarmMode.getRequestMessage()
            if (!isSystemBlockedFlag) sendMessage(alarmMode.getRequestMessage() + cachedPin, true)
            cachedRequest = ""
        } else {
            alarmStatus.apply { set(alarmMode) }
            _alarmModeType.value = alarmStatus.get()
            Log.d(TAG, alarmMode.name)
        }
        return true
    }

    fun getKeyAction(it: String) {
        when (it) {
            "OK" -> {
                when (typedPin.length) {
                    0 -> {
                        setInstructionMessage(if (isSystemBlockedFlag) "Digite o seu pin primeiro." else "Sistema já desbloqueado.")
                    }
                    4 -> {
                        sendMessage("!pin $typedPin")
                    }
                }

            }
            "X" -> {
                typedPin = ""
                displayTypedPin.apply { set(typedPin) }
            }
            else -> {
                if (typedPin.length < 4) typedPin += it
                displayTypedPin.apply { set(typedPin) }
            }
        }
    }


    private fun setInstructionMessage(msg: String) {
        _instructionMsg.value = Event(msg)
    }

    private fun sendMessage(msg: String, isInternal: Boolean? = true) {
        MyApplication.mqttService.publish(msg, isInternal = isInternal)
    }

    fun setPinvalidation(isPinValid: Boolean) {
        if (isPinValid) {
            isSystemBlockedFlag = !isSystemBlockedFlag
            _isSystemBlocked.value = Event(isSystemBlockedFlag)
            displayTypedPin.apply { set("") }
            pinAttempet = 3

            if (isSystemBlockedFlag) {
                cachedPin = ""
                setInstructionMessage("Bloqueado.")
            } else {
                cachedPin = typedPin
                setInstructionMessage("Desbloqueado.")
            }
            typedPin = ""
        } else {
            typedPin = ""
            if (pinAttempet > 0) {
                pinAttempet--
                setInstructionMessage("Pin inválido. Tentativas restantes $pinAttempet")
            } else {
                //sends message to trigger alarm
            }
        }
        displayTypedPin.apply { set(typedPin) }
    }

}