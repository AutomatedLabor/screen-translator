package com.translator.screen

import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.translator.screen.extract.ScreenTextExtractor
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented smoke test for [ScreenTextExtractor] against a real view
 * hierarchy rendered inside [FlagSecureTestActivity].
 *
 * The activity mimics a German parcel-tracking screen protected by
 * [android.view.WindowManager.LayoutParams.FLAG_SECURE]. FLAG_SECURE blocks
 * screenshots and MediaProjection but does NOT prevent the accessibility
 * service from traversing the view tree's [android.view.accessibility.AccessibilityNodeInfo]
 * nodes.
 *
 * On a real device with the accessibility service enabled, the full pipeline
 * (AccessibilityService.getRootInActiveWindow → ScreenTextExtractor.extract →
 * MlKitTranslationEngine.translate) is exercised end-to-end. Under
 * instrumented tests without a bound accessibility service, we cannot obtain a
 * root AccessibilityNodeInfo directly, so this test instead walks the live View
 * hierarchy (the same source the framework uses to populate accessibility
 * nodes) and verifies:
 *  1. The activity rendered the expected German strings.
 *  2. The screen contains at least 4 translatable text views.
 *  3. FLAG_SECURE is active on the window.
 *
 * The extractor-on-real-AccessibilityNodeInfo path is verified manually
 * on-device with the accessibility service turned on.
 */
@RunWith(AndroidJUnit4::class)
class ScreenTextExtractorInstrumentedTest {

    @Test
    fun flagSecureActivityRendersGermanTextForExtraction() {
        ActivityScenario.launch(FlagSecureTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Verify FLAG_SECURE is set on the window.
                val flags = activity.window.attributes.flags
                val flagSecure = WindowManager.LayoutParams.FLAG_SECURE
                assertThat(flags and flagSecure).isEqualTo(flagSecure)

                // Walk the view hierarchy and collect every visible TextView's
                // text. The framework generates AccessibilityNodeInfo from this
                // same tree, so any TextView text here is reachable by the
                // extractor on a real device.
                val contentView = activity.findViewById<ViewGroup>(android.R.id.content)
                assertThat(contentView).isNotNull()
                val collected = mutableListOf<String>()
                walkViews(contentView, collected)

                // At least 4 text views should carry German content.
                assertThat(collected.size).isAtLeast(4)

                // Spot-check that the expected German strings are present.
                assertThat(collected).contains("Ihr Paket befindet sich auf dem Weg.")
                assertThat(collected).contains("Absender: Amazon EU Sàrl")
                assertThat(collected).contains("Empfänger: Max Mustermann")

                // The tracking-code line must also be present (it would be
                // filtered out by shouldTranslate, but it still appears in the
                // accessibility tree for the extractor to see).
                assertThat(collected).contains("Sendungsnummer: DHL1234567890")
            }
        }
    }

    @Test
    fun extractorWalksRealAccessibilityTree() {
        // Run the REAL ScreenTextExtractor against a live AccessibilityNodeInfo tree
        // obtained via UiAutomation (the same source the accessibility service uses).
        // This is the closest an instrumented test can get to the on-device pipeline
        // without enabling the accessibility service (which requires ADB + user confirmation).
        ActivityScenario.launch(FlagSecureTestActivity::class.java).use {
            val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
            // Poll for the activity's root node (it may take a few frames to populate).
            var root: AccessibilityNodeInfo? = null
            for (attempt in 1..25) {
                val candidate = uiAutomation.rootInActiveWindow
                if (candidate != null && candidate.packageName == "com.translator.screen") {
                    root = candidate
                    break
                }
                Thread.sleep(200)
            }
            assertThat(root).isNotNull()
            val regions = ScreenTextExtractor().extract(root!!)
            // The activity renders 6 TextViews; dedup/grouping may merge some. At least 3.
            assertThat(regions.size).isAtLeast(3)
            val allText = regions.joinToString("\n") { it.text }
            assertThat(allText).contains("Sendungsnummer")
            assertThat(allText).contains("Paket befindet sich auf dem Weg")
        }
    }

    /** Recursively collect text from every [TextView] under [view]. */
    private fun walkViews(view: View, out: MutableList<String>) {
        if (view is TextView) {
            val text = view.text?.toString().orEmpty()
            if (text.isNotBlank()) out.add(text)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                walkViews(view.getChildAt(i), out)
            }
        }
    }
}
