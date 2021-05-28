package pt.setralarm

import android.app.Application
import android.content.Context
import pt.setralarm.domain.MqqtService

class MyApplication : Application() {


    companion object{
        lateinit var instance : MyApplication
        lateinit var mqqtService : MqqtService

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
        mqqtService = MqqtService(context)
        mqqtService.connect()

    }

}