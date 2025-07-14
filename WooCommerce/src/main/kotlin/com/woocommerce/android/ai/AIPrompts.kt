@file:Suppress("MaximumLineLength")

package com.woocommerce.android.ai

object AIPrompts {
    private const val PRODUCT_DESCRIPTION_PROMPT = "Write a description for a product with title \"%1\$s\"%2\$s.\n" +
        "Your response should be in the ISO language code \"%3\$s\". \n" +
        "Make the description 50-60 words or less.\n" +
        "Use a 9th grade reading level.\n" +
        "Perform in-depth keyword research relating to the product in the same language of the product title, " +
        "and use them in your sentences without listing them out."

    fun generateProductDescriptionPrompt(
        name: String,
        features: String = "",
        languageISOCode: String = "en"
    ): String {
        val featuresPart = if (features.isNotEmpty()) " and features: \"$features\"" else ""
        return String.format(PRODUCT_DESCRIPTION_PROMPT, name, featuresPart, languageISOCode)
    }

    private const val PRODUCT_SHARING_PROMPT = "Your task is to help a merchant create a message to share with " +
        "their customers a product named \"%1\$s\". More information about the product:\n" +
        "%2\$s\n" +
        "- Product URL: %3\$s.\n" +
        "Your response should be in the ISO language code \"%4\$s\". \n" +
        "The length should be up to 3 sentences.\n" +
        "Use a 9th grade reading level.\n" +
        "Add related hashtags at the end of the message.\n" +
        "Do not include the URL in the message."

    fun generateProductSharingPrompt(
        name: String,
        url: String,
        description: String = "",
        languageISOCode: String = "en"
    ): String {
        val descriptionPart = if (description.isNotEmpty()) "- Product description: \"$description\"" else ""
        return String.format(PRODUCT_SHARING_PROMPT, name, descriptionPart, url, languageISOCode)
    }

    @Suppress("LongParameterList")
    fun generateProductCreationPrompt(
        keywords: String,
        tone: String,
        weightUnit: String,
        dimensionUnit: String,
        currency: String,
        existingCategories: List<String>,
        existingTags: List<String>,
        languageISOCode: String
    ): String {
        fun getTagsLine(): String {
            return if (existingTags.isNotEmpty()) {
                """
                    "tags":"Given the list of available tags "${existingTags.joinToString()}", suggest an array of the best matching tags for this product. You can suggest new tags as well."
                """.trimIndent()
            } else {
                """
                    "tags":"suggest an array of the best matching tags for this product."
                """.trimIndent()
            }
        }

        fun getCategoriesLine(): String {
            return if (existingCategories.isNotEmpty()) {
                """
                    "categories":"Given the list of available categories "${existingCategories.joinToString()}", suggest an array of the best matching categories for this product. You can suggest new categories as well."
                """.trimIndent()
            } else {
                """
                    "categories":"suggest an array of the best matching categories for this product."
                """.trimIndent()
            }
        }

        return """
            You are a WooCommerce SEO and marketing expert, perform in-depth research about the product using the provided name, keywords and tone, and give your response in the below JSON format

            keywords: "$keywords"
            tone: "$tone"

            Your response should be in JSON format and don't send anything extra. Don't include the word JSON in your response:
            "{
               "names":"An array of strings, containing three different names of the product, written in the language with ISO code "$languageISOCode"",
               "descriptions": "An array of strings, each containing three different product descriptions of around 100 words long each in a "$tone" tone, written in the language with ISO code "$languageISOCode"",
               "short_descriptions": "An array of strings, each containing three different short descriptions of the product in a "$tone" tone, written in the language with ISO code "$languageISOCode",
               "virtual":"A boolean value that shows whether the product is virtual or physical",
               "shipping":{
                  "length":"Guess and provide only the number in $dimensionUnit",
                  "weight":"Guess and provide only the number in $weightUnit",
                  "width":"Guess and provide only the number in $dimensionUnit",
                  "height":"Guess and provide only the number in $dimensionUnit"
               },
               "price":"Guess the price in $currency, do not include the currency symbol, only provide the price as a number",
               ${getTagsLine()},
               ${getCategoriesLine()}
            }"
        """.trimIndent()
    }

