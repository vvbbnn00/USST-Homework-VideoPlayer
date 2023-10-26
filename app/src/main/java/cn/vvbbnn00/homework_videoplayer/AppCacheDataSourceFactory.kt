package cn.vvbbnn00.homework_videoplayer

import android.content.Context
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@androidx.media3.common.util.UnstableApi
class AppCacheDataSourceFactory(
    videoId: String,
    context: Context,
    private val defaultDatasourceFactory: DataSource.Factory
) : DataSource.Factory {

    private val cache: Cache = SimpleCache(
        File(context.cacheDir, videoId),
        LoggerCacheEvictor(videoId)
    )

    override fun createDataSource(): DataSource {
        return CacheDataSource(
            cache,
            defaultDatasourceFactory.createDataSource(),
        )
    }

    fun release() {
        cache.release()
    }
}
