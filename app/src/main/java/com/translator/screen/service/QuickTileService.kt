package com.translator.screen.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.translator.screen.R
import com.translator.screen.ui.MainActivity

/**
 * Quick Settings tile that drives [TranslatorAccessibilityService].
 *
 * State mapping:
 *  - [Tile.STATE_UNAVAILABLE] — accessibility service not enabled or not yet
 *    connected; tapping opens [MainActivity] for setup.
 *  - [Tile.STATE_INACTIVE]    — service ready, overlay hidden; tapping
 *    triggers a translation.
 *  - [Tile.STATE_ACTIVE]      — overlay currently shown; tapping dismisses it
 *    (toggle behaviour).
 *
 * When the device is locked ([isLocked]) the tile click is routed through
 * [unlockAndRun] so [TranslatorAccessibilityService.triggerTranslation] runs
 * only after the keyguard is dismissed, which is required for
 * [android.accessibilityservice.AccessibilityService.getWindows] to return
 * the underlying app window rather than the keyguard.
 *
 * The tile is declared `ACTIVE_TILE` in the manifest, so the system does not
 * keep it listening when collapsed; [TranslatorAccessibilityService] pings
 * [requestListeningState] after every overlay show/dismiss so the icon flips
 * between INACTIVE/ACTIVE without the user pulling the shade.
 */
class QuickTileService : TileService() {

    companion object {
        private const val TAG = "QuickTileService"
    }

    override fun onStartListening() {
        super.onStartListening()
        refreshTileState()
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val service = TranslatorAccessibilityService.instance
        if (service == null || !TranslatorAccessibilityService.isReady()) {
            // Not configured — open MainActivity setup flow.
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            // startActivityAndCollapse(Intent) is deprecated on API 34+ with a documented
            // race against the shade-collapse animation; API 34+ added a PendingIntent
            // variant. Branch accordingly. (Fresh-eyes review P1-10.)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pi = android.app.PendingIntent.getActivity(
                    this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pi)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }
        if (service.isOverlayShowing()) {
            // Toggle off if currently showing.
            service.dismissOverlayPublic()
            refreshTileState()
            return
        }
        // Trigger translation. On a locked device, defer until the keyguard
        // is dismissed so the accessibility windows list reflects the actual
        // app under the lock screen.
        // isLocked + unlockAndRun require API 26 (Android 8). On API 24-25 (Android 7-7.1)
        // neither exists; just trigger directly (the keyguard case is rare + the snapshot
        // picks whatever window is available). (v1.4.0: minSdk 24 -> Android 7+.)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isLocked) {
            unlockAndRun { service.triggerTranslation() }
        } else {
            service.triggerTranslation()
        }
        // Optimistically refresh now; the service will ping us again via
        // requestListeningState once the overlay is actually shown.
        refreshTileState()
    }

    private fun refreshTileState() {
        val tile = qsTile ?: return
        val service = TranslatorAccessibilityService.instance
        val label = getString(R.string.quick_tile_label)
        when {
            service == null || !TranslatorAccessibilityService.isReady() -> {
                tile.state = Tile.STATE_UNAVAILABLE
                tile.label = label
            }
            service.isOverlayShowing() -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = label
            }
            else -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = label
            }
        }
        tile.contentDescription = "Translate German screen to English"
        tile.updateTile()
        Log.d(TAG, "refreshTileState -> state=${tile.state}")
    }
}
