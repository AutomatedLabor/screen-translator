package com.translator.screen.extract

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

/**
 * Robolectric tests for [ScreenTextExtractor].
 *
 * Builds [AccessibilityNodeInfo] trees by hand via [AccessibilityNodeInfo.obtain]
 * and exercises every branch of the extract → dedup → group pipeline.
 *
 * Production constants (mirrored here for assertion clarity):
 *  - MAX_NODES = 500
 *  - LEFT_ALIGN_TOLERANCE_PX = 24
 *  - VERTICAL_OVERLAP_TOLERANCE_PX = -2
 *  - VERTICAL_GAP_LINE_HEIGHT_MULTIPLE = 1.5f
 *  - GROUP_AREA_OVERHEAD_LIMIT = 1.35f
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScreenTextExtractorTest {

    private val extractor = ScreenTextExtractor()

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun makeNode(
        text: CharSequence? = null,
        bounds: Rect = Rect(10, 10, 100, 50),
        visible: Boolean = true,
        password: Boolean = false,
        packageName: CharSequence = "com.test.app",
        className: CharSequence = "android.view.View",
    ): AccessibilityNodeInfo =
        AccessibilityNodeInfo.obtain().apply {
            if (text != null) setText(text)
            setBoundsInScreen(bounds)
            setVisibleToUser(visible)
            setPassword(password)
            setPackageName(packageName)
            setClassName(className)
        }

    private fun makeParent(
        children: List<AccessibilityNodeInfo>,
        bounds: Rect = Rect(0, 0, 400, 400),
        className: CharSequence = "android.view.ViewGroup",
        visible: Boolean = true,
        packageName: CharSequence = "com.test.app",
    ): AccessibilityNodeInfo =
        AccessibilityNodeInfo.obtain().apply {
            setBoundsInScreen(bounds)
            setVisibleToUser(visible)
            setPackageName(packageName)
            setClassName(className)
            // The AccessibilityNodeInfo.addChild(AccessibilityNodeInfo) framework
            // overload is hidden from the SDK 35 stub, so we register children
            // via ShadowAccessibilityNodeInfo's own public addChild(AccessibilityNodeInfo)
            // method, invoked reflectively to dodge Kotlin's overload-resolution
            // picking the addChild(View) @Implementation overload instead.
            val shadow = shadowOf(this)
            val m = shadow.javaClass.getDeclaredMethod(
                "addChild", AccessibilityNodeInfo::class.java,
            )
            m.isAccessible = true
            children.forEach { child -> m.invoke(shadow, child) }
        }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun emptyRootReturnsEmptyList() {
        val root = makeNode(text = null, bounds = Rect(0, 0, 100, 100))
        val result = extractor.extract(root)
        assertThat(result).isEmpty()
    }

    @Test
    fun singleVisibleTextNodeYieldsOneRegion() {
        val root = makeNode(text = "Hello", bounds = Rect(10, 10, 200, 50))
        val result = extractor.extract(root)
        assertThat(result).hasSize(1)
        assertThat(result[0].text).isEqualTo("Hello")
        assertThat(result[0].packageName).isEqualTo("com.test.app")
        assertThat(result[0].bounds).isEqualTo(BoundingBox(10, 10, 200, 50))
    }

    @Test
    fun invisibleParentWithVisibleChildStillExtractsChild() {
        // Parent is invisible but the recursion must descend through it.
        val child = makeNode(text = "Child Text", bounds = Rect(10, 10, 200, 50))
        val parent = makeParent(
            children = listOf(child),
            bounds = Rect(0, 0, 400, 400),
            visible = false,
        )
        val result = extractor.extract(parent)
        assertThat(result).hasSize(1)
        assertThat(result[0].text).isEqualTo("Child Text")
    }

    @Test
    fun passwordNodeIsSkipped() {
        val passwordChild = makeNode(
            text = "secret123",
            bounds = Rect(10, 10, 200, 50),
            password = true,
        )
        val root = makeParent(children = listOf(passwordChild))
        val result = extractor.extract(root)
        assertThat(result).isEmpty()
    }

    @Test
    fun duplicateParentAndChildYieldsExactlyOneRegion() {
        // Parent and child share the same text and the child bounds are
        // contained inside the parent. Dedup keeps the larger (parent) and
        // drops the contained duplicate.
        val parentBounds = Rect(10, 10, 300, 100) // area = 290 * 90 = 26100
        val childBounds = Rect(15, 15, 200, 95)   // area = 185 * 80 = 14800
        val child = makeNode(text = "Paket", bounds = childBounds)
        val parent = makeParent(
            children = listOf(child),
            bounds = parentBounds,
        ).also { it.setText("Paket") }
        val result = extractor.extract(parent)
        assertThat(result).hasSize(1)
        assertThat(result[0].text).isEqualTo("Paket")
    }

    @Test
    fun twoAlignedSiblingsWithSmallGapAreMergedWithNewlineSeparator() {
        // Two children of the same parent, left-aligned (left=10), vertical
        // gap = 10px (< 1.5 * avgLineHeight = 60), union area overhead within
        // 1.35x → merged into one region with "\n" separator.
        val child1 = makeNode(text = "Hallo", bounds = Rect(10, 10, 100, 50))
        val child2 = makeNode(text = "Welt", bounds = Rect(10, 60, 100, 100))
        val parent = makeParent(
            children = listOf(child1, child2),
            bounds = Rect(0, 0, 400, 200),
        )
        val result = extractor.extract(parent)
        assertThat(result).hasSize(1)
        assertThat(result[0].text).isEqualTo("Hallo\nWelt")
        assertThat(result[0].bounds).isEqualTo(BoundingBox(10, 10, 100, 100))
    }

    @Test
    fun twoAlignedSiblingsWithLargeVerticalGapAreNotMerged() {
        // gap = 150px >= 1.5 * avgLineHeight(40) = 60 → not merged.
        val child1 = makeNode(text = "Hallo", bounds = Rect(10, 10, 100, 50))
        val child2 = makeNode(text = "Welt", bounds = Rect(10, 200, 100, 240))
        val parent = makeParent(
            children = listOf(child1, child2),
            bounds = Rect(0, 0, 400, 400),
        )
        val result = extractor.extract(parent)
        assertThat(result).hasSize(2)
        assertThat(result.map { it.text }).containsExactly("Hallo", "Welt")
    }

    @Test
    fun twoSiblingsWithExcessiveUnionAreaOverheadAreNotMerged() {
        // Left-aligned, small vertical gap, but very different widths so the
        // union area overhead exceeds 1.35x the sum → not merged.
        // child1 area = 990 * 40 = 39600
        // child2 area = 10 * 40 = 400
        // sum = 40000; union = 990 * 90 = 89100; 1.35 * sum = 54000 < 89100.
        val child1 = makeNode(text = "Hallo", bounds = Rect(10, 10, 1000, 50))
        val child2 = makeNode(text = "Welt", bounds = Rect(10, 60, 20, 100))
        val parent = makeParent(
            children = listOf(child1, child2),
            bounds = Rect(0, 0, 1100, 200),
        )
        val result = extractor.extract(parent)
        assertThat(result).hasSize(2)
        assertThat(result.map { it.text }).containsExactly("Hallo", "Welt")
    }

    @Test
    fun maxNodesCapLimitsCollectedRegionsTo500() {
        // Build a root with 600 leaf children, each with unique text and
        // unique bounds (non-left-aligned so grouping never merges them).
        // The extractor's MAX_NODES = 500 cap must bound the result.
        val children = (0 until 600).map { i ->
            makeNode(
                text = "Item $i",
                bounds = Rect(i * 30, 0, i * 30 + 20, 30),
            )
        }
        val root = makeParent(
            children = children,
            bounds = Rect(0, 0, 20000, 100),
        )
        val result = extractor.extract(root)
        assertThat(result.size).isAtMost(500)
        assertThat(result).isNotEmpty()
    }
}
