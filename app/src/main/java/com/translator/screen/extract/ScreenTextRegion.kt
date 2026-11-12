package com.translator.screen.extract

/**
 * A region of text discovered on screen, ready to be translated.
 *
 * `parentSignature` is a stable hash of the parent container's identity
 * (class name + bounds + child count) used by spatial line grouping to
 * decide whether two nodes belong to the same logical group.
 */
data class ScreenTextRegion(
    val text: String,
    val bounds: BoundingBox,
    val packageName: String,
    val parentSignature: String? = null,
) {
    val textLength: Int get() = text.length
    val hasAlphabetic: Boolean get() = text.any { it.isLetter() }
}
