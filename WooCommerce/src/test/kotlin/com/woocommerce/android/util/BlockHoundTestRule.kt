package com.woocommerce.android.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.debug.CoroutinesBlockHoundIntegration
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import reactor.blockhound.BlockHound

@ExperimentalCoroutinesApi
class BlockHoundTestRule : TestWatcher() {
    override fun starting(description: Description) {
        super.starting(description)
        BlockHound.install(CoroutinesBlockHoundIntegration())
    }
}
