package pt.setralarm.presentation.viewmodel

import android.os.CountDownTimer
import android.util.Log
import android.view.View
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import pt.setralarm.MyApplication
import pt.setralarm.R
import pt.setralarm.domain.MqqtService
import pt.setralarm.util.AlarmMode
import pt.setralarm.util.Event

class DashboardViewModel : ViewModel(){

    companion object {
        const val TAG = "DashboardViewModel"
        private val COUNT_DOWN = 10000L
        private val TIME_FOR_UPDATE = 1000L
    }

    private var countDownTimer: CountDownTimer? = null

    var alarmStatus = ObservableField<AlarmMode>().apply { set(AlarmMode.DISARMED) }

    private val _alarmModeType = MutableLiveData<AlarmMode>(AlarmMode.DISARMED)
    val  alarmModeType : LiveData<AlarmMode> = _alarmModeType

    private val _timerText = MutableLiveData<String>()
    val timerText : LiveData<String> = _timerText

    private val _requestPinCode = MutableLiveData<Event<Unit>>()
    val  requestPinCode : LiveData<Event<Unit>> = _requestPinCode

//    private val _displayTypedPin = MutableLiveData<Event<String>>()
//    val displayTypedPin : LiveData<Event<String>> = _displayTypedPin

    val displayTypedPin= ObservableField<String>().apply { set("") }

    private val _isSystemBlocked = MutableLiveData<Event<Boolean>>()
    val isSystemBlocked : LiveData<Event<Boolean>> = _isSystemBlocked

    private val _instructionMsg = MutableLiveData<Event<String>>()
    val instructionMsg : LiveData<Event<String>> = _instructionMsg

    //for test
    var isSystemBlockedTest = true

    private var cachedPin = ""
    private var typedPin = ""
    private var cachedRequest = ""


    init {
        countDownTimer = object : CountDownTimer(COUNT_DOWN,TIME_FOR_UPDATE) {
            override fun onTick(millisUntilFinished: Long) {
                _timerText.value = ((millisUntilFinished/1000).toFloat()+1).toInt().toString()
            }
            override fun onFinish() {

            }
        }
    }


    fun selectedMode(alarmMode: AlarmMode, isExternal:Boolean) : Boolean{
       if(!isExternal) {
           cachedRequest = alarmMode.getRequestMessage()
           _requestPinCode.value = Event(Unit)
           if(!isSystemBlockedTest) requestStatusChange(alarmMode)
       } else {
           alarmStatus.apply { set(alarmMode) }
           _alarmModeType.value = alarmStatus.get()
           Log.d(TAG, alarmMode.name)
       }
        return true
    }

    private fun requestStatusChange(alarmMode: AlarmMode) {
        MyApplication.mqqtService.publish(alarmMode.getRequestMessage()+cachedPin)
    }

    fun setTypedPin(text: CharSequence?) {
        typedPin = text.toString()
    }

    fun getCurrentPin(): String {
        val pin = typedPin
        typedPin = ""
        return pin
    }

    fun getCachedRequest(): String {
        val cache = cachedRequest
        cachedRequest = ""
        return cache
    }

    fun getKeyAction(it: String) {
        when(it){
            "OK"->{
                when(typedPin.length) {
                    0 ->  {
                        setInstructionMessage(if(isSystemBlockedTest) "Digite o seu pin primeiro." else "Sistema já desbloqueado.")
                    }
                    4 -> {
                        Log.d("Keyboard", typedPin)
                        isSystemBlockedTest = !isSystemBlockedTest
                        _isSystemBlocked.value = Event(isSystemBlockedTest)
                        displayTypedPin.apply { set("") }

                        if(isSystemBlockedTest){
                            cachedPin =  ""
                            setInstructionMessage("Bloqueado.")
                        } else {
                            cachedPin = typedPin
                            setInstructionMessage("Desbloqueado.")
                        }
                        typedPin = ""
                        //request pin is valid
                    }
                }
            }
            "X" ->{
                typedPin = ""
                displayTypedPin.apply { set("") }
            }
            else ->{
                if(typedPin.length<4) typedPin+=it
                displayTypedPin.apply { set(typedPin) }
            }
        }
    }


    private fun setInstructionMessage(msg : String){
        _instructionMsg.value = Event(msg)
    }

}