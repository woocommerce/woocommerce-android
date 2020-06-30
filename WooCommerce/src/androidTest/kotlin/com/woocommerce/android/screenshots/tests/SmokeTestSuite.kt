package com.woocommerce.android.screenshots.tests

import androidx.test.filters.LargeTest
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses

@RunWith(Suite::class)
@SuiteClasses(LoginTestSuite::class, OrderTestSuite::class)
@LargeTest
class SmokeTestSuite
