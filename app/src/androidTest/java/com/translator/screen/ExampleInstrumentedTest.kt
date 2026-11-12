package com.translator.screen

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun appPackageName() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assertThat(ctx.packageName).isEqualTo("com.translator.screen")
    }
}
