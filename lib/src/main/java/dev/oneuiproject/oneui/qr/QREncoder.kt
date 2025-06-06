package dev.oneuiproject.oneui.qr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.appcompat.content.res.AppCompatResources
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.ByteMatrix
import com.google.zxing.qrcode.encoder.Encoder
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.dpToPxFactor
import java.util.Hashtable
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap

/**
 * A utility class for generating QR codes with various customization options.
 *
 * This class allows you to:
 * - Set the content (data) to be encoded in the QR code.
 * - Customize the size of the QR code.
 * - Add an icon to the center of the QR code.
 * - Set the foreground and background colors.
 * - Apply a rounded frame around the QR code.
 * - Tint the anchor points and border of the QR code.
 *
 * Example usage:
 * ```kotlin
 * val qrEncoder = QREncoder(context, "https://example.com")
 *     .setSize(300)
 *     .setIcon(R.drawable.my_icon)
 *     .setBackgroundColor(Color.WHITE)
 *     .setForegroundColor(Color.BLUE, true, true)
 *     .roundedFrame(true)
 * val qrBitmap = qrEncoder.generate()
 *
 * myImageView.setImageBitmap(qrBitmap)
 * ```
 *
 * @property context The Android [Context] used for resource access and density calculations.
 * @property content The string content to be encoded in the QR code.
 * @see dev.oneuiproject.oneui.widget.QRImageView
 */
class QREncoder(private val context: Context, private val content: String) {

    private val dpToPx = context.dpToPxFactor

    private var qrSize = (200 * dpToPx).toInt()
    private var qrIcon: Drawable? = null
    private val qrIconSize = (48 * dpToPx).toInt()
    private var qrFrame = true

    private var qrFGColor = Color.BLACK
    private var qrBGColor = "#fcfcfc".toColorInt()
    private var qrTintAnchor = false
    private var qrTintBorder = false

    /**
     * Sets the size of the QR code.
     *
     * @param size The size in pixels.
     * @return The current [QREncoder] instance for chaining.
     */
    fun setSize(@Px size: Int) = apply { this.qrSize = size }

    /**
     * Sets the icon to be placed at the center of the QR code.
     *
     * @param id The drawable resource ID of the icon.
     * @return The current [QREncoder] instance for chaining.
     */
    fun setIcon(@DrawableRes id: Int) = apply { setIcon(getDrawable(id)) }

    /**
     * Sets the icon to be placed at the center of the QR code.
     *
     * @param icon The drawable icon.
     * @return The current [QREncoder] instance for chaining.
     */
    fun setIcon(icon: Drawable?) = apply { this.qrIcon = icon }

    /**
     * Sets the background color of the QR code.
     *
     * @param color The color int.
     * @return The current [QREncoder] instance for chaining.
     */
    fun setBackgroundColor(@ColorInt color: Int) = apply { this.qrBGColor = color }

    /**
     * Sets whether to add a rounded border around the QR code.
     *
     * @param apply True to add a rounded border, false otherwise.
     * @return The current [QREncoder] instance for chaining.
     */
    fun roundedFrame(apply: Boolean) = apply { this.qrFrame = apply }


    /**
     * Sets the foreground color of the QR code, with options to tint anchor and border.
     *
     * @param color The color int.
     * @param tintAnchor True to tint anchor points, false otherwise.
     * @param tintBorder True to tint the border, false otherwise.
     * @return The current [QREncoder] instance for chaining.
     */
    fun setForegroundColor(color: Int, tintAnchor: Boolean, tintBorder: Boolean) = apply {
        this.qrFGColor = color
        this.qrTintAnchor = tintAnchor
        this.qrTintBorder = tintBorder
    }

    fun generate(): Bitmap? {
        try {
            val hashtable = Hashtable<EncodeHintType, String>()
            hashtable[EncodeHintType.CHARACTER_SET] = "utf-8"
            val matrix = Encoder.encode(content, ErrorCorrectionLevel.H, hashtable).matrix
            val qrcode = createBitmap(qrSize, qrSize)
            qrcode.eraseColor(qrBGColor)

            drawQrImage(qrcode, matrix)
            drawAnchor(qrcode, matrix)
            if (qrIcon != null) drawIcon(qrcode)

            if (qrFrame) return addFrame(qrcode)
            return qrcode
        } catch (e: WriterException) {
            Log.e("QREncoder", "Exception in encoding QR code")
            e.printStackTrace()
            return null
        }
    }

