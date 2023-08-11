package com.kt.apps.core.base

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.leanback.transition.TransitionHelper
import com.kt.apps.core.base.leanback.*
import com.kt.apps.core.R
import com.kt.apps.core.base.leanback.BrowseSupportFragment
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.databinding.DefaultGridFragmentBinding
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

abstract class BaseGridViewFragment<T : ViewDataBinding> : Fragment(),
    BrowseSupportFragment.MainFragmentAdapterProvider, HasAndroidInjector {
    @Inject
    lateinit var injector: DispatchingAndroidInjector<Any>

    abstract val layoutRes: Int
    protected var mAdapter: ObjectAdapter? = null
    abstract fun onCreatePresenter(): VerticalGridPresenter
    abstract fun onItemViewSelectedListener(): OnItemViewSelectedListener
    abstract fun onItemViewClickedListener(): OnItemViewClickedListener
    protected lateinit var binding: T
    protected var mGridViewHolder: VerticalGridPresenter.ViewHolder? = null
    protected var mPresenter: VerticalGridPresenter? = null
    protected var mOnItemViewSelectedListener: OnItemViewSelectedListener? = null
    protected var mOnItemViewClickedListener: OnItemViewClickedListener? = null
    protected var mSceneAfterEntranceTransition: Any? = null
    protected var mSelectedPosition = -1
    protected val progressManager by lazy {
        ProgressBarManager()
    }
    override fun androidInjector(): AndroidInjector<Any> {
        return injector
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    protected val mMainFragmentAdapter by lazy {
        object : BrowseSupportFragment.MainFragmentAdapter<Fragment>(this@BaseGridViewFragment) {
            override fun setEntranceTransitionState(state: Boolean) {
                super.setEntranceTransitionState(state)
            }
        }
    }

    protected val mViewSelectedListener by lazy {
        OnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            val position: Int = mGridViewHolder?.gridView?.selectedPosition ?: -1
            Logger.d(this@BaseGridViewFragment, message = "Selected position: $position")
            gridOnItemSelected(position)
            mOnItemViewSelectedListener?.onItemSelected(
                itemViewHolder, item,
                rowViewHolder, row
            )
        }
    }

    private val mChildLaidOutListener by lazy {
        OnChildLaidOutListener { _, _, position, _ ->
            if (position == 0) {
                showOrHideTitle()
            }
        }
    }

    abstract fun onCreateAdapter()
    abstract fun initView(rootView: View)
    abstract fun initAction(rootView: View)


    protected fun gridOnItemSelected(position: Int) {
        if (position != mSelectedPosition) {
            mSelectedPosition = position
            showOrHideTitle()
        }
    }

    private fun showOrHideTitle() {
        mGridViewHolder?.gridView
            ?.findViewHolderForAdapterPosition(mSelectedPosition)
            ?: return
        if (!mGridViewHolder!!.gridView.hasPreviousViewInSameRow(mSelectedPosition)) {
            mMainFragmentAdapter.fragmentHost.showTitleView(true)
        } else {
            mMainFragmentAdapter.fragmentHost.showTitleView(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPresenter = onCreatePresenter()
        mOnItemViewSelectedListener = onItemViewSelectedListener()
        mOnItemViewClickedListener = onItemViewClickedListener()
        mPresenter?.onItemViewClickedListener = mOnItemViewClickedListener
        mPresenter?.onItemViewSelectedListener = mViewSelectedListener
        mAdapter = ArrayObjectAdapter(mPresenter)
        updateAdapter()
        onCreateAdapter()
        if (savedInstanceState == null) {
            try {
                mainFragmentAdapter.fragmentHost.notifyDataReady(mMainFragmentAdapter)
            } catch (_: NullPointerException) { }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            layoutRes,
            container,
            false
        )
        initView(binding.root)
        progressManager.initialDelay = 500
        progressManager.setRootView(binding.root as ViewGroup?)
        return binding.root
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val gridDock: ViewGroup = view.findViewById(R.id.browse_grid_dock)
        mGridViewHolder = mPresenter!!.onCreateViewHolder(gridDock)
        gridDock.addView(mGridViewHolder!!.view)
        mGridViewHolder!!.gridView.setOnChildLaidOutListener(mChildLaidOutListener)
        mSceneAfterEntranceTransition = TransitionHelper.createScene(
            gridDock
        ) { setEntranceTransitionState(true) }
        mainFragmentAdapter.fragmentHost.notifyViewCreated(mMainFragmentAdapter)
        updateAdapter()
        initAction(view)
    }

    protected fun updateAdapter() {
        if (mGridViewHolder != null) {
            mPresenter!!.onBindViewHolder(mGridViewHolder, mAdapter)
            if (mSelectedPosition != -1) {
                mGridViewHolder!!.gridView.selectedPosition = mSelectedPosition
            }
        }
    }

    open fun setEntranceTransitionState(afterTransition: Boolean) {
        mPresenter!!.setEntranceTransitionState(mGridViewHolder, afterTransition)
    }


    override fun getMainFragmentAdapter(): BrowseSupportFragment.MainFragmentAdapter<*> {
        return mMainFragmentAdapter
    }

    class DefaultGridFragment : BaseGridViewFragment<DefaultGridFragmentBinding>() {
        override val layoutRes: Int
            get() = R.layout.default_grid_fragment

        override fun onCreatePresenter(): VerticalGridPresenter {
            return VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM).apply {
                this.numberOfColumns = 5
            }
        }

        override fun onItemViewSelectedListener(): OnItemViewSelectedListener {
            return OnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
                mSelectedPosition = mGridViewHolder!!.gridView.selectedPosition
            }
        }

        override fun onItemViewClickedListener(): OnItemViewClickedListener {
            return OnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
                mSelectedPosition = mGridViewHolder!!.gridView.selectedPosition
            }
        }

        override fun onCreateAdapter() {
            val cardPresenter = ClassPresenterSelector()
            mAdapter = ArrayObjectAdapter(cardPresenter)
            updateAdapter()
        }

        override fun initView(rootView: View) {
        }

        override fun initAction(rootView: View) {
        }
    }


}