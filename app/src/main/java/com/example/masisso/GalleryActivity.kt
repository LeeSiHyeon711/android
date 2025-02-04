package com.example.masisso

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import com.example.masisso.ui.theme.MasissoTheme

class GalleryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Masisso) // ✅ 테마 강제 적용

        super.onCreate(savedInstanceState)

        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_GALLERY_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == REQUEST_GALLERY_PICK) {
            val imageUri = data?.data
            if (imageUri != null) {
                val resultIntent = Intent()
                resultIntent.data = imageUri
                setResult(RESULT_OK, resultIntent)
            }
        }
        finish() // ✅ GalleryActivity 종료 후 MainActivity로 데이터 전달
    }

    companion object {
        private const val REQUEST_GALLERY_PICK = 2
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen() {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                imageBitmap = bitmap
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("🖼 갤러리") })
        },
        content = { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = {
                    val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    pickImageLauncher.launch(galleryIntent)
                }) {
                    Text("📂 갤러리에서 선택")
                }

                Spacer(modifier = Modifier.height(16.dp))

                imageBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "선택된 이미지",
                        modifier = Modifier.size(250.dp)
                    )
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun GalleryScreenPreview() {
    MasissoTheme {
        GalleryScreen()
    }
}
