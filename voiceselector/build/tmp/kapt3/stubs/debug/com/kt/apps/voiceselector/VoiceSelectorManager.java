package com.kt.apps.voiceselector;

@com.kt.apps.voiceselector.di.VoiceSelectorScope
@kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000|\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u0007\u0018\u0000 42\u00020\u0001:\u00014B/\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u00a2\u0006\u0002\u0010\fJ\u000e\u0010\u001b\u001a\u00020\u001c2\u0006\u0010\u001d\u001a\u00020\u000fJ\u0012\u0010\u001e\u001a\u00020\u001c2\b\u0010\u001f\u001a\u0004\u0018\u00010 H\u0002J\u0017\u0010!\u001a\b\u0012\u0004\u0012\u00020\u000f0\"H\u0086@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010#J\u0006\u0010$\u001a\u00020\u001cJ\u0016\u0010%\u001a\b\u0012\u0004\u0012\u00020\'0&2\b\b\u0002\u0010(\u001a\u00020)J\b\u0010*\u001a\u00020\u001cH\u0002J\u0010\u0010+\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010 0&H\u0002J\u0006\u0010,\u001a\u00020\u001cJ\u001a\u0010-\u001a\u00020.2\u0012\u0010/\u001a\u000e\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u00020\u001c00J\b\u00101\u001a\u00020\'H\u0002J\u0006\u00102\u001a\u00020\u001cJ\u0006\u00103\u001a\u00020\u001cR\u0014\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0011X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00140\u0013X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R$\u0010\u0016\u001a\u00020\u00112\u0006\u0010\u0015\u001a\u00020\u00118F@FX\u0086\u000e\u00a2\u0006\f\u001a\u0004\b\u0017\u0010\u0018\"\u0004\b\u0019\u0010\u001aR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u00065"}, d2 = {"Lcom/kt/apps/voiceselector/VoiceSelectorManager;", "", "interactor", "Lcom/kt/apps/voiceselector/VoiceSelectorInteractor;", "voicePackage", "Lcom/kt/apps/voiceselector/models/VoicePackage;", "app", "Lcom/kt/apps/core/base/CoreApp;", "sharedPreferences", "Landroid/content/SharedPreferences;", "logger", "Lcom/kt/apps/core/logging/FirebaseActionLoggerImpl;", "(Lcom/kt/apps/voiceselector/VoiceSelectorInteractor;Lcom/kt/apps/voiceselector/models/VoicePackage;Lcom/kt/apps/core/base/CoreApp;Landroid/content/SharedPreferences;Lcom/kt/apps/core/logging/FirebaseActionLoggerImpl;)V", "_event", "Lio/reactivex/rxjava3/subjects/PublishSubject;", "Lcom/kt/apps/voiceselector/models/Event;", "_state", "Lcom/kt/apps/voiceselector/models/State;", "lastActivity", "Ljava/lang/ref/WeakReference;", "Landroid/app/Activity;", "value", "state", "getState", "()Lcom/kt/apps/voiceselector/models/State;", "setState", "(Lcom/kt/apps/voiceselector/models/State;)V", "emitEvent", "", "event", "executeFetchedData", "infor", "Lcom/kt/apps/voiceselector/usecase/VoiceInputInfo;", "ktSubscribeToVoiceSearch", "Lkotlinx/coroutines/flow/Flow;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "launchVoicePackageStore", "openVoiceAssistant", "Lio/reactivex/rxjava3/core/Maybe;", "", "extraData", "Landroid/os/Bundle;", "presentSelector", "queryAndExecute", "registerLifeCycle", "subscribeToVoiceSearch", "Lio/reactivex/rxjava3/disposables/Disposable;", "onNext", "Lkotlin/Function1;", "tryDeepLink", "turnOnAlwaysGG", "voiceGGSearch", "Companion", "voiceselector_debug"})
public final class VoiceSelectorManager {
    @org.jetbrains.annotations.NotNull
    private final com.kt.apps.voiceselector.VoiceSelectorInteractor interactor = null;
    @org.jetbrains.annotations.NotNull
    private final com.kt.apps.voiceselector.models.VoicePackage voicePackage = null;
    @org.jetbrains.annotations.NotNull
    private final com.kt.apps.core.base.CoreApp app = null;
    @org.jetbrains.annotations.NotNull
    private final android.content.SharedPreferences sharedPreferences = null;
    @org.jetbrains.annotations.NotNull
    private final com.kt.apps.core.logging.FirebaseActionLoggerImpl logger = null;
    private java.lang.ref.WeakReference<android.app.Activity> lastActivity;
    @org.jetbrains.annotations.NotNull
    private final io.reactivex.rxjava3.subjects.PublishSubject<com.kt.apps.voiceselector.models.Event> _event = null;
    @org.jetbrains.annotations.NotNull
    private com.kt.apps.voiceselector.models.State _state;
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String GG_LAST_TIME = "key:GG_LAST_TIME";
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String GG_ALWAYS = "key:GG_ALWAYS";
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String EXTRA_CALLING_PACKAGE = "calling_package_name";
    @org.jetbrains.annotations.NotNull
    public static final com.kt.apps.voiceselector.VoiceSelectorManager.Companion Companion = null;
    
    @javax.inject.Inject
    public VoiceSelectorManager(@org.jetbrains.annotations.NotNull
    com.kt.apps.voiceselector.VoiceSelectorInteractor interactor, @org.jetbrains.annotations.NotNull
    com.kt.apps.voiceselector.models.VoicePackage voicePackage, @org.jetbrains.annotations.NotNull
    com.kt.apps.core.base.CoreApp app, @org.jetbrains.annotations.NotNull
    android.content.SharedPreferences sharedPreferences, @org.jetbrains.annotations.NotNull
    com.kt.apps.core.logging.FirebaseActionLoggerImpl logger) {
        super();
    }
    
    public final void setState(@org.jetbrains.annotations.NotNull
    com.kt.apps.voiceselector.models.State value) {
    }
    
    @org.jetbrains.annotations.NotNull
    public final com.kt.apps.voiceselector.models.State getState() {
        return null;
    }
    
    public final void registerLifeCycle() {
    }
    
    private final io.reactivex.rxjava3.core.Maybe<com.kt.apps.voiceselector.usecase.VoiceInputInfo> queryAndExecute() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final io.reactivex.rxjava3.core.Maybe<java.lang.Boolean> openVoiceAssistant(@org.jetbrains.annotations.NotNull
    android.os.Bundle extraData) {
        return null;
    }
    
    private final void executeFetchedData(com.kt.apps.voiceselector.usecase.VoiceInputInfo infor) {
    }
    
    private final boolean tryDeepLink() {
        return false;
    }
    
    private final void presentSelector() {
    }
    
    public final void launchVoicePackageStore() {
    }
    
    public final void voiceGGSearch() {
    }
    
    @org.jetbrains.annotations.NotNull
    public final io.reactivex.rxjava3.disposables.Disposable subscribeToVoiceSearch(@org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function1<? super com.kt.apps.voiceselector.models.Event, kotlin.Unit> onNext) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object ktSubscribeToVoiceSearch(@org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlinx.coroutines.flow.Flow<? extends com.kt.apps.voiceselector.models.Event>> $completion) {
        return null;
    }
    
    public final void emitEvent(@org.jetbrains.annotations.NotNull
    com.kt.apps.voiceselector.models.Event event) {
    }
    
    public final void turnOnAlwaysGG() {
    }
    
    @kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/kt/apps/voiceselector/VoiceSelectorManager$Companion;", "", "()V", "EXTRA_CALLING_PACKAGE", "", "GG_ALWAYS", "GG_LAST_TIME", "voiceselector_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}