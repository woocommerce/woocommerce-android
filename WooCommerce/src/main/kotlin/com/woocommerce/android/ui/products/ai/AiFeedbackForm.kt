package com.woocommerce.android.ui.products.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R

@Composable
fun AiFeedbackForm(
    onFeedbackReceived: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                color = colorResource(id = R.color.ai_product_suggestion_box_background),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
            )
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Text(
            text = stringResource(id = R.string.ai_feedback_form_message),
            color = colorResource(id = R.color.color_on_surface_medium),
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = { onFeedbackReceived(true) },
            modifier = Modifier.size(dimensionResource(id = R.dimen.major_200))
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_thumb_up),
                contentDescription = stringResource(id = R.string.ai_feedback_form_positive_button),
                tint = colorResource(id = R.color.color_on_surface_medium)
            )
        }

        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.minor_100)))

        IconButton(
            onClick = { onFeedbackReceived(false) },
            modifier = Modifier.size(dimensionResource(id = R.dimen.major_200))
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_thumb_down),
                contentDescription = stringResource(id = R.string.ai_feedback_form_negative_button),
                tint = colorResource(id = R.color.color_on_surface_medium)
            )
        }
    }
}
