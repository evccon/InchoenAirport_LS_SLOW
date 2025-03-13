/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 6. 7. 오후 7:21
 *
 */

package com.joas.ocppui_LS_2ch.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.ocppui_LS_2ch.MultiChannelUIManager;
import com.joas.ocppui_LS_2ch.R;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.text.DecimalFormat;

public class PageInputCreditCard extends LinearLayout implements PageActivateListener {
    MultiChannelUIManager flowManager;
    Activity mainActivity;

    TimeoutTimer timer;
    int count;

    int channel;

    TextView tvcounter;

    public PageInputCreditCard(Context context, MultiChannelUIManager manager, Activity activity){
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_input_creditcard_land,this, true);
        initComponents();

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                count--;
                DecimalFormat format = new DecimalFormat("00");
                String tpcount = format.format(count);
                tvcounter.setText("유효시간: "+tpcount+"초");
                if ( count == 0 )  {
                    flowManager.getUIFlowManager(channel).onPageCommonEvent(PageEvent.GO_HOME);
                    stopTimer();
                }
            }
        });

    }

    void startTimer() {
        count = flowManager.getUIFlowManager(channel).getChargeData().authTimeout;
        stopTimer();
        timer.start();
    }

    public void stopTimer() {
        if (timer != null) timer.cancel();
    }


    void initComponents() {
        tvcounter = (TextView) findViewById(R.id.tvcounter);
        tvcounter.setText("유효시간: "+String.valueOf(count)+"초");
    }

    @Override
    public void onPageActivate(int chan) {
        channel = chan;
        startTimer();
    }

    @Override
    public void onPageDeactivate() {
        stopTimer();
    }
}
