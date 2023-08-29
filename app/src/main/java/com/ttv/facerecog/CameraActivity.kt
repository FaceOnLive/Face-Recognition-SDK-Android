package com.ttv.facerecog

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.icu.util.UniversalTimeScale.toLong
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.util.Size
import android.view.View
import android.view.ViewGroup
import com.ttv.face.*
import io.fotoapparat.Fotoapparat
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.preview.Frame
import io.fotoapparat.selector.front
import io.fotoapparat.view.CameraView
import io.fotoapparat.util.FrameProcessor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.ExecutorService

class CameraActivity : AppCompatActivity() {

    private val permissionsDelegate = PermissionsDelegate(this)
    private var hasPermission = false

    private var appCtx: Context? = null
    private var cameraView: CameraView? = null
    private var rectanglesView: FaceRectView? = null
    private var faceRectTransformer: FaceRectTransformer? = null
    private var frontFotoapparat: Fotoapparat? = null
    private var startVerifyTime: Long = 0
    private var mydb: DBHelper? = null
    private var recogName:String = ""
    private val LIVENESS_THRESHOLD: Float = 0.7f

    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            val i: Int = msg.what
            if (i == 0) {
                var drawInfoList = ArrayList<FaceRectView.DrawInfo>();
                var detectionResult = msg.obj as ArrayList<FaceResult>

                for(faceResult in detectionResult) {
                    var rect : Rect = faceRectTransformer!!.adjustRect(Rect(faceResult.left, faceResult.top, faceResult.right, faceResult.bottom));
                    var drawInfo : FaceRectView.DrawInfo;
                    if(faceResult.liveness > LIVENESS_THRESHOLD)
                        drawInfo = FaceRectView.DrawInfo(rect, 0, 0, 1, Color.GREEN, null);
                    else if(faceResult.liveness == -200.0f)
                        drawInfo = FaceRectView.DrawInfo(rect, 0, 0, 1, Color.YELLOW, null);
                    else
                        drawInfo = FaceRectView.DrawInfo(rect, 0, 0, 0, Color.RED, null);

                    drawInfoList.add(drawInfo);
                }

                rectanglesView!!.clearFaceInfo();
                rectanglesView!!.addFaceInfo(drawInfoList);
            } else if(i == 1) {
                var verifyResult = msg.obj as Int
                val intent = Intent()
                intent.putExtra("verifyResult", verifyResult);
                intent.putExtra("verifyName", recogName)
                setResult(RESULT_OK, intent)
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        appCtx = applicationContext
        cameraView = findViewById<View>(R.id.camera_view) as CameraView
        rectanglesView = findViewById<View>(R.id.rectanglesView) as FaceRectView

        mydb = DBHelper(appCtx)

        hasPermission = permissionsDelegate.hasPermissions()
        if (hasPermission) {
            cameraView!!.visibility = View.VISIBLE
        } else {
            permissionsDelegate.requestPermissions()
        }

        frontFotoapparat = Fotoapparat.with(this)
            .into(cameraView!!)
            .lensPosition(front())
            .frameProcessor(SampleFrameProcessor())
            .previewResolution { Resolution(1280,720) }
            .build()
    }

    override fun onStart() {
        super.onStart()
        if (hasPermission) {
            frontFotoapparat!!.start()
        }
    }


