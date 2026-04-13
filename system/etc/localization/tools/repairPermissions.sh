#!/system/bin/sh
# HyperOS3 EU Localization - Permission Repair Script
# Grants runtime permissions and fixes restrictions for all localized CN apps

echo "Starting permission repair..."

# Grant runtime permissions
grant_perms() {
    local pkg=$1
    shift
    for perm in "$@"; do
        pm grant "$pkg" "$perm" 2>/dev/null && echo "  granted: $pkg $perm"
    done
}

# Contacts
grant_perms com.android.contacts \
    android.permission.READ_CONTACTS \
    android.permission.WRITE_CONTACTS \
    android.permission.READ_CALL_LOG \
    android.permission.WRITE_CALL_LOG \
    android.permission.CALL_PHONE \
    android.permission.READ_PHONE_STATE \
    android.permission.POST_NOTIFICATIONS

# SoundRecorder
grant_perms com.android.soundrecorder \
    android.permission.RECORD_AUDIO \
    android.permission.READ_EXTERNAL_STORAGE \
    android.permission.WRITE_EXTERNAL_STORAGE \
    android.permission.POST_NOTIFICATIONS

# VoiceAssist
grant_perms com.miui.voiceassist \
    android.permission.RECORD_AUDIO \
    android.permission.READ_CONTACTS \
    android.permission.READ_CALL_LOG \
    android.permission.READ_PHONE_STATE \
    android.permission.POST_NOTIFICATIONS \
    android.permission.READ_EXTERNAL_STORAGE

# AI Assistant Service
grant_perms com.xiaomi.aiasst.service \
    android.permission.RECORD_AUDIO \
    android.permission.READ_CONTACTS \
    android.permission.READ_CALL_LOG \
    android.permission.POST_NOTIFICATIONS

# AI Vision
grant_perms com.xiaomi.aiasst.vision \
    android.permission.CAMERA \
    android.permission.RECORD_AUDIO \
    android.permission.POST_NOTIFICATIONS

# PersonalAssistant
grant_perms com.miui.personalassistant \
    android.permission.READ_CONTACTS \
    android.permission.READ_CALENDAR \
    android.permission.POST_NOTIFICATIONS \
    android.permission.ACCESS_FINE_LOCATION

# Weather
grant_perms com.miui.weather2 \
    android.permission.ACCESS_FINE_LOCATION \
    android.permission.ACCESS_COARSE_LOCATION \
    android.permission.POST_NOTIFICATIONS

# Calendar
grant_perms com.android.calendar \
    android.permission.READ_CALENDAR \
    android.permission.WRITE_CALENDAR \
    android.permission.READ_CONTACTS \
    android.permission.POST_NOTIFICATIONS

# MMS
grant_perms com.android.mms \
    android.permission.READ_SMS \
    android.permission.SEND_SMS \
    android.permission.RECEIVE_SMS \
    android.permission.READ_CONTACTS \
    android.permission.READ_PHONE_STATE \
    android.permission.POST_NOTIFICATIONS

# Music
grant_perms com.miui.player \
    android.permission.READ_EXTERNAL_STORAGE \
    android.permission.WRITE_EXTERNAL_STORAGE \
    android.permission.POST_NOTIFICATIONS

# Notes
grant_perms com.miui.notes \
    android.permission.READ_EXTERNAL_STORAGE \
    android.permission.WRITE_EXTERNAL_STORAGE \
    android.permission.POST_NOTIFICATIONS \
    android.permission.RECORD_AUDIO

# ThemeManager
grant_perms com.android.thememanager \
    android.permission.READ_EXTERNAL_STORAGE \
    android.permission.WRITE_EXTERNAL_STORAGE \
    android.permission.POST_NOTIFICATIONS

# YellowPage
grant_perms com.miui.yellowpage \
    android.permission.READ_CONTACTS \
    android.permission.READ_CALL_LOG \
    android.permission.READ_PHONE_STATE \
    android.permission.POST_NOTIFICATIONS \
    android.permission.ACCESS_FINE_LOCATION

# ContentExtension
grant_perms com.miui.contentextension \
    android.permission.POST_NOTIFICATIONS

# Add all CN apps to battery optimization whitelist
for pkg in \
    com.miui.voiceassist \
    com.xiaomi.aiasst.service \
    com.xiaomi.aiasst.vision \
    com.miui.personalassistant \
    com.miui.weather2 \
    com.miui.player \
    com.android.calendar \
    com.android.mms \
    com.android.contacts \
    com.android.soundrecorder \
    com.miui.yellowpage \
    com.miui.notes \
    com.android.thememanager \
    com.xiaomi.market; do
    dumpsys deviceidle whitelist +"$pkg" 2>/dev/null
done

# Clear package cache to apply changes
rm -rf /data/system/package_cache/*

echo "Permission repair completed!"
echo "Some changes may require a reboot to take effect."
