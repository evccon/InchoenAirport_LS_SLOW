/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 5. 2. 오후 4:29
 *
 */

package com.joas.ocppui_LS_2ch.page;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.joas.hw.payment.tl3500s.TL3500S;
import com.joas.ocppls.msg.ChangeAvailability;
import com.joas.ocppui_LS_2ch.ChargeData;
import com.joas.ocppui_LS_2ch.MultiChannelUIManager;
import com.joas.ocppui_LS_2ch.R;
import com.joas.ocppui_LS_2ch.TypeDefine;
import com.joas.utils.LogWrapper;
import com.joas.utils.NetUtil;
import com.joas.utils.RemoteUpdater;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import org.json.JSONObject;

public class SettingView extends LinearLayout implements PageActivateListener {
    MultiChannelUIManager flowManager;
    Activity mainActivity;
    TimeoutTimer timer = null;

    TextView tvCPID;
    TextView tvServerURI;
    TextView tvServerBasicAuthID;
    TextView tvServerBasicAuthPassword;
    TextView tvPassword;
    TextView tv1chstate;
    TextView tv2chstate;

    Switch switchUseBasicAuth;

    Switch switchSkipAuth;
    Switch switchSettingWatchDogUse;
    Switch switchSettingIsFaster;
    Switch switchSettingUseTl3500S;
    Switch switchSettingUseACS;
    Switch switchSettingUseSehan;

    TextView tvVersion;
    TextView tvLastErrorMsg;
    TextView tvSettingLocalIP;
    LinearLayout layoutContentView;
    TextView tvContentView;
    TextView tvDspCom;
    TextView tvRfCom;
    TextView tvModel;
    TextView tvSerial;

    Spinner chargerTypeSpinner;
    int slowchargertype = 0;


    public SettingView(Context context, MultiChannelUIManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_setting, this, true);

