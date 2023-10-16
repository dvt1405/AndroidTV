package com.kt.apps.voiceselector.usecase

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.util.Log
import com.kt.apps.core.base.rxjava.BaseUseCase
import com.kt.apps.core.base.rxjava.MaybeUseCase
import com.kt.apps.core.utils.TAG
import com.kt.apps.voiceselector.di.VoiceSelectorScope
import com.kt.apps.voiceselector.models.AppInfo
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject
import javax.inject.Singleton

class AppQuery @Inject constructor(
    private val context: Context
): MaybeUseCase<List<AppInfo>>() {
    override fun prepareExecute(params: Map<String, Any>): Maybe<List<AppInfo>> {
        val category = params[EXTRA_CATEGORY] as? String ?: return Maybe.empty()
        Log.d(TAG, "prepareExecute AppQuery: $this")
        return Maybe.create { emitter ->
            val apps = context.getAllApps(category)
            emitter.onSuccess(apps)
        }
    }

    operator fun invoke(
        category: String
    ) = execute(mapOf(
        EXTRA_CATEGORY to category
    ))


    companion object {
        private const val EXTRA_CATEGORY  = "extra:category"
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun Context.getAllApps(category: String): List<AppInfo> {
        val queryIntent = Intent("android.intent.action.MAIN")
        queryIntent.addCategory(category)
        val resolveInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.packageManager.queryIntentActivities(
                queryIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )
        } else {
            this.packageManager.queryIntentActivities(
                queryIntent,
                PackageManager.GET_META_DATA
            )
        }

        return resolveInfoList.map {
            AppInfo(
                it.activityInfo.packageName,
                it.loadLabel(packageManager).toString(),
                it.loadIcon(packageManager),
                getLaunchIntent(it, category)
            )
        }.distinctBy {
            it.packageName
        }
    }

    private fun getLaunchIntent(
        info: ResolveInfo,
        category: String = "android.intent.category.LAUNCHER"
    ): Intent {
        val componentName =
            ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name)
        val intent = Intent("android.intent.action.MAIN")
        intent.addCategory(category)
        intent.component = componentName
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return intent
    }
}