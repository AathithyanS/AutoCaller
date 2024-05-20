package com.fiverr.autocaller

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.telephony.mbms.FileInfo
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.fiverr.autocaller.RvAdapter.FilesRvAdapter
import com.fiverr.autocaller.database.DatabaseHelper
import com.fiverr.autocaller.databinding.ActivityMainBinding
import com.fiverr.autocaller.model.FileData
import com.fiverr.autocaller.model.PhoneAccount
import com.fiverr.autocaller.providers.Providers
import com.fiverr.autocaller.util.CallManager
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xdgf.util.Util

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val PICK_XLSX_FILE_REQUEST = 1
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        binding.addBtn.setOnClickListener {
            openXlsxFile()
        }

        binding.homeMoreIv.setOnClickListener{
            showPopupMenu(it)
        }
        setupRv()
    }

    fun setupRv() {
        val data = dbHelper.getAllFileInfo()
        val adapter = FilesRvAdapter(data, this)
        binding.phoneDatasetRv.adapter = adapter
        binding.phoneDatasetRv.layoutManager = LinearLayoutManager(this)
    }

    private fun openXlsxFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" // MIME type for XLSX files
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        startActivityForResult(Intent.createChooser(intent, "Select XLSX File"), PICK_XLSX_FILE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_XLSX_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // Show toast message
                Toast.makeText(this, "Reading phone data. Please wait...", Toast.LENGTH_SHORT).show()
                // Handle the selected XLSX file URI
                handleXlsxFile(uri)
            }
        }
    }

    private fun handleXlsxFile(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0) // Assuming the first sheet
                val rows = sheet.iterator()

                val fileInfoId = dbHelper.insertFileInfo(System.currentTimeMillis(), uri.toString(), getFileNameFromUri(uri))
                setupRv()

                // Iterate over rows
                rows.next() // Skipping the column name
                while (rows.hasNext()) {
                    val currentRow = rows.next()
                    val cellIterator = currentRow.iterator()

                    val rowData = arrayOfNulls<String>(7) // Adjusted for 7 columns

                    // Iterate over cells (columns)
                    var columnIndex = 0
                    while (cellIterator.hasNext() && columnIndex < 7) {
                        val cell = cellIterator.next()
                        rowData[columnIndex] = when (cell.cellType) {
                            CellType.NUMERIC -> cell.numericCellValue.toString()
                            else -> cell.stringCellValue
                        }
                        columnIndex++
                    }

                    // Ensure there are at least 7 columns in the row
                    if (rowData.size >= 7 && rowData[3]?.isNotBlank() == true) {
                        // Save phone data to database
                        dbHelper.insertPhoneData(
                            fileInfoId = fileInfoId,
                            name = rowData[1] ?: "", // Name
                            lastName = rowData[2] ?: "", // Last Name
                            phone = rowData[3] ?: "", // Cell Number
                            cellNumber = rowData[0] ?: "", // Phone
                            address = rowData[4] ?: "", // Address
                            suburb = rowData[5] ?: "",  // Suburb
                            state = rowData[6] ?: ""   // State¸
                        )

                    }
                }

                // Print the list of PhoneAccount objects
                val cursor = dbHelper.getPhoneDataForFileInfo(fileInfoId)
                while (cursor?.moveToNext() == true) {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    val lastName = cursor.getString(cursor.getColumnIndexOrThrow("last_name"))
                    val phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"))
                    val address = cursor.getString(cursor.getColumnIndexOrThrow("address"))
                    val suburb = cursor.getString(cursor.getColumnIndexOrThrow("suburb")) // Retrieve suburb
                    val state = cursor.getString(cursor.getColumnIndexOrThrow("state")) // Retrieve state

                    val phoneAccount = PhoneAccount(name = name, lastName = lastName, phone = phone, address = address, suburb = suburb, state = state)
                    Log.d("PhoneData", phoneAccount.toString())
                }
                cursor?.close()
            }
        } catch (e: Exception) {
            Log.e("ExcelError", "Error reading XLSX file: ${e.message}", e)
            Toast.makeText(this, "Error reading XLSX file", Toast.LENGTH_SHORT).show()
        }
    }





    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = ""
        val contentResolver: ContentResolver = applicationContext.contentResolver
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex: Int = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                fileName = it.getString(nameIndex)
            }
        }
        return fileName
    }


    fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.home_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.schedule_call -> {
                    showScheduleCallList(dbHelper.getAllFileInfo());
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    @SuppressLint("MissingInflatedId")
    private fun showScheduleCallList(fileList: ArrayList<FileData>) {
        val context = this;
        // Inflate the custom layout for the dialog
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_schedule_call_list, null)

        // Initialize views
        val layoutCheckboxList = dialogView.findViewById<LinearLayout>(R.id.layout_checkbox_list)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btn_cancel)
        val btnSchedule = dialogView.findViewById<TextView>(R.id.btn_schedule)

        // Add checkboxes dynamically for each item in fileList
        for (fileInfo in fileList) {
            val checkBox = CheckBox(context)
            checkBox.text = fileInfo.fileName
            layoutCheckboxList.addView(checkBox)
        }

        // Build the dialog
        val dialogBuilder = AlertDialog.Builder(context)
            .setView(dialogView)
        val dialog = dialogBuilder.create()

        // Set onClickListener for Cancel button
        btnCancel.setOnClickListener {
            dialog.dismiss() // Close the dialog
        }

        // Set onClickListener for Schedule button
        btnSchedule.setOnClickListener {
            // Get selected files
            val selectedFiles = ArrayList<FileData>()
            for (i in 0 until layoutCheckboxList.childCount) {
                val checkBox = layoutCheckboxList.getChildAt(i) as CheckBox
                if (checkBox.isChecked) {
                    selectedFiles.add(fileList[i])
                }
            }
            makeListCall(selectedFiles)
            dialog.dismiss() // Close the dialog
        }

        // Show the dialog
        dialog.show()
    }

    private fun makeListCall(selectedFiles: ArrayList<FileData>) {
        if (selectedFiles.size>0){
            Providers.isSchedule = true
            Providers.currentScheduleIndex = 0
            Providers.setUpSchedule(selectedFiles)
            Providers.startSchedule(this)
        }else{
            Providers.isSchedule = true
            Providers.currentScheduleIndex = 0
            Providers.isSchedule = false
            Toast.makeText(this, "No any list selected!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}