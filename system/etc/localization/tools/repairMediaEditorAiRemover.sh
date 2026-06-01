#!/system/bin/sh

PKG=com.miui.mediaeditor
WRONG_MODEL=ai_remover_mtk_high_v2
RIGHT_MODEL=ai_remover_sd_high_v2
QCOM_MODEL=aigc_image_edgecloud_v2
RIGHT_MODEL_VERSION=4
WAIT_ROUNDS=${AI_REMOVER_FIX_WAIT_ROUNDS:-5}

find_data_dir() {
    local dir
    for dir in \
        /data/user/0/$PKG \
        /data/data/$PKG \
        /data_mirror/data_ce/null/0/$PKG; do
        if [ -d "$dir" ]; then
            echo "$dir"
            return
        fi
    done

    dumpsys package "$PKG" 2>/dev/null \
        | sed -n 's/^[[:space:]]*dataDir=//p' \
        | while read -r dir; do
            if [ -n "$dir" ] && [ -d "$dir" ]; then
                echo "$dir"
                break
            fi
        done
}

i=0
DATA_DIR="$(find_data_dir)"
while [ -z "$DATA_DIR" ] && [ "$i" -lt "$WAIT_ROUNDS" ]; do
    i=$((i + 1))
    sleep 2
    DATA_DIR="$(find_data_dir)"
done

if [ -z "$DATA_DIR" ]; then
    echo AI_REMOVER_FIX_STATE=no_data
    exit 0
fi

device="$(getprop ro.product.device 2>/dev/null)"
odm_device="$(getprop ro.product.odm.device 2>/dev/null)"
soc_model="$(getprop ro.soc.model 2>/dev/null)"
if [ "$device" != "nezha" ] && [ "$odm_device" != "nezha" ] && [ "$soc_model" != "SM8850" ]; then
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
download_failed=0

echo AI_REMOVER_FIX_DATA_DIR=$DATA_DIR

am force-stop com.miui.gallery >/dev/null 2>&1 || true
am force-stop "$PKG" >/dev/null 2>&1 || true

report_item() {
    echo "AI_REMOVER_FIX_ITEM=$1"
}

file_md5() {
    [ -f "$1" ] || return 1
    md5sum "$1" 2>/dev/null | awk '{print $1}'
}

ensure_string_pref() {
    local file="$1"
    local key="$2"
    local value="$3"
    local escaped_value

    mkdir -p "$(dirname "$file")" 2>/dev/null || true
    if [ ! -f "$file" ]; then
        {
            echo "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>"
            echo "<map>"
            echo "</map>"
        } > "$file" 2>/dev/null || return
    fi

    escaped_value="$(printf '%s' "$value" | sed 's/&/\&amp;/g; s/</\&lt;/g; s/>/\&gt;/g')"
    backup_file "$file"
    if grep -q "name=\"$key\"" "$file" 2>/dev/null; then
        sed -i "s#<string name=\"$key\">.*</string>#<string name=\"$key\">$escaped_value</string>#" "$file" 2>/dev/null || true
    else
        sed -i "s#</map>#    <string name=\"$key\">$escaped_value</string>\\
</map>#" "$file" 2>/dev/null || true
    fi
    changed=1
    found=1
}

backup_file() {
    local src="$1"
    local dst="$backup_dir/$(basename "$src")"
    [ -e "$src" ] || return
    cp -p "$src" "$dst" 2>/dev/null || cp "$src" "$dst" 2>/dev/null || true
}

clean_pref_file() {
    local file="$1"
    [ -f "$file" ] || return

    if grep -q "$WRONG_MODEL" "$file" 2>/dev/null; then
        found=1
        backup_file "$file"
        sed -i "/$WRONG_MODEL/d" "$file" 2>/dev/null && changed=1
        report_item "wrong MTK model preference: $(basename "$file")"
    fi
}

