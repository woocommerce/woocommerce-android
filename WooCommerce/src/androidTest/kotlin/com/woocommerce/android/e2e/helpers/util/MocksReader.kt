package com.woocommerce.android.e2e.helpers.util

import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject

class MocksReader {
    private fun readAssetsFile(fileName: String): String {
        val appContext = InstrumentationRegistry.getInstrumentation().context
        return appContext.assets.open(fileName).bufferedReader().use { it.readText() }
    }

    private fun readFileToArray(fileName: String): JSONArray {
        val fileWireMockString = this.readAssetsFile(fileName)
        val fileWireMockJSON = JSONObject(fileWireMockString)
        return fileWireMockJSON
            .getJSONObject("response")
            .getJSONObject("jsonBody")
            .getJSONArray("data")
    }

    fun readAllReviewsToArray(): JSONArray {
        return readFileToArray("mocks/mappings/jetpack-blogs/wc/reviews/products_reviews_all.json")
    }

    fun readAllProductsToArray(): JSONArray {
        return readFileToArray("mocks/mappings/jetpack-blogs/wc/products/products.json")
    }

    fun readOrderToArray(): JSONArray {
        return readFileToArray("mocks/mappings/jetpack-blogs/wc/orders/2201/2201_detailed.json")
    }
}
