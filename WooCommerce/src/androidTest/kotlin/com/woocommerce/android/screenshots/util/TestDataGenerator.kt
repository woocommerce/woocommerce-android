package com.woocommerce.android.screenshots.util

import org.apache.commons.lang3.RandomStringUtils
import java.time.LocalDateTime

import java.util.Random

object TestDataGenerator {
    fun getHumanStyleDate(): String {
        return "Posted this note on " + LocalDateTime.now().dayOfWeek + ", " + LocalDateTime.now().month + " " + LocalDateTime.now().dayOfMonth
    }

    fun getAllProductsSearchRequest(): String {
        return "123" // Stub to retrieve all products
    }

    // HELPERS
    // Random String generators
    private fun generateAscii(min: Int, max: Int): String {
        return RandomStringUtils.randomAscii(min, max).toString()
    }

    private fun generateAlphaNumeric(min: Int, max: Int): String {
        return RandomStringUtils.randomAlphanumeric(min, max).toString()
    }

    private fun generateNumeric(min: Int, max: Int): String {
        return RandomStringUtils.randomNumeric(min, max).toString()
    }

    private fun generateNegativeNumeric(): String {
        val rand = Random().nextDouble() - 3
        return rand.toString()
    }

    private fun generateAlphabetic(min: Int, max: Int): String {
        return RandomStringUtils.randomAlphabetic(min, max).toString()
    }

    // Random Integer generators
    fun getRandomInteger(startRangeAt: Int, endRangeAt: Int): Int {
        return (startRangeAt..endRangeAt).shuffled().first()
    }
}
