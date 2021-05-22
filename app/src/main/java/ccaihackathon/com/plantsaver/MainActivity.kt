package ccaihackathon.com.plantsaver

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.create
import java.io.ByteArrayOutputStream
import java.io.File


class MainActivity : AppCompatActivity() {
    private lateinit var mClassifier: Classifier
    private lateinit var mBitmap: Bitmap

    companion object {
        private const val MY_PERMISSIONS_REQUEST: Int = 100
        private const val TAG = "EditProfileFragment"
        private val PICK_IMAGE_FROM_GALLERY_REQUEST = 1

    }
    var code = 1
    private val mCameraRequestCode = 0
    private val mInputSize = 200 //224
    private val mModelPath = "model.tflite"
    private val mLabelPath = "labels.txt"

    var authStateListener: AuthStateListener? = null
    var mAuth: FirebaseAuth? = null


    override fun onStart() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        Log.e("ak47", "on Start")
        super.onStart()
        Log.e("ak47", "on Start after super")
        mAuth!!.addAuthStateListener(authStateListener!!)
        Log.e("ak47", "on Start Ends")
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("menuclicked", "yes")
        if (item.itemId == R.id.signout) {
            mAuth!!.signOut()

        }
        return super.onOptionsItemSelected(item)
    }

    fun logout(v: MenuItem?) {
       mAuth?.signOut()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main)
        mClassifier = Classifier(assets, mModelPath, mLabelPath, mInputSize)
        mAuth = FirebaseAuth.getInstance()


        authStateListener = AuthStateListener { firebaseAuth: FirebaseAuth ->
            if (firebaseAuth.currentUser == null) {
                Log.e("ak47", "user null")
                val intent = Intent(this, SignInActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        mCameraButton.setOnClickListener {

            code =1

            val permissions = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            if (ContextCompat.checkSelfPermission(
                    this!!,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this!!,
                    permissions,
                    MY_PERMISSIONS_REQUEST
                )
            }
            else
            {
                val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(callCameraIntent, mCameraRequestCode)
            }

        }
        mResultTextView.setOnClickListener{
          /*  val callDetailsIntent = Intent(this, DetailsActivity::class.java)
            startActivity(callDetailsIntent)*/
        }

        mGalleryButton.setOnClickListener {

            code = 0
            val permissions = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            if (ContextCompat.checkSelfPermission(
                    this!!,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this!!,
                    permissions,
                    MY_PERMISSIONS_REQUEST
                )
            }
            else
            {

                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(
                    Intent.createChooser(intent, "Select Picture"),
                    PICK_IMAGE_FROM_GALLERY_REQUEST
                )
            }

        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Toast.makeText(this!!, "Permission Granted", Toast.LENGTH_SHORT).show()

                    if (code == 1) {
                        val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        startActivityForResult(callCameraIntent, mCameraRequestCode)
                    } else {
                        val intent = Intent()
                        intent.type = "image/*"
                        intent.action = Intent.ACTION_GET_CONTENT
                        startActivityForResult(
                            Intent.createChooser(intent, "Select Picture"),
                            PICK_IMAGE_FROM_GALLERY_REQUEST
                        )
                    }

                } else {
                    Toast.makeText(this!!, "Please grant permission", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == mCameraRequestCode){
            if(resultCode == Activity.RESULT_OK && data != null) {
                mBitmap = data.extras!!.get("data") as Bitmap
                mBitmap = scaleImage(mBitmap)
                mPhotoImageView.setImageBitmap(mBitmap)
                val tempUri = getImageUri(applicationContext, mBitmap)

                val finalFile = File(getRealPathFromURI(tempUri))
                postRequest(tempUri)
            }
        }
        if (requestCode == PICK_IMAGE_FROM_GALLERY_REQUEST && resultCode == Activity.RESULT_OK) {


            val uri = data?.data
            val bitmap = Images.Media.getBitmap(this.contentResolver, uri)
            mPhotoImageView.setImageBitmap(bitmap)
            postRequest(uri)
        }
    }

    fun getImageUri(inContext: Context, inImage: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null)
        return Uri.parse(path)
    }

    fun getRealPathFromURI(uri: Uri?): String? {
        var path = ""
        if (contentResolver != null) {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            if (cursor != null) {
                cursor.moveToFirst()
                val idx: Int = cursor.getColumnIndex(Images.ImageColumns.DATA)
                path = cursor.getString(idx)
                cursor.close()
            }
        }
        return path
    }

    private fun postRequest(fileUri: Uri?)
    {
        val originalFile : File = HttpFileUtils.getFile(this, fileUri)
        val filePart = RequestBody.create(
            this?.contentResolver?.getType(
                fileUri!!
            )?.toMediaTypeOrNull(), originalFile
        )

        val file = MultipartBody.Part.createFormData("file", originalFile.name, filePart)

        RetrofitClientInstance.getRetrofit(this)
            ?.create<LoginInterface>()
            ?.sendData(file)
            ?.enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {

                    Log.d("cheks", "error: " + t.message)
                    Toast.makeText(this@MainActivity, "Failure " + t.message, Toast.LENGTH_LONG)
                        .show()
                }

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {

                    mResultTextView.text = response.body()!!.string()
                }
            })
    }

    fun scaleImage(bitmap: Bitmap?): Bitmap {
        val width = bitmap!!.width
        val height = bitmap.height
        val scaledWidth = mInputSize.toFloat() / width
        val scaledHeight = mInputSize.toFloat() / height
        val matrix = Matrix()
        matrix.postScale(scaledWidth, scaledHeight)
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }
}

