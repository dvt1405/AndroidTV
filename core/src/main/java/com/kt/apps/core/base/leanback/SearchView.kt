package com.kt.apps.core.base.leanback

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.SearchManager
import android.app.SearchableInfo
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.View.OnKeyListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ListPopupWindow
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.cursoradapter.widget.CursorAdapter
import com.kt.apps.core.R
import com.kt.apps.core.logging.Logger
import java.lang.reflect.Method

class SearchView @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet,
    defStyle: Int = 0
) : FrameLayout(context, attr, defStyle), View.OnClickListener {
    private var mAppSearchData: Bundle? = null
    private var mQueryHint: CharSequence? = null
    private val mOnSuggestionListener: OnSuggestionListener? = null
    private val mSuggestionRowLayout = 0
    private val mSuggestionCommitIconResId = 0

    // Intents used for voice searching.
    private var mVoiceWebSearchIntent: Intent? = null
    private var mVoiceAppSearchIntent: Intent? = null
    private var mDefaultQueryHint: CharSequence? = null
    private var mOnQueryChangeListener: OnQueryTextListener? = null
    private val mOnCloseListener: OnCloseListener? = null
    private var mSearchSrcTextView: SearchAutoComplete? = null
    private var mOnQueryTextFocusChangeListener: OnFocusChangeListener? = null
    private var mUserQuery: CharSequence? = null
    private var mSearchable: SearchableInfo? = null
    private val mSearchEditFrame: View? = null
    private val mSearchPlate: View? = null
    private val mSubmitArea: View? = null
    private val mSearchButton: ImageView?
    private val mGoButton: ImageView?
    private val mCloseButton: ImageView?
    private val mVoiceButton: ImageView?
    private var mOldQueryText: CharSequence? = null
    private var mVoiceButtonEnabled = false
    private val mSearchHintIcon: Drawable? = null
    private var mIconifiedByDefault = false
    private var mIconified = false
    private val mCollapsedIcon: ImageView? = null
    private var mSubmitButtonEnabled = false
    private val mExpandedInActionView = false
    var mSuggestionsAdapter: CursorAdapter? = null
    private val mOnEditorActionListener by lazy {
        TextView.OnEditorActionListener { v, actionId, event ->
            onSubmitQuery()
            true
        }
    }
    @RequiresApi(Build.VERSION_CODES.M)
    private var mTextKeyListener = OnKeyListener { v, keyCode, event -> // guard against possible race conditions
        if (mSearchable == null) {
            return@OnKeyListener false
        }
        Logger.d(
            this@SearchView,
            message = "mTextListener.onKey($keyCode,$event), selection: "
        )
        if (event.hasNoModifiers()
            && event.action == KeyEvent.ACTION_UP
            && keyCode == KeyEvent.KEYCODE_DPAD_CENTER
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!rootWindowInsets.isVisible(1 shl 3)) {
                    showKeyboard()
                }
            } else {
                showKeyboard()
            }
        }
        if (!mSearchSrcTextView!!.isEmpty && event.hasNoModifiers()) {
            if (event.action == KeyEvent.ACTION_UP) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    v.cancelLongPress()
                    launchQuerySearch(
                        KeyEvent.KEYCODE_UNKNOWN, null, mSearchSrcTextView!!.text
                            .toString()
                    )
                    return@OnKeyListener true
                }
            }
        }
        false
    }

    var queryHint: CharSequence
        get() {
            val hint: CharSequence = mQueryHint ?: if (mSearchable != null && mSearchable!!.hintId != 0) {
                context.getText(mSearchable!!.hintId)
            } else {
                mDefaultQueryHint!!
            }
            return hint
        }
        set(value) {
            mSearchSrcTextView?.hint = value
            mQueryHint = value
        }

    private val mReleaseCursorRunnable = Runnable {
        mSuggestionsAdapter?.changeCursor(null)
    }

    private val mDropDownAnchor: View? = null


    val searchEdtAutoComplete: SearchAutoComplete?
        get() = mSearchSrcTextView

    private val mTextWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, before: Int, after: Int) {}
        override fun onTextChanged(
            s: CharSequence, start: Int,
            before: Int, after: Int
        ) {
            this@SearchView.onTextChanged(s)
        }

        override fun afterTextChanged(s: Editable) {}
    }

    interface OnQueryTextListener {
        fun onQueryTextSubmit(query: String?): Boolean
        fun onQueryTextChange(newText: String?): Boolean
    }


    interface OnCloseListener {
        fun onClose(): Boolean
    }

    init {
        val typedAttr = context.obtainStyledAttributes(R.styleable.SearchView)
        val resLayout = typedAttr.getResourceId(
            R.styleable.SearchView_searchLayout,
            R.layout.default_search_view
        )
        LayoutInflater.from(context)
            .inflate(resLayout, this, true)

        mSearchButton = findViewById(androidx.appcompat.R.id.search_button)
        mGoButton = findViewById(androidx.appcompat.R.id.search_go_btn)
        mCloseButton = findViewById(R.id.search_close_btn)
        mVoiceButton = findViewById(R.id.search_voice_btn)
        mSearchSrcTextView = findViewById(R.id.search_src_text)
        mSearchSrcTextView?.setSearchView(this)
        mDefaultQueryHint = context.getString(
            typedAttr.getResourceId(
                R.styleable.SearchView_searchQueryHintDefault,
                R.string.search_query_hint_default
            )
        )
        mVoiceWebSearchIntent = Intent(RecognizerIntent.ACTION_WEB_SEARCH).apply {
            this.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
            )
        }

        mVoiceButton?.setOnClickListener(this)
        mCloseButton?.setOnClickListener(this)
        mSearchButton?.setOnClickListener(this)

        mVoiceAppSearchIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            this.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val scaleAnimator = ValueAnimator.ofFloat(0f, 1f)
            .apply {
                this.duration = 300
                this.addUpdateListener {
                    mVoiceButton?.scaleX = (1 + 0.2f * (it.animatedValue as Float))
                    mVoiceButton?.scaleY = (1 + 0.2f * (it.animatedValue as Float))
                }
            }

        mVoiceButton.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scaleAnimator.start()
                mSearchSrcTextView?.hint = "Nói để tìm kiếm nội dung"
            } else {
                scaleAnimator.reverse()
                mSearchSrcTextView?.hint = mQueryHint
            }
        }

        mSearchSrcTextView!!.addTextChangedListener(mTextWatcher)
        mSearchSrcTextView!!.setOnEditorActionListener(mOnEditorActionListener)
