package dev.oneuiproject.oneui.navigation.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.appcompat.view.menu.MenuView
import androidx.core.content.withStyledAttributes
import androidx.core.view.doOnLayout
import androidx.core.view.forEach
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.google.android.material.internal.NavigationMenu
import com.google.android.material.navigation.NavigationView
import dev.oneuiproject.oneui.layout.DrawerLayout
import dev.oneuiproject.oneui.layout.DrawerLayout.DrawerState
import dev.oneuiproject.oneui.layout.NavDrawerLayout
import dev.oneuiproject.oneui.layout.internal.util.ToolbarLayoutUtils.getLayoutLocationInfo
import dev.oneuiproject.oneui.navigation.menu.DrawerMenuPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.google.android.material.R as materialR

@SuppressLint("RestrictedApi")
class DrawerNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int =  0
) : FrameLayout(context, attrs, defStyleAttr), MenuView {

    private var navigationItemSelectedListener: NavigationView.OnNavigationItemSelectedListener? = null
    private var menuPresenter: DrawerMenuPresenter
    private var navDrawerMenu: NavigationMenu
    @MenuRes
    private var navMenuRes: Int = 0
    private var lastTimeClicked = 0L

    private val drawerLayout by lazy(LazyThreadSafetyMode.NONE) {
        getLayoutLocationInfo().tblParent as DrawerLayout
    }

    init {
        context.withStyledAttributes(attrs, materialR.styleable.NavigationView, 0, 0) {
            navMenuRes = getResourceId(materialR.styleable.NavigationView_menu, 0)
        }

        navDrawerMenu = NavigationMenu(context).apply {
            isGroupDividerEnabled = true
        }

        menuPresenter = DrawerMenuPresenter { (drawerLayout as? NavDrawerLayout)?.getNavRailSlideRange() ?: 1 }


        if (navMenuRes != 0) {
            inflateMenu(navMenuRes)
        }

        navDrawerMenu.setCallback(
            object : MenuBuilder.Callback {
                override fun onMenuItemSelected(menu: MenuBuilder, item: MenuItem): Boolean {
                    return navigationItemSelectedListener?.onNavigationItemSelected(item) == true
                }

                override fun onMenuModeChange(menu: MenuBuilder) {}
            })

        navDrawerMenu.addMenuPresenter(menuPresenter)
        addView(menuPresenter.getMenuView(this))
    }

    /**
     * Update lock state of the navigation view items.
     */
    fun updateLock(isLock: Boolean) = menuPresenter.adapter!!.updateLock(isLock)

    private val drawerStateListener by lazy(LazyThreadSafetyMode.NONE) {
        DrawerLayout.DrawerStateListener {
            when (it){
                DrawerState.OPEN -> {
                    offsetUpdaterJob?.cancel()
                    menuPresenter.adapter!!.updateOffset(1f)
                }
                DrawerState.CLOSE-> {
                    offsetUpdaterJob?.cancel()
                    menuPresenter.adapter!!.updateOffset(0f)
                }

                DrawerState.CLOSING,
                DrawerState.OPENING -> {
                    startOffsetUpdater()
                }
            }
        }
    }

    private var offsetUpdaterJob: Job? = null
    private fun startOffsetUpdater(){
        if (offsetUpdaterJob?.isActive == true) return
        offsetUpdaterJob = CoroutineScope(Dispatchers.Main).launch {
            while(isActive) {
                menuPresenter.adapter!!.updateOffset(drawerLayout.drawerOffset)
                delay(20)
            }
        }
    }

    private fun getInitialOffset() =
        (drawerLayout as? NavDrawerLayout)?.isLargeScreenMode?.let {
            if (it) drawerLayout.drawerOffset else 1f
        } ?: 1f


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        doOnLayout {
            (drawerLayout as? NavDrawerLayout)?.apply {
                if (isLargeScreenMode) setDrawerStateListener(drawerStateListener)
            }
            menuPresenter.adapter!!.updateOffset(getInitialOffset())
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        (drawerLayout as? NavDrawerLayout)?.setDrawerStateListener(null)
    }

    override fun initialize(menu: MenuBuilder) = Unit

    override fun getWindowAnimations(): Int = 0

    private fun inflateMenu(@MenuRes resId: Int) {
        menuPresenter.setUpdateSuspended(true)
        SupportMenuInflater(context).inflate(resId, navDrawerMenu)
        menuPresenter.setUpdateSuspended(false)
        menuPresenter.updateMenuView(false)

        if(isAttachedToWindow){
            doOnLayout { menuPresenter.adapter!!.updateOffset(getInitialOffset()) }
        }
    }

    /**
     * Set a listener that will be notified when a menu item is selected.
     *
     * @param listener The listener to notify
     */
    fun setNavigationItemSelectedListener(
        listener: NavigationView.OnNavigationItemSelectedListener?) {
        this.navigationItemSelectedListener = listener
    }

    fun getDrawerMenu() = navDrawerMenu

    fun updateSelectedItem(destination: NavDestination){
        navDrawerMenu.forEach { item ->
            @Suppress("RestrictedApi")
            (item as MenuItemImpl).isChecked = destination.matchDestination(item.itemId)
        }
    }

    private fun NavDestination.matchDestination(@IdRes destId: Int): Boolean =
        hierarchy.any { it.id == destId }

    companion object{
        private const val TAG = "DrawerNavigationView"
    }
}
