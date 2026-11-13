package com.translator.screen.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TargetLanguagesTest {

    @Test
    fun all_includesEnglishAndUrdu() {
        val tags = TargetLanguages.ALL.map { it.tag }
        assertThat(tags).contains("en")
        assertThat(tags).contains("ur")
    }

    @Test
    fun all_tagsAreUnique() {
        val tags = TargetLanguages.ALL.map { it.tag }
        assertThat(tags).containsNoDuplicates()
    }

    @Test
    fun displayFor_knownTag_returnsDisplay() {
        assertThat(TargetLanguages.displayFor("en")).isEqualTo("English")
        assertThat(TargetLanguages.displayFor("ur")).contains("Urdu")
    }

    @Test
    fun displayFor_unknownTag_returnsTagItself() {
        assertThat(TargetLanguages.displayFor("xx")).isEqualTo("xx")
    }

    @Test
    fun all_entriesHaveNonBlankDisplay() {
        for (entry in TargetLanguages.ALL) {
            assertThat(entry.display).isNotEmpty()
        }
    }
}
