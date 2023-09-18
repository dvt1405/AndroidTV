package com.kt.apps.media.mobile.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.annotation.CheckResult
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.kt.apps.core.GlideApp
import com.kt.apps.core.base.DataState
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.databaseviews.ExtensionsChannelDBWithCategoryViews
import com.kt.apps.core.storage.local.dto.HistoryMediaItemDTO
import com.kt.apps.core.storage.local.dto.VideoFavoriteDTO
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.utils.*
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.media.mobile.App
import com.kt.apps.resources.R
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

val Fragment.screenWidth: Int
    get() = resources.displayMetrics.widthPixels

val Fragment.screenHeight: Int
    get() = resources.displayMetrics.heightPixels

val View.hitRectOnScreen: Rect
    get() {
        val hitRect = Rect()
        val location =  intArrayOf(0, 0)
        getHitRect(hitRect)
        getLocationOnScreen(location)
        hitRect.offset(location[0], location[1])
        return hitRect
    }

fun <T> debounce(
    waitMs: Long = 300L,
    coroutineScope: CoroutineScope,
    destinationFunction: (T) -> Unit
): (T) -> Unit {
    var debounceJob: Job? = null
    return { param: T ->
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(waitMs)
            destinationFunction(param)
        }
    }
}

@CheckResult
fun EditText.textChanges(): Flow<CharSequence?> {
    return callbackFlow {
        val listener = doOnTextChanged { text, _, _, _ -> trySend(text) }
        awaitClose { removeTextChangedListener(listener) }
    }.onStart { emit(text) }
}


fun EditText.onFocus(): Flow<Unit> {
    return callbackFlow {
        onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->

        }
    }
}

fun SwipeRefreshLayout.onRefresh(): Flow<Unit> {
    return callbackFlow {
        val refreshListener: SwipeRefreshLayout.OnRefreshListener = SwipeRefreshLayout.OnRefreshListener { trySend(Unit) }
        setOnRefreshListener(refreshListener)
        awaitClose {
            setOnRefreshListener(null)
        }
    }
}

fun View.clicks(): Flow<Unit> {
    return callbackFlow {
        setOnClickListener {
            trySend(Unit)
        }
        awaitClose { setOnClickListener(null) }
    }
}

fun Button.clicks(): Flow<Unit> {
    return callbackFlow {
        setOnClickListener {
            trySend(Unit)
        }
        awaitClose { setOnClickListener(null) }
    }
}

fun ImageButton.clicks(): Flow<Unit> {
    return callbackFlow {
        setOnClickListener {
            trySend(Unit)
        }
        awaitClose { setOnClickListener(null) }
    }
}

fun EditText.onSubmit(callback: () -> Unit) {
    setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            callback()
            return@setOnEditorActionListener true
        }
        false
    }
}

fun EditText.submits(): Flow<CharSequence?> {
    return callbackFlow {
        onSubmit {
            trySend(text)
        }
        awaitClose {
            onSubmit {   }
        }
    }
}

suspend fun View.ktFadeIn() = suspendCancellableCoroutine<Unit> { cont ->
    fadeIn(true) {
        cont.resume(Unit)
    }
}

suspend fun View.ktFadeOut() = suspendCoroutine<Unit> { cont ->
    fadeOut(onAnimationEnd = {
        cont.resume(Unit)
    }, executeElse = true)
}
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun Animator.awaitEnd() = suspendCancellableCoroutine<Unit> { cont ->
    // Add an invokeOnCancellation listener. If the coroutine is
    // cancelled, cancel the animation too that will notify
    // listener's onAnimationCancel() function
    cont.invokeOnCancellation { cancel() }

    addListener(object : AnimatorListenerAdapter() {
        private var endedSuccessfully = true

        override fun onAnimationCancel(animation: Animator) {
            // Animator has been cancelled, so flip the success flag
            endedSuccessfully = false
        }

        override fun onAnimationEnd(animation: Animator) {
            // Make sure we remove the listener so we don't keep
            // leak the coroutine continuation
            animation.removeListener(this)

            if (cont.isActive) {
                // If the coroutine is still active...
                if (endedSuccessfully) {
                    // ...and the Animator ended successfully, resume the coroutine
                    cont.resume(Unit) { }
                } else {
                    // ...and the Animator was cancelled, cancel the coroutine too
                    cont.cancel()
                }
            }
        }
    })
}

