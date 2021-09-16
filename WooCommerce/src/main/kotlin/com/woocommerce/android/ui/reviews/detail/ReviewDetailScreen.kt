package com.woocommerce.android.ui.reviews.detail

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import coil.compose.rememberImagePainter
import coil.transform.CircleCropTransformation
import com.google.android.material.composethemeadapter.MdcTheme
import com.gowtham.ratingbar.RatingBar
import com.woocommerce.android.R
import com.woocommerce.android.databinding.SkeletonNotifDetailBinding
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.ProductReviewProduct
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.common.compose.elements.HtmlText
import com.woocommerce.android.ui.reviews.ProductReviewStatus
import com.woocommerce.android.util.ChromeCustomTabUtils
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.UrlUtils
import java.util.Date

@Composable
fun ReviewDetailScreen(viewModel: ReviewDetailViewModel, productImageMap: ProductImageMap) {
    val uiState by viewModel.uiState.collectAsState()

    val product = uiState.productReview?.product
    val productImage = if (product != null) {
        productImageMap.get(product.remoteProductId)?.run {
            val context = LocalContext.current
            val size = remember { DisplayUtils.dpToPx(context, 32) }
            PhotonUtils.getPhotonImageUrl(this, size, size)
        }
    } else {
        null
    }

    ReviewDetailScreen(
        uiState,
        productImage,
        { viewModel.moderateReview(ProductReviewStatus.TRASH) },
        { viewModel.moderateReview(ProductReviewStatus.SPAM) },
        { approved ->
            viewModel.moderateReview(
                if (approved) {
                    ProductReviewStatus.HOLD
                } else {
                    ProductReviewStatus.APPROVED
                }
            )
        },
    )
}

@Composable
fun ReviewDetailScreen(
    uiState: ReviewDetailViewModel.ViewState,
    productImage: String?,
    onTrashClicked: () -> Unit,
    onSpamClicked: () -> Unit,
    onAproveClicked: (Boolean) -> Unit,
) {
    MdcTheme(
        setTextColors = true,
        setDefaultFontFamily = true,
    ) {
        if (uiState.isSkeletonShown == true) {
            ReviewDetailSkeleton()
        } else {
            ReviewDetailCard(
                uiState.productReview!!,
                uiState.enableModeration,
                uiState.reviewApproved,
                productImage,
                onTrashClicked,
                onSpamClicked,
                onAproveClicked,
            )
        }
    }
}

@Composable
private fun ReviewDetailSkeleton() {
    AndroidViewBinding(SkeletonNotifDetailBinding::inflate)
}

@Composable
private fun ReviewDetailCard(
    productReview: ProductReview,
    enableModeration: Boolean,
    reviewApproved: Boolean,
    productImage: String?,
    onTrashClicked: () -> Unit,
    onSpamClicked: () -> Unit,
    onAproveClicked: (Boolean) -> Unit,
) {
    Box(modifier = Modifier.background(MaterialTheme.colors.background)) {
        Card(
            backgroundColor = MaterialTheme.colors.surface,
            elevation = 2.dp,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ReviewHeader(productReview.product!!, productImage)
                Divider(color = colorResource(R.color.divider_color))
                Spacer(modifier = Modifier.height(12.dp))
                ReviewContent(productReview)
                Spacer(modifier = Modifier.height(8.dp))
                if (enableModeration) {
                    ReviewButtons(
                        reviewApproved,
                        onTrashClicked,
                        onSpamClicked,
                        onAproveClicked,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewHeader(product: ProductReviewProduct, productImage: String?) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { ChromeCustomTabUtils.launchUrl(context, product.externalUrl) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Image(
            painter = rememberImagePainter(productImage ?: "") {
                placeholder(R.drawable.ic_product)
            },
            contentDescription = stringResource(R.string.product_image_content_description),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = product.name.fastStripHtml().orEmpty(),
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.weight(1f),
        )
        Image(
            painter = painterResource(R.drawable.ic_external),
            contentDescription = stringResource(R.string.wc_view_the_product_external),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Composable
private fun ReviewContent(review: ProductReview) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
    {
        ReviewUserInfo(review)
        Spacer(modifier = Modifier.height(16.dp))
        RatingBar(
            numStars = 5,
            value = review.rating.toFloat(),
            isIndicator = true,
            onRatingChanged = {},
            padding = 0.dp,
            onValueChange = {},
            activeColor = colorResource(R.color.rating_star_color),
            inactiveColor = colorResource(R.color.woo_gray_5),
            modifier = Modifier.height(16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        HtmlText(
            html = review.review,
            style = R.style.Woo_TextView_Body1,
        )
    }
}

@Composable
private fun ReviewUserInfo(review: ProductReview) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val avatarUrl = UrlUtils.removeQuery(review.reviewerAvatarUrl) + "?s=64&d=404"
        Image(
            painter = rememberImagePainter(avatarUrl) {
                placeholder(R.drawable.ic_user_circle_24dp)
                transformations(CircleCropTransformation())
            },
            contentDescription = null,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column() {
            Text(
                text = review.reviewerName,
                style = MaterialTheme.typography.h6,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = DateTimeUtils.javaDateToTimeSpan(review.dateCreated, LocalContext.current),
                style = MaterialTheme.typography.body2,
                color = colorResource(R.color.color_on_surface_medium),
            )
        }
    }
}

@Composable
private fun ReviewButtons(
    reviewApproved: Boolean,
    onTrashClicked: () -> Unit,
    onSpamClicked: () -> Unit,
    onAproveClicked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = onTrashClicked,
        ) {
            Text(
                text = stringResource(R.string.wc_trash),
                color = colorResource(R.color.color_secondary),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(
            onClick = onSpamClicked,
        ) {
            Text(
                text = stringResource(R.string.wc_spam),
                color = colorResource(R.color.color_secondary)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(
            onClick = { onAproveClicked(reviewApproved) },
        ) {
            Text(
                text = stringResource(if (reviewApproved) R.string.wc_approved else R.string.wc_approve),
                color = colorResource(if (reviewApproved) R.color.woo_green_5 else R.color.color_secondary)
            )
        }
    }
}

// region Preview
@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun PreviewReviewDetailScreen() {
    ReviewDetailScreen(
        ReviewDetailViewModel.ViewState(
            productReview = ProductReview(
                remoteId = 1L,
                dateCreated = Date(),
                review = "all nice bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla",
                rating = 3,
                reviewerName = "Andrey",
                reviewerAvatarUrl = "https://google.com",
                remoteProductId = 5L,
                status = "Done",
                read = false,
                product = ProductReviewProduct(
                    remoteProductId = 5,
                    name = "Good Product",
                    externalUrl = "url",
                )
            ),
            enableModeration = true,
            reviewApproved = false
        ),
        productImage = null,
        {},
        {},
        {},
    )
}

@Preview(name = "Skeleton")
@Composable
fun PreviewReviewDetailScreenSkeleton() {
    ReviewDetailScreen(
        ReviewDetailViewModel.ViewState(
            isSkeletonShown = true
        ),
        productImage = null,
        {},
        {},
        {},
    )
}
// endregion
