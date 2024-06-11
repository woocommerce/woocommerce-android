package com.woocommerce.android.ui.products.subscriptions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WcExposedDropDown
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductExitEvent.ExitProductSubscriptionExpiration
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductSubscriptionExpirationFragment : BaseProductFragment() {
    companion object {
        const val KEY_SUBSCRIPTION_EXPIRATION_RESULT = "key_subscription_expiration_result"
    }

    private val navArgs: ProductSubscriptionExpirationFragmentArgs by navArgs()
    private val resourceProvider: ResourceProvider by lazy { ResourceProvider(requireContext()) }
    private var selectedExpiration: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            val subscription = navArgs.subscription
            selectedExpiration = subscription.length
            setContent {
                WooThemeWithBackground {
                    SubscriptionExpirationPicker(
                        items = subscription.expirationDisplayOptions(resourceProvider),
                        currentValue = subscription.expirationDisplayValue(resourceProvider),
                        onBackPressed = { viewModel.onBackButtonClicked(ExitProductSubscriptionExpiration) }
                    )
                }
            }
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked(ExitProductSubscriptionExpiration)
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ExitProductSubscriptionExpiration ->
                    navigateBackWithResult(KEY_SUBSCRIPTION_EXPIRATION_RESULT, selectedExpiration)
            }
        }
    }

    @Composable
    private fun SubscriptionExpirationPicker(
        items: Map<String, Int>,
        currentValue: String,
        onBackPressed: () -> Unit,
    ) {
        Scaffold(
            topBar = {
                Toolbar(
                    title = stringResource(id = string.product_subscription_expiration_title),
                    onNavigationButtonClick = onBackPressed,
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                Card(
                    shape = RectangleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(paddingValues)
                ) {
                    Row(
                        modifier = Modifier
                            .background(colorResource(id = color.color_surface))
                            .padding(dimensionResource(id = dimen.major_100)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(id = string.subscription_expire))
                        WcExposedDropDown(
                            items = items.keys.toTypedArray(),
                            onSelected = { selectedExpiration = items[it] },
                            currentValue = currentValue,
                            modifier = Modifier
                                .background(colorResource(id = color.color_surface))
                                .padding(start = dimensionResource(id = dimen.major_100))
                        )
                    }
                }
            }
        }
    }

    @Preview
    @Composable
    private fun PreviewSubscriptionExpirationPicker() {
        SubscriptionExpirationPicker(
            items = mapOf("Never" to 0, "1 month" to 1, "2 months" to 2, "3 months" to 3, "4 months" to 4),
            currentValue = "Never",
            onBackPressed = { }
        )
    }
}
