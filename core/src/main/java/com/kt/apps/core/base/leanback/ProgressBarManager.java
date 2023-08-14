package com.kt.apps.core.base.leanback;

import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

public final class ProgressBarManager {
    // Default delay for progress bar widget.
    private static final long DEFAULT_PROGRESS_BAR_DELAY = 1000;

    private long mInitialDelay = DEFAULT_PROGRESS_BAR_DELAY;
    private OnProgressShowHideListener onProgressShowHideListener;
    ViewGroup rootView;
    View mProgressBarView;
    private Handler mHandler = new Handler();
    boolean mEnableProgressBar = true;
    boolean mUserProvidedProgressBar;
    boolean mIsShowing;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (!mEnableProgressBar || (!mUserProvidedProgressBar && rootView == null)) {
                return;
            }

            if (mIsShowing) {
                if (mProgressBarView == null) {
                    mProgressBarView = new ProgressBar(
                            rootView.getContext(), null, android.R.attr.progressBarStyleLarge);
                    FrameLayout.LayoutParams progressBarParams = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT);
                    progressBarParams.gravity = Gravity.CENTER;
                    rootView.addView(mProgressBarView, progressBarParams);
                } else if (mUserProvidedProgressBar) {
                    mProgressBarView.setVisibility(View.VISIBLE);
                }
            }
        }
    };

    /**
     * Sets the root view on which the progress bar will be attached. This class assumes the
     * root view to be {@link FrameLayout} in order to position the progress bar widget
     * in the center of the screen.
     *
     * @param rootView view that will contain the progress bar.
     */
    public void setRootView(ViewGroup rootView) {
        this.rootView = rootView;
    }

    /**
     * Displays the progress bar.
     */
    public void show() {
        if (mEnableProgressBar) {
            mIsShowing = true;
            mHandler.postDelayed(runnable, mInitialDelay);
            if (onProgressShowHideListener != null) {
                onProgressShowHideListener.onShowOrHide(true);
            }
        }
    }

    public interface OnProgressShowHideListener {
        void onShowOrHide(boolean show);
    }

    /**
     * Hides the progress bar.
     */
    public void hide() {
        mIsShowing = false;
        if (mUserProvidedProgressBar) {
            mProgressBarView.setVisibility(View.INVISIBLE);
        } else if (mProgressBarView != null) {
            rootView.removeView(mProgressBarView);
            mProgressBarView = null;
        }

        mHandler.removeCallbacks(runnable);
        if (onProgressShowHideListener != null) {
            onProgressShowHideListener.onShowOrHide(false);
        }
    }

    public boolean isShowing() {
        return mIsShowing;
    }

    public void setProgressBarView(View progressBarView) {
        if (progressBarView != null && progressBarView.getParent() == null) {
            throw new IllegalArgumentException("Must have a parent");
        }

        this.mProgressBarView = progressBarView;
        if (this.mProgressBarView != null) {
            this.mProgressBarView.setVisibility(View.INVISIBLE);
            mUserProvidedProgressBar = true;
        }
    }

    public long getInitialDelay() {
        return mInitialDelay;
    }

    public void setInitialDelay(long initialDelay) {
        this.mInitialDelay = initialDelay;
    }

    public void disableProgressBar() {
        mEnableProgressBar = false;
    }

    public void enableProgressBar() {
        mEnableProgressBar = true;
    }

    public void setOnProgressShowHideListener(OnProgressShowHideListener onProgressShowHideListener) {
        this.onProgressShowHideListener = onProgressShowHideListener;
    }
}
