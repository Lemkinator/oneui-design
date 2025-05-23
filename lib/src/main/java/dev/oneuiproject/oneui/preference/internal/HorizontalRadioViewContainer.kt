package dev.oneuiproject.oneui.preference.internal

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.RestrictTo
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.children
import androidx.core.view.isVisible
import dev.oneuiproject.oneui.design.R
import kotlin.math.roundToInt
import androidx.core.graphics.withTranslation

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class HorizontalRadioViewContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private var isDividerEnabled: Boolean? = null

    private val resources = context.resources
    private val divider = AppCompatResources.getDrawable(context, R.drawable.oui_des_preference_horizontal_radio_divider_vertical)
    private val marginTop = resources.getDimension(R.dimen.oui_des_horizontalradiopref_divider_margin_top)
    private val dividerBottom = resources.getDimension(R.dimen.oui_des_horizontalradiopref_divider_margin_bottom).roundToInt()
    @SuppressLint("PrivateResource")
    private val dividerWidth = resources.getDimension(androidx.appcompat.R.dimen.sesl_list_divider_height).roundToInt()

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (isDividerEnabled!!) {
            val visibleChildCount = children.filter { it.isVisible }.count()
            if (visibleChildCount == 0) return

            val width = width
            for (i in 0 until visibleChildCount - 1) {
                divider!!.setBounds(0, 0, dividerWidth, dividerBottom)
                canvas.withTranslation(width / visibleChildCount * (i + 1f), marginTop) {
                    divider.draw(this)
                }
            }
        }
    }

    fun setDividerEnabled(enabled: Boolean) {
        isDividerEnabled = enabled
    }
}