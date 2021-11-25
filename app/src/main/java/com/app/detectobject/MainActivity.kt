package com.app.detectobject

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.app.detectobject.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private val TAG = "MyDetectTAG"

    private val CAMERA_PERMISSION_CODE = 123
    private val READ_STORAGE_PERMISSION_CODE = 113
    private val WRITE_STORAGE_PERMISSION_CODE = 113

    private lateinit var cameraLauncher : ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher : ActivityResultLauncher<Intent>

    private lateinit var inputImage : InputImage
    private lateinit var imageLabeler : ImageLabeler


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        initLaunchers()
        buttonsListener()
    }


    private fun initLaunchers() {
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), object : ActivityResultCallback<ActivityResult>{
            override fun onActivityResult(result: ActivityResult?) {
                val data = result?.data
                try {
                    val photo = data?.extras?.get("data") as Bitmap
                    binding.pictureIv.setImageBitmap(photo)
                    inputImage = InputImage.fromBitmap(photo,0)
                }catch (e:Exception){
                    Log.d(TAG,"onActivityResult: ${e.message}")
                }
            }
        })

        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
            object : ActivityResultCallback<ActivityResult>{
                override fun onActivityResult(result: ActivityResult?) {
                    val data = result?.data
                    try {
                        inputImage = InputImage.fromFilePath(this@MainActivity,data?.data)
                        binding.pictureIv.setImageURI(data?.data)
                        processImage()
                    }catch (e:Exception){
                        Log.d(TAG,"onActivityResult: ${e.message}")
                    }
                }
            })
    }


    private fun processImage() {
        imageLabeler.process(inputImage)
            .addOnSuccessListener {
                var result = ""
                for (label in it){
                    result = result +"\n"+label.text
                }
                binding.resultTv.text = result
            }.addOnFailureListener {
                Log.d(TAG,"processImage: ${it.message}")            }
    }


    private fun buttonsListener() {
        binding.chooseButton.setOnClickListener {
            val options = arrayOf("Camera","Gallery")
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Pick a option")
            builder.setItems(options,DialogInterface.OnClickListener { dialog,which ->
                if (which ==0){
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    cameraLauncher.launch(cameraIntent)
                }else{
                    val storeageIntent = Intent()
                    storeageIntent.setType("image/*")
                    storeageIntent.setAction(Intent.ACTION_GET_CONTENT)
                    galleryLauncher.launch(storeageIntent)
                }
            })
            builder.show()
        }
    }


    private fun checkPermission(permission:String,requestCode:Int){
        if (ContextCompat.checkSelfPermission(this,permission) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, arrayOf(permission),requestCode)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE,READ_STORAGE_PERMISSION_CODE)
            }else{
                Toast.makeText(this,"Camera permission denied",Toast.LENGTH_SHORT).show()
            }
        }else if (requestCode == READ_STORAGE_PERMISSION_CODE){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,WRITE_STORAGE_PERMISSION_CODE
                )
            }else{
                Toast.makeText(this,"Read storage permission denied",Toast.LENGTH_SHORT).show()
            }
        }else if (requestCode == WRITE_STORAGE_PERMISSION_CODE){
            if (!grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"storage permission denied",Toast.LENGTH_SHORT).show()
            }
        }
    }


}