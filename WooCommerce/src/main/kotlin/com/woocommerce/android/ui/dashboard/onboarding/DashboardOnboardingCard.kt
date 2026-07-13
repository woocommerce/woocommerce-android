package com.woocommerce.android.ui.dashboard.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooBadge
import com.woocommerce.android.ui.compose.designsystem.component.WooDivider
import com.woocommerce.android.ui.compose.designsystem.component.WooLinearProgressIndicator
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.dashboard.DashboardFragmentDirections
import com.woocommerce.android.ui.dashboard.DashboardSkeleton
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.ui.dashboard.WidgetError
import com.woocommerce.android.ui.dashboard.onboarding.DashboardOnboardingViewModel.Companion.MAX_NUMBER_OF_TASK_TO_DISPLAY_IN_CARD
import com.woocommerce.android.ui.dashboard.onboarding.DashboardOnboardingViewModel.OnboardingDashBoardState
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.onboarding.AboutYourStoreTaskRes
import com.woocommerce.android.ui.onboarding.LaunchStoreTaskRes
import com.woocommerce.android.ui.onboarding.NavigateToAboutYourStore
import com.woocommerce.android.ui.onboarding.NavigateToAddProduct
import com.woocommerce.android.ui.onboarding.NavigateToLaunchStore
import com.woocommerce.android.ui.onboarding.NavigateToOnboardingFullScreen
import com.woocommerce.android.ui.onboarding.NavigateToSetupPayments
import com.woocommerce.android.ui.onboarding.NavigateToSetupWooPayments
import com.woocommerce.android.ui.onboarding.NavigateToSurvey
import com.woocommerce.android.ui.onboarding.OnboardingTaskUi
import com.woocommerce.android.ui.onboarding.ShowNameYourStoreDialog
import com.woocommerce.android.ui.products.AddProductNavigator
import com.woocommerce.android.viewmodel.MultiLiveEvent

@Composable
fun DashboardOnboardingCard(
    parentViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    onboardingViewModel: DashboardOnboardingViewModel = hiltViewModel(
        creationCallback = { factory: DashboardOnboardingViewModel.Factory ->
            factory.create(parentViewModel)
        }
    )
) {
    onboardingViewModel.viewState.observeAsState().value?.let { onboardingState ->
        WidgetCard(
            titleResource = onboardingState.title,
            menu = onboardingState.menu,
            button = onboardingState.cardButton,
            modifier = modifier,
            isError = onboardingState.isError
        ) {
            when {
                onboardingState.isError -> {
                    WidgetError(
                        onContactSupportClicked = parentViewModel::onContactSupportClicked,
                        onRetryClicked = onboardingViewModel::onRefresh
                    )
                }
                onboardingState.isLoading -> StoreOnboardingLoading()
                else ->
                    StoreOnboardingCardContent(
                        onboardingState = onboardingState,
                        onTaskClicked = onboardingViewModel::onTaskClicked,
                    )
            }
        }
    }
    HandleEvents(
        onboardingViewModel.event,
        onboardingViewModel.addProductNavigator
    )
}

@Composable
private fun HandleEvents(
    event: LiveData<MultiLiveEvent.Event>,
    addProductNavigator: AddProductNavigator
) {
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(event, navController, lifecycleOwner) {
        val observer = Observer { event: MultiLiveEvent.Event ->
            when (event) {
                is NavigateToSurvey ->
                    NavGraphMainDirections.actionGlobalFeedbackSurveyFragment(SurveyType.STORE_ONBOARDING).apply {
                        navController.navigateSafely(this)
                    }

                is NavigateToLaunchStore ->
                    navController.navigateSafely(
                        directions = DashboardFragmentDirections.actionDashboardToLaunchStoreFragment()
                    )

                is NavigateToAddProduct -> {
                    with(addProductNavigator) {
                        navController.navigateToAddProducts(
                            aiBottomSheetAction = DashboardFragmentDirections
                                .actionDashboardToAddProductWithAIBottomSheet(),
                            typesBottomSheetAction = DashboardFragmentDirections
                                .actionDashboardToProductTypesBottomSheet()
                        )
                    }
                }

                is NavigateToSetupPayments ->
                    navController.navigateSafely(
                        directions = DashboardFragmentDirections.actionDashboardToPaymentsPreSetupFragment(
                            taskId = NavigateToSetupPayments.taskId
                        )
                    )

                is NavigateToSetupWooPayments ->
                    navController.navigateSafely(
                        directions = DashboardFragmentDirections.actionDashboardToWooPaymentsSetupInstructionsFragment()
                    )

                is NavigateToAboutYourStore ->
                    navController.navigateSafely(
                        DashboardFragmentDirections.actionDashboardToAboutYourStoreFragment()
                    )

                is ShowNameYourStoreDialog -> {
                    navController.navigateSafely(
                        DashboardFragmentDirections.actionDashboardToNameYourStoreDialogFragment(fromOnboarding = true)
                    )
                }

                is NavigateToOnboardingFullScreen -> {
                    navController.navigateSafely(
                        directions = DashboardFragmentDirections.actionDashboardToOnboardingFragment(),
                    )
                }
            }
        }
        event.observe(lifecycleOwner, observer)
        onDispose {
            event.removeObserver(observer)
        }
    }
}

