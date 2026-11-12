package com.translator.screen.extract

/** Axis-aligned screen rectangle in absolute screen coordinates. */
data class BoundingBox(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val isEmpty: Boolean get() = width <= 0 || height <= 0
    val area: Int get() = if (isEmpty) 0 else width * height
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2

    /** Strict containment test used by deduplication. */
    fun contains(other: BoundingBox): Boolean =
        !isEmpty && !other.isEmpty &&
            other.left >= left && other.right <= right &&
            other.top >= top && other.bottom <= bottom

    /** Geometric union (smallest enclosing rect). */
    fun union(other: BoundingBox): BoundingBox =
        BoundingBox(
            minOf(left, other.left),
            minOf(top, other.top),
            maxOf(right, other.right),
            maxOf(bottom, other.bottom),
        )

    /** Area overlap between two boxes (0 if disjoint). */
    fun overlapArea(other: BoundingBox): Int {
        val xl = maxOf(left, other.left)
        val xr = minOf(right, other.right)
        val yt = maxOf(top, other.top)
        val yb = minOf(bottom, other.bottom)
        return if (xl < xr && yt < yb) (xr - xl) * (yb - yt) else 0
    }

    /** True if other overlaps at least `fraction` of this box's area. */
    fun overlapsByFraction(other: BoundingBox, fraction: Float): Boolean =
        !isEmpty && overlapArea(other).toFloat() / area.toFloat() >= fraction

    companion object {
        val EMPTY = BoundingBox(0, 0, 0, 0)

        fun from(rect: android.graphics.Rect): BoundingBox =
            BoundingBox(rect.left, rect.top, rect.right, rect.bottom)
    }
}
