package pt.setralarm.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import pt.setralarm.R
import pt.setralarm.util.IOnBackPressed


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }


    override fun onBackPressed() {
        val fragment = this.supportFragmentManager.findFragmentById(R.id.dashboardFragment)
        (fragment as? IOnBackPressed)?.onBackPressed()?.not()?.let {
            super.onBackPressed()
        }
    }

}