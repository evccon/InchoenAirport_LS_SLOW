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

public class SelectChargingOption extends LinearLayout implements PageActivateListener {
    MultiChannelUIManager flowManager;

    int channel;

    public SelectChargingOption(Context context, MultiChannelUIManager manager, Activity activity){
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_select_chargingoption_land,this, true);
        initComponents();
    }

    void initComponents(){
        RelativeLayout btCost = (RelativeLayout) findViewById(R.id.btcost);
        RelativeLayout btkwh = (RelativeLayout) findViewById(R.id.btkwh);

        btCost.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onSelectChargeOption(true);
            }
        });

        btkwh.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onSelectChargeOption(false);
            }
        });


    }
    void onSelectChargeOption(boolean isCost) {
        flowManager.getUIFlowManager(channel).onSelectChargingOption(isCost);

    }
    @Override
    public void onPageActivate(int chan) {
        channel = chan;
    }

    @Override
    public void onPageDeactivate() {

    }
}
