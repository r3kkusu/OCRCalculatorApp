package com.testapp.ocrcalculatorapp

import android.Manifest
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.testapp.ocrcalculatorapp.databinding.ActivityMainBinding
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private val debugTag = "MainActivity"

    private lateinit var binding:ActivityMainBinding

    private val permissionRequestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        initFeatures()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            permissionRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_GRANTED)) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    private fun initFeatures() {
        if (getString(R.string.build_feature).equals("camera", ignoreCase = true)) {
            initCamera();
        } else {
            initGallery();
        }
    }

    private fun initCamera() {
        if (!checkCameraPermission()) {
            requestCameraPermission()
        }
        setupCamera()
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.CAMERA),
            permissionRequestCode
        )
    }

    private fun setupCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "new_picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "from_camera")

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)

        val startCamera = registerForActivityResult(
            StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                processOCR(uri)
            }
        }

        binding.btnInput.setOnClickListener {
            if (!checkCameraPermission()) {
                requestCameraPermission()
            } else {
                startCamera.launch(cameraIntent)
            }
        }
    }

    private fun initGallery() {

        val startGallery = registerForActivityResult(
            ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                processOCR(uri)
            }
        }

        binding.btnInput.setOnClickListener {
            startGallery.launch("image/*")
        }
    }

    private fun processOCR(uri: Uri?) {
        val image: InputImage
        try {
            image = uri?.let { InputImage.fromFilePath(applicationContext, it) }!!

            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { res ->
                    computeExpression(res.text)
                }
                .addOnFailureListener { e ->
                    AlertDialog.Builder(this)
                        .setTitle("Alert")
                        .setMessage("Failed to recognized text from image!")
                        .setNeutralButton("OK") { _, _ -> }
                        .create()
                        .show()
                }

        } catch (e: IOException) {
            Log.e(debugTag, "processOCR(): " + e.message)
        }
    }

    private fun computeExpression(expression: String) {
        val tokenizerCalculator = TokenizerCalculator()
        val result: Int
        val equation: String

        try {
            equation = tokenizerCalculator.groomExpression(expression)
            result = tokenizerCalculator.calculate(expression)
            binding.txtResult.text = if (result != Int.MAX_VALUE)
                "input: $equation\nresult: $result"
            else
                "Invalid expression! $equation"
        } catch (e: Exception) {
            binding.txtResult.text = "Invalid expression! $expression"
        }
    }
}