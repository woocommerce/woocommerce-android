package com.woocommerce.android.ui.products.categories

import android.content.DialogInterface
import android.os.Parcelable
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.WooException
import com.woocommerce.android.model.sortCategories
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.RESOURCE_ALREADY_EXISTS
import javax.inject.Inject

@HiltViewModel
class AddProductCategoryViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val productCategoriesRepository: ProductCategoriesRepository,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedState) {
    private val navArgs: AddProductCategoryFragmentArgs by savedState.navArgs()

    // view state for the add category screen
    val addProductCategoryViewStateLiveData = LiveDataDelegate(
        savedState,
        AddProductCategoryViewState(
            categoryName = navArgs.productCategory?.name ?: "",
            selectedParentId = navArgs.productCategory?.parentId
        )
    )
    private var addProductCategoryViewState by addProductCategoryViewStateLiveData

    // view state for the parent category list screen
    val parentCategoryListViewStateData = LiveDataDelegate(savedState, ParentCategoryListViewState())
    private var parentCategoryListViewState by parentCategoryListViewStateData

    private val _parentCategories = MutableLiveData<List<ProductCategoryItemUiModel>>()
    val parentCategories: LiveData<List<ProductCategoryItemUiModel>> = _parentCategories

    override fun onCleared() {
        super.onCleared()
        productCategoriesRepository.onCleanup()
    }

