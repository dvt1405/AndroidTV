package com.kt.apps.media.xemtv.ui.playback

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.kt.apps.core.base.leanback.Presenter
import com.kt.apps.core.base.leanback.RowPresenter
import com.kt.apps.core.extensions.model.TVScheduler
import com.kt.apps.media.xemtv.R
import java.util.Calendar

class ProgramSchedulePresenter : RowPresenter() {
    init {
        headerPresenter = null
    }

    override fun createRowViewHolder(parent: ViewGroup?): ViewHolder {
        val view = LayoutInflater.from(parent!!.context).inflate(
            R.layout.item_program, parent, false
        )
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        return ProgramScheduleViewHolder(view)
    }

    override fun onBindRowViewHolder(vh: ViewHolder?, item: Any?) {
        super.onBindRowViewHolder(vh, item)
        val programSchedule = (item as TVScheduler.Programme)
        (vh as ProgramScheduleViewHolder).apply {
            time.text = programSchedule.getTime()
            title.text = programSchedule.title
            description.text = programSchedule.description
            if (programSchedule.startTimeMilli <= Calendar.getInstance().timeInMillis) {
                view.alpha = 1f
            } else {
                view.alpha = 0.6f
            }
        }
    }


    class ProgramScheduleViewHolder(view: View) : ViewHolder(view) {
        val time: TextView
        val title: TextView
        val description: TextView

        init {
            time = view.findViewById(R.id.time)
            title = view.findViewById(R.id.title)
            description = view.findViewById(R.id.description)
            view.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    view.alpha = 1f
                } else {
                    view.alpha = 0.8f
                }
            }
            view.setOnClickListener {
                view.requestFocus()
            }
        }

    }
}