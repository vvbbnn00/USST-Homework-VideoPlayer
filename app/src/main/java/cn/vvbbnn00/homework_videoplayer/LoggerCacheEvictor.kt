package cn.vvbbnn00.homework_videoplayer

import android.util.Log
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheEvictor
import androidx.media3.datasource.cache.CacheSpan

@androidx.media3.common.util.UnstableApi
class LoggerCacheEvictor(
    private val vidId: String
) : CacheEvictor {
    companion object {
        private const val TAG = "LoggerCacheEvictor"
    }

    override fun onSpanAdded(cache: Cache, span: CacheSpan) {
        Log.d(TAG, "Video-[${vidId}] onSpanAdded: $span")
    }

    override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
        Log.d(TAG, "Video-[${vidId}] onSpanRemoved: $span")
    }

    override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) {
        // Log.d(TAG, "Video-[${vidId}] onSpanTouched: $oldSpan -> $newSpan")
    }

    override fun requiresCacheSpanTouches(): Boolean {
        return true
    }

    override fun onCacheInitialized() {
        // do nothing
    }

    override fun onStartFile(cache: Cache, key: String, position: Long, length: Long) {
        Log.d(TAG, "Video-[${vidId}] onStartFile: $key, $position, $length")
    }
}