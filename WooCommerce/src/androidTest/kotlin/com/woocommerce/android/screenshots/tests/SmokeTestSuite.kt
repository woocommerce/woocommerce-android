package com.woocommerce.android.screenshots.tests

import androidx.test.filters.LargeTest
import com.woocommerce.android.screenshots.util.rules.RetryTest.Retry
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses

@RunWith(Suite::class)
@SuiteClasses(LoginTestSuite::class, OrderTestSuite::class, ProductTestSuite::class)
@LargeTest
class SmokeTestSuite

//@Rule
//var retry: Retry = Retry(3)
