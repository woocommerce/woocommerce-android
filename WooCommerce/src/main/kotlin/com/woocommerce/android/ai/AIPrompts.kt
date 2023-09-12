package com.woocommerce.android.ai

import org.wordpress.android.fluxc.utils.DateUtils
import java.math.BigDecimal

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
            if (productDescription.isNotEmpty()) "Use the following product description to improve the " +
                "thank-you note's message, but only if it makes sense: \"$productDescription\"" else ""
        return String.format(
            ORDER_DETAIL_THANK_YOU_NOTE_PROMPT,
            customerName,
            productName,
            descriptionPart,
            languageISOCode
        )
    }

    private const val SALES_PRICE_ADVICE_PROMPT = "Please provide a sales/discount price recommendation " +
        "for a product currently priced at \"%1\$s\". " +
        "%2\$s" +
        "The product's name is \"%3\$s\". \n" +
        "%4\$s\n" +
        "Consider the location of the store which is in the country with the code \"%5\$s\" and the state " +
        "with the code \"%6\$s\". When looking into the state code, ensure that the state actually exists in the " +
        "country. If not, please only use the country location as consideration." +
        "Some rules to follow:\n" +
        "1. Never refer to yourself. Don't say things like `I recommend` or `I advise`. Instead say it " +
        " passively like `a possible recommendation is...` or in variations of that." +
        "2. Your sales advice should be in the ISO language code \"%7\$s\". \n" +
        "3. Never mention the country code or sales code, instead use the country name and state name. \n" +
        "4. Keep in mind that this is pricing for an e-commerce store, not a physical store. \n" +
        "5. Ensure the advice is clear, concise, and takes into account the local market conditions. It should aim " +
        "to maximize sales while maintaining a competitive price. " +
        "6. Most importantly, the advised price must be lower than the current product price." +
        "7. The current date is %8\$s .\n" +
        "Based on that date, recommend the next best times to run a sale, with your reasoning. " +
        "But don't recommend a time or month or season that is already in the past."

    @Suppress("LongParameterList")
    fun generateSalesPriceAdvicePrompt(
        currentPrice: BigDecimal,
        currency: String?,
        productName: String,
        productDescription: String = "",
        countryCode: String,
        stateCode: String,
        languageISOCode: String = "en",
    ): String {
        val descriptionPart = if (productDescription.isNotEmpty()) "The product description is as " +
            "follows: \"$productDescription\". " else ""

        val currencyPart = currency?.let { "The current currency is $it. " } ?: ""
        return String.format(
            SALES_PRICE_ADVICE_PROMPT,
            currentPrice,
            currencyPart,
            productName,
            descriptionPart,
            countryCode,
            stateCode,
            languageISOCode,
            DateUtils.getCurrentDateString()
        )
    }

    private const val LANGUAGE_IDENTIFICATION_PROMPT = "What is the ISO language code of the language used in the " +
        "below text? Do not include any explanations and only provide the ISO language code in your response. \n" +
        "Text: ```(%1\$s)```"

    fun generateLanguageIdentificationPrompt(text: String): String {
        return String.format(LANGUAGE_IDENTIFICATION_PROMPT, text)
    }
}
