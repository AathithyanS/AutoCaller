package com.fiverr.autocaller.util

class Util {
    companion object{
        fun removeStringsFromLongString(stringsToRemove: ArrayList<String>, longString: String): String {
            var modifiedString = longString
            for (str in stringsToRemove) {
                modifiedString = modifiedString.replace(str, "", ignoreCase = true)
            }
            return modifiedString
        }
    }
}