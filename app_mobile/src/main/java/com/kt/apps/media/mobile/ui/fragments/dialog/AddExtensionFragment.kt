package com.kt.apps.media.mobile.ui.fragments.dialog

import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import cn.pedant.SweetAlert.ProgressHelper
import com.kt.apps.core.base.BaseDialogFragment
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.hideKeyboard
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.AddExtensionDialogBinding
import com.kt.apps.media.mobile.utils.*
import com.kt.apps.media.mobile.viewmodels.AddIptvViewModels
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class AddExtensionFragment: BaseDialogFragment<AddExtensionDialogBinding>() {
    sealed class State {
        object EDITING: State()
        object PROCESSING: State()
        data class ERROR(val errorText: String) : State()
        object SUCCESS: State()
    }
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val processState: MutableStateFlow<State> = MutableStateFlow(State.EDITING)

    var onSuccess: (DialogFragment) -> Unit = { }

    private val animationQueue: JobQueue by lazy {
        JobQueue()
    }

    private val viewModel by lazy {
        AddIptvViewModels(ViewModelProvider(requireActivity(), factory))
    }

    override val layoutResId: Int
        get() = R.layout.add_extension_dialog

    private val sourceNameEditText by lazy {
        binding.extensionSourceName
    }

    private val sourceLinkEditText by lazy {
        binding.extensionSourceLink
    }

    private val saveButton by lazy {
        binding.saveButton
    }

    private val progressHelper by lazy {
        ProgressHelper(context).apply {
            this.progressWheel = binding.progressWheel
        }
    }

    private val progressDialog by lazy {
        binding.progressDialog
    }

    private val errorLayout by lazy {
        binding.errorLayout
    }

    private val errorText by lazy {
        binding.errorTextView
    }
    override fun initView(savedInstanceState: Bundle?) {
        Log.d(TAG, "initView: ")
    }

    @OptIn(FlowPreview::class)
    override fun initAction(savedInstanceState: Bundle?) {
        saveButton.isEnabled = false

        lifecycleScope.launch {
            combine(flow = sourceNameEditText.textChanges(), flow2 = sourceLinkEditText.textChanges(), transform = {
                    name, link ->
                return@combine validateInfo(name.toString(), link.toString())
            }).collect {
                processState.tryEmit(State.EDITING)
                saveButton.isEnabled = it
            }
        }

        lifecycleScope.launch {
            sourceLinkEditText.textChanges().debounce(250).collect {
                if(it.toString().isNotEmpty() && it?.startsWith("http") == false) {
                    sourceLinkEditText.error = "Đường dẫn không hợp lệ! Đường dẫn phải phải bắt đầu bằng: \"http\""
                }
            }
        }

        processState
            .distinctUntilChanged {
                new, old ->  new == old
            }
            .onEach { state ->
                when(state) {
                    State.EDITING -> {
                        animationQueue.submit(coroutineContext) {
                            saveButton.visibility = View.VISIBLE
                            val fadeIn = async {saveButton.ktFadeIn() }
                            errorLayout.visibility = View.GONE
                            progressDialog.visibility = View.GONE
                            fadeIn.await()
                            Log.d(TAG, "initAction: After await")
                        }
                    }
                    State.PROCESSING -> {
                        animationQueue.submit(coroutineContext) {
                            val fadeOut = async { saveButton.ktFadeOut() }
                            val fadeIn = async { progressDialog.ktFadeIn() }
                            errorLayout.visibility = View.GONE
                            awaitAll(fadeOut, fadeIn)
                            progressHelper.spin()
                        }
                    }
                    is State.ERROR -> {
                        animationQueue.submit(coroutineContext) {
                            saveButton.visibility = View.GONE
                            val fadeOut = async { progressDialog.ktFadeOut() }
                            errorText.text = state.errorText
                            val fadeIn = async { errorLayout.ktFadeIn() }
                            errorText.text = state.errorText
                            fadeOut.await()
                            progressHelper.stopSpinning()
                            progressDialog.visibility = View.GONE
                            fadeIn.await()
                        }
                    }
                    else -> { }
            }
        }.launchIn(lifecycleScope)

        saveButton.clicks().debounce(250)
            .onEach{
                hideKeyboard()
                addExtensionsSource()
            }.launchIn(lifecycleScope)
    }


    private suspend fun addExtensionsSource() {
        val name = sourceNameEditText.text.toString()
        val link = sourceLinkEditText.text.toString()
        if (validateInfo(name, link)) {
            val  extensionConfig = ExtensionsConfig(
                name,
                link
            )
            CoroutineScope(Dispatchers.Main).launch(CoroutineExceptionHandler { _, throwable ->
                Log.d(TAG, "addExtensionsSource: $throwable")
                processState.value = State.ERROR("Không thể tạo link")
            }) {
                processState.value = State.PROCESSING
                val result = viewModel.addIPTVSourceAsync(extensionConfig)
                processState.value = State.SUCCESS
                onSuccess(this@AddExtensionFragment)
            }
        }
    }
    private fun validateInfo(name: String?, source: String?): Boolean {
        return (name?.isNotEmpty() == true
                && source?.isNotEmpty() == true) && source.startsWith("http")
    }

    override fun onResume() {
        dialog?.window?.let {  window ->
            window.windowManager?.defaultDisplay?.let {display ->
                val size = Point()
                display.getSize(size)
                window.setLayout((size.x * 0.8).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
                window.setGravity(Gravity.CENTER)
            }
        }
        super.onResume()
    }


    companion object {
        val TAG: String
            get() = AddExtensionFragment::class.java.simpleName
    }

}

class JobQueue {
    private val scope = MainScope()
    private val queue = Channel<Job>(Channel.UNLIMITED)

    init {
        scope.launch(Dispatchers.Default) {
            for (job in queue) {
                job.join()
            }
        }
    }

    fun submit(
        context: CoroutineContext,
        block: suspend CoroutineScope.() -> Unit
    ) {
        val job = scope.launch(context, CoroutineStart.LAZY, block)
        queue.trySend(job)
    }

    fun submit(job: Job) {
        queue.trySend(job)
    }

    fun cancel() {
        queue.cancel()
        scope.cancel()
    }
}