package com.kt.apps.voiceselector.ui

import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.kt.apps.core.base.BaseBottomSheetDialogFragment
import com.kt.apps.voiceselector.R
import com.kt.apps.voiceselector.VoiceSelectorManager
import com.kt.apps.voiceselector.databinding.FragmentVoiceSelectorDialogBinding
import com.kt.apps.voiceselector.di.VoiceSelectorScope
import com.kt.apps.voiceselector.models.VoicePackage
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

/**
 *
 * A fragment that shows a list of items as a modal bottom sheet.
 *
 * You can show this modal bottom sheet from your activity like this:
 * <pre>
 *    VoiceSelectorDialogFragment.newInstance(30).show(supportFragmentManager, "dialog")
 * </pre>
 */

class VoicePackageInstallDialogFragment : BaseBottomSheetDialogFragment<FragmentVoiceSelectorDialogBinding>(), HasAndroidInjector {
    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var voicePackage: VoicePackage

    @Inject
    lateinit var voiceSelectorManager: VoiceSelectorManager
    override val resLayout: Int
        get() = R.layout.fragment_voice_selector_dialog

    override fun getTheme(): Int = R.style.BottomSheetDialog

    private val voiceAppItem by lazy {
        binding.voiceAppItem
    }
    override fun initView(savedInstanceState: Bundle?) {
        setStyle(R.style.ModalBottomSheetDialog, theme)
        voicePackage.icon?.run {
            voiceAppItem.appIcon = ContextCompat.getDrawable(requireContext(), this)
        }

        voiceAppItem.title = voicePackage.title
        voiceAppItem.descriptionValue = voicePackage.description

        binding.installBtn.setOnClickListener {
            voiceSelectorManager.launchVoicePackageStore()
            dismiss()
        }

        binding.ggAssistant.setOnClickListener {
            voiceSelectorManager.voiceGGSearch()
            dismiss()
        }
    }

    override fun initAction(savedInstanceState: Bundle?) {
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this )
        super.onAttach(context)
    }

    override fun onStart() {
        super.onStart()
        bottomSheetBehavior.peekHeight = 0
        view?.doOnPreDraw {
            bottomSheetBehavior.peekHeight = requireView().measuredHeight - binding.ggAssistant.measuredHeight
        }
    }
    companion object {
        fun newInstance(): VoicePackageInstallDialogFragment =
            VoicePackageInstallDialogFragment().apply {

            }

    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

}