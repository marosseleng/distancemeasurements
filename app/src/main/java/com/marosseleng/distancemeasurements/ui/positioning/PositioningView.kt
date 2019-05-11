/*
 * Copyright 2019 Maroš Šeleng
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.marosseleng.distancemeasurements.ui.positioning

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver
import com.lemmingapex.trilateration.TrilaterationFunction
import org.apache.commons.math3.filter.DefaultMeasurementModel
import org.apache.commons.math3.filter.DefaultProcessModel
import org.apache.commons.math3.filter.KalmanFilter
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.linear.RealMatrix
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


/**
 * @author Maroš Šeleng
 */
@RequiresApi(Build.VERSION_CODES.N)
class PositioningView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var distances: Map<ApInSpace, Int> = mapOf()
        set(newValue) {
            field = newValue
            minX = newValue.minBy { it.key.position.first - it.value }?.run { key.position.first - value } ?: 0
            maxX = newValue.maxBy { it.key.position.first + it.value }?.run { key.position.first + value } ?: 0
            minY = newValue.minBy { it.key.position.second - it.value }?.run { key.position.second - value } ?: 0
            maxY = newValue.maxBy { it.key.position.second + it.value }?.run { key.position.second + value } ?: 0

            // uncomment when the range of APs is not desired to be taken into the account
            // minX = newValue.minBy { it.key.position.first }?.run { key.position.first } ?: 0
            // maxX = newValue.maxBy { it.key.position.first }?.run { key.position.first } ?: 0
            // minY = newValue.minBy { it.key.position.second }?.run { key.position.second } ?: 0
            // maxY = newValue.maxBy { it.key.position.second }?.run { key.position.second } ?: 0

            val positions: MutableList<DoubleArray> = mutableListOf()
            val dst: MutableList<Double> = mutableListOf()

            newValue.forEach { (_, _, location), distance ->
                val (x, y) = location
                positions.add(doubleArrayOf(x.toDouble(), y.toDouble()))
                dst.add(distance.toDouble())
            }

            // TODO offload to the background thread
            val solver =
                NonLinearLeastSquaresSolver(
                    TrilaterationFunction(positions.toTypedArray(), dst.toDoubleArray()),
                    LevenbergMarquardtOptimizer()
                )
            val optimum = solver.solve()

            val pointVector = optimum.point.toArray()
            val triangulatedPos = pointVector[0].toInt() to pointVector[1].toInt()

            kalmanFilterX.predict()
            var z = kalmanHx.operate(doubleArrayOf(triangulatedPos.first.toDouble()))
            kalmanFilterX.correct(z)
            val correctedX = kalmanFilterX.stateEstimation[0].toInt()

            kalmanFilterY.predict()
            z = kalmanHy.operate(doubleArrayOf(triangulatedPos.second.toDouble()))
            kalmanFilterY.correct(z)
            val correctedY = kalmanFilterY.stateEstimation[0].toInt()

            myPositionCm = correctedX to correctedY

            invalidate()
        }

    private var minX = 0
    private var maxX = width - 1
    private var minY = 0
    private var maxY = height - 1
    private var cmToPxRatio = 1f
    private var myPositionCm: Pair<Int, Int> = Pair(0, 0)

    private lateinit var kalmanHx: RealMatrix
    private lateinit var kalmanHy: RealMatrix

    private val kalmanFilterX: KalmanFilter by lazy {
        val constantVoltage = 0.0
        val measurementNoise = 0.1
        val processNoise = 1e-5

        // A = [ 1 ]
        val A = Array2DRowRealMatrix(doubleArrayOf(1.0))
        // B = null
        val B: RealMatrix? = null
        // H = [ 1 ]
        val H = Array2DRowRealMatrix(doubleArrayOf(1.0))
        kalmanHx = H
        // x = [ 10 ]
        val x = ArrayRealVector(doubleArrayOf(constantVoltage))
        // Q = [ 1e-5 ]
        val Q = Array2DRowRealMatrix(doubleArrayOf(processNoise))
        // P = [ 1 ]
        val P0 = Array2DRowRealMatrix(doubleArrayOf(1.0))
        // R = [ 0.1 ]
        val R = Array2DRowRealMatrix(doubleArrayOf(measurementNoise))

        val pm = DefaultProcessModel(A, B, Q, x, P0)
        val mm = DefaultMeasurementModel(H, R)
        KalmanFilter(pm, mm)
    }

    private val kalmanFilterY: KalmanFilter by lazy {
        val constantVoltage = 0.0
        val measurementNoise = 0.1
        val processNoise = 1e-5

        // A = [ 1 ]
        val A = Array2DRowRealMatrix(doubleArrayOf(1.0))
        // B = null
        val B: RealMatrix? = null
        // H = [ 1 ]
        val H = Array2DRowRealMatrix(doubleArrayOf(1.0))
        kalmanHy = H
        // x = [ 10 ]
        val x = ArrayRealVector(doubleArrayOf(constantVoltage))
        // Q = [ 1e-5 ]
        val Q = Array2DRowRealMatrix(doubleArrayOf(processNoise))
        // P = [ 1 ]
        val P0 = Array2DRowRealMatrix(doubleArrayOf(1.0))
        // R = [ 0.1 ]
        val R = Array2DRowRealMatrix(doubleArrayOf(measurementNoise))

        val pm = DefaultProcessModel(A, B, Q, x, P0)
        val mm = DefaultMeasurementModel(H, R)
        KalmanFilter(pm, mm)
    }

    private val graphicsPaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
        }
    }

    private val textPaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            textSize = 30f
        }
    }

    private val colors = listOf(
        Color.BLACK,
        Color.RED,
        Color.GREEN,
        Color.YELLOW,
        Color.CYAN,
        Color.MAGENTA
    )

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        cmToPxRatio = min(width.toFloat() / (maxX - minX), height.toFloat() / (maxY - minY))

        if (canvas == null) {
            return
        }

        drawRoomBounds(canvas)

        var i = 0
        distances.forEach {
            drawAp(it, canvas, colors[i++ % colors.size])
        }

        drawComputedPosition(myPositionCm, canvas)
    }

    private fun drawRoomBounds(canvas: Canvas) {
        val strokeWidth = 4f
        val xPx = abs(minX) * cmToPxRatio - strokeWidth / 2
        val yPx = abs(minY) * cmToPxRatio - strokeWidth / 2
        val textToDraw = "[0, 0]"

        val color = Color.DKGRAY

        graphicsPaint.color = color
        graphicsPaint.style = Paint.Style.STROKE
        graphicsPaint.strokeWidth = strokeWidth
        canvas.drawLine(xPx, yPx, width.toFloat(), yPx, graphicsPaint)
        canvas.drawLine(xPx, yPx, xPx, height.toFloat(), graphicsPaint)

        textPaint.color = color
        val textBounds = Rect()
        textPaint.getTextBounds(textToDraw, 0, textToDraw.length, textBounds)
        val textWidth = textPaint.measureText(textToDraw).toInt()
        val textHeight = textBounds.height()

        val textStartX = if ((xPx - textWidth - 20) < 0) xPx + 20 else (xPx - textWidth - 20)
        val textStartY = if ((yPx - textHeight - 8) < 0) yPx + textHeight + 8 else (yPx - 8)

        canvas.drawText(textToDraw, textStartX, textStartY, textPaint)
    }

    private fun drawAp(entry: Map.Entry<ApInSpace, Int>, canvas: Canvas, @ColorInt color: Int) {
        val (ap, distance) = entry
        val offsetX = if (minX < 0) abs(minX) else 0
        val offsetY = if (minY < 0) abs(minY) else 0
        val xPx = (ap.position.first + offsetX) * cmToPxRatio
        val yPx = (ap.position.second + offsetY) * cmToPxRatio
        val distancePx = distance * cmToPxRatio
        val centerRadiusPx = max(20f, 20 * cmToPxRatio)
        val macAddress = if (ap.name?.isNotBlank() == true) ap.name else ap.macAddress
        val textToDraw = "$macAddress ($distance cm)"

        val lightColor = Color.argb(20, color.red, color.green, color.blue)

        graphicsPaint.color = lightColor
        graphicsPaint.style = Paint.Style.FILL
        canvas.drawCircle(xPx, yPx, distancePx, graphicsPaint)

        graphicsPaint.color = color
        graphicsPaint.style = Paint.Style.STROKE
        graphicsPaint.strokeWidth = 1f
        canvas.drawCircle(xPx, yPx, distancePx, graphicsPaint)

        graphicsPaint.color = color
        graphicsPaint.style = Paint.Style.FILL_AND_STROKE
        graphicsPaint.strokeWidth = 0f
        canvas.drawCircle(xPx, yPx, centerRadiusPx, graphicsPaint)

        textPaint.color = color
        val textBounds = Rect()
        textPaint.getTextBounds(textToDraw, 0, textToDraw.length, textBounds)
        val textWidth = textPaint.measureText(textToDraw).toInt()
        val textHeight = textBounds.height()

        canvas.drawText(textToDraw, xPx - textWidth / 2f, yPx + 3 * centerRadiusPx + textHeight / 2f, textPaint)
    }

    private fun drawComputedPosition(positionCm: Pair<Int, Int>, canvas: Canvas) {
        val offsetX = if (minX < 0) abs(minX) else 0
        val offsetY = if (minY < 0) abs(minY) else 0
        val xPx = (positionCm.first + offsetX) * cmToPxRatio
        val yPx = (positionCm.second + offsetY) * cmToPxRatio
        val coordinatesString = "[${positionCm.first}, ${positionCm.second}]"
        val smallRadiusPx = 15f
        val bigRadiusPx = 23f

        val color = Color.parseColor("#4396BD")
        val lightColor = Color.argb(70, color.red, color.green, color.blue)

        graphicsPaint.color = lightColor
        graphicsPaint.style = Paint.Style.FILL
        canvas.drawCircle(xPx, yPx, bigRadiusPx, graphicsPaint)

        graphicsPaint.color = color
        graphicsPaint.style = Paint.Style.STROKE
        graphicsPaint.strokeWidth = 1f
        canvas.drawCircle(xPx, yPx, bigRadiusPx, graphicsPaint)

        graphicsPaint.color = color
        graphicsPaint.style = Paint.Style.FILL_AND_STROKE
        graphicsPaint.strokeWidth = 0f
        canvas.drawCircle(xPx, yPx, smallRadiusPx, graphicsPaint)

        textPaint.color = color
        val textBounds = Rect()
        textPaint.getTextBounds(coordinatesString, 0, coordinatesString.length, textBounds)
        val textWidth = textPaint.measureText(coordinatesString).toInt()
        val textHeight = textBounds.height()

        canvas.drawText(coordinatesString, xPx - textWidth / 2f, yPx + 2 * bigRadiusPx + textHeight / 2f, textPaint)
    }
}