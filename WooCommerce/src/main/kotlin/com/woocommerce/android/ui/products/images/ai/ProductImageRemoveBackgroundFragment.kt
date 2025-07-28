package com.woocommerce.android.ui.products.images.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.compose.AsyncImage
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class ProductImageRemoveBackgroundFragment : BaseFragment() {

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: ProductImageRemoveBackgroundViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    Surface {
                        ProductImageRemoveBackgroundScreen(viewModel.state.collectAsState())
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ProductImageRemoveBackgroundViewModel.ExitScreen -> {
                    findNavController().navigateUp()
                }
                is MultiLiveEvent.Event.ShowSnackbar -> {
                    uiMessageResolver.showSnack(event.message)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.state.collect {
                when (it) {
                    is ViewState.BackgroundProcessingInProgress -> {
                        // Show loading state if needed
                    }
                    is ViewState.Success -> {
                        // Handle the completion of background processing
                        // For example, you might want to show the processed image
                    }
                    is ViewState.Failure -> {
                        uiMessageResolver.showSnack(R.string.error_generic)
                    }

                    ViewState.ImageUploadInProgress -> {

                    }
                }
            }
        }
    }
}

@Composable
fun ProductImageRemoveBackgroundScreen(state: State<ViewState>) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.remove_background)) },
                backgroundColor = MaterialTheme.colors.surface
            )
        }
    ) { paddingValues ->
        when(val viewState = state.value) {
            is ViewState.Success -> {
                AsyncImage(
                    model = viewState.bitmap,
                    contentDescription = stringResource(R.string.product_image_content_description),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            is ViewState.BackgroundProcessingInProgress -> {
                CircularProgressIndicator()
            }
            ViewState.Failure -> {
                Text("Failure!")
            }
            ViewState.ImageUploadInProgress -> {
                Text("Uploading image...")
            }
        }
    }
}

@Preview
@Composable
fun ProductImageRemoveBackgroundScreenPreview() {
    WooThemeWithBackground {
        val image = Product.Image(
            id = 1L,
            source = "https://ma.tt/",
            name = "Sample Image",
            isCoverImage = true,
            dateCreated = Date.from(Instant.now()),
        )
        val state = remember {
            mutableStateOf(ViewState.Failure)
        }
        ProductImageRemoveBackgroundScreen(state)
    }
}
