##########################################################################################
#
# HyperOS3 EU Localization - Magisk/KernelSU Module Installer
# Forked from MiuiEULocalization by MinaMiGo
#
##########################################################################################

SKIPUNZIP=1
ASH_STANDALONE=1

REPLACE=""

##########################################################################################
# Helper Functions
##########################################################################################

print_banner() {
    ui_print ""
    ui_print "╔══════════════════════════════════════════════════════════════╗"
    ui_print "║                                                              ║"
    ui_print "║           HyperOS 3 EU Localization Module                   ║"
    ui_print "║                                                              ║"
    ui_print "╠══════════════════════════════════════════════════════════════╣"
    ui_print "║  Version: OS3.0.305.0                                         ║"
    ui_print "║  Author:  LSHFGJ & MinaMiGo                                  ║"
    ui_print "║  Target:  Xiaomi 17 Ultra (nezha) - xiaomi.eu HyperOS3       ║"
    ui_print "╚══════════════════════════════════════════════════════════════╝"
    ui_print ""
}

print_step() {
    ui_print "  ► $1"
}

print_success() {
    ui_print "  ✓ $1"
}

print_info() {
    ui_print "  ℹ $1"
}

# Volume Key Detection
chooseport() {
    local timeout=10
    local start_time=$(date +%s)
    : > $TMPDIR/events

    while true; do
        local elapsed=$(( $(date +%s) - start_time ))
        if [ $elapsed -ge $timeout ]; then
            return 0 # Default to YES/UP on timeout
        fi

        # Wrap getevent with /system/bin/timeout so a single call
        # cannot block longer than 1s. Without this the outer 10s
        # timeout never fires when no key is pressed (getevent -lc 1
        # blocks indefinitely waiting for input).
        /system/bin/timeout 1 /system/bin/getevent -lc 1 2>/dev/null \
            | /system/bin/grep VOLUME | /system/bin/grep " DOWN" > $TMPDIR/events
        if /system/bin/grep -q VOLUME $TMPDIR/events 2>/dev/null; then
            break
        fi
    done

    if /system/bin/grep -q VOLUMEUP $TMPDIR/events 2>/dev/null; then
        return 0
    else
        return 1
    fi
}

vk_choose() {
    ui_print "      [音量+] 是 (Yes)    [音量-] 否 (No)"
    ui_print "      (10秒内无操作将默认选择 是)"
    if chooseport; then
        return 0
    else
        return 1
    fi
}

set_config() {
    local key=$1
    local value=$2
    sed -i "s/^$key=.*/$key=$value/g" $MODPATH/HyperOS3EULocalization.ini
}

enable_all() {
    local keys="Mipay VoiceAssist PersonalAssistant Weather Calendar Music SoundRecorder ThemeManager Mms ContentExtension YellowPage AiAsst VoiceTrigger RemoveMod Fonts HybridPlatform VirtualSim MiuiIme SogouInput GboardTheme VideocallBeautify NotificationFilter Contacts AppStore Gallery MediaEditor MiPush GuardProvider GreenGuard"
    for key in $keys; do
        set_config $key "true"
    done
}

generate_default_config() {
    cat > $MODPATH/HyperOS3EULocalization.ini <<EOF
Fonts=false
Mipay=false
HybridPlatform=false
ContentExtension=false
VirtualSim=false
PersonalAssistant=false
Calendar=false
MiuiIme=false
SogouInput=false
Mms=false
YellowPage=false
AiAsst=false
VoiceAssist=false
VoiceTrigger=false
Weather=false
ThemeManager=false
GboardTheme=false
VideocallBeautify=false
NotificationFilter=false
Music=false
SoundRecorder=false
Contacts=false
AppStore=false
RemoveMod=false
Gallery=false
MediaEditor=false
MiPush=false
GuardProvider=false
GreenGuard=false
EOF
}

##########################################################################################
# Installation
##########################################################################################

print_banner

ui_print "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
print_step "Extracting module files..."
ui_print "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
unzip -o "$ZIPFILE" -x 'META-INF/*' -d $MODPATH >&2
print_success "Files extracted"
ui_print ""

# Generate default config
generate_default_config

ui_print "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
ui_print "                    功能选择 / Feature Selection"
ui_print "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
ui_print ""

ui_print "  ┌─────────────────────────────────────────────────────────────┐"
ui_print "  │  Q1: 快速安装全部功能？                                      │"
ui_print "  │      Install all features?                                  │"
ui_print "  └─────────────────────────────────────────────────────────────┘"
if vk_choose; then
    print_success "已选择：安装全部功能"
    enable_all
