package com.translator.screen.translate

import android.util.LruCache
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric test verifying [android.util.LruCache] semantics that
 * [MlKitTranslationEngine] relies on for its 500-entry translation cache.
 *
 * The production engine creates `LruCache<String, String>(500)`. This test
 * confirms that:
 *  - exactly 500 entries fit,
 *  - inserting the 501st evicts the least-recently-used (first inserted) entry,
 *  - re-accessing an entry promotes it to most-recently-used so it survives
 *    eviction over a stale entry.
 *
 * Robolectric provides a working shadow for [LruCache], so these assertions
 * are deterministic on the JVM.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LruCacheBehaviorTest {

    @Test
    fun cacheHoldsExactlyCapacityEntries() {
        val cache = LruCache<String, String>(500)
        for (i in 0 until 500) {
            cache.put("key-$i", "value-$i")
        }
        assertThat(cache.size()).isEqualTo(500)
        // Every key should still be present.
        for (i in 0 until 500) {
            assertThat(cache.get("key-$i")).isEqualTo("value-$i")
        }
    }

    @Test
    fun insertingBeyondCapacityEvictsLeastRecentlyUsed() {
        val cache = LruCache<String, String>(500)
        for (i in 0 until 500) {
            cache.put("key-$i", "value-$i")
        }
        // Insert one more — the LRU (key-0) should be evicted.
        cache.put("key-500", "value-500")
        assertThat(cache.size()).isEqualTo(500)
        assertThat(cache.get("key-0")).isNull()
        assertThat(cache.get("key-500")).isEqualTo("value-500")
        // key-499 (most recently used before the new entry) should survive.
        assertThat(cache.get("key-499")).isEqualTo("value-499")
    }

    @Test
    fun getPromotesEntryToMostRecentlyUsed() {
        val cache = LruCache<String, String>(500)
        for (i in 0 until 500) {
            cache.put("key-$i", "value-$i")
        }
        // Touch key-0 so it becomes the most-recently-used.
        assertThat(cache.get("key-0")).isEqualTo("value-0")
        // Now insert one more. key-1 (the new LRU) should be evicted, not key-0.
        cache.put("key-500", "value-500")
        assertThat(cache.size()).isEqualTo(500)
        assertThat(cache.get("key-0")).isEqualTo("value-0")
        assertThat(cache.get("key-1")).isNull()
    }

    @Test
    fun evictAllClearsCache() {
        val cache = LruCache<String, String>(500)
        cache.put("a", "1")
        cache.put("b", "2")
        assertThat(cache.size()).isEqualTo(2)
        cache.evictAll()
        assertThat(cache.size()).isEqualTo(0)
        assertThat(cache.get("a")).isNull()
    }
}
