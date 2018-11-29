package com.woocommerce.android.util

import android.app.AppOpsManager
import android.content.Context
import com.woocommerce.android.util.WooLog.T
import java.lang.reflect.InvocationTargetException

object NotificationsUtils {
    private const val CHECK_OP_NO_THROW = "checkOpNoThrow"
    private const val OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION"

    /**
     * Checks if global notifications toggle is enabled in the Android app settings
     * See: https://code.google.com/p/android/issues/detail?id=38482#c15
     */
    fun isNotificationsEnabled(context: Context): Boolean {
        val mAppOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val appInfo = context.applicationInfo
        val pkg = context.applicationContext.packageName
        val uid = appInfo.uid

        val appOpsClass: Class<*>
        try {
            appOpsClass = Class.forName(AppOpsManager::class.java.name)

            val checkOpNoThrowMethod = appOpsClass.getMethod(
                    CHECK_OP_NO_THROW,
                    Integer.TYPE,
                    Integer.TYPE,
                    String::class.java
            )

            val opPostNotificationValue = appOpsClass.getDeclaredField(OP_POST_NOTIFICATION)
            val value = opPostNotificationValue.get(Int::class.java) as Int

            return checkOpNoThrowMethod.invoke(mAppOps, value, uid, pkg) as Int == AppOpsManager.MODE_ALLOWED
        } catch (e: ClassNotFoundException) {
            WooLog.e(T.NOTIFS, e.message ?: "")
        } catch (e: NoSuchFieldException) {
            WooLog.e(T.NOTIFS, e.message ?: "")
        } catch (e: NoSuchMethodException) {
            WooLog.e(T.NOTIFS, e.message ?: "")
        } catch (e: IllegalAccessException) {
            WooLog.e(T.NOTIFS, e.message ?: "")
        } catch (e: InvocationTargetException) {
            WooLog.e(T.NOTIFS, e.message ?: "")
        }

        // Default to assuming notifications are enabled
        return true
    }
}
