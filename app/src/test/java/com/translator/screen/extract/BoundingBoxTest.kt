package com.translator.screen.extract

import android.graphics.Rect
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * JVM unit tests for [BoundingBox] geometry.
 *
 * Covers: dimensions, area, center, emptiness, strict containment, union,
 * overlap area, fraction-based overlap, and Rect round-tripping.
 *
 * Uses Robolectric so that `android.graphics.Rect` field initialisation is
 * honoured (the plain SDK stub leaves all int fields at 0).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BoundingBoxTest {

    @Test
    fun dimensionsAndAreaForNormalBox() {
        val box = BoundingBox(left = 10, top = 20, right = 110, bottom = 70)
        assertThat(box.width).isEqualTo(100)
        assertThat(box.height).isEqualTo(50)
        assertThat(box.area).isEqualTo(5000)
        assertThat(box.isEmpty).isFalse()
        assertThat(box.centerX).isEqualTo(60) // (10 + 110) / 2
        assertThat(box.centerY).isEqualTo(45) // (20 + 70) / 2
    }

    @Test
    fun emptyBoxHasZeroArea() {
        val zeroWidth = BoundingBox(10, 10, 10, 50)
        assertThat(zeroWidth.isEmpty).isTrue()
        assertThat(zeroWidth.area).isEqualTo(0)

        val zeroHeight = BoundingBox(10, 10, 50, 10)
        assertThat(zeroHeight.isEmpty).isTrue()
        assertThat(zeroHeight.area).isEqualTo(0)

        val negative = BoundingBox(50, 50, 10, 10)
        assertThat(negative.isEmpty).isTrue()
        assertThat(negative.area).isEqualTo(0)
    }

    @Test
    fun emptyConstantIsConsideredEmpty() {
        assertThat(BoundingBox.EMPTY.isEmpty).isTrue()
        assertThat(BoundingBox.EMPTY.area).isEqualTo(0)
    }

    @Test
    fun containsTrueForFullyContainedBox() {
        val outer = BoundingBox(0, 0, 100, 100)
        val inner = BoundingBox(10, 10, 90, 90)
        assertThat(outer.contains(inner)).isTrue()
    }

    @Test
    fun containsFalseForPartiallyOverlappingBox() {
        val a = BoundingBox(0, 0, 100, 100)
        val b = BoundingBox(50, 50, 150, 150)
        assertThat(a.contains(b)).isFalse()
        assertThat(b.contains(a)).isFalse()
    }

    @Test
    fun containsFalseForDisjointBox() {
        val a = BoundingBox(0, 0, 50, 50)
        val b = BoundingBox(100, 100, 150, 150)
        assertThat(a.contains(b)).isFalse()
        assertThat(b.contains(a)).isFalse()
    }

    @Test
    fun containsFalseWhenEitherBoxIsEmpty() {
        val a = BoundingBox.EMPTY
        val b = BoundingBox(0, 0, 10, 10)
        assertThat(a.contains(b)).isFalse()
        assertThat(b.contains(a)).isFalse()
    }

    @Test
    fun unionOfOverlappingBoxesIsSmallestEnclosing() {
        val a = BoundingBox(0, 0, 100, 50)
        val b = BoundingBox(50, 25, 150, 75)
        val u = a.union(b)
        assertThat(u).isEqualTo(BoundingBox(0, 0, 150, 75))
    }

    @Test
    fun unionOfDisjointBoxesIsSmallestEnclosing() {
        val a = BoundingBox(0, 0, 10, 10)
        val b = BoundingBox(100, 100, 110, 110)
        val u = a.union(b)
        assertThat(u).isEqualTo(BoundingBox(0, 0, 110, 110))
    }

    @Test
    fun overlapAreaZeroForDisjointBoxes() {
        val a = BoundingBox(0, 0, 50, 50)
        val b = BoundingBox(60, 60, 100, 100)
        assertThat(a.overlapArea(b)).isEqualTo(0)
        assertThat(b.overlapArea(a)).isEqualTo(0)
    }

    @Test
    fun overlapAreaExactForNestedBox() {
        val outer = BoundingBox(0, 0, 100, 100)
        val inner = BoundingBox(10, 10, 40, 40)
        // Intersection equals the inner box: 30 x 30 = 900.
        assertThat(outer.overlapArea(inner)).isEqualTo(900)
        assertThat(inner.overlapArea(outer)).isEqualTo(900)
    }

    @Test
    fun overlapAreaCorrectForPartialOverlap() {
        val a = BoundingBox(0, 0, 100, 100)
        val b = BoundingBox(50, 50, 150, 150)
        // Intersection: (50,50)-(100,100) = 50 x 50 = 2500.
        assertThat(a.overlapArea(b)).isEqualTo(2500)
        assertThat(b.overlapArea(a)).isEqualTo(2500)
    }

    @Test
    fun overlapsByFractionTrueWhenOverlapAtLeastHalf() {
        // a has area 100*100 = 10000. b overlaps 60*60 = 3600 -> 0.36 of a.
        // 0.36 < 0.5 -> false.
        val a = BoundingBox(0, 0, 100, 100)
        val b = BoundingBox(40, 40, 100, 100)
        assertThat(a.overlapsByFraction(b, 0.5f)).isFalse()

        // b2 overlaps 70*70 = 4900 -> 0.49 < 0.5 -> false.
        val b2 = BoundingBox(30, 30, 100, 100)
        assertThat(a.overlapsByFraction(b2, 0.5f)).isFalse()

        // b3 overlaps 80*80 = 6400 -> 0.64 >= 0.5 -> true.
        val b3 = BoundingBox(20, 20, 100, 100)
        assertThat(a.overlapsByFraction(b3, 0.5f)).isTrue()
    }

    @Test
    fun overlapsByFractionFalseForDisjoint() {
        val a = BoundingBox(0, 0, 50, 50)
        val b = BoundingBox(100, 100, 150, 150)
        assertThat(a.overlapsByFraction(b, 0.1f)).isFalse()
    }

    @Test
    fun overlapsByFractionFalseWhenBoxIsEmpty() {
        val a = BoundingBox.EMPTY
        val b = BoundingBox(0, 0, 10, 10)
        assertThat(a.overlapsByFraction(b, 0.1f)).isFalse()
    }

    @Test
    fun fromRectRoundTrips() {
        val rect = Rect(11, 22, 33, 44)
        val box = BoundingBox.from(rect)
        assertThat(box.left).isEqualTo(11)
        assertThat(box.top).isEqualTo(22)
        assertThat(box.right).isEqualTo(33)
        assertThat(box.bottom).isEqualTo(44)
    }

    @Test
    fun fromRectPreservesEmptyRect() {
        val empty = Rect(5, 5, 5, 5)
        val box = BoundingBox.from(empty)
        assertThat(box.isEmpty).isTrue()
        assertThat(box.area).isEqualTo(0)
    }
}
