package com.woocommerce.android.e2e.helpers.util

import org.json.JSONArray
import org.json.JSONObject

// Found at https://www.baeldung.com/kotlin/iterate-over-jsonarray
operator fun JSONArray.iterator(): Iterator<JSONObject> =
    (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()
