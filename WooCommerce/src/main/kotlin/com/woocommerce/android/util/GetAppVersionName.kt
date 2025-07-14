package com.woocommerce.android.util

import android.app.Application
import javax.inject.Inject

class GetAppVersionName @Inject constructor(private val app: Application) {
    operator fun invoke(): String {
        return PackageUtils.getVersionName(app)
    }
}
