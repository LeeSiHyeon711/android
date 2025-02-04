package com.example.masisso

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream

class ModelHelper(private val context: Context) {
    private val ldInterpreter: Interpreter  // ✅ LD 감지 모델
    private val gradeInterpreter: Interpreter  // ✅ 등급 판별 모델

    // 등급 클래스 목록
    private val classLabels = arrayOf("1++등급", "1+등급", "1등급", "2등급", "3등급")

    init {
        ldInterpreter = Interpreter(loadModelFile("LD_model.tflite"))  // ✅ LD 감지 모델 로드
        gradeInterpreter = Interpreter(loadModelFile("model.tflite"))  // ✅ 등급 판별 모델 로드
    }

    private fun loadModelFile(modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // ✅ LD 감지 실행 함수
    fun runLDModel(bitmap: Bitmap): Int {
        val inputSize = 224
        val inputBuffer = preprocessLDImage(bitmap)  // ✅ LD 모델 전처리 적용

        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        for (value in inputBuffer) {
            byteBuffer.putFloat(value)
        }

        val output = Array(1) { FloatArray(3) }  // ✅ 3개 클래스로 변경
        ldInterpreter.run(byteBuffer, output)

        val predictedIndex = output[0].indices.maxByOrNull { output[0][it] } ?: -1  // ✅ 가장 높은 확률의 인덱스 반환

        Log.d("TensorFlow", "📊 LD 감지 예측 결과: ${output[0].contentToString()}")
        Log.d("TensorFlow", "✅ 선택된 인덱스: $predictedIndex")

        return predictedIndex  // ✅ 0, 1, 2 중 하나 반환
    }


    // ✅ 등급 판별 실행 함수
    fun runGradeModel(bitmap: Bitmap): Pair<String, Float> {
        val inputSize = 224
        val inputBuffer = preprocessImage(bitmap)

        // ✅ ByteBuffer 크기 확인 (올바른 크기로 설정)
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        for (value in inputBuffer) {
            byteBuffer.putFloat(value)
        }

        // ✅ 모델 출력 형태를 명확하게 설정 (5개 등급 확률)
        val output = Array(1) { FloatArray(5) }
        gradeInterpreter.run(byteBuffer, output)

        // ✅ Softmax 변환이 필요하면 적용
        val probabilities = softmax(output[0])

        return predictClass(probabilities) // ✅ 가장 높은 확률의 등급 반환
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: return logits
        val exps = logits.map { Math.exp((it - maxLogit).toDouble()) }
        val sumExps = exps.sum()
        return exps.map { (it / sumExps).toFloat() }.toFloatArray()
    }


    // ✅ LD 감지용 이미지 전처리 함수
    private fun preprocessLDImage(bitmap: Bitmap): FloatArray {
        val inputSize = 224
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        val inputImage = FloatArray(3 * inputSize * inputSize)
        var pixelIndex = 0

        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val pixel = resizedBitmap.getPixel(x, y)

                // ✅ LD 감지 모델 학습 때 사용한 정규화 적용
                val r = (Color.red(pixel) / 255.0f - 0.485f) / 0.229f
                val g = (Color.green(pixel) / 255.0f - 0.456f) / 0.224f
                val b = (Color.blue(pixel) / 255.0f - 0.406f) / 0.225f

                inputImage[pixelIndex] = r
                inputImage[pixelIndex + inputSize * inputSize] = g
                inputImage[pixelIndex + 2 * inputSize * inputSize] = b
                pixelIndex++
            }
        }
        return inputImage
    }


    private fun preprocessImage(bitmap: Bitmap): FloatArray {
        val inputSize = 224
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        val inputImage = FloatArray(3 * inputSize * inputSize)
        var pixelIndex = 0

        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val pixel = resizedBitmap.getPixel(x, y)

                // ✅ 학습 모델과 동일한 정규화 적용
                val r = ((Color.red(pixel) / 255.0f) - 0.485f) / 0.229f
                val g = ((Color.green(pixel) / 255.0f) - 0.456f) / 0.224f
                val b = ((Color.blue(pixel) / 255.0f) - 0.406f) / 0.225f

                inputImage[pixelIndex] = r
                inputImage[pixelIndex + inputSize * inputSize] = g
                inputImage[pixelIndex + 2 * inputSize * inputSize] = b
                pixelIndex++
            }
        }
        return inputImage
    }


    // ✅ 예측된 등급 반환하는 함수
    private fun predictClass(probabilities: FloatArray): Pair<String, Float> {
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
        val predictedClass = if (maxIndex != -1) classLabels[maxIndex] else "Unknown"
        val confidence = probabilities[maxIndex]

        // 로그 출력
        Log.d("TensorFlow", "📊 등급 예측 결과: ${probabilities.contentToString()}")
        Log.d("TensorFlow", "✅ 가장 높은 확률의 등급: $predictedClass ($confidence)")

        return Pair(predictedClass, confidence)
    }
}