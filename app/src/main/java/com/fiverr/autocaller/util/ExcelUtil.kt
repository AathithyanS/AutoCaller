package com.fiverr.autocaller.util
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.fiverr.autocaller.model.PhoneAccount
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelUtil {

    fun createExcelAndShare(context: Context, dataList: ArrayList<PhoneAccount>) {
        // Create a new Excel workbook
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("PhoneAccounts")

        // Create header row
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("ID")
        headerRow.createCell(1).setCellValue("Name")
        headerRow.createCell(2).setCellValue("Last Name")
        headerRow.createCell(3).setCellValue("Phone")
        headerRow.createCell(4).setCellValue("Address")
        headerRow.createCell(5).setCellValue("Notes")
//        headerRow.createCell(6).setCellValue("Called Time")

        // Add data rows
        var rowNum = 1
        for (phoneAccount in dataList) {
            var date = ""
            if (phoneAccount.calledTime.isNotEmpty()){
                date = formatDate(phoneAccount.calledTime.toLong())
                date = date + " "
            }
            val row: Row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue(phoneAccount.id)
            row.createCell(1).setCellValue(phoneAccount.name)
            row.createCell(2).setCellValue(phoneAccount.lastName)
            row.createCell(3).setCellValue(phoneAccount.phone)
            row.createCell(4).setCellValue(phoneAccount.address)
            row.createCell(5).setCellValue(date + phoneAccount.status)
//            row.createCell(6).setCellValue(date)
        }

        // Save the workbook to a file
        val file = saveExcelFile(context, workbook)

        // Share the generated Excel file
        shareExcelFile(context, file)
    }

    private fun saveExcelFile(context: Context, workbook: XSSFWorkbook): File {
        // Create file
        val file = File(context.getExternalFilesDir(null), "PhoneAccounts.xlsx")
        val fileOutputStream = FileOutputStream(file)
        workbook.write(fileOutputStream)
        fileOutputStream.close()
        return file
    }

    private fun shareExcelFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant read permission to the receiving app
        }
        context.startActivity(Intent.createChooser(intent, "Share Excel File"))
    }

    fun formatDate(milliseconds: Long): String {
        val inputDate = Date(milliseconds)
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return outputFormat.format(inputDate)
    }
}
