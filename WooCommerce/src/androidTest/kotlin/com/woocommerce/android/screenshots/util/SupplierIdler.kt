package com.woocommerce.android.screenshots.util

import java.util.function.Supplier

class SupplierIdler(private var supplier: Supplier<Boolean>) : TestIdler() {
    override fun checkCondition(): Boolean {
        return supplier.get()
    }
}
