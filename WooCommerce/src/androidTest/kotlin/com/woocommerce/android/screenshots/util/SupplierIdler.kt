package com.woocommerce.android.screenshots.util

import java.util.function.Supplier

class SupplierIdler : TestIdler {
    private var supplier: Supplier<Boolean>

    constructor(supplier: Supplier<Boolean>) {
        this.supplier = supplier
    }

    override fun checkCondition(): Boolean {
        return supplier.get()
    }
}
