package com.kt.apps.voiceselector

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView

class AppItemView : ConstraintLayout {

    private val logoIcon by lazy {
        findViewById<ShapeableImageView>(R.id.app_icon)
    }

    private val titleTextView by lazy {
        findViewById<MaterialTextView>(R.id.title)
    }

    private val descriptionTextView by lazy {
        findViewById<MaterialTextView>(R.id.description)
    }
    var appIcon: Drawable?
        get() = logoIcon.drawable
        set(value) {
            logoIcon.setImageDrawable(value)
        }

    var title: String
        get() = titleTextView.text.toString()
        set(value) {
            titleTextView.text = value
        }

    var descriptionValue: String
        get() = descriptionTextView.text.toString()
        set(value) {
            descriptionTextView.text = value
        }
    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        View.inflate(context, R.layout.app_item, this)
        context.obtainStyledAttributes(attrs, R.styleable.AppItemView, defStyle, 0).run {
            getDrawable(R.styleable.AppItemView_icon_logo)?.run {
                findViewById<ShapeableImageView>(R.id.app_icon).setImageDrawable(this)
            }

            getString(R.styleable.AppItemView_title)?.run {
                findViewById<MaterialTextView>(R.id.title)?.text = this
            }

            getString(R.styleable.AppItemView_description)?.run {
                findViewById<MaterialTextView>(R.id.description)?.text = this
            }

            this.recycle()
        }
    }




}