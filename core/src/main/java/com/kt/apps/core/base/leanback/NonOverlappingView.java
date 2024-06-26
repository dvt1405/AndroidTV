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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class NonOverlappingView extends View {
    public NonOverlappingView(Context context) {
        this(context, null);
    }

    public NonOverlappingView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public NonOverlappingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Avoids creating a hardware layer when Transition is animating alpha.
     */
    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
