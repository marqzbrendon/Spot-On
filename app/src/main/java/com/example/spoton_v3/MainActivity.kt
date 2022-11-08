@file:Suppress("DEPRECATION")

package com.example.spoton_v3

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.spoton_v3.ml.ModelMetadata
import org.tensorflow.lite.support.image.TensorImage

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private val cameraRequest = 1888
    lateinit var imageView: ImageView
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "Spot On"

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraRequest)
        imageView = findViewById(R.id.imageView)
        val photoButton: Button = findViewById(R.id.button)
        photoButton.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, cameraRequest)

        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == cameraRequest) {
            val photo: Bitmap = data?.extras?.get("data") as Bitmap
            imageView.setImageBitmap(photo)
            predict(photo)

//            val localModel = LocalModel.Builder()
//                .setAssetFilePath("ml/model_metadata.tflite")
//                // or .setAbsoluteFilePath(absolute file path to model file)
//                // or .setUri(URI to model file)
//                .build()
//
//            val optionsBuilder = CustomImageLabelerOptions.Builder(localModel)
//                .setMaxResultCount(1)
//                .build()
//
//            val labeler = ImageLabeling.getClient(optionsBuilder
//            )
//
//
//            val image = InputImage.fromBitmap(photo, 0)
//            labeler.process(image)
//                .addOnSuccessListener { labels ->
//                    // Task completed successfully
//                    // ...
//                    Log.d("Success", "Success")
//                    for (label in labels) {
//                        val text = label.text
//                        val confidence = label.confidence
//                        val index = label.index
//                    }
//                }
//                .addOnFailureListener { e ->
//                    // Task failed with an exception
//                    // ...
//                    Log.e("Failure", "$e")
//                }
        }
    }

    private fun predict(inputImage: Bitmap): Any {

        val model = ModelMetadata.newInstance(this)

        // Creates inputs for reference.
        val image = TensorImage.fromBitmap(inputImage)

        // Runs model inference and gets result.
//        val outputs = model.process(image)
//        val classification = outputs.classificationAsCategoryList
//        Log.d("output", "model results = $classification")
        data class Pred(val aircraft: String = "", val score: Float = 0.0F)
        val outputs = model.process(image)
        val classification = outputs.classificationAsCategoryList.maxBy { it.score }
        val prediction = Pred(
            aircraft = classification!!.label,

            classification.score)
        Log.d("pred", "my prediction = ${prediction.aircraft}, ${prediction.score}")
        // Releases model resources if no longer used.
        model.close()

        return prediction
    }
}

