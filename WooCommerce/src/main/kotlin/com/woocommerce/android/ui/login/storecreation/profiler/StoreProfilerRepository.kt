package com.woocommerce.android.ui.login.storecreation.profiler

import com.google.gson.Gson
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StoreProfilerRepository @Inject constructor(
    private val gson: Gson,
    private val coroutineDispatchers: CoroutineDispatchers
) {
    suspend fun fetchProfilerOptions(): ProfilerOptions = withContext(coroutineDispatchers.io) {
        gson.fromJson(PROFILER_OPTIONS_JSON, ProfilerOptions::class.java)
    }
}

private const val PROFILER_OPTIONS_JSON = """{
  "aboutMerchant": [
    {
      "id": 1,
      "value": "I'm just starting my business",
      "heading": "I'm just starting my business",
      "description": "",
      "tracks": "im_just_starting_my_business"
    },
    {
      "id": 2,
      "value": "I'm already selling, but not online",
      "heading": "I'm already selling, but not online",
      "description": "",
      "tracks": "im_already_selling_but_not_online"
    },
    {
      "id": 3,
      "value": "I'm already selling online",
      "heading": "I'm already selling online",
      "description": "",
      "tracks": "im_already_selling_online",
      "platforms": [
        { "value": "adobe-commerce", "label": "Adobe Commerce" },
        { "value": "amazon", "label": "Amazon" },
        { "value": "big-cartel", "label": "Big Cartel" },
        { "value": "big-commerce", "label": "Big Commerce" },
        { "value": "ebay", "label": "Ebay" },
        { "value": "ecwid", "label": "Ecwid" },
        { "value": "etsy", "label": "Etsy" },
        { "value": "facebook-marketplace", "label": "Facebook Marketplace" },
        { "value": "google-shopping", "label": "Google Shopping" },
        { "value": "magento", "label": "Magento" },
        { "value": "pinterest", "label": "Pinterest" },
        { "value": "shopify", "label": "Shopify" },
        { "value": "square", "label": "Square" },
        { "value": "squarespace", "label": "Squarespace" },
        { "value": "walmart", "label": "Walmart" },
        { "value": "wish", "label": "Wish" },
        { "value": "wix", "label": "Wix" },
        { "value": "wordPress", "label": "WordPress" }
      ]
    }
  ],
  "industries": [
    {
      "id": 0,
      "label": "Clothing and accessories",
      "key": "clothing_accessories"
    },
    {
      "id": 1,
      "label": "Health and beauty",
      "key": "health_beauty"
    },
    {
      "id": 2,
      "label": "Food and drink",
      "key": "food_drink"
    },
    {
      "id": 3,
      "label": "Home, furniture, and garden",
      "key": "home_furniture_garden"
    },
    {
      "id": 4,
      "label": "Education and learning",
      "key": "education_learning"
    },
    {
      "id": 5,
      "label": "Electronics and computers",
      "key": "electronic_computers"
    },
    {
      "id": 6,
      "label": "Other",
      "key": "other"
    }
  ]
}"""
