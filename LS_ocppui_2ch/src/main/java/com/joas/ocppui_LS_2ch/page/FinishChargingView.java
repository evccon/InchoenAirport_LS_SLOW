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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.ocppui_LS_2ch.CPConfig;
import com.joas.ocppui_LS_2ch.ChargeData;
import com.joas.ocppui_LS_2ch.MultiChannelUIManager;
import com.joas.ocppui_LS_2ch.R;
import com.joas.ocppui_LS_2ch.TypeDefine;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.text.DecimalFormat;


public class FinishChargingView extends LinearLayout implements PageActivateListener {
    MultiChannelUIManager flowManager;
    TextView tvChargeKwhVal;
    TextView tvChargeCost;
    TextView tvFinishChargingTime;

    int channel;

    public FinishChargingView(Context context, MultiChannelUIManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_finish_charging_land, this, true);

        initComponents();
    }

    void initComponents() {
        tvChargeKwhVal = (TextView)findViewById(R.id.tvChargeKwhVal);
        tvChargeCost = (TextView)findViewById(R.id.tvChargeCost);
        tvFinishChargingTime = (TextView)findViewById(R.id.tvfinishtime);


        Button btHome = (Button) findViewById(R.id.btFinishChargingHome);
        btHome.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onHomeClick();
            }
        });

    }


    /**
     * 화면 정보를 업데이트한다.
     */
    void updateDispInfo() {
        ChargeData cData = flowManager.getUIFlowManager(channel).getChargeData();
        // 충전 시간 표시
        int timeSec = (int)(cData.chargingTime / 1000);
        String strTime = String.format("%02d : %02d : %02d", timeSec / 3600, (timeSec % 3600) / 60, (timeSec % 60) );
        tvFinishChargingTime.setText(strTime);

        // 충전량 표시
        String measureVal = String.format("%.2f kWh", (double)(cData.measureWh / 1000.0) );
        tvChargeKwhVal.setText( measureVal);

        // 충전 금액
        DecimalFormat format1;
        if (cData.chargingCost > 1000) format1 = new DecimalFormat("###,###");
        else format1 = new DecimalFormat("0,000.0");
        tvChargeCost.setText(format1.format(cData.chargingCost)+" 원");

    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate(int chan) {
        channel = chan;
        updateDispInfo();
    }

    @Override
    public void onPageDeactivate() {

    }

    public void onHomeClick() {
        flowManager.getUIFlowManager(channel).onFinishChargingHomeClick();
    }
}
