package com.woocommerce.android.ui.products

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.util.ChromeCustomTabUtils

@Composable
fun AIPriceAdvisorDialog(
    viewModel: AIPriceAdvisorViewModel,
    onCloseButtonClick: () -> Unit = {}
) {
    viewModel.viewState.observeAsState().value?.let { state ->
        Scaffold(
            topBar = { Header() },
            bottomBar = {
                Footer(
                    state,
                    onCloseButtonClick,
                    viewModel::onRetryButtonClick,
                )
            },
            backgroundColor = MaterialTheme.colors.surface
        ) { paddingValues ->
            AdvisorContent(
                state = state,
                onDescriptionFeedbackReceived = viewModel::onDescriptionFeedbackReceived,
                modifier = Modifier
                    .background(MaterialTheme.colors.surface)
                    .padding(paddingValues)
                    .fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AdvisorContent(
    state: AIPriceAdvisorViewModel.ViewState,
    onDescriptionFeedbackReceived: (Boolean) -> Unit,
    modifier: Modifier
) {
    AnimatedContent(state) {
        Column(
            modifier = modifier
                .fillMaxWidth()
        ) {
            when (state.generationState) {
                is AIPriceAdvisorViewModel.GenerationState.Generating,
                is AIPriceAdvisorViewModel.GenerationState.Regenerating -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(dimensionResource(id = R.dimen.major_100)),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.ai_price_advisor_sale_price_dialog_loading_message),
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(dimensionResource(id = R.dimen.major_100))
                                .size(dimensionResource(id = R.dimen.major_250))
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                }

                is AIPriceAdvisorViewModel.GenerationState.Generated -> {
                    Column(
                        modifier = Modifier
                            .padding(dimensionResource(id = R.dimen.major_100))
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(text = state.generatedAdvice)
                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
                        Survey(onDescriptionFeedbackReceived)
                    }
                }

                is AIPriceAdvisorViewModel.GenerationState.Failed -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(dimensionResource(id = R.dimen.major_100)),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.ai_price_advisor_sale_price_dialog_failed_message),
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.ai_price_advisor_sale_price_dialog_title),
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))
        )

        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10)
        )
    }
}

@Composable
fun Footer(
    state: AIPriceAdvisorViewModel.ViewState,
    onCloseButtonClick: () -> Unit = {},
    onRetryButtonClick: () -> Unit = {}
) {
    val context = LocalContext.current
    Column {
        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10)
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))

        if (state.generationState is AIPriceAdvisorViewModel.GenerationState.Failed) {
            WCOutlinedButton(
                onClick = onRetryButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            ) {
                Text(text = stringResource(id = R.string.retry))
            }
        } else if (state.generationState is AIPriceAdvisorViewModel.GenerationState.Generated) {
            WCOutlinedButton(
                onClick = onRetryButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            ) {
                Text(text = stringResource(id = R.string.ai_price_advisor_sale_price_dialog_another_one_button))
            }
        }

        WCColoredButton(
            onClick = onCloseButtonClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
        ) {
            Text(text = stringResource(id = R.string.close))
        }
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
        ) {
            Text(
                text = stringResource(id = R.string.ai_generic_powered_by) + " ",
                style = MaterialTheme.typography.subtitle2,
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(stringResource(R.string.learn_more))
                    }
                },
                style = MaterialTheme.typography.subtitle2,
                modifier = Modifier.clickable {
                    ChromeCustomTabUtils.launchUrl(
                        context,
                        AppUrls.AUTOMATTIC_AI_GUIDELINES
                    )
                }
            )
        }
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
    }
}

@Suppress("LongMethod")
@Composable
fun Survey(onDescriptionFeedbackReceived: (Boolean) -> Unit) {
    Column {
        val isFeedbackVisible = remember { mutableStateOf(true) }
        if (isFeedbackVisible.value) {
            ConstraintLayout(
                modifier = Modifier
                    .background(
                        color = colorResource(id = R.color.woo_black_alpha_008),
                        shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_50))
                    )
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(id = R.dimen.major_100),
                        top = dimensionResource(id = R.dimen.minor_100),
                        bottom = dimensionResource(id = R.dimen.minor_100)
                    )
            ) {
                val (text, like, dislike) = createRefs()

                Text(
                    modifier = Modifier
                        .constrainAs(text) {
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(like.start)
                            width = Dimension.fillToConstraints
                        },
                    text = stringResource(id = R.string.ai_product_description_feedback),
                    color = colorResource(id = R.color.color_on_surface_medium),
                    style = MaterialTheme.typography.caption
                )

                IconButton(
                    modifier = Modifier
                        .constrainAs(like) {
                            top.linkTo(parent.top)
                            end.linkTo(dislike.start)
                            bottom.linkTo(parent.bottom)
                        },
                    onClick = {
                        onDescriptionFeedbackReceived(true)
                        isFeedbackVisible.value = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(dimensionResource(id = R.dimen.major_150)),
                        tint = colorResource(id = R.color.color_on_surface_medium)
                    )
                }

                IconButton(
                    modifier = Modifier
                        .constrainAs(dislike) {
                            top.linkTo(parent.top)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        },
                    onClick = {
                        onDescriptionFeedbackReceived(false)
                        isFeedbackVisible.value = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbDown,
                        contentDescription = null,
                        modifier = Modifier.size(dimensionResource(id = R.dimen.major_150)),
                        tint = colorResource(id = R.color.color_on_surface_medium)
                    )
                }
            }
        }
    }
}
