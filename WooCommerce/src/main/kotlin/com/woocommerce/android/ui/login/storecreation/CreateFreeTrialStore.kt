package com.woocommerce.android.ui.login.storecreation

import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow

class CreateFreeTrialStore @Inject constructor(
    private val repository: StoreCreationRepository
) {
    private val error = MutableStateFlow<StoreCreationErrorType?>(null)
}
