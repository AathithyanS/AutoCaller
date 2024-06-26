package com.fiverr.autocaller.RvAdapter

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.fiverr.autocaller.PhoneListActivity
import com.fiverr.autocaller.R
import com.fiverr.autocaller.model.PhoneAccount
import com.fiverr.autocaller.util.CallManager
import com.fiverr.autocaller.util.SharedPreferenceManager
import com.fiverr.autocaller.util.Util.Companion.removeStringsFromLongString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhoneRvAdapter(val phoneList: ArrayList<PhoneAccount>, val context: PhoneListActivity, val fieldId: Int) : RecyclerView.Adapter<PhoneRvAdapter.ViewHolder>() {

    private var pausedPosition: Int = -1

    fun updateData(newData: ArrayList<PhoneAccount>) {
        phoneList.clear()
        phoneList.addAll(newData)
        notifyDataSetChanged() // Notify adapter
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.phone_status_rv_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = phoneList.get(position)
        holder.phoneTv.setText(data.phone)

        holder.countStatTv.setText("${position+1}")

        if (data.name.isEmpty()) {
            holder.statusTv.setText("Name: N/A")
        }else{
            holder.statusTv.setText("Name: "+data.name + " "+data.lastName)
        }
        if (data.address.isEmpty()) {
            holder.statusTv.setText(holder.statusTv.text.toString()+"\nAddress: N/A")
        }else{
            holder.statusTv.setText(holder.statusTv.text.toString()+"\nAddress: "+data.address+", ${data.suburb}, ${data.state}")
        }
        if (data.calledTime.isEmpty()) {
            holder.statusTv.setText(holder.statusTv.text.toString()+"\nDate: N/A")
        }else{
            holder.statusTv.setText(holder.statusTv.text.toString()+"\nDate: "+formatDate(data.calledTime.toLong()))
        }
        if (data.note.isEmpty()) {
            holder.statusTv.setText(holder.statusTv.text.toString()+"\nNote: N/A")
        }else{
            holder.statusTv.setText(holder.statusTv.text.toString()+"\nNote: "+data.note)
        }

        val matchId = SharedPreferenceManager.getCallStatus(context, fieldId)

        if (CallManager.currentPosition == position && CallManager.isContinue){
            holder.phoneCl.setBackgroundColor(context.resources.getColor(R.color.back_selected))
//            context.binding.countTv.setText("Phone List ($position/${context.listOfPhone.size})")
        }
        else{
            holder.phoneCl.setBackgroundColor(context.resources.getColor(R.color.white))
        }

        holder.phoneCl.setOnClickListener {
//            val intent = Intent(context, PhoneDetailsActivity::class.java)
//            intent.putExtra("id", data.id)
//            context.startActivity(intent)
        }

        holder.noteEditIv.setOnClickListener {
            if (!CallManager.isContinue) {
                showUpdateNoteDialog(data)
            }else{
                Toast.makeText(context, "While calling your can't edit.", Toast.LENGTH_SHORT).show()
            }
        }

        holder.sendMsgIv.setOnClickListener {
            if (!CallManager.isContinue) {
                showMessageDialog(data)
            }else{
                Toast.makeText(context, "While calling your can't send messages.", Toast.LENGTH_SHORT).show()
            }
        }

        holder.callIv.setOnClickListener {
            openCallConfirmationDialog(data, position)
        }

        if (position == pausedPosition) {
            holder.phoneCl.setBackgroundColor(context.resources.getColor(R.color.blue)) // Set background color to blue
        }
    }

    private fun openCallConfirmationDialog(data: PhoneAccount, position: Int) {
        val builder = AlertDialog.Builder(context)
        val dialogView = context.layoutInflater.inflate(R.layout.call_confirmation_card, null)
        builder.setView(dialogView)
        val confirmationDialog = builder.create()
        confirmationDialog?.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent);
        confirmationDialog?.show()

        val callMsgTv = confirmationDialog?.findViewById<TextView>(R.id.callConfirmTv)
        val callBtn = confirmationDialog?.findViewById<Button>(R.id.callConfirmCallBtn)
        val cancelBtn = confirmationDialog?.findViewById<Button>(R.id.callConfirmCancelBtn)

        callMsgTv?.setText("Make sure to call: \n${data.phone}\n" +
                "${data.name}\n" +
                "${data.address}")

        cancelBtn?.setOnClickListener {
            confirmationDialog.dismiss()
        }
        callBtn?.setOnClickListener {
            context.makeCallFromIndex(position)
            confirmationDialog.dismiss()
        }
    }

    private fun showUpdateNoteDialog(data: PhoneAccount) {
        val builder = AlertDialog.Builder(context)
        val dialogView = context.layoutInflater.inflate(R.layout.edit_note_dialog, null)
        builder.setView(dialogView)
        val updateNoteDialog = builder.create()
        updateNoteDialog?.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent);
        updateNoteDialog?.show()
        var noteBtnId: String? = null;

        val updateTitle = updateNoteDialog?.findViewById<TextView>(R.id.updateTitleTv)
        val updateBtn = updateNoteDialog?.findViewById<Button>(R.id.updateNoteBtn)
        val NoteEt = updateNoteDialog?.findViewById<EditText>(R.id.updateNoteEt)

        val n1Btn = updateNoteDialog?.findViewById<Button>(R.id.eNoteBtn1)
        val n2Btn = updateNoteDialog?.findViewById<Button>(R.id.eNoteBtn2)
        val n3Btn = updateNoteDialog?.findViewById<Button>(R.id.eNoteBtn3)
        val n4Btn = updateNoteDialog?.findViewById<Button>(R.id.eNoteBtn4)
        val n5Btn = updateNoteDialog?.findViewById<Button>(R.id.eNoteBtn5)
        val n6Btn = updateNoteDialog?.findViewById<Button>(R.id.eNoteBtn6)

        updateTitle?.setText("Update Note\n" +
                "Phone: ${data.phone}\n" +
                "Fullname: ${data.name +" "+ data.lastName} \n" +
                "Address: ${data.address}, ${data.suburb}, ${data.state}")

        NoteEt?.setText(data.note)
        updateBtn?.setOnClickListener {
            val newNote = NoteEt?.text.toString()
            if (noteBtnId != null) {
                context.dbHelper.updateNoteColor(data.id.toInt(), newNote, noteBtnId.toString())
            }else{
                context.dbHelper.updateNote(data.id.toInt(), newNote)
            }
            updateNoteDialog.dismiss()
            updateData(context.dbHelper.getPhoneAccountsByFileInfoId(context.id))
            context.setupLeadCount()
        }

        n1Btn?.setText(SharedPreferenceManager.getCustomButton(context, "button1"))
        n2Btn?.setText(SharedPreferenceManager.getCustomButton(context, "button2"))
        n3Btn?.setText(SharedPreferenceManager.getCustomButton(context, "button3"))
        n4Btn?.setText(SharedPreferenceManager.getCustomButton(context, "button4"))
        n5Btn?.setText(SharedPreferenceManager.getCustomButton(context, "button5"))
        n6Btn?.setText(SharedPreferenceManager.getCustomButton(context, "button6"))

        val btn1Color = SharedPreferenceManager.getCustomButtonColor(context, "button1-color")
        val btn2Color = SharedPreferenceManager.getCustomButtonColor(context, "button2-color")
        val btn3Color = SharedPreferenceManager.getCustomButtonColor(context, "button3-color")
        val btn4Color = SharedPreferenceManager.getCustomButtonColor(context, "button4-color")
        val btn5Color = SharedPreferenceManager.getCustomButtonColor(context, "button5-color")
        val btn6Color = SharedPreferenceManager.getCustomButtonColor(context, "button6-color")
        n1Btn?.setBackgroundColor(btn1Color)
        n2Btn?.setBackgroundColor(btn2Color)
        n3Btn?.setBackgroundColor(btn3Color)
        n4Btn?.setBackgroundColor(btn4Color)
        n5Btn?.setBackgroundColor(btn5Color)
        n6Btn?.setBackgroundColor(btn6Color)


        n1Btn?.setOnClickListener {
            noteBtnId = "button1"
            val btnTextList = ArrayList<String>()
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button1"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button2"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button3"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button4"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button5"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button6"))
            val removedText = removeStringsFromLongString(btnTextList, NoteEt!!.text.toString())
            NoteEt?.setText(SharedPreferenceManager.getCustomButtonMsg(context, "button1")+" "+removedText)
            NoteEt?.setSelection(NoteEt?.text?.length ?: 0)
        }
        n2Btn?.setOnClickListener {
            noteBtnId = "button2"
            val btnTextList = ArrayList<String>()
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button1"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button2"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button3"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button4"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button5"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button6"))
            val removedText = removeStringsFromLongString(btnTextList, NoteEt!!.text.toString())
            NoteEt?.setText(SharedPreferenceManager.getCustomButtonMsg(context, "button2")+" "+removedText)
            NoteEt?.setSelection(NoteEt?.text?.length ?: 0)
        }
        n3Btn?.setOnClickListener {
            noteBtnId = "button3"
            val btnTextList = ArrayList<String>()
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button1"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button2"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button3"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button4"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button5"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button6"))
            val removedText = removeStringsFromLongString(btnTextList, NoteEt!!.text.toString())
            NoteEt?.setText(SharedPreferenceManager.getCustomButtonMsg(context, "button3")+" "+removedText)
            NoteEt?.setSelection(NoteEt?.text?.length ?: 0)
        }
        n4Btn?.setOnClickListener {
            noteBtnId = "button4"
            val btnTextList = ArrayList<String>()
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button1"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button2"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button3"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button4"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button5"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button6"))
            val removedText = removeStringsFromLongString(btnTextList, NoteEt!!.text.toString())
            NoteEt?.setText(SharedPreferenceManager.getCustomButtonMsg(context, "button4")+" "+removedText)
            NoteEt?.setSelection(NoteEt?.text?.length ?: 0)
        }
        n5Btn?.setOnClickListener {
            noteBtnId = "button5"
            val btnTextList = ArrayList<String>()
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button1"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button2"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button3"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button4"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button5"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button6"))
            val removedText = removeStringsFromLongString(btnTextList, NoteEt!!.text.toString())
            NoteEt?.setText(SharedPreferenceManager.getCustomButtonMsg(context, "button5")+" "+removedText)
            NoteEt?.setSelection(NoteEt?.text?.length ?: 0)
        }
        n6Btn?.setOnClickListener {
            noteBtnId = "button6"
            val btnTextList = ArrayList<String>()
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button1"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button2"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button3"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button4"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button5"))
            btnTextList.add(SharedPreferenceManager.getCustomButtonMsg(context, "button6"))
            val removedText = removeStringsFromLongString(btnTextList, NoteEt!!.text.toString())
            NoteEt?.setText(SharedPreferenceManager.getCustomButtonMsg(context, "button6")+" "+removedText)
            NoteEt?.setSelection(NoteEt?.text?.length ?: 0)
        }
        n1Btn?.setOnLongClickListener {
            context.showUpdateCustomButton("button1", n1Btn, btn1Color)
            true
        }
        n2Btn?.setOnLongClickListener {
            context.showUpdateCustomButton("button2", n2Btn, btn2Color)
            true
        }
        n3Btn?.setOnLongClickListener {
            context.showUpdateCustomButton("button3", n3Btn, btn3Color)
            true
        }
        n4Btn?.setOnLongClickListener {
            context.showUpdateCustomButton("button4", n4Btn, btn4Color)
            true
        }
        n5Btn?.setOnLongClickListener {
            context.showUpdateCustomButton("button5", n5Btn, btn4Color)
            true
        }
        n6Btn?.setOnLongClickListener {
            context.showUpdateCustomButton("button6", n6Btn, btn4Color)
            true
        }


    }
    private fun showMessageDialog(data: PhoneAccount) {
        val builder = AlertDialog.Builder(context)
        val dialogView = context.layoutInflater.inflate(R.layout.message_note, null)
        builder.setView(dialogView)
        val msgDialog = builder.create()
        msgDialog?.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent);
        msgDialog?.show()


        val messageTitle = msgDialog?.findViewById<TextView>(R.id.messageTitleTv)
        val sendMessageBtn = msgDialog?.findViewById<Button>(R.id.messageNoteBtn)
        val messageEt = msgDialog?.findViewById<EditText>(R.id.messageEt)

        val n1Btn = msgDialog?.findViewById<Button>(R.id.messageNoteBtn1)
        val n2Btn = msgDialog?.findViewById<Button>(R.id.messageNoteBtn2)
        val n3Btn = msgDialog?.findViewById<Button>(R.id.messageNoteBtn3)
        val n4Btn = msgDialog?.findViewById<Button>(R.id.messageNoteBtn4)

        messageTitle?.setText("Enter your message \nPhone: (${data.phone}\nFullname: ${data.name + data.lastName} \nAddress: ${data.address})")

        sendMessageBtn?.setOnClickListener {
            val newNote = messageEt?.text.toString()
            sendSms(data.phone, newNote, msgDialog)
        }

        n1Btn?.setText(SharedPreferenceManager.getCustomButton(context, "buttonMsg1"))
        n2Btn?.setText(SharedPreferenceManager.getCustomButton(context, "buttonMsg2"))
        n3Btn?.setText(SharedPreferenceManager.getCustomButton(context, "buttonMsg3"))
        n4Btn?.setText(SharedPreferenceManager.getCustomButton(context, "buttonMsg4"))

        n1Btn?.setOnClickListener {
            messageEt?.setText(SharedPreferenceManager.getCustomButtonMsg(context, "buttonMsg1"))
            messageEt?.setSelection(messageEt?.text?.length ?: 0)
        }
        n2Btn?.setOnClickListener {
            messageEt?.setText(SharedPreferenceManager.getCustomButtonMsg(context, "buttonMsg2"))
            messageEt?.setSelection(messageEt?.text?.length ?: 0)
        }
        n3Btn?.setOnClickListener {
            messageEt?.setText(SharedPreferenceManager.getCustomButtonMsg(context, "buttonMsg3"))
            messageEt?.setSelection(messageEt?.text?.length ?: 0)
        }
        n4Btn?.setOnClickListener {
            messageEt?.setText(SharedPreferenceManager.getCustomButtonMsg(context, "buttonMsg4"))
            messageEt?.setSelection(messageEt?.text?.length ?: 0)
        }
        n1Btn?.setOnLongClickListener {
            context.showUpdateCustomButton("buttonMsg1", n1Btn)
            true
        }
        n2Btn?.setOnLongClickListener {
            context.showUpdateCustomButton("buttonMsg2", n2Btn)
            true
        }
        n3Btn?.setOnLongClickListener {
            context.showUpdateCustomButton("buttonMsg3", n3Btn)
            true
        }
        n4Btn?.setOnLongClickListener {
            context.showUpdateCustomButton("buttonMsg4", n4Btn)
            true
        }

    }

    override fun getItemCount(): Int {
        return phoneList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val phoneTv = itemView.findViewById<TextView>(R.id.phoneDTv)
        val statusTv = itemView.findViewById<TextView>(R.id.statusDTv)
        val phoneCl = itemView.findViewById<ConstraintLayout>(R.id.phoneItemDCl)
        val countStatTv = itemView.findViewById<TextView>(R.id.countStatTv)
        val noteEditIv = itemView.findViewById<ImageView>(R.id.noteEditIv)
        val sendMsgIv = itemView.findViewById<ImageView>(R.id.sendMsgIv)
        val callIv = itemView.findViewById<ImageView>(R.id.callIv)
    }


    fun formatDate(milliseconds: Long): String {
        val inputDate = Date(milliseconds)
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return outputFormat.format(inputDate)
    }



    fun sendSms(phoneNumber: String, message: String = "", msgDialog: AlertDialog) {
        val smsUri = Uri.parse("smsto:$phoneNumber")
        val intent = Intent(Intent.ACTION_SENDTO, smsUri)
        intent.putExtra("sms_body", message)

        try {
            context.startActivity(intent)
            Toast.makeText(context, "Opening messaging app", Toast.LENGTH_LONG).show()
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No messaging app found", Toast.LENGTH_LONG).show()
        }

        msgDialog.dismiss()
    }


    fun setupPause(index: Int) {
        pausedPosition = index
        notifyDataSetChanged()
    }




}