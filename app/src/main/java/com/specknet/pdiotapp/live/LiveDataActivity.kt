package com.specknet.pdiotapp.live
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
import org.pytorch.IValue
//import com.specknet.pdiotapp.utils.ThingyLiveData
import kotlin.collections.ArrayList
//import org.tensorflow.lite.Interpreter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class LiveDataActivity : AppCompatActivity() {

    val ACTIVITY_CODE_TO_NAME_MAPPING = mapOf(
        0 to "Ascending stairs Normal",
        1 to "Descending stairs Normal",
        2 to "Lying down back Normal",
        3 to "Lying down on left Normal",
        4 to "Lying down on right Normal",
        5 to "Lying down stomach Normal",
        6 to "Miscellaneous movements Normal",
        7 to "Normal walking Normal",
        8 to "Running Normal",
        9 to "Shuffle walking Normal",
        10 to "Sitting/standing Normal",
    )


    // global graph variables
    lateinit var predictedActivityTextView: TextView

    lateinit var dataSet_res_accel_x: LineDataSet
    lateinit var dataSet_res_accel_y: LineDataSet
    lateinit var dataSet_res_accel_z: LineDataSet

    //lateinit var tfliteInterpreter: Interpreter
    lateinit var pytorchModel: Module

    val inputSize = 50 * 3  // 2 seconds at 25Hz with 3 data points (x, y, z)
    var tfinput = Array(1) { Array(50) { FloatArray(3) } }
    var dataCollectionCounter = 0


//    lateinit var dataSet_thingy_accel_x: LineDataSet
    //  lateinit var dataSet_thingy_accel_y: LineDataSet
    //lateinit var dataSet_thingy_accel_z: LineDataSet

    var time = 0f
    lateinit var allRespeckData: LineData

//    lateinit var allThingyData: LineData

    lateinit var respeckChart: LineChart
//    lateinit var thingyChart: LineChart

    // global broadcast receiver so we can unregister it
    lateinit var respeckLiveUpdateReceiver: BroadcastReceiver

    //    lateinit var thingyLiveUpdateReceiver: BroadcastReceiver
    lateinit var looperRespeck: Looper
    //   lateinit var looperThingy: Looper

    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)
    //   val filterTestThingy = IntentFilter(Constants.ACTION_THINGY_BROADCAST)

//    fun testInterpreter() {
//        // Create a repeating pattern of [-1, 0, 1] shaped [1, 50, 3]
//        val dummyData = Array(1) { Array(50) { FloatArray(3) { i -> (i % 3 - 1).toFloat() } } }
//        val output = Array(1) { FloatArray(12) }
//
//        try {
//            tfliteInterpreter.run(dummyData, output)
//            val maxOutputValue = output[0].maxOrNull()
//            val predictedActivityIndex = output[0].indexOfFirst { it == maxOutputValue }
//            val predictedActivityName = ACTIVITY_CODE_TO_NAME_MAPPING[predictedActivityIndex] ?: "Unknown"
//
//            Log.d("testInterpreter", "Model's prediction: ${output[0].joinToString(", ")}")
//            Log.d("testInterpreter", "Predicted activity number: $predictedActivityIndex")
//            Log.d("testInterpreter", "Predicted activity name: $predictedActivityName")
//        } catch (e: Exception) {
//            Log.e("testInterpreter", "Error running the model: ${e.message}")
//        }
//    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_data)
        predictedActivityTextView = findViewById(R.id.predicted_activity_text)



        pytorchModel = Module.load(assetFilePath(this, "task1.pt"))





        setupCharts()

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


//        if (dataCollectionCounter >= 50) {
//            val output = Array(1) { FloatArray(12) }  // Adjusted for 12 activities
//            tfliteInterpreter.run(tfinput, output)
//
//
//            val maxOutputValue = output[0].maxOrNull()
//            val predictedActivityIndex = output[0].indexOfFirst { it == maxOutputValue }
//            val predictedActivityName = ACTIVITY_CODE_TO_NAME_MAPPING[predictedActivityIndex] ?: "Unknown"
        if (dataCollectionCounter >= 50) {
            // Flatten the tfinput array
            val flattenedInput = tfinput.flatten().flatMap { it.toList() }.toFloatArray()

            // Create a Tensor from the flattened array
            val inputTensor = Tensor.fromBlob(flattenedInput, longArrayOf(1, 50, 3))

            // Run the model
            val outputTensor = pytorchModel.forward(IValue.from(inputTensor)).toTensor()

            // Process the output
            val outputScores = outputTensor.dataAsFloatArray
            val maxScoreIndex = outputScores.indices.maxByOrNull { outputScores[it] } ?: -1

            val predictedActivityName = ACTIVITY_CODE_TO_NAME_MAPPING[maxScoreIndex] ?: "Unknown"


            savePredictedActivityWithTimestamp(predictedActivityName)

            Log.d("modelinput", "Model's prediction: ${outputScores.joinToString(", ")}")
            Log.d("modelinput", "Predicted activity number: $maxScoreIndex")
            Log.d("modelinput", "Predicted activity name: $predictedActivityName")


            // Update UI on the main thread
            runOnUiThread {
                Log.d("DEBUG", "Updating UI with predicted activity")
                predictedActivityTextView.text = "$predictedActivityName"
            }

            // Reset dataCollectionCounter and clear tfinput if needed
            tfinput = Array(1) { Array(50) { FloatArray(3) } }
            dataCollectionCounter = 0

        }
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
//        unregisterReceiver(thingyLiveUpdateReceiver)
        looperRespeck.quit()
//        looperThingy.quit()
    }


    private fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)

        try {
            val inputStream = context.assets.open(assetName)
            try {
                val outputStream = FileOutputStream(file)
                try {
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                } finally {
                    outputStream.close()
                }
            } finally {
                inputStream.close()
            }
        } catch (e: IOException) {
            throw RuntimeException("Error copying asset file: $assetName", e)
        }

        return file.absolutePath
    }


}