//        mSearchSrcTextView!!.onItemClickListener = mOnItemClickListener
//        mSearchSrcTextView!!.onItemSelectedListener = mOnItemSelectedListener
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mSearchSrcTextView!!.setOnKeyListener(mTextKeyListener)
        }

        mSearchSrcTextView!!.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (mOnQueryTextFocusChangeListener != null) {
                mOnQueryTextFocusChangeListener!!.onFocusChange(this@SearchView, hasFocus)
            }
        }
        updateQueryHint()
        typedAttr.recycle()
    }

    fun isIconified(): Boolean {
        return mIconified
    }

    fun setIconifiedByDefault(iconified: Boolean) {
        if (mIconifiedByDefault == iconified) return
        mIconifiedByDefault = iconified
        updateViewsVisibility(iconified)
        updateQueryHint()
    }

    fun setSearchableInfo(searchable: SearchableInfo) {
        mSearchable = searchable
        updateSearchAutoComplete()
        updateQueryHint()
        // Cache the voice search capability
        mVoiceButtonEnabled = hasVoiceSearch()
        if (mVoiceButtonEnabled) {
            mSearchSrcTextView!!.privateImeOptions = IME_OPTION_NO_MICROPHONE
        }
        updateViewsVisibility(isIconified())
    }

    private fun updateViewsVisibility(collapsed: Boolean) {
        mIconified = collapsed
        val visCollapsed = if (collapsed) VISIBLE else GONE
        val hasText = !TextUtils.isEmpty(mSearchSrcTextView!!.text)
        mSearchButton?.visibility = visCollapsed
        updateSubmitButton(hasText)
        mSearchEditFrame?.visibility = if (collapsed) GONE else VISIBLE
        val iconVisibility: Int = if (mCollapsedIcon?.drawable == null || mIconifiedByDefault) {
            GONE
        } else {
            VISIBLE
        }
        mCollapsedIcon?.visibility = iconVisibility
        updateCloseButton()
        updateSubmitArea()
    }

    private fun updateQueryHint() {
        val hint: CharSequence = queryHint
        mSearchSrcTextView?.hint = getDecoratedHint(hint)
    }

    fun setOnQueryTextListener(listener: OnQueryTextListener) {
        mOnQueryChangeListener = listener
    }


    private fun getDecoratedHint(hintText: CharSequence): CharSequence {
        // If the field is always expanded or we don't have a search hint icon,
        // then don't add the search icon to the hint.
        if (!mIconifiedByDefault || mSearchHintIcon == null) {
            return hintText
        }
        val textSize = (mSearchSrcTextView!!.textSize * 1.25).toInt()
        mSearchHintIcon.setBounds(0, 0, textSize, textSize)
        val ssb = SpannableStringBuilder("   ")
        ssb.setSpan(ImageSpan(mSearchHintIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        ssb.append(hintText)
        return ssb
    }


    private fun updateVoiceButton(empty: Boolean) {
        var visibility = GONE
        if (mVoiceButtonEnabled && !isIconified() && empty) {
            visibility = VISIBLE
            mGoButton?.visibility = GONE
        }
        mVoiceButton?.visibility = visibility
    }

    fun setQuery(query: CharSequence?, submit: Boolean) {
        mSearchSrcTextView?.setText(query)
        if (query != null) {
            mSearchSrcTextView!!.setSelection(mSearchSrcTextView!!.length())
            mUserQuery = query
        }

        if (submit && !TextUtils.isEmpty(query)) {
            onSubmitQuery()
        }
    }

    private fun updateSearchAutoComplete() {
        mSearchSrcTextView!!.threshold = mSearchable!!.suggestThreshold
        mSearchSrcTextView!!.imeOptions = mSearchable!!.imeOptions
        var inputType = mSearchable!!.inputType
        // We only touch this if the input type is set up for text (which it almost certainly
        // should be, in the case of search!)
        if (inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT) {
            // The existence of a suggestions authority is the proxy for "suggestions
            // are available here"
            inputType = inputType and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE.inv()
            if (mSearchable!!.suggestAuthority != null) {
                inputType = inputType or InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
                inputType = inputType or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            }
        }
        mSearchSrcTextView!!.inputType = inputType
        if (mSuggestionsAdapter != null) {
            mSuggestionsAdapter?.changeCursor(null)
        }
        // attach the suggestions adapter, if suggestions are available
        // The existence of a suggestions authority is the proxy for "suggestions available here"
        if (mSearchable!!.suggestAuthority != null) {
//            mSuggestionsAdapter = SuggestionsAdapter(
//                context,
//                this,
//                mSearchable,
//                mOutsideDrawablesCache
//            )
//            mSearchSrcTextView!!.setAdapter<CursorAdapter>(mSuggestionsAdapter)
//            (mSuggestionsAdapter as SuggestionsAdapter).setQueryRefinement(
//                if (mQueryRefinement) SuggestionsAdapter.REFINE_ALL else SuggestionsAdapter.REFINE_BY_ENTRY
//            )
        }
    }

    fun onTextChanged(newText: CharSequence) {
        val text: CharSequence = mSearchSrcTextView!!.text
        mUserQuery = text
        val hasText = !TextUtils.isEmpty(text)
        updateSubmitButton(hasText)
        updateCloseButton()
        updateSubmitArea()
        if (mOnQueryChangeListener != null && !TextUtils.equals(newText, mOldQueryText)) {
            mOnQueryChangeListener!!.onQueryTextChange(newText.toString())
        }
        mOldQueryText = newText.toString()
    }


    private fun isSubmitAreaEnabled(): Boolean {
        return (mSubmitButtonEnabled || mVoiceButtonEnabled) && !isIconified()
    }

    private fun updateSubmitButton(hasText: Boolean) {
        var visibility = GONE
        if (mSubmitButtonEnabled && isSubmitAreaEnabled() && hasFocus()
            && (hasText || !mVoiceButtonEnabled)
        ) {
            visibility = VISIBLE
        }
        mGoButton?.visibility = visibility
    }

    private fun updateSubmitArea() {
        var visibility = GONE
        if (isSubmitAreaEnabled()
            && (mGoButton?.visibility == VISIBLE
                    || mVoiceButton?.visibility == VISIBLE)
        ) {
            visibility = VISIBLE
        }
        mSubmitArea?.visibility = visibility
    }

    private val mUpdateDrawableStateRunnable = Runnable { updateFocusedState() }


    private fun updateCloseButton() {
        val hasText = !TextUtils.isEmpty(mSearchSrcTextView!!.text)
        val showClose = hasText || mIconifiedByDefault && !mExpandedInActionView
        mCloseButton?.visibility = if (showClose) VISIBLE else GONE
        val closeButtonImg: Drawable? = mCloseButton?.drawable
        if (closeButtonImg != null) {
            closeButtonImg.state = if (hasText) ENABLED_STATE_SET else EMPTY_STATE_SET
        }
    }

    private fun postUpdateFocusedState() {
        post(mUpdateDrawableStateRunnable)
    }

    private fun updateFocusedState() {
        Logger.d(
            this@SearchView, message = "updateFocusedState: {" +
                    "mSearchSrcTextView_hasFocus(): ${mSearchSrcTextView!!.hasFocus()}" +
                    "}"
        )
        val focused = mSearchSrcTextView!!.hasFocus()
        val stateSet = if (focused) FOCUSED_STATE_SET else EMPTY_STATE_SET
        val searchPlateBg: Drawable? = mSearchPlate?.background
        if (searchPlateBg != null) {
            searchPlateBg.state = stateSet
        }
        val submitAreaBg: Drawable? = mSubmitArea?.background
        if (submitAreaBg != null) {
            submitAreaBg.state = stateSet
        }
        invalidate()
    }


    private fun hasVoiceSearch(): Boolean {
        if (mSearchable != null && mSearchable!!.voiceSearchEnabled) {
            var testIntent: Intent? = null
            if (mSearchable!!.voiceSearchLaunchWebSearch) {
                testIntent = mVoiceWebSearchIntent
            } else if (mSearchable!!.voiceSearchLaunchRecognizer) {
                testIntent = mVoiceAppSearchIntent
            }
            if (testIntent != null) {
                val ri = context.packageManager.resolveActivity(
                    testIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
                return ri != null
            }
        }
        return false
    }

    fun onVoiceClicked() {
        mSearchable ?: return
        try {
            if (mSearchable!!.voiceSearchLaunchWebSearch) {
                val webSearchIntent: Intent = createVoiceWebSearchIntent(
                    mVoiceWebSearchIntent!!,
                    mSearchable!!
                )
                context.startActivity(webSearchIntent)
            } else if (mSearchable!!.voiceSearchLaunchRecognizer) {
                val appSearchIntent: Intent = createVoiceAppSearchIntent(
                    mVoiceAppSearchIntent!!,
                    mSearchable!!
                )
                context.startActivity(appSearchIntent)
            }
        } catch (e: ActivityNotFoundException) {
            Logger.d(this@SearchView, message = "Could not find voice search activity")
        }
    }

    fun launchQuerySearch(actionKey: Int, actionMsg: String?, query: String?) {
        val action = Intent.ACTION_SEARCH
        val intent = createIntent(action, null, null, query, actionKey, actionMsg)
        context.startActivity(intent)
    }

    fun onSuggestionsKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
        // guard against possible race conditions (late arrival after dismiss)
        if (mSearchable == null) {
            return false
        }
        if (mSuggestionsAdapter == null) {
            return false
        }
        if (event.action == KeyEvent.ACTION_DOWN && event.hasNoModifiers()) {
            // First, check for enter or search (both of which we'll treat as a
            // "click")
            if (keyCode == KeyEvent.KEYCODE_ENTER || (keyCode == KeyEvent.KEYCODE_SEARCH) || keyCode == KeyEvent.KEYCODE_TAB) {
                val position = mSearchSrcTextView!!.listSelection
                return onItemClicked(position, KeyEvent.KEYCODE_UNKNOWN, null)
            }

            // Next, check for left/right moves, which we use to "return" the
            // user to the edit view
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                // give "focus" to text editor, with cursor at the beginning if
                // left key, at end if right key
                // TODO: Reverse left/right for right-to-left languages, e.g.
                // Arabic
                val selPoint = if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    0
                } else {
                    mSearchSrcTextView!!.length()
                }
                mSearchSrcTextView!!.setSelection(selPoint)
                mSearchSrcTextView!!.listSelection = 0
                mSearchSrcTextView!!.clearListSelection()
                mSearchSrcTextView!!.ensureImeVisible()
                return true
            }

            // Next, check for an "up and out" move
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP && 0 == mSearchSrcTextView!!.listSelection) {
                // TODO: restoreUserQuery();
                // let ACTV complete the move
                return false
            }
        }
        return false
    }

    fun onItemClicked(position: Int, actionKey: Int, actionMsg: String?): Boolean {
        if (mOnSuggestionListener == null
            || !mOnSuggestionListener.onSuggestionClick(position)
        ) {
            launchSuggestion(position, KeyEvent.KEYCODE_UNKNOWN, null)
            mSearchSrcTextView!!.setImeVisibility(false)
            dismissSuggestions()
            return true
        }
        return false
    }

    private fun dismissSuggestions() {
        mSearchSrcTextView?.dismissDropDown()
    }

    private fun launchSuggestion(position: Int, actionKey: Int, actionMsg: String?): Boolean {
        val c = mSuggestionsAdapter?.cursor
        if (c != null && c.moveToPosition(position)) {
            val intent: Intent? = createIntentFromSuggestion(c, actionKey, actionMsg)
            // launch the intent
            launchIntent(intent)
            return true
        }
        return false
    }

    override fun focusSearch(focused: View?, direction: Int): View? {
        Logger.d(
            this@SearchView, message = "Focus search: {" +
                    "focused: $focused, " +
                    "direction: $direction" +
                    "}"
        )
//        if (focused == mVoiceButton && direction == View.FOCUS_RIGHT) {
//            return if (mSearchSrcTextView?.isEmpty == true) {
//                mSearchSrcTextView
//            } else {
//                mCloseButton
//            }
//        } else
        if (focused == mSearchSrcTextView
            && (mSearchSrcTextView?.isEmpty == true
                    || mSearchSrcTextView!!.selectionStart == 0)
            && ((direction == View.FOCUS_LEFT) || (direction == View.FOCUS_UP))
        ) {
            return if (direction == View.FOCUS_UP) {
                focused
            } else {
                super.focusSearch(focused, direction)
            }
        } else if (focused == mSearchSrcTextView
            && direction == View.FOCUS_RIGHT
            && (mSearchSrcTextView?.isEmpty == true
                    || mSearchSrcTextView!!.selectionEnd == mSearchSrcTextView?.text?.length)
        ) {
            return mCloseButton
        }
        return super.focusSearch(focused, direction)
    }

    private fun createIntentFromSuggestion(c: Cursor, actionKey: Int, actionMsg: String?): Intent? {
        return try {
            // use specific action if supplied, or default action if supplied, or fixed default
            var action = "" //SuggestionsAdapter.getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_ACTION)
            if (action == null) {
                action = mSearchable!!.suggestIntentAction
            }
            if (action == null) {
                action = Intent.ACTION_SEARCH
            }

            // use specific data if supplied, or default data if supplied
            var data = "" //SuggestionsAdapter.getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_DATA)
            if (data == null) {
                data = mSearchable!!.suggestIntentData
            }
            // then, if an ID was provided, append it.
            if (data != null) {
                val id: String? =
                    null//SuggestionsAdapter.getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID)
                if (id != null) {
                    data = data + "/" + Uri.encode(id)
                }
            }
            val dataUri = if (data == null) null else Uri.parse(data)
            val query = "" //SuggestionsAdapter.getColumnString(c, SearchManager.SUGGEST_COLUMN_QUERY)
            val extraData = "" //SuggestionsAdapter.getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA)
            createIntent(action, dataUri, extraData, query, actionKey, actionMsg)
        } catch (e: RuntimeException) {
            val rowNum: Int = try {
                c.position
            } catch (e2: RuntimeException) {
                -1
            }
            Logger.e(this, exception = e)
            null
        }
    }

    private fun launchIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        try {
            // If the intent was created from a suggestion, it will always have an explicit
            // component here.
            context.startActivity(intent)
        } catch (ex: java.lang.RuntimeException) {
            Logger.d(this@SearchView, message = "Failed launch activity: $intent")
            Logger.e(this@SearchView, exception = ex)
        }
    }

    /**
     * Constructs an intent from the given information and the search dialog state.
     *
     * @param action Intent action.
     * @param data Intent data, or `null`.
     * @param extraData Data for [SearchManager.EXTRA_DATA_KEY] or `null`.
     * @param query Intent query, or `null`.
     * @param actionKey The key code of the action key that was pressed,
     * or [KeyEvent.KEYCODE_UNKNOWN] if none.
     * @param actionMsg The message for the action key that was pressed,
     * or `null` if none.
     * @return The intent.
     */
    private fun createIntent(
        action: String,
        data: Uri?,
        extraData: String?,
        query: String?,
        actionKey: Int,
        actionMsg: String?
    ): Intent {
        // Now build the Intent
        val intent = Intent(action)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // We need CLEAR_TOP to avoid reusing an old task that has other activities
        // on top of the one we want. We don't want to do this in in-app search though,
        // as it can be destructive to the activity stack.
        if (data != null) {
            intent.data = data
        }
        intent.putExtra(SearchManager.USER_QUERY, mUserQuery)
        if (query != null) {
            intent.putExtra(SearchManager.QUERY, query)
        }
        if (extraData != null) {
            intent.putExtra(SearchManager.EXTRA_DATA_KEY, extraData)
        }
        if (mAppSearchData != null) {
            intent.putExtra(SearchManager.APP_DATA, mAppSearchData)
        }
        if (actionKey != KeyEvent.KEYCODE_UNKNOWN) {
            intent.putExtra(SearchManager.ACTION_KEY, actionKey)
            intent.putExtra(SearchManager.ACTION_MSG, actionMsg)
        }
        intent.component = mSearchable!!.searchActivity
        return intent
    }

    /**
     * Create and return an Intent that can launch the voice search activity for web search.
     */
    private fun createVoiceWebSearchIntent(baseIntent: Intent, searchable: SearchableInfo): Intent {
        val voiceIntent = Intent(baseIntent)
        val searchActivity = searchable.searchActivity
        voiceIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, searchActivity?.flattenToShortString())
        return voiceIntent
    }

    /**
     * Create and return an Intent that can launch the voice search activity, perform a specific
     * voice transcription, and forward the results to the searchable activity.
     *
     * @param baseIntent The voice app search intent to start from
     * @return A completely-configured intent ready to send to the voice search activity
     */
    private fun createVoiceAppSearchIntent(baseIntent: Intent, searchable: SearchableInfo): Intent {
        val searchActivity = searchable.searchActivity
        val queryIntent = Intent(Intent.ACTION_SEARCH)
        queryIntent.component = searchActivity
        val pending = PendingIntent.getActivity(
            context, 0, queryIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE
        )

        // Now set up the bundle that will be inserted into the pending intent
        // when it's time to do the search.  We always build it here (even if empty)
        // because the voice search activity will always need to insert "QUERY" into
        // it anyway.
        val queryExtras = Bundle()
        if (mAppSearchData != null) {
            queryExtras.putParcelable(SearchManager.APP_DATA, mAppSearchData)
        }

        // Now build the intent to launch the voice search.  Add all necessary
        // extras to launch the voice recognizer, and then all the necessary extras
        // to forward the results to the searchable activity
        val voiceIntent = Intent(baseIntent)

        // Add all of the configuration options supplied by the searchable's metadata
        var languageModel = RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        var prompt: String? = null
        var language: String? = null
        var maxResults = 1
        val resources = resources
        if (searchable.voiceLanguageModeId != 0) {
            languageModel = resources.getString(searchable.voiceLanguageModeId)
        }
        if (searchable.voicePromptTextId != 0) {
            prompt = resources.getString(searchable.voicePromptTextId)
        }
        if (searchable.voiceLanguageId != 0) {
            language = resources.getString(searchable.voiceLanguageId)
        }
        if (searchable.voiceMaxResults != 0) {
            maxResults = searchable.voiceMaxResults
        }
        voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel)
        voiceIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
        voiceIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults)
        voiceIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, searchActivity?.flattenToShortString())
        // Add the values that configure forwarding the results
        voiceIntent.putExtra(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT, pending)
        voiceIntent.putExtra(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT_BUNDLE, queryExtras)
        return voiceIntent
    }

    override fun onClick(v: View) {
        if (v === mSearchButton) {
            onSearchClicked()
        } else if (v === mCloseButton) {
            onCloseClicked()
        } else if (v === mGoButton) {
            onSubmitQuery()
        } else if (v === mVoiceButton) {
            onVoiceClicked()
        } else if (v === mSearchSrcTextView) {
            forceSuggestionQuery()
        }
    }


    private fun onCloseClicked() {
        val text: CharSequence = mSearchSrcTextView!!.text
        if (TextUtils.isEmpty(text)) {
            if (mIconifiedByDefault) {
                // If the app doesn't override the close behavior
                if (mOnCloseListener == null || !mOnCloseListener.onClose()) {
                    // hide the keyboard and remove focus
                    clearFocus()
                    // collapse the search field
                    updateViewsVisibility(true)
                }
            }
        } else {
            mSearchSrcTextView!!.setText("")
            mSearchSrcTextView!!.requestFocus()
            mSearchSrcTextView!!.setImeVisibility(true)
        }
    }

    private fun onSearchClicked() {
        updateViewsVisibility(false)
        mSearchSrcTextView!!.requestFocus()
        mSearchSrcTextView?.setImeVisibility(true)
        mSearchButton?.performClick()
    }


    private fun onSubmitQuery() {
        val query: CharSequence? = mSearchSrcTextView!!.text
        if (query != null && TextUtils.getTrimmedLength(query) > 0) {
            if (mOnQueryChangeListener == null
                || !mOnQueryChangeListener!!.onQueryTextSubmit(query.toString())
            ) {
                if (mSearchable != null) {
                    launchQuerySearch(KeyEvent.KEYCODE_UNKNOWN, null, query.toString())
                }
                mSearchSrcTextView!!.setImeVisibility(false)
                mSearchSrcTextView!!.dismissDropDown()
            }
        }
    }

    fun forceSuggestionQuery() {
        if (Build.VERSION.SDK_INT >= 29) {
            mSearchSrcTextView?.refreshAutoCompleteResults()
        } else {
            PRE_API_29_HIDDEN_METHOD_INVOKER.doBeforeTextChanged(mSearchSrcTextView)
            PRE_API_29_HIDDEN_METHOD_INVOKER.doAfterTextChanged(mSearchSrcTextView)
        }
    }

    open class SearchAutoComplete @JvmOverloads constructor(
        context: Context?,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
    ) : AppCompatAutoCompleteTextView(context!!, attrs, defStyle) {
        private var mThreshold: Int
        private var mSearchView: SearchView? = null
        private var mHasPendingShowSoftInputRequest = false
        private val mRunShowSoftInputIfNecessary = Runnable { showSoftInputIfNecessary() }

        init {
            mThreshold = threshold
        }

        override fun onFinishInflate() {
            super.onFinishInflate()
            val metrics = resources.displayMetrics
            minWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                searchViewTextMinWidthDp.toFloat(), metrics
            ).toInt()
        }

        fun setSearchView(searchView: SearchView?) {
            mSearchView = searchView
        }

        override fun setThreshold(threshold: Int) {
            super.setThreshold(threshold)
            mThreshold = threshold
        }

        /**
         * Returns true if the text field is empty, or contains only whitespace.
         */
        val isEmpty: Boolean
            get() = TextUtils.getTrimmedLength(text) == 0

        /**
         * We override this method to avoid replacing the query box text when a
         * suggestion is clicked.
         */
        override fun replaceText(text: CharSequence) {
            Logger.d(this@SearchAutoComplete, message = "replaceText")
        }

        /**
         * We override this method to avoid an extra onItemClick being called on
         * the drop-down's OnItemClickListener by
         * [AutoCompleteTextView.onKeyUp] when an item is
         * clicked with the trackball.
         */
        override fun performCompletion() {
            Logger.d(this@SearchAutoComplete, message = "performCompletion")
        }

        /**
         * We override this method to be sure and show the soft keyboard if
         * appropriate when the TextView has focus.
         */
        override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
            super.onWindowFocusChanged(hasWindowFocus)
            Logger.d(this@SearchAutoComplete, message = "onWindowFocusChanged")
            if (hasWindowFocus && mSearchView!!.hasFocus() && (visibility == View.VISIBLE)) {
                // Since InputMethodManager#onPostWindowFocus() will be called after this callback,
                // it is a bit too early to call InputMethodManager#showSoftInput() here. We still
                // need to wait until the system calls back onCreateInputConnection() to call
                // InputMethodManager#showSoftInput().
                mHasPendingShowSoftInputRequest = true

                // If in landscape mode, then make sure that the ime is in front of the dropdown.
                if (isLandscapeMode(context)) {
                    ensureImeVisible()
                }
            }
        }

        override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect)
            Logger.d(this@SearchAutoComplete, message = "auto complete text view onFocusChanged")
            mSearchView!!.onTextFocusChanged()
        }

        /**
         * We override this method so that we can allow a threshold of zero,
         * which ACTV does not.
         */
        override fun enoughToFilter(): Boolean {
            return mThreshold <= 0 || super.enoughToFilter()
        }

        override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
            Logger.d(this@SearchAutoComplete, message = "On Key PreIme: $keyCode")
