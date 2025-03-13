/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 13 오후 2:00
 *
 */

package com.joas.ocppui_LS_2ch;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.joas.hw.dsp2.DSPControl2;
import com.joas.hw.dsp2.DSPControl2Listener;
import com.joas.hw.dsp2.DSPRxData2;
import com.joas.hw.payment.tl3500s.TL3500S;
import com.joas.hw.payment.tl3500s.TL3500SListener;
import com.joas.hw.rfid.RfidReader;
import com.joas.hw.rfid.RfidReaderACM1281S;
import com.joas.hw.rfid.RfidReaderListener;
import com.joas.hw.rfid.RfidReaderSehan;
import com.joas.ocppls.chargepoint.OCPPSession;
import com.joas.ocppls.chargepoint.OCPPSessionManager;
import com.joas.ocppls.chargepoint.OCPPSessionManagerListener;
import com.joas.ocppls.msg.CancelReservationResponse;
import com.joas.ocppls.msg.ChangeAvailability;
import com.joas.ocppls.msg.ChargingProfile;
import com.joas.ocppls.msg.DataTransferResponse;
import com.joas.ocppls.msg.FirmwareStatusNotification;
import com.joas.ocppls.msg.IdTagInfo;
import com.joas.ocppls.msg.ReserveNowResponse;
import com.joas.ocppls.msg.StatusNotification;
import com.joas.ocppls.msg.StopTransaction;
import com.joas.ocppls.msg.TriggerMessage;
import com.joas.ocppls.stack.OCPPDiagnosticManager;
import com.joas.ocppls.stack.OCPPMessage;
import com.joas.ocppls.stack.OCPPStackProperty;
import com.joas.ocppls.stack.OCPPTransportMonitorListener;
import com.joas.ocppui_LS_2ch.page.PageEvent;
import com.joas.ocppui_LS_2ch.page.PageID;
import com.joas.utils.LogWrapper;
import com.joas.utils.LogWrapperListener;
import com.joas.utils.LogWrapperMsg;
import com.joas.utils.NetUtil;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;
import com.joas.utils.WatchDogTimer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

