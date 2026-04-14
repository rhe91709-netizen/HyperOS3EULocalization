#!/system/bin/sh
MODDIR=${0%/*}

SYSTEM_VERSION=`getprop ro.system.build.version.incremental`

if [ -f $MODDIR/system/etc/localization/NotificationFilter ] ;then
    NotificationFilter=true
else
    NotificationFilter=false
fi

if [ -f $MODDIR/system/etc/localization/MiPush ] ;then
    MiPush=true
else
    MiPush=false
fi

cache_clean() {
    if [ ! -f $MODDIR/system/etc/localization/SystemVersion/$SYSTEM_VERSION ] ;then
        rm -rf /data/system/package_cache/*
        rm -rf $MODDIR/system/etc/localization/SystemVersion/*
        touch $MODDIR/system/etc/localization/SystemVersion/$SYSTEM_VERSION
    fi
}

notification_feature_process() {
    if $NotificationFilter ;then
        setprop persist.sys.notification_rank 3
        killall com.miui.notification
    fi
}

# Force install CN APKs to override EU versions with different signatures
force_install_cn_apks() {
    local TMPDIR=/data/local/tmp/eu_loc_install
    local MARKER=$MODDIR/system/etc/localization/.apks_installed_$SYSTEM_VERSION

    # Only run if not already done for this system version
    if [ -f "$MARKER" ]; then
        return
    fi

    # Wait for package manager to be ready
    while [ "$(getprop sys.boot_completed)" != "1" ]; do
        sleep 2
    done
    sleep 10

    mkdir -p $TMPDIR

    # Install conflict-causing apps first (Notes, AOD) to resolve permission conflicts
    for apk in \
        "$MODDIR/system/product/app/Notes/Notes.apk" \
        "$MODDIR/system/product/priv-app/MiuiAod/MiuiAod.apk"; do
        if [ -f "$apk" ]; then
            cp "$apk" "$TMPDIR/$(basename $apk)"
            pm install -r -d -g "$TMPDIR/$(basename $apk)" >/dev/null 2>&1
            rm -f "$TMPDIR/$(basename $apk)"
        fi
    done

    sleep 3

    # Install all CN APKs
    for apk in \
        "$MODDIR/system/product/app/VoiceAssistAndroidT/VoiceAssistAndroidT.apk" \
        "$MODDIR/system/product/app/AiasstVision/AiasstVision.apk" \
        "$MODDIR/system/product/app/MIUIAiasstService/MIUIAiasstService.apk" \
        "$MODDIR/system/product/app/HybridPlatform/HybridPlatform.apk" \
        "$MODDIR/system/product/app/MINextpay/MINextpay.apk" \
        "$MODDIR/system/product/app/MITSMClient/MITSMClient.apk" \
        "$MODDIR/system/product/app/MipayService/MipayService.apk" \
        "$MODDIR/system/product/priv-app/MIUIContentExtension/MIUIContentExtension.apk" \
        "$MODDIR/system/product/priv-app/MIUIPersonalAssistantPhoneOS3/MIUIPersonalAssistantPhoneOS3.apk" \
        "$MODDIR/system/product/priv-app/MIUIYellowPage/MIUIYellowPage.apk" \
        "$MODDIR/system/product/priv-app/MiuiMms/MiuiMms.apk" \
        "$MODDIR/system/product/data-app/MIUICalendar/MIUICalendar.apk" \
        "$MODDIR/system/product/data-app/MIUIMusicT/MIUIMusicT.apk" \
        "$MODDIR/system/product/data-app/MIUIWeather/MIUIWeather.apk" \
        "$MODDIR/system/app/ThemeManager/ThemeManager.apk" \
        "$MODDIR/system/app/MiuiAudioMonitor_36/MiuiAudioMonitor.apk" \
        "$MODDIR/system/system_ext/app/MiuiContentCatcher/MiuiContentCatcher.apk" \
        "$MODDIR/system/product/priv-app/Contacts/Contacts.apk" \
        "$MODDIR/system/product/priv-app/SoundRecorder/SoundRecorder.apk" \
        "$MODDIR/system/product/priv-app/MiuiGallery/MIUIGallery.apk" \
        "$MODDIR/system/product/app/MiMediaEditor/MiMediaEditor.apk"; do
        if [ -f "$apk" ]; then
            cp "$apk" "$TMPDIR/$(basename $apk)"
            pm install -r -d -g "$TMPDIR/$(basename $apk)" >/dev/null 2>&1
            rm -f "$TMPDIR/$(basename $apk)"
        fi
    done

    rm -rf $TMPDIR

    # Grant key runtime permissions for replaced apps
    for perm_entry in \
        "com.android.contacts android.permission.READ_CONTACTS android.permission.WRITE_CONTACTS android.permission.READ_CALL_LOG android.permission.WRITE_CALL_LOG android.permission.CALL_PHONE android.permission.READ_PHONE_STATE android.permission.POST_NOTIFICATIONS" \
        "com.android.soundrecorder android.permission.RECORD_AUDIO android.permission.READ_EXTERNAL_STORAGE android.permission.WRITE_EXTERNAL_STORAGE android.permission.POST_NOTIFICATIONS"; do
        pkg=$(echo $perm_entry | awk '{print $1}')
        for perm in $(echo $perm_entry | awk '{$1=""; print $0}'); do
            pm grant $pkg $perm >/dev/null 2>&1
        done
    done

    touch "$MARKER"
}

# Ensure key apps auto-start after boot
start_cn_services() {
    while [ "$(getprop sys.boot_completed)" != "1" ]; do
        sleep 2
    done
    sleep 15

    # Add to battery optimization whitelist
    dumpsys deviceidle whitelist +com.miui.voiceassist >/dev/null 2>&1
    dumpsys deviceidle whitelist +com.xiaomi.aiasst.service >/dev/null 2>&1
    dumpsys deviceidle whitelist +com.xiaomi.aiasst.vision >/dev/null 2>&1
    dumpsys deviceidle whitelist +com.xiaomi.market >/dev/null 2>&1

    # Preserve VoiceAssist power-key wake setting across reboots.
    # Only acts if user has enabled it inside the VoiceAssist app
    # (超级小爱 → 唤醒方式 → 电源键唤醒). We don't force-enable it.
    if [ -f $MODDIR/system/etc/localization/VoiceAssist ]; then
        (
            for i in 1 2; do
                sleep 20
                current=$(settings get global key_xiaoai_ui_settings 2>/dev/null)
                if [ "$current" = "0" ]; then
                    settings put system is_custom_shortcut_effective 1 2>/dev/null
                    settings put system should_filter_toolbox 1 2>/dev/null
                fi
            done
        ) &
    fi

    # Start VoiceAssist and AI services
    am start-foreground-service -n com.xiaomi.aiasst.service/.AiAsstService >/dev/null 2>&1
    am broadcast -a android.intent.action.BOOT_COMPLETED -p com.miui.voiceassist >/dev/null 2>&1
    am broadcast -a android.intent.action.BOOT_COMPLETED -p com.xiaomi.aiasst.service >/dev/null 2>&1
}

set_mipush_region() {
    local base_dir
    local files_dir
    local uid

    if [ -d /data/user_de/0/com.xiaomi.xmsf ]; then
        base_dir=/data/user_de/0/com.xiaomi.xmsf
    else
        base_dir=/data/data/com.xiaomi.xmsf
    fi

    files_dir="$base_dir/files"
    mkdir -p "$files_dir"

    printf '%s\n' CN > "$files_dir/mipush_country_code"
    printf '%s\n' China > "$files_dir/mipush_region"

    uid="$(dumpsys package com.xiaomi.xmsf 2>/dev/null | sed -n 's/.*userId=//p' | sed -n '1p' | tr -d '\r')"
    if [ -n "$uid" ]; then
        chown -R "$uid:$uid" "$base_dir" 2>/dev/null || true
    fi

    restorecon -R "$base_dir" 2>/dev/null || true
}

cache_clean
force_install_cn_apks &
start_cn_services &
notification_feature_process

if $MiPush ; then
    set_mipush_region
fi
