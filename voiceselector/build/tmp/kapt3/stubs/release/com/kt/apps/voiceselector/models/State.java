package com.kt.apps.voiceselector.models;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0003\u0003\u0004\u0005B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0003\u0006\u0007\b\u00a8\u0006\t"}, d2 = {"Lcom/kt/apps/voiceselector/models/State;", "", "()V", "IDLE", "LaunchIntent", "ShowDialog", "Lcom/kt/apps/voiceselector/models/State$IDLE;", "Lcom/kt/apps/voiceselector/models/State$LaunchIntent;", "Lcom/kt/apps/voiceselector/models/State$ShowDialog;", "voiceselector_release"})
public abstract class State {
    
    private State() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/kt/apps/voiceselector/models/State$IDLE;", "Lcom/kt/apps/voiceselector/models/State;", "()V", "voiceselector_release"})
    public static final class IDLE extends com.kt.apps.voiceselector.models.State {
        @org.jetbrains.annotations.NotNull
        public static final com.kt.apps.voiceselector.models.State.IDLE INSTANCE = null;
        
        private IDLE() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/kt/apps/voiceselector/models/State$LaunchIntent;", "Lcom/kt/apps/voiceselector/models/State;", "()V", "voiceselector_release"})
    public static final class LaunchIntent extends com.kt.apps.voiceselector.models.State {
        @org.jetbrains.annotations.NotNull
        public static final com.kt.apps.voiceselector.models.State.LaunchIntent INSTANCE = null;
        
        private LaunchIntent() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/kt/apps/voiceselector/models/State$ShowDialog;", "Lcom/kt/apps/voiceselector/models/State;", "()V", "voiceselector_release"})
    public static final class ShowDialog extends com.kt.apps.voiceselector.models.State {
        @org.jetbrains.annotations.NotNull
        public static final com.kt.apps.voiceselector.models.State.ShowDialog INSTANCE = null;
        
        private ShowDialog() {
        }
    }
}