public class MultiChannelUIManager implements RfidReaderListener, DSPControl2Listener, LogWrapperListener,
        OCPPSessionManagerListener, UpdateManagerListener, TL3500SListener, OCPPTransportMonitorListener {
    public static final String TAG = "MultiChannelUIManager";

    OCPPUI2CHActivity mainActivity;


    // WatchDog Timer 세팅
    int watchDogTimerStartCnt = 0;
    WatchDogTimer watchDogTimer = new WatchDogTimer();


    TimeoutTimer timerSec = null;

    // DSP 관련 Attr
    DSPControl2 dspControl;

    boolean isHardResetEvent = false;
    boolean restartBySoftReset = false;

    OCPPSessionManager ocppSessionManager;

    UpdateManager updateManager;
    int firmwareInstallCounter = 0;

    public PageManager pageManager;
    CPConfig cpConfig;
    MeterConfig mConfig;

    //RFID Reader
    RfidReader rfidReader;
    int rfidReaderSelect = -1;

    UIFlowManager[] uiFlowManager = new UIFlowManager[TypeDefine.MAX_CHANNEL];

    int lastDateValue = 0;
    int timeOffset = 0;
    public TL3500S tl3500s;

    public MultiChannelUIManager(OCPPUI2CHActivity activity, PageManager pages, ChargeData[] datas, CPConfig config, MeterConfig mconfig, String restartReason) {
        mainActivity = activity;
        cpConfig = config;
        pageManager = pages;
        mConfig = mconfig;

        LogWrapper.setLogWrapperListener(this);

        if (restartReason != null && restartReason.equals("SoftReset")) restartBySoftReset = true;

        if (cpConfig.isFastCharger == true) {
            dspControl = new DSPControl2(TypeDefine.MAX_CHANNEL, "/dev/ttyS" + cpConfig.dspcom, DSPControl2.DSP_VER_REG17_SIZE, DSPControl2.DSP_VER_TXREG_V28_SIZE, 10, this);
            dspControl.setMeterType(DSPControl2.METER_TYPE_FAST);
        } else {
            if (cpConfig.chargePointModel.equals("JC-9111KE-TP-BC") || cpConfig.chargePointModel.equals("JC-9511KE-TP-BC")) {
                if (cpConfig.dspcom.contains("USB"))
                    dspControl = new DSPControl2(TypeDefine.MAX_CHANNEL, "/dev/tty" + cpConfig.dspcom, DSPControl2.DSP_VER_REG17_SIZE, DSPControl2.DSP_VER_TXREG_OLDVERSION_SIZE, 1, this);
                else
                    dspControl = new DSPControl2(TypeDefine.MAX_CHANNEL, "/dev/ttyS" + cpConfig.dspcom, DSPControl2.DSP_VER_REG17_SIZE, DSPControl2.DSP_VER_TXREG_OLDVERSION_SIZE, 1, this);
            } else {
                if (cpConfig.dspcom.contains("USB"))
                    dspControl = new DSPControl2(TypeDefine.MAX_CHANNEL, "/dev/tty" + cpConfig.dspcom, DSPControl2.DSP_VER_REG17_SIZE, DSPControl2.DSP_VER_TXREG_DEFAULT_SIZE, 1, this);
                else
                    dspControl = new DSPControl2(TypeDefine.MAX_CHANNEL, "/dev/ttyS" + cpConfig.dspcom, DSPControl2.DSP_VER_REG17_SIZE, DSPControl2.DSP_VER_TXREG_DEFAULT_SIZE, 1, this);
            }
            dspControl.setMeterType(DSPControl2.METER_TYPE_SLOW);
        }
        // 미터값을 DSP에서 가져온다.
        dspControl.setMeterUse(false);
        dspControl.start();

        if (cpConfig.useTl3500S) {
            tl3500s = new TL3500S(2, "/dev/ttyS" + cpConfig.rfcom);
            tl3500s.setListener(this);
            tl3500s.start();

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    tl3500s.getVersionReq();
                }
            }, 500);
        } else {
            if (cpConfig.useSehan) {
                rfidReader = new RfidReaderSehan("/dev/ttyS" + cpConfig.rfcom, RfidReaderSehan.RFID_CMD.RFID_AUTO_TMONEY);
//                rfidReader = new RfidReaderSehan("/dev/ttyS"+cpConfig.rfcom, RfidReaderSehan.RFID_CMD.RFID_AUTO_MYFARE);
            } else if (cpConfig.useACS) {
                rfidReader = new RfidReaderACM1281S("/dev/ttyS" + cpConfig.rfcom, RfidReaderACM1281S.RFID_CMD.RFID_TMONEY);
//                rfidReader = new RfidReaderACM1281S("/dev/ttyS"+cpConfig.rfcom, RfidReaderACM1281S.RFID_CMD.RFID_MYFARE);
            } else {   // 예외처리
                rfidReader = new RfidReaderACM1281S("/dev/ttyS" + cpConfig.rfcom, RfidReaderACM1281S.RFID_CMD.RFID_TMONEY);
            }

            rfidReader.setRfidReaderEvent(this);
            rfidReader.rfidReadRequest();
        }

        // UpdateManager를 생성한다.
        updateManager = new UpdateManager(mainActivity, this, OCPPUI2CHActivity.uiVersion);

        initOCPPSession();

        for (int i = 0; i < TypeDefine.MAX_CHANNEL; i++) {
            uiFlowManager[i] = new UIFlowManager(i, activity, this, datas[i], config, mconfig, ocppSessionManager, dspControl, pageManager);
        }

        pageManager.init(this, mainActivity);

        for (int i = 0; i < TypeDefine.MAX_CHANNEL; i++) {
            uiFlowManager[i].setPageManager(pageManager);
        }

        //DSP MonitorView 등록
        dspControl.setDspMonitorListener(getPageManager().joasDSPMonitorView);

        setSlowChargerType(cpConfig.slowChargerType);

        // 1초 타이머 시작
        startPeroidTimerSec();
    }

    public CPConfig getCpConfig() {
        return cpConfig;
    }

    public TL3500S getTL3500S() {
        return tl3500s;
    }

    public PageManager getPageManager() {
        return pageManager;
    }

    public UIFlowManager[] getUIFlowManagers() {
        return uiFlowManager;
    }

    public UIFlowManager getUIFlowManager(int channel) {
        return uiFlowManager[channel];
    }


    public void onAdminPasswordOK(String pwd) {
        if (pwd.equals(cpConfig.settingPassword) == true) {
            pageManager.hideAdminPasswrodInputView();
            pageManager.showSettingView();
        } else {
            Toast.makeText(mainActivity, mainActivity.getResources().getString(R.string.string_password_incorrect), Toast.LENGTH_SHORT).show();
        }
    }


    /***
     * 2022-04-18 추가 : 완속충전기 타입 적용기능
     * UI에 설정된 값으로 202번지 write
     * @param slowChargerType
     */
    public void setSlowChargerType(int slowChargerType) {
        dspControl.setSlowChargerType(0, slowChargerType);
    }


    void startPeroidTimerSec() {
        timerSec = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                timerProcessSec();
            }
        });
        timerSec.start();
    }

    void initOCPPSession() {
        OCPPStackProperty newOcppProperty = loadOcppStackProperty();

        ocppSessionManager = new OCPPSessionManager(mainActivity, TypeDefine.MAX_CHANNEL, Environment.getExternalStorageDirectory().toString() + TypeDefine.REPOSITORY_BASE_PATH, restartBySoftReset);
        ocppSessionManager.init(newOcppProperty);
        ocppSessionManager.setListener(this);

        ocppSessionManager.getOcppStack().setTransportMonitorListener(this);
    }

    public OCPPStackProperty loadOcppStackProperty() {
        OCPPStackProperty newOcppProperty = new OCPPStackProperty();

        newOcppProperty.serverUri = cpConfig.serverURI;
        newOcppProperty.cpid = cpConfig.chargerID;
        newOcppProperty.useBasicAuth = cpConfig.useHttpBasicAuth;
        newOcppProperty.authID = cpConfig.httpBasicAuthID;
        newOcppProperty.authPassword = cpConfig.httpBasicAuthPassword;
        newOcppProperty.chargePointSerialNumber = cpConfig.chargerID;
        newOcppProperty.chargeBoxSerialNumber = cpConfig.chargeBoxSerial;
        newOcppProperty.chargePointModel = cpConfig.chargePointModel;
        newOcppProperty.firmwareVersion = TypeDefine.SW_VER;

        newOcppProperty.useSSL = false;
        newOcppProperty.useSSLCheckCert = false;


        return newOcppProperty;
    }

    public OCPPSessionManager getOcppSessionManager() {
        return ocppSessionManager;
    }

    void destoryManager() {
        watchDogTimer.stop();
        dspControl.interrupt();
        dspControl.stopThread();
        timerSec.cancel();

        if (rfidReader != null) rfidReader.stopThread();
        if (tl3500s != null) tl3500s.stopThread();
        ocppSessionManager.closeManager();
        updateManager.closeManager();
    }

    public void onFinishApp() {
        if (cpConfig.useWatchDogTimer == true) {
            if (watchDogTimerStartCnt >= TypeDefine.WATCHDOG_START_TIMEOUT) {
                watchDogTimer.stop();
                watchDogTimer.close();
                timerSec.cancel();
            }
        }

        mainActivity.finish();
    }

    public boolean rfidReaderRequest(int channel) {
        if (rfidReaderSelect >= 0) return false;

        rfidReaderSelect = channel;
//        Log.v(TAG, "rfidReaderSelect: "+rfidReaderSelect);
        if (rfidReader != null) rfidReader.rfidReadRequest();
        return true;
    }

    public void rfidReaderRelease(int channel) {
        if (channel == rfidReaderSelect) {
            rfidReaderSelect = -1;
//            Log.v(TAG, "rfidReaderSelect: "+rfidReaderSelect);
            if (rfidReader != null) rfidReader.rfidReadRelease();
        }
    }


    /**
     * 세팅이 바뀔때 불려지는 이벤트
     * 통신프로그램 파라미터를 수정한다.
     */
    public void onSettingChanged() {
        OCPPStackProperty newOcppProperty = loadOcppStackProperty();

        ocppSessionManager.restartManager(newOcppProperty);
    }

    //region 원격 업데이트 (Firmware Update) 처리

    /**
     * 업데이트가 진행될때 이벤트 발생(UpdateManager로부터 발생되는 이벤트)
     *
     * @param state
     */
    public void onUpdateStatus(UpdateManager.UpdateState state) {
        switch (state) {
            case None:
            case Waiting:
//                ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.IDLE);
                break;
            case Started:
                ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.DOWNLOADING);
                break;
            case Error:
                ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.DOWNLOAD_FAILED);
