package com.woocommerce.android.e2e.helpers.util

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