ensure_sd_high_file() {
    local name="$1"
    local rel_path="$2"
    local md5="$3"
    local url="$4"
    local model_dir="$DATA_DIR/files/aigc/$RIGHT_MODEL/$RIGHT_MODEL_VERSION"
    local dst="$model_dir/$rel_path"
    local tmp_root=/data/local/tmp/hyperos3-ai-remover-sd
    local tmp_zip="$tmp_root/$name.zip"
    local tmp_extract="$tmp_root/extract"
    local extracted="$tmp_extract/$name"

    if [ -f "$dst" ] && [ "$(file_md5 "$dst")" = "$md5" ]; then
        return 0
    fi

    mkdir -p "$(dirname "$dst")" "$tmp_extract" 2>/dev/null || return 1
    rm -f "$tmp_zip" "$extracted" 2>/dev/null || true

    if command -v curl >/dev/null 2>&1; then
        curl -L --fail --retry 2 --connect-timeout 15 -o "$tmp_zip" "$url" >/dev/null 2>&1 || return 1
    elif command -v wget >/dev/null 2>&1; then
        wget -O "$tmp_zip" "$url" >/dev/null 2>&1 || return 1
    else
        return 1
    fi

    unzip -oq "$tmp_zip" -d "$tmp_extract" >/dev/null 2>&1 || return 1
    [ -f "$extracted" ] || return 1
    if [ "$(file_md5 "$extracted")" != "$md5" ]; then
        return 1
    fi

    cp -f "$extracted" "$dst" 2>/dev/null || return 1
    chmod 0644 "$dst" 2>/dev/null || true
    changed=1
    found=1
    report_item "SD_HIGH model file: $rel_path"
    return 0
}

