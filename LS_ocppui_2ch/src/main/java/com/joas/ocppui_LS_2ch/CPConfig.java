/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 5. 2. 오후 4:29
 *
 */

package com.joas.ocppui_LS_2ch;

import android.content.Context;
import android.os.Environment;

import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;

import org.json.JSONObject;

public class CPConfig {
    public static final String TAG = "CPConfig";

    public static final String CP_CONFIG_FILE_NAME = "CPConfig.txt";

    public static final String CP_CONFIG_CP_ID = "ChagerID";
    public static final String CP_CONFIG_SERVER_URI = "ServerURI";
    public static final String CP_CONFIG_HTTP_BASIC_AUTH_ID = "HttpBasicAuthID";
    public static final String CP_CONFIG_HTTP_BASIC_AUTH_PASSWORD = "HttpBasicAuthPassword";
    public static final String CP_CONFIG_USE_HTTP_BASIC_AUTH = "UseHttpBasicAuth";
    public static final String CP_CONFIG_SETTING_PASSWORD = "SettingPassword";
    public static final String CP_CONFIG_AUTH_SKIP = "AuthSkip";
    public static final String CP_CONFIG_WATCHDOG_TIMER_USE = "WatchDogTimer";
    public static final String CP_CONFIG_IS_FAST_CHARGER = "IsFastCharger";
    public static final String CP_CONFIG_USE_TRUST_CA = "UseTrustCA";
    public static final String CP_CONFIG_IS_AVAILABLE = "IsAvailable";
    public static final String CP_CONFIG_MODE_CONNECTORID = "ConnectorId";
    public static final String CP_CONFIG_SLOW_CHARGER_TYPE = "SlowChargerType";
    public static final String CP_CONFIG_USE_TL3500S = "UseTL3500S";
    public static final String CP_CONFIG_CHARGEPOINTMODEL = "chargePointModel";
    public static final String CP_CONFIG_LCDSIZE = "lcdSize";
    public static final String CP_CONFIG_DSPCOM = "dspcom";
    public static final String CP_CONFIG_RFCOM = "rfcom";
    public static final String CP_CONFIG_CHARGEBOXSERIAL = "chargeBoxSerial";
    public static final String CP_CONFIG_USE_ACS = "UseACS";
    public static final String CP_CONFIG_USE_SEHAN = "UseSehan";

    public String chargerID = "T2S00-206";
    public String serverURI = "ws://211.238.251.230:31001";
    public String httpBasicAuthID = "T2S00-206";
    public String httpBasicAuthPassword = "ABCDEFGHIJ123456";
    public boolean useHttpBasicAuth = true;

    public String settingPassword = "1234";
    public boolean isAuthSkip = false;
    public boolean useWatchDogTimer = true;
    public boolean isFastCharger = false;
    public boolean useTrustCA = false;
    public boolean isAvailable = true;
    public String opModeConnectorID = "0";
    public boolean useTl3500S = true;
    public String chargePointModel = "TEMP";
    //완속 충전기타입 선택기능 추가 - 220418 - 이전모델들은 해당값에대해 영향을 받지않음
    public int slowChargerType = 1;     //표준형 7kW alter : 7 , 11kW c-type : 9, JC-92C1-7-0P : 사용X

    public int lcdSize = 1; // 0: 4.3inch , 1: 8 inch ...  추가 할수 있도록..
    public String dspcom = "2";     // pine64 dsp : 2 , rf : 3
    public String rfcom = "3";      // 올라 신규 pc dsp :5  ,rf : 9
    public String chargeBoxSerial = "12345678";

    public boolean useACS = false;
    public boolean useSehan = false;

    public CPConfig() {

    }

