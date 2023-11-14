package com.woocommerce.android.ui.login.storecreation.countrypicker

import android.os.Parcelable
import com.woocommerce.android.ui.login.storecreation.NewStore
import kotlinx.parcelize.Parcelize

@Parcelize
data class StoreCreationCountry(
    val name: String,
    val code: String,
    val emojiFlag: String,
    val isSelected: Boolean = false
) : Parcelable {
    fun toNewStoreCountry() =
        NewStore.Country(
            name = name,
            code = code,
        )
}
