/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 11. 19 오후 2:40
 *
 */

package com.joas.ocppui_LS_2ch.webservice;

import android.util.ArrayMap;

import androidx.collection.CircularArray;

import com.joas.ocppls.msg.ConfigurationKey;
import com.joas.ocppls.msg.GetConfigurationResponse;
import com.joas.ocppls.msg.IdTagInfo;
import com.joas.ocppui_LS_2ch.CPConfig;
import com.joas.ocppui_LS_2ch.MultiChannelUIManager;
import com.joas.ocppui_LS_2ch.OCPPUI2CHActivity;
import com.joas.ocppui_LS_2ch.TypeDefine;
import com.joas.ocppui_LS_2ch.page.JoasCommMonitorView;
import com.joas.utils.LogWrapperMsg;
import com.joas.utils.NetUtil;
import com.joas.utils.TimeUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

public class WebChargerInfo {

    public static JSONObject getVersions() {
        MultiChannelUIManager flowManager = OCPPUI2CHActivity.getMainActivity().getMultiChannelUIManager();

        JSONObject json = new JSONObject();
        try {
            json.put("model_name", flowManager.getCpConfig().chargePointModel);
            json.put("sw_ver", TypeDefine.SW_VER + " " + TypeDefine.SW_RELEASE_DATE);
            json.put("dsp_ver", String.format("0x%X", flowManager.getUIFlowManager(0).getDspVersion()));

        }
        catch (Exception e) {}
        return json;
    }

    public static JSONObject getChargerStat() {
        MultiChannelUIManager manager = OCPPUI2CHActivity.getMainActivity().getMultiChannelUIManager();
        CPConfig cpConfig = manager.getCpConfig();

        JSONObject json = new JSONObject();
        try {
            json.put("svraddr", cpConfig.serverURI);
            json.put("cid", cpConfig.chargerID);
            json.put("comm", OCPPUI2CHActivity.getMainActivity().getIsCommConnected() ? "Connected" : "Disconnected");
            String strConnector = "";
            strConnector = " 01: "+manager.getUIFlowManager(0).getChargeData().ocppStatus.value() + " / " +
                    "02: "+manager.getUIFlowManager(1).getChargeData().ocppStatus.value();

            json.put("connector", strConnector);

        }
        catch (Exception e) {}
        return json;
    }

    public static JSONObject getNetworkInfo() {
        ArrayMap<String, String> map = NetUtil.readIpConfigurations();
        String ipaddr = map.get("ipaddress");
        String netmask  = map.get("netmask");
        String gateway= map.get("gateway");
        String dns = map.get("dns");

        if ( map.size() == 0 ) map.put("type", "DHCP");

        if ( map.get("type").equals("DHCP") ) {
            ipaddr = NetUtil.getDHCPIpAddr();
            netmask = NetUtil.getDHCPNetmask();
            gateway = NetUtil.getDHCPGateway();
            dns = NetUtil.getDHCPDNS();
        }

        JSONObject json = new JSONObject();
        try {
            json.put("type", map.get("type"));
            json.put("ip", ipaddr);
            json.put("netmask", netmask);
            json.put("gateway", gateway);
            json.put("dns", dns);
        }
        catch (Exception e) {}
        return json;
    }

