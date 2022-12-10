@file:Suppress("DEPRECATION")

package com.example.spoton_v3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.spoton_v3.ml.ModelMetadata
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import org.tensorflow.lite.support.image.TensorImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.LocalDateTime


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private val cameraRequest = 1888
    private val db = Firebase.firestore
    private val storage = Firebase.storage
    data class Pred(val aircraft: String = "", val score: Float = 0.0F)


    // companion object
    companion object {
        private const val IMAGE_CHOOSE = 1000
        private const val PERMISSION_CODE = 1001
        //private const val REQUEST_CODE = 13
        private const val TAG = "DocSnippets"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "Spot On"

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraRequest)
//        imageView = findViewById(R.id.imageView)
        val photoButton: Button = findViewById(R.id.button)

        photoButton.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, cameraRequest)

        }


        val btnChoosePhoto: Button = findViewById(R.id.chooseButton)
        btnChoosePhoto.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_DENIED){
                    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    requestPermissions(permissions, PERMISSION_CODE)
                } else{
                    chooseImageGallery()
                }
            }else{
                chooseImageGallery()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == cameraRequest) {
            if (data != null) {
                if (data.extras != null) {
                    val photo: Bitmap = data.extras?.get("data") as Bitmap
                    predict(photo)
                }
            }

        }
        if (requestCode == IMAGE_CHOOSE){
            val uri = data?.data
            if (uri != null) {
                val photo = uriToBitmap(uri)
                predict(photo)
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun predict(inputImage: Bitmap) {

        val model = ModelMetadata.newInstance(this)

        // Creates inputs for reference.
        val image = TensorImage.fromBitmap(inputImage)

        // Runs model inference and gets result.
        val outputs = model.process(image)
        val classification = outputs.classificationAsCategoryList.maxBy { it.score }
        val prediction = Pred(
            aircraft = classification!!.label,
            classification.score)
        Log.d("pred", "my prediction = ${prediction.aircraft}, ${prediction.score}")

        val bundle = Bundle()

        val intent = Intent(this, ReturnActivity::class.java)
        addToDb(prediction, inputImage)
        bundle.putString("aircraft", prediction.aircraft)
        bundle.putFloat("score", prediction.score)
        intent.putExtras(bundle)

        // Releases model resources if no longer used.
        model.close()

        // start your next activity
        startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addToDb(prediction: Pred, image: Bitmap) {

        val current = LocalDateTime.now()
        val pred = hashMapOf(
            "aircraft" to prediction.aircraft,
            "score" to prediction.score,
            "time" to current
        )

        // Add a new document with a generated ID
        db.collection("predictions")
            .add(pred)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DB DocumentSnapshot added with ID: ${documentReference.id}")

                // Create a storage reference from our app
                val storageRef = storage.reference

                // Create a reference to "mountains.jpg"
                val aircraftRef = storageRef.child(documentReference.id)

                // Create a reference to 'images/mountains.jpg'
                //val aircraftImagesRef = storageRef.child("{images/${documentReference.id}}")

                val uploadTask = aircraftRef.putBytes(convertBitmapToByteArray(image)!!)
                uploadTask.addOnFailureListener {
                    Log.w(TAG, "Upload error: ${it.message}")
                    // Handle unsuccessful uploads
                }.addOnSuccessListener {
                    // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                    Log.w(TAG, "Upload Successful")
                }

            }
            .addOnFailureListener { e ->
                Log.w(TAG, "DB Error adding document", e)
            }
    }

    private fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray? {
        var baos: ByteArrayOutputStream? = null
        return try {
            baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            baos.toByteArray()
        } finally {
            if (baos != null) {
                try {
                    baos.close()
                } catch (e: IOException) {
                    Log.e(
                        TAG,
                        "ByteArrayOutputStream was not closed"
                    )
                }
            }
        }
    }

    private fun chooseImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_CHOOSE)
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        return MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    chooseImageGallery()
                }else{
                    Toast.makeText(this,"Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

