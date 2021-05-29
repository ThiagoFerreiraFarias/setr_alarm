package pt.setralarm.domain

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import pt.setralarm.MyApplication
import pt.setralarm.R
import pt.setralarm.util.AlarmMode

class MqttService(val context: Context) {

    companion object {
        const val TAG = "AndroidMqttClient"
        const val SERVER_URI = "tcp://public.mqtthq.com"
        const val SERVER_PORT = "1883"


    }

    private val clientId: String = MqttClient.generateClientId()
    private val serverURI = "$SERVER_URI:$SERVER_PORT"
    var client : MqttAndroidClient = MqttAndroidClient(context, serverURI,clientId)

    var externalModeStatus : ((AlarmMode)->Unit) = {}
    var pinValidation : ((Boolean)->Unit) = {}

    var alarmStatusCheck : ((String)->Unit) = {}
    var preAlarmeStatusCheck : ((String)->Unit) = {}
    var fireCheck : ((Boolean)->Unit) = {}
    var internalIntrusionCheck : ((Boolean)->Unit) = {}
    var externalIntrusionCheck : ((Boolean)->Unit) = {}

    private var _msgFeed = MutableLiveData<String>()
    val msgFeed : LiveData<String> =_msgFeed

    private var isInternalMessage = false

    var attempt = 0

    val regex5secCheck = """^(?<mode>[A-Z]+)\|(?<alarm>[A-Z]+)\|(?<prealarm>[A-Z]+)\|FIRE\:(?<fire>[0-1]+)\|HOME\:(?<home>[0-1]+)\|INTRUSION\:(?<intrusion>[0-1]+)\|TIMER\:(?<timer>[0-9]+)$""".toRegex()


    fun connect() : Boolean {
        var isConnected = false
        client.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d(TAG, "Receive message: ${message.toString()} from topic: $topic")
                message?.let {
                    var msg = it.toString().replace("\r","")

                        var regexResult = regex5secCheck?.matchEntire(msg)?.groupValues?.toList()

                        if(regexResult?.isNotEmpty() == true){
                            externalModeStatus.invoke(getModeByName(regexResult[1])) //modo alarme
                            alarmStatusCheck.invoke(regexResult[2]) //estado do alarme
                            preAlarmeStatusCheck.invoke(regexResult[3]) //estado do pré alarme
                            fireCheck.invoke(regexResult[4].toInt()==1) //alarme incendio 0/1
                            internalIntrusionCheck.invoke(regexResult[5].toInt()==1) //alarme intrusão interno 0/1
                            externalIntrusionCheck.invoke(regexResult[6].toInt()==1) //alarme intrusão externo 0/1
                            regexResult[7] //timer
                            Log.d("Regex",regexResult.toString())
                        } else if (msg.toLowerCase().startsWith("pin_")) {
                            pinValidation.invoke(msg.toLowerCase() == "pin_valid")
                        } else {
                            setMessageInFeed("Receive message: $msg from topic: $topic")
                        }
                        isInternalMessage = false
                }
            }

            override fun connectionLost(cause: Throwable?) {
                Log.d(TAG, "Connection lost ${cause.toString()}")
                setMessageInFeed("Connection lost ${cause.toString()}")
                connect()
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }
        })

        val options = MqttConnectOptions()
        try {
            client.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connection success")
                    setMessageInFeed("Connection success")
                    isConnected= true
                    trySubscribe()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Connection failure")
                    setMessageInFeed("Connection failure")
                    isConnected= false
                    connect()
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
        return isConnected
    }

    private fun trySubscribe() {
        if(attempt<3) {
            Log.d("Subscription", "Trying subscription - attempt: $attempt")
            subscribe(context.getString(R.string.subscribe_mqqt_tx))
            attempt++
        }
    }

    fun subscribe(topic: String, qos: Int = 0)  {
        try {
            client.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Subscribed to $topic")
                    setMessageInFeed("Subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to subscribe $topic")
                    setMessageInFeed("Failed to subscribe $topic")
                    trySubscribe()
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun unsubscribe(topic: String) {
        try {
            client.unsubscribe(topic, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Unsubscribed to $topic")
                    setMessageInFeed("Unsubscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to unsubscribe $topic")
                    setMessageInFeed("Failed to unsubscribe $topic")
                }
            })

        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }


    fun publish(msg: String, qos: Int = 0, retained: Boolean = false, isInternal:Boolean?=true) {
        isInternalMessage = isInternal?:true
        var topic = MyApplication.instance.getString(R.string.send_msg_mqqt_rx)
        try {
            val message = MqttMessage()
            message.payload = msg.toByteArray()
            message.qos = qos
            message.isRetained = retained
            client.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "$msg published to $topic")
                    setMessageInFeed("$msg published to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to publish $msg to $topic")
                    setMessageInFeed("Failed to publish $msg to $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            client.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Disconnected")
                    setMessageInFeed("Disconnected")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to disconnect")
                    setMessageInFeed("Failed to disconnect")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }


    private fun setMessageInFeed(msg : String){
        _msgFeed.value = msg
    }

    private fun getModeByName(mode: String): AlarmMode {
       return  when(mode){
            "ARMED" -> AlarmMode.ARMED_FULL
            "ARMEDHOME" -> AlarmMode.ARMED_IN_HOME
            "DISARMED" -> AlarmMode.DISARMED
            else ->  AlarmMode.UNKNOWN
        }
    }
}