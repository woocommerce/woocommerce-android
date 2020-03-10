package com.woocommerce.android

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injectable wrapper around AppPrefs.
 *
 * AppPrefs interface is consisted of static methods, which make the client code difficult to test/mock.
 * Main purpose of this wrapper is to make testing easier.
 *
 */
@Singleton
class AppPrefsWrapper @Inject constructor() {
    var isUsingV4Api: Boolean
        get() = AppPrefs.isUsingV4Api()
        set(enabled) = AppPrefs.setIsUsingV4Api(enabled)
}
