/*
 * Copyright (C) 2010 ZXing authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package qrcode.camera;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

final class PreviewCallback implements Camera.PreviewCallback {
    private static final String TAG = PreviewCallback.class.getName();
    private final CameraConfigurationManager mConfigManager;
    private Handler mPreviewHandler;
    private int mPreviewMessage;

    PreviewCallback(CameraConfigurationManager configManager) {
        this.mConfigManager = configManager;
    }

    void setHandler(Handler previewHandler, int previewMessage) {
        this.mPreviewHandler = previewHandler;
        this.mPreviewMessage = previewMessage;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Size cameraResolution = mConfigManager.getCameraResolution();
        if (mPreviewHandler != null) {
            Message message =
                mPreviewHandler.obtainMessage(mPreviewMessage, cameraResolution.width, cameraResolution.height, data);
            message.sendToTarget();
            mPreviewHandler = null;
        } else {
            Log.v(TAG, "no handler callback.");
        }
    }

}
