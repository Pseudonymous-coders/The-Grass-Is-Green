package com.smerkous.greengrass.apps;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.AppLaunchChecker;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class InstalledApps {
    public static PackageManager manager;
    public static List<Integer> categories = new ArrayList<>();
    private static int appCount = 0;

    public static class App {
        public String name;
        public String packageName;
        public int category;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void init(final Context context) {
        manager = context.getPackageManager();
        categories.add(ApplicationInfo.CATEGORY_GAME);
        categories.add(ApplicationInfo.CATEGORY_PRODUCTIVITY);
        categories.add(ApplicationInfo.CATEGORY_SOCIAL);
        categories.add(ApplicationInfo.CATEGORY_IMAGE);
        categories.add(ApplicationInfo.CATEGORY_VIDEO);
        categories.add(ApplicationInfo.CATEGORY_AUDIO);
    }

    public static List<ApplicationInfo> getInstalledApps() {
        return manager.getInstalledApplications(PackageManager.GET_META_DATA);
    }

    public static List<ApplicationInfo> getAppsByCategories() {
        final List<ApplicationInfo> filteredApps = new ArrayList<>();
        for(ApplicationInfo app : getInstalledApps()) {
            if(categories.contains(app.category)) filteredApps.add(app);
        }
        return filteredApps;
    }

    public static List<String> getPackageNames(List<ApplicationInfo> apps) {
        final List<String> packages = new ArrayList<>();
        for(ApplicationInfo app : apps) {
            App appR = new App();
            appR.name = manager.getApplicationLabel(app).toString();
            appR.packageName = app.packageName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appR.category = app.category;
            }
            JSONObject obj = new JSONObject();
            try {
                obj.put("name", appR.name);
                obj.put("packageName", appR.packageName);
                obj.put("category", appR.category);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            packages.add(obj.toString());
        }
        return packages;
    }

    public static boolean appsChanged() {
        final List<ApplicationInfo> apps = getInstalledApps();
        if(apps.size() != appCount) {
            appCount = apps.size();
            return true;
        }
        return false;
    }


}
