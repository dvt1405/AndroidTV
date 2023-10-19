package com.kt.apps.voiceselector.ui;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0003J\u0012\u0010\u001b\u001a\u00020\u001c2\b\u0010\u001d\u001a\u0004\u0018\u00010\u001eH\u0016J\u0012\u0010\u001f\u001a\u00020\u001c2\b\u0010\u001d\u001a\u0004\u0018\u00010\u001eH\u0016J\b\u0010 \u001a\u00020\u001cH\u0014R\u001c\u0010\u0004\u001a\u0010\u0012\f\u0012\n \u0007*\u0004\u0018\u00010\u00060\u00060\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\b\u001a\u00020\tX\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0014\u0010\f\u001a\u00020\r8VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b\u000e\u0010\u000fR\u001b\u0010\u0010\u001a\u00020\u00068BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0013\u0010\u0014\u001a\u0004\b\u0011\u0010\u0012R\u001e\u0010\u0015\u001a\u00020\u00168\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0017\u0010\u0018\"\u0004\b\u0019\u0010\u001a\u00a8\u0006!"}, d2 = {"Lcom/kt/apps/voiceselector/ui/VoiceSearchActivity;", "Lcom/kt/apps/core/base/BaseActivity;", "Lcom/kt/apps/voiceselector/databinding/ActivityVoiceSearchBinding;", "()V", "launchForResult", "Landroidx/activity/result/ActivityResultLauncher;", "Landroid/content/Intent;", "kotlin.jvm.PlatformType", "layoutRes", "", "getLayoutRes", "()I", "shouldShowUpdateNotification", "", "getShouldShowUpdateNotification", "()Z", "voiceAppSearchIntent", "getVoiceAppSearchIntent", "()Landroid/content/Intent;", "voiceAppSearchIntent$delegate", "Lkotlin/Lazy;", "voiceSelectorManager", "Lcom/kt/apps/voiceselector/VoiceSelectorManager;", "getVoiceSelectorManager", "()Lcom/kt/apps/voiceselector/VoiceSelectorManager;", "setVoiceSelectorManager", "(Lcom/kt/apps/voiceselector/VoiceSelectorManager;)V", "initAction", "", "savedInstanceState", "Landroid/os/Bundle;", "initView", "onPause", "voiceselector_release"})
public final class VoiceSearchActivity extends com.kt.apps.core.base.BaseActivity<com.kt.apps.voiceselector.databinding.ActivityVoiceSearchBinding> {
    @javax.inject.Inject
    public com.kt.apps.voiceselector.VoiceSelectorManager voiceSelectorManager;
    private final int layoutRes = 0;
    @org.jetbrains.annotations.NotNull
    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> launchForResult = null;
    @org.jetbrains.annotations.NotNull
    private final kotlin.Lazy voiceAppSearchIntent$delegate = null;
    
    public VoiceSearchActivity() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final com.kt.apps.voiceselector.VoiceSelectorManager getVoiceSelectorManager() {
        return null;
    }
    
    public final void setVoiceSelectorManager(@org.jetbrains.annotations.NotNull
    com.kt.apps.voiceselector.VoiceSelectorManager p0) {
    }
    
    @java.lang.Override
    public int getLayoutRes() {
        return 0;
    }
    
    @java.lang.Override
    public boolean getShouldShowUpdateNotification() {
        return false;
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
    protected void onPause() {
    }
    
    private final android.content.Intent getVoiceAppSearchIntent() {
        return null;
    }
}