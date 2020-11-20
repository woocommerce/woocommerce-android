package com.woocommerce.android.model

import com.woocommerce.android.extensions.formatToString

interface IProduct {
    val length: Float
    val width: Float
    val height: Float
    val weight: Float

    /**
     * Formats the [Product] weight with the given [weightUnit]
     * for display purposes.
     * Eg: 12oz
     */
    fun getWeightWithUnits(weightUnit: String?): String {
        return if (weight > 0) {
            val unit = weightUnit ?: EMPTY
            weight.formatToString() + unit
        } else {
            EMPTY
        }
    }

    /**
     * Formats the [Product] size (length, width, height) with the given [dimensionUnit]
     * if all the dimensions are available.
     * Eg: 12 x 15 x 13 in
     */
    fun getSizeWithUnits(dimensionUnit: String?): String {
        val unit = dimensionUnit ?: EMPTY
        val dimensions = arrayOf(length, width, height).filter { it > 0 }
        val size = when (dimensions.size) {
            NO_DIMENSIONS -> EMPTY
            ONE_DIMENSIONAL -> dimensions[0].formatToString() + unit
            TWO_DIMENSIONAL -> dimensions[0].formatToString() +
                X + dimensions[1].formatToString() +
                SPACE + unit
            THREE_DIMENSIONAL -> dimensions[0].formatToString() +
                X + dimensions[1].formatToString() +
                X + dimensions[2].formatToString() +
                SPACE + unit
            else -> UnsupportedOperationException("More than three dimensions!")
        } as String
        return size.trim()
    }

    companion object {
        private const val EMPTY = ""
        private const val SPACE = " "
        private const val X = " x "

        private const val NO_DIMENSIONS = 0
        private const val ONE_DIMENSIONAL = 1
        private const val TWO_DIMENSIONAL = 2
        private const val THREE_DIMENSIONAL = 3
    }
}