    public static JSONObject getSystemConfig() {
        MultiChannelUIManager manager = OCPPUI2CHActivity.getMainActivity().getMultiChannelUIManager();
        CPConfig cpConfig = manager.getCpConfig();

        JSONObject json = new JSONObject();
        try {
            json.put("charger_id", cpConfig.chargerID);
            json.put("server_uri", cpConfig.serverURI);
            json.put("http_auth_id", cpConfig.httpBasicAuthID);
            json.put("http_auth_pwd", cpConfig.httpBasicAuthPassword);
            json.put("admin_pwd", cpConfig.settingPassword);
            json.put("use_http_auth", ""+cpConfig.useHttpBasicAuth);
            json.put("use_watchdog", ""+cpConfig.useWatchDogTimer);
            json.put("use_trustca", ""+cpConfig.useTrustCA);
            json.put("is_fastcharger",""+cpConfig.isFastCharger);
            json.put("use_tl3600",""+cpConfig.useTl3500S);
            json.put("use_acs",""+cpConfig.useACS);
            json.put("use_sehan",""+cpConfig.useSehan);
            json.put("is_authskip",""+cpConfig.isAuthSkip);
            json.put("slowchargertype",Integer.toString(cpConfig.slowChargerType));
            json.put("chargepointmodel",cpConfig.chargePointModel);
            json.put("lcdsize",Integer.toString(cpConfig.lcdSize));
            json.put("dsp_com",cpConfig.dspcom);
            json.put("rf_com",cpConfig.rfcom);
            json.put("chargeboxserial",cpConfig.chargeBoxSerial);

        }
        catch (Exception e) {}
        return json;
    }

    public static JSONArray getConfigKeyList() {
        MultiChannelUIManager manager = OCPPUI2CHActivity.getMainActivity().getMultiChannelUIManager();
        GetConfigurationResponse response = manager.getOcppSessionManager().getOcppConfiguration().getConfigurationsAll();

        JSONArray array = new JSONArray();
        try {
            for (ConfigurationKey key: response.getConfigurationKey()) {
                JSONObject json = new JSONObject();
                json.put("key", key.getKey());
                json.put("readonly", key.getReadonly());
                json.put("value", key.getValue());
                array.put(json);
            }
        }
        catch (Exception e) {}
        return array;
    }

    public static void setNetwork(JSONObject json) {
        MultiChannelUIManager manager = OCPPUI2CHActivity.getMainActivity().getMultiChannelUIManager();
        try {
            String type = json.getString("type");
            String ipaddr  = json.getString("ipaddr");
            String netmask = json.getString("netmask");
            String gateway = json.getString("gateway");
            String dns = json.getString("dns");

            if ( type.equals("dhcp") ) {
                NetUtil.configurationDHCP();
            }
            else {
                NetUtil.configurationStaticIP(ipaddr, netmask, gateway, dns);
            }
            manager.onResetRequest(true);
        }
        catch (Exception e) {}
    }

    public static void setConfigKey(String key, String val) {
        MultiChannelUIManager manager = OCPPUI2CHActivity.getMainActivity().getMultiChannelUIManager();
        manager.getOcppSessionManager().getOcppStack().getLocalConfig().saveOcppConfiguration(key, val);
        manager.getOcppSessionManager().getOcppConfiguration().setConfiguration(key, val);
    }

    public static void setChargerSetting(JSONObject json) {
        MultiChannelUIManager manager = OCPPUI2CHActivity.getMainActivity().getMultiChannelUIManager();
        CPConfig cpConfig = manager.getCpConfig();

        try {
            cpConfig.chargerID = json.getString("charger_id");
            cpConfig.serverURI = json.getString("server_uri");
            cpConfig.httpBasicAuthID = json.getString("http_auth_id");
            cpConfig.httpBasicAuthPassword = json.getString("http_auth_pwd");
            cpConfig.settingPassword = json.getString("admin_pwd");
            cpConfig.useHttpBasicAuth = json.getBoolean("use_http_auth");
            cpConfig.useWatchDogTimer = json.getBoolean("use_watchdog");
            cpConfig.useTrustCA = json.getBoolean("use_trustca");
            cpConfig.isFastCharger = json.getBoolean("is_fastcharger");
            cpConfig.useTl3500S = json.getBoolean("use_tl3600");
            cpConfig.useACS = json.getBoolean("use_acs");
            cpConfig.useSehan = json.getBoolean("use_sehan");
            cpConfig.isAuthSkip = json.getBoolean("is_authskip");
            cpConfig.slowChargerType = Integer.parseInt(json.getString("slowchargertype"));
            cpConfig.chargePointModel = json.getString("chargepointmodel");
            cpConfig.lcdSize = Integer.parseInt(json.getString("lcdsize"));
            cpConfig.dspcom = json.getString("dsp_com");
            cpConfig.rfcom = json.getString("rf_com");
            cpConfig.chargeBoxSerial = json.getString("chargeboxserial");


            cpConfig.saveConfig(OCPPUI2CHActivity.getMainActivity().getApplicationContext());
            manager.stopWatdogTimer();
            manager.onSettingChanged();
            manager.runSoftReset(3);
        }
        catch (Exception e) {}
    }