ensure_sd_high_model() {
    local model_dir="$DATA_DIR/files/aigc/$RIGHT_MODEL/$RIGHT_MODEL_VERSION"
    mkdir -p "$model_dir/remove_alg_cache1" 2>/dev/null || return 1

    ensure_sd_high_file "libAlgorithmRemover4.so" "libAlgorithmRemover4.so" "451f2135db336b53b1d19902223d50c4" "https://cdn.cnbj1.fds.api.mi-img.com/aigc/resources/ai_remover_sd_high_v2/4.0/libAlgorithmRemover4.so.zip" || download_failed=1
    ensure_sd_high_file "libinpainter.so" "libinpainter.so" "7b792f41553e9efcfb72c47005396d36" "https://cdn.cnbj1.fds.api.mi-img.com/aigc/resources/ai_remover_sd_high_v2/4.0/libinpainter.so.zip" || download_failed=1
    ensure_sd_high_file "libinteractiveSeg.so" "libinteractiveSeg.so" "a9f67395b8fdac15f3c47fe0253df239" "https://cdn.cnbj1.fds.api.mi-img.com/aigc/resources/ai_remover_sd_high_v2/4.0/libinteractiveSeg.so.zip" || download_failed=1
    ensure_sd_high_file "libIntersegPro.so" "libIntersegPro.so" "74d261fcbf4e1b55ef8d539595f864ff" "https://cdn.cnbj1.fds.api.mi-img.com/aigc/resources/ai_remover_sd_high_v2/4.0/libIntersegPro.so.zip" || download_failed=1
    ensure_sd_high_file "libmace.so" "libmace.so" "de79581ce0be867113468ef049850a27" "https://cdn.cnbj1.fds.api.mi-img.com/aigc/resources/ai_remover_sd_high_v2/4.0/libmace.so.zip" || download_failed=1
    ensure_sd_high_file "libmaskgenerator.so" "libmaskgenerator.so" "9082e292a79196089075b3aece73d7cc" "https://cdn.cnbj1.fds.api.mi-img.com/aigc/resources/ai_remover_sd_high_v2/4.0/libmaskgenerator.so.zip" || download_failed=1
    ensure_sd_high_file "libMNN.so" "libMNN.so" "fdbf947e8810655e68f88b5519bdfe40" "https://cdn.cnbj1.fds.api.mi-img.com/aigc/resources/ai_remover_sd_high_v2/4.0/libMNN.so.zip" || download_failed=1
    ensure_sd_high_file "libremove.so" "libremove.so" "cdf10c5254afc1bcdaa1a1e2bc2fb9cb" "https://cdn.cnbj1.fds.api.mi-img.com/aigc/resources/ai_remover_sd_high_v2/4.0/libremove.so.zip" || download_failed=1
    ensure_sd_high_file "libremove_v4.so" "libremove_v4.so" "5576489911d680d5b266bad44fa37779" "https://cdn.cnbj1.fds.api.mi-img.com/aigc/resources/ai_remover_sd_high_v2/4.0/libremove_v4.so.zip" || download_failed=1
    ensure_sd_high_file "libvis.so" "libvis.so" "767f17479ab1dfd7a0525c5aac70bb1c" "https://cdn.cnbj1.fds.api.mi-img.com/aigc/resources/ai_remover_sd_high_v2/4.0/libvis.so.zip" || download_failed=1
    ensure_sd_high_file "interactivesegPro_decoder.mnn" "remove_alg_cache1/interactivesegPro_decoder.mnn" "535efc876d0771975d2e2316d19ce990" "https://cdn.cnbj1.fds.api.mi-img.com/aigc/resources/ai_remover_sd_high_v2/3.0/remove_alg_cache1_interactivesegPro_decoder.mnn.zip" || download_failed=1
    ensure_sd_high_file "interactivesegPro_encoder.mnn" "remove_alg_cache1/interactivesegPro_encoder.mnn" "0180d9421ba9aaac8d36a5d8f4a0d4bb" "https://cdn.cnbj1.fds.api.mi-img.com/aigc/resources/ai_remover_sd_high_v2/3.0/remove_alg_cache1_interactivesegPro_encoder.mnn.zip" || download_failed=1

    if [ "$download_failed" = "0" ]; then
        ensure_string_pref "$DATA_DIR/shared_prefs/com.miui.gallery_preferences_new.xml" "$RIGHT_MODEL" "$model_dir"
        ensure_string_pref "$DATA_DIR/shared_prefs/ai4update.xml" "$RIGHT_MODEL" "$RIGHT_MODEL_VERSION"
        report_item "SD_HIGH model path preference"
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
    report_item "wrong MTK remover model directory"
fi

if [ -d "$DATA_DIR/files/aigc" ] && find "$DATA_DIR/files/aigc" -maxdepth 4 -iname "*mtk*" -print -quit 2>/dev/null | grep -q .; then
    found=1
    find "$DATA_DIR/files/aigc" -depth -maxdepth 4 -iname "*mtk*" -exec rm -rf {} + 2>/dev/null || true
    changed=1
    report_item "remaining MTK AIGC files"
fi

for cache_path in \
    "$DATA_DIR/cache/undo_redo_disk_cache/remover_pro_cache" \
    "$DATA_DIR/cache/undo_redo_disk_cache/remover_cache" \
    "$DATA_DIR/cache/remover_pro_cache" \
    "$DATA_DIR/cache/remover_cache"; do
    if [ -e "$cache_path" ]; then
        found=1
        mkdir -p "$backup_dir/cache" 2>/dev/null || true
        cp -Rp "$cache_path" "$backup_dir/cache/" 2>/dev/null || true
        rm -rf "$cache_path"
        changed=1
        report_item "remover runtime cache: $(basename "$cache_path")"
    fi
done

if [ -f "$DATA_DIR/databases/CloudConfig.db" ] && command -v sqlite3 >/dev/null 2>&1; then
    mkdir -p "$backup_dir/databases" 2>/dev/null || true
    cp -p "$DATA_DIR/databases"/CloudConfig.db* "$backup_dir/databases/" 2>/dev/null || true
    sqlite3 "$DATA_DIR/databases/CloudConfig.db" \
        "delete from cloudConfigCache where ruleId like '%$WRONG_MODEL%' or moduleKey like '%$WRONG_MODEL%' or content like '%$WRONG_MODEL%';" \
        >/dev/null 2>&1 && report_item "wrong MTK CloudConfig rows"
fi

ensure_sd_high_model

if [ "$download_failed" = "1" ]; then
    if [ -n "$uid" ]; then
        chown -R "$uid:$uid" "$DATA_DIR/files/aigc/$RIGHT_MODEL" 2>/dev/null || true
    fi
    restorecon -R "$DATA_DIR/files/aigc/$RIGHT_MODEL" 2>/dev/null || true
    echo AI_REMOVER_FIX_STATE=model_download_failed
elif [ "$changed" = "1" ]; then
    if [ -n "$uid" ]; then
        chown -R "$uid:$uid" "$DATA_DIR" 2>/dev/null || true
    fi
    restorecon -R "$DATA_DIR" 2>/dev/null || true
    echo AI_REMOVER_FIX_STATE=fixed
    echo AI_REMOVER_FIX_BACKUP=$backup_dir
elif [ "$found" = "0" ]; then
    if grep -R -q "$RIGHT_MODEL\\|$QCOM_MODEL" "$DATA_DIR/shared_prefs" "$DATA_DIR/files/aigc" 2>/dev/null; then
        echo AI_REMOVER_FIX_STATE=already_sd
    else
        echo AI_REMOVER_FIX_STATE=no_mtk_cache
    fi
else
    echo AI_REMOVER_FIX_STATE=unchanged
fi
