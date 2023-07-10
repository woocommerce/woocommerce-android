package com.woocommerce.android.ai

object AIPrompts {
    private const val PRODUCT_DESCRIPTION_PROMPT = "Write a description for a product with title \"%1\$s\"%2\$s.\n" +
        "Identify the language used in the product title and use the same language in your response.\n" +
        "Make the description 50-60 words or less.\n" +
        "Use a 9th grade reading level.\n" +
        "Perform in-depth keyword research relating to the product in the same language of the product title, " +
        "and use them in your sentences without listing them out."

    fun generateProductDescriptionPrompt(name: String, features: String = ""): String {
        val featuresPart = if (features.isNotEmpty()) " and features: \"$features\"" else ""
        return String.format(PRODUCT_DESCRIPTION_PROMPT, name, featuresPart)
    }

    private const val PRODUCT_SHARING_PROMPT = "Your task is to help a merchant create a message to share with " +
        "their customers a product named \"%1\$s\". More information about the product:\n" +
        "%2\$s\n" +
        "- Product URL: %3\$s.\n" +
        "Identify the language used in the product name and product description, if any, to use in your response.\n" +
        "The length should be up to 3 sentences.\n" +
        "Use a 9th grade reading level.\n" +
        "Add related hashtags at the end of the message.\n" +
        "Do not include the URL in the message."

    fun generateProductSharingPrompt(name: String, url: String, description: String = ""): String {
        val descriptionPart = if (description.isNotEmpty()) "- Product description: \"$description\"" else ""
        return String.format(PRODUCT_SHARING_PROMPT, name, descriptionPart, url)
    }

    private const val LANGUAGE_IDENTIFICATION_PROMPT = "What is the ISO language code of the language used in the " +
        "below text? Do not include any explanations and only provide the ISO language code in your response. \n" +
        "Text: ```(%1\$s)```"

    fun generateLanguageIdentificationPrompt(text: String): String {
        return String.format(LANGUAGE_IDENTIFICATION_PROMPT, text)
    }

}