    private fun drawQrImage(qrcode: Bitmap, byteMatrix: ByteMatrix) {
        val canvas = Canvas(qrcode)
        val paint = paint
        paint.color = qrFGColor
        val width = (qrcode.width * 1.0f) / byteMatrix.width
        val radius = 0.382f * width
        val offset = width / 2.0f
        for (i in 0 until byteMatrix.height) {
            for (i2 in 0 until byteMatrix.width) {
                if (byteMatrix[i2, i].toInt() == 1) {
                    canvas.drawCircle((i2 * width) + offset, (i * width) + offset, radius, paint)
                }
            }
        }
    }

    private fun drawAnchor(qrcode: Bitmap, byteMatrix: ByteMatrix) {
        val anchor = getBitmap(getDrawable(R.drawable.oui_des_qr_code_anchor)!!)
        val width = qrcode.width
        val height = qrcode.height

        val anchorWidth =
            (getAnchorWidth(byteMatrix) * (((width * 1.0f) / byteMatrix.width)))
        val paint = paint
        val canvas = Canvas(qrcode)
        canvas.drawRect(RectF(0.0f, 0.0f, anchorWidth, anchorWidth), paint)
        canvas.drawRect(
            RectF(
                (width - anchorWidth),
                0.0f,
                width.toFloat(),
                anchorWidth
            ), paint
        )
        canvas.drawRect(
            RectF(
                0.0f,
                (height - anchorWidth),
                anchorWidth,
                height.toFloat()
            ), paint
        )

        val anchorTint = Paint()
        anchorTint.isAntiAlias = true
        anchorTint.style = Paint.Style.FILL
        if (qrTintAnchor) {
            anchorTint.setColorFilter(PorterDuffColorFilter(qrFGColor, PorterDuff.Mode.SRC_IN))
        }

        val scaleBitmap = getScaleBitmap(anchor, (anchorWidth) / anchor.width)
        canvas.drawBitmap(scaleBitmap, 0.0f, 0.0f, anchorTint)
        canvas.drawBitmap(scaleBitmap, (width - anchorWidth), 0.0f, anchorTint)
        canvas.drawBitmap(scaleBitmap, 0.0f, (height - anchorWidth), anchorTint)
        scaleBitmap.recycle()
        anchor.recycle()
    }

    private fun getAnchorWidth(byteMatrix: ByteMatrix): Int {
        var i = 0
        while (i < byteMatrix.width && byteMatrix[i, 0].toInt() == 1) {
            i++
        }
        return i
    }

    private fun drawIcon(qrCode: Bitmap) {
        val height = qrIconSize
        val width = qrIconSize

        val iconTop = (qrCode.height / 2) - (height / 2)
        val iconLeft = (qrCode.width / 2) - (width / 2)
        val iconRadius = qrIconSize/2
        val iconPadding = (5f * dpToPx).toInt()
        val canvas = Canvas(qrCode)
        val paint = paint
        val rectF = RectF(
            ((iconLeft - iconPadding).toFloat()),
            ((iconTop - iconPadding).toFloat()),
            ((width + iconLeft + iconPadding).toFloat()),
            ((height + iconTop + iconPadding).toFloat())
        )
        canvas.drawRoundRect(rectF, iconRadius.toFloat(), iconRadius.toFloat(), paint)
        qrIcon!!.setBounds(iconLeft, iconTop, iconLeft + width, iconTop + height)
        qrIcon!!.draw(canvas)
    }

    private fun addFrame(qrcode: Bitmap): Bitmap {
        val border = (12 * dpToPx).toInt()
        val radius= (32 * dpToPx).toInt()

        val newWidth = qrcode.width + border * 2
        val newHeight = qrcode.height + border * 2
        val output = createBitmap(newWidth, newHeight)
        val canvas = Canvas(output)

        val paint = paint
        val rectF = RectF(0f, 0f, newWidth.toFloat(), newHeight.toFloat())
        canvas.drawRoundRect(rectF, radius.toFloat(), radius.toFloat(), paint)

        canvas.drawBitmap(qrcode, border.toFloat(), border.toFloat(), null)

        paint.color = if (qrTintBorder) qrFGColor else "#d0d0d0".toColorInt()
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        rectF[1.0f, 1.0f, (newWidth - 1).toFloat()] = (newHeight - 1).toFloat()
        canvas.drawRoundRect(rectF, radius.toFloat(), radius.toFloat(), paint)

        return output
    }


    private val paint: Paint
        get() {
            val paint = Paint()
            paint.isAntiAlias = true
            paint.style = Paint.Style.FILL
            paint.color = qrBGColor
            return paint
        }

    private fun getDrawable(@DrawableRes id: Int): Drawable? {
        return AppCompatResources.getDrawable(context, id)
    }

    private fun getBitmap(drawable: Drawable): Bitmap {
        val createBitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
        val canvas = Canvas(createBitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return createBitmap
    }

    private fun getScaleBitmap(bitmap: Bitmap, scale: Float): Bitmap {
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
