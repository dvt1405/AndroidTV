package com.kt.apps.voiceselector.di;

@dagger.Component(modules = {com.kt.apps.voiceselector.di.VoiceSelectorModule.class}, dependencies = {com.kt.apps.core.di.CoreComponents.class})
@VoiceSelectorScope
@kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\bg\u0018\u00002\u00020\u0001J\b\u0010\u0002\u001a\u00020\u0003H&J\b\u0010\u0004\u001a\u00020\u0005H&J\b\u0010\u0006\u001a\u00020\u0007H&J\b\u0010\b\u001a\u00020\tH&J\b\u0010\n\u001a\u00020\u000bH&\u00a8\u0006\f"}, d2 = {"Lcom/kt/apps/voiceselector/di/VoiceSelectorComponent;", "", "actionLogger", "Lcom/kt/apps/core/logging/FirebaseActionLoggerImpl;", "appQuery", "Lcom/kt/apps/voiceselector/usecase/AppQuery;", "checkVoiceInput", "Lcom/kt/apps/voiceselector/usecase/CheckVoiceInput;", "providesVoicePackage", "Lcom/kt/apps/voiceselector/models/VoicePackage;", "providesVoiceSelectorManger", "Lcom/kt/apps/voiceselector/VoiceSelectorManager;", "voiceselector_debug"})
public abstract interface VoiceSelectorComponent {
    
    @org.jetbrains.annotations.NotNull
    public abstract com.kt.apps.voiceselector.models.VoicePackage providesVoicePackage();
    
    @org.jetbrains.annotations.NotNull
    public abstract com.kt.apps.voiceselector.VoiceSelectorManager providesVoiceSelectorManger();
    
    @org.jetbrains.annotations.NotNull
    public abstract com.kt.apps.core.logging.FirebaseActionLoggerImpl actionLogger();
    
    @org.jetbrains.annotations.NotNull
    public abstract com.kt.apps.voiceselector.usecase.CheckVoiceInput checkVoiceInput();
    
    @org.jetbrains.annotations.NotNull
    public abstract com.kt.apps.voiceselector.usecase.AppQuery appQuery();
}