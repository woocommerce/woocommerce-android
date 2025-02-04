package com.woocommerce.android.ui.products.details

import android.app.Activity
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentProductDetailBinding
import com.woocommerce.android.ui.products.list.ProductListFragment
import com.woocommerce.android.util.IsWindowClassLargeThanCompact
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

class ProductDetailsToolbarHelper @Inject constructor(
    private val activity: Activity,
    private val isWindowClassLargeThanCompact: IsWindowClassLargeThanCompact,
) : DefaultLifecycleObserver,
    Toolbar.OnMenuItemClickListener {
    private var fragment: ProductDetailFragment? = null
    private var binding: FragmentProductDetailBinding? = null
    private var viewModel: ProductDetailViewModel? = null

    private var menu: Menu? = null

    fun onViewCreated(
        fragment: ProductDetailFragment,
        viewModel: ProductDetailViewModel,
        binding: FragmentProductDetailBinding,
    ) {
        this.fragment = fragment
        this.binding = binding
        this.viewModel = viewModel

        fragment.lifecycle.addObserver(this)

        setupToolbar()

        viewModel.menuButtonsState.observe(fragment.viewLifecycleOwner) {
            menu?.updateOptions(it)
        }
    }

    fun updateTitle(title: String) {
        binding?.productDetailToolbar?.title = title
    }

    override fun onDestroy(owner: LifecycleOwner) {
        fragment = null
        binding = null
        viewModel = null
        menu = null
    }

    fun setupToolbar() {
        val toolbar = binding?.productDetailToolbar ?: return

        toolbar.setOnMenuItemClickListener(this)
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.menu_product_detail_fragment)
        this.menu = toolbar.menu

        toolbar.navigationIcon =
            when {
                isWindowClassLargeThanCompact() -> {
                    val startMode = viewModel?.startMode
                    val isAddNewModeCreationFlow = startMode == ProductDetailFragment.Mode.AddNewProduct
                    val isProductShownAfterGenerationWithAi = startMode is ProductDetailFragment.Mode.ShowProduct &&
                        startMode.afterGeneratedWithAi
                    if (isAddNewModeCreationFlow || isProductShownAfterGenerationWithAi) {
                        AppCompatResources.getDrawable(activity, R.drawable.ic_back_24dp)
                    } else {
                        null
                    }
                }
                isPartOfProductListFlow() -> {
                    AppCompatResources.getDrawable(activity, R.drawable.ic_back_24dp)
                }

                else -> {
                    AppCompatResources.getDrawable(activity, R.drawable.ic_gridicons_cross_24dp)
                }
            }

        toolbar.setNavigationOnClickListener {
            if (viewModel?.onBackButtonClickedProductDetail() == false) return@setNavigationOnClickListener

            if (fragment?.findNavController()?.popBackStack(R.id.products, false) == false) {
                // in case the back stack is empty, indicating that the ProductDetailsFragment is shown in details pane
                // of the ProductListFragment, we need to propagate back press to the parent fragment manually.
                fragment?.requireActivity()?.onBackPressedDispatcher?.onBackPressed()
            }
        }

        // change the font color of the trash menu item to red, and only show it if it should be enabled
        with(toolbar.menu.findItem(R.id.menu_trash_product)) {
            if (this == null) return@with
            val title = SpannableString(this.title)
            title.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        activity,
                        R.color.woo_red_30
                    )
                ),
                0,
                title.length,
                0
            )
            this.title = title
        }

        viewModel?.menuButtonsState?.value?.let {
            toolbar.menu.updateOptions(it)
        }
    }

    private fun isPartOfProductListFlow() = fragment?.findNavController()?.hasBackStackEntry(R.id.products) == true ||
        fragment?.parentFragment?.parentFragment is ProductListFragment

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_publish -> {
                ActivityUtils.hideKeyboard(activity)
                viewModel?.onPublishButtonClicked()
                true
            }

            R.id.menu_save_as_draft -> {
                viewModel?.onSaveAsDraftButtonClicked()
                true
            }

            R.id.menu_share -> {
                viewModel?.onShareButtonClicked()
                true
            }

            R.id.menu_save -> {
                ActivityUtils.hideKeyboard(activity)
                viewModel?.onSaveButtonClicked()
                true
            }

            R.id.menu_view_product -> {
                viewModel?.onViewProductOnStoreLinkClicked()
                true
            }

            R.id.menu_product_settings -> {
                viewModel?.onSettingsButtonClicked()
                true
            }

            R.id.menu_duplicate -> {
                viewModel?.onDuplicateProduct()
                true
            }

            R.id.menu_trash_product -> {
                viewModel?.onTrashButtonClicked()
                true
            }

            else -> false
        }
    }

    private fun Menu.updateOptions(state: ProductDetailViewModel.MenuButtonsState) {
        findItem(R.id.menu_save)?.isVisible = state.saveOption
        findItem(R.id.menu_save_as_draft)?.isVisible = state.saveAsDraftOption
        findItem(R.id.menu_view_product)?.isVisible = state.viewProductOption
        findItem(R.id.menu_publish)?.apply {
            isVisible = state.publishOption
            if (state.saveOption) {
                setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER)
            } else {
                setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            }
        }
        findItem(R.id.menu_share)?.apply {
            isVisible = state.shareOption

            setShowAsActionFlags(
                if (state.showShareOptionAsActionWithText) {
                    MenuItem.SHOW_AS_ACTION_IF_ROOM
                } else {
                    MenuItem.SHOW_AS_ACTION_NEVER
                }
            )
        }
        findItem(R.id.menu_trash_product)?.isVisible = state.trashOption
    }

    private fun NavController.hasBackStackEntry(@IdRes destinationId: Int) = runCatching {
        getBackStackEntry(destinationId)
    }.isSuccess
}