    fun onBackButtonClicked(categoryName: String, parentId: String): Boolean {
        val hasChanges = categoryName.isNotEmpty() || parentId.isNotEmpty()
        return if (hasChanges && addProductCategoryViewState.shouldShowDiscardDialog) {
            triggerEvent(
                ShowDialog.buildDiscardDialogEvent(
                    positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                        addProductCategoryViewState = addProductCategoryViewState.copy(shouldShowDiscardDialog = false)
                        triggerEvent(Exit)
                    },
                    negativeBtnAction = DialogInterface.OnClickListener { _, _ ->
                        addProductCategoryViewState = addProductCategoryViewState.copy(shouldShowDiscardDialog = true)
                    }
                )
            )
            false
        } else {
            true
        }
    }

    fun onCategoryNameChanged(categoryName: String) {
        addProductCategoryViewState = if (categoryName.isEmpty()) {
            addProductCategoryViewState.copy(categoryNameErrorMessage = string.add_product_category_empty)
        } else {
            addProductCategoryViewState.copy(categoryNameErrorMessage = 0)
        }
    }

    fun saveProductCategory(categoryName: String, parentId: Long = getSelectedParentId()) {
        if (categoryName.isEmpty()) {
            addProductCategoryViewState = addProductCategoryViewState.copy(
                categoryNameErrorMessage = string.add_product_category_empty
            )
            return
        }

        addProductCategoryViewState = addProductCategoryViewState.copy(displayProgressDialog = true)
        launch {
            if (networkStatus.isConnected()) {
                val categoryNameTrimmed = TextUtils.htmlEncode(categoryName.trim())
                val requestResult = when {
                    isEditingExistingCategory() -> productCategoriesRepository.updateProductCategory(
                        navArgs.productCategory!!.remoteCategoryId,
                        categoryName,
                        parentId
                    )

                    else -> productCategoriesRepository.addProductCategory(categoryName, parentId)
                }
                requestResult
                    .onSuccess {
                        val successString = when {
                            isEditingExistingCategory() -> R.string.update_product_category_success
                            else -> R.string.add_product_category_success
                        }
                        triggerEvent(ShowSnackbar(successString))
                        val addedCategory = productCategoriesRepository
                            .getProductCategoryByNameAndParentId(categoryNameTrimmed, parentId)
                        triggerEvent(ExitWithResult(addedCategory))
                    }
                    .onFailure {
                        WooLog.e(
                            tag = WooLog.T.PRODUCTS,
                            message = "Error adding product category: ${it.message}"
                        )
                        when ((it as WooException).error.type) {
                            RESOURCE_ALREADY_EXISTS -> addProductCategoryViewState = addProductCategoryViewState.copy(
                                categoryNameErrorMessage = string.add_product_category_duplicate
                            )

                            else -> triggerEvent(ShowSnackbar(string.add_product_category_failed))
                        }
                    }
            } else {
                triggerEvent(ShowSnackbar(string.offline_error))
            }
            addProductCategoryViewState = addProductCategoryViewState.copy(displayProgressDialog = false)
        }
    }

    private fun isEditingExistingCategory(): Boolean = navArgs.productCategory != null

    fun fetchParentCategories() {
        loadParentCategories()
    }

    fun onCategorySelected(categoryId: Long) {
        addProductCategoryViewState = addProductCategoryViewState.copy(selectedParentId = categoryId)
        triggerEvent(Exit)
    }

    fun getSelectedParentId() = addProductCategoryViewState.selectedParentId ?: 0L

    fun getSelectedParentCategoryName(): String? =
        productCategoriesRepository.getProductCategoryByRemoteId(getSelectedParentId())?.name

    /**
     * Refreshes the list of categories by calling the [loadParentCategories] method
     * which eventually checks, if there is anything new to fetch from the server
     *
     */
    fun refreshParentCategories() {
        parentCategoryListViewState = parentCategoryListViewState.copy(isRefreshing = true)
        loadParentCategories()
    }

    /**
     * Loads the list of categories from the database or from the server.
     * This depends on whether categories are stored in the database, and if any new ones are
     * required to be fetched.
     *
     * @param loadMore Whether to load more categories after the ones loaded
     */
    private fun loadParentCategories(loadMore: Boolean = false) {
        if (parentCategoryListViewState.isLoading == true) {
            WooLog.d(WooLog.T.PRODUCTS, "already loading parent categories")
            return
        }

        if (loadMore && !productCategoriesRepository.canLoadMoreProductCategories) {
            WooLog.d(WooLog.T.PRODUCTS, "can't load more parent categories")
            return
        }

        launch {
            val showSkeleton: Boolean
            if (loadMore) {
                showSkeleton = false
            } else {
                // if this is the initial load, first get the categories from the db and show them immediately
                val productsInDb = productCategoriesRepository.getProductCategoriesList()
                if (productsInDb.isEmpty()) {
                    showSkeleton = true
                } else {
                    _parentCategories.value = productsInDb.sortCategories(resourceProvider)
                    showSkeleton = false
                }
            }
            parentCategoryListViewState = parentCategoryListViewState.copy(
                isLoading = true,
                isLoadingMore = loadMore,
                isSkeletonShown = showSkeleton,
                isEmptyViewVisible = false
            )
            fetchParentCategories(loadMore = loadMore)
        }
    }

    /**
     * Triggered when the user scrolls past the point of loaded categories
     * already displayed on the screen or on record.
     */
    fun onLoadMoreParentCategoriesRequested() {
        loadParentCategories(loadMore = true)
    }

    /**
     * This method is used to fetch the categories from the backend. It does not
     * check the database.
     *
     * @param loadMore Whether this is another page or the first one
     */
    private suspend fun fetchParentCategories(loadMore: Boolean = false) {
        if (networkStatus.isConnected()) {
            _parentCategories.value = productCategoriesRepository
                .fetchProductCategories(loadMore = loadMore)
                .getOrElse {
                    triggerEvent(ShowSnackbar(R.string.error_generic))
                    productCategoriesRepository.getProductCategoriesList()
                }
                .sortCategories(resourceProvider)

            parentCategoryListViewState = parentCategoryListViewState.copy(
                isLoading = true,
                canLoadMore = productCategoriesRepository.canLoadMoreProductCategories,
                isEmptyViewVisible = _parentCategories.value?.isEmpty() == true
            )
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }

        parentCategoryListViewState = parentCategoryListViewState.copy(
            isSkeletonShown = false,
            isLoading = false,
            isLoadingMore = false,
            isRefreshing = false
        )
    }

    @Parcelize
    data class AddProductCategoryViewState(
        val displayProgressDialog: Boolean? = null,
        val categoryNameErrorMessage: Int? = null,
        val categoryName: String = "",
        val selectedParentId: Long? = null,
        val shouldShowDiscardDialog: Boolean = true
    ) : Parcelable

    @Parcelize
    data class ParentCategoryListViewState(
        val isSkeletonShown: Boolean? = null,
        val isLoading: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val canLoadMore: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null
    ) : Parcelable
}
