package com.fiverr.autocaller.util

import android.provider.ContactsContract.CommonDataKinds.Phone
import android.telecom.Call
import com.fiverr.autocaller.PhoneListActivity

class CallManager {
    companion object{

        var call: Call? = null
        var currentPosition: Int = 0
        var phoneListActivity: PhoneListActivity ?= null
        var currCallId: Int = -1;
        var isContinue = false
        var selectedDate: Long = System.currentTimeMillis()
        var isTakeNote =  false

        fun reject() {
            isContinue = false
            if (call != null) {
                val state = call?.state
                if (state == Call.STATE_RINGING) {
                    call!!.reject(false, null)
                } else if (state != Call.STATE_DISCONNECTED && state != Call.STATE_DISCONNECTING) {
                    call!!.disconnect()
                }
            }
        }
    }
}