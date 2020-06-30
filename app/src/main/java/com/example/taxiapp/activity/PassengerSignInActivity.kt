package com.example.taxiapp.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.taxiapp.Model.User
import com.example.taxiapp.R
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class PassengerSignInActivity : AppCompatActivity() {

    private lateinit var textInputEmail: TextInputLayout
    private lateinit var textInputName: TextInputLayout
    private lateinit var textInputPassword: TextInputLayout
    private lateinit var textInputConfirmPassword: TextInputLayout
    private lateinit var toggleLoginSignUpTextView: TextView
    private lateinit var loginSignUpButton: Button

    private var isLoginModeActive = false

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var passengerUsersDatabaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passeger_sign_in)

        textInputEmail = findViewById(R.id.textInputEmail)
        textInputName = findViewById(R.id.textInputName)
        textInputPassword = findViewById(R.id.textInputPassword)
        textInputConfirmPassword = findViewById(R.id.textInputConfirmPassword)
        toggleLoginSignUpTextView = findViewById(R.id.toggleLoginSignUpTextView)
        loginSignUpButton = findViewById(R.id.loginSignUpButton)

        database = Firebase.database
        passengerUsersDatabaseReference = database.getReference("passengerUsers")
        auth = FirebaseAuth.getInstance()

        auth.currentUser?.let {
            startActivity(Intent(this@PassengerSignInActivity, PassengerMapsActivity::class.java))
        }

    }

    private fun validateEmail(): Boolean {
        val emailInput: String = textInputEmail.editText?.text.toString().trim()
        return if (emailInput.isEmpty()) {
            textInputEmail.error = "Please input your email"
            false
        } else {
            true
        }
    }

    private fun validateName(): Boolean {
        val nameInput: String = textInputName.editText?.text.toString().trim()
        return if (isLoginModeActive) {
            true
        } else {
            when {
                nameInput.isEmpty() -> {
                    textInputName.error = "Please input your name"
                    false
                }
                nameInput.length > 15 -> {
                    textInputName.error = "Name length have to be less than 15"
                    false
                }
                else -> {
                    true
                }
            }
        }
    }

    private fun validatePassword(): Boolean {
        val passwordInput: String = textInputPassword.editText?.text.toString().trim()
        val confirmPasswordInput: String =
            textInputConfirmPassword.editText?.text.toString().trim()
        return when {
            passwordInput.isEmpty() -> {
                textInputPassword.error = "Please input your password"
                false
            }
            passwordInput.length < 7 -> {
                textInputPassword.error = "Password length have to be more than 6"
                false
            }
            !isLoginModeActive -> if (passwordInput != confirmPasswordInput) {
                textInputPassword.error = "Password have to match"
                false
            } else {
                true
            }
            else -> {
                true
            }
        }
    }

    fun loginSignUpUser(view: View) {
        if (!validateEmail() or !validatePassword() or !validateName()) {
            return
        }
        authenticationUser(
            textInputEmail.editText?.text.toString().trim(),
            textInputPassword.editText?.text.toString().trim()
        )
    }

    private fun authenticationUser(email: String, password: String) {
        if (isLoginModeActive) {
            auth.signInWithEmailAndPassword(
                email, password
            ).addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d(DriverSignInActivity.TAG, "signInWithEmail:success")
                    startActivity(
                        Intent(
                            this@PassengerSignInActivity,
                            PassengerMapsActivity::class.java
                        )
                    )
                } else {
                    Log.w(DriverSignInActivity.TAG, "signInWithEmail:failure", it.exception)
                    Toast.makeText(
                        baseContext, "Authentication failed.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d(DriverSignInActivity.TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                   //todo createUser(user)
                    startActivity(
                        Intent(
                            this@PassengerSignInActivity,
                            PassengerMapsActivity::class.java
                        )
                    )
                } else {
                    Log.w(DriverSignInActivity.TAG, "signInWithEmail:failure", it.exception)
                    Toast.makeText(
                        baseContext, "Authentication failed.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun createUser(firebaseUser: FirebaseUser?) {
        val user = User(
            textInputName.editText?.text.toString().trim(),
            firebaseUser?.email,
            firebaseUser?.uid
        )
        passengerUsersDatabaseReference.push().setValue(user)
    }

    fun toggleLoginSignUp(view: View) {
        if (isLoginModeActive) {
            isLoginModeActive = false
            loginSignUpButton.text = getString(R.string.sign_up)
            toggleLoginSignUpTextView.text = getString(R.string.or_log_in)
            textInputConfirmPassword.visibility = View.VISIBLE
            textInputName.visibility = View.VISIBLE
        } else {
            isLoginModeActive = true
            loginSignUpButton.text = getString(R.string.log_in)
            toggleLoginSignUpTextView.text = getString(R.string.or_sign_up)
            textInputConfirmPassword.visibility = View.GONE
            textInputName.visibility = View.GONE
        }
    }

}