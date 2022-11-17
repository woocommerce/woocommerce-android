package com.woocommerce.android.ui.login.storecreation

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewStore @Inject constructor() {

    val store: MutableStateFlow<NewStoreData> = MutableStateFlow(NewStoreData())

    data class NewStoreData(
        val name: String? = null,
        val domain: String? = null,
        val category: String? = null,
        val country: String? = null
    )
}
