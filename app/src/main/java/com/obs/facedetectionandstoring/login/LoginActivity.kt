package com.obs.facedetectionandstoring.login

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import com.obs.facedetectionandstoring.MainActivity
import com.obs.facedetectionandstoring.R
import com.obs.facedetectionandstoring.dashboard.DashboardActivity
import com.obs.facedetectionandstoring.registration.RegistrationActivity

class LoginActivity : AppCompatActivity() {

    private var txtRegisterAccount: AppCompatTextView? = null
    private var txtLoginAccount: AppCompatTextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        getValues()
        txtRegisterAccount = findViewById(R.id.txt_create_account)
        txtLoginAccount = findViewById(R.id.txt_login_face)
        txtRegisterAccount?.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }
        txtLoginAccount?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getValues() {
        var sharedPreferences: SharedPreferences
        sharedPreferences =
            getSharedPreferences(
                "HashMap",
                Context.MODE_PRIVATE
            )
        val editor = sharedPreferences.edit()
        var name = sharedPreferences.getBoolean("login", false)
        editor.apply()
        if (name) {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        } else {
        }
    }
}