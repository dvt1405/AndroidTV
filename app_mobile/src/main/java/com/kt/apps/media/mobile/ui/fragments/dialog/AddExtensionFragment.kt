package com.kt.apps.media.mobile.ui.fragments.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import cn.pedant.SweetAlert.ProgressHelper
import com.kt.apps.core.base.BaseDialogFragment
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.extensions.ParserExtensionsSource
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.fadeOut
import com.kt.apps.core.utils.hideKeyboard
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.AddExtensionDialogBinding
import com.kt.apps.media.mobile.utils.*
import com.kt.apps.media.mobile.viewmodels.AddIptvViewModels
import com.pnikosis.materialishprogress.ProgressWheel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
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

    private val sourceLinkLayout by lazy {
        binding.textInputLayout1
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

    private var isUserEditName: Boolean = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(com.kt.apps.resources.R.color.transparent)
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        return dialog
    }
    override fun initView(savedInstanceState: Bundle?) {
        Log.d(TAG, "initView: ")
        sourceLinkLayout.prefixTextView.updateLayoutParams<ViewGroup.LayoutParams> {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        sourceLinkLayout.prefixTextView.gravity = Gravity.CENTER

        this.view?.setBackgroundColor(resources.getColor(R.color.purple_700))
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
                val text = (it ?: "").let { t -> "${(sourceLinkLayout.prefixText ?: "")}${t}" }
                if(text.isNotEmpty() && !text.startsWith("http")) {
                    sourceLinkEditText.error = "Đường dẫn không hợp lệ! Đường dẫn phải phải bắt đầu bằng: \"http\""
                }
                if (!isUserEditName && sourceLinkEditText.isFocused) {
                    sourceNameEditText.setText(Uri.parse(text).pathSegments.filter { t -> t.trim().isNotEmpty() }.lastOrNull() ?: "")
                }
            }
        }

        sourceNameEditText.doAfterTextChanged {
            if ((it ?: "").isNotEmpty() && !isUserEditName && sourceNameEditText.isFocused) {
                isUserEditName = true
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
                            this@AddExtensionFragment.dismiss()
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
        val link = sourceLinkEditText.text.toString().let {
            "${sourceLinkLayout.prefixText ?: ""}${it}"
        }
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
        return (name?.trim()?.isNotEmpty() == true
                && source?.trim()?.isNotEmpty() == true)
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