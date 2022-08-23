package com.test.tonguephotoshoot

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import java.text.SimpleDateFormat
import java.util.*

const val LOCAL_URL = "http://localhost:80"

class MainActivity : AppCompatActivity() {
    lateinit var currentPhotoPath : String
    val REQUEST_IMAGE_CAPTURE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission()

        main_btn_camera_open.setOnClickListener {
            startCapture()
        }

        test_button.setOnClickListener{
            startTest()
        }
    }

    private fun checkPermission() {
        val permission = mutableMapOf<String, String>()
        permission["camera"] = Manifest.permission.CAMERA

        val denied = permission.count { ContextCompat.checkSelfPermission(this, it.value)  == PackageManager.PERMISSION_DENIED }

        if(denied > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permission.values.toTypedArray(), REQUEST_IMAGE_CAPTURE)
        }

        val MY_PERMISSION_ACCESS_ALL = 100
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            var permissions = arrayOf(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
            ActivityCompat.requestPermissions(this, permissions, MY_PERMISSION_ACCESS_ALL)
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

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap

            main_img_photo.setImageBitmap(imageBitmap)

            val url = edit_url_string.text.toString() // editText에서 받아온 url

            // 서버로 이미지 데이터 보내기
            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val server = retrofit.create(RetrofitAPI::class.java)

            val id = edit_text_number.text.toString().toInt()

            // 해당 파일을 휴대폰에 저장
            val file = convertBitmapToFile(imageBitmap, "/storage/emulated/0/Download/$id")
            val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            val body: MultipartBody.Part = MultipartBody.Part.createFormData("image", file.name, requestFile)

            server.uploadImage(id, body).enqueue((object: Callback<ResponseDC> {
                override fun onFailure(call: Call<ResponseDC>, t: Throwable) {
                    val msg = t.localizedMessage
                    Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(call: Call<ResponseDC>, response: Response<ResponseDC>) {
                    val msg = response?.body().toString()
                    Toast.makeText(applicationContext, msg,
                        Toast.LENGTH_LONG).show()
                }
            }))
        }
   }*/

    private fun startTest() {
        val url = edit_url_string.text.toString() // editText에서 받아온 url

        // 서버로 이미지 데이터 보내기
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val server = retrofit.create(RetrofitAPI::class.java)

        val id = edit_text_number.text.toString().toInt()

        val drawable = getDrawable(R.drawable.tongue)
        val bitmap = (drawable as BitmapDrawable).bitmap

        val directory = File("/storage/emulated/0/Download/")
        if(!directory.exists())
            directory.mkdirs()

        val file = convertBitmapToFile(bitmap, "/storage/emulated/0/Download/$id")
        val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        val body: MultipartBody.Part = MultipartBody.Part.createFormData("image", file.name, requestFile)

        server.uploadImage(id, body).enqueue((object: Callback<ResponseDC> {
            override fun onFailure(call: Call<ResponseDC>, t: Throwable) {
                val msg = t.localizedMessage
                Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
            }

            override fun onResponse(call: Call<ResponseDC>, response: Response<ResponseDC>) {
                val msg = response?.body().toString()
                Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
            }
        }))
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

    @Throws(IOException::class)
    private fun createImageFile() : File {
        val timeStamp : String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir : File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply{
            currentPhotoPath = absolutePath
        }
    }

    fun startCapture(){
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try{
                    createImageFile()
                }catch(ex:IOException){
                    null
                }
                photoFile?.also {
                    val photoURI : Uri = FileProvider.getUriForFile(
                        this,
                        "com.test.tonguephotoshoot.fileprovider",
                        it
                    )

                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                        takePictureIntent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                    } else {
                        takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", 1)
                    }

                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK){
            val file = File(currentPhotoPath)
            if (Build.VERSION.SDK_INT < 28) {
                val bitmap = MediaStore.Images.Media
                    .getBitmap(contentResolver, Uri.fromFile(file))
                main_img_photo.setImageBitmap(bitmap)
            }
            else{
                val decode = ImageDecoder.createSource(this.contentResolver,
                    Uri.fromFile(file))
                val bitmap = ImageDecoder.decodeBitmap(decode)
                main_img_photo.setImageBitmap(bitmap)
            }

            val url = edit_url_string.text.toString() // editText에서 받아온 url
            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val server = retrofit.create(RetrofitAPI::class.java)

            var id: Int?
            if (edit_text_number.text.toString().isEmpty()) {
                id = 1
            } else {
                id = edit_text_number.text.toString().toInt()
            }

            val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            val body: MultipartBody.Part = MultipartBody.Part.createFormData("image", file.name, requestFile)

            server.uploadImage(id, body).enqueue((object: Callback<ResponseDC> {
                override fun onFailure(call: Call<ResponseDC>, t: Throwable) {
                    val msg = t.localizedMessage
                    Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(call: Call<ResponseDC>, response: Response<ResponseDC>) {
                    val msg = response?.body().toString()
                    Toast.makeText(applicationContext, msg,
                        Toast.LENGTH_LONG).show()
                }
            }))
        }
    }
}