package com.lshfgj.minamigo.HyperOS3EULocalization;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import android.content.pm.ApplicationInfo;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainModule implements IXposedHookLoadPackage {
    private static final String MEDIAEDITOR_MTK_REMOVER = "ai_remover_mtk_high_v2";
    private static final String MEDIAEDITOR_SD_REMOVER = "ai_remover_sd_high_v2";
    private static final String MEDIAEDITOR_MTK_MATTING = "image_magic_matting_mtk_high";
    private static final String MEDIAEDITOR_SD_MATTING = "image_magic_matting_sd_high";

    List<String> XSPACE_INTRODUCE_APPS = new ArrayList();

    public MainModule() {
        this.XSPACE_INTRODUCE_APPS.add("com.tencent.mm");
        this.XSPACE_INTRODUCE_APPS.add("com.tencent.mobileqq");
        this.XSPACE_INTRODUCE_APPS.add("com.sina.weibo");
        this.XSPACE_INTRODUCE_APPS.add("com.whatsapp");
        this.XSPACE_INTRODUCE_APPS.add("com.facebook.katana");
        this.XSPACE_INTRODUCE_APPS.add("com.instagram.android");
    }

    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        String pkg = lpparam.packageName;

        switch (pkg) {
            case "com.lshfgj.minamigo.HyperOS3EULocalization":
                try {
                    handleSelf(lpparam);
                } catch (Exception e) {
                    XposedBridge.log("Hooked " + pkg + " Error: " + e.toString());
                }
                break;
            case "com.miui.securitycore":
                try {
                    handleSecuritycore(lpparam);
                } catch (Exception e) {
                    XposedBridge.log("Hooked " + pkg + " Error: " + e.toString());
                }
                break;
            case "com.miui.securitycenter":
                try {
                    handleSecurityCenter(lpparam);
                } catch (Exception e) {
                    XposedBridge.log("Hooked " + pkg + " Error: " + e.toString());
                }
                break;
            case "com.miui.packageinstaller":
                try {
                    handleMiuiPackageInstaller(lpparam);
                } catch (Exception e) {
                    XposedBridge.log("Hooked " + pkg + " Error: " + e.toString());
                }
                break;
            case "com.miui.mediaeditor":
                try {
                    bypassSignatureChecks(lpparam);
                    handleMediaEditor(lpparam);
                } catch (Exception e) {
                    XposedBridge.log("Hooked " + pkg + " Error: " + e.toString());
                }
                break;
            case "com.miui.home":
                try {
                    bypassSignatureChecks(lpparam); // Add bypass here
                    handleMiuiHome(lpparam);
                } catch (Exception e) {
                    XposedBridge.log("Hooked " + pkg + " Error: " + e.toString());
                }
                break;
            case "android":
                try {
                    handleInternational(lpparam);
                    handlePackageManagerService(lpparam); // Renamed from handlePackageManager
                    bypassSignatureChecks(lpparam); // Also apply here for system server loading
                } catch (Exception e) {
                    XposedBridge.log("Hooked " + pkg + " Error: " + e.toString());
                }
                break;
            case "com.miui.powerkeeper":
            case "com.xiaomi.powerchecker":
            case "com.miui.core":
                try {
                    handleInternational(lpparam);
                } catch (Exception e) {
                    XposedBridge.log("Hooked " + pkg + " Error: " + e.toString());
                }
                break;
            default:
                // Apply signature bypass to all ported CN apps that may have signature mismatches
                switch (pkg) {
                    case "com.android.mms":
                    case "com.miui.voiceassist":
                    case "com.miui.personalassistant":
                    case "com.miui.weather2":
                    case "com.android.thememanager":
                    case "com.miui.notes":
                    case "com.miui.aod":
                    case "com.xiaomi.aiasst.vision":
                    case "com.xiaomi.aiasst.service":
                    case "com.miui.player":
                    case "com.android.calendar":
                    case "com.miui.yellowpage":
                    case "com.miui.contentcatcher":
                    case "com.miui.contentextension":
                    case "com.miui.hybrid":
                    case "com.miui.nextpay":
                    case "com.miui.tsmclient":
                    case "com.miui.mipay":
                    case "com.mipay.wallet":
                    case "com.xiaomi.payment":
                    case "com.unionpay.tsmservice.mi":
                    case "com.android.contacts":
                    case "com.android.soundrecorder":
                    case "com.miui.audiomonitor":
                    case "com.xiaomi.market":
                    case "com.miui.gallery":
                    case "com.miui.mediaeditor":
                    case "com.miui.guardprovider":
                    case "com.miui.greenguard":
                        bypassSignatureChecks(lpparam);
                        break;
                }
                break;
        }
    }

    private void handleMediaEditor(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!isNezhaDevice()) {
            return;
        }
        forceMediaEditorDeviceName();
        hookMediaEditorAiModelSelection(lpparam);
        hookMediaEditorAiDownloadSelection(lpparam);
    }

    private boolean isNezhaDevice() {
        String productDevice = getSystemProperty("ro.product.device");
        String odmDevice = getSystemProperty("ro.product.odm.device");
        String socModel = getSystemProperty("ro.soc.model");
        return "nezha".equalsIgnoreCase(productDevice)
                || "nezha".equalsIgnoreCase(odmDevice)
                || "SM8850".equalsIgnoreCase(socModel);
    }

    private String getSystemProperty(String key) {
        try {
            Class<?> systemPropertiesClass = XposedHelpers.findClassIfExists("android.os.SystemProperties", null);
            if (systemPropertiesClass == null) {
                return "";
            }
            Object value = XposedHelpers.callStaticMethod(systemPropertiesClass, "get", key, "");
            return value == null ? "" : String.valueOf(value);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private void forceMediaEditorDeviceName() {
        try {
            Class<?> buildClass = XposedHelpers.findClassIfExists("android.os.Build", null);
            if (buildClass != null) {
                XposedHelpers.setStaticObjectField(buildClass, "DEVICE", "nezha");
                XposedHelpers.setStaticObjectField(buildClass, "PRODUCT", "nezha");
            }
        } catch (Throwable t) {
            XposedBridge.log("HyperOS3 Localization: MediaEditor Build.DEVICE patch failed: " + t);
        }
    }

    private void hookMediaEditorAiModelSelection(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> modelKeyClass = XposedHelpers.findClassIfExists("Db.a", lpparam.classLoader);
            if (modelKeyClass == null) {
                XposedBridge.log("HyperOS3 Localization: MediaEditor model key class not found");
                return;
            }

            XposedBridge.hookAllMethods(modelKeyClass, "b", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args.length == 0) {
                        param.setResult(MEDIAEDITOR_SD_REMOVER);
                    }
                }
            });

            XposedBridge.hookAllMethods(modelKeyClass, "a", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args.length == 0) {
                        param.setResult(MEDIAEDITOR_SD_MATTING);
                    }
                }
            });

            Object sdHighModelType = getMediaEditorSdHighModelType(lpparam.classLoader);
            if (sdHighModelType != null) {
                try {
                    XposedHelpers.setStaticObjectField(modelKeyClass, "f1390a", sdHighModelType);
                } catch (Throwable ignored) {
                }

                Class<?> selectorClass = XposedHelpers.findClassIfExists("Db.c$a", lpparam.classLoader);
                if (selectorClass != null) {
                    final Object finalSdHighModelType = sdHighModelType;
                    XposedBridge.hookAllMethods(selectorClass, "b", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(finalSdHighModelType);
                        }
                    });
                }
            }

            XposedBridge.log("HyperOS3 Localization: MediaEditor AI remover forced to SD_HIGH on nezha");
        } catch (Throwable t) {
            XposedBridge.log("HyperOS3 Localization: MediaEditor AI model hook failed: " + t);
        }
    }

    private void hookMediaEditorAiDownloadSelection(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> modelInfoClass = XposedHelpers.findClassIfExists("t9.h", lpparam.classLoader);
            if (modelInfoClass != null) {
                XposedBridge.hookAllConstructors(modelInfoClass, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        redirectMediaEditorModelInfo(param.thisObject);
                    }
                });
            }
        } catch (Throwable t) {
            XposedBridge.log("HyperOS3 Localization: MediaEditor model info hook failed: " + t);
        }

        try {
            Class<?> localConfigClass = XposedHelpers.findClassIfExists("M8.p", lpparam.classLoader);
            if (localConfigClass != null) {
                XposedBridge.hookAllMethods(localConfigClass, "b", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (param.args != null && param.args.length > 0) {
                            param.args[0] = redirectMediaEditorModelKey(param.args[0]);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            XposedBridge.log("HyperOS3 Localization: MediaEditor local config hook failed: " + t);
        }

        try {
            Class<?> managerBeanClass = XposedHelpers.findClassIfExists("M8.u", lpparam.classLoader);
            if (managerBeanClass != null) {
                XposedBridge.hookAllConstructors(managerBeanClass, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        redirectMediaEditorManagerBean(lpparam.classLoader, param.thisObject);
                    }
                });
            }
        } catch (Throwable t) {
            XposedBridge.log("HyperOS3 Localization: MediaEditor manager bean constructor hook failed: " + t);
        }

        try {
            Class<?> configRepoClass = XposedHelpers.findClassIfExists("t9.c", lpparam.classLoader);
            if (configRepoClass != null) {
                XposedBridge.hookAllMethods(configRepoClass, "b", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object result = param.getResult();
                        if (result instanceof Iterable) {
                            for (Object managerBean : (Iterable<?>) result) {
                                redirectMediaEditorManagerBean(lpparam.classLoader, managerBean);
                            }
                        }
                    }
                });
            }
        } catch (Throwable t) {
            XposedBridge.log("HyperOS3 Localization: MediaEditor config repo hook failed: " + t);
        }

        try {
            hookMediaEditorDownloaderClass(lpparam, "w9.b");
            hookMediaEditorDownloaderClass(lpparam, "w9.C3398b");
        } catch (Throwable t) {
            XposedBridge.log("HyperOS3 Localization: MediaEditor downloader hook failed: " + t);
        }
    }

    private void hookMediaEditorDownloaderClass(final XC_LoadPackage.LoadPackageParam lpparam, String className) {
        try {
            Class<?> downloadWrapClass = XposedHelpers.findClassIfExists(className, lpparam.classLoader);
            if (downloadWrapClass != null) {
                XposedBridge.hookAllMethods(downloadWrapClass, "d", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (param.args != null && param.args.length > 0) {
                            redirectMediaEditorManagerBean(lpparam.classLoader, param.args[0]);
                        }
                    }
                });
                XposedBridge.log("HyperOS3 Localization: MediaEditor downloader hook installed: " + className);
            }
        } catch (Throwable t) {
            XposedBridge.log("HyperOS3 Localization: MediaEditor downloader hook failed for " + className + ": " + t);
        }
    }

    private Object redirectMediaEditorModelKey(Object keyObject) {
        if (!(keyObject instanceof String)) {
            return keyObject;
        }
        String key = (String) keyObject;
        if (MEDIAEDITOR_MTK_REMOVER.equals(key)) {
            return MEDIAEDITOR_SD_REMOVER;
        }
        if (MEDIAEDITOR_MTK_MATTING.equals(key)) {
            return MEDIAEDITOR_SD_MATTING;
        }
        return keyObject;
    }

    private void redirectMediaEditorModelInfo(Object modelInfo) {
        if (modelInfo == null) {
            return;
        }
        try {
            Object oldKey = getFirstObjectField(modelInfo, "h", "f33463h");
            Object newKey = redirectMediaEditorModelKey(oldKey);
            if (!String.valueOf(oldKey).equals(String.valueOf(newKey))) {
                setFirstObjectField(modelInfo, newKey, "h", "f33463h");
                XposedBridge.log("HyperOS3 Localization: MediaEditor model key redirected " + oldKey + " -> " + newKey);
            }
        } catch (Throwable ignored) {
        }
    }

    private void redirectMediaEditorManagerBean(ClassLoader classLoader, Object managerBean) {
        if (managerBean == null) {
            return;
        }
        try {
            Object modelInfo = getFirstObjectField(managerBean, "a", "f5781a");
            Object oldKey = modelInfo == null ? null : getFirstObjectField(modelInfo, "h", "f33463h");
            Object newKey = redirectMediaEditorModelKey(oldKey);
            if (modelInfo != null && !String.valueOf(oldKey).equals(String.valueOf(newKey))) {
                setFirstObjectField(modelInfo, newKey, "h", "f33463h");
            }
            if (!MEDIAEDITOR_SD_REMOVER.equals(newKey) && !MEDIAEDITOR_SD_MATTING.equals(newKey)) {
                return;
            }

            Object sdConfig = loadMediaEditorLocalModelConfig(classLoader, String.valueOf(newKey));
            if (sdConfig != null) {
                setFirstObjectField(managerBean, sdConfig, "b");
                setFirstObjectField(managerBean, null, "c", "f5782c");
                setFirstLongField(managerBean, sumMediaEditorModelSize(sdConfig), "d", "f5783d");
            }
            XposedBridge.log("HyperOS3 Localization: MediaEditor manager bean redirected " + oldKey + " -> " + newKey);
        } catch (Throwable t) {
            XposedBridge.log("HyperOS3 Localization: MediaEditor manager bean redirect failed: " + t);
        }
    }

    private Object loadMediaEditorLocalModelConfig(ClassLoader classLoader, String key) {
        try {
            Class<?> localConfigClass = XposedHelpers.findClassIfExists("M8.p", classLoader);
            if (localConfigClass == null) {
                return null;
            }
            return XposedHelpers.callStaticMethod(localConfigClass, "b", key);
        } catch (Throwable t) {
            XposedBridge.log("HyperOS3 Localization: MediaEditor local model config load failed: " + t);
            return null;
        }
    }

    private long sumMediaEditorModelSize(Object modelConfig) {
        long total = 0;
        try {
            Object itemsObject = XposedHelpers.getObjectField(modelConfig, "g");
            if (itemsObject instanceof Iterable) {
                for (Object item : (Iterable<?>) itemsObject) {
                    try {
                        total += getFirstLongField(item, "c", "f16996c");
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return total;
    }

    private Object getFirstObjectField(Object target, String... names) throws NoSuchFieldException {
        Throwable lastError = null;
        for (String name : names) {
            try {
                return XposedHelpers.getObjectField(target, name);
            } catch (Throwable t) {
                lastError = t;
            }
        }
        NoSuchFieldException error = new NoSuchFieldException(target.getClass().getName() + "#" + names[0]);
        if (lastError != null) {
            error.initCause(lastError);
        }
        throw error;
    }

    private void setFirstObjectField(Object target, Object value, String... names) throws NoSuchFieldException {
        Throwable lastError = null;
        for (String name : names) {
            try {
                XposedHelpers.setObjectField(target, name, value);
                return;
            } catch (Throwable t) {
                lastError = t;
            }
        }
        NoSuchFieldException error = new NoSuchFieldException(target.getClass().getName() + "#" + names[0]);
        if (lastError != null) {
            error.initCause(lastError);
        }
        throw error;
    }

    private long getFirstLongField(Object target, String... names) throws NoSuchFieldException {
        Throwable lastError = null;
        for (String name : names) {
            try {
                return XposedHelpers.getLongField(target, name);
            } catch (Throwable t) {
                lastError = t;
            }
        }
        NoSuchFieldException error = new NoSuchFieldException(target.getClass().getName() + "#" + names[0]);
        if (lastError != null) {
            error.initCause(lastError);
        }
        throw error;
    }

    private void setFirstLongField(Object target, long value, String... names) throws NoSuchFieldException {
        Throwable lastError = null;
        for (String name : names) {
            try {
                XposedHelpers.setLongField(target, name, value);
                return;
            } catch (Throwable t) {
                lastError = t;
            }
        }
        NoSuchFieldException error = new NoSuchFieldException(target.getClass().getName() + "#" + names[0]);
        if (lastError != null) {
            error.initCause(lastError);
        }
        throw error;
    }

    private Object getMediaEditorSdHighModelType(ClassLoader classLoader) {
        try {
            Class<?> modelTypeClass = XposedHelpers.findClassIfExists("Db.c$b", classLoader);
            if (modelTypeClass == null) {
                return null;
            }
            return XposedHelpers.getStaticObjectField(modelTypeClass, "b");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void handleMiuiPackageInstaller(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> callerClass = XposedHelpers.findClassIfExists("com.miui.packageInstaller.g", lpparam.classLoader);
            if (callerClass != null) {
                XposedBridge.hookAllMethods(callerClass, "o", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (isMiuiSystemAppInstallBypassEnabled()) {
                            param.setResult(Boolean.TRUE);
                        }
                    }
                });
                XposedBridge.log("HyperOS3 Localization: MIUI installer source whitelist hook installed");
            }
        } catch (Exception e) {
            XposedBridge.log("HyperOS3 Localization: MIUI installer source whitelist hook failed: " + e);
        }

        final Class<?> apkInfoClass = XposedHelpers.findClassIfExists(
                "com.miui.packageInstaller.model.ApkInfo", lpparam.classLoader);
        final Class<?> cloudParamsClass = XposedHelpers.findClassIfExists(
                "com.miui.packageInstaller.model.CloudParams", lpparam.classLoader);
        if (apkInfoClass == null || cloudParamsClass == null) {
            XposedBridge.log("HyperOS3 Localization: MIUI installer model classes not found");
            return;
        }

        try {
            XposedBridge.hookAllMethods(apkInfoClass, "setCloudParams", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!isMiuiSystemAppInstallBypassEnabled()) {
                        return;
                    }
                    if (isSystemAppApkInfo(param.thisObject)) {
                        markCloudParamsAsSystemAppRules(param.args.length > 0 ? param.args[0] : null);
                    }
                }
            });

            XposedBridge.hookAllMethods(apkInfoClass, "getCloudParams", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!isMiuiSystemAppInstallBypassEnabled()) {
                        return;
                    }
                    if (!isSystemAppApkInfo(param.thisObject)) {
                        return;
                    }
                    Object cloudParams = param.getResult();
                    if (cloudParams == null) {
                        cloudParams = XposedHelpers.newInstance(cloudParamsClass);
                        XposedHelpers.callMethod(param.thisObject, "setCloudParams", cloudParams);
                        param.setResult(cloudParams);
                    }
                    markCloudParamsAsSystemAppRules(cloudParams);
                }
            });

            XposedBridge.hookAllMethods(apkInfoClass, "setSystemApp", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!isMiuiSystemAppInstallBypassEnabled()) {
                        return;
                    }
                    if (param.args.length > 0 && Boolean.TRUE.equals(param.args[0])) {
                        Object cloudParams = XposedHelpers.callMethod(param.thisObject, "getCloudParams");
                        markCloudParamsAsSystemAppRules(cloudParams);
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log("HyperOS3 Localization: MIUI installer ApkInfo hooks failed: " + e);
        }

        try {
            Class<?> installTaskClass = XposedHelpers.findClassIfExists("O2.C0978h", lpparam.classLoader);
            if (installTaskClass != null) {
                XposedBridge.hookAllMethods(installTaskClass, "b0", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!isMiuiSystemAppInstallBypassEnabled()) {
                            return;
                        }
                        Object apkInfo = safeCallMethod(param.thisObject, "F");
                        if (isSystemAppApkInfo(apkInfo)) {
                            markCloudParamsAsSystemAppRules(param.args.length > 0 ? param.args[0] : null);
                            XposedBridge.log("HyperOS3 Localization: MIUI installer system-app cloud params patched");
                        }
                    }
                });
                XposedBridge.hookAllMethods(installTaskClass, "H", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!isMiuiSystemAppInstallBypassEnabled()) {
                            return;
                        }
                        Object apkInfo = safeCallMethod(param.thisObject, "F");
                        if (isSystemAppApkInfo(apkInfo)) {
                            Object cloudParams = param.getResult();
                            markCloudParamsAsSystemAppRules(cloudParams);
                        }
                    }
                });
            }
        } catch (Exception e) {
            XposedBridge.log("HyperOS3 Localization: MIUI installer InstallTask hooks failed: " + e);
        }
    }

    private boolean isMiuiSystemAppInstallBypassEnabled() {
        try {
            Class<?> systemPropertiesClass = XposedHelpers.findClassIfExists("android.os.SystemProperties", null);
            Object value = XposedHelpers.callStaticMethod(systemPropertiesClass, "getBoolean",
                    "persist.hyperos3.allow_sys_app_update", Boolean.FALSE);
            return Boolean.TRUE.equals(value);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private Object safeCallMethod(Object object, String methodName) {
        if (object == null) {
            return null;
        }
        try {
            return XposedHelpers.callMethod(object, methodName);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean isSystemAppApkInfo(Object apkInfo) {
        if (apkInfo == null) {
            return false;
        }
        try {
            Object systemApp = XposedHelpers.callMethod(apkInfo, "getSystemApp");
            if (Boolean.TRUE.equals(systemApp)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            Object installedInfo = XposedHelpers.callMethod(apkInfo, "getInstalledPackageInfo");
            if (installedInfo instanceof ApplicationInfo) {
                int flags = ((ApplicationInfo) installedInfo).flags;
                return (flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        || (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private void markCloudParamsAsSystemAppRules(Object cloudParams) {
        if (cloudParams == null) {
            return;
        }
        setObjectFieldIfExists(cloudParams, "useSystemAppRules", Boolean.TRUE);
        setObjectFieldIfExists(cloudParams, "installNotAllow", Boolean.FALSE);
        setObjectFieldIfExists(cloudParams, "showAdsBefore", Boolean.FALSE);
        setObjectFieldIfExists(cloudParams, "showAdsAfter", Boolean.FALSE);
        setObjectFieldIfExists(cloudParams, "bundleApp", Boolean.FALSE);
        setObjectFieldIfExists(cloudParams, "useRegistrationPop", Boolean.FALSE);
        setObjectFieldIfExists(cloudParams, "showSafeModeTip", Boolean.FALSE);
        setObjectFieldIfExists(cloudParams, "useMiRiskyApp", Boolean.FALSE);
        setObjectFieldIfExists(cloudParams, "highPriority", Boolean.FALSE);
        setObjectFieldIfExists(cloudParams, "strategyLevel", Integer.valueOf(0));
        setObjectFieldIfExists(cloudParams, "riskType", null);
        setObjectFieldIfExists(cloudParams, "sourceRiskType", null);
        setObjectFieldIfExists(cloudParams, "installSourceTips", null);
        setObjectFieldIfExists(cloudParams, "secureWarningTip", null);
        setObjectFieldIfExists(cloudParams, "riskWarningTip", null);
        setObjectFieldIfExists(cloudParams, "riskDetailTip", null);
        setObjectFieldIfExists(cloudParams, "guideOpenEnhanceModeTip", null);
        setObjectFieldIfExists(cloudParams, "guideOpenMidModeTip", null);
    }

    private void setObjectFieldIfExists(Object object, String fieldName, Object value) {
        try {
            Field field = XposedHelpers.findFieldIfExists(object.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.set(object, value);
            }
        } catch (Throwable ignored) {
        }
    }

    // System Server Side Hooks (PM Service)
    private void handlePackageManagerService(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // PM Service is in system server classpath, so we use lpparam.classLoader
            Class<?> utilsClass = XposedHelpers.findClassIfExists("com.android.server.pm.PackageManagerServiceUtils",
                    lpparam.classLoader);
            if (utilsClass != null) {
                XposedBridge.hookAllMethods(utilsClass, "compareSignatures", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if ((Integer) param.getResult() != 0) {
                            XposedBridge.log("HyperOS3 Localization: Forcing signature match in PMUtils");
                            param.setResult(0);
                        }
                    }
                });
            }
        } catch (Exception e) {
            XposedBridge.log("HyperOS3 Localization: Hooking PMUtils failed: " + e);
        }
    }

    // Process-Local Hooks (StrictJarFile, ApkSignatureVerifier) - Safe to apply in
    // any process
    private void bypassSignatureChecks(final XC_LoadPackage.LoadPackageParam lpparam) {
        // 1. Hook ApkSignatureVerifier (Modern Android) -> Framework Class -> Use
        // BOOTCLASSLOADER (null)
        try {
            Class<?> verifierClass = XposedHelpers.findClassIfExists("android.util.apk.ApkSignatureVerifier", null); // <---
                                                                                                                     // Use
                                                                                                                     // null
                                                                                                                     // for
                                                                                                                     // BootContext
            if (verifierClass != null) {
                XposedBridge.hookAllMethods(verifierClass, "verify", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String apkPath = null;
                        if (param.args.length > 0 && param.args[0] instanceof String) {
                            apkPath = (String) param.args[0];
                        }

                        if (apkPath != null && (apkPath.contains("MiuiMms") ||
                                apkPath.contains("VoiceAssist") ||
                                apkPath.contains("HyperOS3EULocalization") ||
                                apkPath.contains("PersonalAssistant") ||
                                apkPath.contains("Weather") ||
                                apkPath.contains("ThemeManager") ||
                                apkPath.contains("Notes") ||
                                apkPath.contains("MiuiAod") ||
                                apkPath.contains("AiasstVision") ||
                                apkPath.contains("AiasstService") ||
                                apkPath.contains("Calendar") ||
                                apkPath.contains("YellowPage") ||
                                apkPath.contains("ContentExtension") ||
                                apkPath.contains("MIUIMusicT") ||
                                apkPath.contains("HybridPlatform") ||
                                apkPath.contains("MINextpay") ||
                                apkPath.contains("MITSMClient") ||
                                apkPath.contains("MipayService") ||
                                apkPath.contains("PaymentService") ||
                                apkPath.contains("UPTsmService") ||
                                apkPath.contains("ContentCatcher") ||
                                apkPath.contains("MiuiAudioMonitor") ||
                                apkPath.contains("Contacts") ||
                                apkPath.contains("SoundRecorder") ||
                                apkPath.contains("SuperMarket") ||
                                apkPath.contains("MIUIGallery") ||
                                apkPath.contains("MiuiGallery") ||
                                apkPath.contains("MiMediaEditor") ||
                                apkPath.contains("GuardProvider") ||
                                apkPath.contains("MIUIGuardProvider") ||
                                apkPath.contains("MIUIgreenguard"))) {
                            if (param.getThrowable() != null) {
                                XposedBridge.log("HyperOS3 Localization: Suppressing signature error for " + apkPath
                                        + " in " + lpparam.processName);
                                param.setThrowable(null);
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            XposedBridge.log("HyperOS3 Localization: Hooking ApkSignatureVerifier failed: " + e);
        }

        // 2. Hook StrictJarFile (File Loading) -> Framework Class -> Use
        // BOOTCLASSLOADER (null)
        try {
            Class<?> jarFileClass = XposedHelpers.findClassIfExists("android.util.jar.StrictJarFile", null); // <--- Use
                                                                                                             // null for
                                                                                                             // BootContext
            if (jarFileClass != null) {
                XposedBridge.hookAllConstructors(jarFileClass, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args.length >= 2 && param.args[0] instanceof String) {
                            String fileName = (String) param.args[0];
                            if (fileName.contains("/product/priv-app/MiuiMms") ||
                                    fileName.contains("/product/app/VoiceAssist") ||
                                    fileName.contains("HyperOS3EULocalization") ||
                                    fileName.contains("PersonalAssistant") ||
                                    fileName.contains("Weather") ||
                                    fileName.contains("ThemeManager") ||
                                    fileName.contains("Notes") ||
                                    fileName.contains("MiuiAod") ||
                                    fileName.contains("AiasstVision") ||
                                    fileName.contains("AiasstService") ||
                                    fileName.contains("Calendar") ||
                                    fileName.contains("YellowPage") ||
                                    fileName.contains("ContentExtension") ||
                                    fileName.contains("MIUIMusicT") ||
                                    fileName.contains("HybridPlatform") ||
                                    fileName.contains("MINextpay") ||
                                    fileName.contains("MITSMClient") ||
                                    fileName.contains("MipayService") ||
                                    fileName.contains("PaymentService") ||
                                    fileName.contains("UPTsmService") ||
                                    fileName.contains("ContentCatcher") ||
                                    fileName.contains("MiuiAudioMonitor") ||
                                    fileName.contains("Contacts") ||
                                    fileName.contains("SoundRecorder") ||
                                    fileName.contains("SuperMarket") ||
                                    fileName.contains("MIUIGallery") ||
                                    fileName.contains("MiuiGallery") ||
                                    fileName.contains("MiMediaEditor") ||
                                    fileName.contains("GuardProvider") ||
                                    fileName.contains("MIUIGuardProvider") ||
                                    fileName.contains("MIUIgreenguard")) {
                                param.args[1] = false; // verify = false
                                if (param.args.length >= 3) {
                                    param.args[2] = false; // signatureSchemeRollbackProtections enforcement = false
                                }
                                XposedBridge.log("HyperOS3 Localization: Bypassed StrictJarFile check for " + fileName
                                        + " in " + lpparam.processName);
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            XposedBridge.log("HyperOS3 Localization: Hooking StrictJarFile failed: " + e);
        }
    }

    private void handleSystemUI(final XC_LoadPackage.LoadPackageParam lpparam) {
        // ... (rest of code)
        // Try HyperOS 3 class name first, fallback to MIUI 14 class name
        Class<?> notificationUtilClass = XposedHelpers
                .findClassIfExists("com.android.systemui.statusbar.notification.NotificationUtil", lpparam.classLoader);
        if (notificationUtilClass == null) {
            notificationUtilClass = XposedHelpers.findClassIfExists(
                    "com.android.systemui.statusbar.notification.row.NotificationUtil", lpparam.classLoader);
        }
        if (notificationUtilClass != null) {
            final Method[] notificationUtilClassDeclaredMethods = notificationUtilClass.getDeclaredMethods();
            Method shouldSubstituteSmallIconMethod = null;
            for (Method method : notificationUtilClassDeclaredMethods) {
                if (method.getName().equals("shouldSubstituteSmallIcon")) {
                    shouldSubstituteSmallIconMethod = method;
                    break;
                }
            }
            if (shouldSubstituteSmallIconMethod != null) {
                XposedBridge.hookMethod(shouldSubstituteSmallIconMethod, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                        // Try HyperOS 3 class first, fallback to MIUI class
                        Class<?> classBuild = XposedHelpers.findClassIfExists("com.miui.systemui.BuildConfig",
                                lpparam.classLoader);
                        if (classBuild == null) {
                            classBuild = XposedHelpers.findClassIfExists("com.android.systemui.BuildConfig",
                                    lpparam.classLoader);
                        }
                        if (classBuild != null) {
                            XposedHelpers.setStaticBooleanField(classBuild, "IS_INTERNATIONAL", true);
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                        Class<?> classBuild = XposedHelpers.findClassIfExists("com.miui.systemui.BuildConfig",
                                lpparam.classLoader);
                        if (classBuild == null) {
                            classBuild = XposedHelpers.findClassIfExists("com.android.systemui.BuildConfig",
                                    lpparam.classLoader);
                        }
                        if (classBuild != null) {
                            XposedHelpers.setStaticBooleanField(classBuild, "IS_INTERNATIONAL", false);
                        }
                    }
                });
                XposedBridge.log("HyperOS3 Localization: hooked shouldSubstituteSmallIcon successfully");
            } else {
                XposedBridge.log("HyperOS3 Localization: Method not found: NotificationUtil.shouldSubstituteSmallIcon");
            }
        } else {
            XposedBridge.log("HyperOS3 Localization: Class not found: NotificationUtil (tried multiple paths)");
        }
    }

    private void handleMiuiHome(XC_LoadPackage.LoadPackageParam lpparam) {
        handleInternational(lpparam);

        // Try HyperOS 3 class names first, fallback to MIUI 14
        Class<?> miuiWidgetUtilClass = XposedHelpers.findClassIfExists("com.miui.home.launcher.MIUIWidgetUtil",
                lpparam.classLoader);
        if (miuiWidgetUtilClass == null) {
            miuiWidgetUtilClass = XposedHelpers.findClassIfExists("com.miui.home.launcher.widget.MIUIWidgetUtil",
                    lpparam.classLoader);
        }
        if (miuiWidgetUtilClass != null) {
            final Method[] MiuiWidgetUtilClassDeclaredMethods = miuiWidgetUtilClass.getDeclaredMethods();
            Method isMiuiWidgetSupportMethod = null;
            for (Method method : MiuiWidgetUtilClassDeclaredMethods) {
                if (method.getName().equals("isMIUIWidgetSupport")) {
                    isMiuiWidgetSupportMethod = method;
                    break;
                }
            }
            if (isMiuiWidgetSupportMethod != null) {
                XposedBridge.hookMethod(isMiuiWidgetSupportMethod, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                        handleChina(lpparam);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                        handleInternational(lpparam);
                    }
                });
                XposedBridge.log("HyperOS3 Localization: hooked isMIUIWidgetSupport successfully");
            } else {
                XposedBridge.log("HyperOS3 Localization: Method not found: MIUIWidgetUtil.isMIUIWidgetSupport");
            }
        } else {
            XposedBridge.log("HyperOS3 Localization: Class not found: MIUIWidgetUtil (tried multiple paths)");
        }
    }

    private void handleSecuritycore(XC_LoadPackage.LoadPackageParam lpparam) {
        handleInternational(lpparam);
        XposedHelpers.setStaticObjectField(
                XposedHelpers.findClass("com.miui.xspace.constant.XSpaceApps", lpparam.classLoader),
                "XSPACE_INTRODUCE_APPS", this.XSPACE_INTRODUCE_APPS);
    }

    private void handleSecurityCenter(final XC_LoadPackage.LoadPackageParam lpparam) {
        hookSecurityCenterWarningRequests(lpparam);
    }

    private void hookSecurityCenterWarningRequests(final XC_LoadPackage.LoadPackageParam lpparam) {
        final Class<?> netUtilClass = XposedHelpers.findClassIfExists("y8.l", lpparam.classLoader);
        if (netUtilClass == null) {
            XposedBridge.log("HyperOS3 Localization: SecurityCenter NetUtil class not found");
            return;
        }

        XposedBridge.hookAllMethods(netUtilClass, "s", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    Object currentResult = param.getResult();
                    if (currentResult instanceof String && !((String) currentResult).isEmpty()) {
                        return;
                    }
                    if (param.args == null || param.args.length < 3 ||
                            !(param.args[0] instanceof Map) ||
                            !(param.args[1] instanceof String) ||
                            !(param.args[2] instanceof String)) {
                        return;
                    }

                    String requestUrl = (String) param.args[1];
                    if (!shouldRestoreSecurityCenterRequest(requestUrl)) {
                        return;
                    }

                    String restoredResult = performSecurityCenterSignedPost(
                            lpparam.classLoader,
                            netUtilClass,
                            (Map<?, ?>) param.args[0],
                            requestUrl,
                            (String) param.args[2]);
                    if (restoredResult != null && !restoredResult.isEmpty()) {
                        param.setResult(restoredResult);
                        XposedBridge.log("HyperOS3 Localization: restored SecurityCenter request " + requestUrl);
                    }
                } catch (Throwable t) {
                    XposedBridge.log("HyperOS3 Localization: restore SecurityCenter request failed: " + t);
                }
            }
        });
    }

    private boolean shouldRestoreSecurityCenterRequest(String requestUrl) {
        if (requestUrl == null) {
            return false;
        }
        return requestUrl.contains("api.sec.miui.com") || requestUrl.contains("srv.sec.miui.com");
    }

    private String performSecurityCenterSignedPost(ClassLoader classLoader, Class<?> netUtilClass,
                                                   Map<?, ?> params, String requestUrl, String salt) throws Throwable {
        Map<String, String> requestParams = new HashMap<>();
        if (params != null) {
            for (Map.Entry<?, ?> entry : params.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (key != null && value != null) {
                    requestParams.put(String.valueOf(key), String.valueOf(value));
                }
            }
        }

        Method signMethod = netUtilClass.getDeclaredMethod("b", Map.class, String.class);
        signMethod.setAccessible(true);
        Method encodeMethod = netUtilClass.getDeclaredMethod("h", Map.class);
        encodeMethod.setAccessible(true);

        Object signedParams;
        Boolean oldInternational = null;
        Boolean oldGlobal = null;
        Class<?> buildClass = XposedHelpers.findClassIfExists("miui.os.Build", classLoader);
        try {
            if (buildClass != null) {
                oldInternational = XposedHelpers.getStaticBooleanField(buildClass, "IS_INTERNATIONAL_BUILD");
                oldGlobal = XposedHelpers.getStaticBooleanField(buildClass, "IS_GLOBAL_BUILD");
                XposedHelpers.setStaticBooleanField(buildClass, "IS_INTERNATIONAL_BUILD", false);
                XposedHelpers.setStaticBooleanField(buildClass, "IS_GLOBAL_BUILD", false);
            }
            signedParams = signMethod.invoke(null, requestParams, salt);
        } finally {
            if (buildClass != null) {
                if (oldInternational != null) {
                    XposedHelpers.setStaticBooleanField(buildClass, "IS_INTERNATIONAL_BUILD", oldInternational);
                }
                if (oldGlobal != null) {
                    XposedHelpers.setStaticBooleanField(buildClass, "IS_GLOBAL_BUILD", oldGlobal);
                }
            }
        }

        String body = (String) encodeMethod.invoke(null, signedParams);
        return postSecurityCenterForm(requestUrl, body);
    }

    private String postSecurityCenterForm(String requestUrl, String body) throws Exception {
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        InputStream inputStream = null;
        ByteArrayOutputStream buffer = null;
        try {
            connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.write(body == null ? new byte[0] : body.getBytes("UTF-8"));
            outputStream.close();
            outputStream = null;

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return "";
            }

            inputStream = connection.getInputStream();
            buffer = new ByteArrayOutputStream();
            byte[] bytes = new byte[4096];
            int read;
            while ((read = inputStream.read(bytes)) != -1) {
                buffer.write(bytes, 0, read);
            }
            return buffer.toString("UTF-8");
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (buffer != null) {
                buffer.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void handleSelf(XC_LoadPackage.LoadPackageParam lpparam) {
        final Class<?> clazz = XposedHelpers.findClass("com.lshfgj.minamigo.HyperOS3EULocalization.MainActivity",
                lpparam.classLoader);
        XposedHelpers.setStaticBooleanField(clazz, "isXposedModuleEnable", true);
    }

    private void handleInternational(XC_LoadPackage.LoadPackageParam lpparam) {
        final Class<?> classBuild = XposedHelpers.findClass("miui.os.Build", lpparam.classLoader);
        XposedHelpers.setStaticBooleanField(classBuild, "IS_INTERNATIONAL_BUILD", true);
        XposedHelpers.setStaticBooleanField(classBuild, "IS_GLOBAL_BUILD", true);
    }

    private void handleChina(XC_LoadPackage.LoadPackageParam lpparam) {
        final Class<?> classBuild = XposedHelpers.findClass("miui.os.Build", lpparam.classLoader);
        XposedHelpers.setStaticBooleanField(classBuild, "IS_INTERNATIONAL_BUILD", false);
        XposedHelpers.setStaticBooleanField(classBuild, "IS_GLOBAL_BUILD", false);
    }
}
