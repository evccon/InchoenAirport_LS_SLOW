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

public class ConnectorWaitView extends LinearLayout implements PageActivateListener {
    MultiChannelUIManager flowManager;
    TimeoutTimer timer = null;
    int count = 0;

    Activity mainActivity;

    TextView tvcounter;

    int channel;

    public ConnectorWaitView(Context context, MultiChannelUIManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_connector_wait_land,this, true);
        initComponents();


        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                count--;
                DecimalFormat format = new DecimalFormat("00");
                String tpcount = format.format(count);
                tvcounter.setText("유효시간: "+tpcount+"초");
                if ( count == 0 )  {
                    stopTimer();
                    flowManager.getUIFlowManager(channel).onPageCommonEvent(PageEvent.GO_HOME);
                }
            }
        });


    }



    public void stopTimer() {
        if ( timer != null ) timer.cancel();
    }

    void startTimer() {
        stopTimer();
        timer.start();
    }

    void initComponents() {
        tvcounter = (TextView) findViewById(R.id.tvcounter1);

    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate(int chan) {
        channel = chan;
        count = flowManager.getUIFlowManager(channel).getOcppConfigConnectorTimeout();
        tvcounter.setText("유효시간: "+String.valueOf(count)+"초");
        startTimer();
    }

    @Override
    public void onPageDeactivate() {
        stopTimer();
    }

}
