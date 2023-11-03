package com.kt.apps.voiceselector.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.kt.apps.core.base.BaseBottomSheetDialogFragment
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.voiceselector.R
import com.kt.apps.voiceselector.VoiceSelectorManager
import com.kt.apps.voiceselector.databinding.FragmentGgVoiceSelectorBinding
import com.kt.apps.voiceselector.log.VoiceSelectorLog
import com.kt.apps.voiceselector.log.logVoiceSelector
import com.kt.apps.voiceselector.models.VoicePackage
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject


class GGVoiceSelectorFragment : BaseBottomSheetDialogFragment<FragmentGgVoiceSelectorBinding>(),
    HasAndroidInjector {
    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var voicePackage: VoicePackage

    @Inject
    lateinit var voiceSelectorManager: VoiceSelectorManager

    @Inject
    lateinit var logger: IActionLogger

    private var installedVoicePackage: Intent? = null

    override val resLayout: Int = R.layout.fragment_gg_voice_selector

    override fun getTheme(): Int = R.style.BottomSheetDialog
    override fun initView(savedInstanceState: Bundle?) {
        setStyle(R.style.ModalBottomSheetDialog, theme)
        installedVoicePackage = requireArguments().get(EXTRA_VOICE_PACKAGE_INTENT) as? Intent
        voicePackage.icon?.run {
            binding.voiceAppItem.appIcon = ContextCompat.getDrawable(requireContext(), this)
        }
        binding.voiceAppItem.title = voicePackage.title
        binding.voiceAppItem.descriptionValue = voicePackage.description

        if (installedVoicePackage != null) {
            binding.installBtn.text = resources.getString(R.string.use)
        }

        arrayListOf(binding.voiceAppItem, binding.installBtn).forEach {
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
        arrayListOf(binding.ggAssistant, binding.useBtn).forEach {
            it.setOnClickListener {
                logger.logVoiceSelector(VoiceSelectorLog.VoiceSearchSelectGGOneTime)
                useGGAssistant()
            }
        }

        binding.alwaysBtn.setOnClickListener {
            logger.logVoiceSelector(VoiceSelectorLog.VoiceSearchSelectGGAlways)
            voiceSelectorManager.turnOnAlwaysGG()
            useGGAssistant()
        }
    }

    override fun initAction(savedInstanceState: Bundle?) {

    }

    override fun onStart() {
        super.onStart()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun useGGAssistant() {
        voiceSelectorManager.voiceGGSearch()
        dismiss()
    }
    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this )
        super.onAttach(context)
    }
    companion object {
        private const val EXTRA_VOICE_PACKAGE_INTENT = "extra:voice_intent"

        @JvmStatic
        fun newInstance(voiceIntent: Intent?) =
            GGVoiceSelectorFragment().apply {
                arguments = bundleOf(
                    EXTRA_VOICE_PACKAGE_INTENT to voiceIntent
                )
            }
    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector
}