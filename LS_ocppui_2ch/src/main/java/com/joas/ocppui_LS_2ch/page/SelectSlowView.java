/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 5. 2. 오후 4:29
 *
 */

package com.joas.ocppui_LS_2ch.page;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import androidx.core.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.joas.ocppui_LS_2ch.MultiChannelUIManager;
import com.joas.ocppui_LS_2ch.R;
import com.joas.ocppui_LS_2ch.TypeDefine;
import com.joas.ocppui_LS_2ch.UIFlowManager;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;


public class SelectSlowView extends LinearLayout implements PageActivateListener {
    MultiChannelUIManager flowManager;
    Activity mainActivity;

    Button bt1ch;
    Button bt2ch;
    Button[] btch = new Button[2];

    RelativeLayout layout1ch;
    RelativeLayout layout2ch;
    RelativeLayout[] layoutch = new RelativeLayout[2];

    TextView tv1ch;
    TextView tv2ch;
    TextView[] tvch = new TextView[2];

    TimeoutTimer timer = null;
    int count = 0;

    int channel;
    public SelectSlowView(Context context, MultiChannelUIManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_select_slow_land,this, true);
        initComponents();

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                count--;
                updateDispInfo();
                if ( count == 0 )  {
                    flowManager.pageManager.changePage(PageID.MAIN_COVER,channel);
                    stopTimer();
                }
            }
        });
    }

    void startTimer() {
        count = TypeDefine.DEFAULT_AUTH_TIMEOUT;
        stopTimer();
        timer.start();
    }

    public void stopTimer() {
        if ( timer != null ) timer.cancel();
    }


    @SuppressLint("ClickableViewAccessibility")
    void initComponents() {
        bt1ch = (Button) findViewById(R.id.btbtype);
        bt2ch = (Button) findViewById(R.id.btctype);
        btch = new Button[]{bt1ch, bt2ch};

        layout1ch = (RelativeLayout) findViewById(R.id.leftlayout);
        layout2ch = (RelativeLayout) findViewById(R.id.rightlayout);
        layoutch = new RelativeLayout[]{layout1ch,layout2ch};

        tv1ch = (TextView) findViewById(R.id.lefttext);
        tv2ch = (TextView) findViewById(R.id.righttext);
        tvch = new TextView[]{tv1ch,tv2ch};


        bt1ch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                on1chClick();
            }
        });
        bt2ch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                on2chClick();
            }
        });

        Button btLeftGuide = findViewById(R.id.btleftguide);
        btLeftGuide.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                bt1ch.performClick();
            }
        });
        btLeftGuide.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        bt1ch.setPressed(true);
                        break;
                    case MotionEvent.ACTION_UP:
                        bt1ch.setPressed(false);
                        break;
                }
                return false;

            }
        });

        Button btRightGuide = findViewById(R.id.btrightguide);
        btRightGuide.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                bt2ch.performClick();
            }
        });
        btRightGuide.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        bt2ch.setPressed(true);
                        break;
                    case MotionEvent.ACTION_UP:
                        bt2ch.setPressed(false);
                        break;
                }
                return false;
            }
        });


    }


    public void on1chClick() {
        flowManager.onSelectChannelEvent(0);
    }

    public void on2chClick() {
        flowManager.onSelectChannelEvent(1);
    }

    public void updateDispInfo() {
        for (UIFlowManager uiFlowManager : flowManager.getUIFlowManagers()) {
            int ch = uiFlowManager.getUIFlowChannel();
            if (uiFlowManager.getUIFlowState() == UIFlowManager.UIFlowState.UI_CHARGING) {
                btch[ch].setBackground(ActivityCompat.getDrawable(mainActivity, R.drawable.bt_inform_sel));
                layoutch[ch].setBackground(ActivityCompat.getDrawable(mainActivity, R.drawable.bg_charging));
                tvch[ch].setText("충전중");
            } else if (uiFlowManager.getUIFlowState() == UIFlowManager.UIFlowState.UI_FINISH_CHARGING ||
                    uiFlowManager.getUIFlowState() == UIFlowManager.UIFlowState.UI_UNPLUG) {
                btch[ch].setBackground(ActivityCompat.getDrawable(mainActivity, R.drawable.bt_inform_sel));
                layoutch[ch].setBackground(ActivityCompat.getDrawable(mainActivity, R.drawable.bg_finish));
                tvch[ch].setText("충전 완료");
            }
            else{
                btch[ch].setBackground(ActivityCompat.getDrawable(mainActivity, R.drawable.bt_choice_land_sel));
                layoutch[ch].setBackground(ActivityCompat.getDrawable(mainActivity, R.drawable.bg_available));
                tvch[ch].setText("충전 가능");
            }
        }

    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate(int chan) {
        channel = chan;
        updateDispInfo();

        if(!flowManager.isConnectorCharging()){
            startTimer();
        }
        flowManager.rfidReaderRelease(channel);
        flowManager.getUIFlowManager(channel).UIprocessChangeState(UIFlowManager.UIFlowState.UI_SELECT);
    }

    @Override
    public void onPageDeactivate() {
        stopTimer();
    }
}
