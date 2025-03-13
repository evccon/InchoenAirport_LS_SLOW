/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 13 오후 1:38
 *
 */

package com.joas.ocppui_LS_2ch;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import java.lang.ref.WeakReference;

public class ImmersiveAppCompatActivity extends AppCompatActivity {
    private HideHandler mHideHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // create a handler to set immersive mode on a delay
        mHideHandler = new HideHandler(this);

        // set on create
        setToImmersiveMode();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setToImmersiveMode();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {
            mHideHandler.removeMessages(0);
            mHideHandler.sendEmptyMessageDelayed(0, 300);
        }
        else mHideHandler.removeMessages(0);
    }

    private void setToImmersiveMode() {
        // set to immersive
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private static class HideHandler extends Handler {
        private final WeakReference<ImmersiveAppCompatActivity> mActivity;

        HideHandler(ImmersiveAppCompatActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ImmersiveAppCompatActivity activity = mActivity.get();
            if(activity != null) activity.setToImmersiveMode();
        }
    }
}
