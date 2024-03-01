✨ _**Update (Mar 1, 2024):**_
Introduce **OpenKYC Community Project**, build your own eKYC solution and contribute at this [Github Repo](https://github.com/FaceOnLive/OpenKYC), try at [HuggingFace Space](https://huggingface.co/spaces/FaceOnLive/OpenKYC).

✨ _**Update (Mar 1, 2024):**_
Introduce **OpenKYC Community Project**, build your own eKYC solution and contribute at this [Github Repo](https://github.com/FaceOnLive/OpenKYC), try at [HuggingFace Space](https://huggingface.co/spaces/FaceOnLive/OpenKYC).

✨ _**Update (Oct 26, 2023):**_
We are on Hugging Face, get your own SDK Servers by duplicating spaces at https://huggingface.co/FaceOnLive.

✨ _**Update (Apr 10, 2023):**_
Our API portal is now live, offering free APIs for various AI solutions, including face recognition, liveness detection, and ID document recognition.<br/>
Make sure to check it out at https://getapi.faceonlive.com and take advantage of our free offerings.
<h1 align="center">Mask-Aware Face Recognition SDK For Android</h1>
<p align="center">NIST FRVT Certified, Fast, Accurate, Mask-Aware Face Recognition SDK with Liveness Detection</p>

<p align="center">
<a target="_blank" href="mailto:contact@faceonlive.com"><img src="https://img.shields.io/badge/email-contact@faceonlive.com-blue.svg?logo=gmail " alt="www.faceonlive.com"></a>&emsp;
<a target="_blank" href="https://t.me/faceonlive"><img src="https://img.shields.io/badge/telegram-@faceonlive-blue.svg?logo=telegram " alt="www.faceonlive.com"></a>&emsp;
<a target="_blank" href="https://wa.me/+17074043606"><img src="https://img.shields.io/badge/whatsapp-faceonlive-blue.svg?logo=whatsapp " alt="www.faceonlive.com"></a>&emsp;
<a target="_blank" href="https://huggingface.co/spaces/FaceOnLive/Face-Recognition-SDK"><img src="https://img.shields.io/badge/%F0%9F%A4%97%20Hugging%20Face-Spaces-blue" alt="www.faceonlive.com"></a>
</p>

## :muscle:  Partnership / Cooperation / Project Discussion
&emsp;<a href="mailto:contact@faceonlive.com?subject=[GitHub]%20Face%20Recognition%20SDK%20Android"><img src="https://img.shields.io/badge/mail-%23DD0031.svg?&style=flat&logo=gmail&logoColor=white"  height="64"/></a>
</br>
</br>

## :tada:  Try It Yourself
<a href="https://play.google.com/store/apps/details?id=com.ttv.facedemo" target="_blank">
  <img alt="Get it on Google Play" src="https://goo.gl/cR2qQH" height="100"/>
</a>
</br>
</br>

#
Integrated into [Huggingface Spaces 🤗](https://huggingface.co/spaces) using [Gradio](https://github.com/gradio-app/gradio). Try out the Web Demo: [![Hugging Face Spaces](https://img.shields.io/badge/%F0%9F%A4%97%20Hugging%20Face-Spaces-blue)](https://huggingface.co/spaces/FaceOnLive/Face-Recognition-SDK)
#

https://user-images.githubusercontent.com/91896009/137511577-57d95888-0157-44aa-98d6-9fda2154d638.mp4


## :clap:  Supporters
[![Stargazers repo roster for @faceonlive/Face-Recognition-SDK-Android](https://reporoster.com/stars/faceonlive/Face-Recognition-SDK-Android)](https://github.com/faceonlive/Face-Recognition-SDK-Android/stargazers)
[![Forkers repo roster for @faceonlive/Face-Recognition-SDK-Android](https://reporoster.com/forks/faceonlive/Face-Recognition-SDK-Android)](https://github.com/faceonlive/Face-Recognition-SDK-Android/network/members)


## 🏃  Usage SDK
### 1. Setup Environment
- Create New Project with Android Studio.

- Place SDK library ttvface.aar into app / libs.

- Include SDK library to app / build.gradle file.
```
dependencies {
    ...
    implementation files('libs/ttvface.aar')
    ...
}
```
- Import library in code and call functions.
```
import com.ttv.face.FaceEngine;

...
// Set License (To obtain license, please contact us)
FaceEngine.getInstance(this).setActivation(license);

// Init with Max Detectable Face Count
FaceEngine.getInstance(this).init(8);

// Detect Face
List<FaceResult> faceInfoList = new ArrayList<>();
faceInfoList = FaceEngine.getInstance(this).detectFace(bitmap);
```
### 2. Error Code
```
F_OK = 0,
F_LICENSE_KEY_ERROR = -1,
F_LICENSE_APPID_ERROR = -2,
F_LICENSE_EXPIRED = -3,
F_INIT_ERROR = -4,
```
### 3. Classes
#### - FaceResult
  | Type      | Name      | Description |
  |------------------|------------------|------------------|
  | Rect         | rect        | Face rectangle coordinates   |
  | int          | liveness        | Liveness status: 0 for spoof, 1 for real, less than 0 for unknown    |
  | int          | gender        | Gender classification result   |
  | int          | mask        | Mask presence: 0 for no mask, 1 for mask    |
  | int          | age        | Age estimation result    |
  | float          | yaw        |  Yaw angle: -45 to 45 degrees  |
  | float          | roll        | Roll angle: -45 to 45 degrees    |
  | float          | pitch        | Pitch angle: -45 to 45 degrees    |
  | byte[]          | feature        |  2056-byte facial feature data   |
  | byte[]          | faceData        | Encrypted facial data     |
  | int          | orient        | Face orientation: 1 for no rotation, 2 for 90° rotation, 3 for 270° rotation, 4 for 180° rotation     |
  | int          | faceId        | Face ID in the tracking face mode    |

  ```
  public class FaceResult {
      public Rect rect;
      public int liveness;
      public int gender;
      public int mask;
      public int age;
      public float yaw;
      public float roll;
      public float pitch;
      public byte[] feature;
      public byte[] faceData;
      public int    orient;
      public int faceId;
      
      public FaceResult() {
      }
  }
  ```
### 4. API Introduction
#### - Set Activation
  ```
  int setActivation(String license)
  ```

  | Name      | Description |
  |------------------|------------------|
  | Function        | Activate SDK with license input    |
  | Parameters        | 0: license String    |
  | Return Value        | F_OK: Succeed, Failed otherwise    |
  ```
  import com.ttv.face.FaceEngine;
  ...
  int activated = FaceEngine.getInstance(this).setActivation(license);
  ```

#### - Detect faces from bitmap object
  ```
  List<FaceResult> detectFace(Bitmap bitmap)
  ```
  | Name      | Description |
  |------------------|------------------|
  | Function        | Get Detection Result from Bitmap image    |
  | Parameters        | Android Bitmap format Image    |
  | Return Value        | List of FaceResult    |
  ```
  import com.ttv.face.FaceEngine;
  ...
  List<FaceResult> detectResult = FaceEngine.getInstance(this).detectFace(bitmap);
  ```
#### - Detect faces from yuv data
  ```
  List<FaceResult> detectFace(byte[] nv21, int width, int height)
  ```
  | Name      | Description |
  |------------------|------------------|
  | Function        | Get Detection Result from Bitmap image    |
  | Parameters        | NV21 YUV ByteArray with it's width and height    |
  | Return Value        | List of FaceResult    |
  ```
  import com.ttv.face.FaceEngine;
  ...
  List<FaceResult> detectResult = FaceEngine.getInstance(this).detectFace(nv21, width, height);
  ```
#### - Extract feature with the face detection result
  ```
  int extractFeature(Bitmap bitmap, boolean isRegister, List<FaceResult> faceResults)
  ```
  | Name      | Description |
  |------------------|------------------|
  | Function        | Extract Face Feature from Detected Face    |
  | Parameters        | Android Bitmap format Image, extract feature for register or not    |
  | Return Value        | 0: Succeed, -1: Failed<br/> If successful, the feature should be get the feature in FaceResult    |
  ```
  import com.ttv.face.FaceEngine;
  ...
  List<FaceResult> detectResult = FaceEngine.getInstance(this).extractFeature(bitmap, isRegister, faceResults);
  ```
#### - Extract feature with the face detection result
  ```
  int extractFeature(byte[] nv21, int width, int height, boolean isRegister, List faceResults)
  ```
  | Name      | Description |
  |------------------|------------------|
  | Function        | Extract Face Feature from Detected Face    |
  | Parameters        | NV21 YUV ByteArray with Width and Height, extract feature for register or not    |
  | Return Value        | 0: Succeed, -1: Failed<br/> If successful, the feature should be get the feature in FaceResult    |
  ```
  import com.ttv.face.FaceEngine;
  ...
  List<FaceResult> detectResult = FaceEngine.getInstance(this).extractFeature(bitmap, isRegister, faceResults);
  ```
#### - Compare similarity between two faces
  ```
  float compareFace(byte[] feat1, byte[] feat2)
  ```
  | Name      | Description |
  |------------------|------------------|
  | Function        | Check two faces are same or not from Feat Array    |
  | Parameters        | Face Features to compare   |
  | Return Value        | Score of Face Comparison (Threshold for same faces 0.82)    |
  ```
  import com.ttv.face.FaceEngine;
  ...
  float score = FaceEngine.getInstance(this).compareFace(feat1, feat2);
  ```
#### - Check liveness from bitmap
  ```
  int livenessProcess(Bitmap bitmap, List<FaceResult> faceResults)
  ```
  | Name      | Description |
  |------------------|------------------|
  | Function        | Check Liveness from Detected Face    |
  | Parameters        | Android Bitmap format Image   |
  | Return Value        | 0: Succeed, -1: Failed<br/> If successful, set Liveness Value to FaceResults    |
  ```
  import com.ttv.face.FaceEngine;
  ...
  int ret = FaceEngine.getInstance(this).livenessProcess(bitmap, faceResults);
  ```
#### - Check liveness from yuv data
  ```
  int livenessProcess(byte[] nv21, int width, int height, List<FaceResult> faceResults)
  ```
  | Name      | Description |
  |------------------|------------------|
  | Function        | Check Liveness from Detected Face    |
  | Parameters        | NV21 YUV ByteArray with Width and Height   |
  | Return Value        | 0: Succeed, -1: Failed<br/> If successful, Set Liveness Value to FaceResults    |
  ```
  import com.ttv.face.FaceEngine;
  ...
  int ret = FaceEngine.getInstance(this).livenessProcess(nv21, width, height, faceResults);
  ```
#### - Get facial attribute(Gender, Age, Yaw, Roll, Pitch, Mask) from bitmap
  ```
  int faceAttrProcess(Bitmap bitmap, List<FaceResult> faceResults)
  ```
  | Name      | Description |
  |------------------|------------------|
  | Function        | Detect Attributes(Gender, Age, Yaw, Roll, Pitch, Mask) from Face    |
  | Parameters        | Android Bitmap format Image   |
  | Return Value        | 0: Succeed, -1: Failed<br/> If successful, Set Attributes Value to FaceResults    |
  ```
  import com.ttv.face.FaceEngine;
  ...
  int ret = FaceEngine.getInstance(this).faceAttrProcess(bitmap, faceResults);
  ```
#### - Get facial attribute(Gender, Age, Yaw, Roll, Pitch, Mask) from yuv data
  ```
  Function: Detect Attributes(Liveness, Gender, Age, Yaw, Roll, Pitch, Mask) from Face
  ```
  | Name      | Description |
  |------------------|------------------|
  | Function        | Detect Attributes(Gender, Age, Yaw, Roll, Pitch, Mask) from Face    |
  | Parameters        | NV21 YUV ByteArray with Width and Height   |
  | Return Value        | 0: Succeed, -1: Failed<br/> If successful, Set Attributes Value to FaceResults    |
  ```
  import com.ttv.face.FaceEngine;
  ...
  int ret = FaceEngine.getInstance(this).faceAttrProcess(nv21, width, height, faceResults);
  ```
#### - Process all(Extract Feature, Liveness, Gender, Age, Yaw, Roll, Pitch, Mask) from bitmap
  ```
  int faceAllProcess(Bitmap bitmap, boolean isRegister, List<FaceResult> faceResults)
  ```
  | Name      | Description |
  |------------------|------------------|
  | Function        | Extract Face Feature, Liveness, Get Attributes from Detected Face    |
  | Parameters        | Android Bitmap format Image   |
  | Return Value        | 0: Succeed, -1: Failed<br/> If successful, Set All Attributes Value to FaceResults    |
  ```
  import com.ttv.face.FaceEngine;
  ...
  int ret = FaceEngine.getInstance(this).faceAllProcess(bitmap, faceResults);
  ```
#### - Process all(Extract Feature, Liveness, Gender, Age, Yaw, Roll, Pitch, Mask) from yuv data
  ```
  int faceAllProcess(Bitmap bitmap, boolean isRegister, List<FaceResult> faceResults)
  ```
  | Name      | Description |
  |------------------|------------------|
  | Function        | Extract Face Feature, Liveness, Get Attributes from Detected Face    |
  | Parameters        | NV21 YUV ByteArray with Width and Height, extract feature for register or not  |
  | Return Value        | 0: Succeed, -1: Failed<br/> If successful, Set All Attributes Value to FaceResults    |
  ```
  import com.ttv.face.FaceEngine;
  ...
  int ret = FaceEngine.getInstance(this).faceAllProcess(nv21, width, height, faceResults);
  ```