//            if (keyCode == KeyEvent.KEYCODE_BACK) {
//                // special case for the back key, we do not even try to send it
//                // to the drop down list but instead, consume it immediately
//                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
//                    val state = keyDispatcherState
//                    state?.startTracking(event, this)
//                    return true
//                } else if (event.action == KeyEvent.ACTION_UP) {
//                    val state = keyDispatcherState
//                    state?.handleUpEvent(event)
//                    if (event.isTracking && !event.isCanceled) {
//                        mSearchView?.mVoiceButton?.requestFocus()
//                        setImeVisibility(false)
//                        return true
//                    }
//                }
//            }
            return super.onKeyPreIme(keyCode, event)
        }

        /**
         * Get minimum width of the search view text entry area.
         */
        private val searchViewTextMinWidthDp: Int
            get() {
                val config = resources.configuration
                val widthDp = config.screenWidthDp
                val heightDp = config.screenHeightDp
                if (widthDp >= 960 && heightDp >= 720 && config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    return 256
                } else if (widthDp >= 600 || widthDp >= 640 && heightDp >= 480) {
                    return 192
                }
                return 160
            }

        /**
         * We override [View.onCreateInputConnection] as a signal to schedule a
         * pending [InputMethodManager.showSoftInput] request (if any).
         */
        override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection {
            val ic = super.onCreateInputConnection(editorInfo)
            if (mHasPendingShowSoftInputRequest) {
                removeCallbacks(mRunShowSoftInputIfNecessary)
                post(mRunShowSoftInputIfNecessary)
            }
            return ic
        }

        fun showSoftInputIfNecessary() {
            if (mHasPendingShowSoftInputRequest) {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(this, 0)
                mHasPendingShowSoftInputRequest = false
            }
        }

        fun setImeVisibility(visible: Boolean) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (!visible) {
                mHasPendingShowSoftInputRequest = false
                removeCallbacks(mRunShowSoftInputIfNecessary)
                imm.hideSoftInputFromWindow(windowToken, 0)
                return
            }
            if (imm.isActive(this)) {
                mHasPendingShowSoftInputRequest = false
                removeCallbacks(mRunShowSoftInputIfNecessary)
                imm.showSoftInput(this, 0)
                return
            }
            mHasPendingShowSoftInputRequest = true
        }

        fun ensureImeVisible() {
            if (Build.VERSION.SDK_INT >= 29) {
                inputMethodMode = ListPopupWindow.INPUT_METHOD_NEEDED
                if (enoughToFilter()) {
                    showDropDown()
                }
            } else {
                PRE_API_29_HIDDEN_METHOD_INVOKER.ensureImeVisible(this)
            }
        }
    }

    @SuppressLint(
        "DiscouragedPrivateApi",
        "SoonBlockedPrivateApi"
    )
    private class PreQAutoCompleteTextViewReflector constructor() {
        private var mDoBeforeTextChanged: Method? = null
        private var mDoAfterTextChanged: Method? = null
        private var mEnsureImeVisible: Method? = null

        init {
            preApi29Check()
            try {
                mDoBeforeTextChanged = AutoCompleteTextView::class.java
                    .getDeclaredMethod("doBeforeTextChanged")
                mDoBeforeTextChanged?.isAccessible = true
            } catch (e: NoSuchMethodException) {
                // Ah well.
            }
            try {
                mDoAfterTextChanged = AutoCompleteTextView::class.java
                    .getDeclaredMethod("doAfterTextChanged")
                mDoAfterTextChanged?.isAccessible = true
            } catch (e: NoSuchMethodException) {
                // Ah well.
            }
            try {
                mEnsureImeVisible = AutoCompleteTextView::class.java
                    .getMethod("ensureImeVisible", Boolean::class.javaPrimitiveType)
                mEnsureImeVisible?.isAccessible = true
            } catch (e: NoSuchMethodException) {
                // Ah well.
            }
        }

        fun doBeforeTextChanged(view: AutoCompleteTextView?) {
            preApi29Check()
            if (mDoBeforeTextChanged != null) {
                try {
                    mDoBeforeTextChanged?.invoke(view)
                } catch (_: java.lang.Exception) {
                }
            }
        }

        fun doAfterTextChanged(view: AutoCompleteTextView?) {
            preApi29Check()
            if (mDoAfterTextChanged != null) {
                try {
                    mDoAfterTextChanged?.invoke(view)
                } catch (_: java.lang.Exception) {
                }
            }
        }

        fun ensureImeVisible(view: AutoCompleteTextView?) {
            preApi29Check()
            if (mEnsureImeVisible != null) {
                try {
                    mEnsureImeVisible?.invoke(view,  /* visible = */true)
                } catch (_: java.lang.Exception) {
                }
            }
        }

        companion object {
            fun preApi29Check() {
                if (Build.VERSION.SDK_INT >= 29) {
                    throw UnsupportedClassVersionError(
                        "This function can only be used for API Level < 29."
                    )
                }
            }
        }
    }


    fun onTextFocusChanged() {
        updateViewsVisibility(isIconified())
        postUpdateFocusedState()
        if (mSearchSrcTextView!!.hasFocus()) {
            forceSuggestionQuery()
        }
    }

    fun showKeyboard() {
        mSearchSrcTextView?.ensureImeVisible()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(mSearchSrcTextView, 0)
    }

    interface OnSuggestionListener {
        fun onSuggestionSelect(position: Int): Boolean
        fun onSuggestionClick(position: Int): Boolean
    }


    companion object {
        private val PRE_API_29_HIDDEN_METHOD_INVOKER by lazy {
            PreQAutoCompleteTextViewReflector()
        }
        private const val TAG = "SearchView"
        private const val IME_OPTION_NO_MICROPHONE = "nm"
        fun isLandscapeMode(context: Context): Boolean {
            return (context.resources.configuration.orientation
                    == Configuration.ORIENTATION_LANDSCAPE)
        }
    }


}