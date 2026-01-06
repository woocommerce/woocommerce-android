@file:Suppress(
    "WooPosDesignSystemTextUsageRule",
    "WooPosDesignSystemCornerRadiusUsageRule",
    "DestructuringDeclarationWithTooManyEntries"
)

package com.woocommerce.android.ui.woopospromo

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun WooPosPromoCarouselModal(
    state: WooPosPromoState,
    onDismiss: () -> Unit,
    onNextClick: () -> Unit,
    onExploreClick: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        WooPosPromoCarouselContent(
            state = state,
            onDismiss = onDismiss,
            onNextClick = onNextClick,
            onExploreClick = onExploreClick,
        )
    }
}

@Composable
private fun WooPosPromoCarouselContent(
    state: WooPosPromoState,
    onDismiss: () -> Unit,
    onNextClick: () -> Unit,
    onExploreClick: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = state.currentPage,
        pageCount = { state.pages.size }
    )
    val isLastPage = state.currentPage == state.pages.size - 1

    LaunchedEffect(state.currentPage) {
        pagerState.animateScrollToPage(state.currentPage)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(size = 8.dp)
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .height(534.dp)
        ) {
            val (image, closeButton, title, pager, indicator, button) = createRefs()
            val spacing = 16.dp

            Image(
                painter = painterResource(id = state.imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .constrainAs(image) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                    }
                    .height(209.dp)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .constrainAs(closeButton) {
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)
                    }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_close_24dp),
                    contentDescription = stringResource(R.string.close),
                    tint = colorResource(R.color.color_on_primary),
                )
            }

            Text(
                text = stringResource(state.titleRes),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .constrainAs(title) {
                        top.linkTo(image.bottom, margin = spacing)
                        start.linkTo(parent.start, margin = spacing)
                        end.linkTo(parent.end, margin = spacing)
                        width = Dimension.fillToConstraints
                    }
            )

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier
                    .constrainAs(pager) {
                        top.linkTo(title.bottom, margin = spacing)
                        start.linkTo(parent.start, margin = spacing)
                        end.linkTo(parent.end, margin = spacing)
                        bottom.linkTo(indicator.top, margin = spacing)
                        width = Dimension.fillToConstraints
                    }
            ) { page ->
                Text(
                    text = stringResource(state.pages[page].descriptionRes),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing),
                )
            }

            PageIndicator(
                pageCount = state.pages.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .constrainAs(indicator) {
                        start.linkTo(parent.start)
                        bottom.linkTo(button.top, margin = 32.dp)
                        end.linkTo(parent.end)
                    }
            )

            if (isLastPage) {
                WCColoredButton(
                    onClick = onExploreClick,
                    modifier = Modifier
                        .constrainAs(button) {
                            start.linkTo(parent.start, margin = spacing)
                            end.linkTo(parent.end, margin = spacing)
                            bottom.linkTo(parent.bottom, margin = spacing)
                            width = Dimension.fillToConstraints
                        }
                ) {
                    Text(text = stringResource(R.string.woo_pos_promo_explore_button))
                }
            } else {
                WCOutlinedButton(
                    onClick = onNextClick,
                    modifier = Modifier
                        .constrainAs(button) {
                            start.linkTo(parent.start, margin = spacing)
                            end.linkTo(parent.end, margin = spacing)
                            bottom.linkTo(parent.bottom, margin = spacing)
                            width = Dimension.fillToConstraints
                        }
                ) {
                    Text(text = stringResource(R.string.woo_pos_promo_next_button))
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        }
                    )
            )
        }
    }
}

@Preview(name = "Page 1 - Light")
@Preview(name = "Page 1 - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WooPosPromoPage1Preview() {
    WooThemeWithBackground {
        WooPosPromoCarouselContent(
            state = WooPosPromoState(currentPage = 0),
            onDismiss = {},
            onNextClick = {},
            onExploreClick = {},
        )
    }
}

@Preview(name = "Page 2 - Light")
@Preview(name = "Page 2 - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WooPosPromoPage2Preview() {
    WooThemeWithBackground {
        WooPosPromoCarouselContent(
            state = WooPosPromoState(currentPage = 1),
            onDismiss = {},
            onNextClick = {},
            onExploreClick = {},
        )
    }
}

@Preview(name = "Page 3 - Light")
@Preview(name = "Page 3 - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WooPosPromoPage3Preview() {
    WooThemeWithBackground {
        WooPosPromoCarouselContent(
            state = WooPosPromoState(currentPage = 2),
            onDismiss = {},
            onNextClick = {},
            onExploreClick = {},
        )
    }
}

@Preview(name = "Page 4 - Light")
@Preview(name = "Page 4 - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WooPosPromoPage4Preview() {
    WooThemeWithBackground {
        WooPosPromoCarouselContent(
            state = WooPosPromoState(currentPage = 3),
            onDismiss = {},
            onNextClick = {},
            onExploreClick = {},
        )
    }
}

@Preview(name = "Page 5 - Light")
@Preview(name = "Page 5 - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WooPosPromoPage5Preview() {
    WooThemeWithBackground {
        WooPosPromoCarouselContent(
            state = WooPosPromoState(currentPage = 4),
            onDismiss = {},
            onNextClick = {},
            onExploreClick = {},
        )
    }
}
