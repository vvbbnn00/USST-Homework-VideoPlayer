package cn.vvbbnn00.homework_videoplayer

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment.getExternalStorageDirectory
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide

@androidx.media3.common.util.UnstableApi
class MainActivity : AppCompatActivity() {

    private var currentIndex = 0
    private var playerView: PlayerView? = null
    private var cachedPlayer: ExoPlayer? = null
    private var globalPlayer: ExoPlayer? = null
    private var cachedFactory: AppCacheDataSourceFactory? = null
    private var globalFactory: AppCacheDataSourceFactory? = null

    private var cachedVideoId: String = "null"


    companion object {
        private const val TAG = "MainActivity"

        // 视频列表
        val VIDEO_OBJECT_LIST = listOf(
            VideoData(
                "vid-001",
                "Eden - Opening Movie",
                "https://naruse.io/api/raw/?path=/%F0%9F%93%BA%20Bangumi/eden-%20Opening%20Movie.mp4",
                "https://naruse.io/api/thumbnail/?path=/%F0%9F%93%BA%20Bangumi/eden-%20Opening%20Movie.mp4&size=medium"
            ),
            VideoData(
                "vid-002",
                "ef - a tale of memories OP1",
                "https://naruse.io/api/raw/?path=/%F0%9F%93%BA%20Bangumi/%E3%80%8A%E6%82%A0%E4%B9%85%E4%B9%8B%E7%BF%BC%E3%80%8BOP1.mp4",
                "https://naruse.io/api/thumbnail/?path=/%F0%9F%93%BA%20Bangumi/%E3%80%8A%E6%82%A0%E4%B9%85%E4%B9%8B%E7%BF%BC%E3%80%8BOP1.mp4&size=medium"
            ),
            VideoData(
                "vid-003",
                "サクラノ詩 - 櫻の森の上を舞う- OP",
                "https://naruse.io/api/raw/?path=/%F0%9F%93%BA%20Bangumi/%E3%80%90%E5%85%AC%E5%BC%8F%E3%80%91%E3%82%B5%E3%82%AF%E3%83%A9%E3%83%8E%E8%A9%A9%20-%E6%AB%BB%E3%81%AE%E6%A3%AE%E3%81%AE%E4%B8%8A%E3%82%92%E8%88%9E%E3%81%86-%20OP%E3%83%A0%E3%83%BC%E3%83%93%E3%83%BC%E3%80%90%E6%9E%95%E3%80%91.mp4",
                "https://naruse.io/api/thumbnail/?path=/%F0%9F%93%BA%20Bangumi/%E3%80%90%E5%85%AC%E5%BC%8F%E3%80%91%E3%82%B5%E3%82%AF%E3%83%A9%E3%83%8E%E8%A9%A9%20-%E6%AB%BB%E3%81%AE%E6%A3%AE%E3%81%AE%E4%B8%8A%E3%82%92%E8%88%9E%E3%81%86-%20OP%E3%83%A0%E3%83%BC%E3%83%93%E3%83%BC%E3%80%90%E6%9E%95%E3%80%91.mp4&size=medium"
            ),
            VideoData(
                "vid-004",
                "Subarashiki Hibi ~Furenzoku Sonzai~ Opening",
                "https://naruse.io/api/raw/?path=/%F0%9F%93%BA%20Bangumi/Subarashiki%20Hibi%20~Furenzoku%20Sonzai~%20Opening.mp4",
                "https://naruse.io/api/thumbnail/?path=/%F0%9F%93%BA%20Bangumi/Subarashiki%20Hibi%20~Furenzoku%20Sonzai~%20Opening.mp4&size=medium"
            )
        )
    }


    private fun askForPermission() {
        // 申请权限
        if (ActivityCompat.checkSelfPermission(
                this,
                ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"),
                1
            )
        }
    }


    /**
     * 预加载下一个视频
     */
    private fun preloadNextVideo() {
        // 预加载前需要释放当前的播放器和缓存对象，否则可能造成资源抢占
        if (cachedPlayer != null) {
            cachedPlayer?.stop()
            cachedPlayer?.release()
            cachedPlayer = null
        }

        if (cachedFactory != null) {
            cachedFactory?.release()
            cachedFactory = null
        }

        val nextIndex = (currentIndex + 1) % VIDEO_OBJECT_LIST.size
        Log.d(
            TAG,
            "Start to preload video: ${nextIndex}, ${VIDEO_OBJECT_LIST[nextIndex].vidId}"
        )
        val nextVideo = VIDEO_OBJECT_LIST[nextIndex]
        val mediaItem = MediaItem.fromUri(nextVideo.url)
        val customCacheDataSourceFactory = AppCacheDataSourceFactory(
            nextVideo.vidId,
            this,
            DefaultHttpDataSource.Factory()
        )
        cachedFactory = customCacheDataSourceFactory

        val mediaSource = ProgressiveMediaSource.Factory(customCacheDataSourceFactory)
            .createMediaSource(mediaItem)

        cachedPlayer = ExoPlayer.Builder(this).build()
        cachedPlayer!!.setMediaSource(mediaSource)
        cachedPlayer!!.prepare()
        cachedPlayer!!.playWhenReady = false
    }