    public void loadConfig(Context context) {
        String loadString = null;
        try {
            loadString = FileUtil.getStringFromFile(Environment.getExternalStorageDirectory() + TypeDefine.CP_CONFIG_PATH + "/" + CP_CONFIG_FILE_NAME);
        } catch (Exception ex) {
            loadString = null;
        }
        if (loadString == null) {
            //Save Default Config
            saveConfig(context);
        } else {
            try {
                JSONObject obj = new JSONObject(loadString);

                chargerID = obj.getString(CP_CONFIG_CP_ID);
                serverURI = obj.getString(CP_CONFIG_SERVER_URI);
                httpBasicAuthID = obj.getString(CP_CONFIG_HTTP_BASIC_AUTH_ID);
                httpBasicAuthPassword = obj.getString(CP_CONFIG_HTTP_BASIC_AUTH_PASSWORD);
                useHttpBasicAuth = obj.getBoolean(CP_CONFIG_USE_HTTP_BASIC_AUTH);
                isAuthSkip = obj.getBoolean(CP_CONFIG_AUTH_SKIP);
                settingPassword = obj.getString(CP_CONFIG_SETTING_PASSWORD);
                useWatchDogTimer = obj.getBoolean(CP_CONFIG_WATCHDOG_TIMER_USE);
                isFastCharger = obj.getBoolean(CP_CONFIG_IS_FAST_CHARGER);
                useTrustCA = obj.getBoolean(CP_CONFIG_USE_TRUST_CA);
                isAvailable = obj.getBoolean(CP_CONFIG_IS_AVAILABLE);
                opModeConnectorID = obj.getString(CP_CONFIG_MODE_CONNECTORID);
                slowChargerType = obj.getInt(CP_CONFIG_SLOW_CHARGER_TYPE);
                useTl3500S = obj.getBoolean(CP_CONFIG_USE_TL3500S);
                chargePointModel = obj.getString(CP_CONFIG_CHARGEPOINTMODEL);
                dspcom = obj.getString(CP_CONFIG_DSPCOM);
                rfcom = obj.getString(CP_CONFIG_RFCOM);
                chargeBoxSerial = obj.getString(CP_CONFIG_CHARGEBOXSERIAL);
                useACS = obj.getBoolean(CP_CONFIG_USE_ACS);
                useSehan = obj.getBoolean(CP_CONFIG_USE_SEHAN);

            } catch (Exception ex) {
                LogWrapper.e(TAG, "Json Parse Err:" + ex.toString());
                saveConfig(context);
            }
        }
    }

    public void saveConfig(Context context) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(CP_CONFIG_CP_ID, chargerID);
            obj.put(CP_CONFIG_SERVER_URI, serverURI);
            obj.put(CP_CONFIG_HTTP_BASIC_AUTH_ID, httpBasicAuthID);
            obj.put(CP_CONFIG_HTTP_BASIC_AUTH_PASSWORD, httpBasicAuthPassword);
            obj.put(CP_CONFIG_USE_HTTP_BASIC_AUTH, useHttpBasicAuth);
            obj.put(CP_CONFIG_AUTH_SKIP, isAuthSkip);
            obj.put(CP_CONFIG_SETTING_PASSWORD, settingPassword);
            obj.put(CP_CONFIG_WATCHDOG_TIMER_USE, useWatchDogTimer);
            obj.put(CP_CONFIG_IS_FAST_CHARGER, isFastCharger);
            obj.put(CP_CONFIG_USE_TRUST_CA, useTrustCA);
            obj.put(CP_CONFIG_IS_AVAILABLE, isAvailable);
            obj.put(CP_CONFIG_MODE_CONNECTORID, opModeConnectorID);
            obj.put(CP_CONFIG_SLOW_CHARGER_TYPE, slowChargerType);
            obj.put(CP_CONFIG_USE_TL3500S, useTl3500S);
            obj.put(CP_CONFIG_CHARGEPOINTMODEL, chargePointModel);
            obj.put(CP_CONFIG_DSPCOM, dspcom);
            obj.put(CP_CONFIG_RFCOM, rfcom);
            obj.put(CP_CONFIG_CHARGEBOXSERIAL, chargeBoxSerial);
            obj.put(CP_CONFIG_USE_ACS, useACS);
            obj.put(CP_CONFIG_USE_SEHAN, useSehan);

        } catch (Exception ex) {
            LogWrapper.e(TAG, "Json Make Err:" + ex.toString());
        }

        FileUtil.stringToFile(Environment.getExternalStorageDirectory() + TypeDefine.CP_CONFIG_PATH, CP_CONFIG_FILE_NAME, obj.toString(), false);
    }
}
