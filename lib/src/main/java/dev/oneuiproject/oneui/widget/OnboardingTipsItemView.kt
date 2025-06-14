package dev.oneuiproject.oneui.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.withStyledAttributes
import androidx.core.view.isGone
import androidx.core.view.isVisible
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.dpToPxFactor
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue

/**
 * OneUI tips item view for on-boarding screens
 *
 * @param context The Context the view is running in, through which it can access the
 * current theme, resources, etc.
 * @param attrs (Optional) The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr (Optional) An attribute in the current theme that contains a
 * reference to a style resource that supplies default values for the view.
 * @param defStyleRes (Optional) A resource identifier of a style resource that
 * supplies default values for the view, used only if defStyleAttr is not provided
 * or cannot be found in the theme.
 */
class OnboardingTipsItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var itemIcon: ImageView
    private var titleTextView: TextView
    private var summaryTextView: TextView

    var title: CharSequence
        get() = titleTextView.text
        set(value) {
            if (titleTextView.text == value) return
            titleTextView.text = value
        }

    fun setTitleColor(@ColorInt color: Int) = titleTextView.setTextColor(color)

    var summary: CharSequence?
        get() = summaryTextView.text
        set(value) {
            if (summaryTextView.text == value) return
            summaryTextView.isGone = value.isNullOrEmpty()
            summaryTextView.text = value
        }

    fun setSummaryColor(@ColorInt color: Int) = summaryTextView.setTextColor(color)

    var icon: Drawable?
        get() = itemIcon.drawable
        set(icon) {
            if (itemIcon.drawable == icon) return
            itemIcon.isVisible = icon != null
            itemIcon.setImageDrawable(icon)
            updatePadding()
        }

    init {
        context.getThemeAttributeValue(androidx.appcompat.R.attr.listChoiceBackgroundIndicator)?.let{
            setBackgroundResource(it.resourceId)
        } ?: Log.w(TAG, "Unable to resolve ?listChoiceBackgroundIndicator attribute!")

        orientation = HORIZONTAL
        inflate(context, R.layout.oui_des_widget_obs_tips_item, this).also {
            itemIcon = it.findViewById(R.id.tips_item_icon)
            titleTextView = it.findViewById(R.id.tips_item_title_text)
            summaryTextView = it.findViewById(R.id.tips_item_summary_text)
        }

        attrs?.let{
            context.withStyledAttributes(
                attrs,
                R.styleable.OnboardingTipsItemView,
                defStyleAttr,
                defStyleRes
            ) {
                getDrawable(R.styleable.OnboardingTipsItemView_icon)?.let { icon = it }
                title = getText(R.styleable.OnboardingTipsItemView_title)
                summary = getText(R.styleable.OnboardingTipsItemView_summary)
            }
        }
        updatePadding()
    }

    private fun updatePadding(){
        val dpToPxFactor = context.dpToPxFactor
        val paddingSize = if (icon != null) HORIZONTAL_PADDING_WITH_ICON else HORIZONTAL_PADDING_NO_ICON
        val horizontalPadding = (paddingSize * dpToPxFactor).toInt()
        val verticalPadding = (VERTICAL_PADDING * dpToPxFactor).toInt()
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
    }

    @JvmOverloads
    fun setIcon(@DrawableRes resId: Int, mutate: Boolean = false) {
        icon = AppCompatResources.getDrawable(context, resId)?.apply { if (mutate) mutate() }
    }

    companion object {
        private const val TAG = "OnboardingTipsItemView"
        private const val HORIZONTAL_PADDING_NO_ICON = 32.0f //dp
        private const val HORIZONTAL_PADDING_WITH_ICON = 24.0f //dp
        private const val VERTICAL_PADDING = 16.0f //dp
    }
}

