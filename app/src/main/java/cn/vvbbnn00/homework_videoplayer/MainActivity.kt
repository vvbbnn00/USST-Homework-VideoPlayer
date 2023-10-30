package cn.vvbbnn00.homework_videoplayer

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment.getExternalStorageDirectory
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.DelicateCoroutinesApi
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject


@androidx.media3.common.util.UnstableApi
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val VID_URL = "https://vvbbnn00.cn/app-dev/vid-backend/videos"
        val VIDEO_OBJECT_LIST: MutableList<VideoData> = mutableListOf()
    }

    private var currentIndex = 0
    private var playerView: PlayerView? = null
    private var cachedPlayer: ExoPlayer? = null
    private var globalPlayer: ExoPlayer? = null
    private var cachedFactory: AppCacheDataSourceFactory? = null
    private var globalFactory: AppCacheDataSourceFactory? = null

    private var cachedVideoId: String = "null"
    private var currentVideoId: String = "null"


    @OptIn(DelicateCoroutinesApi::class)
    private fun getUrlList(callback: () -> Unit = {}) {
        // kotlin 协程执行网络请求
        GlobalScope.launch(Dispatchers.IO) {
            val url = URL(VID_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = StringBuilder()

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                reader.close()

                val jsonResponse = response.toString()
                val jsonObject = JSONObject(jsonResponse)
                val dataArray = jsonObject.getJSONArray("data")

                for (i in 0 until dataArray.length()) {
                    val item = dataArray.getJSONObject(i)
                    val vidId = item.getString("vidId")
                    val title = item.getString("title")
                    val vidurl = item.getString("url")
                    val cover = item.getString("cover")

                    Log.d(TAG, "vidId: $vidId, title: $title, url: $vidurl, cover: $cover")

                    val videoData = VideoData(vidId, title, vidurl, cover)
                    VIDEO_OBJECT_LIST.add(videoData)
                }
                // 加载完毕后回调
                callback()
            } else {
                Log.d(TAG, "HttpURLConnection responseCode: $responseCode")
            }
        }
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

        // 判断是否为最后一个视频，如果是则重新获取数据
        if (nextIndex == 0) {
            getUrlList {
                // 重新获取数据后，再次预加载下一个视频
                runOnUiThread {
                    preloadNextVideo()
                }
            }
            return
        }

        Log.d(
            TAG,
            "Start to preload video: ${nextIndex}, ${VIDEO_OBJECT_LIST[nextIndex].vidId}"
        )
        val nextVideo = VIDEO_OBJECT_LIST[nextIndex]

        if (currentVideoId == nextVideo.vidId) {
            Log.d(TAG, "Same video, skip preload")
            return
        }

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
        currentVideoId = VIDEO_OBJECT_LIST[currentIndex].vidId
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
    @SuppressLint("SetTextI18n")
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

        getUrlList {
            // 预加载第一个视频
            runOnUiThread {
                loadVideo()
                playVideo()
                preloadNextVideo()
            }
        }

        // 播放下一个视频
        findViewById<Button>(R.id.btn_next).setOnClickListener {
            if (currentIndex == VIDEO_OBJECT_LIST.size - 1) {
                Toast.makeText(this, "更多视频加载中", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            currentIndex = (currentIndex + 1) % VIDEO_OBJECT_LIST.size
            loadVideo()
            playVideo()
            preloadNextVideo()
        }

        // 播放上一个视频
        findViewById<Button>(R.id.btn_prev).setOnClickListener {
            if (currentIndex == 0) {
                Toast.makeText(this, "已经是第一个视频了", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
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