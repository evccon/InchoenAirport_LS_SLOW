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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.ocppui_LS_2ch.CPConfig;
import com.joas.ocppui_LS_2ch.MultiChannelUIManager;
import com.joas.ocppui_LS_2ch.R;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.text.DecimalFormat;

public class CardTagView extends LinearLayout implements PageActivateListener {
    MultiChannelUIManager flowManager;
    TimeoutTimer timer;
    int count;

    Activity mainActivity;
    
    TextView tvcounter;

    int channel;

    public CardTagView(Context context, MultiChannelUIManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_cardtag_land,this, true);

        initComponents();

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                count--;
                DecimalFormat format = new DecimalFormat("00");
                String tpcount = format.format(count);
                tvcounter.setText("유효시간: "+tpcount+"초");
                if ( count == 0 )  {
                    flowManager.getUIFlowManager(channel).onAuthTimerOverEvent();
                    stopTimer();
                }
            }
        });

    }



    void initComponents() {
        tvcounter = (TextView) findViewById(R.id.tvcounter);
    }

    void startTimer() {
        count = flowManager.getUIFlowManager(channel).getChargeData().authTimeout;
        stopTimer();
        timer.start();
    }

    public void stopTimer() {
        if (timer != null) timer.cancel();
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate(int chan) {
        channel = chan;
        startTimer();
        tvcounter.setText("유효시간: "+String.valueOf(count)+"초");

        CPConfig cpConfig = flowManager.getCpConfig();
        if(cpConfig.useTl3500S) flowManager.tl3500s.cardInfoReq(channel);
        flowManager.rfidReaderRequest(channel);

    }

    @Override
    public void onPageDeactivate() {
        stopTimer();
        CPConfig cpConfig = flowManager.getCpConfig();
        if(cpConfig.useTl3500S) flowManager.tl3500s.termReadyReq();
        flowManager.rfidReaderRelease(channel);

    }

}
