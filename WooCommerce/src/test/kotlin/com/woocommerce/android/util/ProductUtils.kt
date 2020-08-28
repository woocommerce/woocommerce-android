package com.woocommerce.android.util

import com.woocommerce.android.ui.products.models.ProductProperty.ComplexProperty
import com.woocommerce.android.ui.products.models.ProductProperty.Editable
import com.woocommerce.android.ui.products.models.ProductProperty.Link
import com.woocommerce.android.ui.products.models.ProductProperty.PropertyGroup
import com.woocommerce.android.ui.products.models.ProductProperty.RatingBar
import com.woocommerce.android.ui.products.models.ProductPropertyCard

class ProductUtils {
    fun stripCallbacks(card: ProductPropertyCard): ProductPropertyCard {
        return card.copy(properties = card.properties.map { p ->
            when (p) {
                is ComplexProperty -> p.copy(onClick = null)
                is Editable -> p.copy(onTextChanged = null)
                is PropertyGroup -> p.copy(onClick = null)
                is Link -> p.copy(onClick = null)
                is RatingBar -> p.copy(onClick = null)
                else -> p
            }
        })
    }
}