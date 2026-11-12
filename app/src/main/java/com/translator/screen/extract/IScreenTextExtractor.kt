package com.translator.screen.extract

import android.view.accessibility.AccessibilityNodeInfo

/** Walks an [AccessibilityNodeInfo] tree and returns text regions to translate. */
interface IScreenTextExtractor {
    fun extract(root: AccessibilityNodeInfo): List<ScreenTextRegion>
}
