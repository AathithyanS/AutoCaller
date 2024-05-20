package com.fiverr.autocaller.providers

import android.content.Context
import android.content.Intent
import com.fiverr.autocaller.PhoneListActivity
import com.fiverr.autocaller.model.FileData

class Providers {
    companion object{
        var scheduler: ArrayList<FileData> ?= null;
        var isSchedule: Boolean = false
        var currentScheduleIndex = 0;

        fun setUpSchedule(filesList: ArrayList<FileData>){
            scheduler = filesList
            isSchedule = true
        }
        fun startSchedule(context: Context){
            if (scheduler != null && currentScheduleIndex < scheduler!!.size) {
                var intent = Intent(context, PhoneListActivity::class.java)
                intent.putExtra("id", scheduler!!.get(currentScheduleIndex).id)
                context.startActivity(intent)
                currentScheduleIndex++
            }else{
                scheduler = null
                isSchedule = false
                currentScheduleIndex = 0
            }
        }
    }
}