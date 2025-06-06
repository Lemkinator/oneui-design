package dev.oneuiproject.oneuiexample.ui.main.core.util

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import dev.oneuiproject.oneui.widget.SemToast as SemToast

@JvmOverloads
inline fun Context.semToast(msg: String, length: Int = Toast.LENGTH_SHORT, onCreate: Toast.() -> Unit = {}): SemToast  =
    SemToast.makeText(this, msg, length).apply { onCreate(); show() }

fun Fragment.semToast(msg: String)  = requireContext().semToast(msg)