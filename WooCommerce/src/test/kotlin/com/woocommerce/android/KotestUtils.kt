package com.woocommerce.android

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import com.woocommerce.android.util.CoroutineDispatchers
import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@ExperimentalCoroutinesApi
class CoroutinesTestExtension(
    val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
) : BeforeSpecListener, AfterSpecListener, TestCoroutineScope by TestCoroutineScope(testDispatcher) {
    val testDispatchers = CoroutineDispatchers(testDispatcher, testDispatcher, testDispatcher)

    override suspend fun beforeSpec(spec: Spec) {
        Dispatchers.setMain(testDispatcher)
    }

    override suspend fun afterSpec(spec: Spec) {
        cleanupTestCoroutines()
        Dispatchers.resetMain()
    }
}

//Add @AutoScan to automatically register this extension for all Kotest tests
class InstantExecutorExtension : BeforeSpecListener, AfterSpecListener {
    override suspend fun beforeSpec(spec: Spec) {
        ArchTaskExecutor.getInstance()
            .setDelegate(object : TaskExecutor() {
                override fun executeOnDiskIO(runnable: Runnable) = runnable.run()

                override fun postToMainThread(runnable: Runnable) = runnable.run()

                override fun isMainThread(): Boolean = true
            })
    }

    override suspend fun afterSpec(spec: Spec) {
        ArchTaskExecutor.getInstance().setDelegate(null)
    }
}

// To enable coroutine test dispatcher at project level
//class ProjectConfig : AbstractProjectConfig() {
//    override val testCoroutineDispatcher = true
//}

//fun testBlocking(block: suspend TestCoroutineScope.() -> Unit) =
//    TestCoroutineDispatcher().runBlockingTest {
//        block()
//    }

