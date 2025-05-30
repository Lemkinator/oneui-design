package dev.oneuiproject.oneui.layout.internal.backapi

import android.os.Build
import android.view.View
import android.window.BackEvent
import android.window.OnBackAnimationCallback
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT
import android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class OnBackCallbackDelegateCompat(activity: ComponentActivity,
                                   private val view: View,
                                   private val backHandler: BackHandler
){

    private interface BackCallbackDelegate {
        fun startListening(view: View, backHandler: BackHandler, priorityOverlay: Boolean)
        fun stopListening(view: View)
    }

    private val onBackCallbackDelegate by lazy(LazyThreadSafetyMode.NONE) {
        createBackCallbackDelegate()
    }

    private val onBackPressedDelegate by lazy(LazyThreadSafetyMode.NONE) {
        OnBackPressedCallbackDelegate(activity, backHandler)
    }

    fun startListening(priorityOverlay: Boolean = false) {
        onBackCallbackDelegate?.startListening(view, backHandler, priorityOverlay)
        onBackPressedDelegate.isEnabled = true
    }

    fun stopListening() {
        onBackCallbackDelegate?.stopListening(view)
        onBackPressedDelegate.isEnabled = false
    }

    @RequiresApi(34)
    private open class Api34BackCallbackDelegate : Api33BackCallbackDelegate() {
        override fun createOnBackInvokedCallback(backHandler: BackHandler) : OnBackInvokedCallback {
            return object : OnBackAnimationCallback {
                override fun onBackStarted(backEvent: BackEvent) {
                    if (!isListening) return
                    backHandler.startBackProgress(BackEventCompat(backEvent))
                }

                override fun onBackProgressed(backEvent: BackEvent){
                    if (!isListening) return
                    backHandler.updateBackProgress(BackEventCompat(backEvent))
                }

                override fun onBackInvoked()  = backHandler.handleBackInvoked()

                override fun onBackCancelled() {
                    if (!isListening) return
                    backHandler.cancelBackProgress()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private open class Api33BackCallbackDelegate : BackCallbackDelegate {
        private var obiCallback: OnBackInvokedCallback? = null

        protected val isListening: Boolean get() = obiCallback != null
        private var isOverlayPriority: Boolean = false

        override fun startListening(view: View, backHandler: BackHandler, priorityOverlay: Boolean) {
            if (isListening && isOverlayPriority == priorityOverlay) return
            stopListening(view)

            val onBackInvokedDispatcher = view.findOnBackInvokedDispatcher() ?: return
            isOverlayPriority = priorityOverlay
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                if (priorityOverlay) PRIORITY_OVERLAY else PRIORITY_DEFAULT,
                createOnBackInvokedCallback(backHandler).also {
                    obiCallback = it
                }
            )
        }

        override fun stopListening(view: View) {
            obiCallback?.let {
                view.findOnBackInvokedDispatcher()?.unregisterOnBackInvokedCallback(it)
                obiCallback = null
            }
        }

        open fun createOnBackInvokedCallback(backHandler: BackHandler) =
            OnBackInvokedCallback { backHandler.handleBackInvoked()}

    }

    private class OnBackPressedCallbackDelegate(private val activity: ComponentActivity,
                                                private val backHandler: BackHandler) {
        private var obpCallback:  OnBackPressedCallback? = null
        private val obpDispatcher = activity.onBackPressedDispatcher

        private val isListening: Boolean get() = obpCallback?.isEnabled == true

        var isEnabled: Boolean
            get() = obpCallback?.isEnabled == true
            set(value) {
                if (value) {
                    if (obpCallback == null) {
                        obpCallback = obpDispatcher.addCallback(activity, true) {
                            backHandler.handleBackInvoked()
                        }
                        return
                    }
                    obpCallback!!.isEnabled = true
                } else {
                    obpCallback?.isEnabled = false
                }
            }
    }

    companion object {
        private fun createBackCallbackDelegate(): BackCallbackDelegate? {
            return if (Build.VERSION.SDK_INT >= 34) {
                Api34BackCallbackDelegate()
            } else if (Build.VERSION.SDK_INT == 33) {
                Api33BackCallbackDelegate()
            } else {
                null
            }
        }
    }
}