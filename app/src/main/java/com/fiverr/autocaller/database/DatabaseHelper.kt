package com.fiverr.autocaller.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.fiverr.autocaller.model.FileData
import com.fiverr.autocaller.model.PhoneAccount

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "PhoneData.db"
        private const val TABLE_FILE_INFO = "file_info"
        private const val TABLE_PHONE_DATA = "phone_data"

        // Columns of file_info table
        private const val COL_FILE_INFO_ID = "id"
        private const val COL_SELECTED_DATE = "selected_date"
        private const val COL_FILE_URI = "file_uri"
        private const val COL_FILE_NAME = "file_name"

        // Columns of phone_data table
        private const val COL_PHONE_DATA_ID = "id"
        private const val COL_FILE_INFO_FOREIGN_KEY = "file_info_id"
        private const val COL_NAME = "name"
        private const val COL_LAST_NAME = "last_name"
        private const val COL_PHONE = "phone"
        private const val COL_ADDRESS = "address"
        private const val COL_STATUS = "status"
        private const val COL_DATE = "date"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createFileInfoTable = "CREATE TABLE $TABLE_FILE_INFO ($COL_FILE_INFO_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_SELECTED_DATE INTEGER, $COL_FILE_URI TEXT, $COL_FILE_NAME TEXT)"
        val createPhoneDataTable = "CREATE TABLE $TABLE_PHONE_DATA ($COL_PHONE_DATA_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_FILE_INFO_FOREIGN_KEY INTEGER, $COL_NAME TEXT, $COL_LAST_NAME TEXT, $COL_PHONE TEXT, $COL_ADDRESS TEXT, $COL_STATUS TEXT, $COL_DATE INTEGER, FOREIGN KEY($COL_FILE_INFO_FOREIGN_KEY) REFERENCES $TABLE_FILE_INFO($COL_FILE_INFO_ID))"

        db?.execSQL(createFileInfoTable)
        db?.execSQL(createPhoneDataTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_PHONE_DATA")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_FILE_INFO")
        onCreate(db)
    }

    fun insertFileInfo(selectedDate: Long, fileUri: String, fileName: String): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COL_SELECTED_DATE, selectedDate)
        contentValues.put(COL_FILE_URI, fileUri)
        contentValues.put(COL_FILE_NAME, fileName)
        return db.insert(TABLE_FILE_INFO, null, contentValues)
    }

    fun insertPhoneData(fileInfoId: Long, name: String, lastName: String, phone: String, address: String): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COL_FILE_INFO_FOREIGN_KEY, fileInfoId)
        contentValues.put(COL_NAME, name)
        contentValues.put(COL_LAST_NAME, lastName)
        contentValues.put(COL_PHONE, phone)
        contentValues.put(COL_ADDRESS, address)
        return db.insert(TABLE_PHONE_DATA, null, contentValues)
    }

    fun getPhoneDataForFileInfo(fileInfoId: Long): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_PHONE_DATA WHERE $COL_FILE_INFO_FOREIGN_KEY = ?", arrayOf(fileInfoId.toString()))
    }

    @SuppressLint("Range")
    fun getAllFileInfo(): ArrayList<FileData> {
        val fileInfoList = ArrayList<FileData>()
        val selectQuery = "SELECT * FROM $TABLE_FILE_INFO ORDER BY $COL_SELECTED_DATE DESC"

        val db = this.readableDatabase
        val cursor: Cursor?

        try {
            cursor = db.rawQuery(selectQuery, null)
        } catch (e: Exception) {
            e.printStackTrace()
            db.execSQL(selectQuery)
            return ArrayList()
        }

        var id: Int
        var selectedDate: String
        var fileUri: String
        var fileName: String

        if (cursor.moveToFirst()) {
            do {
                id = cursor.getInt(cursor.getColumnIndex(COL_FILE_INFO_ID))
                selectedDate = cursor.getString(cursor.getColumnIndex(COL_SELECTED_DATE))
                fileUri = cursor.getString(cursor.getColumnIndex(COL_FILE_URI))
                fileName = cursor.getString(cursor.getColumnIndex(COL_FILE_NAME))

                val fileData = FileData(id, selectedDate, fileUri, fileName)
                fileInfoList.add(fileData)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return fileInfoList
    }

    fun deleteFileInfoAndPhoneData(fileInfoId: Int) {
        val db = this.writableDatabase

        // Delete phone data associated with the file info ID
        db.delete(TABLE_PHONE_DATA, "$COL_FILE_INFO_FOREIGN_KEY = ?", arrayOf(fileInfoId.toString()))

        // Delete file info by its ID
        db.delete(TABLE_FILE_INFO, "$COL_FILE_INFO_ID = ?", arrayOf(fileInfoId.toString()))

        db.close()
    }

    @SuppressLint("Range")
    fun getPhoneAccountsByFileInfoId(fileInfoId: Int): ArrayList<PhoneAccount> {
        val phoneAccountsList = ArrayList<PhoneAccount>()
        val db = this.readableDatabase

        val selectQuery = "SELECT * FROM $TABLE_PHONE_DATA WHERE $COL_FILE_INFO_FOREIGN_KEY = ?"
        val selectionArgs = arrayOf(fileInfoId.toString())

        val cursor: Cursor? = db.rawQuery(selectQuery, selectionArgs)
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getString(it.getColumnIndex(COL_PHONE_DATA_ID))
                val name = it.getString(it.getColumnIndex(COL_NAME))
                val lastName = it.getString(it.getColumnIndex(COL_LAST_NAME))
                val phone = it.getString(it.getColumnIndex(COL_PHONE))
                val address = it.getString(it.getColumnIndex(COL_ADDRESS))
                val status = it.getString(it.getColumnIndex(COL_STATUS)) ?: ""
                val date = it.getString(it.getColumnIndex(COL_DATE)) ?: ""

                val phoneAccount = PhoneAccount(id, name, lastName, phone, address, status, date)
                phoneAccountsList.add(phoneAccount)
            }
        }
        cursor?.close()
        return phoneAccountsList
    }

    @SuppressLint("Range")
    fun getPhoneAccountById(phoneAccountId: String): PhoneAccount? {
        val db = this.readableDatabase

        val selectQuery = "SELECT * FROM $TABLE_PHONE_DATA WHERE $COL_PHONE_DATA_ID = ?"
        val selectionArgs = arrayOf(phoneAccountId)

        val cursor: Cursor? = db.rawQuery(selectQuery, selectionArgs)
        var phoneAccount: PhoneAccount? = null

        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getString(it.getColumnIndex(COL_PHONE_DATA_ID))
                val name = it.getString(it.getColumnIndex(COL_NAME))
                val lastName = it.getString(it.getColumnIndex(COL_LAST_NAME))
                val phone = it.getString(it.getColumnIndex(COL_PHONE))
                val address = it.getString(it.getColumnIndex(COL_ADDRESS))
                val status = it.getString(it.getColumnIndex(COL_STATUS)) ?: ""
                val date = it.getString(it.getColumnIndex(COL_DATE)) ?: ""

                phoneAccount = PhoneAccount(id, name, lastName, phone, address, status, date)
            }
        }
        cursor?.close()
        return phoneAccount
    }

    fun updatePhoneData(id: Int, status: String, date: Long) {
        val db = this.writableDatabase

        val values = ContentValues().apply {
            put(COL_STATUS, status)
            put(COL_DATE, date)
        }

        val selection = "$COL_PHONE_DATA_ID = ?"
        val selectionArgs = arrayOf(id.toString())
        db.update(TABLE_PHONE_DATA, values, selection, selectionArgs)

        db.close()
    }

    fun updateNote(id: Int, status: String) {
        val db = this.writableDatabase

        val values = ContentValues().apply {
            put(COL_STATUS, status)
        }

        val selection = "$COL_PHONE_DATA_ID = ?"
        val selectionArgs = arrayOf(id.toString())
        db.update(TABLE_PHONE_DATA, values, selection, selectionArgs)

        db.close()
    }

    fun countAnsweredRecordsForFileInfo(fileInfoId: Long): Int {
        val db = this.readableDatabase

        val selectQuery = "SELECT COUNT(*) FROM $TABLE_PHONE_DATA WHERE $COL_FILE_INFO_FOREIGN_KEY = ? AND (($COL_STATUS = 'Answered' OR $COL_STATUS != 'Unanswered') AND $COL_STATUS IS NOT NULL AND $COL_STATUS != '')"
//        val selectQuery = "SELECT COUNT(*) FROM $TABLE_PHONE_DATA WHERE $COL_FILE_INFO_FOREIGN_KEY = ? AND ($COL_STATUS = 'Answered')"
        val selectionArgs = arrayOf(fileInfoId.toString())

        val cursor: Cursor? = db.rawQuery(selectQuery, selectionArgs)
        var count = 0

        cursor?.use {
            if (it.moveToFirst()) {
                count = it.getInt(0)
            }
        }
        cursor?.close()
        return count
    }

    fun clearDateAndNoteForFileInfo(fileInfoId: Long) {
        val db = this.writableDatabase

        val values = ContentValues().apply {
            put(COL_DATE, "")
            put(COL_STATUS, "")
        }

        val selection = "$COL_FILE_INFO_FOREIGN_KEY = ?"
        val selectionArgs = arrayOf(fileInfoId.toString())
        db.update(TABLE_PHONE_DATA, values, selection, selectionArgs)

        db.close()
    }



}
