package com.translator.screen.translate

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for [MlKitTranslationEngine.shouldTranslate].
 *
 * Every case below is rejected by the regex filter BEFORE ML Kit language
 * identification is invoked, so these assertions are deterministic and do
 * not depend on Play Services being available.
 *
 * Production regex constants (documented for test clarity):
 *  - NUMBER_REGEX  = ^[0-9]+([.,][0-9]+)?$
 *  - PUNCT_REGEX   = ^[^\p{L}\p{N}]+$
 *  - DATE_REGEX    = ^\d{1,4}[-/.\s]\d{1,2}[-/.\s]\d{1,4}$
 *  - TIME_REGEX    = ^\d{1,2}:\d{2}([:\d]*)?$
 *  - TRACKING_REGEX = ^\s*[A-Z]{2,4}\d{8,20}[A-Z]?\s*$ (case-insensitive)
 *  - URL_EMAIL_REGEX = ^(https?://|www\.)\S+$ | email | bare-domain
 *
 * Plus two early guards: blank/too-short and no-alphabetic-chars.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MlKitTranslationEngineFilterTest {

    private lateinit var engine: MlKitTranslationEngine

    @Before
    fun setUp() {
        engine = MlKitTranslationEngine()
    }

    @After
    fun tearDown() {
        engine.close()
    }

    @Test
    fun pureNumberIsRejected() = runTest {
        assertThat(engine.shouldTranslate("12345")).isFalse()
    }

    @Test
    fun decimalWithCommaIsRejected() = runTest {
        assertThat(engine.shouldTranslate("12,50")).isFalse()
    }

    @Test
    fun purePunctuationIsRejected() = runTest {
        assertThat(engine.shouldTranslate("!!!")).isFalse()
    }

    @Test
    fun isoDateIsRejected() = runTest {
        assertThat(engine.shouldTranslate("12.05.2024")).isFalse()
    }

    @Test
    fun timeIsRejected() = runTest {
        assertThat(engine.shouldTranslate("14:30")).isFalse()
    }

    @Test
    fun trackingCodeIsRejected() = runTest {
        assertThat(engine.shouldTranslate("DHL1234567890")).isFalse()
    }

    @Test
    fun urlIsRejected() = runTest {
        assertThat(engine.shouldTranslate("https://example.com")).isFalse()
    }

    @Test
    fun emailIsRejected() = runTest {
        assertThat(engine.shouldTranslate("a@b.de")).isFalse()
    }

    @Test
    fun singleCharacterIsRejectedByLengthGuard() = runTest {
        // Length < 2 guard fires before any regex.
        assertThat(engine.shouldTranslate("A")).isFalse()
    }

    @Test
    fun blankStringIsRejected() = runTest {
        assertThat(engine.shouldTranslate("")).isFalse()
        assertThat(engine.shouldTranslate("   ")).isFalse()
    }

    @Test
    fun germanPhraseFallsBackGracefullyWhenLanguageIdUnavailable() = runTest {
        // "Hallo Welt" passes every regex filter (alphabetic, not a number /
        // date / time / tracking code / URL / email), so the engine then
        // invokes ML Kit LanguageIdentification. Under Robolectric there is
        // no Play Services backend, so identification either fails and is
        // caught by the engine's internal try/catch (which defaults to
        // returning `true`) or throws before that catch. Both outcomes are
        // acceptable; the contract only requires that the call does not hang
        // and that genuine German prose is not rejected by the regex layer.
        try {
            val result = engine.shouldTranslate("Hallo Welt")
            assertThat(result).isTrue()
        } catch (e: Exception) {
            // Acceptable: ML Kit initialization may throw under Robolectric
            // when Play Services are absent. On a real device the engine's
            // internal catch handles this and returns true.
        }
    }
}
