package com.bio.facerecognition

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Size
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bio.facesdk.FaceBox
import com.bio.facesdk.FaceDetectionParam
import com.bio.facesdk.FaceSDK
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.preview.Frame
import io.fotoapparat.preview.FrameProcessor
import io.fotoapparat.selector.LensPositionSelector
import io.fotoapparat.selector.autoFocus
import io.fotoapparat.selector.back
import io.fotoapparat.selector.continuousFocusPicture
import io.fotoapparat.selector.firstAvailable
import io.fotoapparat.selector.fixed
import io.fotoapparat.selector.front
import io.fotoapparat.selector.highestFps
import io.fotoapparat.selector.highestResolution
import io.fotoapparat.selector.off
import io.fotoapparat.selector.standardRatio
import io.fotoapparat.selector.wideRatio
import io.fotoapparat.view.CameraView

class CameraActivityKt : AppCompatActivity() {

    val TAG = CameraActivity::class.java.simpleName
    val PREVIEW_WIDTH = 720
    val PREVIEW_HEIGHT = 1280

    private lateinit var cameraView: CameraView
    private lateinit var faceView: FaceView
    private lateinit var fotoapparat: Fotoapparat
    private lateinit var context: Context
    private var activeCamera: Camera = Camera.FrontCamera
    private var cameraOrientation = 7

    private var recognized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_kt)

        context = this
        cameraView = findViewById(R.id.preview)
        faceView = findViewById(R.id.faceView)

        fotoapparat = Fotoapparat.with(this)
            .into(cameraView)
            .lensPosition(activeCamera.lensPosition)
            .frameProcessor(FaceFrameProcessor())
            .previewResolution { Resolution(PREVIEW_HEIGHT,PREVIEW_WIDTH) }
            .build()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        } else {
            fotoapparat.start()
        }

        findViewById<ImageButton>(R.id.btn_switch_camera).setOnClickListener {
            fotoapparat.stop()
            activeCamera = when (activeCamera) {
                Camera.FrontCamera -> Camera.BackCamera
                Camera.BackCamera -> Camera.FrontCamera
            }

            cameraOrientation = when (activeCamera) {
                Camera.FrontCamera -> 7
                Camera.BackCamera -> 6
            }
            fotoapparat = Fotoapparat.with(this)
                .into(cameraView)
                .lensPosition(activeCamera.lensPosition)
                .frameProcessor(FaceFrameProcessor())
                .previewResolution { Resolution(PREVIEW_HEIGHT,PREVIEW_WIDTH) }
                .build()

            val handler = Handler()

            handler.postDelayed({
                val faceBoxes: List<FaceBox>? = emptyList()
                faceView.setFaceBoxes(faceBoxes)
                fotoapparat.start()
            }, 1000)


        }
    }

    override fun onResume() {
        super.onResume()
        recognized = false
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fotoapparat.start()
        }
    }

    override fun onPause() {
        super.onPause()
        fotoapparat.stop()
        faceView.setFaceBoxes(null)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                fotoapparat.start()
            }
        }
    }

    inner class FaceFrameProcessor : FrameProcessor {

        override fun process(frame: Frame) {

            if(recognized == true) {
                return
            }

            val bitmap = FaceSDK.yuv2Bitmap(frame.image, frame.size.width, frame.size.height, cameraOrientation)

            val faceDetectionParam = FaceDetectionParam()
            faceDetectionParam.check_liveness = true
            faceDetectionParam.check_liveness_level = SettingsActivity.getLivenessModelType(context)
            val faceBoxes = FaceSDK.faceDetection(bitmap, faceDetectionParam)

            runOnUiThread {
                faceView.setFrameSize(Size(bitmap.width, bitmap.height))
                faceView.setFaceBoxes(faceBoxes)
            }

            if(faceBoxes.size > 0) {
                val faceBox = faceBoxes[0]
                if (faceBox.liveness > SettingsActivity.getLivenessThreshold(context)) {
                    val templates = FaceSDK.templateExtraction(bitmap, faceBox)

                    var maxSimiarlity = 0f
                    var maximiarlityPerson: Person? = null
                    for (person in DBManager.personList) {
                        val similarity = FaceSDK.similarityCalculation(templates, person.templates)
                        if (similarity > maxSimiarlity) {
                            maxSimiarlity = similarity
                            maximiarlityPerson = person
                        }
                    }
                    if (maxSimiarlity > SettingsActivity.getMatchThreshold(context)) {
                        recognized = true
                        val identifiedPerson = maximiarlityPerson
                        val identifiedSimilarity = maxSimiarlity

                        runOnUiThread {
                            val faceImage = Utils.cropFace(bitmap, faceBox)
                            val intent = Intent(context, ResultActivity::class.java)
                            intent.putExtra("identified_face", faceImage)
                            intent.putExtra("enrolled_face", identifiedPerson!!.face)
                            intent.putExtra("identified_name", identifiedPerson!!.name)
                            intent.putExtra("similarity", identifiedSimilarity)
                            intent.putExtra("liveness", faceBox.liveness)
                            intent.putExtra("yaw", faceBox.yaw)
                            intent.putExtra("roll", faceBox.roll)
                            intent.putExtra("pitch", faceBox.pitch)
                            startActivity(intent)
                        }
                    }
                }
            }
        }
    }
}


private sealed class Camera(
    val lensPosition: LensPositionSelector,
    val configuration: CameraConfiguration
) {
    object BackCamera : Camera(
        lensPosition = back(),
        configuration = CameraConfiguration(
            previewResolution = firstAvailable(
                wideRatio(highestResolution()),
                standardRatio(highestResolution())
            ),
            previewFpsRange = highestFps(),
            flashMode = off(),
            focusMode = firstAvailable(
                continuousFocusPicture(),
                autoFocus()
            ),
            frameProcessor = {
                // Do something with the preview frame
            }
        )
    )

    object FrontCamera : Camera(
        lensPosition = front(),
        configuration = CameraConfiguration(
            previewResolution = firstAvailable(
                wideRatio(highestResolution()),
                standardRatio(highestResolution())
            ),
            previewFpsRange = highestFps(),
            flashMode = off(),
            focusMode = firstAvailable(
                fixed(),
                autoFocus()
            )
        )
    )
}