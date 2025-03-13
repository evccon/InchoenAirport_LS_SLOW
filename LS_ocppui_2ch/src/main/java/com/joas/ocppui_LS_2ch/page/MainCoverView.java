/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 6. 7. 오전 10:52
 *
 */

package com.joas.ocppui_LS_2ch.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.ocppui_LS_2ch.CPConfig;
import com.joas.ocppui_LS_2ch.MultiChannelUIManager;
import com.joas.ocppui_LS_2ch.R;
import com.joas.ocppui_LS_2ch.TypeDefine;
import com.joas.ocppui_LS_2ch.UIFlowManager;

public class MainCoverView extends LinearLayout implements PageActivateListener {
    MultiChannelUIManager flowManager;

    int channel;
    public MainCoverView(Context context, MultiChannelUIManager manager, Activity activity){
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_maincover_land,this, true);

        initComponents();
    }

    void initComponents(){
        View view = findViewById(R.id.maincoverview);
        view.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                onMainPageClicked();
                return false;
            }
        });

        Button btstart = findViewById(R.id.bt_start);
        btstart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onMainPageClicked();
            }
        });

        TextView tvVersion = findViewById(R.id.tvVersionMain);
        tvVersion.setText(TypeDefine.SW_VER);
    }

    void onMainPageClicked(){
        for(UIFlowManager uiFlowManager : flowManager.getUIFlowManagers()){
            uiFlowManager.onMainPageStart();
        }
    }

    @Override
    public void onPageActivate(int chan) {
        channel = chan;
    }

    @Override
    public void onPageDeactivate() {

    }
}
