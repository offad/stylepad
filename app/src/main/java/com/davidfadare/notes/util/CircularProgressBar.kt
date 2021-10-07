package com.davidfadare.notes.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.davidfadare.notes.R
import kotlin.math.min
import kotlin.math.round

class CircularProgressBar(context: Context, attrs: AttributeSet): View(context, attrs){
    private var strokeWidth = 8f
    private var progress = 0f
    private var min = 0
    var max = 0

    private var startAngle = -90f
    private var color = Color.DKGRAY
    private var rectF: RectF
    private var backgroundPaint: Paint
    private var foregroundPaint: Paint

    init{
        rectF = RectF()
        val typedArray = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.CircularProgressBar,
                0, 0)
        try {
            strokeWidth = typedArray.getDimension(R.styleable.CircularProgressBar_progressbarThickness, strokeWidth)
            progress = typedArray.getFloat(R.styleable.CircularProgressBar_progressValue, progress)
            max = typedArray.getInt(R.styleable.CircularProgressBar_max, max)
            min = typedArray.getInt(R.styleable.CircularProgressBar_min, min)
            color = typedArray.getInt(R.styleable.CircularProgressBar_progressbarColor, color)
        } finally {
            typedArray.recycle()
        }

        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        backgroundPaint.color = adjustAlpha(color, 0.3f)
        backgroundPaint.style = Paint.Style.STROKE
        backgroundPaint.strokeWidth = strokeWidth

        foregroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        foregroundPaint.color = color
        foregroundPaint.style = Paint.Style.STROKE
        foregroundPaint.strokeWidth = strokeWidth
    }

    private fun adjustAlpha(color: Int, factor: Float): Int{
        val alpha = round(Color.alpha(color) * factor)
        val red = Color.red(color)
        val blue = Color.red(color)
        val green = Color.red(color)
        return Color.argb(alpha.toInt(), red, green, blue)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val min = min(width, height)
        setMeasuredDimension(min, min)
        rectF.set(0 + strokeWidth / 2, 0 + strokeWidth / 2,min - strokeWidth / 2, min - strokeWidth / 2)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawOval(rectF, backgroundPaint)
        val angle = 360f * progress / max
        canvas?.drawArc(rectF, startAngle, angle, false, foregroundPaint)
    }

    fun getProgress(): Float{
        return progress
    }

    fun setProgress(progress: Float){
        this.progress = progress
        invalidate()
    }

    fun setColor(color: Int){
        this.color = color
        invalidate()
    }
}
