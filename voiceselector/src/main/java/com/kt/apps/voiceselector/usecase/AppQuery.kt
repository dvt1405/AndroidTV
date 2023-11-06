package com.kt.apps.voiceselector.usecase

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import com.kt.apps.core.base.rxjava.MaybeUseCase
import com.kt.apps.core.utils.TAG
import com.kt.apps.voiceselector.models.AppInfo
import io.reactivex.rxjava3.core.Maybe
import javax.inject.Inject

class AppQuery @Inject constructor(
    private val context: Context
): MaybeUseCase<List<AppInfo>>() {
    override fun prepareExecute(params: Map<String, Any>): Maybe<List<AppInfo>> {
        val category = params[EXTRA_CATEGORY] as? String ?: return Maybe.empty()
        val action = params[EXTRA_ACTION] as? String ?: "android.intent.action.MAIN"
        val launchData = params[EXTRA_LAUNCHDATA] as? String ?: ""
        Log.d(TAG, "prepareExecute AppQuery: $params")
        return Maybe.create { emitter ->
            val apps = context.getAllApps(action, category)
                .union(context.queryByLaunchData(action, launchData))
            apps.forEach {
                Log.d(TAG, "query app : ${it.packageName} ")
            }
            emitter.onSuccess(apps.toList())
        }
    }

    operator fun invoke(
        action: String,
        category: String,
        launchData: String
    ) = execute(mapOf(
        EXTRA_CATEGORY to category,
        EXTRA_ACTION to action,
        EXTRA_LAUNCHDATA to launchData
    ))


    companion object {
        private const val EXTRA_CATEGORY  = "extra:category"
        private const val EXTRA_ACTION = "extra:action"
        private const val EXTRA_LAUNCHDATA = "extra:launchdata"
    }

    fun Context.queryByLaunchData(action: String, launchData: String): List<AppInfo> {
        val queryIntent = Intent(action).apply {
            data = Uri.parse(launchData)
        }
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
                Intent(action).apply {
                    data = Uri.parse(launchData)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }.distinctBy {
            it.packageName
        }
    }
    fun Context.getAllApps(action: String, category: String): List<AppInfo> {
        Log.d(TAG, "getAllApps: action $action category $category")
        val queryIntent = Intent(action)
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
                getLaunchIntent(it, action = action, category = category)
            )
        }.distinctBy {
            it.packageName
        }
    }

    private fun getLaunchIntent(
        info: ResolveInfo,
        action: String = "android.intent.action.MAIN",
        category: String = "android.intent.category.LAUNCHER"
    ): Intent {
        val componentName =
            ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name)
        val intent = Intent(action)
        intent.addCategory(category)
        intent.component = componentName
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return intent
    }
}