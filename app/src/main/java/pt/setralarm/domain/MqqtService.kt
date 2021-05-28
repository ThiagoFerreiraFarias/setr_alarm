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

class MqqtService(val context: Context) {

    companion object {
        const val TAG = "AndroidMqttClient"
        const val SERVER_URI = "tcp://public.mqtthq.com"
        const val SERVER_PORT = "1883"

        private var _msgFeed = MutableLiveData<String>()
        val msgFeed : LiveData<String> =_msgFeed

        var externalModeStatus : ((AlarmMode)->Unit) = {}
    }

    private val clientId: String = MqttClient.generateClientId()
    private val serverURI = "$SERVER_URI:$SERVER_PORT"
    var client : MqttAndroidClient = MqttAndroidClient(context, serverURI,clientId)

    private var isInternalMessage = false

    var attempt = 0
    var checkSubscription : ((Boolean) -> Unit)? =null


    fun connect() : Boolean {
        var isConnected = false
        client.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d(TAG, "Receive message: ${message.toString()} from topic: $topic")
                if(!isInternalMessage) {
                    message?.let {
                        externalModeStatus.invoke(
                            when (it.toString()) {
                                "Arming System\r" -> {
                                    isInternalMessage = false
                                    AlarmMode.ARMED_FULL
                                }
                                "Arming HOME System\r" -> {
                                    isInternalMessage = false
                                    AlarmMode.ARMED_IN_HOME
                                }
                                "Disarming System\r" -> {
                                    isInternalMessage = false
                                    AlarmMode.DISARMED
                                }
                                else -> {
                                    isInternalMessage = false
                                    AlarmMode.UNKNOWN
                                }
                            }
                        )
                    }

                } else {
                    setMessageInFeed("Receive message: ${message.toString()} from topic: $topic")
                    isInternalMessage = false
                }

            }

            override fun connectionLost(cause: Throwable?) {
                Log.d(TAG, "Connection lost ${cause.toString()}")
                setMessageInFeed("Connection lost ${cause.toString()}")
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
}