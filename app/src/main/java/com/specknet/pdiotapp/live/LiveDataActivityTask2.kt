package com.specknet.pdiotapp.live
import java.nio.ByteBuffer
import android.widget.TextView

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.RESpeckLiveData
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import org.jtransforms.fft.DoubleFFT_2D
//import com.specknet.pdiotapp.utils.ThingyLiveData
import kotlin.collections.ArrayList
import org.tensorflow.lite.Interpreter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.log2
import kotlin.collections.flatten
import kotlin.collections.flatten


data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

class LiveDataActivityTask2 : AppCompatActivity() {

    val ACTIVITY_CODE_TO_NAME_MAPPING = mapOf(
        0 to "sitting/standing + breathing normally",
        1 to "lying down on your left side + breathing normally",
        2 to "lying down on your right side + breathing normally",
        3 to "lying down on your back + breathing normally",
        4 to "lying down on your stomach + breathing normally",
        5 to "sitting/standing + coughing",
        6 to "lying down on your left side + coughing",
        7 to "lying down on your right side + coughing",
        8 to "lying down on your back + coughing",
        9 to "lying down on your stomach + coughing",
        10 to "sitting/standing + hyperventilating",
        11 to "lying down on your left side + hyperventilating",
        12 to "lying down on your right side + hyperventilating",
        13 to "lying down on your back + hyperventilating",
        14 to "lying down on your stomach + hyperventilating",
    )


    // global graph variables
    lateinit var predictedActivityTextView: TextView

    lateinit var dataSet_res_accel_x: LineDataSet
    lateinit var dataSet_res_accel_y: LineDataSet
    lateinit var dataSet_res_accel_z: LineDataSet

    lateinit var tfliteInterpreter: Interpreter
    lateinit var tfliteInterpreter1: Interpreter
    lateinit var tfliteInterpreter2: Interpreter
    lateinit var tfliteInterpreter3: Interpreter
    lateinit var tfliteInterpreter4: Interpreter
    lateinit var tfliteInterpreter5: Interpreter

    val inputSize = 50 * 3  // 2 seconds at 25Hz with 3 data points (x, y, z)
    var tfinput = Array(1) { Array(50) { FloatArray(15) } }
    var dataCollectionCounter = 0


