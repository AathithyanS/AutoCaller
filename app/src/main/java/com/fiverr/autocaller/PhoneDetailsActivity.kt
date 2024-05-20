package com.fiverr.autocaller

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.fiverr.autocaller.database.DatabaseHelper
import com.fiverr.autocaller.databinding.ActivityPhoneDetailsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhoneDetailsActivity : AppCompatActivity() {

    lateinit var binding: ActivityPhoneDetailsBinding;
    lateinit var dbHelper: DatabaseHelper;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        val id = intent.getStringExtra("id")

        val data = dbHelper.getPhoneAccountById(id.toString());

        if (data != null){
            binding.phoneNumDeTv.setText(data.phone)
            binding.nameDeTv.setText(data.name)
            binding.lastNameDeTv.setText(data.lastName)
            val status = if (data.note.isEmpty()) "N/A" else formatDate(data.calledTime.toLong())
            binding.statusDeTv.setText(status)
            val date = if (data.calledTime.isEmpty()) "N/A" else formatDate(data.calledTime.toLong())
            binding.dateDeTv.setText(date)
            binding.addressDeTv.setText(data.address)
        }


    }

    fun formatDate(milliseconds: Long): String {
        val inputDate = Date(milliseconds)
        val outputFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        return outputFormat.format(inputDate)
    }
}