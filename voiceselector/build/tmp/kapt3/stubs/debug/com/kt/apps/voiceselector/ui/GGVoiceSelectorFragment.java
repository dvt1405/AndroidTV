package com.kt.apps.voiceselector.ui;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000^\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u0000 .2\b\u0012\u0004\u0012\u00020\u00020\u00012\u00020\u0003:\u0001.B\u0005\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\"H\u0016J\b\u0010#\u001a\u00020\u0013H\u0016J\u0012\u0010$\u001a\u00020%2\b\u0010&\u001a\u0004\u0018\u00010\'H\u0016J\u0012\u0010(\u001a\u00020%2\b\u0010&\u001a\u0004\u0018\u00010\'H\u0016J\u0010\u0010)\u001a\u00020%2\u0006\u0010*\u001a\u00020+H\u0016J\b\u0010,\u001a\u00020%H\u0016J\b\u0010-\u001a\u00020%H\u0002R$\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u00068\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\b\u0010\t\"\u0004\b\n\u0010\u000bR\u001e\u0010\f\u001a\u00020\r8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000e\u0010\u000f\"\u0004\b\u0010\u0010\u0011R\u0014\u0010\u0012\u001a\u00020\u0013X\u0094\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u001e\u0010\u0016\u001a\u00020\u00178\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0018\u0010\u0019\"\u0004\b\u001a\u0010\u001bR\u001e\u0010\u001c\u001a\u00020\u001d8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u001e\u0010\u001f\"\u0004\b \u0010!\u00a8\u0006/"}, d2 = {"Lcom/kt/apps/voiceselector/ui/GGVoiceSelectorFragment;", "Lcom/kt/apps/core/base/BaseBottomSheetDialogFragment;", "Lcom/kt/apps/voiceselector/databinding/FragmentGgVoiceSelectorBinding;", "Ldagger/android/HasAndroidInjector;", "()V", "androidInjector", "Ldagger/android/DispatchingAndroidInjector;", "", "getAndroidInjector", "()Ldagger/android/DispatchingAndroidInjector;", "setAndroidInjector", "(Ldagger/android/DispatchingAndroidInjector;)V", "logger", "Lcom/kt/apps/core/logging/IActionLogger;", "getLogger", "()Lcom/kt/apps/core/logging/IActionLogger;", "setLogger", "(Lcom/kt/apps/core/logging/IActionLogger;)V", "resLayout", "", "getResLayout", "()I", "voicePackage", "Lcom/kt/apps/voiceselector/models/VoicePackage;", "getVoicePackage", "()Lcom/kt/apps/voiceselector/models/VoicePackage;", "setVoicePackage", "(Lcom/kt/apps/voiceselector/models/VoicePackage;)V", "voiceSelectorManager", "Lcom/kt/apps/voiceselector/VoiceSelectorManager;", "getVoiceSelectorManager", "()Lcom/kt/apps/voiceselector/VoiceSelectorManager;", "setVoiceSelectorManager", "(Lcom/kt/apps/voiceselector/VoiceSelectorManager;)V", "Ldagger/android/AndroidInjector;", "getTheme", "initAction", "", "savedInstanceState", "Landroid/os/Bundle;", "initView", "onAttach", "context", "Landroid/content/Context;", "onStart", "useGGAssistant", "Companion", "voiceselector_debug"})
public final class GGVoiceSelectorFragment extends com.kt.apps.core.base.BaseBottomSheetDialogFragment<com.kt.apps.voiceselector.databinding.FragmentGgVoiceSelectorBinding> implements dagger.android.HasAndroidInjector {
    @javax.inject.Inject
    public dagger.android.DispatchingAndroidInjector<java.lang.Object> androidInjector;
    @javax.inject.Inject
    public com.kt.apps.voiceselector.models.VoicePackage voicePackage;
    @javax.inject.Inject
    public com.kt.apps.voiceselector.VoiceSelectorManager voiceSelectorManager;
    @javax.inject.Inject
    public com.kt.apps.core.logging.IActionLogger logger;
    private final int resLayout = 0;
    @org.jetbrains.annotations.NotNull
    public static final com.kt.apps.voiceselector.ui.GGVoiceSelectorFragment.Companion Companion = null;
    
    public GGVoiceSelectorFragment() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final dagger.android.DispatchingAndroidInjector<java.lang.Object> getAndroidInjector() {
        return null;
    }
    
    public final void setAndroidInjector(@org.jetbrains.annotations.NotNull
    dagger.android.DispatchingAndroidInjector<java.lang.Object> p0) {
    }
    
    @org.jetbrains.annotations.NotNull
    public final com.kt.apps.voiceselector.models.VoicePackage getVoicePackage() {
        return null;
    }
    
    public final void setVoicePackage(@org.jetbrains.annotations.NotNull
    com.kt.apps.voiceselector.models.VoicePackage p0) {
    }
    
    @org.jetbrains.annotations.NotNull
    public final com.kt.apps.voiceselector.VoiceSelectorManager getVoiceSelectorManager() {
        return null;
    }
    
    public final void setVoiceSelectorManager(@org.jetbrains.annotations.NotNull
    com.kt.apps.voiceselector.VoiceSelectorManager p0) {
    }
    
    @org.jetbrains.annotations.NotNull
    public final com.kt.apps.core.logging.IActionLogger getLogger() {
        return null;
    }
    
    public final void setLogger(@org.jetbrains.annotations.NotNull
    com.kt.apps.core.logging.IActionLogger p0) {
    }
    
    @java.lang.Override
    protected int getResLayout() {
        return 0;
    }
    
    @java.lang.Override
    public int getTheme() {
        return 0;
    }
    
    @java.lang.Override
    public void initView(@org.jetbrains.annotations.Nullable
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override
    public void initAction(@org.jetbrains.annotations.Nullable
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override
    public void onStart() {
    }
    
    private final void useGGAssistant() {
    }
    
    @java.lang.Override
    public void onAttach(@org.jetbrains.annotations.NotNull
    android.content.Context context) {
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.NotNull
    public dagger.android.AndroidInjector<java.lang.Object> androidInjector() {
        return null;
    }
    
    @kotlin.jvm.JvmStatic
    @org.jetbrains.annotations.NotNull
    public static final com.kt.apps.voiceselector.ui.GGVoiceSelectorFragment newInstance() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H\u0007\u00a8\u0006\u0005"}, d2 = {"Lcom/kt/apps/voiceselector/ui/GGVoiceSelectorFragment$Companion;", "", "()V", "newInstance", "Lcom/kt/apps/voiceselector/ui/GGVoiceSelectorFragment;", "voiceselector_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @kotlin.jvm.JvmStatic
        @org.jetbrains.annotations.NotNull
        public final com.kt.apps.voiceselector.ui.GGVoiceSelectorFragment newInstance() {
            return null;
        }
    }
}