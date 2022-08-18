package com.test.tonguephotoshoot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    val REQUEST_IMAGE_CAPTURE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission()

        main_btn_camera_open.setOnClickListener {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                    takePictureIntent.resolveActivity(packageManager)?.also {
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                    }
                }
        }
    }

    private fun checkPermission() {
        val permission = mutableMapOf<String, String>()
        permission["camera"] = Manifest.permission.CAMERA

        val denied = permission.count { ContextCompat.checkSelfPermission(this, it.value)  == PackageManager.PERMISSION_DENIED }

        if(denied > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permission.values.toTypedArray(), REQUEST_IMAGE_CAPTURE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_IMAGE_CAPTURE) {
            val count = grantResults.count { it == PackageManager.PERMISSION_DENIED }

            if(count != 0) {
                Toast.makeText(applicationContext, "권한을 동의해주세요.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap

            main_img_photo.setImageBitmap(imageBitmap)

            // 서버로 이미지 데이터 보내기
            val url = "http://192.168.0.10:80"
            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val server = retrofit.create(RetrofitAPI::class.java)

            // 해당 파일을 휴대폰에 저장
            val id = edit_text_number.text.toString().toInt()

            val file = convertBitmapToFile(imageBitmap, "/storage/emulated/0/Download/$id")
            val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            val body: MultipartBody.Part = MultipartBody.Part.createFormData("image", file.name, requestFile)

            server.uploadImage(id, body).enqueue((object: Callback<ResponseDC> {
                override fun onFailure(call: Call<ResponseDC>, t: Throwable) {

                }

                override fun onResponse(call: Call<ResponseDC>, response: Response<ResponseDC>) {
                    Log.d("response : ", response?.body().toString())
                }
            }))
        }
   }

    private fun convertBitmapToFile(bitmap: Bitmap, filePath: String): File {
        val file = File(filePath)

        var out: OutputStream? = null
        try {
            file.createNewFile()
            out = FileOutputStream(file)

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        finally {
            try {
                if (out != null) {
                    out.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return file
    }
}