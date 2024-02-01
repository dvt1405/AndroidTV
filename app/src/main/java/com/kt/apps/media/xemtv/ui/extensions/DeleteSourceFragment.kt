package com.kt.apps.media.xemtv.ui.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.leanback.GuidanceStylist
import com.kt.apps.core.base.leanback.GuidedAction
import com.kt.apps.core.base.leanback.GuidedStepSupportFragment
import com.kt.apps.core.base.leanback.ProgressBarManager
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.extensions.ParserExtensionsSource
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.utils.showSuccessDialog
import com.kt.apps.media.xemtv.ui.favorite.FavoriteViewModel
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class DeleteSourceFragment(
    val extensions: ExtensionsConfig,
    val progressBarManager: ProgressBarManager,
    val disposable: CompositeDisposable,
    val roomDataBase: RoomDataBase,
    val onDeleteSuccess: () -> Unit,
    val onUpdateSuccess: () -> Unit
) : GuidedStepSupportFragment(), HasAndroidInjector {
    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var parserExtensionsSource: ParserExtensionsSource

    private val extensionsViewModel by lazy {
        ViewModelProvider(this, viewModelFactory)[ExtensionsViewModel::class.java]
    }

    private val videoFavouriteViewModel by lazy {
        ViewModelProvider(this, viewModelFactory)[FavoriteViewModel::class.java]
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return GuidanceStylist.Guidance(
            extensions.sourceName,
            "Bạn có thể cập nhật hoặc xoá nguồn IPTV tại đây",
            "Nguồn IPTV",
            ContextCompat.getDrawable(
                requireContext(),
                com.kt.apps.media.xemtv.R.drawable.round_insert_link_64
            )
        )
    }

    @SuppressLint("CommitTransaction")
    override fun onGuidedActionClicked(action: GuidedAction?) {
        super.onGuidedActionClicked(action)
        when (action?.id) {
            ACTION_ID_REMOVE -> {
                progressBarManager.show()
                disposable.add(
                    Completable.mergeArray(
                        roomDataBase.extensionsChannelDao()
                            .deleteBySourceId(extensions.sourceUrl),
                        roomDataBase.extensionsConfig()
                            .delete(extensions),
                        roomDataBase.videoFavoriteDao()
                            .deleteBySourceId(extensions.sourceUrl)
                    )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            progressBarManager.hide()
                            requireActivity().showSuccessDialog(content = "Xoá nguồn kênh thành công", onSuccessListener = {
                                onDeleteSuccess()
                            })
                            videoFavouriteViewModel.onShowFavouriteToMain()
                            requireActivity().supportFragmentManager
                                .beginTransaction()
                                .remove(this)
                                .commit()
                        }, {
                            Logger.e(this@DeleteSourceFragment, exception = it)
                            progressBarManager.hide()
                            showSuccessDialog(content = "Xoá nguồn kênh thất bại")
                        })
                )

            }

            ACTION_ID_CANCEL -> {
                requireActivity().supportFragmentManager
                    .beginTransaction()
                    .remove(this)
                    .commit()
            }


            ACTION_ID_REFRESH -> {
                progressBarManager.show()
                val refreshDone = AtomicBoolean(false)
                disposable.add(
                    parserExtensionsSource.parseFromRemoteRxStream(extensions)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            if (!this.isAdded || this.isDetached || this.activity == null) {
                                return@subscribe
                            }
                            if (refreshDone.get()) {
                                return@subscribe
                            }
                            refreshDone.set(true)
                            onUpdateSuccess()
                            progressBarManager.hide()
                            showSuccessDialog(content = "Cập nhật nguồn kênh thành công", onSuccessListener = {
                                requireActivity().supportFragmentManager
                                    .beginTransaction()
                                    .remove(this)
                                    .commit()
                                Unit
                            })
                        }, {
                            if (!this.isAdded || this.isDetached  || this.activity == null) {
                                return@subscribe
                            }
                            progressBarManager.hide()
                            showSuccessDialog(content = "Cập nhật nguồn kênh thất bại", onSuccessListener = {
                                requireActivity().supportFragmentManager
                                    .beginTransaction()
                                    .remove(this)
                                    .commit()
                                Unit
                            })
                        }
                ))
            }

        }
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        super.onCreateActions(actions, savedInstanceState)

        actions.add(
            GuidedAction.Builder()
                .id(ACTION_ID_REFRESH)
                .title("Cập nhật")
                .description("Cập nhật lại danh sách kênh")
                .build()
        )

        actions.add(
            GuidedAction.Builder()
                .id(ACTION_ID_REMOVE)
                .title("Xoá nguồn IPTV")
                .description("Sau khi xoá nguồn sẽ không còn tồn tại")
                .build()
        )

        actions.add(
            GuidedAction.Builder()
                .id(ACTION_ID_CANCEL)
                .title("Trờ về")
                .build()
        )

    }

    companion object {
        private const val ACTION_ID_REMOVE = 1L
        private const val ACTION_ID_REFRESH = 2L
        private const val ACTION_ID_CANCEL = 3L
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return androidInjector
    }
}