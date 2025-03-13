/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:42
 */

package com.joas.ocppls.chargepoint;

import android.content.Context;

import com.joas.ocppls.msg.AuthorizeResponse;
import com.joas.ocppls.msg.CancelReservationResponse;
import com.joas.ocppls.msg.ChangeAvailability;
import com.joas.ocppls.msg.ChargingProfile;
import com.joas.ocppls.msg.ClearChargingProfile;
import com.joas.ocppls.msg.CsChargingProfiles;
import com.joas.ocppls.msg.DataTransferResponse;
import com.joas.ocppls.msg.DiagnosticsStatusNotification;
import com.joas.ocppls.msg.FirmwareStatusNotification;
import com.joas.ocppls.msg.IdTagInfo;
import com.joas.ocppls.msg.ReserveNowResponse;
import com.joas.ocppls.msg.SampledValue;
import com.joas.ocppls.msg.StatusNotification;
import com.joas.ocppls.msg.StopTransaction;
import com.joas.ocppls.msg.TriggerMessage;
import com.joas.ocppls.stack.OCPPConfiguration;
import com.joas.ocppls.stack.OCPPMessage;
import com.joas.ocppls.stack.OCPPStack;
import com.joas.ocppls.stack.OCPPStackListener;
import com.joas.ocppls.stack.OCPPStackProperty;

import java.net.URI;
import java.util.Calendar;

public class OCPPSessionManager implements OCPPStackListener {
    private static final String TAG = "OCPPSessionManager";

    private OCPPStack ocppStack;


    private OCPPSessionManagerListener listener;
    private OCPPSession[] ocppSession;

    private int lastAuthConnectorId = 0;
    private OCPPConfiguration ocppConfiguration;
    private SmartChargerManager smartChargerManager;
    private String basePath;


    public OCPPSessionManager(Context context, int connectorCnt, String basePath, boolean isSoftReset) {
        ocppConfiguration = new OCPPConfiguration();
        ocppConfiguration.NumberOfConnectors = connectorCnt;

        this.basePath = basePath;

        ocppStack = new OCPPStack(context, ocppConfiguration, basePath, isSoftReset);
        ocppStack.setListener(this);
    }


    public OCPPSessionManagerListener getListener() {
        return listener;
    }

    public OCPPStack getOcppStack() {
        return ocppStack;
    }

    public OCPPConfiguration getOcppConfiguration() {
        return ocppConfiguration;
    }

    public SmartChargerManager getSmartChargerManager() {
        return smartChargerManager;
    }

    public OCPPSession getOcppSesstion(int connectorid) {
        return ocppSession[connectorid];
    }

    public void setLastAuthConnectorID(int connectorID) {
        lastAuthConnectorId = connectorID;
    }

    public void init(OCPPStackProperty newOcppProperty) {
        // connector 0 is unused, 0 is charger point packet
        ocppSession = new OCPPSession[ocppConfiguration.NumberOfConnectors + 1];
        for (int i = 0; i <= ocppConfiguration.NumberOfConnectors; i++) {
            ocppSession[i] = new OCPPSession(i, this);
        }

        if (newOcppProperty == null) {
            newOcppProperty = new OCPPStackProperty();
            //Default Value // For Test
            newOcppProperty.cpid = "1234";
            newOcppProperty.useBasicAuth = true;    //
            newOcppProperty.authID = "joas";
            newOcppProperty.authPassword = "j1234";
            newOcppProperty.serverUri = "ws://192.168.0.48:9000/ocpp";
        }

        smartChargerManager = new SmartChargerManager(ocppConfiguration.NumberOfConnectors, ocppConfiguration.ChargeProfileMaxStackLevel, ocppSession, basePath);

        // JSON 방식의 OCPP를 초기화한다.
        ocppStack.init("ocpp-j", newOcppProperty);

        // 커넥터 사용가능 세팅

        ocppStack.startOcpp();
    }

