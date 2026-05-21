package com.woocommerce.android.ui.prefs.notifications

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.prefs.compose.SettingsSectionHeader
import com.woocommerce.android.ui.prefs.notifications.NotificationSettingsSharedViewModel.Companion.MAX_REVIEW_RATING
import com.woocommerce.android.ui.prefs.notifications.NotificationSettingsSharedViewModel.Companion.MIN_REVIEW_RATING
import com.woocommerce.android.ui.prefs.notifications.compose.EnableNotificationsCard
import com.woocommerce.android.ui.prefs.notifications.compose.NotificationPreferenceOption

@Composable
fun NewReviewNotificationSettingsScreen(sharedViewModel: NotificationSettingsSharedViewModel) {
    sharedViewModel.newReviewNotificationSettingsViewState.observeAsState().value?.let { viewState ->
        NewReviewNotificationSettingsScreen(
            viewState = viewState,
            onNotificationsEnabledChanged = sharedViewModel::onNewReviewNotificationsEnabledChanged,
            onNotificationPreferenceChanged = sharedViewModel::onNewReviewNotificationPreferenceChanged,
            onSelectedRatingChanged = sharedViewModel::onNewReviewSelectedRatingChanged
        )
    }
}

@Composable
private fun NewReviewNotificationSettingsScreen(
    viewState: NotificationSettingsSharedViewModel.NewReviewNotificationSettingsViewState,
    onNotificationsEnabledChanged: (Boolean) -> Unit,
    onNotificationPreferenceChanged: (NotificationSettingsSharedViewModel.NewReviewNotificationPreference) -> Unit,
    onSelectedRatingChanged: (Int) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            EnableNotificationsCard(
                title = stringResource(R.string.settings_notifs_enable_title),
                description = stringResource(R.string.settings_notifs_new_reviews_enable_description),
                isEnabled = viewState.notificationsEnabled,
                onEnabledChanged = onNotificationsEnabledChanged
            )
            SettingsSectionHeader(
                text = stringResource(R.string.settings_notifs_notify_me_for),
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp)
            )
            NotificationPreferenceOption(
                title = stringResource(R.string.settings_notifs_new_reviews_all_title),
                description = stringResource(R.string.settings_notifs_new_reviews_all_description),
                selected = viewState.notificationPreference ==
                    NotificationSettingsSharedViewModel.NewReviewNotificationPreference.AllReviews,
                enabled = viewState.notificationsEnabled,
                onClick = {
                    onNotificationPreferenceChanged(
                        NotificationSettingsSharedViewModel.NewReviewNotificationPreference.AllReviews
                    )
                }
            )
            val isRatingFilterSelected =
                viewState.notificationPreference ==
                    NotificationSettingsSharedViewModel.NewReviewNotificationPreference.RatingFilteredReviews
            NotificationPreferenceOption(
                title = stringResource(R.string.settings_notifs_new_reviews_rating_filter_title),
                description = stringResource(R.string.settings_notifs_new_reviews_rating_filter_description),
                selected = isRatingFilterSelected,
                enabled = viewState.notificationsEnabled,
                onClick = {
                    onNotificationPreferenceChanged(
                        NotificationSettingsSharedViewModel.NewReviewNotificationPreference.RatingFilteredReviews
                    )
                }
            )
            AnimatedVisibility(visible = isRatingFilterSelected) {
                RatingSelector(
                    selectedRating = viewState.selectedRating,
                    enabled = viewState.notificationsEnabled,
                    onRatingSelected = onSelectedRatingChanged,
                    modifier = Modifier.padding(start = 64.dp, top = 20.dp, end = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun RatingSelector(
    selectedRating: Int,
    enabled: Boolean,
    onRatingSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = pluralStringResource(
                id = R.plurals.settings_notifs_new_reviews_selected_rating,
                count = selectedRating,
                selectedRating
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary.let {
                if (enabled) it else it.copy(alpha = 0.38f)
            }
        )
        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (rating in MIN_REVIEW_RATING..MAX_REVIEW_RATING) {
                RatingStar(
                    rating = rating,
                    isSelected = rating <= selectedRating,
                    enabled = enabled,
                    onClick = { onRatingSelected(rating) }
                )
            }
        }
    }
}

@Composable
private fun RatingStar(
    rating: Int,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(
                id = if (isSelected) R.drawable.ic_star_filled_24dp else R.drawable.ic_star_24dp
            ),
            contentDescription = pluralStringResource(
                id = R.plurals.settings_notifs_new_reviews_selected_rating,
                count = rating,
                rating
            ),
            tint = ratingStarColor(isSelected = isSelected, enabled = enabled),
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
private fun ratingStarColor(isSelected: Boolean, enabled: Boolean): Color {
    return when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        isSelected -> colorResource(id = R.color.rating_star_color)
        else -> MaterialTheme.colorScheme.outline
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NewReviewNotificationSettingsScreenPreview() {
    WooThemeWithBackground {
        val ratingFilteredReviews =
            NotificationSettingsSharedViewModel.NewReviewNotificationPreference.RatingFilteredReviews
        NewReviewNotificationSettingsScreen(
            viewState = NotificationSettingsSharedViewModel.NewReviewNotificationSettingsViewState(
                notificationPreference = ratingFilteredReviews
            ),
            onNotificationsEnabledChanged = {},
            onNotificationPreferenceChanged = {},
            onSelectedRatingChanged = {}
        )
    }
}
