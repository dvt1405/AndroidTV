package com.kt.apps.media.xemtv.ui.extensions

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.kt.apps.core.base.leanback.GuidanceStylist
import com.kt.apps.core.base.leanback.GuidedAction
import com.kt.apps.core.base.leanback.GuidedStepSupportFragment
import com.kt.apps.core.base.leanback.ProgressBarManager
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.utils.showSuccessDialog
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class DeleteSourceFragment(
    val extensions: ExtensionsConfig,
    val progressBarManager: ProgressBarManager,
    val disposable: CompositeDisposable,
    val roomDataBase: RoomDataBase,
    val onDeleteSuccess: () -> Unit
) : GuidedStepSupportFragment() {

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return GuidanceStylist.Guidance(
            "Xoá nguồn: ${extensions.sourceName}",
            "Sau khi xoá, nguồn kênh sẽ không còn tồn tại nữa!",
            "",
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
            1L -> {
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
                            requireActivity().supportFragmentManager
                                .beginTransaction()
                                .remove(this)
                                .commit()
                            showSuccessDialog(content = "Xoá nguồn kênh thành công", onSuccessListener = {
                                onDeleteSuccess()
                            })
                        }, {
                            Logger.e(this@DeleteSourceFragment, exception = it)
                            progressBarManager.hide()
                            showSuccessDialog(content = "Xoá nguồn kênh thất bại")
                        })
                )

            }

            2L -> {
                requireActivity().supportFragmentManager
                    .beginTransaction()
                    .remove(this)
                    .commit()
            }
        }
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        super.onCreateActions(actions, savedInstanceState)
        actions.add(
            GuidedAction.Builder()
                .id(1)
                .title("Có")
                .description("Xoá nguồn kênh")
                .build()
        )

        actions.add(
            GuidedAction.Builder()
                .id(2)
                .title("Huỷ")
                .build()
        )
    }
}