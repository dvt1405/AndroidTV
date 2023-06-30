/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package com.kt.apps.core.base.leanback;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import androidx.annotation.RestrictTo;

/**
 * Helper for view backgrounds.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public final class BackgroundHelper {
    public static void setBackgroundPreservingAlpha(View view, Drawable drawable) {
        if (Build.VERSION.SDK_INT >= 19) {
            if (view.getBackground() != null) {
                drawable.setAlpha(view.getBackground().getAlpha());
            }
            view.setBackground(drawable);
        } else {
            // Cannot query drawable alpha
            view.setBackground(drawable);
        }
    }

    private BackgroundHelper() {
    }
}
