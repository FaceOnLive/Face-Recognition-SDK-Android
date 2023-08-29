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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.ttv.face.*
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
        FaceEngine.createInstance(this)
        var ret = FaceEngine.getInstance().setActivation("{\n" +
                "  \"licenseKey\": \"eyJQcm9kdWN0SWQiOjIwNzg1LCJJRCI6MSwiS2V5IjoiTVlMQ08tU0JVREQtUFhRSlctSklNRVoiLCJDcmVhdGVkIjoxNjg4NTI3Mzc0LCJFeHBpcmVzIjoxNzA1ODA3Mzc0LCJQZXJpb2QiOjIwMCwiRjEiOnRydWUsIkYyIjpmYWxzZSwiRjMiOmZhbHNlLCJGNCI6ZmFsc2UsIkY1IjpmYWxzZSwiRjYiOmZhbHNlLCJGNyI6ZmFsc2UsIkY4IjpmYWxzZSwiTm90ZXMiOm51bGwsIkJsb2NrIjpmYWxzZSwiR2xvYmFsSWQiOjM1OTQxMSwiQ3VzdG9tZXIiOm51bGwsIkFjdGl2YXRlZE1hY2hpbmVzIjpbeyJNaWQiOiJjb20udHR2LmZhY2VkZW1vIiwiSVAiOiI4OC45OS4xNDUuNyIsIlRpbWUiOjE2ODg1Mjc0NDR9XSwiVHJpYWxBY3RpdmF0aW9uIjpmYWxzZSwiTWF4Tm9PZk1hY2hpbmVzIjoxLCJBbGxvd2VkTWFjaGluZXMiOiIiLCJEYXRhT2JqZWN0cyI6W10sIlNpZ25EYXRlIjoxNjg4NTI3NDQ0fQ==\",\n" +
                "  \"signature\": \"DzA6tC4ByQqtmJvLltg8eDNsGUNZLqV2vEjwSZESjDFPQnI0QN/h+s8Pr0teZ0fg5rfEQ6JV0YXy0crB3k8OXLYLHaqKWZG/gQp4XSzj7o+2lFZWFQMqsyBb/0c8fZx3AoILEqGa8xoDWiibE21aeAVfdAUaGODBZnFRA9ytdsvesInyyzKZmjtHjKDR0MLgIBk3QJKxXaI+RU2NoV7UDrruOoGXeljvjjiwOispmxGPTYri9QkDeQyGcmer3+BeAFKmvd6xWOzHGp53wv7VYIC42ETU6hxvVZqAFJ2ujvtvzIzpZ615Qq1eVyFljCU2u0Clfb2oTsoefyKhLlLQTQ==\",\n" +
                "  \"result\": 0,\n" +
                "  \"message\": \"\"\n" +
                "}")

        Log.e("TestEngine", "activation ret: " + ret)

        if(ret == FaceEngine.F_OK) {
            ret = FaceEngine.getInstance().init()

            mydb = DBHelper(this)
            mydb!!.getAllUsers()
        }

        if(ret != FaceEngine.F_OK) {
            val txtInit = findViewById<TextView>(R.id.txtInit)
            txtInit.visibility = View.VISIBLE

            if(ret == FaceEngine.F_LICENSE_APPID_ERROR) {
                txtInit.text = "AppID Error!"
            } else if(ret == FaceEngine.F_LICENSE_KEY_ERROR) {
                txtInit.text = "License Key Error!"
            } else if(ret == FaceEngine.F_LICENSE_EXPIRED) {
                txtInit.text = "License Expired!"
            } else if(ret == FaceEngine.F_INIT_ERROR) {
                txtInit.text = "Init Error!"
            }
        }

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

        val btnUsers = findViewById<Button>(R.id.btnUser)
        btnUsers.setOnClickListener {
            val intent = Intent(this, UserActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            try {
                var bitmap: Bitmap = ImageRotator.getCorrectlyOrientedImage(this, data?.data!!)
                val faceResults:MutableList<FaceResult> = FaceEngine.getInstance().detectFaceFromBitmap(bitmap)

                if(faceResults.count() == 1) {

                    FaceEngine.getInstance().extractFeatureFromBitmap(bitmap, faceResults)

                    val userName = String.format("User%03d", mydb!!.lastUserId + 1)
                    val cropRect = Utils.getBestRect(bitmap.width, bitmap.height, Rect(faceResults.get(0).left, faceResults.get(0).top, faceResults.get(0).right, faceResults.get(0).bottom))
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

                            confirmUpdateDialog.cancel()

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
            if(verifyResult == 1) {
                Toast.makeText(this, "Verify succeed!", Toast.LENGTH_SHORT).show()
            } else if(verifyResult == -1) {
                Toast.makeText(this, "Liveness failed!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Verify failed!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}