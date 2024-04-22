package com.fiverr.autocaller.RvAdapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.fiverr.autocaller.MainActivity
import com.fiverr.autocaller.PhoneListActivity
import com.fiverr.autocaller.R
import com.fiverr.autocaller.database.DatabaseHelper
import com.fiverr.autocaller.model.FileData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FilesRvAdapter(val filesList: ArrayList<FileData>, val context: MainActivity) : RecyclerView.Adapter<FilesRvAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilesRvAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.data_rv_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val data = filesList.get(position)
        holder.fileName.setText(data.fileName)
        holder.date.setText(formatDate(data.selectedData.toLong()))
        holder.deleteIv.setOnClickListener {
            showRestartAlertDialog(data)

        }

        holder.fileRvCl.setOnClickListener {
            val intent = Intent(context, PhoneListActivity::class.java)
            intent.putExtra("id", data.id)
            context.startActivity(intent)
        }

    }

    override fun getItemCount(): Int {
        return filesList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileName = itemView.findViewById<TextView>(R.id.fileNameTv)
        val date = itemView.findViewById<TextView>(R.id.dateTv)
        val deleteIv = itemView.findViewById<ImageView>(R.id.deleteFileIv)
        val fileRvCl = itemView.findViewById<ConstraintLayout>(R.id.fileRvCl)
    }

    fun formatDate(milliseconds: Long): String {
        val inputDate = Date(milliseconds)
        val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return outputFormat.format(inputDate)
    }

    fun showRestartAlertDialog(data: FileData) {
        val builder3 = AlertDialog.Builder(context)
        val dialogView3 = context.layoutInflater.inflate(R.layout.delete_alert_dialog, null)
        builder3.setView(dialogView3)
        val alertDialog = builder3.create()
        alertDialog?.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent);
        alertDialog?.show()

        val deleteBtn = alertDialog.findViewById<Button>(R.id.listDeleteBtn)
        val canceltBtn = alertDialog.findViewById<Button>(R.id.listCancelBtn)

        deleteBtn?.setOnClickListener {
            val dbHelper = DatabaseHelper(context)
            dbHelper.deleteFileInfoAndPhoneData(data.id)
            context.setupRv()
            alertDialog.dismiss()
        }

        canceltBtn?.setOnClickListener {
            alertDialog.dismiss()
        }
    }

}