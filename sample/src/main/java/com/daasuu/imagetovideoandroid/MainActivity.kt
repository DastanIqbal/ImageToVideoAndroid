package com.daasuu.imagetovideoandroid

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import com.daasuu.imagetovideo.EncodeListener
import com.daasuu.imagetovideo.ImageToVideoConverter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

  companion object {
    private const val PERMISSION_REQUEST_CODE = 88888
  }

  private var imageLoader: ImageLoader? = null
  private var imagePath: String? = null
  private var imageToVideo: ImageToVideoConverter? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_image_to_video)
    val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
    progressBar.max = 100

    findViewById<Button>(R.id.button).setOnClickListener { view ->
      imagePath?.let {
        view.isEnabled = false
        val outputPath = getVideoFilePath()
        imageToVideo = ImageToVideoConverter(outputPath = outputPath, inputImagePath = it, listener = object : EncodeListener {
          override fun onProgress(progress: Float) {
            Log.d("progress", "progress = $progress")
            runOnUiThread {
              progressBar.progress = (progress * 100).toInt()
            }
          }

          override fun onCompleted() {
            runOnUiThread {
              view.isEnabled = true
              progressBar.progress = 100
            }
            exportMp4ToGallery(applicationContext, outputPath)
          }

          override fun onFailed(exception: Exception) {

          }
        })
        imageToVideo?.start()
      }


    }

    findViewById<Button>(R.id.stop_button).setOnClickListener {
      imageToVideo?.stop()
    }

  }

  override fun onResume() {
    super.onResume()
    if (checkPermission()) {
      imageLoader = ImageLoader(applicationContext)
      imageLoader?.loadDeviceVideos(object : ImageLoadListener {
        override fun onVideoLoaded(imagePaths: List<String>) {
          val lv = findViewById<ListView>(R.id.image_list)
          val adapter = ImageListAdapter(applicationContext, R.layout.row_image_list, imagePaths)
          lv.adapter = adapter

          lv.setOnItemClickListener { parent, view, position, id ->
            imagePath = imagePaths[position]
            findViewById<Button>(R.id.button).isEnabled = true
          }
        }

        override fun onFailed(e: Exception) {

        }
      })
    }
  }

  override fun onPause() {
    super.onPause()
    imageLoader?.abortLoadVideos()
    imageLoader = null
  }


  @SuppressLint("NewApi")
  private fun checkPermission(): Boolean {
    // request permission if it has not been grunted.
    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
      return false
    }
    return true
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    when (requestCode) {
      PERMISSION_REQUEST_CODE -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(this@MainActivity, "permission has been grunted.", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(this@MainActivity, "[WARN] permission is not grunted.", Toast.LENGTH_SHORT).show()
      }
    }
  }


  private fun getAndroidMoviesFolder(): File {
    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
  }

  private fun getVideoFilePath(): String {
    return getAndroidMoviesFolder().absolutePath + "/" + SimpleDateFormat("yyyyMM_dd-HHmmss").format(Date()) + "image_video.mp4"
  }

  private fun exportMp4ToGallery(context: Context, filePath: String) {
    val values = ContentValues(2)
    values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
    values.put(MediaStore.Video.Media.DATA, filePath)
    // MediaStoreに登録
    context.contentResolver.insert(
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values
    )
    context.sendBroadcast(
      Intent(
        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://$filePath")
      )
    )
  }
}
