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
        val hasLength = length > 0
        val hasWidth = width > 0
        val hasHeight = height > 0
        val unit = dimensionUnit ?: EMPTY
        val size = if (hasLength && hasWidth && hasHeight) {
            length.formatToString() +
                X + width.formatToString() +
                X + height.formatToString() +
                SPACE + unit
        } else if (hasLength && hasWidth) {
            length.formatToString() +
                X + width.formatToString() +
                SPACE + unit
        } else if (hasLength && hasHeight) {
            length.formatToString() +
                X + height.formatToString() +
                SPACE + unit
        } else if (hasWidth && hasHeight) {
            width.formatToString() +
                X + height.formatToString() +
                SPACE + unit
        } else if (hasLength) {
            length.formatToString() + unit
        } else if (hasWidth) {
            width.formatToString() + unit
        } else if (hasHeight) {
            height.formatToString() + unit
        } else {
            EMPTY
        }
        return size.trim()
    }

    companion object {
        private const val EMPTY = ""
        private const val SPACE = " "
        private const val X = " x "
    }
}
