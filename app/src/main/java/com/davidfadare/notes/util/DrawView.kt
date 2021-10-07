package com.davidfadare.notes.util

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.davidfadare.notes.R

class DrawView internal constructor(context: Context, var mBitmap: Bitmap? = null) : View(context) {

    companion object {
        const val STROKE_WIDTH: Float = 2.5f
    }

    class ColoredPath(val path: Path = Path(), val paint: Paint = Paint())

    var path = Path()
    var paint = Paint()
    var edited = false

    private val paths = ArrayList<ColoredPath>()
    private val clearedPaths = ArrayList<ColoredPath>()
    private val undonePaths = ArrayList<ColoredPath>()

    private var clearedBitmap: Bitmap? = null

    private var selectedColor: Int = Color.BLACK
    private var lastTouchX: Float? = null
    private var lastTouchY: Float? = null
    private var dirtyRect: RectF = RectF()

    init {
        paint.isAntiAlias = true
        paint.color = selectedColor
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeWidth = STROKE_WIDTH
    }


    fun changePaint(color: Int) {
        selectedColor = color
        paint.color = color
    }

    fun clear() {
        clearedPaths.addAll(paths)
        if (mBitmap != null) clearedBitmap = Bitmap.createBitmap(mBitmap!!)
        mBitmap = null
        paths.clear()
        invalidate()
    }

    fun redo() {
        if (undonePaths.size > 0) {
            paths.add(undonePaths.removeAt(undonePaths.size - 1))
            invalidate()
        } else {
            Toast.makeText(context, R.string.editor_drawing_redo_error, Toast.LENGTH_SHORT).show()
        }
    }

    fun undo() {
        if (clearedBitmap != null) {
            mBitmap = Bitmap.createBitmap(clearedBitmap!!)
            clearedBitmap = null
        }
        if (paths.size > 0) {
            undonePaths.add(paths.removeAt(paths.size - 1))
            invalidate()
        } else if (clearedPaths.size > 0) {
            paths.addAll(clearedPaths)
            clearedPaths.clear()
            invalidate()
        } else {
            Toast.makeText(context, R.string.editor_drawing_undo_error, Toast.LENGTH_SHORT).show()
        }
    }

    fun getBitmap(): Bitmap? {
        val returnedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        draw(canvas)
        return returnedBitmap
    }

    override fun onDraw(canvas: Canvas?) {
        setBackgroundColor(Color.WHITE)
        if (mBitmap != null) {
            canvas?.drawBitmap(mBitmap!!, 0f, 0f, null)
        }
        for (p in paths) {
            paint.color = p.paint.color
            canvas?.drawPath(p.path, paint)
        }
        paint.color = selectedColor
        canvas?.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val eventX: Float = event?.x ?: 0f
        val eventY: Float = event?.y ?: 0f

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                undonePaths.clear()
                clearedPaths.clear()
                clearedBitmap = null

                path.reset()
                path.moveTo(eventX, eventY)
                lastTouchX = eventX
                lastTouchY = eventY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                resetDirtyRect(eventX, eventY)
                val historySize = event.historySize
                var i = 0
                while (i < historySize) {
                    val historicalX = event.getHistoricalX(i)
                    val historicalY = event.getHistoricalY(i)
                    expandDirtyRect(historicalX, historicalY)
                    path.lineTo(historicalX, historicalY)

                    i++
                }
                path.lineTo(eventX, eventY)
            }
            MotionEvent.ACTION_UP -> {
                paths.add(ColoredPath(path, Paint(paint)))
                path.lineTo(eventX, eventY)
                path = Path()

                edited = true
            }
            else -> {
                return false
            }
        }

        invalidate()
        lastTouchX = eventX
        lastTouchY = eventY
        return true
    }

    private fun expandDirtyRect(historicalX: Float, historicalY: Float) {
        if (historicalX < dirtyRect.left) {
            dirtyRect.left = historicalX
        } else if (historicalX > dirtyRect.right) {
            dirtyRect.right = historicalX
        }

        if (historicalY < dirtyRect.top) {
            dirtyRect.top = historicalY
        } else if (historicalY > dirtyRect.bottom) {
            dirtyRect.bottom = historicalY
        }
    }

    private fun resetDirtyRect(eventX: Float, eventY: Float) {
        if (lastTouchX != null && lastTouchY != null) {
            dirtyRect.left = Math.min(lastTouchX!!, eventX)
            dirtyRect.right = Math.min(lastTouchX!!, eventX)
            dirtyRect.top = Math.min(lastTouchY!!, eventY)
            dirtyRect.bottom = Math.min(lastTouchY!!, eventY)
        }
    }
}