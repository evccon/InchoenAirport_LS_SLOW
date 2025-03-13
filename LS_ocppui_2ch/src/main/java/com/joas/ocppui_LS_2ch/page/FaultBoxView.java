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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.joas.ocppui_LS_2ch.MultiChannelUIManager;
import com.joas.ocppui_LS_2ch.R;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

public class FaultBoxView extends FrameLayout {
    Activity mainActivity;
    MultiChannelUIManager flowManager;
    TextView tvFaultBoxContent;
    Button btok;

    TimeoutTimer timer = null;
    int timerCount = 0;

    int channel;

    final View viewThis = this;

    public FaultBoxView(Context context, MultiChannelUIManager manager, Activity activity) {
        super(context);
        mainActivity = activity;
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_fault_box2, this, true);

        initComponents();

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
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

    void initComponents(){
        tvFaultBoxContent = (TextView) findViewById(R.id.tvFaultBoxContent);
        btok = (Button) findViewById(R.id.btok);
        btok.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                viewThis.setVisibility(INVISIBLE);
                if ( timer != null ) timer.cancel();
            }
        });
    }

   public void initMessageBox() {
       tvFaultBoxContent.setText(flowManager.getUIFlowManager(channel).getChargeData().faultBoxContent );
    }

    void startTimer() {
        stopTimer();
        timerCount = flowManager.getUIFlowManager(channel).getChargeData().messageBoxTimeout;
        timer.start();
    }

    void stopTimer(){
        if ( timer != null ) timer.cancel();
    }

    // 화면에 나타날때 처리함
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        if ( visibility == VISIBLE ) {
            channel = flowManager.pageManager.channel;
            initMessageBox();
            startTimer();
        }
        else {
            stopTimer();
            flowManager.getUIFlowManager(channel).getChargeData().faultBoxContent = "";
        }
    }

}