else
    print_info "进入自定义选择模式..."
    ui_print ""

    ui_print "  ┌─────────────────────────────────────────────────────────────┐"
    ui_print "  │  Q2: 基础服务                                               │"
    ui_print "  │      小爱同学 / 负一屏 / 短信 / 传送门 / 黄页 / 通讯录         │"
    ui_print "  └─────────────────────────────────────────────────────────────┘"
    if vk_choose; then
        print_success "已选中：基础服务"
        set_config "VoiceAssist" "true"
        set_config "PersonalAssistant" "true"
        set_config "Mms" "true"
        set_config "ContentExtension" "true"
        set_config "YellowPage" "true"
        set_config "AiAsst" "true"
        set_config "VoiceTrigger" "true"
        set_config "Contacts" "true"
    else
        print_info "已跳过"
    fi
    ui_print ""

    ui_print "  ┌─────────────────────────────────────────────────────────────┐"
    ui_print "  │  Q2.5: 小米推送 MiPush (CN 区)                              │"
    ui_print "  │      切换 XMSF 推送服务器至中国区                            │"
    ui_print "  └─────────────────────────────────────────────────────────────┘"
    if vk_choose; then
        print_success "已选中：MiPush CN 区"
        set_config "MiPush" "true"
    else
        print_info "已跳过"
    fi
    ui_print ""

    ui_print "  ┌─────────────────────────────────────────────────────────────┐"
    ui_print "  │  Q3: 小米钱包                                               │"
    ui_print "  │      钱包 / 公交卡 / MiPay                                   │"
    ui_print "  └─────────────────────────────────────────────────────────────┘"
    if vk_choose; then
        print_success "已选中：小米钱包"
        set_config "Mipay" "true"
    else
        print_info "已跳过"
    fi
    ui_print ""

    ui_print "  ┌─────────────────────────────────────────────────────────────┐"
    ui_print "  │  Q4: 国行应用商店                                           │"
    ui_print "  │      安装小米应用商店 (替代 GetApps)                          │"
    ui_print "  └─────────────────────────────────────────────────────────────┘"
    if vk_choose; then
        print_success "已选中：应用商店"
        set_config "AppStore" "true"
    else
        print_info "已跳过"
    fi
    ui_print ""

    ui_print "  ┌─────────────────────────────────────────────────────────────┐"
    ui_print "  │  Q5: 媒体与生活                                             │"
    ui_print "  │      日历 / 天气 / 音乐 / 录音机                             │"
    ui_print "  └─────────────────────────────────────────────────────────────┘"
    if vk_choose; then
        print_success "已选中：媒体与生活"
        set_config "Calendar" "true"
        set_config "Weather" "true"
        set_config "Music" "true"
        set_config "SoundRecorder" "true"
        set_config "ThemeManager" "true"
    else
        print_info "已跳过"
    fi
    ui_print ""

    ui_print "  ┌─────────────────────────────────────────────────────────────┐"
    ui_print "  │  Q6: 系统优化                                               │"
    ui_print "  │      屏蔽国际标识 / 字体 / 快应用                            │"
    ui_print "  └─────────────────────────────────────────────────────────────┘"
    if vk_choose; then
        print_success "已选中：系统优化"
        set_config "RemoveMod" "true"
        set_config "Fonts" "true"
        set_config "HybridPlatform" "true"
        set_config "SogouInput" "true"
        set_config "MiuiIme" "true"
    else
        print_info "已跳过"
    fi
    ui_print ""

    ui_print "  ┌─────────────────────────────────────────────────────────────┐"
    ui_print "  │  Q7: 国行相册 & 相册编辑器                                  │"
    ui_print "  │      CN 相册 v5.0 (Flutter新架构/宠物相册/春节特效/证件照)   │"
    ui_print "  │      CN 编辑器 v4.2.5 (比EU版新2个大版本)                   │"
    ui_print "  │      注意：需同时启用 Xposed Toolbox 模块才能生效            │"
    ui_print "  └─────────────────────────────────────────────────────────────┘"
    if vk_choose; then
        print_success "已选中：国行相册 & 编辑器"
        set_config "Gallery" "true"
        set_config "MediaEditor" "true"
    else
        print_info "已跳过"
    fi
    ui_print ""

    ui_print "  ┌─────────────────────────────────────────────────────────────┐"
    ui_print "  │  Q8: 电诈防护 & 安全守护                                    │"
    ui_print "  │      AI通话预警 / 通话防护 / 家人守护 / 安全守护              │"
    ui_print "  │      注意：需同时启用 Xposed Toolbox 模块才能补齐签名权限     │"
    ui_print "  └─────────────────────────────────────────────────────────────┘"
    if vk_choose; then
        print_success "已选中：电诈防护 & 安全守护"
        set_config "GuardProvider" "true"
        set_config "GreenGuard" "true"
    else
        print_info "已跳过"
    fi
fi

ui_print ""
ui_print "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
print_step "Installing components..."
ui_print "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Execute installation script
chmod -R 0755 $MODPATH/tools
. $MODPATH/tools/unity_install.sh

ui_print ""
ui_print "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
print_step "Cleaning up..."
ui_print "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Delete extra files
rm -rf \
$MODPATH/system/placeholder $MODPATH/customize.sh \
$MODPATH/*.md $MODPATH/.git* $MODPATH/LICENSE $MODPATH/tools $MODPATH/lang 2>/dev/null

# Set Permissions
set_perm_recursive $MODPATH 0 0 0755 0644
print_success "Installation completed"

ui_print ""
ui_print "╔══════════════════════════════════════════════════════════════╗"
ui_print "║                                                              ║"
ui_print "║              ✓ INSTALLATION COMPLETED                        ║"
ui_print "║                                                              ║"
ui_print "║  Please reboot your device to apply changes.                 ║"
ui_print "║                                                              ║"
ui_print "║  Install the Toolbox APK separately and enable the           ║"
ui_print "║  Xposed module in LSPosed for full features.                 ║"
ui_print "║                                                              ║"
ui_print "╚══════════════════════════════════════════════════════════════╝"
ui_print ""
