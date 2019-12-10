package com.woocommerce.android

/**
 * Global intent identifiers
 */
object RequestCodes {
    private const val BASE_REQUEST_CODE = 100

    const val ADD_ACCOUNT = BASE_REQUEST_CODE + 0
    const val SETTINGS = BASE_REQUEST_CODE + 1
    const val SITE_PICKER = BASE_REQUEST_CODE + 2

    const val PRODUCT_IMAGE_VIEWER = BASE_REQUEST_CODE + 10
    const val CHOOSE_PHOTO = BASE_REQUEST_CODE + 11
    const val CAPTURE_PHOTO = BASE_REQUEST_CODE + 12
}
