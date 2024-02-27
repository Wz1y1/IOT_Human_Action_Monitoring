package com.specknet.pdiotapp.live

import android.util.Log
import org.jtransforms.fft.DoubleFFT_2D
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sqrt

class test {
    data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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

        return Quad(fftReal, fftImag, fftMag, spectralEntropy)
    }
}

fun main() {
    // Sample input data (50 groups of x, y, z)
    val sampleData = arrayOf(
        floatArrayOf(-0.59643555f, -0.18121338f, 0.87005615f),
        floatArrayOf(-0.59399414f, -0.18902588f, 0.88031006f),
        floatArrayOf(-0.59106445f, -0.17266846f, 0.8834839f),
        floatArrayOf(-0.58032227f, -0.17681885f, 0.8756714f),
        floatArrayOf(-0.59814453f, -0.1897583f, 0.8822632f),
        floatArrayOf(-0.58691406f, -0.17510986f, 0.8776245f),
        floatArrayOf(-0.59716797f, -0.18145752f, 0.8703003f),
        floatArrayOf(-0.5891113f, -0.175354f, 0.88031006f),
        floatArrayOf(-0.60131836f, -0.1873169f, 0.8810425f),
        floatArrayOf(-0.5930176f, -0.18243408f, 0.8764038f),
        floatArrayOf(-0.5891113f, -0.17926025f, 0.87323f),
        floatArrayOf(-0.5957031f, -0.18365479f, 0.8788452f),
        floatArrayOf(-0.5786133f, -0.18389893f, 0.883728f),
        floatArrayOf(-0.58325195f, -0.18609619f, 0.8739624f),
        floatArrayOf(-0.58032227f, -0.18463135f, 0.8805542f),
        floatArrayOf(-0.5900879f, -0.18511963f, 0.88934326f),
        floatArrayOf(-0.5969238f, -0.18609619f, 0.8925171f),
        floatArrayOf(-0.5949707f, -0.18878174f, 0.885437f),
        floatArrayOf(-0.58203125f, -0.17852783f, 0.87957764f),
        floatArrayOf(-0.57592773f, -0.1843872f, 0.8744507f),
        floatArrayOf(-0.5769043f, -0.18682861f, 0.8859253f),
        floatArrayOf(-0.5871582f, -0.18585205f, 0.88031006f),
        floatArrayOf(-0.5830078f, -0.190979f, 0.885437f),
        floatArrayOf(-0.58569336f, -0.17266846f, 0.8768921f),
        floatArrayOf(-0.592041f, -0.17193604f, 0.8912964f),
        floatArrayOf(-0.58203125f, -0.17510986f, 0.88616943f),
        floatArrayOf(-0.5852051f, -0.18243408f, 0.8966675f),
        floatArrayOf(-0.5859375f, -0.1831665f, 0.89105225f),
        floatArrayOf(-0.59521484f, -0.18292236f, 0.87371826f),
        floatArrayOf(-0.59155273f, -0.18341064f, 0.8722534f),
        floatArrayOf(-0.5793457f, -0.17584229f, 0.8788452f),
        floatArrayOf(-0.5810547f, -0.18414307f, 0.87908936f),
        floatArrayOf(-0.5925293f, -0.190979f, 0.87957764f),
        floatArrayOf(-0.5925293f, -0.18560791f, 0.8812866f),
        floatArrayOf(-0.58374023f, -0.17999268f, 0.8912964f),
        floatArrayOf(-0.59399414f, -0.1843872f, 0.8878784f),
        floatArrayOf(-0.59277344f, -0.18756104f, 0.8866577f),
        floatArrayOf(-0.57128906f, -0.19342041f, 0.8878784f),
        floatArrayOf(-0.5859375f, -0.1897583f, 0.87908936f),
        floatArrayOf(-0.579834f, -0.19024658f, 0.87176514f),
        floatArrayOf(-0.5854492f, -0.1795044f, 0.8851929f),
        floatArrayOf(-0.5800781f, -0.2041626f, 0.88275146f),
        floatArrayOf(-0.58251953f, -0.1873169f, 0.8912964f),
        floatArrayOf(-0.59375f, -0.18511963f, 0.8720093f),
        floatArrayOf(-0.57543945f, -0.20318604f, 0.89959717f),
        floatArrayOf(-0.5864258f, -0.18585205f, 0.8944702f),
        floatArrayOf(-0.5822754f, -0.18267822f, 0.8890991f),
        floatArrayOf(-0.5722656f, -0.19683838f, 0.8883667f),
        floatArrayOf(-0.5703125f, -0.18878174f, 0.89715576f),
        floatArrayOf(-0.5715332f, -0.19781494f, 0.8937378f)
    )

        // Create an instance of your test class
    val testClass = test()

    // Process the FFT
    val (fftReal, fftImag, fftMag, spectralEntropy) = testClass.processForFFT(sampleData)

    // Output the results for verification
    println("FFT Real: ${fftReal.contentDeepToString()}")
    println("FFT Imaginary: ${fftImag.contentDeepToString()}")
    println("FFT Magnitude: ${fftMag.contentDeepToString()}")
    println("Spectral Entropy: ${spectralEntropy.contentDeepToString()}")
}


