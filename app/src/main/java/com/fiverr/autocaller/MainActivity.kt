package com.fiverr.autocaller

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.fiverr.autocaller.RvAdapter.FilesRvAdapter
import com.fiverr.autocaller.database.DatabaseHelper
import com.fiverr.autocaller.databinding.ActivityMainBinding
import com.fiverr.autocaller.model.PhoneAccount
import com.fiverr.autocaller.util.CallManager
import org.apache.poi.ss.usermodel.WorkbookFactory

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
                rows.next() //skiping the column name
                while (rows.hasNext()) {
                    val currentRow = rows.next()
                    val cellIterator = currentRow.iterator()

                    val rowData = arrayOfNulls<String>(4)

                    // Iterate over cells (columns)
                    var columnIndex = 0
                    while (cellIterator.hasNext() && columnIndex < 4) {
                        val cell = cellIterator.next()
                        rowData[columnIndex] = cell.getStringCellValue()
                        columnIndex++
                    }

                    // Ensure there are at least 4 columns in the row
                    if (rowData.size >= 4 && rowData[2]?.isNotBlank() == true) {
                        // Save phone data to database
                        dbHelper.insertPhoneData(fileInfoId, rowData[0] ?: "", rowData[1] ?: "", rowData[2] ?: "", rowData[3] ?: "")
                    }
                }

                // Print the list of PhoneAccount objects
                val cursor = dbHelper.getPhoneDataForFileInfo(fileInfoId)
                while (cursor!!.moveToNext()) {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    val lastName = cursor.getString(cursor.getColumnIndexOrThrow("last_name"))
                    val phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"))
                    val address = cursor.getString(cursor.getColumnIndexOrThrow("address"))

                    val phoneAccount = PhoneAccount(name, lastName, phone, address)
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

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}