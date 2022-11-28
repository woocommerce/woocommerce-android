package com.woocommerce.android.ui.login.storecreation

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewStore @Inject constructor() {
    var data = NewStoreData()
        private set

    fun update(
        name: String? = null,
        domain: String? = null,
        category: String? = null,
        country: String? = null,
        siteId: Long? = null
    ) {
        data = data.copy(
            name = name ?: data.name,
            domain = domain ?: data.domain,
            category = category ?: data.category,
            country = country ?: data.country,
            siteId = siteId ?: data.siteId
        )
    }

    data class NewStoreData(
        val name: String? = null,
        val domain: String? = null,
        val category: String? = null,
        val country: String? = null,
        val siteId: Long? = null
    )
}
