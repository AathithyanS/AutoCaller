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
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private val PREFS_NAME = "MyPrefs"
    private val PREF_TIMESTAMP = "timestamp"

//    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val storedTimestamp = prefs.getLong(PREF_TIMESTAMP, 0)

//    if (storedTimestamp == 0L) {
//        // First launch, store current timestamp
//        val currentTimestamp = System.currentTimeMillis()
//        prefs.edit().putLong(PREF_TIMESTAMP, currentTimestamp).apply()
//    } else {
//        // Subsequent launch, check if one day has passed
//        val currentTimestamp = System.currentTimeMillis()
//        val oneDayInMillis = 24 * 60 * 60 * 1000 // 24 hours in milliseconds
//        if (currentTimestamp - storedTimestamp > oneDayInMillis) {
//            // One day has passed, disable app functionality
//            disableApp()
//            return
//        }
//    }


    val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val defaultDialerPackage = telecomManager.defaultDialerPackage

        if (defaultDialerPackage == packageName) {
            Handler().postDelayed({
                openMainActivity();
            }, 1000)
        } else {
            setPhoneDefault()
        }


    }

//    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setPhoneDefault() {
        val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
        val dialerRoleRequest = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == RESULT_OK){
                Toast.makeText(this, "Successfully changed as default app", Toast.LENGTH_SHORT).show()
                openMainActivity()
            }else{
//                finish()
                Toast.makeText(this, "ff", Toast.LENGTH_SHORT).show()
            }
        }

//        if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) &&
//            !roleManager.isRoleHeld(RoleManager.ROLE_DIALER))
            dialerRoleRequest.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))

    }

    private fun openMainActivity(){
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun disableApp() {
        // Disable app functionality, e.g., display a message and close the app
        Toast.makeText(this, "Trial period has expired.", Toast.LENGTH_SHORT).show()
        finish()
    }

}

