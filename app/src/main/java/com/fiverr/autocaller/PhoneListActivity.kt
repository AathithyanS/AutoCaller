package com.fiverr.autocaller

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.TelecomManager
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.fiverr.autocaller.RvAdapter.PhoneRvAdapter
import com.fiverr.autocaller.database.DatabaseHelper
import com.fiverr.autocaller.databinding.ActivityPhoneListBinding
import com.fiverr.autocaller.model.PhoneAccount
import com.fiverr.autocaller.util.CallManager
import com.fiverr.autocaller.util.ExcelUtil
import com.fiverr.autocaller.util.SharedPreferenceManager
import java.util.Calendar
import kotlin.properties.Delegates
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.fiverr.autocaller.providers.Providers
import com.fiverr.autocaller.util.Util
import yuku.ambilwarna.AmbilWarnaDialog
import yuku.ambilwarna.BuildConfig
import java.io.File

class PhoneListActivity : AppCompatActivity() {

    lateinit var binding : ActivityPhoneListBinding
    lateinit var dbHelper: DatabaseHelper
    var id by Delegates.notNull<Int>();
    lateinit var listOfPhone: ArrayList<PhoneAccount>
    lateinit var telecomManager: TelecomManager
    var phoneTv: TextView? = null
    var processCallDialog: Dialog? = null;
    var noteDialog: Dialog? = null;
    var pausedId = -2
    lateinit var adapter: PhoneRvAdapter;
    var currPhNa: String = ""
    var currPhone: PhoneAccount ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneListBinding.inflate(layoutInflater)
        window.addFlags(FLAG_KEEP_SCREEN_ON)
        setContentView(binding.root)

        CallManager.phoneListActivity = this
        id = intent.getIntExtra("id", -1)
        dbHelper = DatabaseHelper(this)
        telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager

        setupRv()

        setupPasueStatus()

        setupIntialCount()

        updateAnsweredCount()

        binding.leadsCountTv.setText("Leads count: " + dbHelper.countLeadsInFile(id.toLong()).toString())
        if (Providers.isSchedule){
            setupButton()
        }

        binding.startBtn.setOnClickListener {
            setupButton()
        }

        binding.timerIv.setOnClickListener {
            openTimelineDialog()
        }

        binding.resumeBtn.setOnClickListener {
            CallManager.isContinue = true
            CallManager.currentPosition = SharedPreferenceManager.getCallStatus(this, id)
            startCall();
            binding.resumeBtn.visibility = View.GONE
            binding.startBtn.setText("Show Progress")
        }

        binding.moreIv.setOnClickListener {
            showPopupMenu(it)
        }

        binding.calendarIv.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun setupIntialCount() {
//        if (pausedId == -2) {
//            binding.countTv.setText("Phone List (0/${listOfPhone.size})")
//        }else{
//            binding.countTv.setText("Phone List ($pausedId/${listOfPhone.size})")
//        }
    }

    private fun setupButton(){
        if (binding.startBtn.text.equals("Show Progress")) {
            processCallDialog?.show()
            noteDialog?.show()
            binding.resumeBtn.visibility = View.GONE
        }else{
            if (binding.startBtn.text.equals("Restart")){
                showRestartAlertDialog()
            }else {
                binding.startBtn.setText("Show Progress")
                CallManager.isContinue = true
                CallManager.currentPosition = 0
                startCall();
                binding.resumeBtn.visibility = View.GONE
            }
        }
//        if (binding.startBtn.text.equals("Restart")){
//            binding.startBtn.setText("Show Progress")
//        }

    }

    private fun setupPasueStatus() {
        pausedId = SharedPreferenceManager.getCallStatus(this, id)
        binding.countTv.setText("Phone List (${pausedId}/${listOfPhone.size})")
        binding.phoneListRv.smoothScrollToPosition(pausedId)
        adapter.setupPause(pausedId)
        adapter
        if (pausedId == -2){
            binding.countTv.setText("Phone List (0/${listOfPhone.size})")
        }
        if (pausedId == listOfPhone.size){
            binding.startBtn.setText("Restart")
            binding.resumeBtn.visibility = View.GONE
        }else if(pausedId != -2){
            binding.startBtn.setText("Restart")
            binding.resumeBtn.visibility = View.VISIBLE
        }else{
            binding.startBtn.setText("Start")
            binding.resumeBtn.visibility = View.GONE
        }
    }

