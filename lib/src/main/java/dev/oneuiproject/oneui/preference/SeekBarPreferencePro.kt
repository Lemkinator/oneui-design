@file:Suppress("MemberVisibilityCanBePrivate")

package dev.oneuiproject.oneui.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.View.OnTouchListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SeslAbsSeekBar.NO_OVERLAP
import androidx.appcompat.widget.SeslProgressBar.MODE_LEVEL_BAR
import androidx.appcompat.widget.SeslSeekBar
import androidx.core.content.res.TypedArrayUtils
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
import androidx.reflect.view.SeslHapticFeedbackConstantsReflector
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.SeslSeekBarDualColors
import dev.oneuiproject.oneui.ktx.setShowTickMarks
import dev.oneuiproject.oneui.ktx.updateDualColorRange
import dev.oneuiproject.oneui.widget.SeekBarPlus
import java.lang.ref.WeakReference


/**
 * Preference based [SeekBarPreference] but with more options and flexibility
 *
 * If [isAdjustable] is set to true, shows + and - buttons at both ends of the seekbar
 *
 * @see [centerBasedSeekBar]
 * @see [rightLabel]
 * @see [leftLabel]
 * @see [getSeekBarIncrement]
 * @see [isAdjustable]
 *
 */

class SeekBarPreferencePro @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    @SuppressLint("RestrictedApi")
    defStyleAttr: Int = TypedArrayUtils.getAttr(
        context, R.attr.seekBarPreferenceProStyle,
        androidx.preference.R.attr.seekBarPreferenceStyle
    ),
    defStyleRes: Int = 0
) : SeekBarPreference(context, attrs, defStyleAttr, defStyleRes),
    View.OnClickListener,
    OnLongClickListener,
    OnTouchListener {

    private val longPressHandler: Handler = LongPressHandler(this)

    private var seekbarMode: Int = MODE_LEVEL_BAR
    private var isSeamLess: Boolean = false
    private var seekBarPlus: SeekBarPlus? = null

    /**
     * If true, this sets [SeslSeekBar] to only put tick marks at the start, middle and end
     * regardless of the min and max value.
     * This will ignore [SeslSeekBar.setMode] value and will use [MODE_LEVEL_BAR][SeslSeekBar.MODE_LEVEL_BAR] mode instead.
     */
    var centerBasedSeekBar: Boolean = false
        set(centerBased){
            if (field != centerBased) {
                field = centerBased
                notifyChanged()
            }
        }

    var leftLabel: CharSequence? = null
        set(charSequence){
            if (field != charSequence) {
                field = charSequence
                notifyChanged()
            }
        }
    var rightLabel: CharSequence? = null
        set(charSequence){
            if (field != charSequence) {
                field = charSequence
                notifyChanged()
            }
        }
    var stateDescription: CharSequence? = null
        set(charSequence){
            if (field != charSequence) {
                field = charSequence
                notifyChanged()
            }
        }

    /**
     * This does not apply when [centerBasedSeekBar] is true
     */
    var showTickMarks: Boolean = false
        set(show){
            if (field != show) {
                field = show
                notifyChanged()
            }
        }


    var progressTintList: ColorStateList? = null
        set(progressTintList){
            if (field != progressTintList) {
                field = progressTintList
                notifyChanged()
            }
        }

    var tickMarkTintList: ColorStateList? = null
        set(tickMarkTintList){
            if (field != tickMarkTintList) {
                field = tickMarkTintList
                notifyChanged()
            }
        }


    private var overlapPoint: Int = NO_OVERLAP
    private var isLongKeyProcessing = false
    private var seekBarValueTextView: TextView? = null
    private var units: String? = null
    private var addButton: ImageView? = null
    private var deleteButton: ImageView? = null
    private var onSeekBarPreferenceChangeListener: OnSeekBarPreferenceChangeListener? = null

    init {
        context.withStyledAttributes(attrs, R.styleable.SeekBarPreferencePro) {
            centerBasedSeekBar = getBoolean(R.styleable.SeekBarPreferencePro_centerBasedSeekBar, false)
            leftLabel = getString(R.styleable.SeekBarPreferencePro_leftLabelName)
            overlapPoint = getInt(R.styleable.SeekBarPreferencePro_overlapPoint, NO_OVERLAP)
            rightLabel = getString(R.styleable.SeekBarPreferencePro_rightLabelName)
            isSeamLess = getBoolean(R.styleable.SeekBarPreferencePro_seamlessSeekBar, false)
            seekbarMode = getInt(R.styleable.SeekBarPreferencePro_seekBarMode, MODE_LEVEL_BAR)
            showTickMarks = getBoolean(R.styleable.SeekBarPreferencePro_showTickMark, true)
            units = getString(R.styleable.SeekBarPreferencePro_units)
        }
        setOnSeekBarPreferenceChangeListener(null)
    }


    override fun setOnSeekBarPreferenceChangeListener(onSeekBarPreferenceChangeListener: OnSeekBarPreferenceChangeListener?) {
        this@SeekBarPreferencePro.onSeekBarPreferenceChangeListener = object: OnSeekBarPreferenceChangeListener{
            override fun onProgressChanged(
                seekBar: SeslSeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                onSeekBarPreferenceChangeListener?.onProgressChanged(seekBar, progress, fromUser)
                if (showSeekBarValue) updateValueLabel(progress)
            }

            override fun onStartTrackingTouch(seslSeekBar: SeslSeekBar?) {
                onSeekBarPreferenceChangeListener?.onStartTrackingTouch(seslSeekBar)
            }

            override fun onStopTrackingTouch(seslSeekBar: SeslSeekBar?) {
                onSeekBarPreferenceChangeListener?.onStopTrackingTouch(seslSeekBar)
                if (showSeekBarValue) updateValueLabel(seslSeekBar?.progress?:0)
            }

        }
        super.setOnSeekBarPreferenceChangeListener(this@SeekBarPreferencePro.onSeekBarPreferenceChangeListener)
    }


    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        seekBarPlus = (holder.findViewById(R.id.seekbar) as? SeekBarPlus)?.apply sbp@{
            setSeamless(isSeamLess)
            if (!centerBasedSeekBar) {
                centerBasedBar = false
                setMode(seekbarMode)
                if (overlapPoint != NO_OVERLAP) {
                     updateDualColorRange(overlapPoint - this@SeekBarPreferencePro.min,
                         SeslSeekBarDualColors.Custom())
                }
                setShowTickMarks(showTickMarks)
            }
        }
        seekBarValueTextView = (holder.findViewById(R.id.seekbar_value) as? TextView)
        if (showSeekBarValue) {
            seekBarValueTextView!!.isVisible = true
            updateValueLabel(seekBarPlus!!.progress)
        }

        if (leftLabel != null || rightLabel != null) {
            with(holder) {
                (findViewById(R.id.seekbar_label_area) as? LinearLayout)?.isVisible = true
                (findViewById(R.id.left_label) as? TextView)?.text = leftLabel
                (findViewById(R.id.right_label) as? TextView)?.text = rightLabel
            }
        }

        addButton = (holder.findViewById(R.id.add_button) as? ImageView)?.apply add@{
            if (this@SeekBarPreferencePro.isAdjustable) {
                visibility = View.VISIBLE
                isEnabled = this@SeekBarPreferencePro.isEnabled
                alpha = if (isEnabled) 1.0f else 0.4f
                setOnClickListener(this@SeekBarPreferencePro)
                setOnLongClickListener(this@SeekBarPreferencePro)
                setOnTouchListener(this@SeekBarPreferencePro)

            }else{
                visibility = View.GONE
            }
        }

        deleteButton = (holder.findViewById(R.id.delete_button) as? ImageView)?.apply del@{
            if (this@SeekBarPreferencePro.isAdjustable) {
                visibility = View.VISIBLE
                setOnClickListener(this@SeekBarPreferencePro)
                setOnLongClickListener(this@SeekBarPreferencePro)
                setOnTouchListener(this@SeekBarPreferencePro)
                isEnabled = this@SeekBarPreferencePro.isEnabled
                alpha = if (isEnabled) 1.0f else 0.4f
            }else{
                this.visibility = View.GONE
            }
        }
    }


    private fun updateValueLabel(progress: Int) {
        if (seekBarValueTextView != null) {
            val value = progress + min
            val valueStr = "$value${units?:""}"
            seekBarValueTextView!!.text = valueStr
        }
    }

    override fun onClick(view: View) {
        when(view.id){
            R.id.delete_button -> onDeleteButtonClicked()
            R.id.add_button -> onAddButtonClicked()
        }
        view.announceForAccessibility(stateDescription)
    }

    override fun onLongClick(view: View): Boolean {
        isLongKeyProcessing = true
        val id = view.id
        if (id == R.id.delete_button || id == R.id.add_button) {
            Thread {
                while (isLongKeyProcessing) {
                    longPressHandler.sendEmptyMessage(if (view.id == R.id.delete_button) MSG_DELETE else MSG_ADD)
                    try {
                        Thread.sleep(300L)
                    } catch (e: InterruptedException) {
                        Log.w(SeekBarPreferencePro::class.java.simpleName, "InterruptedException!", e)
                    }
                }
            }.start()
            return false
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            isLongKeyProcessing = false
            longPressHandler.removeMessages(MSG_DELETE)
            longPressHandler.removeMessages(MSG_ADD)
        }
        return false
    }

    private fun onDeleteButtonClicked() {
        val newValue = (value - seekBarIncrement).coerceAtLeast(min)
        if (!callChangeListener(newValue)){
            return
        }
        value = newValue
        seekBar?.performHapticFeedback(HAPTIC_CONSTANT_CURSOR_MOVE)
    }

    private fun onAddButtonClicked() {
        val newValue = (value + seekBarIncrement).coerceAtMost(max)
        if (!callChangeListener(newValue)) {
            return
        }
        value = newValue
        seekBar?.performHapticFeedback(HAPTIC_CONSTANT_CURSOR_MOVE)
    }


    private class LongPressHandler(seekBarPref: SeekBarPreferencePro) :
        Handler(Looper.getMainLooper()) {
        private val weakReference: WeakReference<SeekBarPreferencePro> = WeakReference(seekBarPref)

        override fun handleMessage(message: Message) {
            val seekBarPreferencePro = weakReference.get()
            when(message.what) {
                MSG_DELETE -> seekBarPreferencePro!!.onDeleteButtonClicked()
                MSG_ADD -> seekBarPreferencePro!!.onAddButtonClicked()
            }
        }
    }


    private companion object{
        private const val TAG = "SeekBarPreferencePro"

        private val HAPTIC_CONSTANT_CURSOR_MOVE by lazy {
            @SuppressLint("RestrictedApi")
            SeslHapticFeedbackConstantsReflector.semGetVibrationIndex(41)
        }

        private const val MSG_DELETE = 1
        private const val MSG_ADD = 2
    }
}
