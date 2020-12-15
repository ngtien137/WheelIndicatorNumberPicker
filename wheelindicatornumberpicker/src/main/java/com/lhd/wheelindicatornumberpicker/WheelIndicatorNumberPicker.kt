package com.lhd.wheelindicatornumberpicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs

class WheelIndicatorNumberPicker @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private val COLOR_CIRCLE_SELECTED = Color.parseColor("#7767e4")
        private val COLOR_INDICATOR_LINE = Color.WHITE
        private val COLOR_TEXT_UNSELECTED = Color.parseColor("#c1c1c1")
        private val COLOR_TEXT_SELECTED = Color.WHITE
    }

    private val rectView = RectF()

    /**
     * Selected circle
     */

    //region selected circle

    private val paintSelectedCircle = Paint(Paint.ANTI_ALIAS_FLAG)

    private val paintTempSelectedCircle = Paint(Paint.ANTI_ALIAS_FLAG)

    //endregion

    /**
     * indicator line
     */
    //region indicator line

    private val paintIndicatorLine = Paint(Paint.ANTI_ALIAS_FLAG)

    private var indicatorLineHeight = 0f
    private var indicatorLineMaxHeight = 0f
    private var indicatorWidth = 0f
    private var indicatorSpace = 0f
    private var indicatorPaddingText = 0f

    //endregion

    /**
     * Text
     */

    //region text attributes

    private val rectSelectedText = Rect()
    private val rectText = Rect()

    private val paintUnSelectedText = TextPaint(Paint.ANTI_ALIAS_FLAG)

    private val paintSelectedText = TextPaint(Paint.ANTI_ALIAS_FLAG)

    //endregion

    /**
     * Progress
     */

    //region progress

    private var widthFullSize = 0f
    private var min = 0f
    private var max = 0f
    private var progress = 0f

    //endregion

    private var pointDown = PointF()
    private var isScrolling = false
    private var currentDx = 0f
    private val scaleTouchSlop by lazy {
        ViewConfiguration.get(context).scaledTouchSlop
    }

    init {
        attrs?.let {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.WheelIndicatorNumberPicker)
            paintSelectedText.color = ta.getColor(
                R.styleable.WheelIndicatorNumberPicker_winp_text_selected_color,
                COLOR_TEXT_SELECTED
            )
            paintUnSelectedText.color = ta.getColor(
                R.styleable.WheelIndicatorNumberPicker_winp_text_unselected_color,
                COLOR_TEXT_UNSELECTED
            )
            paintSelectedText.textSize = ta.getDimension(
                R.styleable.WheelIndicatorNumberPicker_winp_text_selected_size,
                dpToPixel(16f)
            )
            paintUnSelectedText.textSize = ta.getDimension(
                R.styleable.WheelIndicatorNumberPicker_winp_text_unselected_size,
                dpToPixel(14f)
            )

            paintIndicatorLine.color = ta.getColor(
                R.styleable.WheelIndicatorNumberPicker_winp_indicator_line_color,
                COLOR_INDICATOR_LINE
            )
            indicatorLineHeight = ta.getDimension(
                R.styleable.WheelIndicatorNumberPicker_winp_indicator_line_height,
                dpToPixel(16f)
            )
            indicatorLineMaxHeight = ta.getDimension(
                R.styleable.WheelIndicatorNumberPicker_winp_indicator_line_max_height,
                dpToPixel(28f)
            )
            indicatorWidth = ta.getDimension(
                R.styleable.WheelIndicatorNumberPicker_winp_indicator_line_width,
                dpToPixel(1f)
            )
            paintIndicatorLine.strokeWidth = indicatorWidth
            indicatorSpace = ta.getDimension(
                R.styleable.WheelIndicatorNumberPicker_winp_indicator_line_space,
                dpToPixel(40f)
            )
            indicatorPaddingText = ta.getDimension(
                R.styleable.WheelIndicatorNumberPicker_winp_indicator_line_padding_with_text,
                dpToPixel(8f)
            )

            paintSelectedCircle.color = ta.getColor(
                R.styleable.WheelIndicatorNumberPicker_winp_circle_selected_color,
                COLOR_CIRCLE_SELECTED
            )
            paintTempSelectedCircle.color = ta.getColor(
                R.styleable.WheelIndicatorNumberPicker_winp_circle_selected_color,
                COLOR_CIRCLE_SELECTED
            )

            min = ta.getFloat(R.styleable.WheelIndicatorNumberPicker_winp_min, 0f)
            max = ta.getFloat(R.styleable.WheelIndicatorNumberPicker_winp_max, 100f) + 1
            progress = ta.getFloat(R.styleable.WheelIndicatorNumberPicker_winp_progress, min)

            ta.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthView = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        val heightView = MeasureSpec.getSize(heightMeasureSpec).toFloat()
        rectView.set(0f, 0f, widthView, heightView)
        widthFullSize = (max - min) * indicatorSpace
        paintSelectedText.getTextBounds("0", 0, 1, rectSelectedText)
        paintSelectedText.getTextBounds("0", 0, 1, rectText)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun convertCurrentScrollToProgress(): Float {
        return 0f
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.let { canvas ->
            val progressOfFullSize =
                if (scrollX >= 0) scrollX % widthFullSize else widthFullSize - abs(scrollX) % widthFullSize
            val startDrawProgress = (progressOfFullSize / widthFullSize) * (max - min)
            var offset = scrollX - (progressOfFullSize % widthFullSize) % indicatorSpace
            val textUnSelectedCenter = rectView.centerY()
            var progressDraw = startDrawProgress
            while (offset <= scrollX + rectView.width() + indicatorSpace) {
                val textDraw = progressDraw.toInt().toString()
                val textWidth = paintUnSelectedText.measureText(textDraw)
                val bottomText = textUnSelectedCenter + rectText.height() / 2f
                canvas.drawText(
                    textDraw,
                    offset - textWidth / 2f,
                    bottomText,
                    paintUnSelectedText
                )
                val topLine = bottomText + indicatorPaddingText
                canvas.drawLine(
                    offset,
                    topLine,
                    offset,
                    topLine + indicatorLineHeight,
                    paintIndicatorLine
                )
                offset += indicatorSpace
                progressDraw += 1
                if (progressDraw > max) {
                    progressDraw = min
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pointDown.set(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val disX = event.x - pointDown.x
                return if (isScrolling) {
                    scroll(-disX.toInt())
                    currentDx = disX
                    pointDown.set(event.x, event.y)
                    true
                } else {
                    if (abs(disX) > scaleTouchSlop) {
                        isScrolling = true
                        true
                    } else {
                        false
                    }
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (isScrolling) {
                    isScrolling = false
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun scroll(disX: Int) {
        scrollBy(disX, 0)
    }

}