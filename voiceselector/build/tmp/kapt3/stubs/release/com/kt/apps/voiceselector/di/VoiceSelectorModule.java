package com.kt.apps.voiceselector.di;

@dagger.Module
@kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0017\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0017J\b\u0010\u0007\u001a\u00020\bH\u0017\u00a8\u0006\t"}, d2 = {"Lcom/kt/apps/voiceselector/di/VoiceSelectorModule;", "", "()V", "providesAndroidTVLogger", "Lcom/kt/apps/core/logging/FirebaseActionLoggerImpl;", "analytics", "Lcom/google/firebase/analytics/FirebaseAnalytics;", "providesVoicePackage", "Lcom/kt/apps/voiceselector/models/VoicePackage;", "voiceselector_release"})
public class VoiceSelectorModule {
    
    public VoiceSelectorModule() {
        super();
    }
    
    @dagger.Provides
    @VoiceSelectorScope
    @org.jetbrains.annotations.NotNull
    public com.kt.apps.voiceselector.models.VoicePackage providesVoicePackage() {
        return null;
    }
    
    @dagger.Provides
    @VoiceSelectorScope
    @org.jetbrains.annotations.NotNull
    public com.kt.apps.core.logging.FirebaseActionLoggerImpl providesAndroidTVLogger(@org.jetbrains.annotations.NotNull
    com.google.firebase.analytics.FirebaseAnalytics analytics) {
        return null;
    }
}