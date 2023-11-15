package com.woocommerce.android.ui.products.subscriptions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.model.SubscriptionPeriod
import com.woocommerce.android.model.SubscriptionPeriod.Day
import com.woocommerce.android.model.SubscriptionPeriod.Month
import com.woocommerce.android.model.SubscriptionPeriod.Week
import com.woocommerce.android.model.SubscriptionPeriod.Year
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.component.WcExposedDropDown
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils

@AndroidEntryPoint
class ProductSubscriptionFreeTrialFragment : BaseFragment(), BackPressListener {
    companion object {
        const val KEY_SUBSCRIPTION_FREE_TRIAL_RESULT = "key_subscription_free_trial_result"
    }

    private val resourceProvider: ResourceProvider by lazy { ResourceProvider(requireContext()) }

    private val trialViewModel: ProductSubscriptionFreeTrialViewModel by viewModels()

    override fun getFragmentTitle() = getString(R.string.product_subscription_free_trial_title)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    trialViewModel.viewState.observeAsState().value?.let { state ->
                        SubscriptionFreeTrial(
                            length = state.length,
                            items = state.periods,
                            period = state.period,
                            isError = state.isError,
                            onTrialLengthUpdated = { trialViewModel.onLengthChanged(it) },
                            onTrialPeriodUpdated = { trialViewModel.onPeriodChanged(it) }
                        )
                    }
                }
            }
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        trialViewModel.onExitRequested()
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        trialViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Exit -> {
                    findNavController().navigateUp()
                }
                is ExitWithResult<*> -> {
                    navigateBackWithResult(KEY_SUBSCRIPTION_FREE_TRIAL_RESULT, event.data)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()

        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
    }

    @Composable
    private fun SubscriptionFreeTrial(
        length: Int,
        items: List<SubscriptionPeriod>,
        period: SubscriptionPeriod,
        isError: Boolean,
        onTrialLengthUpdated: (Int) -> Unit,
        onTrialPeriodUpdated: (SubscriptionPeriod) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Card(
                shape = RectangleShape,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .background(colorResource(id = color.color_surface))
                        .padding(dimensionResource(id = dimen.major_100)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = if (length == 0) "" else length.toString(),
                        isError = isError,
                        onValueChange = { input ->
                            val trialLength = if (input.isNotBlank()) {
                                input.filter { it.isDigit() }.toIntOrNull() ?: 0
                            } else {
                                0
                            }
                            onTrialLengthUpdated(trialLength)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(.3f)
                            .padding(end = dimensionResource(id = dimen.major_100))
                    )
                    WcExposedDropDown(
                        items = items.toTypedArray(),
                        onSelected = { onTrialPeriodUpdated(it) },
                        currentValue = period,
                        mapper = { it.getPeriodString(resourceProvider, length) },
                        modifier = Modifier
                            .weight(.7f)
                            .background(colorResource(id = color.color_surface))
                    )
                }
            }

            Row(
                modifier = Modifier
                    .padding(dimensionResource(id = dimen.major_100)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_deprecated_info_outline_24dp),
                    contentDescription = "",
                    tint = colorResource(id = if (isError) color.color_error else color.color_on_surface_medium),
                    modifier = Modifier
                        .align(Alignment.Top)
                )
                Text(
                    text = resourceProvider.getString(R.string.product_subscription_free_trial_info),
                    style = MaterialTheme.typography.caption,
                    color = colorResource(id = if (isError) color.color_error else color.color_on_surface_medium),
                    modifier = Modifier
                        .align(Alignment.Top)
                        .padding(start = dimensionResource(id = dimen.major_100))
                )
            }
        }
    }

    @Preview
    @Composable
    private fun PreviewSubscriptionFreeTrial() {
        SubscriptionFreeTrial(
            length = 4,
            items = listOf(Day, Week, Month, Year),
            period = Day,
            isError = true,
            onTrialLengthUpdated = {},
            onTrialPeriodUpdated = {}
        )
    }
}