@Composable
fun StoreOnboardingCardContent(
    onboardingState: OnboardingDashBoardState,
    onTaskClicked: (OnboardingTaskUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(WooTheme.colors.surface.default)
        ) {
            @Suppress("MagicNumber")
            OnboardingCardProgressHeader(
                tasks = onboardingState.tasks,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(horizontal = 16.dp)
            )
            Column(
                modifier = Modifier
                    .padding(top = dimensionResource(id = R.dimen.major_100))
                    .fillMaxWidth()
            ) {
                onboardingState.tasks
                    .filter { it.isCompleted.not() }
                    .take(MAX_NUMBER_OF_TASK_TO_DISPLAY_IN_CARD)
                    .forEachIndexed { index, task ->
                        DashboardOnboardingTaskItem(task, onTaskClicked)
                        if (index < onboardingState.tasks.size - 1) {
                            WooDivider()
                        }
                    }
            }
        }
    }
}

@Composable
fun OnboardingCardProgressHeader(
    tasks: List<OnboardingTaskUi>,
    modifier: Modifier = Modifier
) {
    val completedTasks = tasks.count { it.isCompleted }
    val totalTasks = tasks.count()
    val progress by remember { mutableFloatStateOf(if (totalTasks == 0) 0f else completedTasks / totalTasks.toFloat()) }
    val animatedProgress = animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow,
            visibilityThreshold = 1 / 1000f,
        ),
        label = "OnboardingProgressAnimation"
    ).value
    Column(modifier) {
        WooLinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .height(dimensionResource(id = R.dimen.minor_100))
                .clip(RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))),
        )
        Text(
            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.minor_100)),
            text = stringResource(R.string.store_onboarding_completed_tasks_status, completedTasks, totalTasks),
            style = WooTheme.text.bodyMedium.regular,
            color = WooTheme.colors.surface.onDefault,
        )
    }
}

@Composable
private fun StoreOnboardingLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        DashboardSkeleton(
            modifier = Modifier
                .height(10.dp)
                .width(200.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        DashboardSkeleton(
            modifier = Modifier
                .height(10.dp)
                .width(80.dp)
        )
        repeat(MAX_NUMBER_OF_TASK_TO_DISPLAY_IN_CARD) {
            OnboardingSkeletonItem()
            WooDivider()
        }
    }
}

@Composable
private fun OnboardingSkeletonItem() {
    Row(
        modifier = Modifier
            .padding(top = 16.dp, bottom = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DashboardSkeleton(
            modifier = Modifier
                .height(42.dp)
                .width(42.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            DashboardSkeleton(
                modifier = Modifier
                    .height(12.dp)
                    .width(150.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            DashboardSkeleton(
                modifier = Modifier
                    .height(10.dp)
                    .width(250.dp)
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun OnboardingPreview() = WooDesignSystemThemeWithBackground { OnboardingPreviewContent() }

@Composable
private fun OnboardingPreviewContent() {
    StoreOnboardingCardContent(
        OnboardingDashBoardState(
            title = R.string.store_onboarding_title,
            menu = DashboardWidgetMenu(emptyList()),
            tasks = listOf(
                OnboardingTaskUi(
                    taskUiResources = AboutYourStoreTaskRes,
                    isCompleted = false,
                ),
                OnboardingTaskUi(
                    taskUiResources = AboutYourStoreTaskRes,
                    isCompleted = true,
                ),
                OnboardingTaskUi(
                    taskUiResources = AboutYourStoreTaskRes,
                    isCompleted = false,
                )
            ),
            onViewAllTapped = {}
        ),
        onTaskClicked = {},
    )
}

@Composable
private fun DashboardOnboardingTaskItem(
    task: OnboardingTaskUi,
    onTaskClicked: (OnboardingTaskUi) -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable { onTaskClicked(task) }
            .padding(WooTheme.padding.padding5),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space5),
    ) {
        Image(
            painter = painterResource(id = task.taskUiResources.icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(WooTheme.colors.surface.onVariant),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(
                WooTheme.spacing.space2 + WooTheme.spacing.space1
            ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = task.taskUiResources.title),
                    style = WooTheme.text.titleMedium.strong,
                    color = WooTheme.colors.surface.onDefault,
                )
                if (task.taskUiResources is LaunchStoreTaskRes) {
                    WooBadge(
                        text = stringResource(
                            id = R.string.store_onboarding_launch_store_task_private_tag
                        ).uppercase(),
                        modifier = Modifier.padding(start = WooTheme.padding.padding5),
                    )
                }
            }
            Text(
                text = stringResource(id = task.taskUiResources.description),
                style = WooTheme.text.bodyLarge.regular,
                color = WooTheme.colors.surface.onDefault,
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
        )
    }
}
