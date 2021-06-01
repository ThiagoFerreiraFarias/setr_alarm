package pt.setralarm.presentation.viewmodel

import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import pt.setralarm.MyApplication
import pt.setralarm.domain.SharedPreferenceRepository
import pt.setralarm.util.AlarmMode
import pt.setralarm.util.Event

class DashboardViewModel : ViewModel() {

    companion object {
        const val TAG = "DashboardViewModel"
        private val COUNT_DOWN = 5000L
        private val COUNT_DOWN_HANDLE = 8000L
        private val TIME_FOR_UPDATE = 1000L
    }

    private var countDownTimer: CountDownTimer? = null
    private val preferences =
        SharedPreferenceRepository.getInstance(MyApplication.applicationContext())

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

    private val _changeAlarmIcon = MutableLiveData<Event<Unit>>()
    val changeAlarmIcon: LiveData<Event<Unit>> = _changeAlarmIcon

    private var isSystemBlockedFlag = true
    private var pinAttempet = 3
    private var cachedPin = ""
    private var typedPin = ""
    private var cachedRequest = ""


    init {
        countDownTimer = object : CountDownTimer(COUNT_DOWN, TIME_FOR_UPDATE) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                _changeAlarmIcon.value = Event(Unit)
            }
        }
        requestStatus()
    }

    private fun requestStatus(){
        Handler(Looper.getMainLooper()).postDelayed({
            sendMessage("!status")
            requestStatus()
        }, COUNT_DOWN_HANDLE)
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

    fun setPinValidation(isPinValid: Boolean) {
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
                storePin(cachedPin)
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

    private fun storePin(pin: String) {
        preferences.storePint(pin)
    }

    fun onPanicBtnClick() {
        sendMessage("!panic ${preferences.getStoredPin()}")
    }

    fun onRefreshPinClick() {
        if (!isSystemBlockedFlag) {
            if (typedPin.isEmpty()) {
                _instructionMsg.value = Event("Digite o novo pin e refresh button.")
            } else {
                sendMessage("!set $cachedPin pin $typedPin")
                preferences.storePint(typedPin)
                cachedPin = typedPin
                typedPin = ""
                displayTypedPin.apply { set(typedPin) }
            }
        } else {
            _instructionMsg.value = Event("Digite o seu pin primeiro.")
        }
    }

    fun startTimer() {
        countDownTimer!!.start()
    }

    fun cancelTimer() {
        countDownTimer!!.cancel()
    }

    fun submitPin() {
        getKeyAction(typedPin)
    }

    fun resetAlarm() {
        sendMessage("!set $cachedPin ALARM 0")
    }
}