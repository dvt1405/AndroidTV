package com.kt.apps.core.base.leanback

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.BaseInputConnection
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.SupportMenuInflater
import androidx.core.view.forEachIndexed
import com.google.android.material.internal.NavigationMenu
import com.kt.apps.core.R
import com.kt.apps.core.logging.Logger
import com.kt.skeleton.dpToPx
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("RestrictedApi")
class NavDrawerView @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attr, defStyle) {
    @FunctionalInterface
    interface AnimateFractionChange {
        fun onProgress(progress: Float)
    }

    private var selectedItemId: Int = 0
    private var menu = NavigationMenu(context)
    private var _onAnimatedFraction: AnimateFractionChange? = null
    private val _enablePositionMap by lazy {
        mutableMapOf<Int, Int>()
    }
    var onAnimatedFraction: AnimateFractionChange?
        get() = _onAnimatedFraction
        set(value) {
            _onAnimatedFraction = value
        }

    private val openNavigator by lazy {
        ValueAnimator.ofInt(0.dpToPx(context), 150.dpToPx(context)).apply {
            this.addUpdateListener {
                _onAnimatedFraction?.onProgress(it.animatedFraction)
                for (i in 0 until childCount) {
                    val headerTitle = getChildAt(i).findViewById<TextView>(R.id.row_header)
                    val layoutParams = headerTitle?.layoutParams
                    layoutParams?.width = it.animatedValue as Int
                    headerTitle?.layoutParams = layoutParams
                }
            }
            this.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    var childSelected: View? = null
                    forEachIndexed { _, childView ->
                        childView.isPressed = false
                        childView.isSelected = childView.tag == selectedItemId
                        if (childView.isSelected) {
                            childSelected = childView
                        }
                    }
                    _isOpen = true
                    _isAnimating.set(false)
                    if (this@NavDrawerView.focusedChild == null) {
                        childSelected?.requestFocus()
                    }
                }
            })
        }
    }

    private val closeAnimator by lazy {
        ValueAnimator.ofInt(150.dpToPx(context), 0.dpToPx(context)).apply {
            this.addUpdateListener {
                _onAnimatedFraction?.onProgress(1 - it.animatedFraction)
                for (i in 0 until childCount) {
                    val headerTitle = getChildAt(i).findViewById<TextView>(R.id.row_header)
                    val layoutParams = headerTitle?.layoutParams
                    layoutParams?.width = it.animatedValue as Int
                    headerTitle?.layoutParams = layoutParams
                }
            }
            this.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                    onAnimationEnd(animation)
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    forEachIndexed { index, childView ->
                        childView.isSelected = false
                        childView.isPressed = childView.tag == selectedItemId
                        val headerTitle = childView.findViewById<TextView>(R.id.row_header)
                        headerTitle?.visibility = INVISIBLE
                    }
                    _isOpen = false
                    _isAnimating.set(false)
                }
            })
        }
    }

    init {
        val typedArray = context.obtainStyledAttributes(attr, R.styleable.NavDrawerView)
        val childSpacingBetween = typedArray.getDimensionPixelSize(
            R.styleable.NavDrawerView_childSpacingBetween,
            com.kt.apps.resources.R.dimen.nav_spacing_between
        )
        val defaultSelectedItemPosition = typedArray.getInteger(
            R.styleable.NavDrawerView_defaultSelectedPosition,
            0
        )
        if (typedArray.hasValue(R.styleable.NavDrawerView_navMenu)) {
            val menuResId = typedArray.getResourceId(
                R.styleable.NavDrawerView_navMenu,
                R.menu.navigation_menu
            )
            SupportMenuInflater(context).inflate(menuResId, menu)
            /**
             * In menu item list: Declare which item is enable or not.
             * Menu item is not enable while not focus by default, parent will focus first enable item
             * when init view/
             * */
            var firstInit = true
            var enableItemPosition = 0
            var firstEnableItem = -1
            for (i in 0 until menu.size()) {
                val menuItem = menu.getItem(i)
                val isItemEnabled = menuItem.isEnabled
                val childItemView = LayoutInflater.from(context)
                    .inflate(R.layout.header_dashboard, null)
                val marginLayoutParams = MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                if (isItemEnabled) {
                    if (firstInit && defaultSelectedItemPosition == i) {
                        selectedItemId = menuItem.itemId
                        firstEnableItem = i
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            childItemView.isFocusedByDefault = true
                        } else {
                            childItemView.requestFocus()
                            preApi26.setDefaultFocus(this, childItemView)
                        }
                        firstInit = false
                    }
                    _enablePositionMap[i] = enableItemPosition
                    enableItemPosition++
                }

                val icon = childItemView.findViewById<ImageView>(R.id.icon)
                val title = childItemView.findViewById<TextView>(R.id.row_header)
                marginLayoutParams.topMargin = childSpacingBetween / 2
                marginLayoutParams.bottomMargin = childSpacingBetween / 2
                icon.setImageDrawable(menuItem.icon)
                childItemView.tag = menuItem.itemId
                childItemView.layoutParams = marginLayoutParams
                childItemView.isPressed = i == firstEnableItem
                childItemView.isSelected = false
                childItemView.setOnFocusChangeListener { v, hasFocus ->
                    Logger.d(this@NavDrawerView, message = "OnFocusChange: $i, $hasFocus")
                    if (!isOpen) {
                        childItemView.isPressed = childItemView.tag == selectedItemId
                        icon.isSelected = false
                        title.isSelected = false
                    }
                    if (hasFocus) {
                        onNavDrawerItemSelected?.onSelected(_enablePositionMap[i] ?: -1, menuItem.itemId)
                        if (!menuItem.isEnabled) {
                            setItemSelected(i, invalidate = true)
                        }
                    }
                    if (!menuItem.isEnabled) {
                        childItemView.isSelected = false
                        childItemView.isPressed = false
                        icon.isSelected = false
                        title.isSelected = false
                    }
                }
                childItemView.setOnClickListener {
                    val now: Long = SystemClock.uptimeMillis()
                    val mInputConnection = BaseInputConnection(this, true)
                    val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT, 0)
                    mInputConnection.sendKeyEvent(down)
                }
                title.text = null
                title.visibility = INVISIBLE
                this.addView(childItemView, i)
            }
        }

        typedArray.recycle()
    }

    private val _isAnimating by lazy {
        AtomicBoolean()
    }

    private var _isOpen = false

    val isOpen: Boolean
        get() = _isOpen

    val isAnimating: Boolean
        get() = _isAnimating.get()

    interface INavDrawerItemSelected {
        fun onSelected(position: Int, itemSelected: Int)
    }

    var onNavDrawerItemSelected: INavDrawerItemSelected? = null

    fun setEnableSelectedItem(position: Int, invalidate: Boolean) {
        Logger.d(this, message = "setEnableSelectedItem: $position")
        val index = _enablePositionMap.values.indexOf(position)
        setItemSelected(_enablePositionMap.keys.elementAt(index), invalidate)
    }
    fun setItemSelected(position: Int, invalidate: Boolean) {
        Logger.d(this, message = "setItemSelected: $position")
        val itemId = menu.getItem(position.takeIf {
            it > -1
        } ?: 0).itemId
        selectedItemId = itemId
        if (invalidate) {
            var isFocused = false
            for (i in 0 until childCount) {
                getChildAt(i).isSelected = false
                if (getChildAt(i).isFocused) {
                    isFocused = true
                }
            }
            if (isFocused) {
                findViewWithTag<LinearLayout>(itemId).isSelected = true
            } else {
                findViewWithTag<LinearLayout>(itemId).isPressed = true
            }
        }
    }

    fun openNav() {
        Logger.d(this@NavDrawerView, message = "openNav")
        if (!_isOpen && !isAnimating) {
            _isOpen = true
            _isAnimating.set(true)
            for (i in 0 until childCount) {
                val headerTitle = getChildAt(i).findViewById<TextView?>(R.id.row_header)
                headerTitle?.visibility = VISIBLE
                headerTitle?.text = menu.getItem(i).title
            }

            openNavigator.start()
        }

    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        forEachIndexed { _, view ->
            if (view.tag == selectedItemId) {
                return view.requestFocus(direction, previouslyFocusedRect)
            }
        }
        return super.requestFocus(direction, previouslyFocusedRect)
    }

    fun closeNav() {
        Logger.d(this@NavDrawerView, message = "closeNav")
        if (_isOpen && !isAnimating) {
            forceCloseNav()
        }
    }

    private fun forceCloseNav() {
        _isOpen = false
        _isAnimating.set(true)
        openNavigator.cancel()
        for (i in 0 until childCount) {
            val headerTitle = getChildAt(i).findViewWithTag<TextView?>(R.id.row_header)
            headerTitle?.visibility = VISIBLE
            headerTitle?.text = null
        }
        closeAnimator.start()
    }

    fun setCloseState() {
        forEachIndexed { _, childView ->
            childView.isSelected = false
            childView.isPressed = childView.tag == selectedItemId
            val headerTitle = childView.findViewById<TextView?>(R.id.row_header)
            headerTitle?.visibility = INVISIBLE
        }
        _isOpen = false
        _isAnimating.set(false)
    }

    override fun focusSearch(focused: View?, direction: Int): View {
        Logger.d(
            this@NavDrawerView,
            message = "{" +
                    "focusedView: $focused," +
                    "direction: $direction" +
                    "}"
        )
        if (!isOpen && focused != null
            && direction != FOCUS_LEFT
            && direction != FOCUS_RIGHT
        ) {
            return focused
        }
        if (direction == FOCUS_DOWN) {
            for (i in 0 until childCount) {
                if (getChildAt(i).tag == selectedItemId) {
                    if (i == childCount - 1) return getChildAt(i)
                    setItemSelected((i + 1) % childCount, false)
                    return getChildAt((i + 1) % childCount)
                }
            }
        }

        if (direction == FOCUS_UP) {
            for (i in 0 until childCount) {
                if (getChildAt(i).tag == selectedItemId) {
                    if (i == 0) return getChildAt(i)
                    setItemSelected((i + childCount - 1) % childCount, false)
                    return getChildAt((i + childCount - 1) % childCount)
                }
            }
        }

        if (focused?.tag == selectedItemId && direction == FOCUS_RIGHT) {
            return parent.focusSearch(focused, direction)
        }

        for (i in 0 until childCount) {
            if (getChildAt(i).tag == selectedItemId) {
                return getChildAt(i)
            }
        }
        return parent.focusSearch(focused, direction)
    }

    @SuppressLint("SoonBlockedPrivateApi")
    class PreAndroidApi26Reflection {
        private var preFocusByDefaultMethod: Method? = null

        init {
            preApi26Check()
            preFocusByDefaultMethod = try {
                ViewGroup::class.java
                    .getDeclaredMethod("setDefaultFocus", View::class.java)
            } catch (e: Exception) {
                Logger.e(this@PreAndroidApi26Reflection, exception = e)
                null
            }
        }

        fun setDefaultFocus(parentView: ViewGroup, chilView: View) {
            preFocusByDefaultMethod?.invoke(parentView, chilView)
        }
    }

    override fun toString(): String {
        return "NavDrawerView"
    }

    companion object {
        const val DEFAULT_DURATION = 250L
        private val preApi26 by lazy {
            PreAndroidApi26Reflection()
        }

        fun preApi26Check() {
            if (Build.VERSION.SDK_INT >= 26) {
                throw UnsupportedClassVersionError(
                    "This function can only be used for API Level < 29."
                )
            }
        }
    }

}