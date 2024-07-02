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
                "  \"licenseKey\": \"eyJQcm9kdWN0SWQiOjIwNzg1LCJJRCI6OCwiS2V5IjoiRFJFQUQtR0NCT04tWExJSlctVU5BQ00iLCJDcmVhdGVkIjoxNzAwNTM2Mjc3LCJFeHBpcmVzIjoxNzA2NzAyOTM2LCJQZXJpb2QiOjIzLCJGMSI6dHJ1ZSwiRjIiOmZhbHNlLCJGMyI6ZmFsc2UsIkY0IjpmYWxzZSwiRjUiOmZhbHNlLCJGNiI6ZmFsc2UsIkY3IjpmYWxzZSwiRjgiOmZhbHNlLCJOb3RlcyI6bnVsbCwiQmxvY2siOmZhbHNlLCJHbG9iYWxJZCI6NDAxNTU0LCJDdXN0b21lciI6bnVsbCwiQWN0aXZhdGVkTWFjaGluZXMiOlt7Ik1pZCI6ImNvbS5vbGFtLnNtYXJ0ZmFybXMiLCJJUCI6Ijk2LjQ0LjE2MS40IiwiVGltZSI6MTcwMDUzNjM4N31dLCJUcmlhbEFjdGl2YXRpb24iOmZhbHNlLCJNYXhOb09mTWFjaGluZXMiOjIsIkFsbG93ZWRNYWNoaW5lcyI6IiIsIkRhdGFPYmplY3RzIjpbXSwiU2lnbkRhdGUiOjE3MDQ3MTU3NTF9\",\n" +
                "  \"signature\": \"kR1Fse5b+P1sQiz3nuyqcyHAIcUScB+skqTt5+mIpPj3Ac/3+WRQkEsTdVXNelFkNqd7yb/F4zFrotJMJ1b3kyS4hn8j0IHrHF6pA32aw7VbDVgRyUAJWbDbzOAp8dRy7NlMPy3TPUOVDL5bi7UaH7NBp4nhfw/XrSGQEKqq+0EBvErUhRw7bRQHEjHfzbnyanxpWp6LWjo+5RXUWYWXC3bMu4UgBF/RRJUMRc+7YFUL1t6Ss8I3ihmLqMtFoNXmwqrdGZF+RsFlP/7yc2SPPnSbxjSwE6yQAPTjbC85vJFYm2G6p948Y7Wsh1pC6hGRGetyjwd9EA09rnxGsfNm1w==\",\n" +
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

            when (ret) {
                FaceEngine.F_LICENSE_APPID_ERROR -> {
                    txtInit.text = "AppID Error!"
                }
                FaceEngine.F_LICENSE_KEY_ERROR -> {
                    txtInit.text = "License Key Error!"
                }
                FaceEngine.F_LICENSE_EXPIRED -> {
                    txtInit.text = "License Expired!"
                }
                FaceEngine.F_INIT_ERROR -> {
                    txtInit.text = "Init Error!"
                }
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
        }
        else if(requestCode == 2 && resultCode == RESULT_OK) {
            val verifyResult = data!!.getIntExtra ("verifyResult", 0)
            when (verifyResult) {
                1 -> {
                    val  verifyUserId = data.getIntExtra("verifyId",0)
                    val  verifyName = data.getStringExtra("verifyName")?:""
                    Toast.makeText(this, "Verify succeed! for user Id is $verifyUserId $verifyName", Toast.LENGTH_SHORT).show()
                }
                -1 -> {
                    Toast.makeText(this, "Liveness failed!", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "Verify failed!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}