    var time = 0f
    lateinit var allRespeckData: LineData
    lateinit var respeckChart: LineChart
    lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    lateinit var looperRespeck: Looper
    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_data_task2)
        predictedActivityTextView = findViewById(R.id.predicted_activity_text)

        // Load the TensorFlow Lite model0 from assets
        val model = assets.open("model_task2.tflite").readBytes()
        val tfliteOptions = Interpreter.Options()
        val byteBuffer = ByteBuffer.allocateDirect(model.size)
        byteBuffer.put(model)
        tfliteInterpreter = Interpreter(byteBuffer, tfliteOptions)

        // Load the model1 from assets
        val model1 = assets.open("model1_task2.tflite").readBytes()
        val tfliteOptions1 = Interpreter.Options()
        val byteBuffer1 = ByteBuffer.allocateDirect(model1.size)
        byteBuffer1.put(model1)
        tfliteInterpreter1 = Interpreter(byteBuffer1, tfliteOptions1)

        // Load the model2 from assets
        val model2 = assets.open("model2_task2.tflite").readBytes()
        val tfliteOptions2 = Interpreter.Options()
        val byteBuffer2 = ByteBuffer.allocateDirect(model2.size)
        byteBuffer2.put(model2)
        tfliteInterpreter2 = Interpreter(byteBuffer2, tfliteOptions2)

        // Load the model3 from assets
        val model3 = assets.open("model3_task2.tflite").readBytes()
        val tfliteOptions3 = Interpreter.Options()
        val byteBuffer3 = ByteBuffer.allocateDirect(model3.size)
        byteBuffer3.put(model3)
        tfliteInterpreter3 = Interpreter(byteBuffer3, tfliteOptions3)

        // Load the model4 from assets
        val model4 = assets.open("model4_task2.tflite").readBytes()
        val tfliteOptions4 = Interpreter.Options()
        val byteBuffer4 = ByteBuffer.allocateDirect(model4.size)
        byteBuffer4.put(model4)
        tfliteInterpreter4 = Interpreter(byteBuffer4, tfliteOptions4)

        // Load the model5 from assets
        val model5 = assets.open("model5_task2.tflite").readBytes()
        val tfliteOptions5 = Interpreter.Options()
        val byteBuffer5 = ByteBuffer.allocateDirect(model5.size)
        byteBuffer5.put(model5)
        tfliteInterpreter5 = Interpreter(byteBuffer5, tfliteOptions5)

        setupCharts()

        // set up the broadcast receiver
        respeckLiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                Log.d("DEBUG", "onReceive triggered")

                if (action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {
                    try {
                        val liveData = intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData

                        val x = liveData.accelX
                        val y = liveData.accelY
                        val z = liveData.accelZ

                        modelinput(x, y, z)

                        time += 1
                        updateGraph("respeck", x, y, z)

                    } catch (e: Exception) {
                        // Handle any exceptions that may occur when processing the broadcast data
                        Log.e("BroadcastReceiver", "Error processing RESpeckLiveData: ${e.message}")
                    }
                }
            }
        }




        // register receiver on another thread
        val handlerThreadRespeck = HandlerThread("bgThreadRespeckLive")
        handlerThreadRespeck.start()
        looperRespeck = handlerThreadRespeck.looper
        val handlerRespeck = Handler(looperRespeck)
        this.registerReceiver(respeckLiveUpdateReceiver, filterTestRespeck, null, handlerRespeck)
    }

    fun modelinput(x: Float, y: Float, z: Float) {
        Log.d("modelinput", "Received data: x=$x, y=$y, z=$z")
        Log.d("DEBUG", "modelinput called")
        if (dataCollectionCounter < 50) {
            tfinput[0][dataCollectionCounter][0] = x
            tfinput[0][dataCollectionCounter][1] = y
            tfinput[0][dataCollectionCounter][2] = z
            dataCollectionCounter++
        }


        if (dataCollectionCounter >= 50) {
            // Convert tfinput to the required format for FFT processing
            val sequence = Array(50) { FloatArray(3) }
            for (i in 0 until 50) {
                sequence[i][0] = tfinput[0][i][0]
                sequence[i][1] = tfinput[0][i][1]
                sequence[i][2] = tfinput[0][i][2]
            }

            val (fftReal, fftImag, fftMag, spectralEntropy) = processForFFT(sequence)

            for (i in 0 until 50) {
                // Incorporate FFT and spectral entropy data into tfinput
                tfinput[0][i][3] = fftReal[0][i]  // FFT Real for x
                tfinput[0][i][4] = fftReal[1][i]  // FFT Real for y
                tfinput[0][i][5] = fftReal[2][i] // FFT Real for z

                tfinput[0][i][6] = fftImag[0][i]  // FFT Imag for x
                tfinput[0][i][7] = fftImag[1][i]  // FFT Imag for y
                tfinput[0][i][8] = fftImag[2][i] // FFT Imag for z


                tfinput[0][i][9] = fftMag[0][i]   // FFT Mag for x
                tfinput[0][i][10] = fftMag[1][i]   // FFT Mag for y
                tfinput[0][i][11] = fftMag[2][i]  // FFT Mag for z

                tfinput[0][i][12] = spectralEntropy[0][i] // Spectral Entropy for x
                tfinput[0][i][13] = spectralEntropy[1][i] // Spectral Entropy for y
                tfinput[0][i][14] = spectralEntropy[2][i] // Spectral Entropy for z

                // Log the updated tfinput content
                Log.d("UpdatedTfinputContent", "Index: $i, Data: ${tfinput[0][i].joinToString()}")
            }

            val output = Array(1) { FloatArray(5) }  // Adjusted for 12 activities

            tfliteInterpreter.run(tfinput, output)
            val maxOutputValue = output[0].maxOrNull()
            val predictedActivityIndex = output[0].indexOfFirst { it == maxOutputValue }

            when (predictedActivityIndex) {
                0 -> {
                    val output1 = Array(1) { FloatArray(3)}
                    tfliteInterpreter1.run(tfinput, output1)
                    val maxOutputValue1 = output1[0].maxOrNull()
                    val predictedActivityIndex1 = output1[0].indexOfFirst { it == maxOutputValue1 }
                    val predictedActivityName = ACTIVITY_CODE_TO_NAME_MAPPING[predictedActivityIndex1 * 5] ?: "Unknown"
                    savePredictedActivityWithTimestamp(predictedActivityName)
                    runOnUiThread {
                        Log.d("DEBUG", "Updating UI with predicted activity")
                        predictedActivityTextView.text = "Predicted Activity: $predictedActivityName"
                    }
                }
                1 -> {
                    val output1 = Array(1) { FloatArray(3)}
                    tfliteInterpreter2.run(tfinput, output1)
                    val maxOutputValue1 = output1[0].maxOrNull()
                    val predictedActivityIndex1 = output1[0].indexOfFirst { it == maxOutputValue1 }
                    val predictedActivityName = ACTIVITY_CODE_TO_NAME_MAPPING[predictedActivityIndex1 * 5 + 1] ?: "Unknown"
                    savePredictedActivityWithTimestamp(predictedActivityName)
                    runOnUiThread {
                        Log.d("DEBUG", "Updating UI with predicted activity")
                        predictedActivityTextView.text = "Predicted Activity: $predictedActivityName"
                    }
                }
                2 -> {
                    val output1 = Array(1) { FloatArray(3)}
                    tfliteInterpreter3.run(tfinput, output1)
                    val maxOutputValue1 = output1[0].maxOrNull()
                    val predictedActivityIndex1 = output1[0].indexOfFirst { it == maxOutputValue1 }
                    val predictedActivityName = ACTIVITY_CODE_TO_NAME_MAPPING[predictedActivityIndex1 * 5 + 2] ?: "Unknown"
                    savePredictedActivityWithTimestamp(predictedActivityName)
                    runOnUiThread {
                        Log.d("DEBUG", "Updating UI with predicted activity")
                        predictedActivityTextView.text = "Predicted Activity: $predictedActivityName"
                    }
                }
                3 -> {
                    val output1 = Array(1) { FloatArray(3)}
                    tfliteInterpreter4.run(tfinput, output1)
                    val maxOutputValue1 = output1[0].maxOrNull()
                    val predictedActivityIndex1 = output1[0].indexOfFirst { it == maxOutputValue1 }
                    val predictedActivityName = ACTIVITY_CODE_TO_NAME_MAPPING[predictedActivityIndex1 * 5 + 3] ?: "Unknown"
                    savePredictedActivityWithTimestamp(predictedActivityName)
                    runOnUiThread {
                        Log.d("DEBUG", "Updating UI with predicted activity")
                        predictedActivityTextView.text = "Predicted Activity: $predictedActivityName"
                    }
                }
                4 -> {val output1 = Array(1) { FloatArray(3)}
                    tfliteInterpreter5.run(tfinput, output1)
                    val maxOutputValue1 = output1[0].maxOrNull()
                    val predictedActivityIndex1 = output1[0].indexOfFirst { it == maxOutputValue1 }
                    val predictedActivityName = ACTIVITY_CODE_TO_NAME_MAPPING[predictedActivityIndex1 * 5 + 4] ?: "Unknown"
                    savePredictedActivityWithTimestamp(predictedActivityName)
                    runOnUiThread {
                        Log.d("DEBUG", "Updating UI with predicted activity")
                        predictedActivityTextView.text = "Predicted Activity: $predictedActivityName"
                    }
                }
            }

            // Reset dataCollectionCounter and clear tfinput if needed
            tfinput = Array(1) { Array(50) { FloatArray(15) } }
            dataCollectionCounter = 0

        }
    }


