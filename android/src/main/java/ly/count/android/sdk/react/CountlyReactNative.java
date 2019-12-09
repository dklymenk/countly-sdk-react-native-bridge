package ly.count.android.sdk.react;

import android.app.Activity;
import android.util.Log;

import android.os.Environment;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.JavaScriptModule;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;



// import ly.count.android.sdknative.CountlyNative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;




import ly.count.android.sdk.Countly;
import ly.count.android.sdk.CountlyConfig;
import static ly.count.android.sdk.Countly.TAG;   // for debug logging
import ly.count.android.sdk.RemoteConfig;
import ly.count.android.sdk.DeviceId;


public class CountlyReactNative extends ReactContextBaseJavaModule {
	private ReactApplicationContext _reactContext;

    private final Set<String> validConsentFeatureNames = new HashSet<String>(Arrays.asList(
            Countly.CountlyFeatureNames.sessions,
            Countly.CountlyFeatureNames.events,
            Countly.CountlyFeatureNames.views,
            Countly.CountlyFeatureNames.location,
            Countly.CountlyFeatureNames.crashes,
            Countly.CountlyFeatureNames.attribution,
            Countly.CountlyFeatureNames.users,
            Countly.CountlyFeatureNames.push,
            Countly.CountlyFeatureNames.starRating
    ));

