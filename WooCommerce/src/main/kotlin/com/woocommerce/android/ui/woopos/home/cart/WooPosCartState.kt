package com.woocommerce.android.ui.woopos.home.cart

import android.os.Parcelable
import androidx.annotation.DrawableRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class WooPosCartState(
    val cartStatus: WooPosCartStatus = WooPosCartStatus.EDITABLE,
    val toolbar: Toolbar = Toolbar(),
    val body: Body = Body.Empty,
    val areItemsRemovable: Boolean = true,
    val isCheckoutButtonVisible: Boolean = true,
) : Parcelable {
    @Parcelize
    sealed class Body : Parcelable {
        abstract val amountOfItems: Int

        @Parcelize
        data object Empty : Body() {
            override val amountOfItems: Int
                get() = 0
        }

        @Parcelize
        data class WithItems(val itemsInCart: List<Item>) : Body() {
            @Parcelize
            data class Item(
                val id: Id,
                val name: String,
                val price: String,
                val imageUrl: String?,
                val isAppearanceAnimationPlayed: Boolean,
            ) : Parcelable {
                @Parcelize
                data class Id(val productId: Long, val itemNumber: Int) : Parcelable
            }

            override val amountOfItems: Int
                get() = itemsInCart.size
        }
    }

    @Parcelize
    data class Toolbar(
        @DrawableRes val icon: Int? = null,
        val itemsCount: String? = null,
        val isClearAllButtonVisible: Boolean = false,
    ) : Parcelable
}

enum class WooPosCartStatus {
    EDITABLE, CHECKOUT, EMPTY,
}
