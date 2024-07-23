package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ShippingLabelCreateServicePackageViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val shippingLabelRepository: ShippingLabelRepository,
    parameterRepository: ParameterRepository
) : ScopedViewModel(savedState) {
    companion object {
        private const val KEY_PARAMETERS = "key_parameters"
    }

    /**
     * Saving more than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can
     * replace @Suppress("OPT_IN_USAGE") with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    val dimensionUnit: String by lazy {
        parameterRepository.getParameters(KEY_PARAMETERS, savedState).dimensionUnit ?: ""
    }

    init {
        launch {
            getSelectableServicePackages()
        }
    }

    private fun getSelectableServicePackages() {
        launch {
            viewState = viewState.copy(isLoading = true)
            val result = shippingLabelRepository.getSelectableServicePackages()
            if (result.isError) {
                triggerEvent(ShowSnackbar(R.string.shipping_label_packages_loading_error))
                return@launch
            }

            val uiModels = result.model!!.map { ServicePackageUiModel(it) }
            viewState = viewState.copy(isLoading = false, uiModels = uiModels, isEmpty = uiModels.isEmpty())
        }
    }

    fun onPackageSelected(id: String) {
        val newList = viewState.uiModels.map {
            ServicePackageUiModel(
                data = it.data,
                isChecked = id == it.data.id
            )
        }

        viewState = viewState.copy(uiModels = newList)
    }

    fun onCustomFormDoneMenuClicked() {
        if (viewState.uiModels.none { it.isChecked }) {
            triggerEvent(ShowSnackbar(R.string.shipping_label_create_service_package_nothing_selected))
        } else {
            val packageToCreate = viewState.uiModels.first { it.isChecked }.data
            launch {
                viewState = viewState.copy(isSavingProgressDialogVisible = true)
                val result = shippingLabelRepository.activateServicePackage(packageToCreate)
                viewState = viewState.copy(isSavingProgressDialogVisible = false)
                when {
                    result.isError -> {
                        AnalyticsTracker.track(
                            stat = AnalyticsEvent.SHIPPING_LABEL_ADD_PACKAGE_FAILED,
                            properties = mapOf(
                                "type" to "predefined",
                                "error" to result.error.message
                            )
                        )
                        val errorMsg = if (result.error.message != null) {
                            result.error.message
                        } else {
                            resourceProvider.getString(
                                R.string.shipping_label_create_custom_package_api_unknown_failure
                            )
                        }

                        triggerEvent(
                            ShowSnackbar(
                                message = R.string.shipping_label_create_custom_package_api_failure,
                                args = arrayOf(errorMsg as String)
                            )
                        )
                    }
                    result.model == true -> {
                        triggerEvent(PackageSuccessfullyMadeEvent(packageToCreate))
                    }
                    else -> triggerEvent(
                        ShowSnackbar(
                            R.string.shipping_label_create_custom_package_api_unknown_failure
                        )
                    )
                }
            }
        }
    }

    @Parcelize
    data class ViewState(
        val isEmpty: Boolean = false,
        val isLoading: Boolean = false,
        val isSavingProgressDialogVisible: Boolean? = null,
        val uiModels: List<ServicePackageUiModel> = emptyList()
    ) : Parcelable {
        val canSave: Boolean
            get() = !isEmpty
    }

    @Parcelize
    data class ServicePackageUiModel(
        val data: ShippingPackage,
        val isChecked: Boolean = false
    ) : Parcelable

    data class PackageSuccessfullyMadeEvent(val madePackage: ShippingPackage) : MultiLiveEvent.Event()
}
