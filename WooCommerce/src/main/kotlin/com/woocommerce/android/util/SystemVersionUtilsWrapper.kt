package com.woocommerce.android.util

import javax.inject.Inject

class SystemVersionUtilsWrapper @Inject constructor() {
    fun isAtLeastT() = SystemVersionUtils.isAtLeastT()
    fun isAtMostT() = SystemVersionUtils.isAtMostT()

    fun isAtLeastS() = SystemVersionUtils.isAtLeastS()
    fun isAtMostS() = SystemVersionUtils.isAtMostS()

    fun isAtLeastR() = SystemVersionUtils.isAtLeastR()
    fun isAtMostR() = SystemVersionUtils.isAtMostR()

    fun isAtLeastQ() = SystemVersionUtils.isAtLeastQ()
    fun isAtMostQ() = SystemVersionUtils.isAtMostQ()

    fun isAtLeastP() = SystemVersionUtils.isAtLeastP()
    fun isAtMostP() = SystemVersionUtils.isAtMostP()
}
