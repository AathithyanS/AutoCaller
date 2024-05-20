package com.fiverr.autocaller.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.ContextCompat
import com.fiverr.autocaller.R
import java.util.*

object SharedPreferenceManager {

    private const val PREF_NAME = "CallSettings"
    private const val KEY_START_TIME = "start_time"
    private const val KEY_END_TIME = "end_time"

    fun saveCallTimes(context: Context, startTime: Int, endTime: Int) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putInt(KEY_START_TIME, startTime)
        editor.putInt(KEY_END_TIME, endTime)
        editor.apply()
    }

    fun getCallTimes(context: Context): Pair<Int, Int> {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val startTime = sharedPreferences.getInt(KEY_START_TIME, 8)
        val endTime = sharedPreferences.getInt(KEY_END_TIME, 24)
        return Pair(startTime, endTime)
    }

    fun saveCallStatus(context: Context,fileId: Int, pausedId: Int) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putInt(fileId.toString(), pausedId)
        editor.apply()
    }

    fun getCallStatus(context: Context,fileId: Int): Int{
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val pasuedId = sharedPreferences.getInt(fileId.toString(), -2)
        return pasuedId
    }

    fun saveCustomButton(context: Context,button: String, value: String) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString(button, value)
        editor.apply()
    }
    fun saveCustomButtonMsg(context: Context,button: String, value: String) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString(button+"-msg", value)
        editor.apply()
    }
    fun saveCustomButtonColor(context: Context,button: String, value: Int) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putInt(button, value)
        editor.apply()
    }

    fun getCustomButton(context: Context,button: String): String{
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var data = "Unknown"
        if (button.equals("button1")){
             data = sharedPreferences.getString(button, "Not Interested")!!
        }else if (button.equals("button2")){
            data = sharedPreferences.getString(button, "Not The Owner")!!
        }else if (button.equals("button3")){
            data = sharedPreferences.getString(button, "Wrong Number")!!
        }else if (button.equals("button4")){
            data = sharedPreferences.getString(button, "Lead")!!
        }else if (button.equals("buttonMsg1")){
            data = sharedPreferences.getString(button, "Message1")!!
        }else if (button.equals("buttonMsg2")){
            data = sharedPreferences.getString(button, "Message2")!!
        }else if (button.equals("buttonMsg3")){
            data = sharedPreferences.getString(button, "Message3")!!
        }else if (button.equals("buttonMsg4")){
            data = sharedPreferences.getString(button, "Message4")!!
        }
        return data
    }
    fun getCustomButtonMsg(context: Context,buttonV: String): String{
        val button = buttonV +"-msg"
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var data = "Unknown"
        if (button.equals("button1-msg")){
             data = sharedPreferences.getString(button, "Not Interested")!!
        }else if (button.equals("button2-msg")){
            data = sharedPreferences.getString(button, "Not The Owner")!!
        }else if (button.equals("button3-msg")){
            data = sharedPreferences.getString(button, "Wrong Number")!!
        }else if (button.equals("button4-msg")){
            data = sharedPreferences.getString(button, "Lead")!!
        }else if (button.equals("buttonMsg1-msg")){
            data = sharedPreferences.getString(button, "Message1")!!
        }else if (button.equals("buttonMsg2-msg")){
            data = sharedPreferences.getString(button, "Message2")!!
        }else if (button.equals("buttonMsg3-msg")){
            data = sharedPreferences.getString(button, "Message3")!!
        }else if (button.equals("buttonMsg4-msg")){
            data = sharedPreferences.getString(button, "Message4")!!
        }
        return data
    }
    fun getCustomButtonColor(context: Context,button: String): Int{
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var data = sharedPreferences.getInt(button, ContextCompat.getColor(context, R.color.defaultColor))!!
        return data
    }
}