    /**
     * 播放视频
     */
    private fun playVideo() {
        globalFactory?.release()
        globalFactory = null

        if (cachedVideoId != VIDEO_OBJECT_LIST[currentIndex].vidId) {
            cachedPlayer?.stop()
            cachedPlayer?.release()
            cachedPlayer = null
            cachedVideoId = "null"
            cachedFactory?.release()
            cachedFactory = null
        }

        Log.d(TAG, "Start to play video: ${currentIndex}, ${VIDEO_OBJECT_LIST[currentIndex].vidId}")
        val nextVideo = VIDEO_OBJECT_LIST[currentIndex]
        val mediaItem = MediaItem.fromUri(nextVideo.url)
        val customCacheDataSourceFactory = AppCacheDataSourceFactory(
            nextVideo.vidId,
            this,
            DefaultHttpDataSource.Factory()
        )
        globalFactory = customCacheDataSourceFactory

        val mediaSource = ProgressiveMediaSource.Factory(customCacheDataSourceFactory)
            .createMediaSource(mediaItem)

        globalPlayer!!.setMediaSource(mediaSource)
        globalPlayer!!.prepare()
        globalPlayer!!.playWhenReady = true
        cachedVideoId = nextVideo.vidId
    }


    /**
     * 加载视频信息，并显示在界面上
     */
    private fun loadVideo() {
        Glide.with(this)
            .load(VIDEO_OBJECT_LIST[currentIndex].cover)
            .into(findViewById(R.id.img_cover))
        findViewById<TextView>(R.id.txt_title).text = VIDEO_OBJECT_LIST[currentIndex].title
        findViewById<TextView>(R.id.txt_description).text =
            "Id: ${VIDEO_OBJECT_LIST[currentIndex].vidId}"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        askForPermission()

        playerView = findViewById(R.id.player_view)
        globalPlayer = ExoPlayer.Builder(this).build()
        playerView!!.player = globalPlayer

        // 加载第一个视频
        loadVideo()
        playVideo()
        preloadNextVideo() // 在播放第一个视频的同时，预加载第二个视频

        // 监听播放器状态
        globalPlayer!!.addAnalyticsListener(object : AnalyticsListener {
            override fun onIsPlayingChanged(
                eventTime: AnalyticsListener.EventTime,
                isPlaying: Boolean
            ) {
                // 当前视频播放完毕时，自动播放下一个视频
                if (!isPlaying && globalPlayer!!.playbackState == ExoPlayer.STATE_ENDED) {
                    currentIndex = (currentIndex + 1) % VIDEO_OBJECT_LIST.size
                    loadVideo()
                    playVideo()
                }
            }
        })


        // 播放下一个视频
        findViewById<Button>(R.id.btn_next).setOnClickListener {
            currentIndex = (currentIndex + 1) % VIDEO_OBJECT_LIST.size
            loadVideo()
            playVideo()
            preloadNextVideo()
        }

        // 播放上一个视频
        findViewById<Button>(R.id.btn_prev).setOnClickListener {
            currentIndex = (currentIndex - 1 + VIDEO_OBJECT_LIST.size) % VIDEO_OBJECT_LIST.size
            loadVideo()
            playVideo()
            preloadNextVideo()
        }

        findViewById<Button>(R.id.btn_download).setOnClickListener {
            val fileUrl = VIDEO_OBJECT_LIST[currentIndex].url
            // 下载到 /sdcard/Download/ 下
            val destinationPath =
                "${getExternalStorageDirectory().path}/Download/${VIDEO_OBJECT_LIST[currentIndex].vidId}.mp4"
            val intent = Intent(this, DownloadService::class.java)
            intent.putExtra("fileUrl", fileUrl)
            intent.putExtra("destinationPath", destinationPath)
            startService(intent)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        cachedPlayer?.release()
        globalPlayer?.release()
        cachedPlayer = null
        globalPlayer = null
    }
}