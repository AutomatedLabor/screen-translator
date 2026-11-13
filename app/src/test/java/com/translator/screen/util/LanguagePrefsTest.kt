package com.translator.screen.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LanguagePrefsTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun defaultTargetTag_isEnglish() {
        context.getSharedPreferences("translator_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        assertThat(LanguagePrefs.getTargetTag(context)).isEqualTo("en")
    }

    @Test
    fun defaultConstant_isEnglish() {
        assertThat(LanguagePrefs.DEFAULT_TARGET_TAG).isEqualTo("en")
    }

    @Test
    fun setTargetTag_persistsAndReadsBack() {
        LanguagePrefs.setTargetTag(context, "ur")
        assertThat(LanguagePrefs.getTargetTag(context)).isEqualTo("ur")
        LanguagePrefs.setTargetTag(context, "fr")
        assertThat(LanguagePrefs.getTargetTag(context)).isEqualTo("fr")
    }

    @Test
    fun setTargetTag_survivesNewContextInstance() {
        LanguagePrefs.setTargetTag(context, "ar")
        val again = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertThat(LanguagePrefs.getTargetTag(again)).isEqualTo("ar")
    }
}
