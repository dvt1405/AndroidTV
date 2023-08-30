package com.kt.apps.media.xemtv.ui.extensions

import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kt.apps.core.Constants
import com.kt.apps.core.base.BaseRowSupportFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.leanback.BrowseFrameLayout
import com.kt.apps.core.base.receiver.NetworkChangeReceiver.Companion.isNetworkAvailable
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.core.utils.showSuccessDialog
import com.kt.apps.media.xemtv.R
import kotlinx.coroutines.flow.combine
import javax.inject.Inject


class FragmentAddExtensions : BaseRowSupportFragment() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val extensionsViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[ExtensionsViewModel::class.java]
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = LayoutInflater.from(context)
            .inflate(R.layout.fragment_add_extensions, container, false)
        initView(rootView)
        progressManager.initialDelay = 500
        progressManager.setRootView(requireActivity().findViewById(android.R.id.content))
        return rootView
    }

    override fun initView(rootView: View) {
        (rootView as BrowseFrameLayout).setOnFocusSearchListener { focused, direction ->
            if (progressManager.isShowing) {
                return@setOnFocusSearchListener focused
            }
            return@setOnFocusSearchListener null
        }
        rootView.setOnDispatchKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && (event.action == KeyEvent.ACTION_DOWN)) {
                if (progressManager.isShowing) {
                    progressManager.hide()
                    extensionsViewModel.addExtensionConfigLiveData.removeObservers(viewLifecycleOwner)
                    extensionsViewModel.removePendingIPTVSource()
                    return@setOnDispatchKeyListener true
                }
            }
            return@setOnDispatchKeyListener false
        }
    }

    override fun onResume() {
        super.onResume()
        view?.requestFocus()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Logger.e(this, message = "onViewCreated")
        initAction(view)
        view.findViewById<TextInputEditText>(R.id.textInputEditText).setupFocusChangeShowKeyboard()
        view.findViewById<TextInputEditText>(R.id.textInputEditText_2).setupFocusChangeShowKeyboard()
        view.findViewById<TextInputEditText>(R.id.textInputEditText).requestFocus()
    }

    private fun TextInputEditText.setupFocusChangeShowKeyboard() {
        val oldFocusChange = onFocusChangeListener
        setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                val imm: InputMethodManager = requireContext().getSystemService(
                    InputMethodManager::class.java
                )
                if (imm.isActive(v)) {
                    imm.showSoftInput(v, 0)
                }
            }
            oldFocusChange?.onFocusChange(v, hasFocus)
        }
    }

    override fun initAction(rootView: View) {
        rootView.findViewById<TextInputEditText>(R.id.textInputEditText_2)
            .apply {
                this.doOnTextChanged { text, start, before, count ->
                    Uri.parse(text.toString()).pathSegments.lastOrNull {
                        it.trim().isNotEmpty()
                    }?.let {
                        rootView.findViewById<TextInputLayout>(R.id.textInputLayout).hint = it
                    } ?: kotlin.run {
                        rootView.findViewById<TextInputLayout>(R.id.textInputLayout).hint = "Tên nguồn"
                    }
                }
                this.setOnEditorActionListener { v, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        val imm: InputMethodManager = requireContext().getSystemService(
                            InputMethodManager::class.java
                        )
                        if (imm.isActive(v)) {
                            imm.hideSoftInputFromWindow(v.windowToken, 0)
                        }
                        addExtensionsSource()
                    } else if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        val imm: InputMethodManager = requireContext().getSystemService(
                            InputMethodManager::class.java
                        )
                        if (imm.isActive(v)) {
                            imm.hideSoftInputFromWindow(v.windowToken, 0)
                        }
                        rootView.findViewById<TextInputEditText>(R.id.textInputEditText).requestFocus()
                    }
                    return@setOnEditorActionListener true
                }
            }

        rootView.findViewById<TextInputEditText>(R.id.textInputEditText)
            .setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT
                    || actionId == EditorInfo.IME_ACTION_DONE
                ) {
                    val imm: InputMethodManager = requireContext().getSystemService(
                        InputMethodManager::class.java
                    )
                    if (imm.isActive(v)) {
                        imm.hideSoftInputFromWindow(v.windowToken, 0)
                    }
                    rootView.findViewById<ChipGroup?>(R.id.type_group)
                        ?.requestFocus()
                }
                return@setOnEditorActionListener true
            }

        rootView.findViewById<View>(R.id.btn_save)!!.setOnClickListener {
            addExtensionsSource()
        }
    }

    fun onDpadUp() {
        if (view?.findViewById<View>(R.id.textInputEditText_2)?.isFocused == true) {
            view?.findViewById<View>(R.id.textInputEditText)?.requestFocus()
        } else if (view?.findViewById<View>(R.id.btn_save)?.isFocused == true) {
            view?.findViewById<View>(R.id.textInputEditText_2)?.requestFocus()
        }
    }

    fun onDpadCenter() {
    }

    private fun addExtensionsSource() {
        val inputTextUrl = view?.findViewById<TextInputEditText>(R.id.textInputEditText_2)
        if (inputTextUrl?.text.isNullOrBlank()) {
            showErrorDialog(
                content = "Đường dẫn không được bỏ trống")
            return
        }
        val sourceUrl = view?.findViewById<TextInputLayout>(R.id.textInputLayout_2)?.prefixText.toString() +
                inputTextUrl?.text.toString()

        if (!Constants.regexHttp.matches(sourceUrl)) {
            showErrorDialog(
                titleText = "Lỗi",
                content = "Đường dẫn không hợp lệ! Vui lòng kiểm tra lại")
            return
        }
        val type = when (view?.findViewById<ChipGroup>(R.id.type_group)
            ?.checkedChipId) {
            R.id.type_football -> ExtensionsConfig.Type.FOOTBALL
            R.id.type_movie -> ExtensionsConfig.Type.MOVIE
            else -> ExtensionsConfig.Type.TV_CHANNEL
        }
        val name = (view?.findViewById<TextInputEditText>(R.id.textInputEditText)
            ?.text?.trim().takeIf {
                !it.isNullOrEmpty()
            } ?: view?.findViewById<TextInputLayout>(R.id.textInputLayout)
            ?.hint).toString()
        val extensionsConfig = ExtensionsConfig(
            name,
            sourceUrl,
            type
        )
        if (!requireContext().isNetworkAvailable()) {
            showErrorDialog(content = "Vui lòng kết nối internet và thử lại")
            return
        }
        extensionsViewModel.addIPTVSource(extensionsConfig)
        extensionsViewModel.addExtensionConfigLiveData.removeObservers(viewLifecycleOwner)
        extensionsViewModel.addExtensionConfigLiveData.observe(viewLifecycleOwner,
            object : Observer<DataState<ExtensionsConfig>> {
                override fun onChanged(t: DataState<ExtensionsConfig>?) {
                    when (t) {
                        is DataState.Success -> {
                            progressManager.hide()
                            showSuccessDialog(
                                content = "Thêm nguồn IPTV thành công!" +
                                        "\r\nVui lòng chờ trong giây lát",
                                onSuccessListener = {
                                    if (!this@FragmentAddExtensions.isDetached) {
                                        requireActivity().supportFragmentManager
                                            .popBackStack()
                                    }
                                }
                            )
                            extensionsViewModel.addExtensionConfigLiveData.removeObserver(this)
                            Logger.d(this@FragmentAddExtensions, message = "Save link success")
                        }

                        is DataState.Loading -> {
                            progressManager.show()
                        }

                        is DataState.Error -> {
                            progressManager.hide()
                            if (context?.isNetworkAvailable() == true) {
                                showErrorDialog(
                                    titleText = "Lỗi thêm nguồn video",
                                    content = t.throwable.message)
                            } else {
                                showErrorDialog(content = "Vui lòng kiểm tra kết nối internet và thử lại!")
                            }
                            Logger.e(this@FragmentAddExtensions, exception = t.throwable)
                        }

                        else -> {

                        }
                    }
                }
            })
    }

    override fun onDestroyView() {
        extensionsViewModel.removePendingIPTVSource()
        super.onDestroyView()
    }

}