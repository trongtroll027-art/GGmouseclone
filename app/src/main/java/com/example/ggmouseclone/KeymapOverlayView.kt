package com.example.ggmouseclone

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout

class KeymapOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val paint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    init {
        addDefaultButtons()
    }

    private fun addDefaultButtons() {
        val aimBtn = createDraggableButton("AIMLOCK", 200f, 400f)
        addView(aimBtn)
    }

    private fun createDraggableButton(text: String, x: Float, y: Float): View {
        val btn = android.widget.Button(context).apply {
            this.text = text
            this.textSize = 14f
            this.setBackgroundColor(Color.parseColor("#80000000"))
            this.setTextColor(Color.CYAN)
            this.x = x
            this.y = y
        }

        btn.setOnTouchListener(object : OnTouchListener {
            private var initialX = 0f
            private var initialY = 0f
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = v.x
                        initialY = v.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        v.x = initialX + (event.rawX - initialTouchX)
                        v.y = initialY + (event.rawY - initialTouchY)
                        return true
                    }
                }
                return false
            }
        })
        return btn
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            canvas.drawRect(child.x, child.y, child.x + child.width, child.y + child.height, paint)
        }
    }
}
