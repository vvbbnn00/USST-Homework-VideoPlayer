package cn.vvbbnn00.homework_videoplayer;

import android.Manifest
import android.app.IntentService;
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent;
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

class DownloadService : IntentService("DownloadService") {
    private var NOTIFICATION_CHANNEL_ID = "download_channel"
    private val NOTIFICATION_ID = 1

    /**
     * 接受下载请求
     */
    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        val fileUrl = intent?.getStringExtra("fileUrl") ?: return
        val destinationPath = intent.getStringExtra("destinationPath") ?: return

        try {
            downloadFile(fileUrl, destinationPath)
        } catch (e: Exception) {
            // Handle the exception as you see fit.
            e.printStackTrace()
        }
    }

    /**
     * 开始下载文件
     */
    private fun downloadFile(fileUrl: String, destinationPath: String) {
        try {
            Toast.makeText(this, "Waiting For Download...", Toast.LENGTH_SHORT).show()
            NOTIFICATION_CHANNEL_ID = "download_channel_${destinationPath.hashCode()}"
            createNotificationChannel()
            Toast.makeText(this, "Downloading into $destinationPath", Toast.LENGTH_SHORT).show()
            val url = URL(fileUrl)
            val connection = url.openConnection()
            connection.connect()

            val input: InputStream = BufferedInputStream(url.openStream(), 8192)
            val fileSize = connection.contentLength
            val output = FileOutputStream(destinationPath)

            val data = ByteArray(1024)
            var count: Int
            var total: Long = 0

            // 下载文件
            while (input.read(data).also { count = it } != -1) {
                output.write(data, 0, count)
                total += count.toLong()
                if (fileSize > 0) {
                    val progress = (total * 100 / fileSize)
                    showNotification(progress.toInt())
                }
            }

            output.flush()
            output.close()
            input.close()
            Toast.makeText(this, "Downloaded at $destinationPath", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to download ($fileUrl)", Toast.LENGTH_SHORT).show()
        }
        removeNotification()
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        val channelName = "Download Channel"
        val channelDescription = "Channel for downloading files"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance).apply {
            description = channelDescription
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * 显示下载进度
     */
    private fun showNotification(progress: Int) {
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Downloading File")
            .setContentText("$progress%")
            .setSmallIcon(R.drawable.ic_download)
            .setProgress(100, progress, false)

        val notificationManager = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("DownloadService", "No permission to post notifications")
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    /**
     * 移除通知
     */
    private fun removeNotification() {
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
