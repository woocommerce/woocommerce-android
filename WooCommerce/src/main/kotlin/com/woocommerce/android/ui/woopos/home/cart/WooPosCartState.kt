package com.woocommerce.android.ui.woopos.home.cart

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class WooPosCartState(
    val cartStatus: WooPosCartStatus = WooPosCartStatus.EDITABLE,
    val toolbar: Toolbar = Toolbar(),
    val body: Body = Body.Empty,
    val areItemsRemovable: Boolean = true,
    val checkoutButtonState: CheckoutButtonState = CheckoutButtonState.Enabled,
) : Parcelable {
    @Parcelize
    sealed class Body : Parcelable {
        abstract val amountOfItems: Int

        @Parcelize
        data object Empty : Body(), Parcelable {
            override val amountOfItems: Int
                get() = 0
        }

        @Parcelize
        data class WithItems(val itemsInCart: List<WooPosCartItemViewState>) : Body(), Parcelable {
            override val amountOfItems: Int
                get() = itemsInCart.size
        }
    }

    @Parcelize
    data class Toolbar(
        val backIconVisible: Boolean = false,
        val itemsCount: String? = null,
        val isClearAllButtonVisible: Boolean = false,
    ) : Parcelable

    enum class CheckoutButtonState {
        Enabled,
        Disabled,
        Invisible
    }
}

enum class WooPosCartStatus {
    EDITABLE, CHECKOUT, EMPTY,
}

@Parcelize
sealed class WooPosCartItemViewState(open val itemNumber: Int, open val name: String) : Parcelable {
    @Parcelize
    @Suppress("LongParameterList")
    sealed class Product(
        override val itemNumber: Int,
        open val id: Long,
        override val name: String,
        open val price: String,
        open val description: String?,
        open val imageUrl: String?,
        open val productDoesNotExist: Boolean,
    ) : WooPosCartItemViewState(itemNumber, name) {
        @Parcelize
        data class Simple(
            override val itemNumber: Int,
            override val id: Long,
            override val name: String,
            override val price: String,
            override val description: String?,
            override val imageUrl: String?,
            override val productDoesNotExist: Boolean = false,
        ) : Product(itemNumber, id, name, price, description, imageUrl, productDoesNotExist)

        @Parcelize
        data class Variation(
            override val itemNumber: Int,
            override val id: Long,
            val variationId: Long,
            override val name: String,
            override val price: String,
            override val description: String?,
            override val imageUrl: String?,
            override val productDoesNotExist: Boolean = false,
        ) : Product(itemNumber, id, name, price, description, imageUrl, productDoesNotExist)
    }

    @Parcelize
    data class Coupon(
        override val itemNumber: Int,
        override val name: String,
        val summary: String,
        val id: Long,
        val validationState: CouponValidationState = CouponValidationState.Unknown,
    ) : WooPosCartItemViewState(itemNumber, name) {
        @Parcelize
        sealed class CouponValidationState : Parcelable {
            @Parcelize
            data class Valid(val formattedDiscount: String) : CouponValidationState()

            @Parcelize
            data object Invalid : CouponValidationState()

            @Parcelize
            data object Unknown : CouponValidationState()
        }
    }

    @Parcelize
    data class CustomAmount(
        override val itemNumber: Int,
        override val name: String,
        val customAmountId: Long,
        val amount: BigDecimal,
        val formattedAmount: String,
        val isTaxable: Boolean,
    ) : WooPosCartItemViewState(itemNumber, name)

    @Parcelize
    data class Loading(
        override val itemNumber: Int,
        override val name: String,
    ) : WooPosCartItemViewState(itemNumber, name)

    @Parcelize
    data class Error(
        override val itemNumber: Int,
        override val name: String,
        val message: String,
    ) : WooPosCartItemViewState(itemNumber, name)
}
