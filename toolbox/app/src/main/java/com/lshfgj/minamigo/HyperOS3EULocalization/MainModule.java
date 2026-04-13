package com.lshfgj.minamigo.HyperOS3EULocalization;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MainModule implements IXposedHookLoadPackage {
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
            case "com.miui.home":
                try {
                    bypassSignatureChecks(lpparam); // Add bypass here
                    handleMiuiHome(lpparam);
                } catch (Exception e) {
                    XposedBridge.log("Hooked " + pkg + " Error: " + e.toString());
                }
                break;
            case "com.android.systemui":
                try {
                    bypassSignatureChecks(lpparam); // Add bypass here
                    handleSystemUI(lpparam);
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
                    case "com.miui.contentextension":
                    case "com.miui.hybrid":
                    case "com.miui.nextpay":
                    case "com.miui.tsmclient":
                    case "com.miui.mipay":
                    case "com.android.contacts":
                    case "com.android.soundrecorder":
                    case "com.xiaomi.market":
                    case "com.miui.gallery":
                    case "com.miui.mediaeditor":
                        bypassSignatureChecks(lpparam);
                        break;
                }
                break;
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
                                apkPath.contains("ContentCatcher") ||
                                apkPath.contains("MiuiAudioMonitor") ||
                                apkPath.contains("Contacts") ||
                                apkPath.contains("SoundRecorder") ||
                                apkPath.contains("SuperMarket") ||
                                apkPath.contains("MIUIGallery") ||
                                apkPath.contains("MiuiGallery") ||
                                apkPath.contains("MiMediaEditor"))) {
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
                                    fileName.contains("ContentCatcher") ||
                                    fileName.contains("MiuiAudioMonitor") ||
                                    fileName.contains("Contacts") ||
                                    fileName.contains("SoundRecorder") ||
                                    fileName.contains("SuperMarket") ||
                                    fileName.contains("MIUIGallery") ||
                                    fileName.contains("MiuiGallery") ||
                                    fileName.contains("MiMediaEditor")) {
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
