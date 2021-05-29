package pt.setralarm

import android.app.Application
import android.content.Context
import pt.setralarm.domain.MqttService

class MyApplication : Application() {


    companion object{
        lateinit var instance : MyApplication
        lateinit var mqttService : MqttService

        fun applicationContext(): Context {
            return instance.applicationContext
        }
    }

    init {
        instance=this
    }

    override fun onCreate() {
        super.onCreate()
        createMqqtService(applicationContext())

    }

    private fun createMqqtService(context: Context) {
        mqttService = MqttService(context)
        mqttService.connect()
    }

}