package com.woocommerce.android.extensions

/**
 * If the provided [line] is not null and not empty, then add the string to this instance. Will prepend the
 * [separator] if the current contents of this StringBuilder are not empty.
 */
fun StringBuilder.appendWithIfNotEmpty(line: String?, separator: String = ", "): StringBuilder {
    return line?.takeIf { it.isNotEmpty() }?.let {
        if (isNotEmpty()) {
            append(separator)
        }
        append(it)
    } ?: this
}
