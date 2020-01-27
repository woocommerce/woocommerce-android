package com.woocommerce.android

/**
 * Global intent identifiers
 */
object RequestCodes {
    private const val BASE_REQUEST_CODE = 100

    const val ADD_ACCOUNT = BASE_REQUEST_CODE + 0
    const val SETTINGS = BASE_REQUEST_CODE + 1
    const val SITE_PICKER = BASE_REQUEST_CODE + 2
    const val IN_APP_UPDATE = BASE_REQUEST_CODE + 3

    const val STORAGE_PERMISSION = BASE_REQUEST_CODE + 10
    const val CAMERA_PERMISSION = BASE_REQUEST_CODE + 11

    const val PRODUCT_IMAGE_VIEWER = BASE_REQUEST_CODE + 100
    const val CHOOSE_PHOTO = BASE_REQUEST_CODE + 101
    const val CAPTURE_PHOTO = BASE_REQUEST_CODE + 102

    const val ORDER_REFUND = BASE_REQUEST_CODE + 200

    const val PRODUCT_INVENTORY = BASE_REQUEST_CODE + 300
    const val PRODUCT_INVENTORY_BACKORDERS = BASE_REQUEST_CODE + 301
    const val PRODUCT_INVENTORY_STOCK_STATUS = BASE_REQUEST_CODE + 302
}
