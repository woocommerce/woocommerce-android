package com.woocommerce.android.ui.blaze.creation.ad

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.blaze.creation.ad.ProductImagePickerViewModel.ImageSelectedResult
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductImagePickerFragment : BaseFragment() {
    companion object {
        const val ON_PRODUCT_IMAGE_SELECTED = "on_product_image_selected"
    }

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    val viewModel: ProductImagePickerViewModel by viewModels()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            ProductImagePickerScreen(viewModel = viewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Exit -> findNavController().popBackStack()
                is ExitWithResult<*> -> {
                    navigateBackWithResult(ON_PRODUCT_IMAGE_SELECTED, event.data as ImageSelectedResult)
                }
            }
        }
    }

    @Composable
    fun ProductImagePickerScreen(viewModel: ProductImagePickerViewModel) {
        viewModel.viewState.observeAsState().value?.let { viewState ->
            ProductImagePickerScreen(
                viewState = viewState,
                onImageSelected = viewModel::onImageSelected
            )
        }
    }

    @Composable
    fun ProductImagePickerScreen(
        viewState: ProductImagePickerViewModel.ViewState,
        onImageSelected: (Product.Image) -> Unit
    ) {
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            columns = GridCells.Adaptive(minSize = 128.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(viewState.productImages) { photoUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoUrl.source)
                        .crossfade(true)
                        .build(),
                    fallback = painterResource(R.drawable.blaze_campaign_product_placeholder),
                    placeholder = painterResource(R.drawable.blaze_campaign_product_placeholder),
                    error = painterResource(R.drawable.blaze_campaign_product_placeholder),
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(128.dp)
                        .clickable { onImageSelected(photoUrl) }
                )
            }
        }
    }
}
