@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package dev.oneuiproject.oneui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewStub
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue
import dev.oneuiproject.oneui.utils.SemTouchFeedbackAnimator

/**
 * A custom view that displays a card item with a title, summary, icon, and dividers.
 * It is designed to be used as a row item in a list or similar container.
 *
 * Features:
 * - Displays a title and summary.
 * - Optionally displays an icon on the left.
 * - Optionally displays top and bottom dividers.
 * - Supports showing a badge on the right.
 *
 * ## Example usage:
 * ```xml
 * <dev.oneuiproject.oneui.widget.CardItemView
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:title="Card Title"
 *     app:summary="Card summary text."
 *     app:icon="@drawable/ic_some_icon"
 *     app:showTopDivider="true"
 *     app:showBottomDivider="true" />
 * ```
 *
 * @param context The Context the view is running in, through which it can access the
 * current theme, resources, etc.
 * @param attrs (Optional) The attributes of the XML tag that is inflating the view.
 */
class CardItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private var containerView: LinearLayout
    private var titleTextView: TextView
    private var summaryTextView: TextView
    private var dividerViewTop: View? = null
    private var dividerViewBottom: View? = null
    private var iconImageView: ImageView? = null
    private var badgeFrame: LinearLayout? = null

    private var containerLeftPaddingWithIcon: Int = 0
    private var containerLeftPaddingNoIcon: Int = 0
    private var dividerMarginStart: Int = 0
    private var dividerMarginStartWithIcon: Int = 0

    private var suspendLayoutUpdates = false

    @RequiresApi(29)
    private lateinit var semTouchFeedbackAnimator: SemTouchFeedbackAnimator

    /**
     *  Show divider on top. True by default
     */
    var showTopDivider: Boolean
        get() = dividerViewTop?.isVisible == true
        set(value) {
            if (value) ensureTopDivider()
            dividerViewTop?.isVisible = value
        }

    /**
     *  Show divider at the bottom. False by default
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
     * Show full divider width even when there's an icon.
     */
    var fullWidthDivider: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            updateLayoutParams()
        }

    /**
     * The title of the card item.
     */
    var title: CharSequence?
        get() = titleTextView.text
        set(value) {
            if (titleTextView.text == value) return
            titleTextView.text = value
        }

    /**
     * The summary for the item.
     * This will be shown below the title.
     */
    var summary: CharSequence?
        get() = summaryTextView.text
        set(value) {
            if (summaryTextView.text == value) return
            summaryTextView.isVisible = !value.isNullOrEmpty()
            summaryTextView.text = value
        }

    /** The icon to be displayed in the card item view. */
    var icon: Drawable?
        get() = iconImageView?.drawable
        set(value) {
            if (value != null) ensureInflatedIconView()
            if (iconImageView?.drawable == value) return
            iconImageView!!.setImageDrawable(value)
            updateLayoutParams()
        }

    /** Whether to show a badge on this card item. */
    var showBadge: Boolean
        get() = badgeFrame?.isVisible == true
        set(value) {
            if (value) {
                (badgeFrame
                    ?: (findViewById<ViewStub>(R.id.viewstub_badge_frame).inflate() as LinearLayout).also { badgeFrame = it }
                ).isVisible = true
            } else {
                badgeFrame?.isVisible = false
            }
        }

    init {
        orientation = VERTICAL
        val resources = context.resources
        context.getThemeAttributeValue(android.R.attr.listPreferredItemPaddingStart)!!.run {
            resources.getDimensionPixelSize(resourceId)
        }.let {
            containerLeftPaddingNoIcon = it
            containerLeftPaddingWithIcon = it - 4
        }

        resources.apply {
            dividerMarginStart = containerLeftPaddingNoIcon
            dividerMarginStartWithIcon = containerLeftPaddingWithIcon + getDimensionPixelSize(R.dimen.oui_des_cardview_icon_size) +
                    getDimensionPixelSize(R.dimen.oui_des_cardview_icon_margin_end)
        }

        inflate(context, R.layout.oui_des_widget_card_item, this)
        containerView = findViewById(R.id.cardview_container)
        titleTextView = findViewById<TextView?>(R.id.cardview_title)
        summaryTextView = findViewById<TextView>(R.id.cardview_summary)

        suspendLayoutUpdates = true
        attrs?.let { parseAttributes(it) }
        suspendLayoutUpdates = false
        updateLayoutParams()

        if (Build.VERSION.SDK_INT >= 29) {
            semTouchFeedbackAnimator = SemTouchFeedbackAnimator(containerView)
        }
    }


    private fun parseAttributes(attrs: AttributeSet) {
        context.withStyledAttributes(attrs, R.styleable.CardItemView) {
            title = getString(R.styleable.CardItemView_title)
            titleTextView.maxLines = getInteger(R.styleable.CardItemView_titleMaxLines, 5)

            val iconDrawable = getDrawable(R.styleable.CardItemView_icon)
            if (iconDrawable != null) {
                icon = iconDrawable
                val iconTint = getColor(R.styleable.CardItemView_iconTint, -1)
                if (iconTint != -1) {
                    DrawableCompat.setTint(iconImageView!!.drawable, iconTint)
                }
            }

            summary = getString(R.styleable.CardItemView_summary)
            if (getBoolean(R.styleable.CardItemView_userUpdatableSummary, false)){
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
                summaryTextView.setTextColor(ColorStateList(states, colors))
            }
            summaryTextView.maxLines = getInteger(R.styleable.CardItemView_summaryMaxLines, 10)
            showTopDivider = getBoolean(R.styleable.CardItemView_showTopDivider, true)
            showBottomDivider = getBoolean(R.styleable.CardItemView_showBottomDivider, false)
            isEnabled = getBoolean(R.styleable.CardItemView_android_enabled, true)
            fullWidthDivider = getBoolean(R.styleable.CardItemView_fullWidthDivider, false)
        }
    }

    private fun ensureInflatedIconView(){
        if (iconImageView == null) {
            iconImageView = (findViewById<ViewStub>(R.id.viewstub_icon_frame).inflate() as FrameLayout)
                .findViewById(R.id.cardview_icon)
        }
    }

    private fun updateLayoutParams(){
        if (suspendLayoutUpdates) return

        val hasIcon = iconImageView?.isVisible == true && iconImageView?.drawable != null
        val desiredPaddingStart = if (hasIcon) containerLeftPaddingWithIcon else containerLeftPaddingNoIcon

        containerView.apply {
            if (desiredPaddingStart == paddingLeft) return@apply
            updatePaddingRelative(start = desiredPaddingStart)
        }

        val desiredDividerStartMargin = if (!hasIcon || fullWidthDivider) dividerMarginStart else dividerMarginStartWithIcon

        dividerViewTop?.apply {
            if (desiredDividerStartMargin == marginStart) return@apply
            updateLayoutParams<LayoutParams> { marginStart = desiredDividerStartMargin }
        }

        dividerViewBottom?.apply {
            if (desiredDividerStartMargin == marginStart) return@apply
            updateLayoutParams<LayoutParams> { marginStart = desiredDividerStartMargin }
        }
    }

    /**
     * Returns the icon ImageView.
     * This method inflates the icon view if not yet inflated before returning it.
     *
     * @return The ImageView used to display the icon, or null if no icon is set.
     */
    fun getIconImageView(): ImageView {
        ensureInflatedIconView()
        return iconImageView!!
    }

    override fun setEnabled(enabled: Boolean) {
        if (isEnabled == enabled) return
        super.setEnabled(enabled)
        containerView.apply {
            isFocusable = enabled
            isClickable = enabled
            alpha = when {
                enabled -> 1.0f
                else -> 0.4f
            }
        }
    }

    /**
     * Register a callback to be invoked when this view is clicked. If this view is not
     * clickable, it becomes clickable.
     *
     * @param l The [View.OnClickListener] that will be invoked
     */
    override fun setOnClickListener(l: OnClickListener?) {
        containerView.setOnClickListener {
            if (isEnabled) {
                l?.onClick(this)
            }
        }
    }

    override fun dispatchTouchEvent(motionEvent: MotionEvent): Boolean {
        if (Build.VERSION.SDK_INT >= 29) {
            semTouchFeedbackAnimator.animate(motionEvent)
        }
        return super.dispatchTouchEvent(motionEvent)
    }
}