    override fun onStop() {
        super.onStop()
        if (hasPermission) {
            try {
                frontFotoapparat!!.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (permissionsDelegate.hasPermissions() && !hasPermission) {
            hasPermission = true
            cameraView!!.visibility = View.VISIBLE
            frontFotoapparat!!.start()
        } else {
            permissionsDelegate.requestPermissions()
        }
    }

    fun adjustPreview() : Boolean{
        if(faceRectTransformer == null) {
            val frameSize: Size = Size(1280, 720);
            if(cameraView!!.measuredWidth == 0)
                return false;

            var displayOrientation: Int = 90;
            adjustPreviewViewSize (cameraView!!,
                cameraView!!, rectanglesView!!,
                Size(frameSize.width, frameSize.height), displayOrientation, 1.0f);

            faceRectTransformer = FaceRectTransformer (
                frameSize.height, frameSize.width,
                cameraView!!.getLayoutParams().width, cameraView!!.getLayoutParams().height,
                0, 1, false,
                false,
                false);

            return true;
        }

        return true;
    }

    private fun adjustPreviewViewSize(
        rgbPreview: View,
        previewView: View,
        faceRectView: FaceRectView,
        previewSize: Size,
        displayOrientation: Int,
        scale: Float
    ): ViewGroup.LayoutParams? {
        val layoutParams = previewView.layoutParams
        val measuredWidth = previewView.measuredWidth
        val measuredHeight = previewView.measuredHeight
        layoutParams.width = measuredWidth
        layoutParams.height = measuredHeight

        faceRectView.layoutParams.width = measuredWidth
        faceRectView.layoutParams.height = measuredHeight
        return layoutParams
    }

    /* access modifiers changed from: private */ /* access modifiers changed from: public */
    private fun sendMessage(w: Int, o: Any) {
        val message = Message()
        message.what = w
        message.obj = o
        mHandler.sendMessage(message)
    }

    inner class SampleFrameProcessor : FrameProcessor {
        var frThreadQueue: LinkedBlockingQueue<Runnable>? = null
        var frExecutor: ExecutorService? = null

        init {
            frThreadQueue = LinkedBlockingQueue<Runnable>(1)
            frExecutor = ThreadPoolExecutor(
                1, 1, 0, TimeUnit.MILLISECONDS, frThreadQueue
            ) { r: Runnable? ->
                val t = Thread(r)
                t.name = "frThread-" + t.id
                t
            }
        }

        override fun invoke(frame: Frame) {
            val faceResults:List<FaceResult> = FaceEngine.getInstance().detectFaceFromYuv(frame.image, frame.size.width, frame.size.height, 7)
            if(faceResults.count() > 0) {
                if(frThreadQueue!!.remainingCapacity() > 0 && MainActivity.userLists.size > 0) {
                    frExecutor!!.execute(
                        FaceRecognizeRunnable(
                            frame.image,
                            frame.size.width,
                            frame.size.height,
                            faceResults
                        )
                    )
                }
            }

            if(adjustPreview())
                sendMessage(0, faceResults)

        }
    }

    inner class FaceRecognizeRunnable(nv21Data_: ByteArray, width_: Int, height_: Int, faceResults_:List<FaceResult>) : Runnable {
        val nv21Data: ByteArray
        val width: Int
        val height: Int
        val faceResults: List<FaceResult>

        init {
            nv21Data = nv21Data_
            width = width_
            height = height_
            faceResults = faceResults_
        }

        override fun run() {
            if(startVerifyTime == 0.toLong())
                startVerifyTime = System.currentTimeMillis()

            FaceEngine.getInstance().extractFeatureFromYuv(nv21Data, width, height, 7, faceResults)

            var exists = false
            var maxScore = 0.0f
            var maxScoreName: String = ""
            for(user in MainActivity.userLists) {
                val score = FaceEngine.getInstance().compareFeature(user.feature, faceResults.get(0).feature)
                if(maxScore < score) {
                    maxScore = score
                    maxScoreName = user.userName
                }
            }

            if(maxScore > 78 && faceResults.get(0).liveness > LIVENESS_THRESHOLD) {
                exists = true
                recogName = maxScoreName
            }

            if(exists == true) {
                sendMessage(1, 1)   //success
            } else {
                var result = 0
                if(faceResults.get(0).liveness < LIVENESS_THRESHOLD)
                    result = -1;

                if(System.currentTimeMillis() - startVerifyTime > 3000) {
                    sendMessage(1, result)   //fail
                }
            }
        }
    }
}