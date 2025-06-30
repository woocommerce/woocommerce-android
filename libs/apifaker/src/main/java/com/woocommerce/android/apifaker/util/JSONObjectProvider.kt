package com.woocommerce.android.apifaker.util

import org.json.JSONObject
import javax.inject.Inject

internal class JSONObjectProvider @Inject constructor() {
    fun parseString(content: String): JSONObject = JSONObject(content)
}
