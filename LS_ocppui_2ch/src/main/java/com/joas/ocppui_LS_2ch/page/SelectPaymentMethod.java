/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 6. 7. 오후 6:58
 *
 */

package com.joas.ocppui_LS_2ch.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.joas.ocppui_LS_2ch.CPConfig;
import com.joas.ocppui_LS_2ch.MultiChannelUIManager;
import com.joas.ocppui_LS_2ch.R;
import com.joas.ocppui_LS_2ch.TypeDefine;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

public class SelectPaymentMethod extends LinearLayout implements PageActivateListener {
    MultiChannelUIManager flowManager;

    int channel;

    TimeoutTimer timer;
    int count = 0;
    public SelectPaymentMethod(Context context, MultiChannelUIManager manager, Activity activity){
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_select_paymethod_land,this, true);
        initComponents();

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                count--;
                if ( count == 0 )  {
                    stopTimer();
                    flowManager.getUIFlowManager(channel).onPageCommonEvent(PageEvent.GO_HOME);
                }
            }
        });
    }

    void initComponents(){
        RelativeLayout btMemberCard = (RelativeLayout) findViewById(R.id.btmemcard);
        RelativeLayout btNomemCard = (RelativeLayout) findViewById(R.id.btcreditcard);

        btMemberCard.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onMemberCardClicked();
            }
        });

        btNomemCard.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreditCardClicked();
            }
        });


    }
    void onMemberCardClicked(){
        flowManager.getUIFlowManager(channel).onSelectMember();
    }
    void onCreditCardClicked(){
        flowManager.getUIFlowManager(channel).onSelectNomember();
    }

    public void stopTimer() {
        if ( timer != null ) timer.cancel();
    }

    void startTimer() {
        count = TypeDefine.DEFAULT_AUTH_TIMEOUT;
        stopTimer();
        timer.start();
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
