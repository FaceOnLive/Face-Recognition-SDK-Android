package com.ttv.facerecog

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.ttv.face.*
import com.ttv.imageutil.TTVImageFormat
import com.ttv.imageutil.TTVImageUtil
import com.ttv.imageutil.TTVImageUtilError
import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.ArrayList

class MainActivity : AppCompatActivity(){
    companion object {
        lateinit var userLists: ArrayList<FaceEntity>
    }

    private var context: Context? = null
    private var mydb: DBHelper? = null

    init {
        userLists = ArrayList(0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        context = this
        FaceEngine.getInstance(this).setActivation("")
        FaceEngine.getInstance(this).init(2)
        mydb = DBHelper(this)
        mydb!!.getAllUsers()

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        btnRegister.setOnClickListener {
            val intent = Intent()
            intent.setType("image/*")
            intent.setAction(Intent.ACTION_PICK)
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1)
        }

        val btnVerify = findViewById<Button>(R.id.btnVerify)
        btnVerify.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivityForResult(intent, 2)
        }
        btnVerify.isEnabled = false;

        val btnUsers = findViewById<Button>(R.id.btnUser)
        btnUsers.setOnClickListener {
            val intent = Intent(this, UserActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        findViewById<Button>(R.id.btnVerify).isEnabled = userLists.size > 0
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            try {
                var bitmap: Bitmap = ImageRotator.getCorrectlyOrientedImage(this, data?.data!!)
                val faceResults:MutableList<FaceResult> = FaceEngine.getInstance(this).detectFace(bitmap)
                if(faceResults.count() == 1) {
                    FaceEngine.getInstance(this).extractFeature(bitmap, true, faceResults)

                    val userName = String.format("User%03d", userLists.size + 1)
                    val cropRect = Utils.getBestRect(bitmap.width, bitmap.height, faceResults.get(0).rect)
                    val headImg = Utils.crop(bitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height(), 120, 120)

                    val inputView = LayoutInflater.from(context)
                        .inflate(R.layout.dialog_input_view, null, false)
                    val editText = inputView.findViewById<EditText>(R.id.et_user_name)
                    val ivHead = inputView.findViewById<ImageView>(R.id.iv_head)
                    ivHead.setImageBitmap(headImg)
                    editText.setText(userName)
                    val confirmUpdateDialog: AlertDialog = AlertDialog.Builder(context!!)
                        .setView(inputView)
                        .setPositiveButton(
                            "OK", null
                        )
                        .setNegativeButton(
                            "Cancel", null
                        )
                        .create()
                    confirmUpdateDialog.show()
                    confirmUpdateDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener { v: View? ->
                            val s = editText.text.toString()
                            if (TextUtils.isEmpty(s)) {
                                editText.error = application.getString(R.string.name_should_not_be_empty)
                                return@setOnClickListener
                            }

                            var exists:Boolean = false
                            for(user in userLists) {
                                if(TextUtils.equals(user.userName, s)) {
                                    exists = true
                                    break
                                }
                            }

                            if(exists) {
                                editText.error = application.getString(R.string.duplicated_name)
                                return@setOnClickListener
                            }

                            val user_id = mydb!!.insertUser(s, headImg, faceResults.get(0).feature)
                            val face = FaceEntity(user_id, s, headImg, faceResults.get(0).feature)
                            userLists.add(face)

                            val faceFeatureInfo = FaceFeatureInfo(
                                user_id,
                                faceResults.get(0).feature
                            )

                            FaceEngine.getInstance(this).registerFaceFeature(faceFeatureInfo)

                            confirmUpdateDialog.cancel()

                            findViewById<Button>(R.id.btnVerify).isEnabled = userLists.size > 0
                            Toast.makeText(this, "Register succeed!", Toast.LENGTH_SHORT).show()
                        }

                } else if(faceResults.count() > 1) {
                    Toast.makeText(this, "Multiple face detected!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No face detected!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: java.lang.Exception) {
                //handle exception
                e.printStackTrace()
            }
        } else if(requestCode == 2 && resultCode == RESULT_OK) {
            val verifyResult = data!!.getIntExtra ("verifyResult", 0)
            val recogName = data!!.getStringExtra ("verifyName")
            if(verifyResult == 1) {
                Toast.makeText(this, "Verify succeed! " + recogName, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Verify failed!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}