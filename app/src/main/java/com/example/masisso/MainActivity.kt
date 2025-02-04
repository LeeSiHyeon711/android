package com.example.masisso

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.masisso.ui.theme.MasissoTheme
import android.Manifest
import android.annotation.SuppressLint

class MainActivity : ComponentActivity() {
    private lateinit var modelHelper: ModelHelper
    private var predictionResult by mutableStateOf("")  // ✅ 예측 결과 상태
    private var resultImage by mutableStateOf<Bitmap?>(null)  // ✅ 분석에 사용한 이미지를 저장
    private val REQUEST_CAMERA_PERMISSION = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ modelHelper 초기화 추가
        modelHelper = ModelHelper(this)

        setContent {
            MasissoTheme {
                MainScreen(
                    onCameraClick = { checkCameraPermission() },
                    onGalleryClick = { startActivityForResult(Intent(this, GalleryActivity::class.java), REQUEST_GALLERY_PICK) },
                    predictionResult = predictionResult,
                    resultImage = resultImage // ✅ 결과 이미지 전달
                )
            }
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startActivityForResult(Intent(this, CameraActivity::class.java), REQUEST_IMAGE_CAPTURE)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            val imageBitmap: Bitmap? = when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> data?.extras?.get("data") as? Bitmap
                REQUEST_GALLERY_PICK -> data?.data?.let { uri ->
                    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }
                else -> null
            }

            if (imageBitmap != null) {
                resultImage = imageBitmap // ✅ UI에 이미지 표시

                val detectedClass = modelHelper.runLDModel(imageBitmap)  // ✅ 문자열 반환

                if (detectedClass == 2) { // ✅ 배최장근으로 감지된 경우
                    val (grade, confidence) = modelHelper.runGradeModel(imageBitmap)
                    predictionResult = """
                        🥩 배최장근이 맞아요!
                        📊 예상 등급: $grade (%.2f%% 확률)
                    """.trimIndent().format(confidence * 100)

                    predictionResult += when (grade) {
                        "1++등급" -> "\n🔥 최상급 한우입니다!"
                        "1+등급" -> "\n👌 고급 한우네요!"
                        "1등급" -> "\n🥩 좋은 품질의 한우입니다."
                        "2등급" -> "\n🔹 보통 등급의 한우입니다."
                        "3등급" -> "\n⚠️ 등급이 낮은 편입니다."
                        else -> ""
                    }
                } else { // ✅ 배최장근이 아닌 경우
                    predictionResult = """
                    ❌ 배최장근이 아닙니다.
                    📌 분석 결과, 등급 판별이 어려운 부위입니다.
                    📸 배최장근 사진을 올려주세요!
                    """.trimIndent()
                }

                Log.d("TensorFlow", "📊 LD 모델 결과: $detectedClass")
            }
        }
    }

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_GALLERY_PICK = 2
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    predictionResult: String,
    resultImage: Bitmap?
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "맛있소 AI v_0.0.1📸",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(20.dp))

        resultImage?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "분석한 이미지",
                modifier = Modifier
                    .size(200.dp)
                    .padding(10.dp)
            )
        }

        Text(
            text = predictionResult,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = onCameraClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = "📷 카메라 열기")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = onGalleryClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = "🖼 갤러리에서 선택")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MasissoTheme {
        MainScreen(
            onCameraClick = {},
            onGalleryClick = {},
            predictionResult = "예측 결과가 여기에 표시됩니다.",
            resultImage = null
        )
    }
}