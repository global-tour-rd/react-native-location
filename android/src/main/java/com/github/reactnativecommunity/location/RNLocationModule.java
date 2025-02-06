package com.github.reactnativecommunity.location;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.module.annotations.ReactModule;

@ReactModule(name = RNLocationModule.NAME)
public class RNLocationModule extends ReactContextBaseJavaModule {
    public static final String NAME = "RNLocation";

    private RNLocationProvider locationProvider;
    private boolean isForegroundServiceRequired = false;
    private boolean isForegroundServiceRunning = false;

    public RNLocationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(activityEventListener);
        reactContext.addLifecycleEventListener(lifecycleEventListener);
    }

    @Override
    public void onCatalystInstanceDestroy() {
        ReactApplicationContext context = getReactApplicationContext();
        context.removeActivityEventListener(activityEventListener);
        context.removeLifecycleEventListener(lifecycleEventListener);
    }

    @Override
    public String getName() {
        return NAME;
    }

    // React interface
    @ReactMethod
    public void addListener(String eventName) {}

    @ReactMethod
    public void removeListeners(double count) {}

    @ReactMethod
    @SuppressWarnings("unused")
    public void configure(ReadableMap options, final Promise promise) {
        // Update the location provider if we are given one
        if (options.hasKey("androidProvider")) {
            String providerName = options.getString("androidProvider");
            switch (providerName) {
                case "auto":
                    locationProvider = createDefaultLocationProvider();
                    break;
                case "playServices":
                    locationProvider = createPlayServicesLocationProvider();
                    break;
                case "standard":
                    locationProvider = createStandardLocationProvider();
                    break;
                default:
                    Utils.emitWarning(getReactApplicationContext(), "androidProvider was passed an unknown value: " + providerName, "401");
            }
        } else if (locationProvider == null) {
            // Otherwise ensure we have a provider and create a default if not
            locationProvider = createDefaultLocationProvider();
        }

        // Pass the options to the location provider
        locationProvider.configure(getCurrentActivity(), options, promise);

        isForegroundServiceRequired = options.hasKey("allowsBackgroundLocationUpdates") && options.getBoolean("allowsBackgroundLocationUpdates");

        if (isForegroundServiceRequired) {
            RNLocationForegroundService.setLocationProvider(locationProvider);
        }
    }

    @ReactMethod
    @SuppressWarnings("unused")
    public void startUpdatingLocation() {
        // Ensure we have a provider
        if (locationProvider == null) {
            locationProvider = createDefaultLocationProvider();
        }

        // Call the provider
        locationProvider.startUpdatingLocation();
    }

    @ReactMethod
    @SuppressWarnings("unused")
    public void stopUpdatingLocation() {
        // Ensure we have a provider
        if (locationProvider == null) {
            locationProvider = createDefaultLocationProvider();
        }

        // Call the provider
        locationProvider.stopUpdatingLocation();
    }

    private void startForegroundService() {
        ReactApplicationContext context = getReactApplicationContext();
        Intent intent = new Intent(context, RNLocationForegroundService.class);
        Intent intentContext = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());

        RNLocationForegroundService.setContext(context);
        RNLocationForegroundService.setContextIntent(intentContext);
        RNLocationForegroundService.setLocationProvider(locationProvider);

        isForegroundServiceRunning = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private void stopForegroundService() {
        ReactApplicationContext context = getReactApplicationContext();
        Intent intent = new Intent(context, RNLocationForegroundService.class);

        isForegroundServiceRunning = false;
        context.stopService(intent);
    }

    // Helpers

    private ActivityEventListener activityEventListener = new BaseActivityEventListener() {
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
            if (locationProvider instanceof RNPlayServicesLocationProvider) {
                ((RNPlayServicesLocationProvider) locationProvider).onActivityResult(requestCode, resultCode, data);
            }
        }
    };

    private LifecycleEventListener lifecycleEventListener = new LifecycleEventListener() {
        @Override
        public void onHostPause() {
            if (isForegroundServiceRequired && !isForegroundServiceRunning) {
                startForegroundService();
            }
        }

        @Override
        public void onHostResume() {
            if (isForegroundServiceRequired && isForegroundServiceRunning) {
                stopForegroundService();
            }
        }

        @Override
        public void onHostDestroy() {
            if (isForegroundServiceRequired && isForegroundServiceRunning) {
                stopForegroundService();
            }
        }
    };

    private RNLocationProvider createDefaultLocationProvider() {
        // If we have the correct classes for the fused location provider, we default to that. Otherwise, we default to the built-in methods
        if (Utils.hasFusedLocationProvider()) {
            return createPlayServicesLocationProvider();
        } else {
            return createStandardLocationProvider();
        }
    }

    private RNPlayServicesLocationProvider createPlayServicesLocationProvider() {
        return new RNPlayServicesLocationProvider(getCurrentActivity(), getReactApplicationContext());
    }

    private RNStandardLocationProvider createStandardLocationProvider() {
        return new RNStandardLocationProvider(getReactApplicationContext());
    }
}
