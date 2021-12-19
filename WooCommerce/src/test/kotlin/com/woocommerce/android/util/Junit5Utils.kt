package com.woocommerce.android.util

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.extension.*

@ExperimentalCoroutinesApi
class CoroutinesTestExtension(
    val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
) : BeforeEachCallback, AfterEachCallback, TestCoroutineScope by TestCoroutineScope(testDispatcher) {
    val testDispatchers = CoroutineDispatchers(testDispatcher, testDispatcher, testDispatcher)

    override fun beforeEach(context: ExtensionContext?) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun afterEach(context: ExtensionContext?) {
        cleanupTestCoroutines()
        Dispatchers.resetMain()
    }
}

class InstantExecutorExtension : BeforeEachCallback, AfterEachCallback {
    override fun beforeEach(context: ExtensionContext?) {
        ArchTaskExecutor.getInstance()
            .setDelegate(object : TaskExecutor() {
                override fun executeOnDiskIO(runnable: Runnable) = runnable.run()

                override fun postToMainThread(runnable: Runnable) = runnable.run()

                override fun isMainThread(): Boolean = true
            })
    }

    override fun afterEach(context: ExtensionContext?) {
        ArchTaskExecutor.getInstance().setDelegate(null)
    }
}

@ExperimentalCoroutinesApi
@ExtendWith(InstantExecutorExtension::class)
open class BaseJunit5Test {
    @RegisterExtension @JvmField
    val coroutinesTestExtension = CoroutinesTestExtension()

    @ExperimentalCoroutinesApi
    protected fun testBlocking(block: suspend TestCoroutineScope.() -> Unit) =
        coroutinesTestExtension.testDispatcher.runBlockingTest {
            block()
        }
}
