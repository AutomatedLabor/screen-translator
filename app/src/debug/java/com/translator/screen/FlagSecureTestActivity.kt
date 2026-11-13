package com.translator.screen

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

/**
 * A minimal parcel-tracking-style Activity protected by
 * [WindowManager.LayoutParams.FLAG_SECURE].
 *
 * Used by [ScreenTextExtractorInstrumentedTest] to verify that the screen
 * translator pipeline can still observe German text on a FLAG_SECURE window
 * via the accessibility tree (FLAG_SECURE blocks MediaProjection / screenshots
 * but does NOT block accessibility node introspection).
 *
 * Registered only in debug builds via `src/debug/AndroidManifest.xml`.
 */
class FlagSecureTestActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        val texts = listOf(
            "Sendungsnummer: DHL1234567890",
            "Ihr Paket befindet sich auf dem Weg.",
            "Voraussichtliche Zustellung: 15. Mai 2026",
            "Absender: Amazon EU Sàrl",
            "Empfänger: Max Mustermann",
            "Status: In Transit",
        )
        texts.forEach { t ->
            root.addView(
                TextView(this).apply {
                    text = t
                    textSize = 16f
                    setPadding(0, 24, 0, 24)
                }
            )
        }
        setContentView(root)
    }
}
