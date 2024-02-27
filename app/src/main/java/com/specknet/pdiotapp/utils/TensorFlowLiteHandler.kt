package com.specknet.pdiotapp.utils

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log
import android.widget.TextView
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer

class TensorFlowLiteHandler {
    var dataCollectionCounter = 0
    var tfinput = Array(1) { Array(50) { FloatArray(3) } }

    val ACTIVITY_CODE_TO_NAME_MAPPING = mapOf(
        0 to "Ascending stairs Normal",
        1 to "Descending stairs Normal",
        2 to "Lying down back Normal",
        3 to "Lying down on left Normal",
        4 to "Lying down on stomach Normal",
        5 to "Lying down right Normal",
        6 to "Miscellaneous movements Normal",
        7 to "Normal walking Normal",
        8 to "Running Normal",
        9 to "Shuffle walking Normal",
        10 to "Sitting Normal",
        11 to "Standing Normal"
    )

    private lateinit var tfliteInterpreter: Interpreter

    fun testInterpreter() {
        // Create a repeating pattern of [-1, 0, 1] shaped [1, 50, 3]
        val dummyData = Array(1) { Array(50) { FloatArray(3) { i -> (i % 3 - 1).toFloat() } } }
        val output = Array(1) { FloatArray(12) }

        try {
            tfliteInterpreter.run(dummyData, output)
            val maxOutputValue = output[0].maxOrNull()
            val predictedActivityIndex = output[0].indexOfFirst { it == maxOutputValue }
            val predictedActivityName = ACTIVITY_CODE_TO_NAME_MAPPING[predictedActivityIndex] ?: "Unknown"

            Log.d("testInterpreter", "Model's prediction: ${output[0].joinToString(", ")}")
            Log.d("testInterpreter", "Predicted activity number: $predictedActivityIndex")
            Log.d("testInterpreter", "Predicted activity name: $predictedActivityName")
        } catch (e: Exception) {
            Log.e("testInterpreter", "Error running the model: ${e.message}")
        }
    }

    fun loadModel(context: Context) {
        // Code to load the model
        // Load the TensorFlow Lite model from assets
        val model = context.assets.open("model.tflite").readBytes()
        val tfliteOptions = Interpreter.Options()
        // Convert byte array to ByteBuffer
        val byteBuffer = ByteBuffer.allocateDirect(model.size)
        byteBuffer.put(model)
        // Initialize the Interpreter with the ByteBuffer and options
        tfliteInterpreter = Interpreter(byteBuffer, tfliteOptions)
        testInterpreter()
    }

    fun modelinput(activity: Activity, predictedActivityTextView: TextView, x: Float, y: Float, z: Float) {
        Log.d("modelinput", "Received data: x=$x, y=$y, z=$z")
        Log.d("DEBUG", "modelinput called")
        if (dataCollectionCounter < 50) {
            tfinput[0][dataCollectionCounter][0] = x
            tfinput[0][dataCollectionCounter][1] = y
            tfinput[0][dataCollectionCounter][2] = z
            dataCollectionCounter++
        }
        if (dataCollectionCounter >= 50) {
            val output = Array(1) { FloatArray(12) }  // Adjusted for 12 activities
            tfliteInterpreter.run(tfinput, output)

            val maxOutputValue = output[0].maxOrNull()
            val predictedActivityIndex = output[0].indexOfFirst { it == maxOutputValue }
            val predictedActivityName = ACTIVITY_CODE_TO_NAME_MAPPING[predictedActivityIndex] ?: "Unknown"

            Log.d("modelinput", "Model's prediction: ${output[0].joinToString(", ")}")
            Log.d("modelinput", "Predicted activity number: $predictedActivityIndex")
            Log.d("modelinput", "Predicted activity name: $predictedActivityName")

            // Update UI on the main thread
            activity.runOnUiThread {
                Log.d("DEBUG", "Updating UI with predicted activity")
                predictedActivityTextView.text = "Predicted Activity: $predictedActivityName"
            }
            // Reset dataCollectionCounter and clear tfinput if needed
            tfinput = Array(1) { Array(50) { FloatArray(3) } }
            dataCollectionCounter = 0
        }
    }

}
