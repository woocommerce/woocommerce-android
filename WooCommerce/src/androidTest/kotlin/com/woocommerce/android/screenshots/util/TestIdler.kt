package com.woocommerce.android.screenshots.util

import androidx.test.espresso.IdlingResource
import androidx.test.espresso.IdlingResource.ResourceCallback
import junit.framework.Assert

abstract class TestIdler : IdlingResource {
    private var mResourceCallback: ResourceCallback? = null
    private var mConditionWasMet = false

    private val mNumberOfTries = 100
    private val mRetryInterval = 100

    override fun getName(): String? {
        return this.javaClass.name
    }

    override fun isIdleNow(): Boolean {
        if (mConditionWasMet) {
            return true
        }
        val isConditionMet = checkCondition()
        if (isConditionMet) {
            mConditionWasMet = true
            mResourceCallback!!.onTransitionToIdle()
        }
        return isConditionMet
    }

    abstract fun checkCondition(): Boolean

    open fun idleUntilReady() {
        idleUntilReady(true)
    }

    open fun idleUntilReady(failIfUnsatisfied: Boolean) {
        var tries = 0
        while (!checkCondition() && ++tries < mNumberOfTries) {
            idle()
        }
        if (tries === mNumberOfTries && failIfUnsatisfied) {
            Assert.fail("Unable to continue – expectation wasn't satisfied quickly enough")
        }
        // Idle one more cycle to allow the UI to settle down
        idle()
    }

    private fun idle() {
        try {
            Thread.sleep(mRetryInterval.toLong())
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun registerIdleTransitionCallback(resourceCallback: ResourceCallback?) {
        mResourceCallback = resourceCallback
    }
}
