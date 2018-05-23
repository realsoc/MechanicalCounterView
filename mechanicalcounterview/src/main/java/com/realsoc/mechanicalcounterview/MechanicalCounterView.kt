package com.realsoc.mechanicalcounterview

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.properties.Delegates

class MechanicalCounterView @JvmOverloads constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0): View(context, attrs, defStyleAttr), ValueAnimator.AnimatorUpdateListener {

    private lateinit var paint: Paint
    private var r: Rect = Rect()
    private var animator: ValueAnimator? = ValueAnimator()

    private var canvasWidth: Int = -1
    private var marginSize: Int = -1
    private var canvasHeight: Int = -1
    private var maxCharacterWidth: Int = -1
    private var maxCharacterHeight: Int = -1
    private val upCoordinates = Coordinates()

    private val downCoordinates = Coordinates()
    private var progress: Int = 0
    private var shouldRotate: Boolean = true
    private var _goal: Int = 0
    private var _currentValue: Int = 0
    private var running: Boolean = false

    var onCountTerminatedListener: OnCountStopListener? = null
    var textSize: Int by Delegates.notNull()
    var bold: Boolean by Delegates.notNull()
    var autoStart: Boolean by Delegates.notNull()
    var duration: Long by Delegates.notNull()
    var color: Int by Delegates.notNull()

    var currentValue: Int
        set(value) {
            _currentValue = value

        }
        get() = _currentValue

    var goal: Int = 0
        get() = _goal
        set(value) {
            _goal = abs(value)
            animator?.cancel()
            if (field != _currentValue && autoStart) {
                start()
            }
        }


    var digitNumber: Int = 4
        set(value) {
            field = value
            requestLayout()
        }

    enum class AnimationDirection(val value: Int) {
        ALWAYS_UP(0), ALWAYS_DOWN(1), MORE_UP(2), MORE_DOWN(3);

        companion object {
            fun getEnum(value: Int): AnimationDirection {
                return values().find { value == it.value } ?: MORE_UP
            }
        }
    }

    var direction: AnimationDirection = AnimationDirection.MORE_UP

    enum class AnimationType(val value: Int) {
        ACCELERATE(0), DECELERATE(1), LINEAR(2), ACCELERATE_DECELERATE(3);

        fun getInterpolator(factor: Float): Interpolator {
            return when(this) {
                ACCELERATE -> AccelerateInterpolator(factor)
                DECELERATE -> DecelerateInterpolator(factor)
                LINEAR -> LinearInterpolator()
                ACCELERATE_DECELERATE -> AccelerateDecelerateInterpolator()
            }
        }

        companion object {
            fun getEnum(value: Int): AnimationType {
                return values().find { value == it.value } ?: DECELERATE
            }
        }
    }

    var mode: AnimationType = AnimationType.DECELERATE
    var modeFactor: Float by Delegates.notNull()

    init {
        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.MechanicalCounterView)

        _currentValue = typedArray.getInteger(R.styleable.MechanicalCounterView_currentValue, 0)
        autoStart = typedArray.getBoolean(R.styleable.MechanicalCounterView_autoStart, true)
        bold = typedArray.getBoolean(R.styleable.MechanicalCounterView_bold, false)
        color = typedArray.getColor(R.styleable.MechanicalCounterView_counterColor, ContextCompat.getColor(context, android.R.color.black))
        textSize = typedArray.getDimensionPixelSize(R.styleable.MechanicalCounterView_size, 20)
        duration = typedArray.getInt(R.styleable.MechanicalCounterView_duration, 3000).toLong()
        digitNumber = typedArray.getInteger(R.styleable.MechanicalCounterView_digitNumber, 4)
        direction = AnimationDirection.getEnum(typedArray.getInteger(R.styleable.MechanicalCounterView_direction, 2))
        mode = AnimationType.getEnum(typedArray.getInteger(R.styleable.MechanicalCounterView_mode, 1))
        modeFactor = typedArray.getFloat(R.styleable.MechanicalCounterView_modeFactor, 1f)
        _goal = typedArray.getInteger(R.styleable.MechanicalCounterView_goal, 9*10.toDouble().pow(digitNumber - 1).toInt())

        typedArray.recycle()

