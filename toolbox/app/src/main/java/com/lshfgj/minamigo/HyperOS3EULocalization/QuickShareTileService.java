package com.lshfgj.minamigo.HyperOS3EULocalization;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

public class QuickShareTileService extends TileService {
    private static final String GMS_PACKAGE = "com.google.android.gms";
    private static final String ACTION_QUICK_SHARE = "com.google.android.gms.nearby.QUICK_SHARE";
    private static final String ACTION_QUICK_SHARE_UNIFIED = "com.google.android.gms.nearby.sharing.UNIFIED";

    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        tile.setLabel(getString(R.string.quickshare_tile_label));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(getString(R.string.quickshare_tile_subtitle));
        }
        tile.setState(Tile.STATE_ACTIVE);
        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        unlockAndRun(this::openQuickShare);
    }

    private void openQuickShare() {
        Intent intent = resolveQuickShareIntent();
        if (intent == null) {
            Toast.makeText(this, R.string.quickshare_tile_missing_gms, Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            startActivityAndCollapse(pendingIntent);
        } else {
            startActivityAndCollapse(intent);
        }
    }

    private Intent resolveQuickShareIntent() {
        Intent quickShare = new Intent(ACTION_QUICK_SHARE)
                .setPackage(GMS_PACKAGE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (quickShare.resolveActivity(getPackageManager()) != null) {
            return quickShare;
        }

        Intent unified = new Intent(ACTION_QUICK_SHARE_UNIFIED)
                .setPackage(GMS_PACKAGE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (unified.resolveActivity(getPackageManager()) != null) {
            return unified;
        }

        Intent gmsSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(android.net.Uri.parse("package:" + GMS_PACKAGE))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (gmsSettings.resolveActivity(getPackageManager()) != null) {
            return gmsSettings;
        }
        return null;
    }
}
