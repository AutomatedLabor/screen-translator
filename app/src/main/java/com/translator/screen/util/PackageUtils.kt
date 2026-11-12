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
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY,
        )
        return resolveInfo?.activityInfo?.packageName == pkg
    }
}
