package com.fiverr.autocaller.util
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.fiverr.autocaller.R
import com.fiverr.autocaller.model.PhoneAccount
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFColor
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
        headerRow.createCell(0).setCellValue("Cell Number")
        headerRow.createCell(1).setCellValue("Name")
        headerRow.createCell(2).setCellValue("Last Name")
        headerRow.createCell(3).setCellValue("Phone")
        headerRow.createCell(4).setCellValue("Address")
        headerRow.createCell(5).setCellValue("Suburb")
        headerRow.createCell(6).setCellValue("State")
        headerRow.createCell(7).setCellValue("Notes")
//        headerRow.createCell(6).setCellValue("Called Time")

        var btn1Color = SharedPreferenceManager.getCustomButtonColor(context, "button1-color")
        val btn2Color = SharedPreferenceManager.getCustomButtonColor(context, "button2-color")
        val btn3Color = SharedPreferenceManager.getCustomButtonColor(context, "button3-color")
        val btn4Color = SharedPreferenceManager.getCustomButtonColor(context, "button4-color")



        val colorBytes1 = byteArrayOf(
            Color.red(btn1Color).toByte(),
            Color.green(btn1Color).toByte(),
            Color.blue(btn1Color).toByte()
        )
        val backgroundColor1 = XSSFColor(colorBytes1, null)  // XSSFColor for RGB colors
        val backgroundStyle1: CellStyle = workbook.createCellStyle()
        if (btn1Color != ContextCompat.getColor(context, R.color.defaultColor)){
            backgroundStyle1.apply {
                setFillForegroundColor(backgroundColor1)
                fillPattern = FillPatternType.SOLID_FOREGROUND
            }
        }



        val colorBytes2 = byteArrayOf(
            Color.red(btn2Color).toByte(),
            Color.green(btn2Color).toByte(),
            Color.blue(btn2Color).toByte()
        )
        val backgroundColor2 = XSSFColor(colorBytes2, null)  // XSSFColor for RGB colors
        val backgroundStyle2: CellStyle = workbook.createCellStyle()
        if (btn2Color != ContextCompat.getColor(context, R.color.defaultColor)){
            backgroundStyle2.apply {
                setFillForegroundColor(backgroundColor2)
                fillPattern = FillPatternType.SOLID_FOREGROUND
            }
        }


        val colorBytes3 = byteArrayOf(
            Color.red(btn3Color).toByte(),
            Color.green(btn3Color).toByte(),
            Color.blue(btn3Color).toByte()
        )
        val backgroundColor3 = XSSFColor(colorBytes3, null)  // XSSFColor for RGB colors
        val backgroundStyle3: CellStyle = workbook.createCellStyle()
        if (btn3Color != ContextCompat.getColor(context, R.color.defaultColor)){
            backgroundStyle3.apply {
                setFillForegroundColor(backgroundColor3)
                fillPattern = FillPatternType.SOLID_FOREGROUND
            }
        }


        val colorBytes4 = byteArrayOf(
            Color.red(btn4Color).toByte(),
            Color.green(btn4Color).toByte(),
            Color.blue(btn4Color).toByte()
        )
        val backgroundColor4 = XSSFColor(colorBytes4, null)  // XSSFColor for RGB colors
        val backgroundStyle4: CellStyle = workbook.createCellStyle()
        if (btn4Color != ContextCompat.getColor(context, R.color.defaultColor)){
            backgroundStyle4.apply {
                setFillForegroundColor(backgroundColor4)
                fillPattern = FillPatternType.SOLID_FOREGROUND
            }
        }



        // Add data rows
        var rowNum = 1
        for (phoneAccount in dataList) {
            var date = ""
            if (phoneAccount.calledTime.isNotEmpty()){
                date = formatDate(phoneAccount.calledTime.toLong())
                date = date + " "
            }
            val row: Row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue((rowNum - 1).toString())
            row.createCell(1).setCellValue(phoneAccount.name)
            row.createCell(2).setCellValue(phoneAccount.lastName)
            row.createCell(3).setCellValue(phoneAccount.phone)
            row.createCell(4).setCellValue(phoneAccount.address)
            row.createCell(5).setCellValue(phoneAccount.suburb)
            row.createCell(6).setCellValue(phoneAccount.state)
            row.createCell(7).setCellValue(date + phoneAccount.note)
//            row.createCell(6).setCellValue(date)
            for (cellIndex in 0..7) {
                if (phoneAccount.noteBtn == "button1") {
                    row.getCell(cellIndex).cellStyle = backgroundStyle1
                }else if (phoneAccount.noteBtn == "button2") {
                    row.getCell(cellIndex).cellStyle = backgroundStyle2
                }else if (phoneAccount.noteBtn == "button3") {
                    row.getCell(cellIndex).cellStyle = backgroundStyle3
                }else if (phoneAccount.noteBtn == "button4") {
                    row.getCell(cellIndex).cellStyle = backgroundStyle4
                }
            }
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
