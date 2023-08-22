package com.woocommerce.android.ui.login.storecreation.countrypicker

import com.woocommerce.android.ui.login.storecreation.NewStore

data class StoreCreationCountry(
    val name: String,
    val code: String,
    val emojiFlag: String,
    val isSelected: Boolean = false
) {
    fun toNewStoreCountry() =
        NewStore.Country(
            name = name,
            code = code,
        )
}
