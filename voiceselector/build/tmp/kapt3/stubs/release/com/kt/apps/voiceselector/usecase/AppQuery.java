package com.kt.apps.voiceselector.usecase;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010$\n\u0002\u0010\u0000\n\u0002\b\u0003\u0018\u0000 \u00152\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00030\u00020\u0001:\u0001\u0015B\u000f\b\u0007\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J$\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\n2\b\b\u0002\u0010\u000b\u001a\u00020\f2\b\b\u0002\u0010\r\u001a\u00020\fH\u0002J%\u0010\u000e\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00030\u00020\u000f2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\fH\u0086\u0002J(\u0010\u0010\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00030\u00020\u000f2\u0012\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\u00130\u0012H\u0016J\"\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00030\u0002*\u00020\u00052\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\fH\u0007R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0016"}, d2 = {"Lcom/kt/apps/voiceselector/usecase/AppQuery;", "Lcom/kt/apps/core/base/rxjava/MaybeUseCase;", "", "Lcom/kt/apps/voiceselector/models/AppInfo;", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "getLaunchIntent", "Landroid/content/Intent;", "info", "Landroid/content/pm/ResolveInfo;", "action", "", "category", "invoke", "Lio/reactivex/rxjava3/core/Maybe;", "prepareExecute", "params", "", "", "getAllApps", "Companion", "voiceselector_release"})
public final class AppQuery extends com.kt.apps.core.base.rxjava.MaybeUseCase<java.util.List<? extends com.kt.apps.voiceselector.models.AppInfo>> {
    @org.jetbrains.annotations.NotNull
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String EXTRA_CATEGORY = "extra:category";
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String EXTRA_ACTION = "extra:action";
    @org.jetbrains.annotations.NotNull
    public static final com.kt.apps.voiceselector.usecase.AppQuery.Companion Companion = null;
    
    @javax.inject.Inject
    public AppQuery(@org.jetbrains.annotations.NotNull
    android.content.Context context) {
        super(null);
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.NotNull
    public io.reactivex.rxjava3.core.Maybe<java.util.List<com.kt.apps.voiceselector.models.AppInfo>> prepareExecute(@org.jetbrains.annotations.NotNull
    java.util.Map<java.lang.String, ? extends java.lang.Object> params) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final io.reactivex.rxjava3.core.Maybe<java.util.List<com.kt.apps.voiceselector.models.AppInfo>> invoke(@org.jetbrains.annotations.NotNull
    java.lang.String action, @org.jetbrains.annotations.NotNull
    java.lang.String category) {
        return null;
    }
    
    @android.annotation.SuppressLint(value = {"QueryPermissionsNeeded"})
    @org.jetbrains.annotations.NotNull
    public final java.util.List<com.kt.apps.voiceselector.models.AppInfo> getAllApps(@org.jetbrains.annotations.NotNull
    android.content.Context $this$getAllApps, @org.jetbrains.annotations.NotNull
    java.lang.String action, @org.jetbrains.annotations.NotNull
    java.lang.String category) {
        return null;
    }
    
    private final android.content.Intent getLaunchIntent(android.content.pm.ResolveInfo info, java.lang.String action, java.lang.String category) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/kt/apps/voiceselector/usecase/AppQuery$Companion;", "", "()V", "EXTRA_ACTION", "", "EXTRA_CATEGORY", "voiceselector_release"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}