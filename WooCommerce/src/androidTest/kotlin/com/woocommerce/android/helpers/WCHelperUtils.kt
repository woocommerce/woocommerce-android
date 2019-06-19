package com.woocommerce.android.helpers

import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE

/**
 * Utils class for helper methods
 */
object WCHelperUtils {
    /**
     * Method to get the Clipboard text
     */
    fun getClipboardText(context: Context): String {
        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
        val item = clipboard?.primaryClip?.getItemAt(0)
        return item?.text.toString()
    }
}
