package com.woocommerce.android.ui.login.jetpack.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R

@Composable
fun UserInfo(
    emailOrUsername: String,
    avatarUrl: String,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(
            dimensionResource(id = R.dimen.major_100),
            Alignment.Start
        ),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .border(1.dp, color = colorResource(id = R.color.divider_color), shape = MaterialTheme.shapes.medium)
            .semantics(mergeDescendants = true) {}
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarUrl)
                .crossfade(true)
                .placeholder(R.drawable.img_gravatar_placeholder)
                .error(R.drawable.img_gravatar_placeholder)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.image_minor_100))
                .clip(CircleShape)
        )
        Text(
            text = emailOrUsername,
            style = MaterialTheme.typography.subtitle1
        )
    }
}
