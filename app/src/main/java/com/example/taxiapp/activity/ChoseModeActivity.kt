package com.example.taxiapp.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.example.taxiapp.R

class ChoseModeActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_mode)
    }

    fun goToDriverSignIn(view: View) {
        startActivity(Intent(this@ChoseModeActivity,DriverSignInActivity::class.java))
    }
    fun goToPassengerSignIn(view: View) {
        startActivity(Intent(this@ChoseModeActivity,PassengerSignInActivity::class.java))
    }
}