    public void restartManager(OCPPStackProperty newOcppProperty) {
        ocppStack.stopOcpp();
        init(newOcppProperty);
    }

    public void closeManager() {
        ocppStack.closeOcpp();
        smartChargerManager.closeManager();
    }

    public void setListener(OCPPSessionManagerListener newListener) {
        this.listener = newListener;
    }

    public boolean checkConnectorId(int connectorId) {
        return (connectorId > 0 && connectorId <= ocppConfiguration.NumberOfConnectors);
    }

    //========================================================================
    // Call from OCPPSession
    //========================================================================

    public void sendMeterValueRequest(int connectorId, int value, int soc, boolean isSOC) {
        if (checkConnectorId(lastAuthConnectorId) == false) return;
        ocppSession[connectorId].sendMeterValueRequest(connectorId, (long) value, -1, -1, -1, -1, -1, (isSOC ? soc : -1), SampledValue.Context.SAMPLE_PERIODIC, ocppConfiguration.MeterValuesSampledData, true);
    }

    /**
     * Full Version MeterValueRequest
     */

    public void sendMeterValueRequest(int connectorId, long meterVal, int meterValInterval, int current, int curPower, int curOffered, int powerOffered, int soc, SampledValue.Context context, String measurand, boolean isInTransaction) {
        if (connectorId == 0) {
            ocppStack.sendMeterValueRequest(connectorId, meterVal, meterValInterval, current, curPower, curOffered, powerOffered, soc, context, measurand, false, null, -1);
        } else {
            if (checkConnectorId(lastAuthConnectorId) == false) return;
            ocppSession[connectorId].sendMeterValueRequest(connectorId, meterVal, meterValInterval, current, curPower, curOffered, powerOffered, soc, context, measurand, isInTransaction);
        }
    }

    public void sendStatusNotificationRequest(int connectorId, StatusNotification.Status status, StatusNotification.ErrorCode error) {
        ocppStack.sendStatusNotificationRequest(connectorId, status, error, null);
    }

    public void sendStatusNotificationRequest(int connectorId, StatusNotification.Status status, StatusNotification.ErrorCode error, String vendorerrorcode) {
        ocppStack.sendStatusNotificationRequest(connectorId, status, error, vendorerrorcode);
    }

    public void sendDataTransferReq(String messageID, String data) {
        ocppStack.sendDataTransferRequest(messageID, data);
    }

    public void sendDataTransferResponse(DataTransferResponse.Status status, OCPPMessage message, String data) {
        ocppStack.sendDataTransferResponse(status, message, data);
    }

    public void authorizeRequest(String idTag) {
        ocppStack.sendAuthorizeRequest(idTag);
    }

    public void sendFirmwareStatusNotification(FirmwareStatusNotification.Status status) {
        ocppStack.sendFirmwareStatusNotification(status);
    }

    public void sendDiagnosticsStatusNotification(DiagnosticsStatusNotification.Status status) {
        ocppStack.sendDiagnosticsStatusNotification(status);
    }
    //========================================================================
    // Call to OCPPSession
    //========================================================================

    public void authorizeRequest(int connectorId, String idTag) {
        if (checkConnectorId(connectorId) == false) return;
        lastAuthConnectorId = connectorId;
        ocppSession[connectorId].authorizeRequest(idTag);
    }

    public void startSession(int connectorId) {
        if (checkConnectorId(connectorId) == false) return;
        ocppSession[connectorId].startSession();
    }

    public void closeSession(int connectorId) {
        if (checkConnectorId(connectorId) == false) return;
        ocppSession[connectorId].closeSession();
    }

    public void startCharging(int connectorId, int meterStart) {
        if (checkConnectorId(connectorId) == false) return;
        ocppSession[connectorId].startCharging(meterStart);
    }