        currentValue = _currentValue
        goal = _goal
    }

    override fun onAnimationUpdate(animation: ValueAnimator?) {
        progress = animation?.animatedValue as Int
        _currentValue = progress.div(1000)
        invalidate()
        if (_currentValue == _goal) {
            stop()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (running) {
            shouldRotate = true
            for (currentDigitNumber in 1..digitNumber) {
                val lowDigit = progress.div(10.toDouble().pow(currentDigitNumber + 2)).rem(10).toInt()
                val highDigit = (lowDigit + 1).rem(10)
                val showHigh = if (shouldRotate) {
                    progress.rem(1000)
                } else {
                    0
                }
                shouldRotate = shouldRotate && ((_goal >= _currentValue && lowDigit == 9) || (_goal <= _currentValue && highDigit == 0))
                canvas?.apply {
                    drawDigitZone(this, lowDigit, highDigit, currentDigitNumber, showHigh)
                }
            }
        } else {
            canvas?.apply {
                drawNumber(this, _currentValue)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        initializePaint()
        initializeMaxSize()
        initializeCanvasSize()
        setMeasuredDimension(canvasWidth, canvasHeight)
    }

    fun start() {
        if (running) {
            reset()
        } else {
            animator = ValueAnimator().apply {
                setIntValues(_currentValue * 1000, _goal * 1000)
                addUpdateListener(this@MechanicalCounterView)
                interpolator = mode.getInterpolator(modeFactor)
                duration = this@MechanicalCounterView.duration
                this@MechanicalCounterView.running = true
                start()
            }
        }
    }

    fun stop() {
        Log.e("Lib", "Stop")
        if (running) {
            onCountTerminatedListener?.apply {
                onCountStop(_currentValue)
                Log.e("Lib", "sending to countstoplistener " + _currentValue.toString())
            }
            animator?.cancel()
            running = false
            invalidate()
        }
    }

    fun reset() {
        stop()
        start()
    }

    private enum class DigitType {
        DIGIT_DOWN, DIGIT_UP
    }

    private fun drawDigitZone(canvas: Canvas, lowDigit: Int, highDigit: Int, position: Int, showHigh: Int) {
        var downDigit = ""
        var upDigit = ""
        val offset: Int =
                /** Counting up*/
                if (_goal > currentValue) {
                    when (direction) {
                    /** Low digit is up, high is down */
                        AnimationDirection.ALWAYS_UP, AnimationDirection.MORE_UP-> {
                            upDigit = lowDigit.toString()
                            downDigit = highDigit.toString()
                            1000 - showHigh
                        }
                    /** Low digit is down, high is up */
                        AnimationDirection.ALWAYS_DOWN, AnimationDirection.MORE_DOWN -> {
                            upDigit = highDigit.toString()
                            downDigit = lowDigit.toString()
                            showHigh
                        }
                    }
                }
                /** Counting down */
                else {
                    when (direction) {
                    /** Low digit is down, high is up */
                        AnimationDirection.ALWAYS_UP, AnimationDirection.MORE_DOWN-> {
                            upDigit = highDigit.toString()
                            downDigit = lowDigit.toString()
                            showHigh
                        }
                    /** Low digit is up, high is down */
                        AnimationDirection.ALWAYS_DOWN, AnimationDirection.MORE_UP -> {
                            upDigit = lowDigit.toString()
                            downDigit = highDigit.toString()
                            1000 - showHigh
                        }
                    }
                }
        getDigitCoordinates(upDigit, position, DigitType.DIGIT_UP, offset, upCoordinates)
        canvas.drawText(upDigit, upCoordinates.x, upCoordinates.y, paint)
        getDigitCoordinates(downDigit, position, DigitType.DIGIT_DOWN, offset, downCoordinates)
        canvas.drawText(downDigit, downCoordinates.x, downCoordinates.y, paint)
    }

    private fun drawNumber(canvas: Canvas, number: Int) {
        for (position in 1..digitNumber) {
            val currentDigit = number.div(10.toDouble().pow(position-1)).rem(10).toInt().toString()
            getDigitCoordinates(currentDigit, position, DigitType.DIGIT_DOWN, 0, downCoordinates)
            canvas.drawText(currentDigit, downCoordinates.x, downCoordinates.y, paint)
        }
    }

    private fun initializePaint() {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        textPaint.color = color
        textPaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize.toFloat(), resources.displayMetrics)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.isFakeBoldText = bold
        paint = textPaint
    }

    private fun initializeMaxSize() {
        if (!::paint.isInitialized) {
            throw RuntimeException("paint is not initialized")
        }
        val (maxWidth, maxHeight) = getFigureMaxDimension()
        maxCharacterWidth = maxWidth
        maxCharacterHeight = maxHeight
        marginSize = (maxCharacterWidth * 0.1).toInt()
    }

    private fun initializeCanvasSize() {
        if (maxCharacterHeight == -1 || maxCharacterHeight == -1) {
            throw RuntimeException("Figure max dimension not initialized")
        }
        canvasHeight = maxCharacterHeight
        canvasWidth = digitNumber * maxCharacterWidth + (digitNumber - 1) * marginSize
    }

    private fun getFigureMaxDimension(): Pair<Int, Int> {
        return (0..9).map {
            paint.getTextBounds(it.toString(), 0, 1, r)
            Pair(r.width(), r.height())
        }.reduce { acc, newPair ->
            Pair(Math.max(acc.first, newPair.first), Math.max(acc.second, newPair.second))
        }
    }

    private fun getDigitCoordinates(digit: String, position: Int, type: DigitType, offset: Int, coordinates: Coordinates) {
        paint.getTextBounds(digit, 0, 1, r)
        getDigitX(r, position, coordinates)
        getDigitY(r, type, offset, coordinates)
    }

    private fun getDigitX(textBounds: Rect, position: Int, coordinates: Coordinates) {
        coordinates.x  = maxCharacterWidth / 2f - textBounds.width() / 2f - textBounds.left.toFloat() + (digitNumber - position)* (maxCharacterWidth + marginSize)
    }

    /**
     *
     *
     * @param textBounds
     * @param type
     * @param offset is the offset of the number, the bigger the downer
     */
    private fun getDigitY(textBounds: Rect, type: DigitType, offset: Int, coordinates: Coordinates) {
        var centerY = if (type == DigitType.DIGIT_UP) {
            - maxCharacterHeight / 2f + textBounds.height() / 2f - textBounds.bottom.toFloat()
        } else {
            maxCharacterHeight / 2f + textBounds.height() / 2f - textBounds.bottom.toFloat()
        }
        centerY += (offset * maxCharacterHeight) / 1000
        coordinates.y = centerY
    }

    private data class Coordinates(var x: Float = -1F, var y: Float = -1F)

    interface OnCountStopListener {
        fun onCountStop(count: Int)
    }
}