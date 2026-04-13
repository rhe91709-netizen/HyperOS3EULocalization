# This script will be executed during uninstallation, you can write your custom uninstall rules
rm -rf /data/system/package_cache/*

echo "gb" > /data/miui/cust_variant

for u in `ls /data/user/` ;do
    echo -n "CN" > /data/user/$u/com.xiaomi.xmsf/files/mipush_country_code  
    echo -n "China" > /data/user/$u/com.xiaomi.xmsf/files/mipush_region
done
sed -i "s/Europe/China/g" /data/user/*/*/shared_prefs/mipush.xml
rm -rf /data/user/*/com.xiaomi.xmsf/files/com.xiaomi.xmsf
ps -ef  | grep "com.xiaomi.xmsf" | grep -v grep |awk '{print $2}' | xargs kill -9