    public static JSONArray getRecentSysLog() {
        CircularArray<LogWrapperMsg> queue = OCPPUI2CHActivity.getMainActivity().getMultiChannelUIManager().getPageManager().getJoasDebugMsgView().getPacketQueue();
        int cnt = queue.size();
        JSONArray array = new JSONArray();
        try {
            synchronized (queue) {
                for (int i = 0; i < cnt; i++) {
                    JSONObject json = new JSONObject();
                    LogWrapperMsg item = queue.get(i);
                    json.put("time", item.time);
                    json.put("level", LogWrapperMsg.getLevelString(item.level));
                    json.put("tag", item.TAG);
                    json.put("msg", item.msg);
                    array.put(json);
                }
            }
        }
        catch (Exception e) {}
        return array;
    }

    public static JSONArray getRecentCommLog() {
        CircularArray<JoasCommMonitorView.OCPPMonitorMsg> queue = OCPPUI2CHActivity.getMainActivity().getMultiChannelUIManager().getPageManager().getCommMonitorView().getPacketQueue();
        int cnt = queue.size();
        JSONArray array = new JSONArray();
        try {
            synchronized (queue) {
                for (int i = 0; i < cnt; i++) {
                    JSONObject json = new JSONObject();
                    JoasCommMonitorView.OCPPMonitorMsg item = queue.get(i);
                    json.put("time", item.time);
                    json.put("trx", item.trx);
                    json.put("msg", item.data);
                    array.put(json);
                }
            }
        }
        catch (Exception e) {}
        return array;
    }

    public static JSONArray getAuthCacheList() {
        Map<String, IdTagInfo> list = OCPPUI2CHActivity.getMainActivity().getMultiChannelUIManager().getOcppSessionManager().getOcppStack().getAuthorizeCache().getAllCacheIdTag();
        JSONArray array = new JSONArray();
        try {
            String k;
            IdTagInfo v;
            for(Map.Entry<String, IdTagInfo> entry : list.entrySet()) {
                String key = entry.getKey();
                IdTagInfo idTagInfo = entry.getValue();
                JSONObject json = new JSONObject();
                json.put("idtag", key);
                json.put("parentid", idTagInfo.getParentIdTag());
                json.put("status", idTagInfo.getStatus());
                json.put("expired", TimeUtil.getDateAsString("dd/MM/yyyy HH:mm:ss", idTagInfo.getExpiryDate().getTime()));
                array.put(json);
            }
        }
        catch (Exception e) {}
        return array;
    }

    public static JSONArray getLocalAuthList() {
        Map<String, IdTagInfo> list = OCPPUI2CHActivity.getMainActivity().getMultiChannelUIManager().getOcppSessionManager().getOcppStack().getLocalAuthList().getAllLocalAuthList();
        JSONArray array = new JSONArray();
        try {
            String k;
            IdTagInfo v;
            for(Map.Entry<String, IdTagInfo> entry : list.entrySet()) {
                String key = entry.getKey();
                IdTagInfo idTagInfo = entry.getValue();
                JSONObject json = new JSONObject();
                json.put("idtag", key);
                json.put("parentid", idTagInfo.getParentIdTag());
                json.put("status", idTagInfo.getStatus());
                json.put("expired", TimeUtil.getDateAsString("dd/MM/yyyy HH:mm:ss", idTagInfo.getExpiryDate().getTime()));
                array.put(json);
            }
        }
        catch (Exception e) {}
        return array;
    }
}