    private fun startCall() {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.call_dialog, null)
        builder.setView(dialogView)
        processCallDialog = builder.create()
        processCallDialog?.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent);
        processCallDialog?.show()
        processCallDialog?.setCancelable(false)

        phoneTv = processCallDialog?.findViewById<TextView>(R.id.phoneProTv)
        val pasueBtn = processCallDialog?.findViewById<Button>(R.id.pauseBtn)
        val minimizeIv = processCallDialog?.findViewById<ImageView>(R.id.minimizeIv)
        val takeNoteSwitch = processCallDialog?.findViewById<Switch>(R.id.takeNoteSwitch)

        takeNoteSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                CallManager.isTakeNote = true
            } else {
                CallManager.isTakeNote = false
            }
        }
        pasueBtn?.setOnClickListener {
            CallManager.reject()
            processCallDialog?.dismiss()
            setupRv()
            savePauseStatus()
            setupPasueStatus()
            CallManager.isTakeNote =  false
        }

        minimizeIv?.setOnClickListener {
            processCallDialog?.hide()
        }

        startCalling()
    }


    fun openNoteDialog(){
        val builder2 = AlertDialog.Builder(this)
        val dialogView2 = layoutInflater.inflate(R.layout.add_note_dialog, null)
        builder2.setView(dialogView2)
        noteDialog = builder2.create()
        noteDialog?.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent);
        noteDialog?.show()
        noteDialog?.setCancelable(false)
        var noteBtnId: String? = null
        val titleTv = noteDialog?.findViewById<TextView>(R.id.noteTitleTv)
        val NoteEt = noteDialog?.findViewById<EditText>(R.id.noteEt)
        val n1Btn = noteDialog?.findViewById<Button>(R.id.n1Btn)
        val n2Btn = noteDialog?.findViewById<Button>(R.id.n2Btn)
        val n3Btn = noteDialog?.findViewById<Button>(R.id.n3Btn)
        val n4Btn = noteDialog?.findViewById<Button>(R.id.n4Btn)
        val noteMinIv = noteDialog?.findViewById<ImageView>(R.id.noteMinIv)
        val saveNoteBtn = noteDialog?.findViewById<Button>(R.id.saveNoteBtn)

        noteDialog?.show()


        titleTv?.text = "Name: ${currPhone?.name}\nPhone: ${currPhone?.phone}\nLastname: ${currPhone?.lastName}\nAddress: ${currPhone?.address}"

        n1Btn?.setText(SharedPreferenceManager.getCustomButton(this, "button1"))
        n2Btn?.setText(SharedPreferenceManager.getCustomButton(this, "button2"))
        n3Btn?.setText(SharedPreferenceManager.getCustomButton(this, "button3"))
        n4Btn?.setText(SharedPreferenceManager.getCustomButton(this, "button4"))

        val btn1Color = SharedPreferenceManager.getCustomButtonColor(this, "button1-color")
        val btn2Color = SharedPreferenceManager.getCustomButtonColor(this, "button2-color")
        val btn3Color = SharedPreferenceManager.getCustomButtonColor(this, "button3-color")
        val btn4Color = SharedPreferenceManager.getCustomButtonColor(this, "button4-color")
        n1Btn?.setBackgroundColor(btn1Color)
        n2Btn?.setBackgroundColor(btn2Color)
        n3Btn?.setBackgroundColor(btn3Color)
        n4Btn?.setBackgroundColor(btn4Color)

        n1Btn?.setOnClickListener {
            noteBtnId = "button1"
            val btnTextList = ArrayList<String>()
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(this, "button1"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(this, "button2"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(this, "button3"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(this, "button4"))
            val removedText = Util.removeStringsFromLongString(btnTextList, NoteEt!!.text.toString())
            NoteEt?.setText(SharedPreferenceManager.getCustomButtonMsg(this, "button1")+" "+removedText)
            NoteEt?.setSelection(NoteEt?.text?.length ?: 0)
        }
        n2Btn?.setOnClickListener {
            noteBtnId = "button2"
            val btnTextList = ArrayList<String>()
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(this, "button1"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(this, "button2"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(this, "button3"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(this, "button4"))
            val removedText = Util.removeStringsFromLongString(btnTextList, NoteEt!!.text.toString())
            NoteEt?.setText(SharedPreferenceManager.getCustomButtonMsg(this, "button2")+" "+removedText)
            NoteEt?.setSelection(NoteEt?.text?.length ?: 0)
        }
        n3Btn?.setOnClickListener {
            noteBtnId = "button3"
            val btnTextList = ArrayList<String>()
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(this, "button1"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(this, "button2"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(this, "button3"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(this, "button4"))
            val removedText = Util.removeStringsFromLongString(btnTextList, NoteEt!!.text.toString())
            NoteEt?.setText(SharedPreferenceManager.getCustomButtonMsg(this, "button3")+" "+removedText)
            NoteEt?.setSelection(NoteEt?.text?.length ?: 0)
        }
        n4Btn?.setOnClickListener {
            noteBtnId = "button4"
            val btnTextList = ArrayList<String>()
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(this, "button1"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(this, "button2"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(this, "button3"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(this, "button4"))
            val removedText = Util.removeStringsFromLongString(btnTextList, NoteEt!!.text.toString())
            NoteEt?.setText(SharedPreferenceManager.getCustomButtonMsg(this, "button4")+" "+removedText)
            NoteEt?.setSelection(NoteEt?.text?.length ?: 0)
        }
        n1Btn?.setOnLongClickListener {
            showUpdateCustomButton("button1", n1Btn, btn1Color)
            true
        }
        n2Btn?.setOnLongClickListener {
            showUpdateCustomButton("button2", n2Btn, btn2Color)
            true
        }
        n3Btn?.setOnLongClickListener {
            showUpdateCustomButton("button3", n3Btn, btn3Color)
            true
        }
        n4Btn?.setOnLongClickListener {
            showUpdateCustomButton("button4", n4Btn, btn4Color)
            true
        }

        saveNoteBtn?.setOnClickListener {
            CallManager.call?.disconnect()
            dbHelper.updatePhoneData(CallManager.currCallId, NoteEt!!.text.toString(), CallManager.selectedDate, noteBtnId.toString())
            startCalling()
            noteDialog?.dismiss()
            noteDialog = null
        }

        noteMinIv?.setOnClickListener {
            noteDialog?.hide()
            processCallDialog?.hide()
        }

    }


    fun startCalling() {
        binding.leadsCountTv.setText("Leads count: " + dbHelper.countLeadsInFile(id.toLong()).toString())
        Handler(Looper.getMainLooper()).postDelayed({
            updateAnsweredCount()
            binding.countTv.setText("Phone List (${CallManager.currentPosition+1}/${listOfPhone.size})")
            Log.d("currPosition", "startCalling: ${CallManager.currentPosition}")
            if (CallManager.currentPosition < (listOfPhone.size)){
                val phone = listOfPhone.get(CallManager.currentPosition)
                currPhone = phone
                CallManager.currCallId = phone.id.toInt()
                phoneTv?.text = "Name: ${currPhone?.name}\n" +
                        "Phone: ${currPhone?.phone}\n" +
                        "Lastname: ${currPhone?.lastName}\n" +
                        "Address: ${currPhone?.address}"
                currPhNa = "${phone.phone} / ${phone.name}"
                listOfPhone = dbHelper.getPhoneAccountsByFileInfoId(id)
                adapter.updateData(listOfPhone)
                val uri = Uri.fromParts("tel", phone.phone, null)
                telecomManager.placeCall(uri, Bundle())
                if ((CallManager.currentPosition+3) < (listOfPhone.size)) {
                    if (CallManager.currentPosition <= 3){
                        binding.phoneListRv.smoothScrollToPosition(CallManager.currentPosition )
                    }else{
                        binding.phoneListRv.smoothScrollToPosition(CallManager.currentPosition + 3)
                    }

                }else{
                    binding.phoneListRv.smoothScrollToPosition(CallManager.currentPosition)
                }
            }else{
                Toast.makeText(this, "Call completed", Toast.LENGTH_SHORT).show()
                stopCallProcessDialog()
                SharedPreferenceManager.saveCallStatus(this, id, listOfPhone.size)
                CallManager.currCallId = -1
                CallManager.currentPosition = 0
                CallManager.isTakeNote =  false
                CallManager.isContinue =  false
                setupRv()
                binding.countTv.setText("Phone List (${listOfPhone.size}/${listOfPhone.size})")
                if (!Providers.isSchedule) {
                    val i = Intent(this, PhoneListActivity::class.java)
                    i.putExtra("id", id)
                    startActivity(i)
                    finish()
                }else{
                    Providers.startSchedule(this);
                    finish()
                }

            }
        }, 1500)
    }

    fun savePauseStatus(){
        SharedPreferenceManager.saveCallStatus(this, id, (CallManager.currentPosition - 1))
        setupPasueStatus()
    }

    fun setupRv() {
         listOfPhone = dbHelper.getPhoneAccountsByFileInfoId(id)
         adapter = PhoneRvAdapter(listOfPhone, this, id)
         binding.phoneListRv.layoutManager = LinearLayoutManager(this)
         binding.phoneListRv.adapter = adapter
    }

    private fun openTimelineDialog() {
        val (startTime, endTime) = SharedPreferenceManager.getCallTimes(this)

        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.time_set_dialog, null)
        builder.setView(dialogView)
        val dialog: Dialog = builder.create()
        dialog.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show()

        val startTv = dialog.findViewById<EditText>(R.id.startTimeTv)
        val endTv = dialog.findViewById<EditText>(R.id.endTimeTv)
        val saveBtn = dialog.findViewById<Button>(R.id.saveBtn)

        startTv.setText(startTime.toString())
        endTv.setText( endTime.toString())

        saveBtn.setOnClickListener{
            val sTime = startTv.text.toString()
            val eTime =  endTv.text.toString()
            if (sTime.isNotEmpty() && eTime.isNotEmpty()) {
                if(sTime.toInt() < eTime.toInt()) {
                    SharedPreferenceManager.saveCallTimes(this, sTime.toInt(), eTime.toInt())
                }else{
                    Toast.makeText(this, "Start time always lesser than the end time!", Toast.LENGTH_SHORT).show()
                }
            }else{
                Toast.makeText(this, "Fields can't be empty!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
    }

    private fun showDatePickerDialog() {
        val currentDate = Calendar.getInstance()
        val year = currentDate.get(Calendar.YEAR)
        val month = currentDate.get(Calendar.MONTH)
        val day = currentDate.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener { view: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                // Update TextView with the selected date
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                val hexDate = selectedDate.timeInMillis
                CallManager.selectedDate = hexDate

            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun stopCallProcessDialog(){
        processCallDialog?.dismiss()
    }

    private fun showRestartAlertDialog(){
        val builder3 = AlertDialog.Builder(this)
        val dialogView3 = layoutInflater.inflate(R.layout.restart_alert_dialog, null)
        builder3.setView(dialogView3)
        val alertDialog = builder3.create()
        alertDialog?.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent);
        alertDialog?.show()

        val startBtn = alertDialog.findViewById<Button>(R.id.reOkayBtn)
        val canceltBtn = alertDialog.findViewById<Button>(R.id.reCancelBtn)

        startBtn?.setOnClickListener {
            dbHelper.clearDateAndNoteForFileInfo(id.toLong())
            binding.startBtn.setText("Show Progress")
            CallManager.isContinue = true
            CallManager.currentPosition = 0
            startCall();
            binding.resumeBtn.visibility = View.GONE
            alertDialog.dismiss()
        }

        canceltBtn?.setOnClickListener {
            alertDialog.dismiss()
        }
    }

    @SuppressLint("ResourceAsColor")
    fun showUpdateCustomButton(value: String, n1Btn: Button) {
        val builder3 = AlertDialog.Builder(this)
        val dialogView3 = layoutInflater.inflate(R.layout.update_custom_button_dialog, null)
        builder3.setView(dialogView3)
        val updateDialog = builder3.create()
        updateDialog?.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent);
        updateDialog?.show()

        val cBtnEt = updateDialog.findViewById<EditText>(R.id.cBtnEt)
        val cMessageEt = updateDialog.findViewById<EditText>(R.id.cMessageEt)
        val noteColorTv = updateDialog.findViewById<TextView>(R.id.noteColorTv)
        val saveBtn = updateDialog.findViewById<Button>(R.id.cSaveBtn)
        val canceltBtn = updateDialog.findViewById<Button>(R.id.cCancelBtn)
        val colorView = updateDialog.findViewById<View>(R.id.colorView)

        cBtnEt?.setText(n1Btn.text)
        cMessageEt?.setText(SharedPreferenceManager.getCustomButtonMsg(this, value))

        colorView?.visibility = View.INVISIBLE
        noteColorTv?.visibility = View.INVISIBLE



        colorView?.setOnClickListener{
            val colorPickerDialog = AmbilWarnaDialog(this, getColorFromView(colorView), object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                   colorView.setBackgroundColor(color)
                }

                override fun onCancel(dialog: AmbilWarnaDialog?) {
                    // cancel was selected by the user
                }
            })

            colorPickerDialog.show()
        }


        saveBtn?.setOnClickListener {
            val newText = cBtnEt!!.text.toString()
            n1Btn.setText(newText)
            SharedPreferenceManager.saveCustomButton(this, value, newText)
            SharedPreferenceManager.saveCustomButtonMsg(this, value, cMessageEt!!.text.toString())
            SharedPreferenceManager.saveCustomButtonColor(this, value+"-color", getColorFromView(colorView!!))
            updateDialog.dismiss()
            n1Btn.setBackgroundColor(getColorFromView(colorView!!))
        }

        canceltBtn?.setOnClickListener {
            updateDialog.dismiss()
        }
    }
    @SuppressLint("ResourceAsColor")
    fun showUpdateCustomButton(value: String, n1Btn: Button, btnColor: Int) {
        val builder3 = AlertDialog.Builder(this)
        val dialogView3 = layoutInflater.inflate(R.layout.update_custom_button_dialog, null)
        builder3.setView(dialogView3)
        val updateDialog = builder3.create()
        updateDialog?.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent);
        updateDialog?.show()

        val cBtnEt = updateDialog.findViewById<EditText>(R.id.cBtnEt)
        val cMessageEt = updateDialog.findViewById<EditText>(R.id.cMessageEt)
        val saveBtn = updateDialog.findViewById<Button>(R.id.cSaveBtn)
        val canceltBtn = updateDialog.findViewById<Button>(R.id.cCancelBtn)
        val colorView = updateDialog.findViewById<View>(R.id.colorView)
        cBtnEt?.setText(n1Btn.text)
        cMessageEt?.setText(SharedPreferenceManager.getCustomButtonMsg(this, value))

        colorView?.setBackgroundColor(btnColor)

        val colorDefultView = updateDialog.findViewById<View>(R.id.defaultColorView)
        colorDefultView?.setOnClickListener {
            colorView?.setBackgroundColor(ContextCompat.getColor(this, R.color.defaultColor))
        }

        colorView?.setOnClickListener{
            val colorPickerDialog = AmbilWarnaDialog(this, getColorFromView(colorView), object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                   colorView.setBackgroundColor(color)
                }

                override fun onCancel(dialog: AmbilWarnaDialog?) {
                    // cancel was selected by the user
                }
            })

            colorPickerDialog.show()
        }


        saveBtn?.setOnClickListener {
            val newText = cBtnEt!!.text.toString()
            n1Btn.setText(newText)
            SharedPreferenceManager.saveCustomButton(this, value, newText)
            SharedPreferenceManager.saveCustomButtonMsg(this, value, cMessageEt!!.text.toString())
            SharedPreferenceManager.saveCustomButtonColor(this, value+"-color", getColorFromView(colorView!!))
            updateDialog.dismiss()
            n1Btn.setBackgroundColor(getColorFromView(colorView!!))
        }

        canceltBtn?.setOnClickListener {
            updateDialog.dismiss()
        }
    }

    private fun updateAnsweredCount() {
        val count = dbHelper.countAnsweredRecordsForFileInfo(id.toLong())
        binding.answeredcountTv.setText("Answered count: $count")
    }

    private fun getColorFromView(view: View): Int {
        val background = view.background
        if (background is ColorDrawable) {
            return background.color
        }
        return R.color.defaultColor // Fallback to initial color if no color is found
    }

    fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.op_export -> {
                    exportXlsx()
                    true
                }
                R.id.op_sort_answered -> {
                    createSortCall("Unanswered")
                    true
                }
//                R.id.op_sort_unanswered -> {
//                    createSortCall("Unanswered")
//                    true
//                }
                R.id.share_lead -> {
                    shareLeads()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun shareLeads() {
        val leads = dbHelper.getPhoneAccountsByStatusKeywordAndFileInfoId(id.toLong(), "Lead")
        val fileData = dbHelper.getFileInfoById(id)
        // Create a StringBuilder to construct the text file content
        val stringBuilder = StringBuilder()
        stringBuilder.append("\t\t\t${fileData?.fileName}")
        // Iterate through each lead and format it according to the specified format
        leads.forEachIndexed { index, lead ->
            stringBuilder.append("Lead ${index + 1}\u0000\n")
            stringBuilder.append("Cell Number: ${lead.cell}\n")
            stringBuilder.append("Name: ${lead.name} ${lead.lastName}\n")
            stringBuilder.append("Address: ${lead.address}, ${lead.suburb}, ${lead.state}\n")
            if (lead.calledTime != "" && lead.calledTime.isNotEmpty()) {
                stringBuilder.append("Date: ${adapter.formatDate(lead.calledTime.toLong())}\n")
            }else {
                stringBuilder.append("Date: N/A\n")
            }
            stringBuilder.append("Notes: ${lead.note}\n\n")
        }

        // Convert the StringBuilder content to a string
        val fileContent = stringBuilder.toString()

        // Define the file path and name
        val fileName = "Leads.txt"
        val file = File(getExternalFilesDir(null), fileName)

        // Write the content to the file
        file.writeText(fileContent)

        // Share the file
        shareFile(this, file)
    }

    private fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant read permission to the receiving app
        }
        context.startActivity(Intent.createChooser(intent, "Share Excel File"))
    }

    // Function to share the file
//    private fun shareFile(file: File) {
//        val uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", file)
//        val intent = Intent(Intent.ACTION_SEND).apply {
//            type = "text/plain"
//            putExtra(Intent.EXTRA_STREAM, uri)
//            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        }
//        startActivity(Intent.createChooser(intent, "Share Leads"))
//    }



    private fun createSortCall(keyword: String) {
        val fileInfo = dbHelper.getFileInfoById(id)
        val answeredId = dbHelper.insertFileInfo(selectedDate = System.currentTimeMillis(), fileUri = fileInfo!!.fileUri, fileName = fileInfo.fileName +"-(Answered)")
        val answeredList = dbHelper.getPhoneDataByStatusAndFileId(id.toLong())
        dbHelper.addPhoneAccounts(answeredList, answeredId)
        finishAffinity()
        val intent = Intent(this, MainActivity::class.java);
        startActivity(intent)

    }

    private fun exportXlsx() {
        Toast.makeText(
            this,
            "Wait few seconds It will take few minutes or seconds stay in the same page.",
            Toast.LENGTH_SHORT
        ).show()
        ExcelUtil.createExcelAndShare(this, listOfPhone)
    }

    override fun onDestroy() {
        super.onDestroy()
        CallManager.currCallId = -1
        CallManager.currentPosition = 0
        CallManager.isContinue = false
        CallManager.isTakeNote =  false
        CallManager.call?.disconnect()
    }

    fun setupLeadCount(){
        binding.leadsCountTv.setText("Leads count: " + dbHelper.countLeadsInFile(id.toLong()).toString())
    }

}