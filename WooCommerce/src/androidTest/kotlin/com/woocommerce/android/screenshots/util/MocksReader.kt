package com.woocommerce.android.screenshots.util

import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject

class MocksReader {
    fun readAllReviewsToArray(): JSONArray {
        val reviewsWireMockFileName = "mocks/mappings/jetpack-blogs/wc/reviews/products_reviews_all.json"
        val reviewsWireMockString = this.readAssetsFile(reviewsWireMockFileName)
        val reviewsWireMockJSON = JSONObject(reviewsWireMockString)
        return reviewsWireMockJSON
            .getJSONObject("response")
            .getJSONObject("jsonBody")
            .getJSONArray("data")
    }

    fun readAllProductsToArray(): JSONArray {
        val productsWireMockString = this.readAssetsFile("mocks/mappings/jetpack-blogs/wc/products/products.json")
        val productsWireMockJSON = JSONObject(productsWireMockString)
        return productsWireMockJSON
            .getJSONObject("response")
            .getJSONObject("jsonBody")
            .getJSONArray("data")
    }

    private fun readAssetsFile(fileName: String): String {
        val appContext = InstrumentationRegistry.getInstrumentation().context
        return appContext.assets.open(fileName).bufferedReader().use { it.readText() }
    }
}
