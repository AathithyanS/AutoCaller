package com.fiverr.autocaller.providers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.fiverr.autocaller.PhoneListActivity
import com.fiverr.autocaller.model.FileData
import com.fiverr.autocaller.util.CallManager

class Providers {
    companion object{
        var scheduler: ArrayList<FileData> ?= null;
        var isSchedule: Boolean = false
        var currentScheduleIndex = 0;

        fun setUpSchedule(filesList: ArrayList<FileData>){
            scheduler = filesList
            isSchedule = true
        }
        fun startSchedule(context: Activity){
            Handler(Looper.getMainLooper()).postDelayed({
                if (scheduler != null && currentScheduleIndex < scheduler!!.size) {
                    CallManager.currCallId = -1
                    CallManager.currentPosition = 0
                    CallManager.isContinue = false
                    CallManager.isTakeNote = false
                    CallManager.call?.disconnect()
                    var intent = Intent(context, PhoneListActivity::class.java)
                    intent.putExtra("id", scheduler!!.get(currentScheduleIndex).id)
                    context.startActivity(intent)
                    currentScheduleIndex++
                } else {
                    scheduler = null
                    isSchedule = false
                    currentScheduleIndex = 0
                }
            }, 1500)
        }
    }
}