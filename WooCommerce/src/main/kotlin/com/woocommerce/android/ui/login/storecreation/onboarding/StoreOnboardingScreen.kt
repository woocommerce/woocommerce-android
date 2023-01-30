package com.woocommerce.android.ui.login.storecreation.onboarding

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R

@Composable
fun StoreOnboardingScreen(
    onboardingState: StoreOnboardingViewModel.OnboardingState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Text(
            text = stringResource(id = onboardingState.title),
            style = MaterialTheme.typography.h6,
        )
        OnboardingTaskList(
            tasks = onboardingState.tasks,
            modifier = Modifier
                .padding(top = dimensionResource(id = R.dimen.major_100))
                .fillMaxWidth()
        )
    }
}

@Composable
fun OnboardingTaskList(
    tasks: List<StoreOnboardingViewModel.OnboardingTask>,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        tasks.forEachIndexed { index, task ->
            Row(
                modifier = modifier.padding(bottom = dimensionResource(id = R.dimen.major_100)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100))
            ) {
                Image(
                    modifier = Modifier.fillMaxHeight(),
                    painter = painterResource(id = task.icon),
                    contentDescription = "",
                    colorFilter = ColorFilter.tint(color = colorResource(id = R.color.color_icon))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = task.title),
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        modifier = Modifier.padding(top = dimensionResource(id = R.dimen.minor_75)),
                        text = stringResource(id = task.description),
                        style = MaterialTheme.typography.body1,
                    )
                }
                Image(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = ""
                )
            }
            if (index < tasks.lastIndex)
                Divider(
                    color = colorResource(id = R.color.divider_color),
                    thickness = dimensionResource(id = R.dimen.minor_10)
                )
        }
    }
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
@Composable
private fun OnboardingPreview() {
    StoreOnboardingScreen(
        StoreOnboardingViewModel.OnboardingState(
            show = true,
            title = R.string.store_onboarding_title,
            tasks = listOf(
                StoreOnboardingViewModel.OnboardingTask(
                    icon = R.drawable.ic_product,
                    title = R.string.store_onboarding_task_add_product_title,
                    description = R.string.store_onboarding_task_add_product_description,
                    status = StoreOnboardingViewModel.OnboardingTaskStatus.UNDONE
                ),
                StoreOnboardingViewModel.OnboardingTask(
                    icon = R.drawable.ic_product,
                    title = R.string.store_onboarding_task_launch_store_title,
                    description = R.string.store_onboarding_task_launch_store_description,
                    status = StoreOnboardingViewModel.OnboardingTaskStatus.UNDONE
                ),
                StoreOnboardingViewModel.OnboardingTask(
                    icon = R.drawable.ic_product,
                    title = R.string.store_onboarding_task_change_domain_title,
                    description = R.string.store_onboarding_task_change_domain_description,
                    status = StoreOnboardingViewModel.OnboardingTaskStatus.UNDONE
                )
            )
        )
    )
}
