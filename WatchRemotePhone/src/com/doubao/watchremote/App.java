package com.doubao.watchremote;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import java.lang.ref.WeakReference;

/** 监听应用回到前台：每次回前台检查一次更新（没更新不弹窗） */
public class App extends Application {

    private int started = 0;
    private WeakReference<Activity> cur = new WeakReference<Activity>(null);
    private static WeakReference<Activity> sCur = new WeakReference<Activity>(null);

    /** 当前前台 Activity，供手表遥控等跨组件调用 */
    public static Activity top() {
        return sCur.get();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logx.init(this);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity a, Bundle b) {
            }

            @Override
            public void onActivityStarted(Activity a) {
                started++;
                // 手表遥控不做自动更新检查（原来那套会去问吃药提醒的版本）
                if (started == 1) {
                    Activity top = cur.get();
                }
            }

            @Override
            public void onActivityResumed(Activity a) {
                cur = new WeakReference<Activity>(a);
                sCur = new WeakReference<Activity>(a);
            }

            @Override
            public void onActivityPaused(Activity a) {
            }

            @Override
            public void onActivityStopped(Activity a) {
                started--;
            }

            @Override
            public void onActivitySaveInstanceState(Activity a, Bundle b) {
            }

            @Override
            public void onActivityDestroyed(Activity a) {
            }
        });
    }
}
