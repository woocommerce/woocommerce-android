package com.woocommerce.android.ui.products

import android.app.AlertDialog
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.transition.ChangeBounds
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.woocommerce.android.R
import com.woocommerce.android.R.style
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.di.GlideApp
import kotlinx.android.synthetic.main.fragment_image_viewer.*

class ProductImageViewerFragment : BaseProductFragment(), RequestListener<Drawable> {
    companion object {
        private const val KEY_IS_CONFIRMATION_SHOWING = "is_confirmation_showing"
    }

    private val navArgs: ProductImageViewerFragmentArgs by navArgs()

    private var isConfirmationShowing = false
    private var confirmationDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        sharedElementEnterTransition = ChangeBounds()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_image_viewer, container, false)
    }

    override fun onRequestAllowBackPress(): Boolean {
        return true
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        loadImage()
        savedInstanceState?.let { bundle ->
            if (bundle.getBoolean(KEY_IS_CONFIRMATION_SHOWING)) {
                confirmRemoveProductImage()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_CONFIRMATION_SHOWING, isConfirmationShowing)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_trash, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_trash -> {
                confirmRemoveProductImage()
                true
            } else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun loadImage() {
        showProgress(true)

        GlideApp.with(this)
                .load(navArgs.imageUrl)
                .listener(this)
                .into(photoView)
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    /**
     * Glide failed to load the image
     */
    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: com.bumptech.glide.request.target.Target<Drawable>?,
        isFirstResource: Boolean
    ): Boolean {
        showProgress(false)
        uiMessageResolver.showSnack(R.string.error_loading_image)
        findNavController().navigateUp()
        return false
    }

    /**
     * Glide has loaded the image
     */
    override fun onResourceReady(
        resource: Drawable?,
        model: Any?,
        target: com.bumptech.glide.request.target.Target<Drawable>?,
        dataSource: DataSource?,
        isFirstResource: Boolean
    ): Boolean {
        showProgress(false)
        return false
    }

    /**
     * Confirms that the user meant to remove this image from the product
     */
    private fun confirmRemoveProductImage() {
        isConfirmationShowing = true
        confirmationDialog = AlertDialog.Builder(ContextThemeWrapper(requireActivity(), style.AppTheme))
                .setMessage(R.string.product_image_remove_confirmation)
                .setCancelable(true)
                .setPositiveButton(R.string.remove) { _, _ ->
                    viewModel.removeProductImageFromDraft(navArgs.imageId)
                    findNavController().navigateUp()
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    isConfirmationShowing = false
                }
                .show()
    }
}
