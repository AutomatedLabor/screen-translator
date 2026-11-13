package com.translator.screen.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Helpers for inspecting the installed package set. Currently used (Phase 2A)
 * to filter out launcher/home-screen packages so the translator does not try
 * to translate the launcher itself.
 */
object PackageUtils {

    /**
     * Returns `true` if [pkg] is the resolved default home (launcher) package
     * on this device. Phase 2A uses this to short-circuit translation when the
     * user is sitting on the home screen.
     */
    fun isLauncherPackage(context: Context, pkg: String): Boolean {
        resolveLauncherPackage(context)?.let { return it == pkg } ?: return false
    }

    /**
     * Returns the resolved default home (launcher) package name, or null if none.
     * Callers that check multiple windows should cache this once per pick rather
     * than calling [isLauncherPackage] N times (each call does a synchronous
     * PackageManager.resolveActivity binder IPC). (Fresh-eyes review P1-15.)
     */
    fun resolveLauncherPackage(context: Context): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY,
        )
        return resolveInfo?.activityInfo?.packageName
    }
}
