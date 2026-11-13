package com.translator.screen.extract

import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.abs

/**
 * Default [IScreenTextExtractor].
 *
 * Walks the accessibility tree depth-first, collects text-bearing nodes,
 * removes duplicates (parent/child text redundancy), and merges vertically
 * stacked, left-aligned siblings into single logical lines.
 */
class ScreenTextExtractor : IScreenTextExtractor {

    override fun extract(root: AccessibilityNodeInfo): List<ScreenTextRegion> {
        val collected = mutableListOf<ScreenTextRegion>()
        val visited = IntArray(1)
        walk(root, 0, null, collected, visited)
        Log.i(TAG, "Visited ${visited[0]} nodes; collected ${collected.size} raw regions")
        val deduped = deduplicate(collected)
        Log.i(TAG, "After dedup: ${deduped.size} regions")
        val grouped = groupLines(deduped)
        Log.i(TAG, "After grouping: ${grouped.size} regions")
        return grouped.sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))
    }

    private fun walk(
        node: AccessibilityNodeInfo,
        depth: Int,
        parent: AccessibilityNodeInfo?,
        collected: MutableList<ScreenTextRegion>,
        visited: IntArray,
    ) {
        if (depth > MAX_DEPTH) {
            // Root is owned by the caller; only recycle nodes obtained via getChild.
            if (depth > 0) recycleIfLegacy(node)
            return
        }
        if (visited[0] >= MAX_NODES) {
            if (depth > 0) recycleIfLegacy(node)
            return
        }
        visited[0]++

        // Password fields are skipped entirely: no collection and no descent,
        // so sensitive input never propagates to translation.
        if (!node.isPassword) {
            // Skip this node's own text collection when invisible, but still
            // descend because compose/material trees sometimes mark parents
            // invisible while their children remain visible.
            if (node.isVisibleToUser) {
                collectIfValid(node, parent, collected)
            }
            val childCount = node.childCount
            for (i in 0 until childCount) {
                val child = node.getChild(i) ?: continue
                walk(child, depth + 1, node, collected, visited)
            }
        }

        if (depth > 0) recycleIfLegacy(node)
    }

    private fun collectIfValid(
        node: AccessibilityNodeInfo,
        parent: AccessibilityNodeInfo?,
        collected: MutableList<ScreenTextRegion>,
    ) {
        val text = pickText(node) ?: return
        val rect = Rect()
        node.getBoundsInScreen(rect)
        // Skip empty boxes and boxes fully off the top/left half of the screen.
        if (rect.width() <= 0 || rect.height() <= 0) return
        if (rect.right <= 0 || rect.bottom <= 0) return
        collected.add(
            ScreenTextRegion(
                text = text,
                bounds = BoundingBox(rect.left, rect.top, rect.right, rect.bottom),
                packageName = node.packageName?.toString().orEmpty(),
                parentSignature = parent?.let { signatureOf(it) },
            )
        )
    }

    private fun pickText(node: AccessibilityNodeInfo): String? {
        // Order matters: prefer explicit text, then contentDescription, then hint.
        // AccessibilityNodeInfo.hintText requires API 26 (Android 8); on API 24-25 it
        // would NoSuchMethodError, so guard it. (v1.4.0: minSdk 24 -> Android 7+.)
        val candidates = mutableListOf<CharSequence?>().apply {
            add(node.text)
            add(node.contentDescription)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) add(node.hintText)
        }
        for (candidate in candidates) {
            if (candidate.isNullOrBlank()) continue
            val trimmed = candidate.toString().trim()
            if (trimmed.length >= 2) return trimmed
        }
        return null
    }

    private fun deduplicate(regions: List<ScreenTextRegion>): List<ScreenTextRegion> {
        if (regions.isEmpty()) return regions
        // Largest first so containers are accepted before their children.
        val sorted = regions.sortedByDescending { it.bounds.area }
        val accepted = mutableListOf<ScreenTextRegion>()
        for (region in sorted) {
            val isDuplicate = accepted.any { acceptedRegion ->
                val textEqualIgnoreCaseTrimmed =
                    region.text.trim().equals(acceptedRegion.text.trim(), ignoreCase = true)
                val textIdentical = region.text == acceptedRegion.text
                val contained = acceptedRegion.bounds.contains(region.bounds)
                val overlap80 = region.bounds.overlapsByFraction(acceptedRegion.bounds, 0.8f)
                (contained && textEqualIgnoreCaseTrimmed) || (textIdentical && overlap80)
            }
            if (!isDuplicate) accepted.add(region)
        }
        return accepted
    }

    private fun groupLines(regions: List<ScreenTextRegion>): List<ScreenTextRegion> {
        if (regions.size < 2) return regions
        val result = regions.toMutableList()
        var mergedAny = true
        while (mergedAny) {
            mergedAny = false
            // Deterministic ordering: top-to-bottom, then left-to-right.
            result.sortWith(compareBy({ it.bounds.top }, { it.bounds.left }))
            var mergedRegion: ScreenTextRegion? = null
            var mergeI = -1
            var mergeJ = -1
            outer@ for (i in result.indices) {
                for (j in i + 1 until result.size) {
                    if (canMerge(result[i], result[j])) {
                        mergeI = i
                        mergeJ = j
                        mergedRegion = mergePair(result[i], result[j])
                        break@outer
                    }
                }
            }
            if (mergedRegion != null) {
                result.removeAt(mergeJ)
                result.removeAt(mergeI)
                result.add(mergedRegion)
                mergedAny = true
            }
        }
        return result
    }

    private fun canMerge(a: ScreenTextRegion, b: ScreenTextRegion): Boolean {
        val sig = a.parentSignature ?: return false
        if (sig != b.parentSignature) return false
        val ab = a.bounds
        val bb = b.bounds
        if (ab.isEmpty || bb.isEmpty) return false
        if (abs(ab.left - bb.left) > LEFT_ALIGN_TOLERANCE_PX) return false
        // Caller sorts by top, so a.top <= b.top -> a is the upper region.
        val gap = bb.top - ab.bottom
        if (gap < VERTICAL_OVERLAP_TOLERANCE_PX) return false
        val avgLineHeight = (ab.height + bb.height) / 2f
        if (avgLineHeight <= 0f) return false
        if (gap >= VERTICAL_GAP_LINE_HEIGHT_MULTIPLE * avgLineHeight) return false
        val sumArea = ab.area + bb.area
        if (sumArea <= 0) return false
        if (ab.union(bb).area > GROUP_AREA_OVERHEAD_LIMIT * sumArea) return false
        return true
    }

    private fun mergePair(a: ScreenTextRegion, b: ScreenTextRegion): ScreenTextRegion =
        ScreenTextRegion(
            text = a.text + "\n" + b.text,
            bounds = a.bounds.union(b.bounds),
            packageName = a.packageName,
            parentSignature = a.parentSignature,
        )

    private fun signatureOf(node: AccessibilityNodeInfo): String {
        val r = Rect()
        node.getBoundsInScreen(r)
        return "${node.className ?: "?"}|${r.left},${r.top},${r.right},${r.bottom}|${node.childCount}"
    }

    private fun recycleIfLegacy(node: AccessibilityNodeInfo) {
        if (Build.VERSION.SDK_INT < 33) {
            node.recycle()
        }
    }

    companion object {
        private const val TAG = "ScreenTextExtractor"
        const val MAX_DEPTH = 30
        const val MAX_NODES = 500
        const val LEFT_ALIGN_TOLERANCE_PX = 24
        const val VERTICAL_OVERLAP_TOLERANCE_PX = -2
        const val VERTICAL_GAP_LINE_HEIGHT_MULTIPLE = 1.5f
        const val GROUP_AREA_OVERHEAD_LIMIT = 1.35f
    }
}
