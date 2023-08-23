package com.woocommerce.android.ui.orders.creation.bundle

class OrderItemRules private constructor(
    val quantityMin: Long?,
    val quantityMax: Long?,
    val childrenRules: List<OrderChildItemRules>?
) {
    fun needsConfiguration(): Boolean {
        if (quantityMin != quantityMax) return true
        return childrenRules?.any { childrenRules ->
            childrenRules.isOptional || childrenRules.quantityMin != childrenRules.quantityMax
        } ?: false
    }

    class Builder {
        private var quantityMin: Long? = null
        private var quantityMax: Long? = null
        private val childrenRules = mutableMapOf<Long, OrderChildItemRules>()

        fun setMinMax(min: Long?, max: Long?) {
            quantityMin = min
            quantityMax = max
        }

        fun setChildItemRule(
            itemId: Long,
            productId: Long,
            quantityMin: Long?,
            quantityMax: Long?,
            quantityDefault: Long = 0,
            optional: Boolean = false
        ) {
            childrenRules[itemId] =
                OrderChildItemRules(itemId, productId, quantityMin, quantityMax, quantityDefault, optional)
        }

        fun build(): OrderItemRules {
            val itemChildrenRules = childrenRules.values.toList()
            return OrderItemRules(
                quantityMin = quantityMin,
                quantityMax = quantityMax,
                childrenRules = itemChildrenRules
            )
        }
    }
}

class OrderChildItemRules(
    val itemId: Long,
    val productId: Long,
    val quantityMin: Long?,
    val quantityMax: Long?,
    val quantityDefault: Long = 0,
    val isOptional: Boolean = false
)
