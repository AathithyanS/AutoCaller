package com.fiverr.autocaller.service

import android.os.Build
import android.os.Handler
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.fiverr.autocaller.PhoneListActivity
import com.fiverr.autocaller.database.DatabaseHelper
import com.fiverr.autocaller.util.CallManager
import com.fiverr.autocaller.util.SharedPreferenceManager

class CallService: InCallService() {

    private var callStartTime: Long = 0

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCallAdded(call: Call?) {
        super.onCallAdded(call)

        var (startTime, endTime) = SharedPreferenceManager.getCallTimes(applicationContext)
        startTime *= 1000;
        endTime *= 1000;
        val callDirection = call!!.details.callDirection
        if (callDirection == Call.Details.DIRECTION_INCOMING) {
            call.disconnect()
        } else {
            CallManager.call = call
            callStartTime = System.currentTimeMillis()
        }

        var isAnswered = false;
        var noAnsw = false
        var isDisconnected = false;

        call.registerCallback(object : Call.Callback() {

            override fun onStateChanged(call: Call, state: Int) {
                super.onStateChanged(call, state)
                if (callDirection != Call.Details.DIRECTION_INCOMING) {
                    val dbHelper = DatabaseHelper(applicationContext)

                    when (state) {
                        Call.STATE_ACTIVE -> {
                            isAnswered = true
                            val callDuration = System.currentTimeMillis() - callStartTime
                            if (CallManager.isTakeNote) {
                                if (callDuration < startTime) {
                                    // Mark the call as unanswered
                                    markCallAsUnanswered(call)
                                    Log.d("callDetetectionFunc", "Call marked as unanswered = $callDuration")
                                    dbHelper.updatePhoneData(CallManager.currCallId, "Unanswered", CallManager.selectedDate)
                                    call.disconnect()
                                    noAnsw = true
                                }else {
                                    CallManager.phoneListActivity?.openNoteDialog()
                                }
                            }else{
                                call.disconnect()
                            }
//                            Log.d("callDetetectionFunc", "onStateChanged: Active")
                        }
                        Call.STATE_DISCONNECTED -> {
                            isDisconnected = true
                            val callDuration = System.currentTimeMillis() - callStartTime
                            if (CallManager.isContinue) {
                                if (noAnsw && isAnswered){
//                                    CallManager.call = null
                                    CallManager.phoneListActivity?.startCalling()
                                }
                                if (!CallManager.isTakeNote || !isAnswered) {
                                    if (callDuration < startTime) {
                                        // Mark the call as unanswered
                                        markCallAsUnanswered(call)
                                        Log.d("callDetetectionFunc", "Call marked as unanswered = $callDuration")
                                        dbHelper.updatePhoneData(CallManager.currCallId, "Unanswered", CallManager.selectedDate)
                                    }
                                    else if (callDuration >= startTime && callDuration <= endTime && isAnswered) {
                                        // Mark the call as answered
                                        markCallAsAnswered(call)
                                        Log.d("callDetetectionFunc", "Call marked as answered = $callDuration")
                                        dbHelper.updatePhoneData(CallManager.currCallId, "Answered", CallManager.selectedDate)
                                    } else {
                                        // Mark the call as unanswered
                                        markCallAsUnanswered(call)
                                        Log.d("callDetetectionFunc", "Call marked as unanswered = $callDuration")
                                        dbHelper.updatePhoneData(CallManager.currCallId, "Unanswered", CallManager.selectedDate)
                                    }
                                    CallManager.phoneListActivity?.startCalling()
                                }
                            }else{
                                CallManager.call = null
//                                CallManager.phoneListActivity?.savePauseStatus()
                            }
//                            Log.d("callDetetectionFunc", "onStateChanged: Disconnected")
                        }
                    }


                }
            }
        })

        Handler().postDelayed({
            if (!isAnswered && !isDisconnected){
                call.disconnect()
            }
        }, endTime.toLong())

        CallManager.currentPosition++


    }

    private fun markCallAsAnswered(call: Call) {
        // Update call status as answered (you may implement this)
        // For example, you can use a function to update status in your database
    }

    private fun markCallAsUnanswered(call: Call) {
        // Update call status as unanswered (you may implement this)
        // For example, you can use a function to update status in your database
    }
}