    // TODO remove this when cleaning up legacy code for product creation
    @Suppress("LongParameterList")
    fun generateProductCreationPromptLegacy(
        name: String,
        keywords: String,
        tone: String,
        weightUnit: String,
        dimensionUnit: String,
        currency: String,
        existingCategories: List<String>,
        existingTags: List<String>,
        languageISOCode: String
    ): String {
        fun getTagsLine(): String {
            return if (existingTags.isNotEmpty()) {
                """
                    "tags":"Given the list of available tags "${existingTags.joinToString()}", suggest an array of the best matching tags for this product. You can suggest new tags as well."
                """.trimIndent()
            } else {
                """
                    "tags":"suggest an array of the best matching tags for this product."
                """.trimIndent()
            }
        }

        fun getCategoriesLine(): String {
            return if (existingCategories.isNotEmpty()) {
                """
                    "categories":"Given the list of available categories "${existingCategories.joinToString()}", suggest an array of the best matching categories for this product. You can suggest new categories as well."
                """.trimIndent()
            } else {
                """
                    "categories":"suggest an array of the best matching categories for this product."
                """.trimIndent()
            }
        }

        return """
            You are a WooCommerce SEO and marketing expert, perform in-depth research about the product using the provided name, keywords and tone, and give your response in the below JSON format

            name: "$name"
            keywords: "$keywords"
            tone: "$tone"

            Your response should be in JSON format and don't send anything extra. Don't include the word JSON in your response:
            "{
               "name":"The name of the product, in the ISO language code "$languageISOCode"",
               "description":"Product description of around 100 words long in a "$tone" tone, in the ISO language code "$languageISOCode"",
               "short_description":"Product's short description, in the ISO language code "$languageISOCode"",
               "virtual":"A boolean value that shows whether the product is virtual or physical",
               "shipping":{
                  "length":"Guess and provide only the number in $dimensionUnit",
                  "weight":"Guess and provide only the number in $weightUnit",
                  "width":"Guess and provide only the number in $dimensionUnit",
                  "height":"Guess and provide only the number in $dimensionUnit"
               },
               "price":"Guess the price in $currency, do not include the currency symbol, only provide the price as a number",
               ${getTagsLine()},
               ${getCategoriesLine()}
            }"
        """.trimIndent()
    }

    private const val ORDER_DETAIL_THANK_YOU_NOTE_PROMPT = "Write a 2 paragraphs thank-you note for a customer " +
        "whose name is \"%1\$s\", who has just purchased a product named \"%2\$s\". \n" +
        "%3\$s\n" +
        "Your response should be in the ISO language code \"%4\$s\". \n" +
        "Make sure the note sounds genuine and explains the appreciation well." +
        "Use a 9th grade reading level.\n"

    fun generateThankYouNotePrompt(
        customerName: String,
        productName: String,
        productDescription: String = "",
        languageISOCode: String = "en"
    ): String {
        val descriptionPart =
            if (productDescription.isNotEmpty()) {
                "Use the following product description to improve the " +
                    "thank-you note's message, but only if it makes sense: \"$productDescription\""
            } else {
                ""
            }
        return String.format(
            ORDER_DETAIL_THANK_YOU_NOTE_PROMPT,
            customerName,
            productName,
            descriptionPart,
            languageISOCode
        )
    }

    private const val LANGUAGE_IDENTIFICATION_PROMPT = "What is the ISO language code of the language used in the " +
        "below text? Do not include any explanations and only provide the ISO language code in your response. \n" +
        "Text: ```(%1\$s)```"

    fun generateLanguageIdentificationPrompt(text: String): String {
        return String.format(LANGUAGE_IDENTIFICATION_PROMPT, text)
    }
}
