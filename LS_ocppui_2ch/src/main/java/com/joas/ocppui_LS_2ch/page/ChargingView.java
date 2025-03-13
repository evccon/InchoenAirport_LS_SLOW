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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.joas.ocppui_LS_2ch.ChargeData;
import com.joas.ocppui_LS_2ch.MultiChannelUIManager;
import com.joas.ocppui_LS_2ch.R;
import com.joas.ocppui_LS_2ch.TypeDefine;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.text.DecimalFormat;

public class ChargingView extends LinearLayout implements PageActivateListener {
    MultiChannelUIManager flowManager;
    TimeoutTimer timer = null;
    int count = 0;
    Activity mainActivity;

    TextView tvChargeKwhVal;
    TextView tvChargeCost;
    TextView tvChargeTime;

    int aminationCnt = 0;
    int[] charging_icons = {R.drawable.img_charging0, R.drawable.img_charging1, R.drawable.img_charging2,
            R.drawable.img_charging3, R.drawable.img_charging4, R.drawable.img_charging5, R.drawable.img_charging6,
            R.drawable.img_charging7, R.drawable.img_charging8, R.drawable.img_charging9, R.drawable.img_charging10,
            R.drawable.img_charging11, R.drawable.img_charging12, R.drawable.img_charging13, R.drawable.img_charging14,
            R.drawable.img_charging15};
    TimeoutTimer aniTimer = null;
    RelativeLayout imageAni;

    int channel;

    public ChargingView(Context context, MultiChannelUIManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_charging_land, this, true);

        initComponents();

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                updateDispInfo();
                count--;
                if (count == 0) {
                    flowManager.pageManager.changePage(PageID.SELECT_SLOW, channel);
                    stopTimer();
                }
            }
        });

        aniTimer = new TimeoutTimer(200, new TimeoutHandler() {
            @Override
            public void run() {
                doAnimation();
            }
        });

    }

    void doAnimation() {
        imageAni.setBackground(ActivityCompat.getDrawable(this.getContext(), charging_icons[aminationCnt]));
        aminationCnt = (aminationCnt + 1) % charging_icons.length;
        imageAni.invalidate();
    }


    void initComponents() {
        tvChargeKwhVal = (TextView) findViewById(R.id.tvChargeKwhVal);
        tvChargeCost = (TextView) findViewById(R.id.tvChargeCost);
        tvChargeTime = (TextView) findViewById(R.id.tvChargeTime);
        imageAni = (RelativeLayout) findViewById(R.id.imgcharging);

        Button btStopTag = (Button) findViewById(R.id.btCharingFinishTag);
        btStopTag.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onStopClickTag();
            }
        });
    }

    /**
     * 화면 정보를 업데이트한다.
     */
    void updateDispInfo() {
        ChargeData cData = flowManager.getUIFlowManager(channel).getChargeData();

        // 충전량 표시
        String measureVal = String.format("%.2f", (double) (cData.measureWh / 1000.0));
        tvChargeKwhVal.setText(measureVal);

        // 충전 금액
        DecimalFormat format1;
        if (cData.chargingCost > 1000) format1 = new DecimalFormat("###,###");
        else format1 = new DecimalFormat("0,000.0");
        tvChargeCost.setText(format1.format(cData.chargingCost));

        // 충전 시간 표시
        int timeSec = (int) (cData.chargingTime / 1000);
        String strTime = String.format("%02d:%02d:%02d", timeSec / 3600, (timeSec % 3600) / 60, (timeSec % 60));
        tvChargeTime.setText(strTime);

    }

    void startTimer() {
        count = TypeDefine.GO_SELECT_PAGE_TIMEOUT;
        stopTimer();
        timer.start();
        aniTimer.start();

    }

    public void stopTimer() {
        if (timer != null) timer.cancel();
        if (aniTimer != null) aniTimer.cancel();

    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate(int chan) {
        channel = chan;
        updateDispInfo();
        startTimer();
    }

    @Override
    public void onPageDeactivate() {
        stopTimer();
        tvChargeKwhVal.setText("0");
        tvChargeCost.setText("0");
        tvChargeTime.setText("00:00:00");

    }

    void onStopClickTag() {
        flowManager.getUIFlowManager(channel).onStopAsk();
    }

}
