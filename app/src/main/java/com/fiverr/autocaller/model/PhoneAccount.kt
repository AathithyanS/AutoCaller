package com.fiverr.autocaller.model

import com.fiverr.autocaller.R

data class PhoneAccount(
    val id: String = "",
    val cell: String = "",
    val name: String = "",
    val lastName: String = "",
    val phone: String = "",
    val address: String = "",
    val note: String = "",
    val calledTime: String = "",
    val suburb: String = "",
    val state: String = "",
    val noteBtn: String = "default"
)