//                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);// TODO 충전기 상태 정리 필요
                break;
            case Retrying:
                break;
            case Finished:
                ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.DOWNLOADED);
                updateManager.setState(UpdateManager.UpdateState.Installing);
                break;
            case Installing:
                ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.INSTALLING);
                updateManager.setState(UpdateManager.UpdateState.Installed);
                break;
            case Installed:
                ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.INSTALLED);
                break;
            case InstallFailed:
                ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.INSTALLATION_FAILED);
//                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);// TODO 충전기 상태 정리 필요
                break;


        }
    }

    /**
     * 1초에 한번식 수행 만약 파일 다운로드 상태라고 하면 충전중이 아닐때 수행됨
     */
    void firmwareInstallProcess() {
        if (updateManager.getStatus() == UpdateManager.UpdateState.Installed) {
            //TODO 하드코딩 수정필요..(채널에따른 상태 확인 어캐하지..)
            if (uiFlowManager[0].getUIFlowState() != UIFlowManager.UIFlowState.UI_CHARGING &&
                    uiFlowManager[1].getUIFlowState() != UIFlowManager.UIFlowState.UI_CHARGING) {

                firmwareInstallCounter++;
                if (firmwareInstallCounter == TypeDefine.FIRMWARE_UPDATE_COUNTER) {
                    updateManager.doInstallFirmware(TypeDefine.SW_VER);
                    LogWrapper.v(TAG, "Firmware install start");
                }
            }
        }
    }
    //endregion

    /**
     * 1초에 한번씩 실행되면서 필요한 일을 수핸한다.
     */
    public void timerProcessSec() {

        checkAvailability();
        //WatchDog 수행
        watchDogTimerProcess();

        //FirmwareInstall 체크
        firmwareInstallProcess();

        for (int i = 0; i < TypeDefine.MAX_CHANNEL; i++) {
            uiFlowManager[i].timerProcessSec();
        }

        TextView tvDSPversion = (TextView) mainActivity.findViewById(R.id.tvDSPversion);
        tvDSPversion.setText(" / " + getUIFlowManager(0).getDspVersion());

        checkTransitionDay();
    }

    protected void checkTransitionDay() {
        Calendar curTime = Calendar.getInstance();
        if (curTime.get(Calendar.DAY_OF_MONTH) != lastDateValue) {
            lastDateValue = curTime.get(Calendar.DAY_OF_MONTH);

            // 날짜가 봐뀔때 과거 로그를 지운다.
            ocppSessionManager.getOcppStack().getOcppDiagnosticManager().removePastLog();
        }
    }


    boolean chkAvailabilityStartFlag = true;

    public void checkAvailability() {
        if (chkAvailabilityStartFlag) {
            if (cpConfig.isAvailable) {
                pageManager.hideUnavailableConView();
                for (UIFlowManager uiFlowManager : uiFlowManager) {
                    uiFlowManager.isConnectorOperative = true;
                }
            } else {
                pageManager.showUnavailableConView();
                for (UIFlowManager uiFlowManager : uiFlowManager) {
                    uiFlowManager.isConnectorOperative = false;
                }
            }
            chkAvailabilityStartFlag = false;
        }
    }


    public void stopWatdogTimer() {
        watchDogTimerStartCnt = 0;
        watchDogTimer.stopAndClose();
    }

    // 1초에 한번씩 WatchDog 타이머를 수행한다.
    public void watchDogTimerProcess() {
        // 설정에 WatchDog를 사용하지 않거나 HardReset메시지를 받는경우에는 Skip함
        if (cpConfig.useWatchDogTimer == false || isHardResetEvent == true) return;

        if (watchDogTimerStartCnt >= TypeDefine.WATCHDOG_START_TIMEOUT) {
            if (watchDogTimerStartCnt == TypeDefine.WATCHDOG_START_TIMEOUT) {
                // WatchDog 타이머 시작(open과 함께 시작)
                watchDogTimer.openAndStart(WatchDogTimer.WATCHDOG_MAX_UPDATE_TIMEOUT);
                watchDogTimerStartCnt++; // 이후버터는 update만 실행
            } else {
                //Watch Dog Timer 갱신
                watchDogTimer.update();
                //Log.v(TAG, "WatchDog Update..");
            }
        } else {
            watchDogTimerStartCnt++;
        }
    }

    /**
     * App을 재시작한다.
     */
    public void runSoftReset(int timeout) {
        //Soft Reset인 경우 화면을 초기화 한다.
        TimeoutTimer timer = new TimeoutTimer(timeout, new TimeoutHandler() {
            @Override
            public void run() {
                // App 재시작
                Intent i = mainActivity.getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage(mainActivity.getBaseContext().getPackageName());
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.putExtra("RestartReason", "SoftReset");
                mainActivity.startActivity(i);
            }
        });
        timer.startOnce();
    }


    /**
     * Reset 명령을 원격에서 내려 받았을 떄 처리
     * 충전중인경우 충전을 중지한 다음에 Reset을 한다.
     *
     * @param isHard true 인경우 system reset을 하고 false인 경우에는 충전 중지 이후에 초기화만 진행한다.
     */
    @Override
    public void onResetRequest(boolean isHard) {
        if (isHard == true) {
            isHardResetEvent = true;

            // 충전중이라면 충전을 중지한다.
            for (int i = 0; i < TypeDefine.MAX_CHANNEL; i++) {
                if (uiFlowManager[i].getUIFlowState() == UIFlowManager.UIFlowState.UI_CHARGING) {
                    uiFlowManager[i].setStopReason(StopTransaction.Reason.HARD_RESET);
                    LogWrapper.v("StopReason", "Hard Reset");
                    uiFlowManager[i].onChargingStop();
                }
            }

            // 약 15초뒤에 Reset됨
            if (watchDogTimer.isStarted == false) watchDogTimer.openAndStart(15);
            else watchDogTimer.update();

            // 메시지 박스를 띄운다.
            getPageManager().messageSingleBoxView.setSingleMessageBoxTitle(mainActivity.getResources().getString(R.string.str_hard_reset_title));
            getPageManager().messageSingleBoxView.setSingleMessageBoxContent(mainActivity.getResources().getString(R.string.str_hard_reset_content));
            getPageManager().messageSingleBoxView.setMessageTimeout(15); // WatchDog 시간 최대가 16초)
            getPageManager().showSingleMessageBoxView();

            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            rebootSystem();
                        }
                    }, 10000);
                }
            });

        } else { // Soft Reset
            // 충전중이라면 충전을 중지한다.
            for (int i = 0; i < TypeDefine.MAX_CHANNEL; i++) {
                if (uiFlowManager[i].getUIFlowState() == UIFlowManager.UIFlowState.UI_CHARGING) {
                    uiFlowManager[i].setStopReason(StopTransaction.Reason.SOFT_RESET);
                    LogWrapper.v("StopReason", "Soft Reset");
                    uiFlowManager[i].onChargingStop();
                }
            }

            // 메시지 박스를 띄운다.
            getPageManager().messageSingleBoxView.setSingleMessageBoxTitle(mainActivity.getResources().getString(R.string.str_soft_reset_title));
            getPageManager().messageSingleBoxView.setSingleMessageBoxContent(mainActivity.getResources().getString(R.string.str_soft_reset_content));
            getPageManager().messageSingleBoxView.setMessageTimeout(5);
            getPageManager().showSingleMessageBoxView();

            runSoftReset(5 * 1000);
        }
    }


    void rebootSystem() {
        try {
            PowerManager pm = (PowerManager) mainActivity.getSystemService(Context.POWER_SERVICE);
            pm.reboot("force");

        } catch (Exception e) {

        }
    }

    @Override
    public void onTriggerMessage(TriggerMessage message) {
        switch (message.getRequestedMessage().toString()) {
            case "DiagnosticsStatusNotification":
                break;
            case "FirmwareStatusNotification":
                onUpdateStatus(updateManager.getStatus());
                break;
            case "MeterValues":
                if (message.getConnectorId() != null) {
                    uiFlowManager[message.getConnectorId() - 1].onTriggerMessage(message);
                }
                break;
            case "StatusNotification":
                if (message.getConnectorId() == null) {
                    sendStatusNotificationStatusOfSystem(0);
                } else {
                    if (message.getConnectorId() != null) {
                        sendStatusNotificationStatusOfSystem(message.getConnectorId());
                    }
                }
                break;
        }
    }


    @Override
    public void onTimeUpdate(Calendar syncTime) {
        syncTime.setTimeZone(TimeZone.getTimeZone("UTC"));

        Calendar curTime = Calendar.getInstance();
        curTime.setTimeZone(TimeZone.getTimeZone("UTC"));

//        syncTime.add(Calendar.HOUR, settimeoffset);

//         현재 시각과 서버 시각이 일정이상 차이가 나면 현재 시간을 갱신한다.
        if (Math.abs(curTime.getTimeInMillis() - (syncTime.getTimeInMillis())) > TypeDefine.TIME_SYNC_GAP_MS) {
            LogWrapper.v(TAG, "Curtime : " + curTime.getTime());
            Calendar timetmp = syncTime;
            AlarmManager am = (AlarmManager) mainActivity.getSystemService(Context.ALARM_SERVICE);
            am.setTime(timetmp.getTimeInMillis());
            LogWrapper.v(TAG, "TimeSync : " + syncTime.getTime());
        }

        // bootNotification시에 연결 OK
        mainActivity.setCommConnStatus(true);

        for (UIFlowManager uiFlowManager : uiFlowManager) {
            uiFlowManager.onTimeUpdate(syncTime);
        }


    }


    @Override
    public void onDataTransferResponse(DataTransferResponse.Status status, String messageID, String data) {
        if (messageID.equals("tariff")) {
            uiFlowManager[rfidReaderSelect].onDataTransferResponse(status, messageID, data);
        } else if (messageID.equals("timeOffset")) {
            if (status == DataTransferResponse.Status.ACCEPTED) {
                onRequestTimeOffsetResponse(data);
            }

            // 처음 접속이후에 StatusNotification을 보낸다.
            if (cpConfig.isAvailable) {
                setOcppStatus(StatusNotification.Status.AVAILABLE);
            } else {
                setOcppStatus(StatusNotification.Status.UNAVAILABLE);
            }
        }

    }

    void onRequestTimeOffsetResponse(String data) {
        try {
            JSONObject obj = new JSONObject(data);
            String timeoffset = obj.getString("timeOffset");
            int _timeoffset = Integer.parseInt(timeoffset.substring(1, 3));
            timeOffset = _timeoffset;
            for (UIFlowManager uiFlowManager : uiFlowManager) {
                uiFlowManager.timeOffset = _timeoffset;
            }

//            LogWrapper.v(TAG, "Set timeoffset : " + timeoffset);
//            AlarmManager am = (AlarmManager) mainActivity.getSystemService(Context.ALARM_SERVICE);
//            Calendar curTime = Calendar.getInstance();
//            curTime.add(Calendar.HOUR, settimeoffset);
//            am.setTime(curTime.getTimeInMillis());
//
//            LogWrapper.v(TAG, "Curtime : " + curTime.getTime());

        } catch (JSONException e) {
            LogWrapper.e(TAG, "DatatansferResponese Json Parse Err:" + e.toString());
        }
    }


    @Override
    public void onDataTransferRequest(OCPPMessage message) {

    }

    @Override
    public boolean onCheckUIChargingStatus() {
        return false;
    }


    /**
     * 원격 업데이트 이벤트를 받을때 처리한다.
     *
     * @param location      URI 주소(FTP, HTTP)
     * @param retry         최대 재시도횟수
     * @param retrieveDate  업데이트 시도 시간(이후)
     * @param retryInterval 업데이트 시도 간격
     */
    @Override
    public void onUpdateFirmwareRequest(URI location, int retry, Calendar retrieveDate, int retryInterval) {
        LogWrapper.v(TAG, "Firmware Update Request: " + location.toString());

//        retrieveDate.add(Calendar.HOUR, settimeoffset);

        LogWrapper.v(TAG, "Firmware Update time: " + retrieveDate.getTime());

        updateManager.setUpdateInfo(location, retry, retrieveDate, retryInterval);
    }

    // 통신 연결 상태 관리
    public void onChangeOcppServerConnectStatus(boolean status) {
        // 타이밍상 Listener 생성 보다 connection이 더 빠른경우에 호출이 안되는 경우가 있음
        // bootNotification에서 연결 상태 Update 할 필요가 있음(위에 TimeSync에서 수행)
        mainActivity.setCommConnStatus(status);
    }

    // 통신 이벤트 발생 처리(UI 표시)
    public void onOcppMessageTransportEvent() {
        mainActivity.setCommConnActive();
    }


    @Override
    public void onDspStatusChange(int channel, DSPRxData2.STATUS400 idx, boolean val) {
        uiFlowManager[channel].onDspStatusChange(channel, idx, val);
        switch (idx) {
            case FAULT:
                if (val == true) {
                    for (int i = 0; i < TypeDefine.MAX_CHANNEL; i++) {
                        if (i == channel) break;
                        uiFlowManager[i].onOtherChannelFaultEvent();
                    }
                }
                break;

        }
    }

    @Override
    public void onDspMeterChange(int channel, long meterVal) {
        uiFlowManager[channel].onDspMeterChange(channel, meterVal);
    }

    @Override
    public void onDspCommErrorStatus(boolean isError) {
        for (int i = 0; i < TypeDefine.MAX_CHANNEL; i++) {
            uiFlowManager[i].onDspCommErrorStatus(isError);
        }
    }

    @Override
    public void onAuthSuccess(int connectorId) {
        uiFlowManager[connectorId - 1].onAuthSuccess(connectorId);
    }

    @Override
    public void onAuthFailed(int connectorId) {
        uiFlowManager[connectorId - 1].onAuthFailed(connectorId);
    }

    @Override
    public void onChangeState(int connectorId, OCPPSession.SessionState state) {
        uiFlowManager[connectorId - 1].onChangeState(connectorId, state);
    }

    @Override
    public CancelReservationResponse.Status onCancelReservation(int reservationId) {
        CancelReservationResponse.Status ret = CancelReservationResponse.Status.REJECTED;
        for (int i = 0; i < TypeDefine.MAX_CHANNEL; i++) {
            if (uiFlowManager[i].onCancelReservation(reservationId) == CancelReservationResponse.Status.ACCEPTED) {
                ret = CancelReservationResponse.Status.ACCEPTED;
                break;
            }
        }
        return ret;
    }

    @Override
    public void onBootNotificationResponse(boolean success) {
        if (success) {
            ocppSessionManager.sendDataTransferReq(TypeDefine.DATA_TRANSFER_MESSAGEID_TIMEOFFSET, "");

            String data = makeGatewayInfoJsonObject();
            ocppSessionManager.sendDataTransferReq(TypeDefine.DATA_TRANSFER_MESSAGEID_GATEWAY_INFO, data);


            // 펌웨어 업데이트가 되었다면 메시지를 보내고` 펌웨어 업데이트 필드를 초기화한다.
            if (updateManager.newFirmwareUpdateed) {
                ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.INSTALLED);
                updateManager.onUpdateCompleteMsgSent();
            }
        }
    }

    public String makeGatewayInfoJsonObject() {
        String retstr = "";
        try {
            JSONObject obj = new JSONObject();
            //요청관련
            obj.put("sn", cpConfig.chargeBoxSerial);
            obj.put("fwver", TypeDefine.SW_VER);

            retstr = obj.toString();

        } catch (Exception e) {
            LogWrapper.e(TAG, "DataTransfer(GatewayInfo) Json Make Err:" + e.toString());
        }

        return retstr;
    }

    public void sendStatusNotificationStatusOfSystem(int cunID) {
        if (cunID == 0) {
            // 전체 커넥터 상태 전달
            if (getUIFlowManager(0).chargeData.ocppStatus == StatusNotification.Status.UNAVAILABLE &&
                    getUIFlowManager(1).chargeData.ocppStatus == StatusNotification.Status.UNAVAILABLE) {
                ocppSessionManager.sendStatusNotificationRequest(0,
                        StatusNotification.Status.UNAVAILABLE, StatusNotification.ErrorCode.NO_ERROR, "");
                if (cpConfig.isAvailable == true) {
                    cpConfig.isAvailable = false;
                    cpConfig.saveConfig(mainActivity);
                }
            } else if (getUIFlowManager(0).chargeData.ocppStatus == StatusNotification.Status.FAULTED &&
                    getUIFlowManager(1).chargeData.ocppStatus == StatusNotification.Status.FAULTED) {
                ocppSessionManager.sendStatusNotificationRequest(0,
                        StatusNotification.Status.FAULTED, StatusNotification.ErrorCode.OTHER_ERROR, "");
            } else {
                ocppSessionManager.sendStatusNotificationRequest(0,
                        StatusNotification.Status.AVAILABLE, StatusNotification.ErrorCode.NO_ERROR, "");
                if (cpConfig.isAvailable == false) {
                    cpConfig.isAvailable = true;
                    cpConfig.saveConfig(mainActivity);
                }

            }
            // 각 커넥터 상태 전달
            for (UIFlowManager uiFlowManager : uiFlowManager) {
                uiFlowManager.sendStatusNotificationStatusOfSystem();
            }
        } else {
            uiFlowManager[cunID - 1].sendStatusNotificationStatusOfSystem();
        }
    }


    /**
     * 원격에서 충전을 중지시키는 이벤트가 발생했을때 처리
     *
     * @param connectorId
     */
    @Override
    public void onRemoteStopTransaction(int connectorId) {
        uiFlowManager[connectorId - 1].onRemoteStopTransaction(connectorId);
    }

    /**
     * 원격에서 충전을 시작시키는 이벤트가 발생했을 때 처리     *
     *
     * @param connectorId
     * @param idTag
     * @param chargingProfile
     * @return 성공여부
     */
    @Override
    public boolean onRemoteStartTransaction(int connectorId, String idTag, ChargingProfile chargingProfile) {
        return uiFlowManager[connectorId - 1].onRemoteStartTransaction(connectorId, idTag, chargingProfile);
    }

    /**
     * StartTranscation의 IdTagInfo 상태를 받았을 때처리함.
     *
     * @param connectorId
     * @param tagInfo
     */
    public void onStartTransactionResult(int connectorId, IdTagInfo tagInfo, int transactionId) {
        uiFlowManager[connectorId - 1].onStartTransactionResult(connectorId, tagInfo, transactionId);
    }

    @Override
    public ReserveNowResponse.Status onReserveNow(int connectorId, Calendar expiryDate, String idTag, String parentIdTag, int reservationId) {
        return uiFlowManager[connectorId - 1].onReserveNow(connectorId, expiryDate, idTag, parentIdTag, reservationId);
    }

    @Override
    public void onChangeAvailability(int connectorId, ChangeAvailability.Type type) {
        if (connectorId == 0) {
            if (type == ChangeAvailability.Type.INOPERATIVE) cpConfig.isAvailable = false;
            else cpConfig.isAvailable = true;

            cpConfig.saveConfig(mainActivity);

            for (UIFlowManager uiFlowManager : uiFlowManager) {
                uiFlowManager.onChangeAvailability(connectorId, type);
            }
            chkAvailabilityStartFlag = true;
        } else {
            uiFlowManager[connectorId - 1].onChangeAvailability(connectorId, type);
        }
    }

    //================================================
    // RFID 이벤트 수신
    //================================================
    @Override
    public void onRfidDataReceive(String rfid, boolean success) {
        uiFlowManager[rfidReaderSelect].onRfidDataReceive(rfid, success);
    }

