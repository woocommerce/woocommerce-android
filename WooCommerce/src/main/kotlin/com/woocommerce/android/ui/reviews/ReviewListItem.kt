package com.woocommerce.android.ui.reviews

import android.widget.RatingBar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.woocommerce.android.R
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.ProductReviewProduct
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.StringUtils
import java.util.Date

@Composable
fun ReviewListItem(
    review: ProductReview,
    onClicked: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    showDivider: Boolean = true,
    titleColor: Color = colorResource(R.color.color_on_surface_high),
    descriptionColor: Color = colorResource(R.color.color_on_surface_medium)
) {
    val layoutDirection = LocalLayoutDirection.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClicked)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(contentPadding)
        ) {
            val isPending = review.status == ProductReviewStatus.HOLD.toString()
            Icon(
                painter = painterResource(id = R.drawable.ic_comment),
                contentDescription = null,
                tint = if (isPending) {
                    colorResource(R.color.woo_purple_60)
                } else {
                    titleColor
                }
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (review.product == null) {
                        stringResource(R.string.product_review_list_item_title, review.reviewerName)
                    } else {
                        stringResource(
                            R.string.review_list_item_title,
                            review.reviewerName,
                            review.product?.name?.fastStripHtml().orEmpty()
                        )
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = titleColor
                )

                val reviewText = buildAnnotatedString {
                    if (isPending) {
                        withStyle(SpanStyle(color = colorResource(id = R.color.woo_orange_50))) {
                            append(stringResource(id = R.string.pending_review_label))
                        }

                        append(" \u2022 ")
                    }

                    append(StringUtils.getRawTextFromHtml(review.review))
                }

                Text(
                    text = reviewText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = descriptionColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (review.rating > 0) {
                    AndroidView(
                        factory = { context ->
                            RatingBar(context, null, androidx.appcompat.R.attr.ratingBarStyleSmall)
                        },
                        update = { ratingBar ->
                            // Set rating higher than numStars to fill all visible stars,
                            // hiding the unfilled star background
                            ratingBar.rating = 100F
                            ratingBar.numStars = review.rating
                        }
                    )
                }
            }
        }

        if (showDivider) {
            val dividerStart = contentPadding.calculateLeftPadding(layoutDirection) + ICON_SIZE + ICON_TEXT_SPACING
            HorizontalDivider(modifier = Modifier.padding(start = dividerStart))
        }
    }
}

@Preview(name = "Approved with product and rating", showBackground = true)
@Composable
private fun ReviewListItemApprovedPreview() {
    WooThemeWithBackground {
        ReviewListItem(
            review = ProductReview(
                remoteId = 1L,
                dateCreated = Date(),
                review = "This product is absolutely wonderful! I would recommend it to everyone.",
                rating = 4,
                reviewerName = "John Doe",
                reviewerAvatarUrl = null,
                remoteProductId = 100L,
                status = ProductReviewStatus.APPROVED.toString(),
                read = true,
                product = ProductReviewProduct(
                    remoteProductId = 100L,
                    name = "Vintage Leather Bag",
                    externalUrl = ""
                )
            ),
            onClicked = {}
        )
    }
}

@Preview(name = "Pending review", showBackground = true)
@Composable
private fun ReviewListItemPendingPreview() {
    WooThemeWithBackground {
        ReviewListItem(
            review = ProductReview(
                remoteId = 2L,
                dateCreated = Date(),
                review = "Still waiting for my order, but the product looks promising.",
                rating = 0,
                reviewerName = "Jane Smith",
                reviewerAvatarUrl = null,
                remoteProductId = 101L,
                status = ProductReviewStatus.HOLD.toString(),
                read = false,
                product = ProductReviewProduct(
                    remoteProductId = 101L,
                    name = "Handmade Candle Set",
                    externalUrl = ""
                )
            ),
            onClicked = {}
        )
    }
}

@Preview(name = "Without product info", showBackground = true)
@Composable
private fun ReviewListItemNoProductPreview() {
    WooThemeWithBackground {
        ReviewListItem(
            review = ProductReview(
                remoteId = 3L,
                dateCreated = Date(),
                review = "Great quality!",
                rating = 5,
                reviewerName = "Alex Turner",
                reviewerAvatarUrl = null,
                remoteProductId = 102L,
                status = ProductReviewStatus.APPROVED.toString(),
                read = true,
                product = null
            ),
            onClicked = {}
        )
    }
}

@Preview(name = "No rating, no divider", showBackground = true)
@Composable
private fun ReviewListItemNoRatingNoDividerPreview() {
    WooThemeWithBackground {
        ReviewListItem(
            review = ProductReview(
                remoteId = 4L,
                dateCreated = Date(),
                review = "Decent product overall.",
                rating = 0,
                reviewerName = "Sam Wilson",
                reviewerAvatarUrl = null,
                remoteProductId = 103L,
                status = ProductReviewStatus.APPROVED.toString(),
                read = true,
                product = ProductReviewProduct(
                    remoteProductId = 103L,
                    name = "Ceramic Mug",
                    externalUrl = ""
                )
            ),
            onClicked = {},
            showDivider = false
        )
    }
}

@Preview(name = "Long review text", showBackground = true)
@Composable
private fun ReviewListItemLongTextPreview() {
    WooThemeWithBackground {
        ReviewListItem(
            review = ProductReview(
                remoteId = 5L,
                dateCreated = Date(),
                review = "I purchased this item last week and I have to say it exceeded all my expectations. " +
                    "The build quality is exceptional and the materials feel premium. " +
                    "I would highly recommend this to anyone looking for a reliable product.",
                rating = 3,
                reviewerName = "Christopher Johnson-Williams",
                reviewerAvatarUrl = null,
                remoteProductId = 104L,
                status = ProductReviewStatus.APPROVED.toString(),
                read = false,
                product = ProductReviewProduct(
                    remoteProductId = 104L,
                    name = "Premium Wireless Bluetooth Noise-Cancelling Headphones",
                    externalUrl = ""
                )
            ),
            onClicked = {}
        )
    }
}

private val ICON_SIZE = 24.dp
private val ICON_TEXT_SPACING = 16.dp