//    fun processForFFT(sequence: Array<FloatArray>): Quad<Array<FloatArray>, Array<FloatArray>, Array<FloatArray>, Array<FloatArray>> {
//        val transformer = FastFourierTransformer(DftNormalization.STANDARD)
//        val fftReal = Array(3) { FloatArray(sequence.size) }
//        val fftImag = Array(3) { FloatArray(sequence.size) }
//        val fftMag = Array(3) { FloatArray(sequence.size) }
//        val spectralEntropy = Array(3) { FloatArray(sequence.size) }
//
//        // Find the next power of 2 greater than or equal to sequence.size
//        val paddedSize = Integer.highestOneBit(sequence.size - 1) * 2
//
//        for (j in 0..2) {
//            val componentSequence = DoubleArray(paddedSize) { 0.0 }
//            for (i in sequence.indices) {
//                componentSequence[i] = sequence[i][j].toDouble()
//            }
//            val complex = transformer.transform(componentSequence, TransformType.FORWARD)
//
//            // Iterate only over the original sequence size, not the padded size
//            for (i in sequence.indices) {
//                fftReal[j][i] = complex[i].real.toFloat()
//                fftImag[j][i] = complex[i].imaginary.toFloat()
//                fftMag[j][i] = sqrt(fftReal[j][i].pow(2) + fftImag[j][i].pow(2))
//            }
//
//            val totalEnergy = fftMag[j].sumOf { it.toDouble().pow(2) }
//            for (i in sequence.indices) {
//                val normalizedEnergy = fftMag[j][i].toDouble().pow(2) / totalEnergy
//                spectralEntropy[j][i] = if (normalizedEnergy > 0) {
//                    (-normalizedEnergy * log2(normalizedEnergy)).toFloat()
//                } else {
//                    0f
//                }
//            }
//        }
//
//        return Quad(fftReal, fftImag, fftMag, spectralEntropy)
//    }

    fun processForFFT(sequence: Array<FloatArray>): Quad<Array<FloatArray>, Array<FloatArray>, Array<FloatArray>, Array<FloatArray>> {
        val rows = 3 // (x, y, z)
        val columns = sequence.size

        // Padding for FFT
        val paddedRows = Integer.highestOneBit(rows - 1) * 2
        val paddedColumns = Integer.highestOneBit(columns - 1) * 2

        val complexData = Array(paddedRows) { DoubleArray(2 * paddedColumns) } // Factor of 2 for real and imaginary parts

        // Fill the real parts of the input data
        for (i in 0 until rows) {
            for (j in sequence.indices) {
                complexData[i][2 * j] = sequence[j][i].toDouble() // Transpose and fill the data
            }
        }

        // Perform 2D FFT
        val transformer = DoubleFFT_2D(paddedRows.toLong(), paddedColumns.toLong())
        transformer.realForwardFull(complexData)

        // Extract and process FFT results
        val fftReal = Array(rows) { FloatArray(columns) }
        val fftImag = Array(rows) { FloatArray(columns) }
        val fftMag = Array(rows) { FloatArray(columns) }
        val spectralEntropy = Array(rows) { FloatArray(columns) }

        for (i in 0 until rows) {
            for (j in 0 until columns) {
                val real = complexData[i][2 * j]
                val imag = complexData[i][2 * j + 1]

                fftReal[i][j] = real.toFloat()
                fftImag[i][j] = imag.toFloat()
                fftMag[i][j] = sqrt(real.pow(2) + imag.pow(2)).toFloat()
            }

            // Compute spectral entropy for each axis
            val totalEnergy = fftMag[i].sumOf { it.toDouble().pow(2) }
            for (j in 0 until columns) {
                val normalizedEnergy = fftMag[i][j].toDouble().pow(2) / totalEnergy
                spectralEntropy[i][j] = if (normalizedEnergy > 0) {
                    (-normalizedEnergy * log2(normalizedEnergy)).toFloat()
                } else {
                    0f
                }
            }
        }

        Log.d("FFTDebug", "Final FFT Real: ${fftReal.contentDeepToString()}")
        Log.d("FFTDebug", "Final FFT Imag: ${fftImag.contentDeepToString()}")
        Log.d("FFTDebug", "Final FFT Mag: ${fftMag.contentDeepToString()}")
        Log.d("FFTDebug", "Final Spectral Entropy: ${spectralEntropy.contentDeepToString()}")

        return Quad(fftReal, fftImag, fftMag, spectralEntropy)
    }




    private fun savePredictedActivityWithTimestamp(activityName: String) {
        val sharedPreferences = getSharedPreferences("ActivityPredictions", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Creating a timestamp
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())

        // Saving the activity name with the timestamp as key
        editor.putString(timestamp, activityName)
        editor.apply()

        // Optionally, log the saved data for verification
        Log.d("LiveDataActivity", "Saved activity: $activityName at $timestamp")
    }

    fun setupCharts() {
        respeckChart = findViewById(R.id.respeck_chart)
        //       thingyChart = findViewById(R.id.thingy_chart)

        // Respeck

        time = 0f
        val entries_res_accel_x = ArrayList<Entry>()
        val entries_res_accel_y = ArrayList<Entry>()
        val entries_res_accel_z = ArrayList<Entry>()

        dataSet_res_accel_x = LineDataSet(entries_res_accel_x, "Accel X")
        dataSet_res_accel_y = LineDataSet(entries_res_accel_y, "Accel Y")
        dataSet_res_accel_z = LineDataSet(entries_res_accel_z, "Accel Z")

        dataSet_res_accel_x.setDrawCircles(false)
        dataSet_res_accel_y.setDrawCircles(false)
        dataSet_res_accel_z.setDrawCircles(false)

        dataSet_res_accel_x.setColor(
            ContextCompat.getColor(
                this,
                R.color.red
            )
        )
        dataSet_res_accel_y.setColor(
            ContextCompat.getColor(
                this,
                R.color.green
            )
        )
        dataSet_res_accel_z.setColor(
            ContextCompat.getColor(
                this,
                R.color.blue
            )
        )

        val dataSetsRes = ArrayList<ILineDataSet>()
        dataSetsRes.add(dataSet_res_accel_x)
        dataSetsRes.add(dataSet_res_accel_y)
        dataSetsRes.add(dataSet_res_accel_z)

        allRespeckData = LineData(dataSetsRes)
        respeckChart.data = allRespeckData
        respeckChart.invalidate()
    }

    fun updateGraph(graph: String, x: Float, y: Float, z: Float) {
        // take the first element from the queue
        // and update the graph with it
        if (graph == "respeck") {
            dataSet_res_accel_x.addEntry(Entry(time, x))
            dataSet_res_accel_y.addEntry(Entry(time, y))
            dataSet_res_accel_z.addEntry(Entry(time, z))

            runOnUiThread {
                allRespeckData.notifyDataChanged()
                respeckChart.notifyDataSetChanged()
                respeckChart.invalidate()
                respeckChart.setVisibleXRangeMaximum(150f)
                respeckChart.moveViewToX(respeckChart.lowestVisibleX + 40)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(respeckLiveUpdateReceiver)
        looperRespeck.quit()
    }
}

