package com.example.taxiapp.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.taxiapp.R
import java.lang.Exception

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val thread = object : Thread() {
            override fun run() {
                super.run()
                try {
                    //поток засыпает на 5 сек и затем запускается finally
                    sleep(3000)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    startActivity(Intent(this@SplashScreenActivity, ChoseModeActivity::class.java))
                }
            }
        }
        thread.start()
    }

    override fun onPause() {
        super.onPause()
        //закончить активити что бы не висело в паматя
        finish()
    }
}