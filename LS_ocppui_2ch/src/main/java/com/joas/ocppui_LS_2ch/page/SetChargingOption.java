/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 6. 7. 오후 6:18
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

import com.joas.ocppui_LS_2ch.ChargeData;
import com.joas.ocppui_LS_2ch.MultiChannelUIManager;
import com.joas.ocppui_LS_2ch.R;
import com.joas.ocppui_LS_2ch.TypeDefine;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.text.DecimalFormat;

public class SetChargingOption extends LinearLayout implements PageActivateListener {
    MultiChannelUIManager flowManager;

    public int setStrval;
    public int setPower;

    TextView tvSetVal;
    ImageView imgindicator;
    Button btFull;
    TextView tvcost;
    TextView tvcost2;
    TextView tvUnit;

    Button bt1;
    Button bt2;
    Button bt3;
    Button bt4;
    Button bt5;
    Button bt6;

    public static final String TAG = "SetChargingOption";

    int channel;

    TimeoutTimer timer;
    int count = 0;
    public SetChargingOption(Context context, MultiChannelUIManager manager, Activity activity){
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_sel_chg_option_land,this, true);

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
        tvSetVal = (TextView) findViewById(R.id.tvsetValue);
        tvcost = (TextView) findViewById(R.id.textcost);
        tvcost2 = (TextView)findViewById(R.id.textcost2);
        tvUnit = (TextView)findViewById(R.id.tvwon);
        initButtons();
    }

    void initButtons(){
        Button btOK= findViewById(R.id.btSetOk_so);
        btOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnOKClick();
            }
        });

        btFull = findViewById(R.id.btFull_so);
        btFull.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnFullClick();
            }
        });

        bt1 = findViewById(R.id.bt1);
        bt1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnClickNum(0);
            }
        });

        bt2 = findViewById(R.id.bt2);
        bt2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnClickNum(1);
            }
        });

        bt3 = findViewById(R.id.bt3);
        bt3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnClickNum(2);
            }
        });

        bt4 = findViewById(R.id.bt4);
        bt4.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnClickNum(3);
            }
        });

        bt5 = findViewById(R.id.bt5);
        bt5.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnClickNum(4);
            }
        });

        bt6 = findViewById(R.id.bt6);
        bt6.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnClickNum(5);
            }
        });


        Button btClear = findViewById(R.id.btClear);
        btClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                OnClickNum(-1);
            }
        });


    }

    int[] won = new int[]{500,1000,3000,5000,10000,20000};
    int[] power = new int[]{5,10,20,30,50,100};

    void OnClickNum(int number){
        if(flowManager.getUIFlowManager(channel).isCostChargeOtion){
            if(number >= 0) {
                int num = won[number];
                setStrval += num;
            }
            else setStrval = 0;

            DecimalFormat format = new DecimalFormat("###,###");
            tvSetVal.setText(format.format(setStrval));

            ChargeData cdata = flowManager.getUIFlowManager(channel).getChargeData();
            double estimatedPower = Math.round((setStrval / cdata.chargingUnitCost)*100) /100.0;

            tvcost2.setText("예상 충전량 : "+estimatedPower+" kWh");
        }
        else{
            if(number >= 0) {
                int num = power[number];
                setPower += num;
            }
            else setPower = 0;

            tvSetVal.setText(""+setPower);

            ChargeData cdata = flowManager.getUIFlowManager(channel).getChargeData();
            int estimatedCost = setPower * (int)cdata.chargingUnitCost;

            DecimalFormat format = new DecimalFormat("###,###");
            tvcost2.setText("예상 금액 : "+format.format(estimatedCost)+" 원");

            setStrval = estimatedCost;
        }

    }

    void OnOKClick() {
        ChargeData cdata = flowManager.getUIFlowManager(channel).getChargeData();
        cdata.approvalRequestPrice = setStrval;
        cdata.requestPrice = cdata.approvalRequestPrice;
        if(cdata.approvalRequestPrice <= 0){
            //경고 띄우기
        }
        else{
            flowManager.getUIFlowManager(channel).onSetPrepayCostOk();
        }
    }
    void OnFullClick() {
        ChargeData cdata = flowManager.getUIFlowManager(channel).getChargeData();
        cdata.approvalRequestPrice = -1;

        flowManager.getUIFlowManager(channel).onSetPrepayCostOk();
    }

    void onStartPage(){
        ChargeData cdata = flowManager.getUIFlowManager(channel).getChargeData();
        tvcost.setText("서비스 단가 : "+cdata.chargingUnitCost+"원");

        setStrval = 0;
        setPower=0;

        tvSetVal.setText("0");

        TextView tvtitle = findViewById(R.id.texttitle);
        if(flowManager.getUIFlowManager(channel).isCostChargeOtion){
            tvtitle.setText("충전할 금액");
            tvcost2.setText("예상 충전량 : 00.00 kWh");
            tvUnit.setText("원");
        }
        else{
            tvtitle.setText("충전할 전력량");
            tvcost2.setText("예상 금액 : 000 원");
            tvUnit.setText("kWh");
        }

        if(cdata.isNomember){
            btFull.setVisibility(View.INVISIBLE);
        }
        else{
            btFull.setVisibility(View.VISIBLE);
        }

        initButtonsText();
    }

    void initButtonsText(){
        if(flowManager.getUIFlowManager(channel).isCostChargeOtion){
            bt1.setText("+500원");
            bt2.setText("+1,000원");
            bt3.setText("+3,000원");
            bt4.setText("+5,000원");
            bt5.setText("+10,000원");
            bt6.setText("+20,000원");
        }
        else{
            bt1.setText("+5kWh");
            bt2.setText("+10kWh");
            bt3.setText("+20kWh");
            bt4.setText("+30kWh");
            bt5.setText("+50kWh");
            bt6.setText("+100kWh");
        }
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
        onStartPage();

        startTimer();

    }


    @Override
    public void onPageDeactivate() {
        stopTimer();
        flowManager.rfidReaderRelease(channel);
    }
}