        initComponents();

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                updateStatus();
            }
        });
    }

    void initComponents() {
        tvCPID      = (TextView)findViewById(R.id.textSettingChargerID);
        tvServerURI  = (TextView)findViewById(R.id.textSettingServerURI);
        tvServerBasicAuthID = (TextView)findViewById(R.id.textSettingBasicAuthID);
        tvServerBasicAuthPassword = (TextView)findViewById(R.id.textSettingBasicAuthPassword);
        switchUseBasicAuth = (Switch)findViewById(R.id.switchSettingBasicAuth);

        switchSkipAuth = (Switch)findViewById(R.id.switchSettingSkipAuth);
        tvPassword    = (TextView)findViewById(R.id.textSettingPassword);

        tvVersion = (TextView)findViewById(R.id.textSettingVersion);
        tvLastErrorMsg = (TextView)findViewById(R.id.textSettingLastError);

        switchSettingWatchDogUse = (Switch)findViewById(R.id.switchSettingWatchDogUse);
        tvSettingLocalIP = (TextView)findViewById(R.id.textSettingLocalIP);
        switchSettingIsFaster = (Switch)findViewById(R.id.switchSettingIsFaster);
        switchSettingUseTl3500S = (Switch)findViewById(R.id.switchSettingUseTl3500S);
        switchSettingUseACS = (Switch)findViewById(R.id.switchSettingUseACS);
        switchSettingUseSehan = (Switch)findViewById(R.id.switchSettingUseSEHAN);

        layoutContentView = findViewById(R.id.layoutContentView);
        tvContentView = findViewById(R.id.tvContentView);

        tvDspCom= (TextView)findViewById(R.id.textDspCom);
        tvRfCom = (TextView) findViewById(R.id.textRfcom);
        tvModel = (TextView) findViewById(R.id.textmodel);
        tvSerial = (TextView) findViewById(R.id.textserial);

        tv1chstate = (TextView) findViewById(R.id.text1chflowstate);
        tv2chstate = (TextView) findViewById(R.id.text2chflowstate);

        chargerTypeSpinner = (Spinner) findViewById(R.id.spinner_chargertype);
        ArrayAdapter adapter = ArrayAdapter.createFromResource(
                mainActivity,
                R.array.chargertyps_arr,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chargerTypeSpinner.setAdapter(adapter);
        chargerTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onSelectChargerType();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        initButtonEvent();
    }





    void onSelectChargerType(){
        String tmpchgtype = chargerTypeSpinner.getSelectedItem().toString();
        if(tmpchgtype.equals("빌트인 2ch(14kW)")){
            slowchargertype = 0;
        }
        else if(tmpchgtype.equals("스탠드 동시 2ch(14kW)")){
            slowchargertype = 1;
        }
        else if(tmpchgtype.equals("스탠드 분산 2ch(14kW)")){
            slowchargertype = 2;
        }
        else if(tmpchgtype.equals("스탠드 분산 2ch(17.6kW)")){
            slowchargertype = 3;
        }
        else if(tmpchgtype.equals("스탠드 1ch(14kW)")){
            slowchargertype = 4;
        }
        else if(tmpchgtype.equals("스탠드 1ch(17.6kW)")){
            slowchargertype = 5;
        }
        else if(tmpchgtype.equals("스탠드 1ch(22kW)")){
            slowchargertype = 6;
        }
        else if(tmpchgtype.equals("월박스 1CH(7kW Alter)")){
            slowchargertype = 7;
        }
        else if(tmpchgtype.equals("월박스 1CH(7kW B/C TYPE)")){
            slowchargertype = 8;
        }
        else {
            slowchargertype = 9;
        }
    }


    void initButtonEvent() {
        Button btDataTransferTest = (Button)findViewById(R.id.btDataTransfer);
        btDataTransferTest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onDataTransferReqTest();
            }
        });

        Button btExit = (Button) findViewById(R.id.btSettingExit);
        btExit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onExitClick();
            }
        });

        Button btSave = (Button) findViewById(R.id.btSettingSave);
        btSave.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onSaveClick();
            }
        });

        Button btHideKeyboard = (Button) findViewById(R.id.btSettingHideKeyboard);
        btHideKeyboard.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onHideKeyboard();
            }
        });

        Button  btSettingMeterMNT = (Button) findViewById(R.id.btSettingMeterMNT);
        btSettingMeterMNT.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onMeterMntShow();
            }
        });

        Button  btSettingCommMNT = (Button) findViewById(R.id.btSettingCommMNT);
        btSettingCommMNT.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCommMntShow();
            }
        });

        Button btFinishApp = (Button) findViewById(R.id.btFinishApp);
        btFinishApp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCommFinishApp();
            }
        });

        Button  btSettingUpdateTest = (Button) findViewById(R.id.btSettingUpdateTest);
        btSettingUpdateTest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCommUpdateTest();
            }
        });

        Button  btSettingDspMNT  = (Button) findViewById(R.id.btSettingDspMNT);
        btSettingDspMNT.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onDspMntViewShow();
            }
        });

        Button btSettingDebugView = (Button) findViewById(R.id.btSettingDebugView);
        btSettingDebugView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onDspDebugViewShow();
            }
        });

        //for Test
        Button  btTestErr = (Button)findViewById(R.id.btTestErr);
        btTestErr.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCommTestErr();
            }
        });


        Button btViewOcppConfig = findViewById(R.id.btViewOcppConfig);
        btViewOcppConfig.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onViewOcppConfig();
            }
        });

        Button btContentViewClose = findViewById(R.id.btContentViewClose);
        btContentViewClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                layoutContentView.setVisibility(INVISIBLE);
            }
        });

        //결제관련 테스트버튼s
//        Button btPrepay = findViewById(R.id.btPrepay);
//        btPrepay.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onPrepayReq();
//            }
//        });
//        Button btPartialCancel = findViewById(R.id.btPartialCancelPay);
//        btPartialCancel.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onPartialCancelPayReq();
//            }
//        });
//        Button btNoCardCancel = findViewById(R.id.btNoncardCancel);
//        btNoCardCancel.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onNonCardCancelPayReq();
//            }
//        });
        Button btTermready = findViewById(R.id.btTermready);
        btTermready.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onTermReadyReq();
            }
        });
