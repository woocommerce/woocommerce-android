package com.woocommerce.android.ui.coupons.edit

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.COUPON_CREATION_INITIATED
import com.woocommerce.android.analytics.AnalyticsEvent.COUPON_CREATION_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.COUPON_UPDATE_INITIATED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_COUPON_DISCOUNT_TYPE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_DESCRIPTION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_EXPIRY_DATE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_PRODUCT_OR_CATEGORY_RESTRICTIONS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_USAGE_RESTRICTIONS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_INCLUDES_FREE_SHIPPING
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_COUPON_DISCOUNT_TYPE_CUSTOM
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_COUPON_DISCOUNT_TYPE_FIXED_CART
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_COUPON_DISCOUNT_TYPE_FIXED_PRODUCT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_COUPON_DISCOUNT_TYPE_PERCENTAGE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.model.Coupon.CouponRestrictions
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.coupons.CouponRepository
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.EditIncludedProductCategories
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.EditIncludedProducts
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.OpenCouponRestrictions
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.OpenDescriptionEditor
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductRestriction
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.SelectedItem
import com.woocommerce.android.util.CouponUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUiStringSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class EditCouponViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val couponRepository: CouponRepository,
    private val couponUtils: CouponUtils,
    private val parameterRepository: ParameterRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val resourceProvider: ResourceProvider,
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val PARAMETERS_KEY = "parameters_key"
    }

    private val navArgs: EditCouponFragmentArgs by savedStateHandle.navArgs()
    private val mode: StateFlow<Mode> = savedStateHandle.getStateFlow(this, navArgs.mode, "key_mode")

    private val storedCoupon: Deferred<Coupon> = async {
        with(mode.value) {
            when (this) {
                is Mode.Edit -> couponRepository.observeCoupon(couponId).first()
                is Mode.Create -> Coupon.EMPTY.copy(type = type)
            }
        }
    }

    private val couponDraft = savedStateHandle.getNullableStateFlow(viewModelScope, null, Coupon::class.java)
    private val currencyCode
        get() = parameterRepository.getParameters(PARAMETERS_KEY, savedState).currencySymbol.orEmpty()

    private val isSaving = MutableStateFlow(false)

    val viewState = combine(
        flow = couponDraft
            .filterNotNull(),
        flow2 = isSaving
    ) { coupon, isSaving ->
        ViewState(
            couponDraft = coupon,
            screenTitle = getScreenTitle(coupon),
            amountUnit = if (coupon.type == Coupon.Type.Percent) "%" else currencyCode,
            hasChanges = !coupon.isSameCoupon(storedCoupon.await()),
            isSaving = isSaving,
            saveButtonText = getSaveButtonText()
        )
    }
        .asLiveData()

    private fun getScreenTitle(coupon: Coupon): String {
        val localizedType = coupon.type?.let { couponUtils.localizeType(it) }
        return when (mode.value) {
            is Mode.Edit -> getEditModeScreenTitle(localizedType)
            is Mode.Create -> getCreateModeScreenTitle(localizedType)
        }
    }

    private fun getCreateModeScreenTitle(localizedType: String?) =
        localizedType ?: resourceProvider.getString(R.string.coupon_create_screen_title_default)

    private fun getEditModeScreenTitle(localizedType: String?) =
        localizedType ?: resourceProvider.getString(R.string.coupon_edit_screen_title_default)

    init {
        if (couponDraft.value == null) {
            launch {
                couponDraft.value = storedCoupon.await()
            }
        }
    }

    fun onAmountChanged(value: BigDecimal?) {
        couponDraft.update {
            it?.copy(amount = value)
        }
    }

    fun onCouponCodeChanged(code: String) {
        couponDraft.update {
            it?.copy(code = code)
        }
    }

    fun onRegenerateCodeClick() {
        val newCode = couponUtils.generateRandomCode()
        couponDraft.update {
            it?.copy(code = newCode)
        }
    }

    fun onDescriptionButtonClick() {
        triggerEvent(OpenDescriptionEditor(couponDraft.value?.description))
    }

    fun onDescriptionChanged(description: String) {
        couponDraft.update {
            it?.copy(description = description)
        }
    }

    fun onExpiryDateChanged(expiryDate: Date?) {
        couponDraft.update {
            it?.copy(dateExpires = expiryDate)
        }
    }

    fun onFreeShippingChanged(value: Boolean) {
        couponDraft.update {
            it?.copy(isShippingFree = value)
        }
    }

    fun onUsageRestrictionsClick() {
        couponDraft.value?.let {
            // If a Coupon has no set minimum or maximum spend restriction, the REST API returns their values
            // as BigDecimal `0.00`. Yet, in wp-admin, a Coupon with no minimum or maximum spend restriction has the
            // "No minimum" or "No maximum" placeholders displayed, instead of `0.00`.
            // To replicate that behavior on the app's Coupon Restriction Screen, here we set `0.00` values as null.
            val minimumAmount =
                if (it.restrictions.minimumAmount isEqualTo BigDecimal.ZERO) null else it.restrictions.minimumAmount
            val maximumAmount =
                if (it.restrictions.maximumAmount isEqualTo BigDecimal.ZERO) null else it.restrictions.maximumAmount
            triggerEvent(
                OpenCouponRestrictions(
                    restrictions = it.restrictions.copy(
                        minimumAmount = minimumAmount,
                        maximumAmount = maximumAmount
                    ),
                    currencyCode = currencyCode,
                    showLimitUsageToXItems = it.type != Coupon.Type.FixedCart
                )
            )
        }
    }

    fun onSelectProductsButtonClick() {
        couponDraft.value?.let {
            it.productIds.map { productOrVariationId ->
                SelectedItem.ProductOrVariation(productOrVariationId)
            }.let { selectedItems ->
                triggerEvent(
                    EditIncludedProducts(
                        selectedItems,
                        listOf(
                            ProductRestriction.NonPublishedProducts,
                            ProductRestriction.VariableProductsWithNoVariations
                        )
                    )
                )
            }
        }
    }

    fun onSelectedProductsUpdated(productItems: Collection<SelectedItem>) {
        couponDraft.update {
            it?.copy(productIds = productItems.map { it.id })
        }
    }

    fun onSelectCategoriesButtonClick() {
        couponDraft.value?.let {
            triggerEvent(EditIncludedProductCategories(it.categoryIds))
        }
    }

    fun onIncludedCategoriesChanged(includedCategoryIds: Set<Long>) {
        couponDraft.update {
            it?.copy(categoryIds = includedCategoryIds.toList())
        }
    }

    fun onRestrictionsUpdated(restrictions: CouponRestrictions) {
        couponDraft.update {
            it?.copy(restrictions = restrictions)
        }
    }

    fun onSaveClick() = launch {
        isSaving.value = true

        val oldCoupon = storedCoupon.await()
        val newCoupon = couponDraft.value!!
        when (mode.value) {
            is Mode.Edit -> trackUpdateChanges(oldCoupon, newCoupon)
            is Mode.Create -> trackCreateCoupon(newCoupon)
        }

        when (mode.value) {
            is Mode.Edit -> updateCoupon(newCoupon)
            is Mode.Create -> addCoupon(newCoupon)
        }

        isSaving.value = false
    }

    private fun getSaveButtonText(): Int = when (mode.value) {
        is Mode.Edit -> R.string.coupon_edit_save_button
        is Mode.Create -> R.string.coupon_create_save_button
    }

    private suspend fun addCoupon(newCoupon: Coupon) {
        couponRepository.createCoupon(newCoupon)
            .onSuccess {
                triggerEvent(ShowSnackbar(R.string.coupon_create_coupon_created))
                triggerEvent(Exit)
                analyticsTrackerWrapper.track(COUPON_CREATION_SUCCESS)
            }
            .onFailure { exception ->
                WooLog.e(
                    tag = WooLog.T.COUPONS,
                    message = "Coupon create failed: ${exception.message}"
                )
                val wooErrorType = (exception as? WooException)?.error?.type
                analyticsTrackerWrapper.track(
                    stat = AnalyticsEvent.COUPON_CREATION_FAILED,
                    errorContext = this@EditCouponViewModel.javaClass.simpleName,
                    errorType = wooErrorType?.name,
                    errorDescription = exception.message
                )
                val message =
                    exception.takeIf { wooErrorType == WooErrorType.GENERIC_ERROR && it.message.isNotNullOrEmpty() }
                        ?.message?.let { UiString.UiStringText(it) }
                        ?: UiString.UiStringRes(R.string.coupon_create_coupon_creation_failed)
                triggerEvent(ShowUiStringSnackbar(message))
            }
    }

    private suspend fun updateCoupon(newCoupon: Coupon) {
        couponRepository.updateCoupon(newCoupon)
            .onSuccess {
                triggerEvent(ShowSnackbar(R.string.coupon_edit_coupon_updated))
                triggerEvent(Exit)

                analyticsTrackerWrapper.track(AnalyticsEvent.COUPON_UPDATE_SUCCESS)
            }
            .onFailure { exception ->
                WooLog.e(
                    tag = WooLog.T.COUPONS,
                    message = "Coupon update failed: ${exception.message}"
                )

                val wooErrorType = (exception as? WooException)?.error?.type
                analyticsTrackerWrapper.track(
                    stat = AnalyticsEvent.COUPON_UPDATE_FAILED,
                    errorContext = this@EditCouponViewModel.javaClass.simpleName,
                    errorType = wooErrorType?.name,
                    errorDescription = exception.message
                )

                val message =
                    exception.takeIf { wooErrorType == WooErrorType.GENERIC_ERROR && it.message.isNotNullOrEmpty() }
                        ?.message?.let { UiString.UiStringText(it) }
                        ?: UiString.UiStringRes(R.string.coupon_edit_coupon_update_failed)
                triggerEvent(ShowUiStringSnackbar(message))
            }
    }

    private fun trackUpdateChanges(oldCoupon: Coupon, newCoupon: Coupon) {
        val wasCouponTypeUpdated = oldCoupon.type != newCoupon.type
        val wasCouponCodeUpdated = oldCoupon.code != newCoupon.code
        val wasCouponAmountUpdated = oldCoupon.amount != newCoupon.amount
        val wasCouponDescriptionUpdated = oldCoupon.description != newCoupon.description
        val wereCouponProdsOrCatsUpdated = oldCoupon.productIds != newCoupon.productIds ||
            oldCoupon.categoryIds != newCoupon.categoryIds
        val wasCouponExpiryDateUpdated = oldCoupon.dateExpires != newCoupon.dateExpires
        val wereCouponUsageRestrictionsUpdated = oldCoupon.restrictions != newCoupon.restrictions

        analyticsTrackerWrapper.track(
            COUPON_UPDATE_INITIATED,
            mapOf(
                Pair(AnalyticsTracker.KEY_COUPON_DISCOUNT_TYPE_UPDATED, wasCouponTypeUpdated),
                Pair(AnalyticsTracker.KEY_COUPON_CODE_UPDATED, wasCouponCodeUpdated),
                Pair(AnalyticsTracker.KEY_COUPON_AMOUNT_UPDATED, wasCouponAmountUpdated),
                Pair(AnalyticsTracker.KEY_COUPON_DESCRIPTION_UPDATED, wasCouponDescriptionUpdated),
                Pair(AnalyticsTracker.KEY_COUPON_ALLOWED_PRODUCTS_OR_CATEGORIES_UPDATED, wereCouponProdsOrCatsUpdated),
                Pair(AnalyticsTracker.KEY_COUPON_EXPIRY_DATE_UPDATED, wasCouponExpiryDateUpdated),
                Pair(AnalyticsTracker.KEY_COUPON_USAGE_RESTRICTIONS_UPDATED, wereCouponUsageRestrictionsUpdated)
            )
        )
    }

    private fun trackCreateCoupon(newCoupon: Coupon) {
        val type = when (newCoupon.type) {
            is Coupon.Type.FixedCart -> VALUE_COUPON_DISCOUNT_TYPE_FIXED_CART
            is Coupon.Type.Percent -> VALUE_COUPON_DISCOUNT_TYPE_PERCENTAGE
            is Coupon.Type.FixedProduct -> VALUE_COUPON_DISCOUNT_TYPE_FIXED_PRODUCT
            is Coupon.Type.Custom -> VALUE_COUPON_DISCOUNT_TYPE_CUSTOM
            null -> null
        }

        val hasProductOrCategoryRestrictions = with(newCoupon.restrictions) {
            excludedProductIds.isNotEmpty() || excludedCategoryIds.isNotEmpty()
        }
        analyticsTrackerWrapper.track(
            COUPON_CREATION_INITIATED,
            mapOf(
                KEY_COUPON_DISCOUNT_TYPE to type,
                KEY_HAS_EXPIRY_DATE to (newCoupon.dateExpires != null),
                KEY_INCLUDES_FREE_SHIPPING to newCoupon.isShippingFree,
                KEY_HAS_DESCRIPTION to (newCoupon.description != null),
                KEY_HAS_PRODUCT_OR_CATEGORY_RESTRICTIONS to hasProductOrCategoryRestrictions,
                KEY_HAS_USAGE_RESTRICTIONS to newCoupon.hasUsageRestrictions()
            )
        )
    }

    private fun Coupon.hasUsageRestrictions() = with(restrictions) {
        isForIndividualUse == true ||
            usageLimit != null ||
            usageLimitPerUser != null ||
            limitUsageToXItems != null ||
            areSaleItemsExcluded == true ||
            minimumAmount != null ||
            maximumAmount != null ||
            excludedProductIds.isNotEmpty() ||
            excludedCategoryIds.isNotEmpty() ||
            restrictedEmails.isNotEmpty()
    }

    data class ViewState(
        val couponDraft: Coupon,
        val amountUnit: String,
        val hasChanges: Boolean,
        val isSaving: Boolean,
        @StringRes val saveButtonText: Int,
        val screenTitle: String,
    )

    @Parcelize
    sealed class Mode : Parcelable {
        @Parcelize
        data class Create(val type: Coupon.Type) : Mode()

        @Parcelize
        data class Edit(val couponId: Long) : Mode()
    }
}
