package com.woocommerce.android.util

import org.mockito.internal.stubbing.answers.Returns
import org.mockito.internal.util.KotlinInlineClassUtil
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

/**
 * A utility Mockito [Answer] to support kotlin inline classes.
 * It uses internally the same functionality that Mockito uses for supporting inline classes when using [Returns]
 *
 * check https://github.com/mockito/mockito/pull/2280
 */
class InlineClassesAnswer<T : Any>(private val defaultAnswer: Answer<T>) : Answer<T> {
    override fun answer(invocation: InvocationOnMock?): T {
        return KotlinInlineClassUtil.unboxUnderlyingValueIfNeeded(invocation, defaultAnswer.answer(invocation)) as T
    }
}
