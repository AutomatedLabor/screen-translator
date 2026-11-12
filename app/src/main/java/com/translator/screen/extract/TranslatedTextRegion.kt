package com.translator.screen.extract

/**
 * A [ScreenTextRegion] paired with its translated text.
 * Bounds default to the source region bounds but may be overridden
 * (e.g. expanded to fit a longer translation).
 */
data class TranslatedTextRegion(
    val source: ScreenTextRegion,
    val translated: String,
    val bounds: BoundingBox = source.bounds,
)