//        Button btSearchCard = findViewById(R.id.btSearchCard);
//        btSearchCard.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onSaerchCardNum();
//            }
//        });

        Button btReqCancelPay1 = (Button)findViewById(R.id.btcancelreq1);
        btReqCancelPay1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancelpayReq1();
            }
        });

        Button btReqCancelPay2 = (Button)findViewById(R.id.btcancelreq2);
        btReqCancelPay1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancelpayReq2();
            }
        });
    }

    void onDataTransferReqTest() {
//        flowManager.getOcppSessionManager().SendDataTransferReq(TypeDefine.DATA_TRANSFER_MESSAGEID_TIMEOFFSET, "");
    }

    //신용카드 결제 test===========================
    void onPrepayReq(){
        TL3500S tl3500S = flowManager.getTL3500S();
        tl3500S.payReq_G(1000,0,true,0);
    }
    void onPartialCancelPayReq() {
        //부분취소
        ChargeData cdata = flowManager.getUIFlowManager(0).getChargeData();
        TL3500S tl3500S = flowManager.getTL3500S();
        String prepayDatetime = cdata.paidDate+cdata.paidTime;
        tl3500S.cancelPay_Partial(0, 900, 0,
                cdata.authCode, prepayDatetime, cdata.pgTransactionSerialNo, 5);
    }
    void onNonCardCancelPayReq(){
        //무카드취소(선결제 전체 취소)
        ChargeData cdata = flowManager.getUIFlowManager(0).getChargeData();
        TL3500S tl3500S = flowManager.getTL3500S();
        String prepayDatetime = cdata.paidDate+cdata.paidTime;
        tl3500S.cancelPay_Partial(0, 1000, 0,
                cdata.authCode, prepayDatetime, cdata.pgTransactionSerialNo, 4);
    }
    void onTermReadyReq(){
        //단말기 대기
        TL3500S tl3500S = flowManager.getTL3500S();
        tl3500S.termReadyReq();
    }
    void onSaerchCardNum(){
        TL3500S tl3500s = flowManager.getTL3500S();
        tl3500s.cardInfoReq(0);
    }
    void onCancelpayReq1(){
        TL3500S tl3500S = flowManager.getTL3500S();
        tl3500S.cancelPrePay(0);
    }

    void onCancelpayReq2(){
        TL3500S tl3500S = flowManager.getTL3500S();
        tl3500S.cancelPrePay(1);
    }

    void onExitClick() {
        onHideKeyboard();

        this.setVisibility(View.INVISIBLE);
    }

    void saveCPConfig() {
        boolean oldUseWatchDog = flowManager.getCpConfig().useWatchDogTimer;
        flowManager.getCpConfig().chargerID = tvCPID.getText().toString();
        flowManager.getCpConfig().serverURI = tvServerURI.getText().toString();
        flowManager.getCpConfig().httpBasicAuthID = tvServerBasicAuthID.getText().toString();
        flowManager.getCpConfig().httpBasicAuthPassword = tvServerBasicAuthPassword.getText().toString();
        flowManager.getCpConfig().useHttpBasicAuth = switchUseBasicAuth.isChecked();

        flowManager.getCpConfig().isAuthSkip = switchSkipAuth.isChecked();
        flowManager.getCpConfig().settingPassword = tvPassword.getText().toString();
        flowManager.getCpConfig().useWatchDogTimer = switchSettingWatchDogUse.isChecked();
        flowManager.getCpConfig().isFastCharger = switchSettingIsFaster.isChecked();
        flowManager.getCpConfig().slowChargerType = slowchargertype;
        flowManager.getCpConfig().useTl3500S = switchSettingUseTl3500S.isChecked();
        flowManager.getCpConfig().useACS = switchSettingUseACS.isChecked();
        flowManager.getCpConfig().useSehan = switchSettingUseSehan.isChecked();
        flowManager.getCpConfig().dspcom = tvDspCom.getText().toString();
        flowManager.getCpConfig().rfcom = tvRfCom.getText().toString();
        flowManager.getCpConfig().chargePointModel = tvModel.getText().toString();
        flowManager.getCpConfig().chargeBoxSerial = tvSerial.getText().toString();

        flowManager.getCpConfig().saveConfig(mainActivity);
        flowManager.onSettingChanged();
        if ( oldUseWatchDog != flowManager.getCpConfig().useWatchDogTimer ) {
            if (flowManager.getCpConfig().useWatchDogTimer != false) flowManager.stopWatdogTimer();
        }
    }

    void loadCPConfig() {
        flowManager.getCpConfig().loadConfig(mainActivity);

        tvCPID.setText(flowManager.getCpConfig().chargerID);
        tvServerURI.setText(flowManager.getCpConfig().serverURI);
        tvServerBasicAuthID.setText(flowManager.getCpConfig().httpBasicAuthID);
        tvServerBasicAuthPassword.setText(flowManager.getCpConfig().httpBasicAuthPassword);
        switchUseBasicAuth.setChecked(flowManager.getCpConfig().useHttpBasicAuth);

        tvPassword.setText(flowManager.getCpConfig().settingPassword);
        switchSkipAuth.setChecked(flowManager.getCpConfig().isAuthSkip);
        switchSettingWatchDogUse.setChecked(flowManager.getCpConfig().useWatchDogTimer);
        switchSettingIsFaster.setChecked(flowManager.getCpConfig().isFastCharger);
        chargerTypeSpinner.setSelection(flowManager.getCpConfig().slowChargerType);
        switchSettingUseTl3500S.setChecked(flowManager.getCpConfig().useTl3500S);
        switchSettingUseACS.setChecked(flowManager.getCpConfig().useACS);
        switchSettingUseSehan.setChecked(flowManager.getCpConfig().useSehan);

        tvDspCom.setText(flowManager.getCpConfig().dspcom);
        tvRfCom.setText(flowManager.getCpConfig().rfcom);
        tvModel.setText(flowManager.getCpConfig().chargePointModel);
        tvSerial.setText(flowManager.getCpConfig().chargeBoxSerial);
    }

    void statusUpdate() {
        String localIP = NetUtil.getLocalIpAddress();
        if (localIP != null) tvSettingLocalIP.setText(localIP);
        tvVersion.setText(TypeDefine.SW_VER + " "+TypeDefine.SW_RELEASE_DATE);
    }

    void onHideKeyboard() {
        InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }

    public void onSaveClick() {
        final Context context = this.getContext();
        new AlertDialog.Builder(mainActivity)
                .setTitle("설정 저장")
                .setMessage("설정을 저장하시겠습니까?")
                .setPositiveButton("예", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        saveCPConfig();
                        Toast.makeText(context, "세팅 내용이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Toast.makeText(context, "취소되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    public void onCommMntShow() {
        flowManager.getPageManager().showJoasCommMonitor();
    }

    public void onMeterMntShow() {
        flowManager.getPageManager().showJoasMeterMonitor();
    }


    public void onDspMntViewShow()  {
        flowManager.getPageManager().showJoasDspMonitor();
    }

    public void onDspDebugViewShow() {
        flowManager.getPageManager().showJoasDebugView();
    }

    public void onCommFinishApp() {
        flowManager.onFinishApp();
    }

    public void onCommUpdateTest() {
        flowManager.stopWatdogTimer();
        RemoteUpdater updater = new RemoteUpdater(mainActivity, Environment.getExternalStorageDirectory()+"/Update", "update.apk");
        updater.doUpdateFromApk("com.joas.smartcharger");
    }


    int i=1;
    public void onCommTestErr() {
        TextView a = null;
        a.setText("abcd");
    }
    public void onViewOcppConfig() {
        String content = flowManager.getOcppSessionManager().getOcppConfiguration().getListAsString();
        tvContentView.setText(content);
        layoutContentView.setVisibility(VISIBLE);
    }

    void updateStatus() {
        tvLastErrorMsg.setText(LogWrapper.lastErrorMessage);
        tv1chstate.setText(flowManager.getUIFlowManager(0).getUIFlowState().toString());
        tv2chstate.setText(flowManager.getUIFlowManager(1).getUIFlowState().toString());
    }

    void startTimer() {
        if ( timer != null ) timer.cancel();
        timer.start();
    }


    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate(int chan) {
        loadCPConfig();
        statusUpdate();
        onHideKeyboard();
        startTimer();
    }

    @Override
    public void onPageDeactivate() {
        if ( timer != null ) timer.cancel();
    }
}
