package com.fiverr.autocaller

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
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

        binding.exportBtn.setOnClickListener {
            Toast.makeText(
                this,
                "Wait few seconds It will take few minutes or seconds stay in the same page.",
                Toast.LENGTH_SHORT
            ).show()
            ExcelUtil.createExcelAndShare(this, listOfPhone)
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

        n1Btn?.setOnClickListener {
            NoteEt?.setText(n1Btn.text.toString())
            NoteEt?.setSelection(NoteEt?.text?.length ?: 0)
        }
        n2Btn?.setOnClickListener {
            NoteEt?.setText(n2Btn?.text.toString())
            NoteEt?.setSelection(NoteEt?.text?.length ?: 0)
        }
        n3Btn?.setOnClickListener {
            NoteEt?.setText(n3Btn?.text.toString())
            NoteEt?.setSelection(NoteEt?.text?.length ?: 0)
        }
        n4Btn?.setOnClickListener {
            NoteEt?.setText(n4Btn?.text.toString())
            NoteEt?.setSelection(NoteEt?.text?.length ?: 0)
        }
        n1Btn?.setOnLongClickListener {
            showUpdateCustomButton("button1", n1Btn)
            true
        }
        n2Btn?.setOnLongClickListener {
            showUpdateCustomButton("button2", n2Btn)
            true
        }
        n3Btn?.setOnLongClickListener {
            showUpdateCustomButton("button3", n3Btn)
            true
        }
        n4Btn?.setOnLongClickListener {
            showUpdateCustomButton("button4", n4Btn)
            true
        }

        saveNoteBtn?.setOnClickListener {
            CallManager.call?.disconnect()
            dbHelper.updatePhoneData(CallManager.currCallId, NoteEt!!.text.toString(), CallManager.selectedDate)
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
                binding.phoneListRv.smoothScrollToPosition(CallManager.currentPosition + 3)
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
            val i = Intent(this, PhoneListActivity::class.java)
            i.putExtra("id", id)
            startActivity(i)
            finish()

        }
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

    fun showUpdateCustomButton(value: String, n1Btn: Button) {
        val builder3 = AlertDialog.Builder(this)
        val dialogView3 = layoutInflater.inflate(R.layout.update_custom_button_dialog, null)
        builder3.setView(dialogView3)
        val updateDialog = builder3.create()
        updateDialog?.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent);
        updateDialog?.show()

        val cBtnEt = updateDialog.findViewById<EditText>(R.id.cBtnEt)
        val saveBtn = updateDialog.findViewById<Button>(R.id.cSaveBtn)
        val canceltBtn = updateDialog.findViewById<Button>(R.id.cCancelBtn)

        saveBtn?.setOnClickListener {
            val newText = cBtnEt!!.text.toString()
            n1Btn.setText(newText)
            SharedPreferenceManager.saveCustomButton(this, value, newText)
            updateDialog.dismiss()
        }

        canceltBtn?.setOnClickListener {
            updateDialog.dismiss()
        }
    }

    private fun updateAnsweredCount() {
        val count = dbHelper.countAnsweredRecordsForFileInfo(id.toLong())
        binding.answeredcountTv.setText("Answered count: $count")
    }

    override fun onDestroy() {
        super.onDestroy()
        CallManager.currCallId = -1
        CallManager.currentPosition = 0
        CallManager.isContinue = false
        CallManager.isTakeNote =  false
        CallManager.call?.disconnect()
    }
}