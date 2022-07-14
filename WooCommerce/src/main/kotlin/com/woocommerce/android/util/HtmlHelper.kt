package com.woocommerce.android.util

import java.util.regex.Pattern

object HtmlHelper {
   private val htmlPattern: Pattern = Pattern
        .compile(".*\\<[^>]+>.*", Pattern.DOTALL)

    fun isHtml(text: String?): Boolean {
        return text?.let {
            htmlPattern.matcher(it).find()
        } ?: false
    }
}
