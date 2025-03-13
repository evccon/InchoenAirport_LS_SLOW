/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:40
 */

package com.joas.ocppls.stack;

// Start of user code (user defined imports)

// End of user code

import com.joas.ocppls.msg.AuthorizeResponse;
import com.joas.ocppls.msg.CancelReservationResponse;
import com.joas.ocppls.msg.ChangeAvailability;
import com.joas.ocppls.msg.ChargingProfile;
import com.joas.ocppls.msg.ClearChargingProfile;
import com.joas.ocppls.msg.CsChargingProfiles;
import com.joas.ocppls.msg.DataTransferResponse;
import com.joas.ocppls.msg.IdTagInfo;
import com.joas.ocppls.msg.ReserveNowResponse;
import com.joas.ocppls.msg.TriggerMessage;

import java.net.URI;
import java.util.Calendar;

/**
 * Description of OCPPStackListener.
 * 
 * @author user
 */
public interface OCPPStackListener {

	public void onAuthorizeResponse(AuthorizeResponse response);
	/**
	 * Description of the method OnOCPPStackConnectEvent.
	 */
	public void onBootNotificationResponse(boolean success);
	public CancelReservationResponse.Status onCancelReservation(int reservationId);
	public boolean onRemoteStartTransaction(int connectorId, String idTag, ChargingProfile chargingProfile);
	public boolean onRemoteStopTransaction(int transactionId);
	public void onResetRequest(boolean isHard);
	public boolean onClearChargingProfile(ClearChargingProfile profile);
	public ReserveNowResponse.Status onReserveNow(int connectorId, Calendar expiryDate, String idTag, String parentIdTag, int reservationId);
	public void onTriggerMessage(TriggerMessage triggerMessage);
	public void onUpdateFirmwareRequest(URI location, int retry, Calendar retrieveDate, int retryInterval);

	public void onStartTransactionResult(int connectorId, IdTagInfo tagInfo, String startTime, int transactionId);

	public boolean onSetChargingProfile(int connectorId, CsChargingProfiles profiles);

	public void onChangeAvailability(int connectorId, ChangeAvailability.Type type);

	public boolean onCheckUIChargingStatus();

	/**
	 * Description of the method OnOCPPStackError.
	 */
	public void onOCPPStackError();

	public void onTimeUpdate(Calendar syncTime);
	public void onDataTransferResponse(DataTransferResponse.Status status, String messageID,String data );
	public void onDataTransferRequest(OCPPMessage message);
}
