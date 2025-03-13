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
import android.widget.Button;
import android.widget.FrameLayout;

import com.joas.ocppui_LS_2ch.MultiChannelUIManager;
import com.joas.ocppui_LS_2ch.R;
import com.joas.utils.LogWrapper;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

public class StopAskBoxView extends FrameLayout {
    TimeoutTimer timer = null;
    Activity mainActivity;
    int timerCount = 0;
    MultiChannelUIManager flowManager;
    final View viewThis = this;

    boolean firstvisible = true;

    int channel;

    public StopAskBoxView(Context context, MultiChannelUIManager manager, Activity activity) {
        super(context);
        mainActivity = activity;
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_stopask_box, this, true);

        timer = new TimeoutTimer(10 * 1000, new TimeoutHandler() {
            @Override
            public void run() {
                timerCount--;
                if ( timerCount == 0 ) {
                    viewThis .setVisibility(INVISIBLE);
                    if ( timer != null ) timer.cancel();
                }

            }
        });
    }

    void initMessageBox() {

        Button btYes = (Button) findViewById(R.id.btstopaskYes);
        btYes.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                flowManager.getUIFlowManager(channel).isStopByCard = true;
                LogWrapper.v("StopReason", "Pressed UI Stop Button");
                flowManager.getUIFlowManager(channel).onChargingStop();
                timer.cancel();
                viewThis .setVisibility(INVISIBLE);
            }
        });

        Button btNo = (Button) findViewById(R.id.btstopaskNo);
        btNo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                timer.cancel();
                viewThis .setVisibility(INVISIBLE);
            }
        });

        startTimer();
    }

    void startTimer() {
        if ( timer != null ) timer.cancel();
        timerCount = flowManager.getUIFlowManager(channel).getChargeData().messageBoxTimeout;
        timer.start();
    }

    // 화면에 나타날때 처리함
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
            initMessageBox();
        }
        else {
            if ( timer != null ) timer.cancel();
        }
    }

}