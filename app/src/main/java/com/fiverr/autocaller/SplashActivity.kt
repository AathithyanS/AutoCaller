package com.fiverr.autocaller

import android.app.role.RoleManager
import android.content.Context
import android.content.Context.ROLE_SERVICE
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import java.util.Calendar

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // Months are indexed from 0
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        if (currentYear > 2024 || (currentYear == 2024 && currentMonth > 5) || (currentYear == 2024 && currentMonth == 5 && currentDay > 26)) {
            Toast.makeText(this, "App is no longer available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val defaultDialerPackage = telecomManager.defaultDialerPackage

        if (defaultDialerPackage == packageName) {
            Handler().postDelayed({
                openMainActivity()
            }, 1000)
        } else {
            setPhoneDefault()
        }
    }

    private fun setPhoneDefault() {
        val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
        val dialerRoleRequest = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == RESULT_OK) {
                Toast.makeText(this, "Successfully changed as default app", Toast.LENGTH_SHORT).show()
                openMainActivity()
            } else {
                Toast.makeText(this, "Failed to set as default app", Toast.LENGTH_SHORT).show()
            }
        }

        dialerRoleRequest.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
    }

    private fun openMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        //test
    }
}
