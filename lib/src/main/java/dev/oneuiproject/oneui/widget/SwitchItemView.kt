@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package dev.oneuiproject.oneui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Rect
import android.os.Build
import android.text.SpannableString
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewStub
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.ColorUtils
import androidx.core.view.isGone
import androidx.core.view.isVisible
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue
import dev.oneuiproject.oneui.utils.SemTouchFeedbackAnimator
import kotlinx.coroutines.Runnable

/**
 * A custom view that displays a title, summary, and a switch.
 * This view can be used to represent a setting or option that can be toggled on or off.
 *
 * The SwitchItemView supports the following custom attributes:
 * - `app:title`: The main text displayed in the view.
 * - `app:summaryOn`: The summary text displayed when the switch is checked.
 * - `app:summaryOff`: The summary text displayed when the switch is unchecked.
 * - `app:separateSwitch`: Whether to separate the click and check change events.
 *       If true, the switch can be clicked independently of the rest of the view.
 * - `app:showTopDivider`: Whether to display a divider line above the view.
 * - `app:showBottomDivider`: Whether to display a divider line below the view.
 * - `app:userUpdatableSummary`: Whether the summary text color should change based on the switch state.
 *
 * # Example usage:
 * ```xml
 * <dev.oneuiproject.oneui.widget.SwitchItemView
 *     android:id="@+id/my_switch_item"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:title="Enable Feature"
 *     app:summaryOn="Feature is enabled"
 *     app:summaryOff="Feature is disabled"
 *     android:checked="true" />;
 * ```

 * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr An attribute in the current theme that contains a reference to a style resource that supplies default values for the view. Can be 0 to not look for defaults.
 */
class SwitchItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var switchView: SwitchCompat
    private var titleView: TextView
    private var summaryView: TextView
    private var dividerViewTop: View? = null
    private var dividerViewBottom: View? = null
    private var verticalDivider: View
    private val mainContent: ConstraintLayout
    private val contentFrame: FrameLayout
    private var badgeFrame: LinearLayout? = null
    private var bottomSpacer: Space
    private var isLargeLayout = false

    @RequiresApi(29)
    private lateinit var semTouchFeedbackAnimator: SemTouchFeedbackAnimator

    /**
     * Function lambda that is called when the checked state of the switch changes.
     *
     * @param viewId The ID of the SwitchItemView whose switch state changed.
     * @param checked True if the switch is now checked, false otherwise.
     */
    var onCheckedChangedListener: ((viewId: Int, checked: Boolean) -> Unit)? = null

    /**
     *  Makes click and check change events separate.
     *  This allows you to register separate callbacks for click and check change events.
     *  `false` by default.
     */
    var separateSwitch: Boolean
        get() = verticalDivider.isVisible
        set(value) {
            if (verticalDivider.isVisible != value) {
                verticalDivider.isVisible = value
                switchView.isClickable = value
            }
        }


    /**
     * Whether to show a divider at the top of the item.
     *
     * Default value is `true`.
     */
    var showTopDivider: Boolean
        get() = dividerViewTop?.isVisible == true
        set(value) {
            if (value) ensureTopDivider()
            dividerViewTop?.isVisible = value
        }

    /**
     * Whether to show a divider at the bottom of the item.
     *
     * Default value is `false`.
     */
    var showBottomDivider: Boolean
        get() = dividerViewBottom?.isVisible == true
        set(value) {
            if (value) ensureBottomDivider()
            dividerViewBottom?.isVisible = value
        }

    private fun ensureTopDivider(){
        if (dividerViewTop == null){
            dividerViewTop = LayoutInflater.from(context)
                .inflate(R.layout.oui_des_widget_card_item_divider, this, false)
            addView(dividerViewTop, 0)
        }
    }

    private fun ensureBottomDivider(){
        if (dividerViewBottom == null){
            dividerViewBottom = LayoutInflater.from(context)
                .inflate(R.layout.oui_des_widget_card_item_divider, this, false)
            addView(dividerViewBottom, childCount)
        }
    }

    /**
     * The summary for the switch when it's checked.
     * If the value is null, the summary view will be hidden.
     */
    var summaryOn: CharSequence? = null
        set(value) {
            if (field != value) {
                field = value
                updateSubtitleVisibility()
            }
        }

    /**
     * The summary to be displayed when the switch is OFF.
     * Setting this value updates the summary text if the switch is OFF, otherwise it will be
     * updated when the switch state changes.
     * To set the same summary for both states, use [setSummary].
     */
    var summaryOff: CharSequence? = null
        set(value) {
            if (field != value) {
                field = value
                updateSubtitleVisibility()
            }
        }

    /**
     * Sets both the [summaryOn] and [summaryOff]
     */
    fun setSummary(summary: String?){
        summaryOn = summary
        summaryOff = summary
    }

    /**
     * The title of the switch item.
     */
    var title: CharSequence?
        get() = titleView.text?.toString()
        set(value) {
            if (titleView.text != value) {
                titleView.text = value
            }
        }

    /**
     * Sets the title of this item using a [SpannableString]
     *
     * @param value The [SpannableString] to set as title.
     */
    fun setTitle(value: SpannableString) {
        titleView.text = value
    }

    /** Returns whether the Switch is checked. */
    var isChecked: Boolean
        get() = switchView.isChecked
        set(checked) {
            if (switchView.isChecked == checked) return
            switchView.isChecked = checked
        }

    /** Shows a badge on the right of the title. */
    var showBadge: Boolean
        get() = badgeFrame?.isVisible == true
        set(value) {
            if (value) {
                if (badgeFrame == null){
                    badgeFrame = (findViewById<ViewStub>(R.id.viewstub_badge_frame).inflate() as LinearLayout)
                }
                badgeFrame!!.isVisible = true
            } else {
                badgeFrame?.isGone = true
            }
        }

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        LayoutInflater.from(context).inflate(R.layout.oui_des_widget_switch_item, this@SwitchItemView)

        titleView = findViewById(R.id.switch_card_title)
        summaryView = findViewById(R.id.switch_card_summary)

        mainContent = findViewById(R.id.main_content)
        contentFrame = findViewById(R.id.content_frame)
        verticalDivider = findViewById(R.id.vertical_divider)

        switchView = findViewById<SwitchCompat>(R.id.switch_widget).apply {
            isSaveEnabled = false
        }

        bottomSpacer = findViewById(R.id.bottom_spacer)

        isFocusable = true
        isClickable = true


        context.withStyledAttributes(
            attrs,
            R.styleable.SwitchItemView,
            defStyleAttr,
            defStyleRes
        ) {
            isEnabled = getBoolean(R.styleable.SwitchItemView_android_enabled, true)
            isChecked = getBoolean(R.styleable.SwitchItemView_android_checked, false)
            title = getText(R.styleable.SwitchItemView_title)
            summaryOn = getText(R.styleable.SwitchItemView_summaryOn)
            summaryOff = getText(R.styleable.SwitchItemView_summaryOff)
            separateSwitch = getBoolean(R.styleable.SwitchItemView_separateSwitch, false)
            showTopDivider = getBoolean(R.styleable.SwitchItemView_showTopDivider, true)
            showBottomDivider = getBoolean(R.styleable.SwitchItemView_showBottomDivider, false)
            if (getBoolean(R.styleable.SwitchItemView_userUpdatableSummary, false)){
                val colorEnabled = ContextCompat.getColor(context,
                    context.getThemeAttributeValue(androidx.appcompat.R.attr.colorPrimaryDark)!!.resourceId)
                val states = arrayOf(
                    intArrayOf(android.R.attr.state_enabled),
                    intArrayOf(-android.R.attr.state_enabled)
                )
                val colors = intArrayOf(
                    colorEnabled,
                    ColorUtils.setAlphaComponent(colorEnabled, (255 * 0.4).toInt())
                )
                summaryView.setTextColor(ColorStateList(states, colors))
            }
        }

        contentFrame.setOnClickListener {
            if (isEnabled) {
                if (!separateSwitch) {
                    switchView.performClick()
                }else{
                    super.callOnClick()
                }
            }
        }

        switchView.apply {
            setOnClickListener {v ->
                (v as SwitchCompat).isChecked.let { b ->
                    this.isChecked = b
                }
            }
            setOnCheckedChangeListener { _, isChecked ->
                updateSubtitleVisibility()
                onCheckedChangedListener?.invoke(this@SwitchItemView.id, isChecked)
            }
        }

        if (Build.VERSION.SDK_INT >= 29) {
            semTouchFeedbackAnimator = SemTouchFeedbackAnimator(contentFrame)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun performClick(): Boolean {
        return switchView.performClick()
    }

    private fun updateSubtitleVisibility(){
        val summaryToSet = if (isChecked) summaryOn else summaryOff
        summaryView.apply {
            if (text == summaryToSet) return
            text = summaryToSet
            isVisible = summaryToSet != null
        }
    }

    override fun setEnabled(enable: Boolean) {
        super.setEnabled(enable)
        contentFrame.isEnabled = enable
        mainContent.isEnabled = enable
        titleView.isEnabled = enable
        summaryView.isEnabled = enable
        switchView.isEnabled = enable
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateSwitchPosition()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        updateSwitchPosition()
    }

    private val responsiveSwitchUpdater = Runnable {
        val res = context.resources
        val configuration = res.configuration
        val swDp = configuration.screenWidthDp
        var isLargeLayout = !((swDp > 320 || configuration.fontScale < 1.1f)
                && (swDp >= 411 || configuration.fontScale < 1.3f))

        if (isLargeLayout) {
            val titleLen: Float = titleView.paint.measureText(titleView.getText().toString())

            val availableWidth =
                mainContent.width - mainContent.paddingStart - mainContent.paddingEnd -
                        (switchView.width + switchView.paddingStart + switchView.paddingEnd)

            if (titleLen < availableWidth) {
                val summaryLen: Float = if (summaryView.isVisible) {
                    summaryView.paint.measureText(summaryView.getText().toString())
                } else 0.0f
                if (summaryLen < availableWidth) isLargeLayout = false
            }
        }

        if (this.isLargeLayout != isLargeLayout) {

            val switchLP = switchView.layoutParams as ConstraintLayout.LayoutParams
            val titleLP = titleView.layoutParams as ConstraintLayout.LayoutParams
            val summaryLP = summaryView.layoutParams as ConstraintLayout.LayoutParams
            val bottomSpacerLP = bottomSpacer.layoutParams as ConstraintLayout.LayoutParams

            if (isLargeLayout) {
                switchLP.topToTop = ConstraintLayout.LayoutParams.UNSET
                switchLP.topToBottom = R.id.switch_card_summary
                switchLP.height = 22.dpToPx(res)
                res.getDimensionPixelSize(androidx.preference.R.dimen.sesl_preference_switch_padding_vertical)
                    .let {
                        switchLP.bottomMargin = it
                        switchLP.topMargin = it
                    }

                titleLP.endToStart = ConstraintLayout.LayoutParams.UNSET
                summaryLP.endToStart = ConstraintLayout.LayoutParams.UNSET
                summaryLP.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                bottomSpacerLP.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            } else {
                switchLP.topToBottom = ConstraintLayout.LayoutParams.UNSET
                switchLP.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                switchLP.height = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                switchLP.bottomMargin = 0
                switchLP.topMargin = 0

                titleLP.endToStart = R.id.switch_widget
                summaryLP.endToStart = R.id.switch_widget
                bottomSpacerLP.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            }
            switchView.layoutParams = switchLP
            titleView.layoutParams = titleLP
            summaryView.layoutParams = summaryLP
            bottomSpacer.layoutParams = bottomSpacerLP

            this.isLargeLayout = isLargeLayout

            post { requestLayout() }
        }
    }

    private fun updateSwitchPosition(){
        removeCallbacks(responsiveSwitchUpdater)
        postDelayed(responsiveSwitchUpdater, 100)
    }

    override fun dispatchTouchEvent(motionEvent: MotionEvent): Boolean {
        if (Build.VERSION.SDK_INT >= 29) {
            val switchBounds = Rect().apply { switchView.getHitRect(this) }
            val isTouchOnSwitch = switchBounds.contains(motionEvent.x.toInt(), motionEvent.y.toInt())
            if (!isTouchOnSwitch) {
                semTouchFeedbackAnimator.animate(motionEvent)
            }
        }
        return super.dispatchTouchEvent(motionEvent)
    }
}