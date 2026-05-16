#!/system/bin/sh

PKG=com.miui.mediaeditor
DATA_DIR=/data/user/0/$PKG
ALT_DATA_DIR=/data/data/$PKG
WRONG_MODEL=ai_remover_mtk_high_v2
RIGHT_MODEL=ai_remover_sd_high_v2

if [ ! -d "$DATA_DIR" ] && [ -d "$ALT_DATA_DIR" ]; then
    DATA_DIR="$ALT_DATA_DIR"
fi

if [ ! -d "$DATA_DIR" ]; then
    echo AI_REMOVER_FIX_STATE=no_data
    exit 0
fi

device="$(getprop ro.product.device 2>/dev/null)"
vendor_device="$(getprop ro.product.vendor.device 2>/dev/null)"
if [ "$device" != "nezha" ] && [ "$vendor_device" != "nezha" ]; then
    echo AI_REMOVER_FIX_STATE=unsupported_device
    echo AI_REMOVER_FIX_DEVICE=$device
    exit 0
fi

uid="$(dumpsys package "$PKG" 2>/dev/null | sed -n 's/.*userId=//p' | sed -n '1p' | tr -d '\r')"
backup_root=/sdcard/Download
[ -d "$backup_root" ] || backup_root=/data/local/tmp
backup_dir="$backup_root/mediaeditor-ai-fix-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$backup_dir" 2>/dev/null || backup_dir=/data/local/tmp/mediaeditor-ai-fix
mkdir -p "$backup_dir" 2>/dev/null

changed=0
found=0

am force-stop "$PKG" >/dev/null 2>&1 || true

backup_file() {
    local src="$1"
    local dst="$backup_dir/$(basename "$src")"
    [ -e "$src" ] || return
    cp -p "$src" "$dst" 2>/dev/null || cp "$src" "$dst" 2>/dev/null || true
}

clean_pref_file() {
    local file="$1"
    [ -f "$file" ] || return
    if grep -q "$WRONG_MODEL\\|aigc_config" "$file" 2>/dev/null; then
        found=1
        backup_file "$file"
        sed -i "/$WRONG_MODEL/d;/aigc_config/d" "$file" 2>/dev/null && changed=1
    fi
}

clean_pref_file "$DATA_DIR/shared_prefs/aisp.xml"
clean_pref_file "$DATA_DIR/shared_prefs/com.miui.camerainfra.cloudconfig.xml"

if [ -d "$DATA_DIR/files/aigc/$WRONG_MODEL" ]; then
    found=1
    mkdir -p "$backup_dir/aigc" 2>/dev/null || true
    cp -Rp "$DATA_DIR/files/aigc/$WRONG_MODEL" "$backup_dir/aigc/" 2>/dev/null || true
    rm -rf "$DATA_DIR/files/aigc/$WRONG_MODEL"
    changed=1
fi

if find "$DATA_DIR/files/aigc" -maxdepth 2 -name "*mtk*" -print -quit 2>/dev/null | grep -q .; then
    found=1
    find "$DATA_DIR/files/aigc" -maxdepth 2 -name "*mtk*" -exec rm -rf {} + 2>/dev/null || true
    changed=1
fi

if [ -f "$DATA_DIR/databases/CloudConfig.db" ]; then
    if grep -a -q "$WRONG_MODEL\\|aigc_config" "$DATA_DIR/databases/CloudConfig.db" 2>/dev/null; then
        found=1
        mkdir -p "$backup_dir/databases" 2>/dev/null || true
        cp -p "$DATA_DIR/databases"/CloudConfig.db* "$backup_dir/databases/" 2>/dev/null || true
        rm -f "$DATA_DIR/databases"/CloudConfig.db*
        changed=1
    fi
fi

if [ "$changed" = "1" ]; then
    if [ -n "$uid" ]; then
        chown -R "$uid:$uid" "$DATA_DIR" 2>/dev/null || true
    fi
    restorecon -R "$DATA_DIR" 2>/dev/null || true
    echo AI_REMOVER_FIX_STATE=fixed
    echo AI_REMOVER_FIX_BACKUP=$backup_dir
elif [ "$found" = "0" ]; then
    if grep -R -q "$RIGHT_MODEL" "$DATA_DIR/shared_prefs" "$DATA_DIR/files/aigc" 2>/dev/null; then
        echo AI_REMOVER_FIX_STATE=already_sd
    else
        echo AI_REMOVER_FIX_STATE=no_mtk_cache
    fi
else
    echo AI_REMOVER_FIX_STATE=unchanged
fi