fun Fragment.showKeyboard() {
    (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
}

fun RecyclerView.removeAllItemDecoration() {
    while (itemDecorationCount > 0) {
        removeItemDecorationAt(0)
    }
}

inline fun <reified T> groupAndSort(list: List<T>): List<Pair<String, List<T>>> {
    return when (T::class) {
        TVChannel::class -> list.groupBy { (it as TVChannel).tvGroup }
            .toList()
            .sortedWith(Comparator { o1, o2 ->
                return@Comparator if (o2.first == TVChannelGroup.VOV.value || o2.first == TVChannelGroup.VOH.value)
                    if (o1.first == TVChannelGroup.VOH.value) 0 else -1
                else 1
            })
            .map {
                return@map Pair(TVChannelGroup.valueOf(it.first).value, it.second)
            }
        ExtensionsChannel::class -> list.groupBy { (it as ExtensionsChannel).tvGroup }
            .toList()
            .sortedWith(Comparator { o1, o2 ->
                return@Comparator o1.first.compareTo(o2.first)
            })
        FootballMatch::class -> list.groupBy { (it as FootballMatch).league }
            .toList()
            .sortedBy { it.first }
        VideoFavoriteDTO::class -> list.groupBy { (it as VideoFavoriteDTO).category }
            .toList()
            .sortedBy { it.first }
        else -> emptyList()
    }
}

fun <T> LiveData<DataState<T>>.asProgressFlow(): Flow<Boolean> {
    return callbackFlow {
        val observer = Observer<DataState<*>> {value ->
            when (value) {
                is DataState.Loading -> trySend(true)
                else -> trySend(false)
            }
        }
        observeForever(observer)
        awaitClose {
            removeObserver(observer)
        }
    }
}

suspend fun <T> LiveData<DataState<T>>.await(tag: String = "TAG") : T {
    return suspendCancellableCoroutine { continuation ->
        val oldData = value
        val observer = object : Observer<DataState<T>> {
            override fun onChanged(value: DataState<T>) {
                if (value == oldData) {
                    return
                }
                when (value) {
                    is DataState.Success -> {
                        removeObserver(this)
                        continuation.resume(value.data)
                    }
                    is DataState.Error -> {
                        removeObserver(this)
                        continuation.resumeWithException(value.throwable)
                    }
                    else -> { }
                }

            }
        }

        observeForever(observer)

        continuation.invokeOnCancellation {
            removeObserver(observer)
        }
    }
}

fun <T> LiveData<DataState<T>>.asUpdateFlow(tag: String = ""): Flow<T> {
    return callbackFlow {
        val observer = Observer<DataState<T>> {value ->
            when (value) {
                is DataState.Success -> trySend(value.data)
                is DataState.Update -> trySend(value.data)
                else -> { }
            }
        }
        Log.d(TAG, "asUpdateFlow register with $tag")
        observeForever(observer)
        awaitClose {
            Log.d(TAG, "asUpdateFlow awaitClose with $tag")
            removeObserver(observer)
        }
    }
}

//emit data only success
fun <T> LiveData<DataState<T>>.asSuccessFlow(tag: String): Flow<T> {
    return callbackFlow {
        val observer = Observer<DataState<T>> {value ->
            when (value) {
                is DataState.Success -> trySend(value.data)
                else -> { }
            }
        }
        observeForever(observer)
        awaitClose {
            removeObserver(observer)
        }
    }
}

fun <T> LiveData<DataState<T>>.asDataStateFlow(tag: String): Flow<DataState<T>> {
    return callbackFlow {
        val observer = Observer<DataState<T>> {value ->
            trySend(value)
        }
        observeForever(observer)
        awaitClose {
            removeObserver(observer)
        }
    }
}


inline val channelItemDecoration
    get() = object: RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val value = App.get().resources.getDimensionPixelSize(com.kt.apps.media.mobile.R.dimen.item_channel_decoration)
            outRect.right = value
            outRect.bottom = value
        }
    }

