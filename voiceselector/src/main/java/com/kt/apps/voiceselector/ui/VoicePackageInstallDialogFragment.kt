package com.kt.apps.voiceselector.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.doOnPreDraw
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.kt.apps.core.base.BaseBottomSheetDialogFragment
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.voiceselector.R
import com.kt.apps.voiceselector.VoiceSelectorManager
import com.kt.apps.voiceselector.databinding.FragmentVoiceSelectorDialogBinding
import com.kt.apps.voiceselector.di.VoiceSelectorScope
import com.kt.apps.voiceselector.log.VoiceSelectorLog
import com.kt.apps.voiceselector.log.logVoiceSelector
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

    @Inject
    lateinit var logger: IActionLogger

    private var installedVoicePackage: Intent? = null
    override val resLayout: Int
        get() = R.layout.fragment_voice_selector_dialog

    override fun getTheme(): Int = R.style.BottomSheetDialog

    private val voiceAppItem by lazy {
        binding.voiceAppItem
    }
    override fun initView(savedInstanceState: Bundle?) {
        setStyle(R.style.ModalBottomSheetDialog, theme)
        installedVoicePackage = requireArguments().get(EXTRA_VOICE_PACKAGE_INTENT) as? Intent
        voicePackage.icon?.run {
            voiceAppItem.appIcon = ContextCompat.getDrawable(requireContext(), this)
        }

        voiceAppItem.title = voicePackage.title
        voiceAppItem.descriptionValue = voicePackage.description

        if (installedVoicePackage != null) {
            binding.installBtn.text = resources.getString(R.string.use)
        }

        arrayListOf(binding.installBtn, binding.voiceAppItem).forEach {
            it.setOnClickListener {
                if (installedVoicePackage != null) {
                    dismiss()
                    try {
                        voiceSelectorManager.launchVoiceIntent(installedVoicePackage!!)
                    } catch (t: Throwable) {
                        requireActivity().showErrorDialog(content = "Đã xảy ra lỗi vui lòng thử lại sau")
                    }

                    return@setOnClickListener
                }
                logger.logVoiceSelector(VoiceSelectorLog.VoiceSearchSelectInstallKiki)
                voiceSelectorManager.launchVoicePackageStore()
                dismiss()
            }

        }

        binding.ggAssistant.setOnClickListener {
            logger.logVoiceSelector(VoiceSelectorLog.VoiceSearchSelectGGOneTime)
            voiceSelectorManager.voiceGGSearch()
            dismiss()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupForAndroidTV()
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

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        binding.installBtn.requestFocus()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupForAndroidTV() {
        binding.installBtn.apply {
            isFocusedByDefault = true
            nextFocusDownId = binding.ggAssistant.id
        }

        binding.ggAssistant.apply {
            nextFocusUpId = binding.installBtn.id
            setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }

    }
    companion object {
        private const val EXTRA_VOICE_PACKAGE_INTENT = "extra:voice_intent"
        fun newInstance(voiceIntent: Intent?): VoicePackageInstallDialogFragment =
            VoicePackageInstallDialogFragment().apply {
                arguments = bundleOf(
                    EXTRA_VOICE_PACKAGE_INTENT to voiceIntent
                )
            }

    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

}