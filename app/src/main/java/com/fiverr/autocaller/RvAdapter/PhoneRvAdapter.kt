package com.fiverr.autocaller.RvAdapter

import android.content.Intent
import android.telecom.Call
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
import com.fiverr.autocaller.MainActivity
import com.fiverr.autocaller.PhoneDetailsActivity
import com.fiverr.autocaller.PhoneListActivity
import com.fiverr.autocaller.R
import com.fiverr.autocaller.model.FileData
import com.fiverr.autocaller.model.PhoneAccount
import com.fiverr.autocaller.util.CallManager
import com.fiverr.autocaller.util.SharedPreferenceManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhoneRvAdapter(val phoneList: ArrayList<PhoneAccount>, val context: PhoneListActivity, val fieldId: Int) : RecyclerView.Adapter<PhoneRvAdapter.ViewHolder>() {


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
            holder.statusTv.setText("Name: "+data.name)
        }
        if (data.lastName.isEmpty()) {
            holder.statusTv.setText(holder.statusTv.text.toString()+"\nLastname: N/A")
        }else{
            holder.statusTv.setText(holder.statusTv.text.toString()+"\nLastname: "+data.lastName)
        }
        if (data.address.isEmpty()) {
            holder.statusTv.setText(holder.statusTv.text.toString()+"\nAddress: N/A")
        }else{
            holder.statusTv.setText(holder.statusTv.text.toString()+"\nAddress: "+data.address)
        }
        if (data.calledTime.isEmpty()) {
            holder.statusTv.setText(holder.statusTv.text.toString()+"\nDate: N/A")
        }else{
            holder.statusTv.setText(holder.statusTv.text.toString()+"\nDate: "+formatDate(data.calledTime.toLong()))
        }
        if (data.status.isEmpty()) {
            holder.statusTv.setText(holder.statusTv.text.toString()+"\nNote: N/A")
        }else{
            holder.statusTv.setText(holder.statusTv.text.toString()+"\nNote: "+data.status)
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
    }

    private fun showUpdateNoteDialog(data: PhoneAccount) {
        val builder = AlertDialog.Builder(context)
        val dialogView = context.layoutInflater.inflate(R.layout.edit_note_dialog, null)
        builder.setView(dialogView)
        val updateNoteDialog = builder.create()
        updateNoteDialog?.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent);
        updateNoteDialog?.show()


        val updateTitle = updateNoteDialog?.findViewById<TextView>(R.id.updateTitleTv)
        val updateBtn = updateNoteDialog?.findViewById<Button>(R.id.updateNoteBtn)
        val NoteEt = updateNoteDialog?.findViewById<EditText>(R.id.updateNoteEt)

        val n1Btn = updateNoteDialog?.findViewById<Button>(R.id.eNoteBtn1)
        val n2Btn = updateNoteDialog?.findViewById<Button>(R.id.eNoteBtn2)
        val n3Btn = updateNoteDialog?.findViewById<Button>(R.id.eNoteBtn3)
        val n4Btn = updateNoteDialog?.findViewById<Button>(R.id.eNoteBtn4)

        updateTitle?.setText("Update Note(${data.phone}/ ${data.name})")

        NoteEt?.setText(data.status)
        updateBtn?.setOnClickListener {
            val newNote = NoteEt?.text.toString()
            context.dbHelper.updateNote(data.id.toInt(), newNote)
            updateNoteDialog.dismiss()
            updateData(context.dbHelper.getPhoneAccountsByFileInfoId(context.id))
        }

        n1Btn?.setText(SharedPreferenceManager.getCustomButton(context, "button1"))
        n2Btn?.setText(SharedPreferenceManager.getCustomButton(context, "button2"))
        n3Btn?.setText(SharedPreferenceManager.getCustomButton(context, "button3"))
        n4Btn?.setText(SharedPreferenceManager.getCustomButton(context, "button4"))

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
            context.showUpdateCustomButton("button1", n1Btn)
            true
        }
        n2Btn?.setOnLongClickListener {
            context.showUpdateCustomButton("button2", n2Btn)
            true
        }
        n3Btn?.setOnLongClickListener {
            context.showUpdateCustomButton("button3", n3Btn)
            true
        }
        n4Btn?.setOnLongClickListener {
            context.showUpdateCustomButton("button4", n4Btn)
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
    }


    fun formatDate(milliseconds: Long): String {
        val inputDate = Date(milliseconds)
        val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return outputFormat.format(inputDate)
    }


}