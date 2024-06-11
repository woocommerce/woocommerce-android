package com.woocommerce.android.ui.products.ai

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCTextButton

@Composable
fun AddProductWithAIScreen(viewModel: AddProductWithAIViewModel) {
    BackHandler(onBack = viewModel::onBackButtonClick)

    viewModel.state.observeAsState().value?.let {
        AddProductWithAIScreen(
            state = it,
            onBackButtonClick = viewModel::onBackButtonClick,
            onSaveButtonClick = viewModel::onSaveButtonClick
        )
    }
}

@Composable
fun AddProductWithAIScreen(
    state: AddProductWithAIViewModel.State,
    onBackButtonClick: () -> Unit,
    onSaveButtonClick: () -> Unit
) {
    Scaffold(
        topBar = {
            Toolbar(
                navigationIcon = if (state.isFirstStep) Icons.Filled.Clear else Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationButtonClick = onBackButtonClick,
                actions = {
                    when (state.saveButtonState) {
                        AddProductWithAIViewModel.SaveButtonState.Shown -> WCTextButton(onClick = onSaveButtonClick) {
                            Text(text = stringResource(id = R.string.product_detail_save_as_draft))
                        }

                        AddProductWithAIViewModel.SaveButtonState.Loading -> CircularProgressIndicator(
                            modifier = Modifier
                                .size(
                                    width = dimensionResource(id = R.dimen.major_325),
                                    height = dimensionResource(id = R.dimen.major_100)
                                )
                                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                        )

                        else

                        -> {
                        } // No-op
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            val animatedProgress = animateFloatAsState(
                targetValue = state.progress,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                label = "progressBarAnimation"
            ).value
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colorResource(id = color.linear_progress_background),
            )

            SubScreen(
                subViewModel = state.subViewModel,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SubScreen(subViewModel: AddProductWithAISubViewModel<*>, modifier: Modifier) {
    when (subViewModel) {
        is ProductNameSubViewModel -> ProductNameSubScreen(subViewModel, modifier)
        is AboutProductSubViewModel -> AboutProductSubScreen(subViewModel, modifier)
        is ProductPreviewSubViewModel -> ProductPreviewSubScreen(subViewModel, modifier)
    }
}
