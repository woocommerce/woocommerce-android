package com.woocommerce.android.ui.products.subscriptions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.model.SubscriptionPeriod
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.component.WcExposedDropDown
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
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
        onTrialLengthUpdated: (Int) -> Unit,
        onTrialPeriodUpdated: (SubscriptionPeriod) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Box(
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
                        onValueChange = { input ->
                            val trialLength = if (input.isNotEmpty()) {
                                input.filter { it.isDigit() }.toInt()
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
                            .padding(start = dimensionResource(id = dimen.major_100))
                    )
                }
            }
            Divider(
                color = colorResource(id = color.divider_color),
                thickness = dimensionResource(id = R.dimen.minor_10)
            )
        }
    }
}

