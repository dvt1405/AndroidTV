// Generated by Dagger (https://dagger.dev).
package com.kt.apps.voiceselector.di;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.kt.apps.core.base.CoreApp;
import com.kt.apps.core.di.CoreComponents;
import com.kt.apps.core.logging.FirebaseActionLoggerImpl;
import com.kt.apps.voiceselector.VoiceSelectorInteractor;
import com.kt.apps.voiceselector.VoiceSelectorInteractor_Factory;
import com.kt.apps.voiceselector.VoiceSelectorManager;
import com.kt.apps.voiceselector.VoiceSelectorManager_Factory;
import com.kt.apps.voiceselector.models.VoicePackage;
import com.kt.apps.voiceselector.usecase.AppQuery;
import com.kt.apps.voiceselector.usecase.AppQuery_Factory;
import com.kt.apps.voiceselector.usecase.CheckVoiceInput;
import com.kt.apps.voiceselector.usecase.CheckVoiceInput_Factory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.Preconditions;
import javax.inject.Provider;

@DaggerGenerated
@SuppressWarnings({
    "unchecked",
    "rawtypes"
})
public final class DaggerVoiceSelectorComponent {
  private DaggerVoiceSelectorComponent() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private VoiceSelectorModule voiceSelectorModule;

    private CoreComponents coreComponents;

    private Builder() {
    }

    public Builder voiceSelectorModule(VoiceSelectorModule voiceSelectorModule) {
      this.voiceSelectorModule = Preconditions.checkNotNull(voiceSelectorModule);
      return this;
    }

    public Builder coreComponents(CoreComponents coreComponents) {
      this.coreComponents = Preconditions.checkNotNull(coreComponents);
      return this;
    }

    public VoiceSelectorComponent build() {
      if (voiceSelectorModule == null) {
        this.voiceSelectorModule = new VoiceSelectorModule();
      }
      Preconditions.checkBuilderRequirement(coreComponents, CoreComponents.class);
      return new VoiceSelectorComponentImpl(voiceSelectorModule, coreComponents);
    }
  }

  private static final class VoiceSelectorComponentImpl implements VoiceSelectorComponent {
    private final CoreComponents coreComponents;

    private final VoiceSelectorComponentImpl voiceSelectorComponentImpl = this;

    private Provider<VoicePackage> providesVoicePackageProvider;

    private Provider<Context> contextProvider;

    private Provider<AppQuery> appQueryProvider;

    private Provider<CheckVoiceInput> checkVoiceInputProvider;

    private Provider<VoiceSelectorInteractor> voiceSelectorInteractorProvider;

    private Provider<CoreApp> coreAppProvider;

    private Provider<SharedPreferences> sharedPreferencesProvider;

    private Provider<FirebaseAnalytics> firebaseAnalyticsProvider;

    private Provider<FirebaseActionLoggerImpl> providesAndroidTVLoggerProvider;

    private Provider<VoiceSelectorManager> voiceSelectorManagerProvider;

    private VoiceSelectorComponentImpl(VoiceSelectorModule voiceSelectorModuleParam,
        CoreComponents coreComponentsParam) {
      this.coreComponents = coreComponentsParam;
      initialize(voiceSelectorModuleParam, coreComponentsParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final VoiceSelectorModule voiceSelectorModuleParam,
        final CoreComponents coreComponentsParam) {
      this.providesVoicePackageProvider = DoubleCheck.provider(VoiceSelectorModule_ProvidesVoicePackageFactory.create(voiceSelectorModuleParam));
      this.contextProvider = new ContextProvider(coreComponentsParam);
      this.appQueryProvider = AppQuery_Factory.create(contextProvider);
      this.checkVoiceInputProvider = CheckVoiceInput_Factory.create(providesVoicePackageProvider, appQueryProvider);
      this.voiceSelectorInteractorProvider = VoiceSelectorInteractor_Factory.create(checkVoiceInputProvider);
      this.coreAppProvider = new CoreAppProvider(coreComponentsParam);
      this.sharedPreferencesProvider = new SharedPreferencesProvider(coreComponentsParam);
      this.firebaseAnalyticsProvider = new FirebaseAnalyticsProvider(coreComponentsParam);
      this.providesAndroidTVLoggerProvider = DoubleCheck.provider(VoiceSelectorModule_ProvidesAndroidTVLoggerFactory.create(voiceSelectorModuleParam, firebaseAnalyticsProvider));
      this.voiceSelectorManagerProvider = DoubleCheck.provider(VoiceSelectorManager_Factory.create(voiceSelectorInteractorProvider, providesVoicePackageProvider, coreAppProvider, sharedPreferencesProvider, providesAndroidTVLoggerProvider));
    }

    @Override
    public VoicePackage providesVoicePackage() {
      return providesVoicePackageProvider.get();
    }

    @Override
    public VoiceSelectorManager providesVoiceSelectorManger() {
      return voiceSelectorManagerProvider.get();
    }

    @Override
    public FirebaseActionLoggerImpl actionLogger() {
      return providesAndroidTVLoggerProvider.get();
    }

    @Override
    public CheckVoiceInput checkVoiceInput() {
      return new CheckVoiceInput(providesVoicePackageProvider.get(), appQuery());
    }

    @Override
    public AppQuery appQuery() {
      return new AppQuery(Preconditions.checkNotNullFromComponent(coreComponents.context()));
    }

    private static final class ContextProvider implements Provider<Context> {
      private final CoreComponents coreComponents;

      ContextProvider(CoreComponents coreComponents) {
        this.coreComponents = coreComponents;
      }

      @Override
      public Context get() {
        return Preconditions.checkNotNullFromComponent(coreComponents.context());
      }
    }

    private static final class CoreAppProvider implements Provider<CoreApp> {
      private final CoreComponents coreComponents;

      CoreAppProvider(CoreComponents coreComponents) {
        this.coreComponents = coreComponents;
      }

      @Override
      public CoreApp get() {
        return Preconditions.checkNotNullFromComponent(coreComponents.coreApp());
      }
    }

    private static final class SharedPreferencesProvider implements Provider<SharedPreferences> {
      private final CoreComponents coreComponents;

      SharedPreferencesProvider(CoreComponents coreComponents) {
        this.coreComponents = coreComponents;
      }

      @Override
      public SharedPreferences get() {
        return Preconditions.checkNotNullFromComponent(coreComponents.sharedPreferences());
      }
    }

    private static final class FirebaseAnalyticsProvider implements Provider<FirebaseAnalytics> {
      private final CoreComponents coreComponents;

      FirebaseAnalyticsProvider(CoreComponents coreComponents) {
        this.coreComponents = coreComponents;
      }

      @Override
      public FirebaseAnalytics get() {
        return Preconditions.checkNotNullFromComponent(coreComponents.firebaseAnalytics());
      }
    }
  }
}