    public CountlyReactNative(ReactApplicationContext reactContext) {
        super(reactContext);
        _reactContext = reactContext;
    }
    @Override
    public String getName() {
        return "CountlyReactNative";
    }

    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return Collections.emptyList();
    }

	@ReactMethod
	public void init(ReadableArray args){
        Log.d(Countly.TAG, "Initializing...");
        String serverUrl = args.getString(0);
        String appKey = args.getString(1);
        String deviceId = args.getString(2);
        if("".equals(deviceId)){
            deviceId = null;
        }
        Countly.sharedInstance()
                .init(_reactContext, serverUrl, appKey, deviceId);
 	}

    @ReactMethod
    public void initWithConfig(String serverUrl, String appKey, ReadableMap jsConfig) {
        CountlyConfig javaConfig = new CountlyConfig();
        javaConfig
           .setContext(_reactContext)
           .setServerURL(serverUrl)
           .setAppKey(appKey);
        if (jsConfig.hasKey("enableDebug")) {
            javaConfig.setLoggingEnabled(jsConfig.getBoolean("enableDebug"));
        }
        if (jsConfig.hasKey("requiresConsent")) {
            boolean requiresConsent = jsConfig.getBoolean("requiresConsent");
            if (requiresConsent) {
                if (jsConfig.hasKey("consentFeatures")) {
                     javaConfig.setRequiresConsent(requiresConsent);
                     ReadableArray featuresList = jsConfig.getArray("consentFeatures");
                     String featuresArr[] = new String[featuresList.size()];
                     for (int j = 0; j < featuresList.size(); j++) {
                         featuresArr[j] = featuresList.getString(j);
                     }
                     javaConfig.setConsentEnabled(featuresArr);
                }
                else {
                     Log.w(TAG, "You need to specify consentFeatures array if requiresConsent is true");
                }
            }
            else {
               javaConfig.setRequiresConsent(requiresConsent);
            }
        }
        if (jsConfig.hasKey("updateSessionPeriod")) {
            // Not implemented yet. TIMER_DELAY_IN_SECONDS is static in Countly.java.
        }
        if (jsConfig.hasKey("alwaysUsePOST")) {
            javaConfig.setHttpPostForced(jsConfig.getBoolean("alwaysUsePOST"));
        }
        if (jsConfig.hasKey("customHeaderFieldName") && jsConfig.hasKey("customHeaderFieldValue")) {
            HashMap<String, String> customHeaderValues = new HashMap<String,String>();
            String fieldName = jsConfig.getString("customHeaderFieldName");
            String fieldValue = jsConfig.getString("customHeaderFieldValue");
            customHeaderValues.put(fieldName, fieldValue);
            javaConfig.addCustomNetworkRequestHeaders(customHeaderValues);
        }
        if (jsConfig.hasKey("starRating")) {
            ReadableMap starRatingConfig = jsConfig.getMap("starRating");
            if (starRatingConfig.hasKey("message")) {
               javaConfig.setStarRatingTextMessage(starRatingConfig.getString("message"));
            }
            if (starRatingConfig.hasKey("sessionCount")) {
               javaConfig.setStarRatingLimit(starRatingConfig.getInt("sessionCount"));
            }
        }

        Countly.sharedInstance().init(javaConfig);

        // First make sure init worked
        int retries = 1;
        try {
            while (retries <= 5 && !Countly.sharedInstance().isInitialized()) {
              Log.d(TAG, "Waiting for Countly to get initialized");
              Thread.sleep(retries * 1000);
              retries++;
            }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }

        // Now we can set the following after Android init.
        if (Countly.sharedInstance().isInitialized()) {
            if (jsConfig.hasKey("eventSendThreshold")) {
                Countly.sharedInstance().setEventQueueSizeToSend(jsConfig.getInt("eventSendThreshold"));
            }
            if (jsConfig.hasKey("location")) {
                ReadableMap location = jsConfig.getMap("location");
                String city = location.hasKey("city") ? location.getString("city") : null;
                String country_code = location.hasKey("country_code") ? location.getString("country_code") : null;
                String latLonCoordinates = location.hasKey("latLonCoordinates") ? location.getString("latLonCoordinates") : null;
                String IP = location.hasKey("IP") ? location.getString("IP") : null;
                Countly.sharedInstance().setLocation(country_code, city, latLonCoordinates, IP);
            }
            if (jsConfig.hasKey("secretSalt")) {
                Countly.sharedInstance().enableParameterTamperingProtection(jsConfig.getString("secretSalt"));
            }
            if (jsConfig.hasKey("starRating")) {
                ReadableMap starRatingConfig = jsConfig.getMap("starRating");
                if (starRatingConfig.hasKey("disableAskingForEachAppVersion")) {
                   Boolean disableAskingForEachAppVersion = starRatingConfig.getBoolean("disableAskingForEachAppVersion");
                   Countly.sharedInstance().setStarRatingDisableAskingForEachAppVersion(disableAskingForEachAppVersion);
                }
            }
        }
        else {
          Log.d(TAG, "Gave up waiting for Countly to get initialized.");
        }
    }

	@ReactMethod
	public void setLoggingEnabled(ReadableArray args){
        Boolean enabled = args.getBoolean(0);
        Countly.sharedInstance().setLoggingEnabled(enabled);
	}

    @ReactMethod
    public Boolean isInitialized(ReadableArray args){
        return Countly.sharedInstance().isInitialized();
    }

    @ReactMethod
    public Boolean hasBeenCalledOnStart(ReadableArray args){
        return Countly.sharedInstance().hasBeenCalledOnStart();
    }

    @ReactMethod
    public void changeDeviceId(ReadableArray args){
        String newDeviceID = args.getString(0);
        String onServerString = args.getString(1);
        if("1".equals(onServerString)){
            Countly.sharedInstance().changeDeviceId(newDeviceID);
        }else{
            Countly.sharedInstance().changeDeviceId(DeviceId.Type.DEVELOPER_SUPPLIED, newDeviceID);
        }
    }

    @ReactMethod
    public void setHttpPostForced(ReadableArray args){
        int isEnabled = Integer.parseInt(args.getString(0));
        if(isEnabled == 1){
            Countly.sharedInstance().setHttpPostForced(true);
        }else{
            Countly.sharedInstance().setHttpPostForced(false);
        }
    }

    @ReactMethod
    public void enableParameterTamperingProtection(ReadableArray args){
        String salt = args.getString(0);
        Countly.sharedInstance().enableParameterTamperingProtection(salt);
    }

    @ReactMethod
    public void setLocation(ReadableArray args){
        String countryCode = args.getString(0);
        String city = args.getString(1);
        String location = args.getString(2);
        String ipAddress = args.getString(3);
        if("".equals(countryCode)){
            countryCode = null;
        }
        if("".equals(city)){
            city = null;
        }
        if("0.0.0.0".equals(ipAddress)){
            ipAddress = null;
        }
        if("0.0,0.0".equals(location)){
            location = null;
        }
        Countly.sharedInstance().setLocation(countryCode, city, location, ipAddress);
    }

    @ReactMethod
    public void disableLocation(){
        Countly.sharedInstance().disableLocation();
    }

    @ReactMethod
    public void enableCrashReporting(){
        Countly.sharedInstance().enableCrashReporting();
    }

    @ReactMethod
    public void addCrashLog(ReadableArray args){
        String record = args.getString(0);
        Countly.sharedInstance().addCrashLog(record);
    }
    @ReactMethod
    public void logException(ReadableArray args){
        String exceptionString = args.getString(0);
        Exception exception = new Exception(exceptionString);

        Boolean nonfatal = args.getBoolean(1);

        HashMap<String, String> segments = new HashMap<String, String>();
        for(int i=2,il=args.size();i<il;i+=2){
            segments.put(args.getString(i), args.getString(i+1));
        }
        segments.put("nonfatal", nonfatal.toString());
        Countly.sharedInstance().setCustomCrashSegments(segments);

        Countly.sharedInstance().logException(exception);
    }

    @ReactMethod
    public void setCustomCrashSegments(ReadableArray args){
        Map<String, String> segments = null;
        for(int i=0,il=args.size();i<il;i++){
            segments.put(args.getString(i), args.getString(i));
        }
        Countly.sharedInstance().setCustomCrashSegments(segments);
    }

   @ReactMethod
    public void event(ReadableArray args){
        String eventType = args.getString(0);
        if("event".equals(eventType)){
            String eventName = args.getString(1);
            int eventCount= Integer.parseInt(args.getString(2));
            Countly.sharedInstance().recordEvent(eventName, eventCount);
        }
        else if ("eventWithSum".equals(eventType)) {
            String eventName = args.getString(1);
            int eventCount= Integer.parseInt(args.getString(2));
            float eventSum= new Float(args.getString(3)).floatValue();
            Countly.sharedInstance().recordEvent(eventName, eventCount, eventSum);
        }
        else if ("eventWithSegment".equals(eventType)) {
            String eventName = args.getString(1);
            int eventCount= Integer.parseInt(args.getString(2));
            HashMap<String, String> segmentation = new HashMap<String, String>();
            for(int i=3,il=args.size();i<il;i+=2){
                segmentation.put(args.getString(i), args.getString(i+1));
            }
            Countly.sharedInstance().recordEvent(eventName, segmentation, eventCount);
            }
        else if ("eventWithSumSegment".equals(eventType)) {
            String eventName = args.getString(1);
            int eventCount= Integer.parseInt(args.getString(2));
            float eventSum= new Float(args.getString(3)).floatValue();
            HashMap<String, String> segmentation = new HashMap<String, String>();
            for(int i=4,il=args.size();i<il;i+=2){
                segmentation.put(args.getString(i), args.getString(i+1));
            }
            Countly.sharedInstance().recordEvent(eventName, segmentation, eventCount,eventSum);
        }
        else{
        }
    }

    @ReactMethod
    public void startEvent(ReadableArray args){
        String startEvent = args.getString(0);
        Countly.sharedInstance().startEvent(startEvent);
    }

    @ReactMethod
    public void endEvent(ReadableArray args){
        String eventType = args.getString(0);
        if("event".equals(eventType)){
            String eventName = args.getString(1);
            Countly.sharedInstance().endEvent(eventName);
        }
        else if ("eventWithSum".equals(eventType)) {
            String eventName = args.getString(1);
            int eventCount= Integer.parseInt(args.getString(2));
            float eventSum= new Float(args.getString(3)).floatValue();
            Countly.sharedInstance().endEvent(eventName, null, eventCount,eventSum);
        }
        else if ("eventWithSegment".equals(eventType)) {
            String eventName = args.getString(1);
            int eventCount= Integer.parseInt(args.getString(2));
            HashMap<String, String> segmentation = new HashMap<String, String>();
            for(int i=4,il=args.size();i<il;i+=2){
                segmentation.put(args.getString(i), args.getString(i+1));
            }
            Countly.sharedInstance().endEvent(eventName, segmentation, eventCount,0);
        }
        else if ("eventWithSumSegment".equals(eventType)) {
            String eventName = args.getString(1);
            int eventCount= Integer.parseInt(args.getString(2));
            float eventSum= new Float(args.getString(3)).floatValue();
            HashMap<String, String> segmentation = new HashMap<String, String>();
            for(int i=4,il=args.size();i<il;i+=2){
                segmentation.put(args.getString(i), args.getString(i+1));
            }
            Countly.sharedInstance().endEvent(eventName, segmentation, eventCount,eventSum);
        }
        else{
        }
    }

	@ReactMethod
	public void recordView(ReadableArray args){
        String viewName = args.getString(0);
		Countly.sharedInstance().recordView(viewName);
    }

    @ReactMethod
    public void setViewTracking(ReadableArray args){
        String flag = args.getString(0);
        if("true".equals(flag)){
            Countly.sharedInstance().setViewTracking(true);
        }else{
            Countly.sharedInstance().setViewTracking(false);
        }
    }




	@ReactMethod
	public void setUserData(ReadableArray args){
        Map<String, String> bundle = new HashMap<String, String>();
        bundle.put("name", args.getString(0));
        bundle.put("username", args.getString(1));
        bundle.put("email", args.getString(2));
        bundle.put("org", args.getString(3));
        bundle.put("phone", args.getString(4));
        bundle.put("picture", args.getString(5));
        bundle.put("picturePath", args.getString(6));
        bundle.put("gender", args.getString(7));
        bundle.put("byear", String.valueOf(args.getInt(8)));
        Countly.userData.setUserData(bundle);
        Countly.userData.save();
	}

	@ReactMethod
	 public void onRegistrationId(ReadableArray args){
        String pushToken = args.getString(0);
        int messagingMode = Integer.parseInt(args.getString(1));

        Countly.CountlyMessagingMode mode = null;
        if(messagingMode == 0){
            mode = Countly.CountlyMessagingMode.PRODUCTION;
        }
        else{
            mode = Countly.CountlyMessagingMode.TEST;
        }
        Countly.sharedInstance().onRegistrationId(pushToken, mode);
	}

	@ReactMethod
	public void start(){
		Countly.sharedInstance().onStart(getCurrentActivity());
	}

	@ReactMethod
	public void stop(){
		Countly.sharedInstance().onStop();
	}

    @ReactMethod
    public void userData_setProperty(ReadableArray args){
        String keyName = args.getString(0);
        String keyValue = args.getString(1);
        Countly.userData.setProperty(keyName, keyValue);
        Countly.userData.save();
    }

    @ReactMethod
    public void userData_increment(ReadableArray args){
        String keyName = args.getString(0);
        Countly.userData.increment(keyName);
        Countly.userData.save();
    }

    @ReactMethod
    public void userData_incrementBy(ReadableArray args){
        String keyName = args.getString(0);
        int keyIncrement = Integer.parseInt(args.getString(1));
        Countly.userData.incrementBy(keyName, keyIncrement);
        Countly.userData.save();
    }

    @ReactMethod
    public void userData_multiply(ReadableArray args){
        String keyName = args.getString(0);
        int multiplyValue = Integer.parseInt(args.getString(1));
        Countly.userData.multiply(keyName, multiplyValue);
        Countly.userData.save();
    }

    @ReactMethod
    public void userData_saveMax(ReadableArray args){
        String keyName = args.getString(0);
        int maxScore = Integer.parseInt(args.getString(1));
        Countly.userData.saveMax(keyName, maxScore);
        Countly.userData.save();
    }

    @ReactMethod
    public void userData_saveMin(ReadableArray args){
        String keyName = args.getString(0);
        int minScore = Integer.parseInt(args.getString(1));
        Countly.userData.saveMin(keyName, minScore);
        Countly.userData.save();
    }

    @ReactMethod
    public void userData_setOnce(ReadableArray args){
        String keyName = args.getString(0);
        String minScore = args.getString(1);
        Countly.userData.setOnce(keyName, minScore);
        Countly.userData.save();
    }

    @ReactMethod
    public void userData_pushUniqueValue(ReadableArray args){
        String keyName = args.getString(0);
        String keyValue = args.getString(1);
        Countly.userData.pushUniqueValue(keyName, keyValue);
        Countly.userData.save();
    }

    @ReactMethod
    public void userData_pushValue(ReadableArray args){
        String keyName = args.getString(0);
        String keyValue = args.getString(1);
        Countly.userData.pushValue(keyName, keyValue);
        Countly.userData.save();
    }

    @ReactMethod
    public void userData_pullValue(ReadableArray args){
        String keyName = args.getString(0);
        String keyValue = args.getString(1);
        Countly.userData.pullValue(keyName, keyValue);
        Countly.userData.save();
    }

    // GDPR
    @ReactMethod
    public void setRequiresConsent(ReadableArray args){
			  Boolean consentFlag = args.getBoolean(0);
        Countly.sharedInstance().setRequiresConsent(consentFlag);
    }

    @ReactMethod
    public void giveConsent(ReadableArray featureNames){
        List<String> features = new ArrayList<>();
        for (int i = 0; i < featureNames.size(); i++) {
            String featureName = featureNames.getString(i);
            if (validConsentFeatureNames.contains(featureName)) {
               features.add(featureName);
            }
            else {
               Log.d(Countly.TAG, "Not a valid consent feature to add: " + featureName);
            }
        }
        Countly.sharedInstance().giveConsent(features.toArray(new String[features.size()]));
    }

    @ReactMethod
    public void removeConsent(ReadableArray featureNames){
        List<String> features = new ArrayList<>();
        for (int i = 0; i < featureNames.size(); i++) {
            String featureName = featureNames.getString(i);
            if (validConsentFeatureNames.contains(featureName)) {
               features.add(featureName);
            }
            else {
               Log.d(Countly.TAG, "Not a valid consent feature to remove: " + featureName);
            }
        }
        Countly.sharedInstance().removeConsent(features.toArray(new String[features.size()]));
    }

    @ReactMethod
    public void giveAllConsent(){
        Countly.sharedInstance().giveConsent(validConsentFeatureNames.toArray(new String[validConsentFeatureNames.size()]));
    }

    @ReactMethod
    public void removeAllConsent(){
        Countly.sharedInstance().removeConsent(validConsentFeatureNames.toArray(new String[validConsentFeatureNames.size()]));
    }


    @ReactMethod
    public void remoteConfigUpdate(ReadableArray args, final Callback myCallback){
        Countly.sharedInstance().remoteConfigUpdate(new RemoteConfig.RemoteConfigCallback() {
            String resultString = "";
            @Override
            public void callback(String error) {
                if(error == null) {
                    resultString = "Remote Config is updated and ready to use!";
                } else {
                    resultString = "There was an error while updating Remote Config: " + error;
                }
                myCallback.invoke(resultString);
            }
        });
    }

    @ReactMethod
    public void updateRemoteConfigForKeysOnly(ReadableArray args, final Callback myCallback){
        int i = args.size();
        int n = ++i;
        String[] newArray = new String[n];
        for(int cnt=0;cnt<args.size();cnt++)
        {
            newArray[cnt] = args.getString(cnt);
        }
        Countly.sharedInstance().updateRemoteConfigForKeysOnly(newArray, new RemoteConfig.RemoteConfigCallback() {
            String resultString = "";
            @Override
            public void callback(String error) {
                if(error == null) {
                    resultString = "Remote Config is updated only for given keys and ready to use!";
                } else {
                    resultString = "There was an error while updating Remote Config: " + error;
                }
                myCallback.invoke(resultString);
            }
        });
    }


    @ReactMethod
    public void updateRemoteConfigExceptKeys(ReadableArray args, final Callback myCallback){
        int i = args.size();
        int n = ++i;
        String[] newArray = new String[n];
        for(int cnt=0;cnt<args.size();cnt++)
        {
            newArray[cnt] = args.getString(cnt);
        }
        Countly.sharedInstance().updateRemoteConfigExceptKeys(newArray, new RemoteConfig.RemoteConfigCallback() {
            String resultString = "";
            @Override
            public void callback(String error) {
                if (error == null) {
                    resultString = "Remote Config is updated except for given keys and ready to use !";
                } else {
                    resultString = "There was an error while updating Remote Config: " + error;
                }
                myCallback.invoke(resultString);
            }
        });
    }

    @ReactMethod
    public void getRemoteConfigValueForKey(ReadableArray args, final Callback myCallback){
        String keyName = args.getString(0);
        Object keyValue = Countly.sharedInstance().getRemoteConfigValueForKey(keyName);
        if (keyValue == null) {
            // Log.d(TAG, keyName + ": ConfigKeyNotFound");
            myCallback.invoke("ConfigKeyNotFound");
        }
        else {
            String resultString = (keyValue).toString();
            // Log.d(TAG, keyName + ": " + resultString);
            myCallback.invoke(resultString);
        }
    }

    @ReactMethod
    public void getRemoteConfigValueForKeyP(String keyName, Promise promise){
        Object keyValue = Countly.sharedInstance().getRemoteConfigValueForKey(keyName);
        if (keyValue == null) {
            Log.d(TAG, keyName + ": ConfigKeyNotFound");
            promise.reject("ConfigKeyNotFound");
        }
        else {
            String resultString = (keyValue).toString();
            Log.d(TAG, keyName + ": " + resultString);
            promise.resolve(resultString);
        }
    }

    @ReactMethod
    public void remoteConfigClearValues(Promise promise){
        Countly.sharedInstance().remoteConfigClearValues();
        promise.resolve("Remote Config Cleared.");
    }

    @ReactMethod
    public void showStarRating(ReadableArray args){
        Activity activity = getCurrentActivity();
        Countly.sharedInstance().showStarRating(activity, null);

    }

    @ReactMethod
    public void showFeedbackPopup(ReadableArray args){
        String widgetId = args.getString(0);
        String closeFeedBackButton = args.getString(1);
        Activity activity = getCurrentActivity();
        Countly.sharedInstance().showFeedbackPopup( widgetId, closeFeedBackButton, activity, null);
    }

    @ReactMethod
    public void setEventSendThreshold(ReadableArray args){
        int size = Integer.parseInt(args.getString(0));
        Countly.sharedInstance().setEventQueueSizeToSend(size);
    }

    /*
    @ReactMethod
    public void initNative(){
            CountlyNative.initNative(getReactApplicationContext());
    }

    @ReactMethod
    public void testCrash(){
            CountlyNative.crash();
    }
    */

}
