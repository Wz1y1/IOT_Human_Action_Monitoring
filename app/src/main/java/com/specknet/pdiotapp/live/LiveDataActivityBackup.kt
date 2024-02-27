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
//import com.specknet.pdiotapp.utils.ThingyLiveData
import kotlin.collections.ArrayList
import org.tensorflow.lite.Interpreter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class LiveDataActivityBackup : AppCompatActivity() {

    val ACTIVITY_CODE_TO_NAME_MAPPING = mapOf(
        0 to "sitting/standing",
        1 to "lying down on your left side",
        2 to "lying down on your right side",
        3 to "lying down on your back",
        4 to "lying down on your stomach",
        5 to "walking",
        6 to "running/jogging",
        7 to "descending stairs",
        8 to "ascending stairs",
        9 to "shuffle walking",
        10 to "miscellaneous movements",
        //11 to "Standing Normal"
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

    val inputSize = 50 * 3  // 2 seconds at 25Hz with 3 data points (x, y, z)
    var tfinput = Array(1) { Array(50) { FloatArray(3) } }
    var dataCollectionCounter = 0


    //lateinit var dataSet_thingy_accel_x: LineDataSet
    //lateinit var dataSet_thingy_accel_y: LineDataSet
    //lateinit var dataSet_thingy_accel_z: LineDataSet

    var time = 0f
    lateinit var allRespeckData: LineData

    //lateinit var allThingyData: LineData

    lateinit var respeckChart: LineChart
    //lateinit var thingyChart: LineChart

    //global broadcast receiver so we can unregister it
    lateinit var respeckLiveUpdateReceiver: BroadcastReceiver

    //lateinit var thingyLiveUpdateReceiver: BroadcastReceiver
    lateinit var looperRespeck: Looper
    //lateinit var looperThingy: Looper

    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)
    //val filterTestThingy = IntentFilter(Constants.ACTION_THINGY_BROADCAST)

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
        setContentView(R.layout.activity_live_data)
        predictedActivityTextView = findViewById(R.id.predicted_activity_text)

        // Load the TensorFlow Lite model0 from assets
        val model = assets.open("model.tflite").readBytes()
        val tfliteOptions = Interpreter.Options()
        // Convert byte array to ByteBuffer
        val byteBuffer = ByteBuffer.allocateDirect(model.size)
        byteBuffer.put(model)
        // Initialize the Interpreter with the ByteBuffer and options
        tfliteInterpreter = Interpreter(byteBuffer, tfliteOptions)
        testInterpreter()

        // Load the model1 from assets
        val model1 = assets.open("model1.tflite").readBytes()
        val tfliteOptions1 = Interpreter.Options()
        val byteBuffer1 = ByteBuffer.allocateDirect(model1.size)
        byteBuffer1.put(model1)
        tfliteInterpreter1 = Interpreter(byteBuffer1, tfliteOptions1)

        // Load the model2 from assets
        val model2 = assets.open("model2.tflite").readBytes()
        val tfliteOptions2 = Interpreter.Options()
        val byteBuffer2 = ByteBuffer.allocateDirect(model2.size)
        byteBuffer2.put(model2)
        tfliteInterpreter2 = Interpreter(byteBuffer2, tfliteOptions2)

        // Load the model1 from assets
        val model3 = assets.open("model3.tflite").readBytes()
        val tfliteOptions3 = Interpreter.Options()
        val byteBuffer3 = ByteBuffer.allocateDirect(model3.size)
        byteBuffer3.put(model3)
        tfliteInterpreter3 = Interpreter(byteBuffer3, tfliteOptions3)

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
            val output = Array(1) { FloatArray(4) }  // Adjusted for 12 activities
            tfliteInterpreter.run(tfinput, output)
            val maxOutputValue = output[0].maxOrNull()
            val predictedActivityIndex = output[0].indexOfFirst { it == maxOutputValue }

            when (predictedActivityIndex) {
                0 -> {
                    val predictedActivityName = ACTIVITY_CODE_TO_NAME_MAPPING[0] ?: "Unknown"
                    savePredictedActivityWithTimestamp(predictedActivityName)
                    runOnUiThread {
                        Log.d("DEBUG", "Updating UI with predicted activity")
                        predictedActivityTextView.text = "Predicted Activity: $predictedActivityName"
                    }
                }
                1 -> {
                    val output1 = Array(1) { FloatArray(4)}
                    tfliteInterpreter1.run(tfinput, output1)
                    val maxOutputValue1 = output1[0].maxOrNull()
                    val predictedActivityIndex1 = output1[0].indexOfFirst { it == maxOutputValue1 }
                    val predictedActivityName = ACTIVITY_CODE_TO_NAME_MAPPING[predictedActivityIndex1 + 1] ?: "Unknown"
                    savePredictedActivityWithTimestamp(predictedActivityName)
                    runOnUiThread {
                        Log.d("DEBUG", "Updating UI with predicted activity")
                        predictedActivityTextView.text = "Predicted Activity: $predictedActivityName"
                    }
                }
                2 -> {
                    val output2 = Array(1) { FloatArray(4)}
                    tfliteInterpreter2.run(tfinput, output2)
                    val maxOutputValue2 = output2[0].maxOrNull()
                    val predictedActivityIndex2 = output2[0].indexOfFirst { it == maxOutputValue2 }
                    when (predictedActivityIndex2) {
                        0 -> {
                            val predictedActivityName = ACTIVITY_CODE_TO_NAME_MAPPING[5] ?: "Unknown"
                            savePredictedActivityWithTimestamp(predictedActivityName)
                            runOnUiThread {
                                Log.d("DEBUG", "Updating UI with predicted activity")
                                predictedActivityTextView.text = "Predicted Activity: $predictedActivityName"
                            }
                        }
                        1 -> {
                            val predictedActivityName = ACTIVITY_CODE_TO_NAME_MAPPING[6] ?: "Unknown"
                            savePredictedActivityWithTimestamp(predictedActivityName)
                            runOnUiThread {
                                Log.d("DEBUG", "Updating UI with predicted activity")
                                predictedActivityTextView.text = "Predicted Activity: $predictedActivityName"
                            }
                        }
                        2 -> {
                            val predictedActivityName = ACTIVITY_CODE_TO_NAME_MAPPING[9] ?: "Unknown"
                            savePredictedActivityWithTimestamp(predictedActivityName)
                            runOnUiThread {
                                Log.d("DEBUG", "Updating UI with predicted activity")
                                predictedActivityTextView.text = "Predicted Activity: $predictedActivityName"
                            }
                        }
                        3 -> {
                            val predictedActivityName = ACTIVITY_CODE_TO_NAME_MAPPING[10] ?: "Unknown"
                            savePredictedActivityWithTimestamp(predictedActivityName)
                            runOnUiThread {
                                Log.d("DEBUG", "Updating UI with predicted activity")
                                predictedActivityTextView.text = "Predicted Activity: $predictedActivityName"
                            }
                        }
                    }
                }
                3 -> {
                    val output3 = Array(1) { FloatArray(2)}
                    tfliteInterpreter3.run(tfinput, output3)
                    val maxOutputValue3 = output3[0].maxOrNull()
                    val predictedActivityIndex3 = output3[0].indexOfFirst { it == maxOutputValue3 }
                    val predictedActivityName = ACTIVITY_CODE_TO_NAME_MAPPING[predictedActivityIndex3 + 7] ?: "Unknown"
                    savePredictedActivityWithTimestamp(predictedActivityName)
                    runOnUiThread {
                        Log.d("DEBUG", "Updating UI with predicted activity")
                        predictedActivityTextView.text = "Predicted Activity: $predictedActivityName"
                    }
                }
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

    /*
    // set up the broadcast receiver
    //      thingyLiveUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

            val action = intent.action

            if (action == Constants.ACTION_THINGY_BROADCAST) {

                val liveData =
                    intent.getSerializableExtra(Constants.THINGY_LIVE_DATA) as ThingyLiveData
                Log.d("Live", "onReceive: liveData = " + liveData)

                // get all relevant intent contents
                val x = liveData.accelX
                val y = liveData.accelY
                val z = liveData.accelZ

                time += 1
                updateGraph("thingy", x, y, z)

            }
        }
    }

    // register receiver on another thread
    val handlerThreadThingy = HandlerThread("bgThreadThingyLive")
    handlerThreadThingy.start()
    looperThingy = handlerThreadThingy.looper
    val handlerThingy = Handler(looperThingy)
    this.registerReceiver(thingyLiveUpdateReceiver, filterTestThingy, null, handlerThingy)

}

*/


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

        // Thingy
        /*
    time = 0f
    val entries_thingy_accel_x = ArrayList<Entry>()
    val entries_thingy_accel_y = ArrayList<Entry>()
    val entries_thingy_accel_z = ArrayList<Entry>()

    dataSet_thingy_accel_x = LineDataSet(entries_thingy_accel_x, "Accel X")
    dataSet_thingy_accel_y = LineDataSet(entries_thingy_accel_y, "Accel Y")
    dataSet_thingy_accel_z = LineDataSet(entries_thingy_accel_z, "Accel Z")

    dataSet_thingy_accel_x.setDrawCircles(false)
    dataSet_thingy_accel_y.setDrawCircles(false)
    dataSet_thingy_accel_z.setDrawCircles(false)

    dataSet_thingy_accel_x.setColor(
        ContextCompat.getColor(
            this,
            R.color.red
        )
    )
    dataSet_thingy_accel_y.setColor(
        ContextCompat.getColor(
            this,
            R.color.green
        )
    )
    dataSet_thingy_accel_z.setColor(
        ContextCompat.getColor(
            this,
            R.color.blue
        )
    )

    val dataSetsThingy = ArrayList<ILineDataSet>()
    dataSetsThingy.add(dataSet_thingy_accel_x)
    dataSetsThingy.add(dataSet_thingy_accel_y)
    dataSetsThingy.add(dataSet_thingy_accel_z)

    allThingyData = LineData(dataSetsThingy)
    thingyChart.data = allThingyData
    thingyChart.invalidate()
}

*/
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
            /*       } else if (graph == "thingy") {
    dataSet_thingy_accel_x.addEntry(Entry(time, x))
    dataSet_thingy_accel_y.addEntry(Entry(time, y))
    dataSet_thingy_accel_z.addEntry(Entry(time, z))

    runOnUiThread {
        allThingyData.notifyDataChanged()
        thingyChart.notifyDataSetChanged()
        thingyChart.invalidate()
        thingyChart.setVisibleXRangeMaximum(150f)
        thingyChart.moveViewToX(thingyChart.lowestVisibleX + 40)
    }

*/
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(respeckLiveUpdateReceiver)
//        unregisterReceiver(thingyLiveUpdateReceiver)
        looperRespeck.quit()
//        looperThingy.quit()
    }

}