inline fun <T1: Any, T2: Any, R: Any> safeLet(p1: T1?, p2: T2?, block: (T1, T2)->R?): R? {
    return if (p1 != null && p2 != null) block(p1, p2) else null
}
inline fun <T1: Any, T2: Any, T3: Any, R: Any> safeLet(p1: T1?, p2: T2?, p3: T3?, block: (T1, T2, T3)->R?): R? {
    return if (p1 != null && p2 != null && p3 != null) block(p1, p2, p3) else null
}
inline fun <T1: Any, T2: Any, T3: Any, T4: Any, R: Any> safeLet(p1: T1?, p2: T2?, p3: T3?, p4: T4?, block: (T1, T2, T3, T4)->R?): R? {
    return if (p1 != null && p2 != null && p3 != null && p4 != null) block(p1, p2, p3, p4) else null
}
inline fun <T1: Any, T2: Any, T3: Any, T4: Any, T5: Any, R: Any> safeLet(p1: T1?, p2: T2?, p3: T3?, p4: T4?, p5: T5?, block: (T1, T2, T3, T4, T5)->R?): R? {
    return if (p1 != null && p2 != null && p3 != null && p4 != null && p5 != null) block(p1, p2, p3, p4, p5) else null
}

fun ConstraintSet.fillParent(viewId: Int) {
    arrayListOf(ConstraintSet.START, ConstraintSet.TOP, ConstraintSet.END, ConstraintSet.BOTTOM).forEach {
        connect(viewId, it, ConstraintSet.PARENT_ID, it)
    }
}

fun ConstraintSet.matchParentWidth(viewId: Int) {
    connect(viewId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
    connect(viewId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
    constrainedWidth(viewId, true)
}

fun ConstraintSet.alignParent(viewId: Int, side: Int, margin: Int = 0) {
    connect(viewId, side, ConstraintSet.PARENT_ID, side)
    setMargin(viewId, side, margin)
}


fun LifecycleOwner.repeatLaunchesOnLifeCycle(
    state: Lifecycle.State,
    block: suspend CoroutineScope.() -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(state, block)
    }
}

fun CoroutineScope.avoidExceptionLaunch(block: suspend CoroutineScope.() -> Unit) = launch (
    CoroutineExceptionHandler { coroutineContext, throwable ->  }, block = block)

fun TVChannel.loadImgDrawable(context: Context): Drawable? {
    val context = context.applicationContext
    val id = context.resources.getIdentifier(
        this.logoChannel.trim().removeSuffix(".png")
            .removeSuffix(".jpg")
            .removeSuffix(".webp")
            .removeSuffix(".jpeg"),
        "drawable",
        context.packageName
    )
    try {
        val drawable = ContextCompat.getDrawable(context, id)
        return drawable
    } catch (e: Exception) {
        return null
    }
}

fun FootballMatch.isLiveMatch(): Boolean {
    val calendar = Calendar.getInstance(Locale.TAIWAN)
    val currentTime = calendar.timeInMillis / 1000
    return  (currentTime - kickOffTimeInSecond) > -20 * 60
            && (currentTime - kickOffTimeInSecond) < 150 * 60
}

fun ExtensionsChannelDBWithCategoryViews.toExtensionChannel() : ExtensionsChannel {
    return ExtensionsChannel(
        tvGroup = categoryName,
        logoChannel = logoChannel,
        tvChannelName = tvChannelName,
        tvStreamLink = tvStreamLink,
        sourceFrom = sourceFrom,
        channelId = "",
        isHls = tvStreamLink.contains("m3u8"),
        extensionSourceId = ""
    )
}
