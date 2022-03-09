package com.obs.facedetectionandstoring.dashboard

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.obs.facedetectionandstoring.R
import android.content.SharedPreferences
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.obs.facedetectionandstoring.login.LoginActivity

class DashboardActivity : AppCompatActivity(), View.OnClickListener {
    private var txtLogout: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        txtLogout = findViewById(R.id.txt_logout)
        txtLogout?.setOnClickListener(this)
        setStatusColor(this, R.color.txt_status)
    }

    private fun setLogoutOption() {
        val preferences = getSharedPreferences("HashMap", MODE_PRIVATE)
        val editor = preferences.edit()
        editor.clear()
        editor.apply()
        Toast.makeText(this, "Logout Successfully", Toast.LENGTH_SHORT).show()
        val i = Intent(this, LoginActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
        finish()
    }

    fun setStatusColor(activity: Activity, color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.window
                .addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            activity.window.statusBarColor = ContextCompat.getColor(activity, color)
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.txt_logout -> {
                setLogoutOption()
            }
        }
    }
}