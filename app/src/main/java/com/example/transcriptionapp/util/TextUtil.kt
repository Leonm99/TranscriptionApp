package com.example.transcriptionapp.util

import android.util.Log
import java.util.regex.Pattern

private const val TAG = "TextUtil"
private const val URL_REGEX_PATTERN =
    "(http|https)://[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&/~+#])?"




    fun String?.toHttpsUrl(): String =
        this?.run { if (matches(Regex("^(http:).*"))) replaceFirst("http", "https") else this } ?: ""


    fun matchUrlFromSharedText(s: String?): String {
        findURLsFromString(s!!, true).joinToString(separator = "\n").run {
            if (isEmpty()) Log.d(TAG, "No URL found in shared text")
            // else makeToast(R.string.share_success_msg)
            return this
        }
    }



    fun matchUrlFromString(s: String, isMatchingMultiLink: Boolean = false): String =
        findURLsFromString(s, !isMatchingMultiLink).joinToString(separator = "\n")

    fun findURLsFromString(input: String, firstMatchOnly: Boolean = false): List<String> {
        val result = mutableListOf<String>()
        val pattern = Pattern.compile(URL_REGEX_PATTERN)

        with(pattern.matcher(input)) {
            if (!firstMatchOnly) {
                while (find()) {
                    result += group()
                }
            } else {
                if (find()) result += (group())
            }
        }
        return result
    }



