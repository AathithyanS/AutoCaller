package com.fiverr.autocaller.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.telephony.mbms.FileInfo
import com.fiverr.autocaller.model.FileData
import com.fiverr.autocaller.model.PhoneAccount

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 2
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
        private const val COL_CELL_NUMBER = "cell_number" // New column
        private const val COL_ADDRESS = "address"
        private const val COL_SUBURB = "suburb" // New column
        private const val COL_STATE = "state" // New column
        private const val COL_STATUS = "status"
        private const val COL_DATE = "date"
        private const val COL_NOTEBTN = "noteBtn"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createFileInfoTable = "CREATE TABLE $TABLE_FILE_INFO ($COL_FILE_INFO_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_SELECTED_DATE INTEGER, $COL_FILE_URI TEXT, $COL_FILE_NAME TEXT)"
        val createPhoneDataTable = "CREATE TABLE $TABLE_PHONE_DATA ($COL_PHONE_DATA_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_FILE_INFO_FOREIGN_KEY INTEGER, $COL_NAME TEXT, $COL_LAST_NAME TEXT, $COL_PHONE TEXT, $COL_CELL_NUMBER TEXT, $COL_ADDRESS TEXT, $COL_SUBURB TEXT, $COL_STATE TEXT, $COL_STATUS TEXT, $COL_DATE INTEGER, $COL_NOTEBTN TEXT, FOREIGN KEY($COL_FILE_INFO_FOREIGN_KEY) REFERENCES $TABLE_FILE_INFO($COL_FILE_INFO_ID))"

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

    fun insertPhoneData(fileInfoId: Long, name: String, lastName: String, phone: String, cellNumber: String, address: String, suburb: String, state: String): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COL_FILE_INFO_FOREIGN_KEY, fileInfoId)
        contentValues.put(COL_NAME, name)
        contentValues.put(COL_LAST_NAME, lastName)
        contentValues.put(COL_PHONE, phone)
        contentValues.put(COL_CELL_NUMBER, cellNumber) // Inserting value for new column
        contentValues.put(COL_ADDRESS, address)
        contentValues.put(COL_SUBURB, suburb) // Inserting value for new column
        contentValues.put(COL_STATE, state) // Inserting value for new column
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
                val cellNumber = it.getString(it.getColumnIndex(COL_CELL_NUMBER)) // Retrieving value from new column
                val address = it.getString(it.getColumnIndex(COL_ADDRESS))
                val suburb = it.getString(it.getColumnIndex(COL_SUBURB)) // Retrieving value from new column
                val state = it.getString(it.getColumnIndex(COL_STATE)) // Retrieving value from new column
                val status = it.getString(it.getColumnIndex(COL_STATUS)) ?: ""
                val date = it.getString(it.getColumnIndex(COL_DATE)) ?: ""
                val noteBtn = it.getString(it.getColumnIndex(COL_NOTEBTN)) ?: ""

                val phoneAccount = PhoneAccount(id = id, name = name, lastName = lastName, phone = phone, cell = cellNumber, address = address, suburb = suburb, state = state, note = status, calledTime = date, noteBtn = noteBtn)
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
                val cellNumber = it.getString(it.getColumnIndex(COL_CELL_NUMBER)) // Retrieve value from new column
                val address = it.getString(it.getColumnIndex(COL_ADDRESS))
                val suburb = it.getString(it.getColumnIndex(COL_SUBURB)) // Retrieve value from new column
                val state = it.getString(it.getColumnIndex(COL_STATE)) // Retrieve value from new column
                val status = it.getString(it.getColumnIndex(COL_STATUS)) ?: ""
                val date = it.getString(it.getColumnIndex(COL_DATE)) ?: ""

                phoneAccount = PhoneAccount(id, name, lastName, phone, cellNumber, address, suburb, state, status, date)
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
    fun updatePhoneData(id: Int, status: String, date: Long, noteBtn: String) {
        val db = this.writableDatabase

        val values = ContentValues().apply {
            put(COL_STATUS, status)
            put(COL_DATE, date)
            put(COL_NOTEBTN, noteBtn)
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

    fun updateNoteColor(id: Int, status: String, noteBtn: String) {
        val db = this.writableDatabase

        val values = ContentValues().apply {
            put(COL_STATUS, status)
            put(COL_NOTEBTN, noteBtn)
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

    @SuppressLint("Range")
    fun getPhoneAccountsByFileInfoIdAndStatus(fileInfoId: Int, status: String): ArrayList<PhoneAccount> {
        val phoneAccountsList = ArrayList<PhoneAccount>()
        val db = this.readableDatabase

        val selectQuery = "SELECT * FROM $TABLE_PHONE_DATA WHERE $COL_FILE_INFO_FOREIGN_KEY = ? AND $COL_STATUS = ?"
        val selectionArgs = arrayOf(fileInfoId.toString(), status)

        val cursor: Cursor? = db.rawQuery(selectQuery, selectionArgs)
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getString(it.getColumnIndex(COL_PHONE_DATA_ID))
                val name = it.getString(it.getColumnIndex(COL_NAME))
                val lastName = it.getString(it.getColumnIndex(COL_LAST_NAME))
                val phone = it.getString(it.getColumnIndex(COL_PHONE))
                val cellNumber = it.getString(it.getColumnIndex(COL_CELL_NUMBER))
                val address = it.getString(it.getColumnIndex(COL_ADDRESS))
                val suburb = it.getString(it.getColumnIndex(COL_SUBURB))
                val state = it.getString(it.getColumnIndex(COL_STATE))
                val status = it.getString(it.getColumnIndex(COL_STATUS)) ?: ""
                val date = it.getString(it.getColumnIndex(COL_DATE)) ?: ""

                val phoneAccount = PhoneAccount(id = id, name = name, lastName = lastName, phone = phone, cell = cellNumber, address = address, suburb = suburb, state = state, note = status, calledTime = date)
                phoneAccountsList.add(phoneAccount)
            }
        }
        cursor?.close()
        return phoneAccountsList
    }


    @SuppressLint("Range")
    fun getFileInfoById(fileInfoId: Int): FileData? {
        val db = this.readableDatabase
        var fileInfo: FileData? = null

        val selectQuery = "SELECT * FROM $TABLE_FILE_INFO WHERE $COL_FILE_INFO_ID = ?"
        val selectionArgs = arrayOf(fileInfoId.toString())

        val cursor: Cursor? = db.rawQuery(selectQuery, selectionArgs)
        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getInt(it.getColumnIndex(COL_FILE_INFO_ID))
                val selectedDate = it.getLong(it.getColumnIndex(COL_SELECTED_DATE))
                val fileUri = it.getString(it.getColumnIndex(COL_FILE_URI))
                val fileName = it.getString(it.getColumnIndex(COL_FILE_NAME))

                fileInfo = FileData(id, selectedDate.toString(), fileUri, fileName)
            }
        }
        cursor?.close()
        return fileInfo
    }


    @SuppressLint("Range")
    fun getPhoneDataByStatusAndFileId(status: String, fileInfoId: Long): ArrayList<PhoneAccount> {
        val phoneAccountsList = ArrayList<PhoneAccount>()
        val db = this.readableDatabase

        // Query to retrieve phone data where the status value includes the word "answered" and file ID matches
        val selectQuery = "SELECT * FROM $TABLE_PHONE_DATA WHERE $COL_STATUS LIKE ? AND $COL_STATUS NOT LIKE 'Unanswered'  AND $COL_FILE_INFO_FOREIGN_KEY = ?"
//        val selectionArgs = arrayOf("%$status%", fileInfoId.toString())
        val selectionArgs = arrayOf(fileInfoId.toString())

        val cursor: Cursor? = db.rawQuery(selectQuery, selectionArgs)
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getString(it.getColumnIndex(COL_PHONE_DATA_ID))
                val name = it.getString(it.getColumnIndex(COL_NAME))
                val lastName = it.getString(it.getColumnIndex(COL_LAST_NAME))
                val phone = it.getString(it.getColumnIndex(COL_PHONE))
                val cellNumber = it.getString(it.getColumnIndex(COL_CELL_NUMBER))
                val address = it.getString(it.getColumnIndex(COL_ADDRESS))
                val suburb = it.getString(it.getColumnIndex(COL_SUBURB))
                val state = it.getString(it.getColumnIndex(COL_STATE))
                val status = it.getString(it.getColumnIndex(COL_STATUS)) ?: ""
                val date = it.getString(it.getColumnIndex(COL_DATE)) ?: ""

                val phoneAccount = PhoneAccount(id = id, name = name, lastName = lastName, phone = phone, cell = cellNumber, address = address, suburb = suburb, state = state, note = status, calledTime = date)
                phoneAccountsList.add(phoneAccount)
            }
        }
        cursor?.close()
        return phoneAccountsList
    }

    @SuppressLint("Range")
    fun getPhoneDataByStatusAndFileId(fileInfoId: Long): ArrayList<PhoneAccount> {
        val phoneAccountsList = ArrayList<PhoneAccount>()
        val leadAccountsList = ArrayList<PhoneAccount>()
        val db = this.readableDatabase

        // Query to retrieve phone accounts where the file info ID matches, the status is not like "Unanswered",
        // and the status is not empty
        val selectQuery = "SELECT * FROM $TABLE_PHONE_DATA WHERE $COL_FILE_INFO_FOREIGN_KEY = ? AND $COL_STATUS NOT LIKE 'Unanswered' AND $COL_STATUS IS NOT NULL AND $COL_STATUS != ''"
        val selectionArgs = arrayOf(fileInfoId.toString())
        val cursor: Cursor? = db.rawQuery(selectQuery, selectionArgs)
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getString(it.getColumnIndex(COL_PHONE_DATA_ID))
                val name = it.getString(it.getColumnIndex(COL_NAME))
                val lastName = it.getString(it.getColumnIndex(COL_LAST_NAME))
                val phone = it.getString(it.getColumnIndex(COL_PHONE))
                val cellNumber = it.getString(it.getColumnIndex(COL_CELL_NUMBER))
                val address = it.getString(it.getColumnIndex(COL_ADDRESS))
                val suburb = it.getString(it.getColumnIndex(COL_SUBURB))
                val state = it.getString(it.getColumnIndex(COL_STATE))
                val status = it.getString(it.getColumnIndex(COL_STATUS)) ?: ""
                val date = it.getString(it.getColumnIndex(COL_DATE)) ?: ""
                val noteBtn = it.getString(it.getColumnIndex(COL_NOTEBTN)) ?: ""

                val phoneAccount = PhoneAccount(id = id, name = name, lastName = lastName, phone = phone, cell = cellNumber, address = address, suburb = suburb, state = state, note = status, calledTime = date, noteBtn = noteBtn)
                phoneAccountsList.add(phoneAccount)
            }
        }
        cursor?.close()

        val leadQuery = "SELECT * FROM $TABLE_PHONE_DATA WHERE $COL_FILE_INFO_FOREIGN_KEY = ? AND ($COL_STATUS LIKE 'Unanswered%' OR $COL_STATUS IS NULL OR $COL_STATUS = '')"
        val leadCursor: Cursor? = db.rawQuery(leadQuery, selectionArgs)
        leadCursor?.use {
            while (it.moveToNext()) {
                val id = it.getString(it.getColumnIndex(COL_PHONE_DATA_ID))
                val name = it.getString(it.getColumnIndex(COL_NAME))
                val lastName = it.getString(it.getColumnIndex(COL_LAST_NAME))
                val phone = it.getString(it.getColumnIndex(COL_PHONE))
                val cellNumber = it.getString(it.getColumnIndex(COL_CELL_NUMBER))
                val address = it.getString(it.getColumnIndex(COL_ADDRESS))
                val suburb = it.getString(it.getColumnIndex(COL_SUBURB))
                val state = it.getString(it.getColumnIndex(COL_STATE))
                val status = it.getString(it.getColumnIndex(COL_STATUS)) ?: ""
                val date = it.getString(it.getColumnIndex(COL_DATE)) ?: ""
                val noteBtn = it.getString(it.getColumnIndex(COL_NOTEBTN)) ?: ""

                val phoneAccount = PhoneAccount(id = id, name = name, lastName = lastName, phone = phone, cell = cellNumber, address = address, suburb = suburb, state = state, note = status, calledTime = date, noteBtn = noteBtn)
                leadAccountsList.add(phoneAccount)
            }
        }
        leadCursor?.close()

        // Append lead accounts to the main list
        phoneAccountsList.addAll(leadAccountsList)
        return phoneAccountsList
    }

    fun addPhoneAccounts(phoneAccounts: ArrayList<PhoneAccount>, fileInfoId: Long): Int {
        val db = this.writableDatabase
        var insertedCount = 0

        db.beginTransaction()
        try {
            for (phoneAccount in phoneAccounts) {
                val contentValues = ContentValues().apply {
                    put(COL_FILE_INFO_FOREIGN_KEY, fileInfoId)
                    put(COL_NAME, phoneAccount.name)
                    put(COL_LAST_NAME, phoneAccount.lastName)
                    put(COL_PHONE, phoneAccount.phone)
                    put(COL_CELL_NUMBER, phoneAccount.cell)
                    put(COL_ADDRESS, phoneAccount.address)
                    put(COL_SUBURB, phoneAccount.suburb)
                    put(COL_STATE, phoneAccount.state)
                    put(COL_STATUS, phoneAccount.note)
                    put(COL_DATE, phoneAccount.calledTime)
                    put(COL_NOTEBTN, phoneAccount.noteBtn)
                }

                val rowId = db.insert(TABLE_PHONE_DATA, null, contentValues)
                if (rowId != -1L) {
                    insertedCount++
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        return insertedCount
    }

    @SuppressLint("Range")
    fun getPhoneAccountsByStatusKeywordAndFileInfoId(fileInfoId: Long, statusKeyword: String): ArrayList<PhoneAccount> {
        val phoneAccountsList = ArrayList<PhoneAccount>()
        val db = this.readableDatabase

        // Query to retrieve phone data where the status value contains the keyword and file ID matches
        val selectQuery = "SELECT * FROM $TABLE_PHONE_DATA WHERE $COL_STATUS LIKE ? AND $COL_FILE_INFO_FOREIGN_KEY = ?"
        val selectionArgs = arrayOf("%$statusKeyword%", fileInfoId.toString())

        val cursor: Cursor? = db.rawQuery(selectQuery, selectionArgs)
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getString(it.getColumnIndex(COL_PHONE_DATA_ID))
                val name = it.getString(it.getColumnIndex(COL_NAME))
                val lastName = it.getString(it.getColumnIndex(COL_LAST_NAME))
                val phone = it.getString(it.getColumnIndex(COL_PHONE))
                val cellNumber = it.getString(it.getColumnIndex(COL_CELL_NUMBER))
                val address = it.getString(it.getColumnIndex(COL_ADDRESS))
                val suburb = it.getString(it.getColumnIndex(COL_SUBURB))
                val state = it.getString(it.getColumnIndex(COL_STATE))
                val status = it.getString(it.getColumnIndex(COL_STATUS)) ?: ""
                val date = it.getString(it.getColumnIndex(COL_DATE)) ?: ""
                val noteBtn = it.getString(it.getColumnIndex(COL_NOTEBTN)) ?: ""

                val phoneAccount = PhoneAccount(
                    id = id,
                    name = name,
                    lastName = lastName,
                    phone = phone,
                    cell = cellNumber,
                    address = address,
                    suburb = suburb,
                    state = state,
                    note = status,
                    calledTime = date,
                    noteBtn = noteBtn
                )
                phoneAccountsList.add(phoneAccount)
            }
        }
        cursor?.close()
        return phoneAccountsList
    }

    fun countLeadsInFile(fileInfoId: Long): Int {
        val db = this.readableDatabase

        // Query to count leads in the file where the file info ID matches and the status contains "Lead"
        val selectQuery = "SELECT COUNT(*) FROM $TABLE_PHONE_DATA WHERE $COL_FILE_INFO_FOREIGN_KEY = ? AND $COL_STATUS LIKE '%Lead%'"
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







}
