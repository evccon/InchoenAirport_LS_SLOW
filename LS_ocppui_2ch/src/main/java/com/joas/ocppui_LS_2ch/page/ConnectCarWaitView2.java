/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 5. 2. 오후 4:29
 *
 */

package com.joas.ocppui_LS_2ch.page;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.joas.ocppui_LS_2ch.MultiChannelUIManager;
import com.joas.ocppui_LS_2ch.R;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

public class ConnectCarWaitView2 extends FrameLayout  {
    MultiChannelUIManager flowManager;
    TimeoutTimer timer = null;
    int count = 0;
    Activity mainActivity;
    final View viewThis = this;

    boolean firstvisible = true;
    int channel;

    int animationCnt = 0;
    int[] charging_icons = {R.drawable.imgload0,R.drawable.imgload1,R.drawable.imgload2,
            R.drawable.imgload3,R.drawable.imgload4,R.drawable.imgload5,R.drawable.imgload6,
            R.drawable.imgload7,R.drawable.imgload8,R.drawable.imgload9,R.drawable.imgload10,
            R.drawable.imgload11,R.drawable.imgload12,R.drawable.imgload13,R.drawable.imgload14,
            R.drawable.imgload15,R.drawable.imgload16,R.drawable.imgload17,R.drawable.imgload18
            ,R.drawable.imgload19,R.drawable.imgload20,R.drawable.imgload21,R.drawable.imgload22
            ,R.drawable.imgload23,R.drawable.imgload24,R.drawable.imgload25,R.drawable.imgload26
            ,R.drawable.imgload27,R.drawable.imgload28,R.drawable.imgload29,R.drawable.imgload30
            ,R.drawable.imgload31,R.drawable.imgload32,R.drawable.imgload33,R.drawable.imgload34
            ,R.drawable.imgload35,R.drawable.imgload36,R.drawable.imgload37,R.drawable.imgload38
            ,R.drawable.imgload39,R.drawable.imgload40,R.drawable.imgload41,R.drawable.imgload42
            ,R.drawable.imgload43,R.drawable.imgload44,R.drawable.imgload45,R.drawable.imgload46
            ,R.drawable.imgload47};
    TimeoutTimer aniTimer = null;
    ImageView imageAni;

    public ConnectCarWaitView2(Context context, MultiChannelUIManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_connect_car_wait2, this, true);

        initComponents();

        count = flowManager.getUIFlowManager(channel).getOcppConfigConnectorTimeout();

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                count--;
                if ( count == 0 )  {
                    stopTimer();
                    viewThis.setVisibility(View.INVISIBLE);
                    flowManager.getUIFlowManager(channel).onPageCommonEvent(PageEvent.GO_HOME);
                }
            }
        });

        aniTimer = new TimeoutTimer(30, new TimeoutHandler() {
            @Override
            public void run() {
                doAnimation();
            }
        });


    }

    void doAnimation() {
        imageAni.setImageResource(charging_icons[animationCnt]);
        animationCnt = (animationCnt+1) % charging_icons.length;
        imageAni.invalidate();
    }

    void initComponents() {
        imageAni = (ImageView) findViewById(R.id.imgcarwait);
    }

    void startTimer() {
        stopTimer();
        count = flowManager.getUIFlowManager(channel).getOcppConfigConnectorTimeout();
        timer.start();

        animationCnt =0;
        doAnimation();
        aniTimer.start();
    }

    public void stopTimer() {
        if ( timer != null ) timer.cancel();
        if ( aniTimer != null ) aniTimer.cancel();

    }



    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        if ( visibility == VISIBLE ) {
            channel = flowManager.pageManager.channel;
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
                if(firstvisible){
                    firstvisible =false;
                    return;
                }
            }
            startTimer();

        }
        else {
            stopTimer();
        }
    }
}