//    @Override
//    public void onRfidCommErr(boolean isErr) {
//        for (UIFlowManager uiFlowManager : uiFlowManager) {
//            uiFlowManager.onRfidCommErr(isErr);
//        }
//    }

    //================================================
    // 초기화(팩토리리셋)
    //================================================
    public void doFactoryReset() {
        // IP 세팅
        NetUtil.configurationStaticIP("192.168.0.230", "255.255.255.0", "192.168.0.1", "8.8.8.8");
        // 데이터베이스 Close
        ocppSessionManager.getOcppStack().closeOcpp();
        ocppSessionManager.getOcppStack().getDbOpenHelper().close();

        // BaseDirectory 아래 데이터 모두 삭제.
        String deleteCmd = "rm -rf " + Environment.getExternalStorageDirectory().toString() + TypeDefine.REPOSITORY_BASE_PATH + "/";
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(deleteCmd);
        } catch (IOException e) {
        }
        onResetRequest(true);
    }

    @Override
    public void responseCallback(TL3500S.ResponseType type, Map<String, String> retVal, int ch) {
        switch (type) {
            case Error:
                for (UIFlowManager uiFlowManager : uiFlowManager) {
                    uiFlowManager.onTL3500S_Error(retVal, ch);
                }
                break;
            case Check:
                for (UIFlowManager uiFlowManager : uiFlowManager) {
                    uiFlowManager.onTL3500S_Check(retVal, ch);
                }
                break;
            default:
                uiFlowManager[ch].responseCallback(type, retVal, ch);
                break;
        }

    }


    @Override
    public void onOCPPTransportRecvRaw(String data) {
        getPageManager().commMonitorView.addOCPPRawMsg("RX", data);
        onOcppMessageTransportEvent();

        // Add to Diagnostic Log
        if (ocppSessionManager != null) {
            if (ocppSessionManager.getOcppStack().getOcppDiagnosticManager() != null) {
                ocppSessionManager.getOcppStack().getOcppDiagnosticManager().addLog(
                        OCPPDiagnosticManager.DiagnosticType.Comm, "RX", data);
            }
        }
    }

    @Override
    public void onOCPPTransportSendRaw(String data) {
        getPageManager().commMonitorView.addOCPPRawMsg("TX", data);
        onOcppMessageTransportEvent();

        // Add to Diagnostic Log
        if (ocppSessionManager != null) {
            if (ocppSessionManager.getOcppStack().getOcppDiagnosticManager() != null) {
                ocppSessionManager.getOcppStack().getOcppDiagnosticManager().addLog(
                        OCPPDiagnosticManager.DiagnosticType.Comm, "TX", data);
            }
        }
    }

    @Override
    public void onOCPPTransportConnected() {
        for (UIFlowManager uiFlowManager : uiFlowManager) {
            uiFlowManager.onOCPPTransportConnected();
        }
    }

    @Override
    public void onOCPPTransportDisconnected() {
        for (UIFlowManager uiFlowManager : uiFlowManager) {
            uiFlowManager.onOCPPTransportDisconnected();
        }
    }


    public void onSelectChannelEvent(int channel) {
        if (uiFlowManager[channel].getUIFlowState() == UIFlowManager.UIFlowState.UI_SELECT) {
            uiFlowManager[channel].onPageSelectEvent(PageEvent.SELECT_CTYPE_CLICK);
        } else if (uiFlowManager[channel].getUIFlowState() == UIFlowManager.UIFlowState.UI_CARD_TAG) {
            pageManager.changePage(PageID.CARD_TAG, channel);
        } else if (uiFlowManager[channel].getUIFlowState() == UIFlowManager.UIFlowState.UI_AUTH_WAIT) {
            uiFlowManager[channel].onPageStartEvent();
        } else if (uiFlowManager[channel].getUIFlowState() == UIFlowManager.UIFlowState.UI_CONNECTOR_WAIT) {
            pageManager.changePage(PageID.CONNECTOR_WAIT, channel);
        } else if (uiFlowManager[channel].getUIFlowState() == UIFlowManager.UIFlowState.UI_SELECT_PAYMENT_METHOD) {
            pageManager.changePage(PageID.SELECT_PAYMENT_METHOD, channel);
        } else if (uiFlowManager[channel].getUIFlowState() == UIFlowManager.UIFlowState.UI_SET_CHARGING_OPTION) {
            pageManager.changePage(PageID.SET_CHARING_OPTION, channel);
        } else if (uiFlowManager[channel].getUIFlowState() == UIFlowManager.UIFlowState.UI_SELECT_CHARGING_OPTION) {
            pageManager.changePage(PageID.SELECT_CHATGING_OPTION, channel);
        } else if (uiFlowManager[channel].getUIFlowState() == UIFlowManager.UIFlowState.UI_CHARGING) {
            pageManager.changePage(PageID.CHARGING, channel);
        } else if (uiFlowManager[channel].getUIFlowState() == UIFlowManager.UIFlowState.UI_FINISH_CHARGING) {
            pageManager.changePage(PageID.FINISH_CHARGING, channel);
        } else if (uiFlowManager[channel].getUIFlowState() == UIFlowManager.UIFlowState.UI_UNPLUG) {
            pageManager.changePage(PageID.UNPLUG, channel);
        }
        uiFlowManager[channel].UIprocessChangeState(uiFlowManager[channel].getUIFlowState());
    }

    public void setOcppStatus(StatusNotification.Status status) {
        ocppSessionManager.sendStatusNotificationRequest(0, status, StatusNotification.ErrorCode.NO_ERROR, "");
        ocppSessionManager.sendStatusNotificationRequest(1, status, StatusNotification.ErrorCode.NO_ERROR, "");
        ocppSessionManager.sendStatusNotificationRequest(2, status, StatusNotification.ErrorCode.NO_ERROR, "");

        getUIFlowManager(0).chargeData.ocppStatus = status;
        getUIFlowManager(1).chargeData.ocppStatus = status;
    }


    @Override
    public void onRecvDebugMsg(LogWrapperMsg packet) {
        if (pageManager != null) pageManager.getJoasDebugMsgView().addPacket(packet);
        // Add to Diagnostic Log
        if (ocppSessionManager != null) {
            if (ocppSessionManager.getOcppStack().getOcppDiagnosticManager() != null) {
                OCPPDiagnosticManager.DiagnosticType type;
                switch (packet.level) {
                    case Log.ERROR:
                        type = OCPPDiagnosticManager.DiagnosticType.Error;
                        break;
                    case Log.DEBUG:
                        type = OCPPDiagnosticManager.DiagnosticType.Debug;
                        break;
                    case Log.VERBOSE:
                        type = OCPPDiagnosticManager.DiagnosticType.Verbose;
                        break;
                    default:
                        type = OCPPDiagnosticManager.DiagnosticType.Debug;
                        break;
                }
                ocppSessionManager.getOcppStack().getOcppDiagnosticManager().addLog(packet.time, type, packet.TAG, packet.msg);
            }
        }
    }

    public boolean isConnectorCharging() {
        boolean ret = false;
        if (getUIFlowManager(0).getUIFlowState() == UIFlowManager.UIFlowState.UI_CHARGING
                || getUIFlowManager(1).getUIFlowState() == UIFlowManager.UIFlowState.UI_CHARGING)
            ret = true;

        return ret;
    }

}
