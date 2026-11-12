package com.translator.screen.translate

/** Contract for the German→English translator. All methods are main-safe. */
interface ITranslationEngine {
  /** Translate a single source string to English. Returns source unchanged if filtered out. */
  suspend fun translate(source: String): String

  /** True if the German→English offline model is downloaded and ready. */
  suspend fun isModelDownloaded(): Boolean

  /** Trigger ML Kit model download (idempotent). Suspends until download completes or fails. */
  suspend fun downloadModelIfNeeded()

  /** True if [source] passes the pre-translation filter (alphabetic, not pure number/date/tracking code, not confidently English). */
  suspend fun shouldTranslate(source: String): Boolean

  /** Release native resources. Safe to call multiple times. */
  fun close()

  /** Optional warmup; called from service onServiceConnected(). */
  fun warmup()
}
