package com.woocommerce.android

import android.os.Bundle
import androidx.navigation.NavArgs

fun NavArgs.toMap(): Map<String, Any?> {
    val toBundleMethod = javaClass.getDeclaredMethod("toBundle")
    val bundle = toBundleMethod.invoke(this) as Bundle
    return bundle.keySet().map { it to bundle[it] }.toMap()
}