    public void stopCharging(int connectorId, int meterStop, StopTransaction.Reason reason) {
        if (checkConnectorId(connectorId) == false) return;
        ocppSession[connectorId].stopCharging(meterStop, reason);
    }

    public boolean checkAuthTag(int connectorId, String tag) {
        if (checkConnectorId(connectorId) == false) return false;
        return ocppSession[connectorId].checkAuthTag(tag);
    }


    //================================================================
    //= OCPP Stack Listener
    //================================================================

    @Override
    public void onAuthorizeResponse(AuthorizeResponse response) {
        if (checkConnectorId(lastAuthConnectorId) == false) return;

        ocppSession[lastAuthConnectorId].onAuthorizeResponse(response);
    }

    @Override
    public void onBootNotificationResponse(boolean success) {
        if (listener != null) listener.onBootNotificationResponse(success);
    }

    @Override
    public CancelReservationResponse.Status onCancelReservation(int reservationId) {
        CancelReservationResponse.Status ret = CancelReservationResponse.Status.REJECTED;
        if (listener != null) ret = listener.onCancelReservation(reservationId);
        return ret;
    }

    @Override
    public boolean onRemoteStartTransaction(int connectorId, String idTag, ChargingProfile chargingProfile) {
        return ocppSession[connectorId].onRemoteStartTransaction(connectorId, idTag, chargingProfile);
    }

    @Override
    public boolean onRemoteStopTransaction(int transactionId) {
        for (int i = 1; i < ocppConfiguration.NumberOfConnectors + 1; i++) {
            if (ocppSession[i].getCurTransactionId() == transactionId) {
                listener.onRemoteStopTransaction(i);
                return true;
            }
        }
        return false;

    }

    @Override
    public void onResetRequest(boolean isHard) {
        if (listener != null) listener.onResetRequest(isHard);
    }

    @Override
    public ReserveNowResponse.Status onReserveNow(int connectorId, Calendar expiryDate, String idTag, String parentIdTag, int reservationId) {
        return listener.onReserveNow(connectorId, expiryDate, idTag, parentIdTag, reservationId);
    }

    @Override
    public void onStartTransactionResult(int connectorId, IdTagInfo tagInfo, String startTime, int transactionId) {
        if (checkConnectorId(connectorId) == false) return;
        ocppSession[connectorId].onStartTransactionResult(connectorId, tagInfo, startTime, transactionId);
    }

    @Override
    public boolean onSetChargingProfile(int connectorId, CsChargingProfiles profiles) {
        return smartChargerManager.setProfile(connectorId, profiles);
    }

    @Override
    public void onChangeAvailability(int connectorId, ChangeAvailability.Type type) {
        if (listener != null) listener.onChangeAvailability(connectorId, type);
    }

    @Override
    public boolean onClearChargingProfile(ClearChargingProfile profile) {
        return smartChargerManager.clearChargingProfile(profile);
    }

    @Override
    public void onTriggerMessage(TriggerMessage triggerMessage) {
        if (listener != null) listener.onTriggerMessage(triggerMessage);
    }

    @Override
    public void onUpdateFirmwareRequest(URI location, int retry, Calendar retrieveDate, int retryInterval) {
        if (listener != null)
            listener.onUpdateFirmwareRequest(location, retry, retrieveDate, retryInterval);
    }

    @Override
    public void onOCPPStackError() {

    }

    @Override
    public void onTimeUpdate(Calendar syncTime) {
        if (listener != null) listener.onTimeUpdate(syncTime);
    }

    @Override
    public void onDataTransferResponse(DataTransferResponse.Status status, String messageID, String data) {
        if (listener != null) listener.onDataTransferResponse(status, messageID, data);
    }

    @Override
    public void onDataTransferRequest(OCPPMessage message) {
        if (listener != null) listener.onDataTransferRequest(message);

    }

    @Override
    public boolean onCheckUIChargingStatus() {
        if (listener != null) return listener.onCheckUIChargingStatus();
        else return false;
    }
}
