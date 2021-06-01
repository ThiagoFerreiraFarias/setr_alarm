package pt.setralarm.domain

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class SharedPreferenceRepository (private val application: Context) {

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(application)
    }

    fun getStoredPin(): String = sharedPreferences.getString(PIN, "").orEmpty()

    fun storePint(pin : String) {
        sharedPreferences.edit().putString(PIN, pin).apply()
    }

    companion object{

        private const val PIN = "pin"

        @Volatile
        private var instance: SharedPreferenceRepository? = null

        @JvmStatic
        fun getInstance(application: Context) = instance ?: synchronized(this) {
            instance ?: SharedPreferenceRepository(application).also { instance = it }
        }
    }

}