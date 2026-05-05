package com.lshfgj.minamigo.HyperOS3EULocalization;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private boolean isMagiskModuleInstalled = false, isRooted = false;
    private static boolean isXposedModuleEnable = false;

    private TextView rootStateText, appVersionNameText, magiskModuleVersionText, xposedModuleStateText;

    private String nonrootToastString;
    private String processingToastString;
    private String processSuccessedToastString;
    private String processFailedToastString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.rootStateText = (TextView) findViewById(R.id.textView_root_state);
        this.magiskModuleVersionText = (TextView) findViewById(R.id.textView_magisk_module_version);
        this.appVersionNameText = (TextView) findViewById(R.id.textView_app_version_name);
        this.xposedModuleStateText = (TextView) findViewById(R.id.textView_xposed_module_state);

        nonrootToastString = this.getString(R.string.mainactivity_toast_nonroot);
        processingToastString = this.getString(R.string.mainactivity_toast_processing);
        processSuccessedToastString = this.getString(R.string.mainactivity_toast_process_success);
        processFailedToastString = this.getString(R.string.mainactivity_toast_process_failed);

        getDeviceStatus();
        preserveVoiceAssistPowerWakeIfEnabled();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (os != null) {
                os.writeBytes("exit\n");
                os.flush();

                os.close();
            }
            if (process != null) {
                process.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void recheckDeviceStatusHandler(View view) {
        getDeviceStatus();
    }

    public void resetPermissionsHandler(View view) {
        if (!this.isRooted) {
            Toast.makeText(this, nonrootToastString, Toast.LENGTH_SHORT).show();
            return;
        } else {
            Toast.makeText(this, processingToastString, Toast.LENGTH_SHORT).show();
        }
        if (!rootCommand("rm -f /data/misc_de/0/apexdata/com.android.permission/runtime-permissions.xml")) {
            Toast.makeText(this, processFailedToastString, Toast.LENGTH_SHORT).show();
            return;
        }
        rootCommand("/system/bin/svc power reboot || /system/bin/reboot");
        Toast.makeText(this, processSuccessedToastString, Toast.LENGTH_SHORT).show();
    }

    public void fixPermissionsHandler(View view) {
        if (!this.isRooted) {
            Toast.makeText(this, nonrootToastString, Toast.LENGTH_SHORT).show();
            return;
        } else if (!isMagiskModuleInstalled) {
            Toast.makeText(this, this.getString(R.string.mainactivity_toast_not_magisk_module_installed),
                    Toast.LENGTH_SHORT).show();
            return;
        } else {
            Toast.makeText(this, processingToastString, Toast.LENGTH_SHORT).show();
        }
        if (!rootCommand("sh /system/etc/localization/tools/repairPermissions.sh")) {
            Toast.makeText(this, processFailedToastString, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, processSuccessedToastString, Toast.LENGTH_SHORT).show();
    }

    public void cleanPackageCacheHandler(View view) {
        if (!this.isRooted) {
            Toast.makeText(this, nonrootToastString, Toast.LENGTH_SHORT).show();
            return;
        } else {
            Toast.makeText(this, processingToastString, Toast.LENGTH_SHORT).show();
        }
        if (!rootCommand("rm -rf /data/system/package_cache/*")) {
            Toast.makeText(this, processFailedToastString, Toast.LENGTH_SHORT).show();
            return;
        }
        rootCommand("/system/bin/svc power reboot || /system/bin/reboot");
        Toast.makeText(this, processSuccessedToastString, Toast.LENGTH_SHORT).show();
    }

    public void resetPackageRestrictionHandler(View view) {
        if (!this.isRooted) {
            Toast.makeText(this, nonrootToastString, Toast.LENGTH_SHORT).show();
            return;
        } else {
            Toast.makeText(this, processingToastString, Toast.LENGTH_SHORT).show();
        }
        if (!rootCommand("rm -rf /data/system/users/0/package-restrictions.xml")) {
            Toast.makeText(this, processFailedToastString, Toast.LENGTH_SHORT).show();
            return;
        }
        rootCommand("/system/bin/svc power reboot || /system/bin/reboot");
        Toast.makeText(this, processSuccessedToastString, Toast.LENGTH_SHORT).show();
    }

    public void deleteErrorLogHandler(View view) {
        if (!this.isRooted) {
            Toast.makeText(this, nonrootToastString, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!rootCommand("rm -f /sdcard/HyperOS3EULocalization/ErrorLog.tar rm -rf /data/system/dropbox/*wtf@*")) {
            Toast.makeText(this, processFailedToastString, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, processSuccessedToastString, Toast.LENGTH_SHORT).show();
    }

    public void packageErrorLogHandler(View view) {
        if (!this.isRooted) {
            Toast.makeText(this, nonrootToastString, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!rootCommand(
                "[ -d /sdcard/HyperOS3EULocalization ] || mkdir /sdcard/HyperOS3EULocalization && tar -cf /sdcard/HyperOS3EULocalization/ErrorLog.tar /data/system/dropbox/*wtf@* /system/build.prop /system/etc/localization")) {
            Toast.makeText(this, processFailedToastString, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, this.getString(R.string.mainactivity_toast_package_error_log_success), Toast.LENGTH_LONG)
                .show();
    }

    public void hideThemeFolderHandler(View view) {
        if (!this.isRooted) {
            Toast.makeText(this, nonrootToastString, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!rootCommand(
                "touch /sdcard/MIUI/theme/.nomedia && rm -rf /data/user/0/com.miui.gallery/databases/*gallery* && am force-stop com.miui.gallery")) {
            Toast.makeText(this, processFailedToastString, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, processSuccessedToastString, Toast.LENGTH_SHORT).show();
    }

    public void allowSysAppUpdateHandler(View view) {
        if (!this.isRooted) {
            Toast.makeText(this, nonrootToastString, Toast.LENGTH_SHORT).show();
            return;
        }
        boolean ok = rootCommand("setprop persist.sys.allow_sys_app_update true")
                && rootCommand("settings put secure miui_optimization 0")
                && rootCommand("settings put global force_allow_on_external 1")
                && rootCommand("settings put global miui_security_mode_style off")
                && rootCommand("settings put secure pure_mode_open_time 0")
                && rootCommand("pm clear com.miui.packageinstaller");
        if (!ok) {
            Toast.makeText(this, processFailedToastString, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, processSuccessedToastString, Toast.LENGTH_SHORT).show();
    }

    public void fixUnknownSourcePermissionsHandler(View view) {
        if (!this.isRooted) {
            Toast.makeText(this, nonrootToastString, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, processingToastString, Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            String output = rootCommandForOutput(
                    "tmp=/data/local/tmp/eu_loc_restricted_apps.txt\n" +
                            "count=0\n" +
                            "(cmd appops query-op ACCESS_RESTRICTED_SETTINGS ignore 2>/dev/null || appops query-op ACCESS_RESTRICTED_SETTINGS ignore 2>/dev/null || true) > \"$tmp\"\n" +
                            "while IFS= read -r line; do\n" +
                            "  pkg=\"${line%% *}\"\n" +
                            "  if [ \"$pkg\" = \"Package\" ]; then\n" +
                            "    rest=\"${line#Package }\"\n" +
                            "    pkg=\"${rest%% *}\"\n" +
                            "  fi\n" +
                            "  pkg=\"${pkg#package:}\"\n" +
                            "  pkg=\"${pkg%:}\"\n" +
                            "  case \"$pkg\" in \"\"|*[!A-Za-z0-9._-]*) continue ;; esac\n" +
                            "  cmd package path \"$pkg\" >/dev/null 2>&1 || continue\n" +
                            "  if cmd appops set \"$pkg\" ACCESS_RESTRICTED_SETTINGS allow 2>/dev/null || appops set \"$pkg\" ACCESS_RESTRICTED_SETTINGS allow 2>/dev/null; then\n" +
                            "    count=$((count + 1))\n" +
                            "  fi\n" +
                            "done < \"$tmp\"\n" +
                            "rm -f \"$tmp\"\n" +
                            "echo FIXED_COUNT=$count\n");
            final int fixedCount = parseFixedCount(output);

            runOnUiThread(() -> {
                if (output == null || fixedCount < 0) {
                    Toast.makeText(this, processFailedToastString, Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(this,
                        this.getString(R.string.mainactivity_toast_restricted_permission_fixed, fixedCount),
                        Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    public void enableVoiceAssistPowerWakeFixHandler(View view) {
        if (!this.isRooted) {
            Toast.makeText(this, nonrootToastString, Toast.LENGTH_SHORT).show();
            return;
        } else if (!isMagiskModuleInstalled) {
            Toast.makeText(this, this.getString(R.string.mainactivity_toast_not_magisk_module_installed),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, processingToastString, Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            String output = rootCommandForOutput(
                    "module_dir=/data/adb/modules/HyperOS3EULocalization\n" +
                            "marker=\"$module_dir/voiceassist_powerwake.enabled\"\n" +
                            "[ -d \"$module_dir\" ] || exit 1\n" +
                            "if [ -f \"$marker\" ]; then\n" +
                            "  rm -f \"$marker\"\n" +
                            "  echo POWERWAKE_STATE=disabled\n" +
                            "  exit 0\n" +
                            "fi\n" +
                            "settings put global key_xiaoai_ui_settings 0\n" +
                            "settings put system is_custom_shortcut_effective 1\n" +
                            "settings put system should_filter_toolbox 1\n" +
                            "settings put system long_press_power_key launch_voice_assistant\n" +
                            "settings put system long_press_power_launch_xiaoai 1\n" +
                            "touch \"$marker\"\n" +
                            "echo POWERWAKE_STATE=enabled\n");

            runOnUiThread(() -> {
                if (output == null) {
                    Toast.makeText(this, processFailedToastString, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (output.contains("POWERWAKE_STATE=disabled")) {
                    Toast.makeText(this,
                            this.getString(R.string.mainactivity_toast_voiceassist_powerwake_disabled),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (!output.contains("POWERWAKE_STATE=enabled")) {
                    Toast.makeText(this, processFailedToastString, Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(this,
                        this.getString(R.string.mainactivity_toast_voiceassist_powerwake_enabled),
                        Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    private void preserveVoiceAssistPowerWakeIfEnabled() {
        if (!this.isRooted) {
            return;
        }

        new Thread(() -> rootCommandForOutput(
                "module_dir=/data/adb/modules/HyperOS3EULocalization\n" +
                        "marker=\"$module_dir/voiceassist_powerwake.enabled\"\n" +
                        "[ -d \"$module_dir\" ] || exit 0\n" +
                        "xiaoai_key=$(settings get global key_xiaoai_ui_settings 2>/dev/null)\n" +
                        "power_key=$(settings get system long_press_power_key 2>/dev/null)\n" +
                        "power_xiaoai=$(settings get system long_press_power_launch_xiaoai 2>/dev/null)\n" +
                        "if [ -f \"$marker\" ] || [ \"$xiaoai_key\" = \"0\" ] || { [ \"$power_key\" = \"launch_voice_assistant\" ] && [ \"$power_xiaoai\" = \"1\" ]; }; then\n" +
                        "  settings put global key_xiaoai_ui_settings 0\n" +
                        "  settings put system is_custom_shortcut_effective 1\n" +
                        "  settings put system should_filter_toolbox 1\n" +
                        "  settings put system long_press_power_key launch_voice_assistant\n" +
                        "  settings put system long_press_power_launch_xiaoai 1\n" +
                        "  touch \"$marker\"\n" +
                        "fi\n")).start();
    }

    private void getDeviceStatus() {
        getRootState();

        String appVersion = "";
        try {
            appVersion = this.getApplicationContext().getPackageManager().getPackageInfo(this.getPackageName(),
                    0).versionName;
            this.appVersionNameText.setText(appVersion);
        } catch (Exception e) {
            e.printStackTrace();
            this.appVersionNameText.setText(this.getString(R.string.mainactivity_text_check_app_version_failed));
        }

        // Check module installation by detecting module directory (supports both Magisk
        // and KernelSU)
        checkModuleInstallation(appVersion);

        if (isXposedModuleEnable) {
            this.xposedModuleStateText.setText(this.getString(R.string.mainactivity_text_xposed_module_activated));
            return;
        } else {
            this.xposedModuleStateText.setText(this.getString(R.string.mainactivity_text_xposed_module_inactivated));
        }
    }

    private void checkModuleInstallation(final String appVersion) {
        if (!isRooted) {
            this.magiskModuleVersionText
                    .setText(this.getString(R.string.mainactivity_text_magisk_module_not_installed));
            this.isMagiskModuleInstalled = false;
            return;
        }

        // Check module paths for both Magisk and KernelSU
        // Magisk: /data/adb/modules/HyperOS3EULocalization/
        // KernelSU: /data/adb/modules/HyperOS3EULocalization/
        new Thread(() -> {
            String moduleVersion = null;
            try {
                Process checkProcess = Runtime.getRuntime().exec("su");
                DataOutputStream checkOs = new DataOutputStream(checkProcess.getOutputStream());
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(checkProcess.getInputStream()));

                // Read version from module.prop
                checkOs.writeBytes(
                        "cat /data/adb/modules/HyperOS3EULocalization/module.prop 2>/dev/null | grep '^version=' | cut -d'=' -f2\n");
                checkOs.writeBytes("exit\n");
                checkOs.flush();

                String line = reader.readLine();
                if (line != null && !line.isEmpty()) {
                    moduleVersion = line.trim();
                }

                checkProcess.waitFor();
                reader.close();
                checkOs.close();
            } catch (Exception e) {
                Log.e(TAG, "Error checking module: " + e.getMessage());
            }

            final String finalModuleVersion = moduleVersion;
            runOnUiThread(() -> {
                if (finalModuleVersion != null && !finalModuleVersion.isEmpty()) {
                    this.magiskModuleVersionText.setText(finalModuleVersion);
                    this.isMagiskModuleInstalled = true;
                    if (!appVersion.equals(finalModuleVersion)) {
                        Toast.makeText(this, this.getString(R.string.mainactivity_toast_magisk_module_tools_not_match),
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    this.magiskModuleVersionText
                            .setText(this.getString(R.string.mainactivity_text_magisk_module_not_installed));
                    this.isMagiskModuleInstalled = false;
                }
            });
        }).start();
    }

    private Process process = null;
    private DataOutputStream os = null;

    private synchronized void getRootState() {
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            int exitValue = process.waitFor();
            if (exitValue == 0) {
                rootStateText.setText("已获得！");
                isRooted = true;
                process = Runtime.getRuntime().exec("su");
                os = new DataOutputStream(process.getOutputStream());
            } else {
                rootStateText.setText("未获取Root权限或未授权！");
                isRooted = false;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            rootStateText.setText("检查错误，请重试！");
            isRooted = false;
        }
    }

    private boolean rootCommand(String command) {
        if (process == null || os == null) {
            return false;
        }
        try {
            os.writeBytes(command + "\n");
            os.flush();
            Log.d("MainActivity", "Command Successed!");
            return true;
        } catch (Exception e2) {
            Log.e("MainActivity", "Command Failed: " + command + "\n" + e2.getMessage());
            return false;
        }
    }

    private String rootCommandForOutput(String command) {
        Process localProcess = null;
        DataOutputStream localOs = null;
        BufferedReader reader = null;
        try {
            localProcess = Runtime.getRuntime().exec("su");
            localOs = new DataOutputStream(localProcess.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(localProcess.getInputStream()));

            localOs.writeBytes(command + "\n");
            localOs.writeBytes("exit\n");
            localOs.flush();

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }

            int exitValue = localProcess.waitFor();
            if (exitValue != 0) {
                Log.e(TAG, "Command failed with exit code " + exitValue + ": " + command);
                return null;
            }
            return output.toString();
        } catch (Exception e) {
            Log.e(TAG, "Command failed: " + command + "\n" + e.getMessage());
            return null;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (localOs != null) {
                    localOs.close();
                }
                if (localProcess != null) {
                    localProcess.destroy();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private int parseFixedCount(String output) {
        if (output == null) {
            return -1;
        }
        String[] lines = output.split("\\n");
        for (String line : lines) {
            if (line.startsWith("FIXED_COUNT=")) {
                try {
                    return Integer.parseInt(line.substring("FIXED_COUNT=".length()).trim());
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

}
