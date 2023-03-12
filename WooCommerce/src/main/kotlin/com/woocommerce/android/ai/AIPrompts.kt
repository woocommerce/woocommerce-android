package com.woocommerce.android.ai

object AIPrompts {
    const val GENERATE_PRODUCT_DESCRIPTION_FROM_TITLE =
        "Create a product description for the following product title: "

    const val GENERATE_PROMO_TWEET_FROM_PRODUCT_TITLE =
        "Create a promotional tweet, that should not exceed 280 characters, for the following product: "

    private const val GENERATE_AD_TEXT_BASE =
        "Create an advertisement text to promote "

    fun generateAdvertisementTextPrompt(name: String, description: String = ""): String {
        val prompt = GENERATE_AD_TEXT_BASE + name
        return if (description.isNotEmpty()) {
            "$prompt . Whenever it makes sense, take into account the following " +
                "product description when creating the text: $description"
        } else {
            prompt
        }

    }
}
