package com.lhd.wheelindicatornumberpicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.Animation
import android.view.animation.Transformation
import kotlin.math.abs
import kotlin.math.roundToInt

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

    private var sizeSelectedCircle = 0f

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

    private var isSnapSupported = true
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
            sizeSelectedCircle = ta.getDimension(
                R.styleable.WheelIndicatorNumberPicker_winp_circle_selected_full_size,
                dpToPixel(40f)
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
        post {
            setProgress(0f)
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        canvas?.let { canvas ->
            val progressOfFullSize =
                if (scrollX >= 0) scrollX % widthFullSize else widthFullSize - abs(scrollX) % widthFullSize
            val startDrawProgress = (progressOfFullSize / widthFullSize) * (max - min)
            var offset =
                scrollX - (progressOfFullSize % widthFullSize) % indicatorSpace + (rectView.width() / 2f) % indicatorSpace
            val textUnSelectedCenter =
                rectView.centerY() - (rectText.height() + indicatorPaddingText + indicatorLineHeight * 2 - sizeSelectedCircle) / 2f
            var progressDraw = startDrawProgress
            if (progressDraw > max)
                progressDraw = max
            val progressCenter = getProgressAtCenter()
            val progressCurrent = progressCenter.toInt()
            var progressNext = progressCurrent + 1
            if (progressNext >= max)
                progressNext = min.toInt()
            canvas.drawLine(
                scrollX + rectView.width() / 2f,
                rectView.top,
                scrollX + rectView.width() / 2f,
                rectView.bottom,
                paintIndicatorLine
            )
            while (offset <= scrollX + rectView.width() + indicatorSpace) {
                val textDraw = progressDraw.toInt().toString()
                val textWidth = paintUnSelectedText.measureText(textDraw)
                val bottomText = textUnSelectedCenter + rectText.height() / 2f
                val centerText = bottomText - rectText.height() / 2f

                val sizeOval: Float
                var isSelected = false
                val percent: Float
                when {
                    progressDraw.toInt() == progressCurrent -> {
                        percent = 1 - abs(progressCenter % 1)
                        paintSelectedCircle.alpha = (255 * percent).roundToInt()
                        sizeOval = percent * sizeSelectedCircle
                    }
                    progressDraw.toInt() == progressNext -> {
                        percent = abs(progressCenter % 1)
                        paintSelectedCircle.alpha = (255 * percent).roundToInt()
                        sizeOval = (percent * sizeSelectedCircle)
                    }
                    else -> {
                        paintSelectedCircle.alpha = 255
                        percent = 0f
                        sizeOval = 0f
                    }
                }
                if (sizeOval > sizeSelectedCircle / 2) {
                    isSelected = true
                }
                val bottomOval = centerText + sizeOval / 2f
                val rectOval = RectF(
                    offset - sizeOval / 2f,
                    centerText - sizeOval / 2f,
                    offset + sizeOval / 2f,
                    bottomOval
                )
                canvas.drawOval(rectOval, paintSelectedCircle)

                val paintText = if (isSelected) paintSelectedText else paintUnSelectedText

                canvas.drawText(
                    textDraw,
                    offset - textWidth / 2f,
                    bottomText,
                    paintText
                )
                var topLine = bottomText + indicatorPaddingText
                if (bottomOval > topLine) {
                    topLine = bottomOval + (indicatorPaddingText / 2f) * percent
                }
                canvas.drawLine(
                    offset,
                    topLine,
                    offset,
                    topLine + indicatorLineHeight + indicatorLineHeight * percent,
                    paintIndicatorLine
                )
                offset += indicatorSpace
                progressDraw += 1
                if (progressDraw >= max) {
                    progressDraw = min
                }
            }
        }
    }

    private fun getProgressAtCenter(): Float {
        val paddingByCenter = (rectView.width() / 2f) % indicatorSpace
        val progressOfFullSize =
            if (scrollX - paddingByCenter + rectView.width() / 2f >= 0) (scrollX - paddingByCenter + rectView.width() / 2f) % widthFullSize else widthFullSize - abs(
                scrollX - paddingByCenter + rectView.width() / 2f
            ) % widthFullSize
        return (progressOfFullSize / widthFullSize) * (max - min)
    }

    private fun getStartProgressForDrawing(): Float {
        val progressOfFullSize =
            if (scrollX >= 0) scrollX % widthFullSize else widthFullSize - abs(scrollX) % widthFullSize
        return (progressOfFullSize / widthFullSize) * (max - min)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                anim?.cancel()
                pointDown.set(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val disX = event.x - pointDown.x
                return if (isScrolling) {
                    scroll(-disX.toInt())
                    currentDx = disX
                    pointDown.set(event.x, event.y)
                    eLog("Progress: ${getProgressAtCenter()}")
                    eLog("Scroll X: $scrollX")
                    eLog("=====")
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
                    if (isSnapSupported) {
                        setProgressWithAnimation(getProgressAtCenter().roundToInt())
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun scroll(disX: Int) {
        scrollBy(disX, 0)
    }

    fun setProgress(progress: Float) {
        val centerProgress = getProgressAtCenter()
        val distanceTranslate = abs(centerProgress - progress) * indicatorSpace
        if (progress < centerProgress) {
            scroll(-distanceTranslate.toInt())
        } else {
            scroll(distanceTranslate.toInt())
        }
    }

    private var anim: ProgressAnimation? = null
    fun setProgressWithAnimation(progress: Int) {
        anim = ProgressAnimation(this, getProgressAtCenter(), progress.toFloat())
        anim?.startAnim(200)
    }

    class ProgressAnimation(
        private val view: WheelIndicatorNumberPicker,
        private var oldProgress: Float,
        private var newProgress: Float
    ) : Animation() {
        private var currentProgress: Float = 0F

        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            val progress = oldProgress + (newProgress - oldProgress) * interpolatedTime
            currentProgress = progress
            view.setProgress(progress)
        }

        fun startAnim(duration: Long = 1000) {
            setDuration(duration)
            view.startAnimation(this)
        }

        override fun cancel() {
            view.setProgress(currentProgress)
            super.cancel()
        }

    }


}