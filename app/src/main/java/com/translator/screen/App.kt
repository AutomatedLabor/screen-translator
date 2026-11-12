package com.translator.screen

import android.app.Application

/**
 * Application entry point. Later phases will initialise DI containers, the
 * translation engine and telemetry here; for now it exists only so the manifest
 * attribute `android:name=".App"` resolves at compile time.
 */
class App : Application()
