/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 13 오후 1:38
 *
 */

package com.joas.ocppui_LS_2ch;

import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.joas.hw.dsp2.DSPControl2;
import com.joas.hw.dsp2.DSPControl2Listener;
import com.joas.hw.dsp2.DSPRxData2;
import com.joas.hw.dsp2.DSPTxData2;
import com.joas.hw.payment.tl3500s.TL3500S;
import com.joas.hw.payment.tl3500s.TL3500SListener;
import com.joas.hw.rfid.RfidReaderListener;
import com.joas.ocppls.chargepoint.OCPPSession;
import com.joas.ocppls.chargepoint.OCPPSessionManager;
import com.joas.ocppls.chargepoint.OCPPSessionManagerListener;
import com.joas.ocppls.msg.CancelReservationResponse;
import com.joas.ocppls.msg.ChangeAvailability;
import com.joas.ocppls.msg.ChargingProfile;
import com.joas.ocppls.msg.DataTransferResponse;
import com.joas.ocppls.msg.IdTagInfo;
import com.joas.ocppls.msg.MeterValue;
import com.joas.ocppls.msg.MeterValues;
import com.joas.ocppls.msg.ReserveNowResponse;
import com.joas.ocppls.msg.SampledValue;
import com.joas.ocppls.msg.StatusNotification;
import com.joas.ocppls.msg.StopTransaction;
import com.joas.ocppls.msg.TriggerMessage;
import com.joas.ocppls.stack.OCPPConfiguration;
import com.joas.ocppls.stack.OCPPMessage;
import com.joas.ocppls.stack.OCPPTransportMonitorListener;
import com.joas.ocppls.stack.Transceiver;
import com.joas.ocppui_LS_2ch.page.PageEvent;
import com.joas.ocppui_LS_2ch.page.PageID;
import com.joas.utils.LogWrapper;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

public class UIFlowManager implements RfidReaderListener, DSPControl2Listener, OCPPSessionManagerListener,
        UpdateManagerListener, TL3500SListener, OCPPTransportMonitorListener {
    public static final String TAG = "UIFlowManager";

    @Override
    public void onUpdateStatus(UpdateManager.UpdateState state) {

    }

    @Override
    public void responseCallback(TL3500S.ResponseType type, Map<String, String> retVal, int ch) {
        switch (type) {
            case Check:
                onTL3500S_Check(retVal, ch);
                break;

            case Search:        //카드 번호 조회 관련 응답 처리
                onTL3500S_Search(retVal, ch);
                break;

            case Pay:           //결제 관련 응답 처리(최초 결제, 실결제)
                onTL3500S_Pay(retVal, ch);
                break;

            case CancelPay:         //취소 결제 관련 응답 처리
                onTL3500S_CancelPay(retVal, ch);
                break;

            case Error:
                onTL3500S_Error(retVal, ch);
                break;

            case Event:             //결제 단말기 응답 이벤트 처리
                onTL3500S_Event(retVal, ch);
                break;

            case GetVersion:        //버전 요청 관련 응답 처리
//                onTL3500S_GetVersion(retVal);
                break;

            case GetConfig:         //단말기 설정 정보 요청 관련 응답 처리
//                onTL3500S_GetConfig(retVal);
                break;
        }
    }

    void onTL3500S_Check(Map<String, String> retVal, int ch) {
        pageManager.hideFaultBox();

        if (tl3500stermChecktimer != null)
            tl3500stermChecktimer.cancel();

        if (istl3500sCommError) {
            faultList.remove(tlfaultinfo);
            tlfaultinfo = null;
            setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);
            istl3500sCommError = false;
        }

    }

    public boolean istl3500sCommError = false;
    FaultInfo tlfaultinfo;

    void onTL3500S_Error(Map<String, String> retVal, int ch) {
        if (!istl3500sCommError) {
            istl3500sCommError = true;
            tlfaultinfo = new FaultInfo(99998, 99998, "결제단말기통신오류", false);
            faultList.add(tlfaultinfo);

            // 충전충이라면 충전을 중지한다.
            if (flowState == UIFlowState.UI_CHARGING) {
                stopReason = StopTransaction.Reason.OTHER;
                LogWrapper.v("StopReason", "TL3500 COMM ERR");
                onChargingStop();
            } else if (flowState == UIFlowState.UI_CONNECTOR_WAIT || flowState == UIFlowState.UI_RUN_CHECK) {
                if (chargeData.isNomember) {
                    //충전금액이 0일경우 무카드 취소 진행
                    onSetPaymentRequestData("2", chargeData.approvalRequestPrice, chargeData.installmentMonth, "4",
                            chargeData.authCode, chargeData.paidDate, chargeData.paidTime, chargeData.pgTransactionSerialNo);

                    //취소 요청
                    String prepayDatetime = chargeData.paidDate + chargeData.paidTime;
                    tl3500s.cancelPay_Partial(channel, chargeData.approvalRequestPrice, 0,
                            chargeData.approvalNumber, prepayDatetime, chargeData.pgTransactionSerialNo, 4);
                }
                onPageStartEvent();
            } else {
                if (flowState != UIFlowState.UI_FINISH_CHARGING && flowState != UIFlowState.UI_MAIN) {
                    onPageStartEvent();
                }
            }

            pageManager.showFaultBox(channel);
            //TODO 결제단말기 오류 코드 수정 필요
            setOcppError(chargeData.curConnectorId, "결제단말기통신오류");

        }

        if (tl3500stermChecktimer != null) tl3500stermChecktimer.cancel();
        tl3500stermChecktimer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                tl3500s.termCheckReq();
            }
        });
        tl3500stermChecktimer.start();


    }

    void onTL3500S_Search(Map<String, String> retVal, int ch) {
        String cardtype = retVal.get("card_type");
        String cnum = "";
        if (cardtype.equals("3")) {
            cnum = retVal.get("cardnum").substring(4);
        } else {
            cnum = retVal.get("cardnum").substring(0, 14);
        }
        onCardTagEvent(cnum, true);
        LogWrapper.d(TAG, "onTL3500S_Search: cardnum:" + cnum);
    }

    void onTL3500S_Pay(Map<String, String> retVal, int ch) {
        try {
            //최초결제 관련 정보 저장
            if (tl3500s.isprepayFlag) {
                if (retVal.get("payCode").equals("1")) { // 정상 승인
                    chargeData.transactionTypeCode = retVal.get("payCode");
                    chargeData.transactionMedium = retVal.get("payType");
                    chargeData.paycardNo = retVal.get("cardNum").replaceFirst("^0+(?!$)", ""); // 앞에 나온 0으로 제거
                    chargeData.paidPrice = Integer.valueOf(retVal.get("totalCost"));
                    chargeData.tax = 0;
                    chargeData.installmentMonth = retVal.get("div");
                    chargeData.authCode = retVal.get("authNum").trim();
                    chargeData.paidDate = retVal.get("payDate");
                    chargeData.paidTime = retVal.get("payTime");
                    chargeData.tid = retVal.get("uniqueNum");
                    chargeData.pgTransactionSerialNo = retVal.get("pgnum");
                    chargeData.mrhstNo = retVal.get("regNum");
                    chargeData.terminalNo = retVal.get("termId");
                    chargeData.issureCode = "";
                    chargeData.issureName = "";
                    chargeData.resCode = "";
                    chargeData.resMessage = "";

                    chargeData.prePayTid = chargeData.tid;

                    //결제 성공
                    onPrepaySuccess();
                } else if (retVal.get("payCode").equals("X")) {
                    //결제 실패
                    chargeData.transactionTypeCode = retVal.get("payCode");
                    chargeData.transactionMedium = retVal.get("payType");
                    chargeData.paycardNo = retVal.get("cardNum").replaceFirst("^0+(?!$)", ""); // 앞에 나온 0으로 제거
                    chargeData.paidPrice = Integer.valueOf(retVal.get("totalCost"));
                    chargeData.tax = 0;
                    chargeData.installmentMonth = retVal.get("div");
                    chargeData.authCode = retVal.get("authNum").trim();
                    chargeData.paidDate = retVal.get("payDate");
                    chargeData.paidTime = retVal.get("payTime");
                    chargeData.tid = retVal.get("uniqueNum");
                    chargeData.pgTransactionSerialNo = retVal.get("pgnum");
                    chargeData.mrhstNo = retVal.get("regNum");
                    chargeData.terminalNo = retVal.get("termId");
                    chargeData.issureCode = "";
                    chargeData.issureName = "";
                    chargeData.resCode = retVal.get("retCode");
                    chargeData.resMessage = retVal.get("errMsg").replaceAll("\\p{Z}", "").trim();

                    chargeData.prePayTid = chargeData.tid;

                    onPrepayFailed();
                }
            } else if (!tl3500s.isprepayFlag) {       //실결제 관련 정보 저장
                if (retVal.get("payCode").equals("1")) { // 정상 승인
//                    //실결제 승인금액
//                    poscoChargerInfo.paymentDealApprovalCost = Integer.parseInt(retVal.get("totalCost").replace(" ", ""));
//                    //실결제 승인번호
//                    poscoChargerInfo.paymentDealApprovalNo = retVal.get("authNum").replace(" ", ""); // 승인번호
//                    //실결제 거래시간
//                    poscoChargerInfo.paymentDealApprovalTime = (retVal.get("payDate")+retVal.get("payTime")).replace(" ","");
//                    //실결제 거래번호
//                    poscoChargerInfo.paymentDealId = retVal.get("uniqueNum").replace(" ","");
//                    //실결제 거래일련번호
//                    poscoChargerInfo.paymentDealSerialNo = retVal.get("pgnum").replace(" ", "");
//                    //결제 성공
//                    poscoChargerInfo.paymentResultStat = "01";
//                    pageManager.paymentRealPayView.onPaySuccess();

                } else if (retVal.get("payCode").equals("X")) {
//                    //결제 실패
//                    poscoChargerInfo.paymentErrCode = retVal.get("retCode");
//                    poscoChargerInfo.paymentErrmsg = retVal.get("errMsg").replaceAll("\\p{Z}", "").trim();
//                    poscoChargerInfo.paymentResultStat = "02";
//                    pageManager.paymentRealPayView.onPayFailed();
                }
            }
        } catch (Exception e) {
            LogWrapper.e(TAG, "onTL3500S_Pay:" + e.toString());
        }
    }

    void onTL3500S_CancelPay(Map<String, String> retVal, int ch) {
        try {

            if (retVal.get("payCode").equals("1")) { // 정상 승인
                chargeData.transactionTypeCode = retVal.get("payCode");
                chargeData.transactionMedium = retVal.get("payType");
                chargeData.paycardNo = retVal.get("cardNum").replaceFirst("^0+(?!$)", ""); // 앞에 나온 0으로 제거
                chargeData.paidPrice = Integer.valueOf(retVal.get("totalCost"));
                chargeData.tax = 0;
                chargeData.installmentMonth = retVal.get("div");
                chargeData.authCode = retVal.get("authNum").trim();
                chargeData.paidDate = retVal.get("payDate");
                chargeData.paidTime = retVal.get("payTime");
                chargeData.tid = retVal.get("uniqueNum");
                chargeData.pgTransactionSerialNo = "";
                chargeData.mrhstNo = retVal.get("regNum");
                chargeData.terminalNo = retVal.get("termId");
                chargeData.issureCode = "";
                chargeData.issureName = "";
                chargeData.resCode = "";
                chargeData.resMessage = "";

                //취소성공
                onCancelPaySuccess();

            } else if (retVal.get("payCode").equals("X")) {
                //취소실패
                chargeData.transactionTypeCode = retVal.get("payCode");
                chargeData.transactionMedium = retVal.get("payType");
                chargeData.paycardNo = retVal.get("cardNum").replaceFirst("^0+(?!$)", ""); // 앞에 나온 0으로 제거
                chargeData.paidPrice = Integer.valueOf(retVal.get("totalCost"));
                chargeData.tax = 0;
                chargeData.installmentMonth = retVal.get("div");
                chargeData.authCode = retVal.get("authNum").trim();
                chargeData.paidDate = retVal.get("payDate");
                chargeData.paidTime = retVal.get("payTime");
                chargeData.tid = retVal.get("uniqueNum");
                chargeData.pgTransactionSerialNo = "";
                chargeData.mrhstNo = retVal.get("regNum");
                chargeData.terminalNo = retVal.get("termId");
                chargeData.issureCode = "";
                chargeData.issureName = "";
                chargeData.resCode = retVal.get("retCode");
                chargeData.resMessage = retVal.get("errMsg").replaceAll("\\p{Z}", "").trim();

                //취소실패
                onCancelPayFailed();
            }
        } catch (Exception e) {
            LogWrapper.e(TAG, "onTL3500S_CancelPay:" + e.toString());
        }
    }


    void onTL3500S_Event(Map<String, String> retVal, int ch) {
        if (retVal.get("event") != null) {
            String eventcode = retVal.get("event");
            switch (eventcode) {
                case "I":
                    onTL3500s_ICInputEventOccured();
                    break;
                case "O":
//                    onTL3500s_ICOutputEAventOccured();
                    break;
                case "F":
                    onTL3500s_FallbackEventOccured();
                    break;
                case "R":
//                    onTL3500s_RFPayEventOccured();
                    break;
                case "M":       //삼성페이
//                    onPrepayWait();
                    break;
            }
        }
    }

    void onTL3500s_FallbackEventOccured() {
        pageManager.hideAuthWaitView();
        chargeData.faultBoxContent = "카드를 다시 삽입해주세요.";
        chargeData.faultBoxContent += "\r\n";
        pageManager.showFaultBox2(channel);
    }

    void onTL3500s_ICInputEventOccured() {
        // 승인대기 화면 전환
        if (flowState == UIFlowState.UI_INSERT_CREDIT_CARD) {
            pageManager.showAuthWaitView(channel);
        }
    }

    /**
     * 카드 단말기 이벤트 I 혹은 M 수신에 의한 결제요청을 진행
     */

    public void onPrepayWait() {
        //결제응답 대기상태 변환
        setUIFlowState(UIFlowState.UI_CREDIT_APPROVAL_WAIT);
        pageManager.changePage(PageID.CREDIT_APPROVAL_WAIT, channel);

    }

    public void onPrepaySuccess() {
        setUIFlowState(UIFlowState.UI_AUTH_WAIT);
        //make json obj string
        String data = makePaymentJsonObject();
        //최초결제 정보 DataTransfer.req 전송
        ocppSessionManager.sendDataTransferReq(TypeDefine.DATA_TRANSFER_MESSAGEID_PAYMENT_INFO, data);

        // 비회원 인증 시 승인번호(authnum)로 전달
        ocppSessionManager.authorizeRequest(chargeData.curConnectorId, chargeData.authCode);
    }

    public void onPrepayFailed() {
        pageManager.hideAuthWaitView();
        chargeData.messageBoxTitle = "최초결제 실패";
        chargeData.messageBoxContent = chargeData.resCode + "\n" + chargeData.resMessage;
        pageManager.showAuthalarmBox(channel);

        String data = makePaymentJsonObject();
        //최초결제 정보 DataTransfer.req 전송
        ocppSessionManager.sendDataTransferReq(TypeDefine.DATA_TRANSFER_MESSAGEID_PAYMENT_INFO, data);

        //최초결제 대기상태로 전환
        setUIFlowState(UIFlowState.UI_INSERT_CREDIT_CARD);
        pageManager.changePage(PageID.INSERT_CREDIT_CARD, channel);
    }

    public void onCancelPaySuccess() {
        //make json obj string
        String data = makeCancelPaymentJsonObject();
        //취소(부분) 정보 DataTransfer.req 전송
        ocppSessionManager.sendDataTransferReq(TypeDefine.DATA_TRANSFER_MESSAGEID_CANCEL_PAYMENT_INFO, data);

        // 커넥터 대기에서 back 버튼으로 취소 이벤트
        if (flowState == UIFlowState.UI_CONNECTOR_WAIT) {
            if (chargeData.isNomember) {
                //요청정보 관련 데이터 세팅
                onSetPaymentRequestData("1", chargeData.approvalRequestPrice, "", "", "", "", "", "");
                //최초결제 요청
//                tl3500s.payReq_G(chargeData.approvalRequestPrice, 0, true, channel, cpConfig.chargerID);
                tl3500s.payReq_G(chargeData.approvalRequestPrice, 0, true, channel, "인천공항 전기차충전");

                setUIFlowState(UIFlowState.UI_INSERT_CREDIT_CARD);
                pageManager.changePage(PageID.INSERT_CREDIT_CARD, channel);
            }
        }

    }

    int cancelpayRetry = 0;

    public void onCancelPayFailed() {
        pageManager.hideAuthWaitView();
        chargeData.messageBoxTitle = "취소결제 실패";
        chargeData.messageBoxContent = chargeData.resCode + "\n" + chargeData.resMessage;
        pageManager.showAuthalarmBox(channel);

        if (cancelpayRetry <= TypeDefine.CANCELPAY_MAX_RETRY_ATTEMPTS) {
            if (chargeData.chargingCost == 0) {
                //충전금액이 0일경우 무카드 취소 진행
                onSetPaymentRequestData("2", chargeData.approvalRequestPrice, chargeData.installmentMonth, "4",
                        chargeData.authCode, chargeData.paidDate, chargeData.paidTime, chargeData.pgTransactionSerialNo);
                //취소 요청
                String prepayDatetime = chargeData.paidDate + chargeData.paidTime;
                tl3500s.cancelPay_Partial(channel, chargeData.approvalRequestPrice, 0, chargeData.approvalNumber, prepayDatetime, chargeData.pgTransactionSerialNo, 4);
            } else if (chargeData.chargingCost < chargeData.approvalRequestPrice) {
                //충전금액이 최초결제 금액보다 적을경우 차액 부분취소 진행
                int cancelCost = chargeData.approvalRequestPrice - (int) chargeData.chargingCost;

                onSetPaymentRequestData("3", cancelCost, chargeData.installmentMonth, "5",
                        chargeData.authCode, chargeData.paidDate, chargeData.paidTime, chargeData.pgTransactionSerialNo);
                //취소(부분) 요청
                String prepayDatetime = chargeData.paidDate + chargeData.paidTime;
                tl3500s.cancelPay_Partial(channel, chargeData.approvalRequestPrice, 0, chargeData.approvalNumber, prepayDatetime, chargeData.pgTransactionSerialNo, 5);
            }

            cancelpayRetry++;
        }


    }

    /**
     * guestAdvancePayment
     * DataTransfer의 data로 전송하기위해 json objstr 생성함수
     */
    public String makePaymentJsonObject() {
        String retstr = "";
        try {
            JSONObject obj = new JSONObject();

            //G365 Protocol
            obj.put("approvalNum", chargeData.authCode);                      //승인번호
            int number = chargeData.paidPrice * 100;
            double decimalNumber = (double) number / 100;
            obj.put("authAmount", String.format("%.2f", decimalNumber));      //승인금액
            obj.put("cardNum", chargeData.paycardNo);                         //카드번호
            obj.put("connectorId", chargeData.curConnectorId);                //connector id
            obj.put("payId", chargeData.tid);                                 //거래고유번호
            obj.put("pgTransactionNum", chargeData.pgTransactionSerialNo);    //pg거래일련번호
            obj.put("transactionDate", chargeData.paidDate);                  //매출일자
            obj.put("transactionTime", chargeData.paidTime);                  //매출기간
            obj.put("msg", chargeData.resMessage);
            obj.put("code", chargeData.resCode);

            retstr = obj.toString();

        } catch (Exception e) {
            LogWrapper.e(TAG, "DataTransfer(Payment) Json Make Err:" + e.toString());
        }

        return retstr;
    }


    /**
     * partialCancellation
     * DataTransfer의 data로 전송하기위해 json objstr 생성함수
     */
    public String makeCancelPaymentJsonObject() {
        String retstr = "";
        try {
            JSONObject obj = new JSONObject();

            //G365 Protocol
            obj.put("connectorId", chargeData.curConnectorId);                //connector id
            obj.put("transactionId", chargeData.curTransactionID);          // (int)transaction ID
            obj.put("startSoC", 0);
            obj.put("stopSoC", 0);
            int elapsedTime = (int) (chargeData.chargingTime / 1000);
            obj.put("elapsedTime", elapsedTime);
            obj.put("cardNum", chargeData.paycardNo);                         //카드번호
            obj.put("approvalNum", chargeData.authCode);                      //승인번호
            obj.put("tid", chargeData.prePayTid);                                    //거래고유번호
            obj.put("pTid", chargeData.tid);                                    //부분취소 PTid
            obj.put("cancelSeq", "" + cancelpayRetry);                                 // (String) 취소 차수

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(chargeData.chargeStartTime);
            calendar.add(Calendar.HOUR_OF_DAY, timeOffset);
            Date chargeStartTime = calendar.getTime();

            calendar = Calendar.getInstance();
            calendar.setTime(chargeData.chargeEndTime);
            calendar.add(Calendar.HOUR_OF_DAY, timeOffset);
            Date chargeEndTime = calendar.getTime();

            obj.put("startDate", formatter.format(chargeStartTime));           // 충전시작일시
            obj.put("stopDate", formatter.format(chargeEndTime));                // 충전종료일시

            obj.put("payMethod", chargeData.transactionMedium);              // 결제방식(IC)
            obj.put("transactionDate", chargeData.paymentDate + chargeData.paymentTime);                  //결제일시
            obj.put("cancelDate", chargeData.paidDate + chargeData.paidTime);                  // 취소결제일시
            obj.put("energy", chargeData.measureWh);                           //충전량

//            obj.put("chargingAmount", "" + (int) chargeData.chargingCost); // (String)충전금액 (충전완료 후 전력량으로 계산된 금액)
//            obj.put("authAmount", "" + chargeData.requestPrice);           // (String) 최초결제금액 (선 결제시 승인된 금액)
//            obj.put("cancelAmount", "" + chargeData.approvalRequestPrice);        //(String)취소예정금액 (최초결제금액 - 충전금액)
//            obj.put("finalAmount", "" + (chargeData.requestPrice - chargeData.approvalRequestPrice));     // (String)최종결제금액 (최초결제금액 - 취소예정금액)

            obj.put("chargingAmount", String.format("%.2f", chargeData.chargingCost)); // (String)충전금액 (충전완료 후 전력량으로 계산된 금액)
            obj.put("authAmount", chargeData.requestPrice + ".00");           // (String) 최초결제금액 (선 결제시 승인된 금액)
            obj.put("cancelAmount", chargeData.approvalRequestPrice + ".00");        //(String)취소예정금액 (최초결제금액 - 충전금액)
            obj.put("finalAmount", (chargeData.requestPrice - chargeData.approvalRequestPrice) + ".00");     // (String)최종결제금액 (최초결제금액 - 취소예정금액)

            retstr = obj.toString();

        } catch (Exception e) {
            LogWrapper.e(TAG, "DataTransfer(Cancel Payment) Json Make Err:" + e.toString());
        }

        return retstr;
    }

    /**
     * finalChargingRpt
     * DataTransfer
     *
     * @return
     */
    public String makeFinalChargingJsonObject() {
        String retstr = "";
        try {
            JSONObject obj = new JSONObject();

            //G365 Protocol
            obj.put("connectorId", chargeData.curConnectorId);                //connector id
            obj.put("transactionId", chargeData.curTransactionID);
            obj.put("startSoC", 0);
            obj.put("stopSoC", 0);

            int elapsedTime = (int) (chargeData.chargingTime / 1000);
            obj.put("elapsedTime", elapsedTime);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(chargeData.chargeStartTime);
            calendar.add(Calendar.HOUR_OF_DAY, timeOffset);
            Date chargeStartTime = calendar.getTime();

            calendar = Calendar.getInstance();
            calendar.setTime(chargeData.chargeEndTime);
            calendar.add(Calendar.HOUR_OF_DAY, timeOffset);
            Date chargeEndTime = calendar.getTime();

            obj.put("startDate", formatter.format(chargeStartTime));         // 충전시작일시
            obj.put("stopDate", formatter.format(chargeEndTime));            // 충전종료일시

            obj.put("energy", chargeData.measureWh);                                    //충전량
            obj.put("chargingAmount", String.format("%.2f", chargeData.chargingCost));  // 충전금액 (충전완료 후 전력량으로 계산된 금액)

            retstr = obj.toString();

        } catch (Exception e) {
            LogWrapper.e(TAG, "DataTransfer(finalChargingRpt) Json Make Err:" + e.toString());
        }

        return retstr;
    }

    @Override
    public void onOCPPTransportRecvRaw(String data) {

    }

    @Override
    public void onOCPPTransportSendRaw(String data) {

    }

    @Override
    public void onOCPPTransportConnected() {
        onChangeOcppServerConnectStatus(true);

        chargeData.serverStat = true;
    }

    @Override
    public void onOCPPTransportDisconnected() {
        chargeData.serverStat = false;
//        Transceiver trans = ocppSessionManager.getOcppStack().getTransceiver();
//        //transaciton monitor stat true
//        if (trans.getTransactionMonitorStat()) {
//            trans.setTransactionMonitorStat(false);
//            //충전중 모니터링시 생성된 트랜젝션 패킷 db에서 제거
//            trans.removeSaveTransactionMessage(trans.getLastUniqeId_Metervalue());
//            trans.removeSaveTransactionMessage(trans.getLastUniqeId_Stoptransaction());
//        }
        onChangeOcppServerConnectStatus(false);

    }

    // 통신 연결 상태 관리
    public void onChangeOcppServerConnectStatus(boolean status) {
        // 타이밍상 Listener 생성 보다 connection이 더 빠른경우에 호출이 안되는 경우가 있음
        // bootNotification에서 연결 상태 Update 할 필요가 있음(위에 TimeSync에서 수행)
        mainActivity.setCommConnStatus(status);
    }


    public enum UIFlowState {
        UI_MAIN,
        UI_SELECT,
        UI_SELECT_PAYMENT_METHOD,
        UI_SELECT_CHARGING_OPTION,
        UI_SET_CHARGING_OPTION,
        UI_CARD_TAG,
        UI_AUTH_WAIT,
        UI_AUTH_ALARM,
        UI_INSERT_CREDIT_CARD,
        UI_CREDIT_APPROVAL_WAIT,
        UI_CONNECTOR_WAIT,
        UI_RUN_CHECK,
        UI_CHARGING,
        UI_FINISH_CHARGING,
        UI_UNPLUG,
        UI_SERVICE_STOP,
    }

    MultiChannelUIManager multiChannelUIManager;
    PageManager pageManager;
    DSPControl2 dspControl;

    OCPPUI2CHActivity mainActivity;

    UIFlowState flowState = UIFlowState.UI_SELECT;
    ChargeData chargeData;
    CPConfig cpConfig;
    MeterConfig mconfig;

    boolean isDspReady = false;
    boolean isDspAvalCharge = false;
    boolean isDspDoor = false;
    boolean isDspPlug = false;
    boolean isDspChargeRun = false;
    boolean isDspChargeFinish = false;

    boolean isDspFault = false;
    boolean isPreDspFault = false;
    boolean isEmergencyPressed = false;

    long lastMeterValue = -1;

    String lastCardNum = "";

    int meterTimerCnt = 0;
    long lastClockedMeterValue = -1;

    boolean isRemoteStarted = false;
    boolean isHardResetEvent = false;
    boolean isSoftResetEvent = false;
    boolean isConnectorOperative = true;
    boolean restartBySoftReset = false;
    public boolean isStopByCard = false;
    boolean isStopByStartTransactionInvalid = false;
    boolean isStopbySuspendedEVSE = false;
    boolean isStopByCompliteCharging = false;

    int unplugTimerCnt = 0;
    int finishWaitCnt = 0;


    OCPPSessionManager ocppSessionManager;

    FaultManager faultManager;
    Vector<FaultInfo> faultList = new Vector<FaultInfo>();

    StopTransaction.Reason stopReason = StopTransaction.Reason.OTHER;
    double powerLimit = -1;

    int dspVersion = 0;

    public TL3500S tl3500s;
    // ReserveNow
    ReserveInfo reserveInfo = new ReserveInfo();

    int channel;

    TimeoutTimer tl3500stermChecktimer = null;
    boolean nonChangeMeterStopFlag = false;
    int timeOffset = 0;
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());

    public UIFlowManager(int chan, OCPPUI2CHActivity activity, MultiChannelUIManager uiManager, ChargeData data, CPConfig config, MeterConfig mconfig, OCPPSessionManager sessionManager, DSPControl2 control, PageManager page) {
        channel = chan;
        mainActivity = activity;
        multiChannelUIManager = uiManager;
        chargeData = data;
        cpConfig = config;
        ocppSessionManager = sessionManager;
        dspControl = control;
        pageManager = page;
        this.mconfig = mconfig;

        // FaultManager를 생성한다.
        faultManager = new FaultManager(dspControl, mainActivity, chargeData.dspChannel);

        if (multiChannelUIManager.getTL3500S() != null)
            tl3500s = multiChannelUIManager.getTL3500S();

        initStartState();
    }

    public void setPageManager(PageManager manager) {
        pageManager = manager;
    }

    public DSPControl2 getDspControl() {
        return dspControl;
    }

    public CPConfig getCpConfig() {
        return cpConfig;
    }

    public ChargeData getChargeData() {
        return chargeData;
    }

    public MultiChannelUIManager getMultiChannelUIManager() {
        return multiChannelUIManager;
    }

    public int getOcppConfigConnectorTimeout() {
        return ocppSessionManager.getOcppConfiguration().ConnectionTimeOut;
    }

    public UIFlowState getUIFlowState() {
        return flowState;
    }

    void setUIFlowState(UIFlowState state) {
        flowState = state;
        Log.v(TAG, "curConnectID:" + chargeData.curConnectorId + " > UIflowstate: " + flowState);
        processChangeState(flowState);

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                UIprocessChangeState(flowState);
            }
        });

    }

    public void setStopReason(StopTransaction.Reason reason) {
        stopReason = reason;
    }

    public int getDspVersion() {
        return dspVersion;
    }

    public int getUIFlowChannel() {
        return this.channel;
    }


    /**
     * UI 상태값이 바뀔 때 수행되어야 할 부분을 구현
     * DSP의 UI 상태값 변경, 도어, 변경 충전시작 관련
     * DSP 이외에 다른 동작은 되도록 추가하지 말것(타 이벤트에서 처리. 이 함수는 DSP에서 처리하는 것을 모아서 하나로 보려고함)
     *
     * @param state 바뀐 UI 상태 값
     */
    void processChangeState(UIFlowState state) {
        switch (state) {
            case UI_MAIN:
            case UI_SELECT:
                initStartState();
                break;
            case UI_CARD_TAG:
                break;
            case UI_CONNECTOR_WAIT:
                dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_CONNECT);
                // Ctype인 경우에는 도어를 오픈할 필요가 없음
                if (chargeData.connectorType != TypeDefine.ConnectorType.CTYPE) {
                    dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);
                }
                break;

            case UI_RUN_CHECK:
                dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_START_CHARGE);
                dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.START_CHARGE, true);
                break;

            case UI_FINISH_CHARGING:
                dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_FINISH_CHARGE);

                // Ctype인 경우에는 도어를 오픈할 필요가 없음
                if (chargeData.connectorType != TypeDefine.ConnectorType.CTYPE) {
                    dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);
                }
                unplugTimerCnt = 0;
                break;
        }
    }

    public void UIprocessChangeState(UIFlowState state) {
        ImageView iv1 = mainActivity.findViewById(R.id.imageView2);
        TextView tv1 = mainActivity.findViewById(R.id.tvVolt1);
        ImageView iv2 = mainActivity.findViewById(R.id.imageView3);
        TextView tv2 = mainActivity.findViewById(R.id.tvAmp1);
        ImageView iv3 = mainActivity.findViewById(R.id.imageView4);
        TextView tv3 = mainActivity.findViewById(R.id.tvPower1);
        ImageView iv4 = mainActivity.findViewById(R.id.imageView5);
        TextView tv4 = mainActivity.findViewById(R.id.textView5);
        ImageView iv5 = mainActivity.findViewById(R.id.imageView6);
        TextView tv5 = mainActivity.findViewById(R.id.textView6);
        ImageView iv6 = mainActivity.findViewById(R.id.imageView7);
        TextView tv6 = mainActivity.findViewById(R.id.textView7);

        switch (state) {
            case UI_SELECT:
                iv1.setImageResource(R.drawable.icon_plugselect_on);
                tv1.setTextColor(Color.parseColor("#ffffff"));
                iv3.setImageResource(R.drawable.icon_pay_off);
                tv3.setTextColor(Color.parseColor("#515c6a"));
                iv4.setImageResource(R.drawable.icon_chargeoption_off);
                tv4.setTextColor(Color.parseColor("#515c6a"));
                iv2.setImageResource(R.drawable.icon_plug_off);
                tv2.setTextColor(Color.parseColor("#515c6a"));
                iv5.setImageResource(R.drawable.icon_charging_off);
                tv5.setTextColor(Color.parseColor("#515c6a"));
                iv6.setImageResource(R.drawable.icon_finish_off);
                tv6.setTextColor(Color.parseColor("#515c6a"));
                break;
            case UI_SELECT_PAYMENT_METHOD:
            case UI_CARD_TAG:
                iv1.setImageResource(R.drawable.icon_plugselect_off);
                tv1.setTextColor(Color.parseColor("#515c6a"));
                iv3.setImageResource(R.drawable.icon_pay_on);
                tv3.setTextColor(Color.parseColor("#ffffff"));
                iv4.setImageResource(R.drawable.icon_chargeoption_off);
                tv4.setTextColor(Color.parseColor("#515c6a"));
                iv2.setImageResource(R.drawable.icon_plug_off);
                tv2.setTextColor(Color.parseColor("#515c6a"));
                iv5.setImageResource(R.drawable.icon_charging_off);
                tv5.setTextColor(Color.parseColor("#515c6a"));
                iv6.setImageResource(R.drawable.icon_finish_off);
                tv6.setTextColor(Color.parseColor("#515c6a"));
                break;
            case UI_SELECT_CHARGING_OPTION:
            case UI_SET_CHARGING_OPTION:
            case UI_INSERT_CREDIT_CARD:
                iv1.setImageResource(R.drawable.icon_plugselect_off);
                tv1.setTextColor(Color.parseColor("#515c6a"));
                iv3.setImageResource(R.drawable.icon_pay_off);
                tv3.setTextColor(Color.parseColor("#515c6a"));
                iv4.setImageResource(R.drawable.icon_chargeoption_on);
                tv4.setTextColor(Color.parseColor("#ffffff"));
                iv2.setImageResource(R.drawable.icon_plug_off);
                tv2.setTextColor(Color.parseColor("#515c6a"));
                iv5.setImageResource(R.drawable.icon_charging_off);
                tv5.setTextColor(Color.parseColor("#515c6a"));
                iv6.setImageResource(R.drawable.icon_finish_off);
                tv6.setTextColor(Color.parseColor("#515c6a"));
                break;
            case UI_CONNECTOR_WAIT:
                iv1.setImageResource(R.drawable.icon_plugselect_off);
                tv1.setTextColor(Color.parseColor("#515c6a"));
                iv3.setImageResource(R.drawable.icon_pay_off);
                tv3.setTextColor(Color.parseColor("#515c6a"));
                iv4.setImageResource(R.drawable.icon_chargeoption_off);
                tv4.setTextColor(Color.parseColor("#515c6a"));
                iv2.setImageResource(R.drawable.icon_plug_on);
                tv2.setTextColor(Color.parseColor("#ffffff"));
                iv5.setImageResource(R.drawable.icon_charging_off);
                tv5.setTextColor(Color.parseColor("#515c6a"));
                iv6.setImageResource(R.drawable.icon_finish_off);
                tv6.setTextColor(Color.parseColor("#515c6a"));
                break;
            case UI_CHARGING:
                iv1.setImageResource(R.drawable.icon_plugselect_off);
                tv1.setTextColor(Color.parseColor("#515c6a"));
                iv3.setImageResource(R.drawable.icon_pay_off);
                tv3.setTextColor(Color.parseColor("#515c6a"));
                iv4.setImageResource(R.drawable.icon_chargeoption_off);
                tv4.setTextColor(Color.parseColor("#515c6a"));
                iv2.setImageResource(R.drawable.icon_plug_off);
                tv2.setTextColor(Color.parseColor("#515c6a"));
                iv5.setImageResource(R.drawable.icon_charging_on);
                tv5.setTextColor(Color.parseColor("#ffffff"));
                iv6.setImageResource(R.drawable.icon_finish_off);
                tv6.setTextColor(Color.parseColor("#515c6a"));
                break;
            case UI_FINISH_CHARGING:
                iv1.setImageResource(R.drawable.icon_plugselect_off);
                tv1.setTextColor(Color.parseColor("#515c6a"));
                iv3.setImageResource(R.drawable.icon_pay_off);
                tv3.setTextColor(Color.parseColor("#515c6a"));
                iv4.setImageResource(R.drawable.icon_chargeoption_off);
                tv4.setTextColor(Color.parseColor("#515c6a"));
                iv2.setImageResource(R.drawable.icon_plug_off);
                tv2.setTextColor(Color.parseColor("#515c6a"));
                iv5.setImageResource(R.drawable.icon_charging_off);
                tv5.setTextColor(Color.parseColor("#515c6a"));
                iv6.setImageResource(R.drawable.icon_finish_on);
                tv6.setTextColor(Color.parseColor("#ffffff"));

                break;
        }
    }

    void initStartState() {
        dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_READY);
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.READY, true);
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.START_CHARGE, false);
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, false);
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, false);

        // 변수 초기화
        chargeData.measureWh = 0;
        chargeData.chargingTime = 0;
        chargeData.chargingCost = 0;
        powerLimit = -1.0d;

        unplugTimerCnt = 0;
        finishWaitCnt = 0;
        unitPriceIndex = 0;
    }

    public void onPageStartEvent() {
//        setUIFlowState(UIFlowState.UI_MAIN);
//        pageManager.changePage(PageID.MAIN_COVER, channel);

        setUIFlowState(UIFlowState.UI_SELECT);
        if (cpConfig.isFastCharger) {
            pageManager.changePage(PageID.SELECT_FAST, channel);
        } else {
            pageManager.changePage(PageID.SELECT_SLOW, channel);
        }

        if (cpConfig.useTl3500S) {
            tl3500s.termReadyReq();
        }
        multiChannelUIManager.rfidReaderRelease(channel);
        pageManager.hideAuthWaitView();
        pageManager.hideConwaitBox();


        //fault status check
        if (chargeData.isdspCommError || chargeData.ismeterCommError || istl3500sCommError) {
            if (chargeData.ocppStatus != StatusNotification.Status.FAULTED) {
                if (chargeData.ocppStatus != StatusNotification.Status.FAULTED) {
                    for (FaultInfo fInfo : faultList) {
                        setOcppError(chargeData.curConnectorId, "");
                    }
                }
            } else if ((chargeData.ocppStatus == StatusNotification.Status.FAULTED)
                    && (chargeData.isdspCommError == false && chargeData.ismeterCommError == false
                    && istl3500sCommError == false)) {
                if (isConnectorOperative) {
                    setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);
                }
            }
        } else {
            if (isDspFault == false) {
                if (isConnectorOperative) {
                    setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);
                }
            }
        }

        ocppSessionManager.closeSession(chargeData.curConnectorId);

        mainActivity.setRemoteStartedVisible(View.INVISIBLE);
        isRemoteStarted = false;
        isStopByCard = false;
        isStopByStartTransactionInvalid = false;
        isStopbySuspendedEVSE = false;
        isStopByCompliteCharging = false;
        Transceiver transceiver = ocppSessionManager.getOcppStack().getTransceiver();
        transceiver.setTransactionMonitorStat(false);

        chargeData.isNomember = false;
    }

    /**
     * Select화면에서 선택되었을때 이벤트 발생
     *
     * @param event
     */

    public void onPageSelectEvent(PageEvent event) {
        // Fault 인 경우에 초기화면으로 돌아감
        if (isDspFault) {
            pageManager.showFaultBox(channel);
        } else if (chargeData.isdspCommError) {
            pageManager.showFaultBox(channel);
        }
        if (istl3500sCommError) {
            pageManager.showFaultBox(channel);
        }
        if (isMeterCommErr) {
            pageManager.showFaultBox(channel);
        }
        if (!isConnectorOperative) {
            if (flowState == UIFlowState.UI_SELECT) pageManager.showUnavailableConView();
        }

        if (isDspFault || chargeData.isdspCommError || istl3500sCommError || isMeterCommErr || !isConnectorOperative)
            return;

        switch (event) {
            case SELECT_BTYPE_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.BTYPE;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_SLOW_BTYPE);
                break;
            case SELECT_CTYPE_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.CTYPE;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_SLOW_CTYPE);
                break;
            case SELECT_AC3_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.AC3;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_FAST_AC3);
                break;
            case SELECT_CHADEMO_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.CHADEMO;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_FAST_DCCHADEMO);
                break;
            case SELECT_DCCOMBO_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.DCCOMBO;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_FAST_DCCOMBO);
                break;
        }

        if (cpConfig.isAuthSkip) {
            setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
            doAuthComplete();
        } else {
            // OCPP Session Start
            ocppSessionManager.startSession(chargeData.curConnectorId);

            if (cpConfig.useTl3500S) {
                //Next Flow. select payment method
                multiChannelUIManager.rfidReaderRequest(channel);
                setUIFlowState(UIFlowState.UI_SELECT_PAYMENT_METHOD);
                pageManager.changePage(PageID.SELECT_PAYMENT_METHOD, channel);
            } else {
                // Next Flow. Card Tag
                chargeData.isNomember = false;
                setUIFlowState(UIFlowState.UI_CARD_TAG);
                pageManager.changePage(PageID.CARD_TAG, channel);
            }
        }

    }


    public void onPageCommonEvent(PageEvent event) {
        switch (event) {
            case GO_HOME:
                if (flowState == UIFlowState.UI_SELECT) {
                    setUIFlowState(UIFlowState.UI_MAIN);
                    pageManager.changePage(PageID.MAIN_COVER, channel);
                    return;
                }
                if (flowState == UIFlowState.UI_CONNECTOR_WAIT || flowState == UIFlowState.UI_RUN_CHECK) {
                    if (chargeData.isNomember) {
                        //충전금액이 0일경우 무카드 취소 진행
                        onSetPaymentRequestData("2", chargeData.approvalRequestPrice, chargeData.installmentMonth, "4",
                                chargeData.authCode, chargeData.paidDate, chargeData.paidTime, chargeData.pgTransactionSerialNo);

                        //취소 요청
                        String prepayDatetime = chargeData.paidDate + chargeData.paidTime;
                        tl3500s.cancelPay_Partial(channel, chargeData.approvalRequestPrice, 0,
                                chargeData.approvalNumber, prepayDatetime, chargeData.pgTransactionSerialNo, 4);
                    }
                    onPageStartEvent();

                } else if (flowState == UIFlowState.UI_CHARGING || flowState == UIFlowState.UI_FINISH_CHARGING || flowState == UIFlowState.UI_UNPLUG) {
                    if (pageManager.getCurPageID() == PageID.SELECT_SLOW || pageManager.getCurPageID() == PageID.SELECT_FAST) {
                        pageManager.changePage(PageID.MAIN_COVER, channel);
                    } else {
                        if (cpConfig.isFastCharger) {
                            pageManager.changePage(PageID.SELECT_FAST, channel);
                        } else {
                            pageManager.changePage(PageID.SELECT_SLOW, channel);
                        }
                    }
                } else {
                    onPageStartEvent();
                }
                break;


            case GO_BACK:
                if (flowState == UIFlowState.UI_SELECT) {
                    setUIFlowState(UIFlowState.UI_MAIN);
                    pageManager.changePage(PageID.MAIN_COVER, channel);
                } else if (flowState == UIFlowState.UI_SELECT_PAYMENT_METHOD) {
                    ocppSessionManager.closeSession(chargeData.curConnectorId);
                    setUIFlowState(UIFlowState.UI_SELECT);
                    if (cpConfig.isFastCharger) {
                        pageManager.changePage(PageID.SELECT_FAST, channel);
                    } else {
                        pageManager.changePage(PageID.SELECT_SLOW, channel);
                    }
                } else if (flowState == UIFlowState.UI_CARD_TAG) {
                    if (cpConfig.useTl3500S) {
                        setUIFlowState(UIFlowState.UI_SELECT_PAYMENT_METHOD);
                        pageManager.changePage(PageID.SELECT_PAYMENT_METHOD, channel);
                    } else {
                        setUIFlowState(UIFlowState.UI_SELECT);
                        if (cpConfig.isFastCharger) {
                            pageManager.changePage(PageID.SELECT_FAST, channel);
                        } else {
                            pageManager.changePage(PageID.SELECT_SLOW, channel);
                        }

                    }

                } else if (flowState == UIFlowState.UI_SELECT_CHARGING_OPTION) {
                    ocppSessionManager.startSession(chargeData.curConnectorId);
                    if (chargeData.isNomember) {
                        setUIFlowState(UIFlowState.UI_SELECT_PAYMENT_METHOD);
                        pageManager.changePage(PageID.SELECT_PAYMENT_METHOD, channel);
                    } else {
                        setUIFlowState(UIFlowState.UI_CARD_TAG);
                        pageManager.changePage(PageID.CARD_TAG, channel);
                    }
                }
                // 회원카드태그에서 결재옵션선택 or 현장결제옵션에서 결재옵션선택
                else if (flowState == UIFlowState.UI_SET_CHARGING_OPTION) {
                    setUIFlowState(UIFlowState.UI_SELECT_CHARGING_OPTION);
                    pageManager.changePage(PageID.SELECT_CHATGING_OPTION, channel);
                }


                // 신용카드 결제 대기 화면에서 현장결재옵션
                else if (flowState == UIFlowState.UI_INSERT_CREDIT_CARD) {
                    initPaymentInfo();
                    if (cpConfig.useTl3500S) {
                        tl3500s.termReadyReq();
                    }
                    setUIFlowState(UIFlowState.UI_SET_CHARGING_OPTION);
                    pageManager.changePage(PageID.SET_CHARING_OPTION, channel);
                }
                // 충전기 연결대기 에서 카드태그 또는 현장결재옵션
                else if (flowState == UIFlowState.UI_CONNECTOR_WAIT) {
                    if (cpConfig.isAuthSkip) {
                        setUIFlowState(UIFlowState.UI_SELECT);
                        if (cpConfig.isFastCharger) {
                            pageManager.changePage(PageID.SELECT_FAST, channel);
                        } else {
                            pageManager.changePage(PageID.SELECT_SLOW, channel);
                        }
                    } else {
                        if (cpConfig.useTl3500S) {
                            onSetPaymentRequestData("2", chargeData.approvalRequestPrice, chargeData.installmentMonth, "4",
                                    chargeData.authCode, chargeData.paidDate, chargeData.paidTime, chargeData.pgTransactionSerialNo);
                            //취소 요청
                            String prepayDatetime = chargeData.paidDate + chargeData.paidTime;
                            tl3500s.cancelPay_Partial(channel, chargeData.approvalRequestPrice, 0,
                                    chargeData.approvalNumber, prepayDatetime, chargeData.pgTransactionSerialNo, 4);

                        }
                        ocppSessionManager.startSession(chargeData.curConnectorId);
                        setUIFlowState(UIFlowState.UI_SET_CHARGING_OPTION);
                        pageManager.changePage(PageID.SET_CHARING_OPTION, channel);
                    }

                } else if (flowState == UIFlowState.UI_CHARGING || flowState == UIFlowState.UI_FINISH_CHARGING) {
                    if (pageManager.getCurPageID() == PageID.SELECT_SLOW ||
                            pageManager.getCurPageID() == PageID.SELECT_FAST) {
                        pageManager.changePage(PageID.MAIN_COVER, channel);
                    } else {
                        if (cpConfig.isFastCharger) {
                            pageManager.changePage(PageID.SELECT_FAST, channel);
                        } else {
                            pageManager.changePage(PageID.SELECT_SLOW, channel);
                        }
                    }


                } else if (flowState == UIFlowState.UI_UNPLUG) {
                    setUIFlowState(UIFlowState.UI_FINISH_CHARGING);
                    pageManager.changePage(PageID.FINISH_CHARGING, channel);
                }

                break;
        }
    }

    public void onCardTagEvent(String tagNum, boolean isSuccess) {
        if (isSuccess) {
            if (flowState == UIFlowState.UI_CARD_TAG) {
                // 인증유효시간 체크 타이머 중지(카드태그화면)
                pageManager.getCardTagView().stopTimer();

                /**
                 * 예약중이라면 예약된 사용자가 아니면 인증 하지 않음
                 */
                if (reserveInfo.reservationId > 0) {
                    boolean reserveAuth = false;
                    if (tagNum.compareTo(reserveInfo.idTag) == 0) {
                        reserveAuth = true;
                    } else if (reserveInfo.parentIdTag != null) {
                        if (tagNum.compareTo(reserveInfo.parentIdTag) == 0) reserveAuth = true;
                    }

                    if (reserveAuth == false) {
                        chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_auth_fail_title);
                        chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_auth_fail_content_reserved);
                        pageManager.showMessageBox(channel);

                        goHomeProcessDelayed(chargeData.messageBoxTimeout * 1000);
                    } else {
                        reserveInfo.init();
                        mainActivity.setReservedVisible(View.INVISIBLE);
                        setUIFlowState(UIFlowState.UI_AUTH_WAIT);
                        onAuthResultEvent(true, 100);
                    }
                } else {
                    lastCardNum = tagNum;
                    // 승인대기 화면 전환
                    setUIFlowState(UIFlowState.UI_AUTH_WAIT);
                    pageManager.showAuthWaitView(channel);

                    // Tag Data 전송
                    ocppSessionManager.authorizeRequest(chargeData.curConnectorId, tagNum);

                }
            } else if (flowState == UIFlowState.UI_CHARGING) {
//                if(chargeData.serverStat && !lastCardNum.equals(tagNum)){
//                    ocppSessionManager.authorizeRequest(chargeData.curConnectorId, tagNum);
//                }
//                else{
//                    if ( tagNum.equals(lastCardNum) || chargeData.startransaction_parentID.equals(chargeData.pidNum) ) {
//                        if(chargeData.startransaction_parentID!=null && chargeData.pidNum!=null){
//                            if(chargeData.startransaction_parentID.equals(chargeData.pidNum)){
//                                OCPPSession session = ocppSessionManager.getOcppSesstion(chargeData.curConnectorId);
//                                session.setAuthTag(tagNum);
//                            }
//                        }
//                        LogWrapper.v(TAG, "Stop by User Card Tag");
//                        isStopByCard = true;
//                        isRemoteStarted = false;
//                        isStopByStartTransactionInvalid = false;
//                        isStopbySuspendedEVSE = false;
//                        onChargingStop();
////                        dispMeteringString(new String[] {"Stoping...", "Wait a second"});
//                    }
//                    else {
//                        chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_auth_fail_title);
//                        chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_auth_fail_content);
//                        pageManager.showMessageBox();
////                        dispTempMeteringString(new String[] {"Card Not Match"}, 4000);
//                    }
//                }
            }
        }
    }

    public void goHomeProcessDelayed(int delayMs) {
        final int timeout = delayMs;
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onPageStartEvent();
                    }
                }, timeout);
            }
        });
    }

    public void onAuthResultEvent(final boolean isSuccess, final int delayMs) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onAuthResultEvent(isSuccess);
                    }
                }, delayMs);
            }
        });
    }

    public void onAuthResultEvent(boolean isSuccess) {
        if (flowState == UIFlowState.UI_AUTH_WAIT) {
            setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.PREPARING);
            if (chargeData.isNomember) {
                pageManager.hideAuthWaitView();
                if (isSuccess) {
                    //인증요청된 카드의 parentTag정보 저장
                    OCPPSession session = ocppSessionManager.getOcppSesstion(chargeData.curConnectorId);
                    chargeData.pidNum = session.getParentTag();

                    setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
                    doAuthComplete();

                } else {
                    // 메시지 박스 내용 채움
                    chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_auth_fail_title);
                    chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.authfail);
                    pageManager.showAuthalarmBox(channel);

                    onSetPaymentRequestData("2", chargeData.approvalRequestPrice, chargeData.installmentMonth, "4",
                            chargeData.authCode, chargeData.paidDate, chargeData.paidTime, chargeData.pgTransactionSerialNo);
                    //취소 요청
                    String prepayDatetime = chargeData.paidDate + chargeData.paidTime;
                    tl3500s.cancelPay_Partial(channel, chargeData.approvalRequestPrice, 0,
                            chargeData.approvalNumber, prepayDatetime, chargeData.pgTransactionSerialNo, 4);
                }
            } else {
                if (isSuccess) {

                    //인증요청된 카드의 parentTag정보 저장
                    OCPPSession session = ocppSessionManager.getOcppSesstion(chargeData.curConnectorId);
                    chargeData.pidNum = session.getParentTag();

                    String data = makeUnitpriceJsonObject(chargeData.isNomember);
                    //단가정보 요청
                    ocppSessionManager.sendDataTransferReq(TypeDefine.DATA_TRANSFER_MESSAGEID_COST_UNIT, data);


                } else {
                    // 메시지 박스 내용 채움
                    chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_auth_fail_title);
                    chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.authfail);
                    pageManager.hideAuthWaitView();
                    setUIFlowState(UIFlowState.UI_AUTH_ALARM);
                    pageManager.showAuthalarmBox(channel);
                }
            }


        } else if (flowState == UIFlowState.UI_CHARGING) {
//            if (isSuccess) {
//                stopReason = StopTransaction.Reason.LOCAL;
//                onChargingStop();
//            } else {
//                    chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_auth_fail_title);
//                    chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_auth_fail_content);
//                    pageManager.showMessageBox(channel);
//            }

        }

    }

    public String makeUnitpriceJsonObject(boolean isNomember) {
        String retstr = "";
        try {
            JSONObject obj = new JSONObject();
            //요청관련
            obj.put("connectorId", chargeData.curConnectorId);
            if (isNomember) {
                obj.put("idTag", "");
            } else {
                obj.put("idTag", lastCardNum);
            }

            Calendar curTime = Calendar.getInstance();
            curTime.add(Calendar.HOUR_OF_DAY, timeOffset);
            obj.put("timestamp", formatter.format(curTime.getTime()));
            retstr = obj.toString();

        } catch (Exception e) {
            LogWrapper.e(TAG, "DataTransfer(tariff) Json Make Err:" + e.toString());
        }

        return retstr;
    }


    /**
     * 충전 시작시에 초기화가 필요한 변수를 세팅한다.
     */
    public void initChargingStartValue() {
        chargeData.measureWh = 0;
        chargeData.chargeStartTime = new Date();
        chargeData.chargeEndTime = new Date();
        chargeData.chargingTime = 0;
        chargeData.chargingCost = 0;
        meterTimerCnt = 0;

        stopReason = StopTransaction.Reason.LOCAL;

        initTime = System.currentTimeMillis();
        endTime = System.currentTimeMillis();
        backupMeterval = 0;
        nonChangeMeterStopFlag = false;


    }

    /**
     * 실제 충전 시작일때 이벤트(DSP에서 받은)
     * 충전 시작시 필요한 사항을 기술한다.
     */
    public void onDspChargingStartEvent() {
        if (flowState == UIFlowState.UI_RUN_CHECK) {
            // 충전 관련 변수를 초기화 한다.
            initChargingStartValue();
            pageManager.hideConwaitBox();
            setUIFlowState(UIFlowState.UI_CHARGING);
            pageManager.changePage(PageID.CHARGING, channel);


            if (cpConfig.isAuthSkip) {

            } else {
                // 통신으로 충전시작 메시지를 보낸다.
                ocppSessionManager.startCharging(chargeData.curConnectorId, (int) lastMeterValue);
            }


            // RFID (충전 중지)
//            rfidReader.rfidReadRequest();

            DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
            if (!cpConfig.isAuthSkip) {
                sendOcppMeterValues(rxData, SampledValue.Context.TRANSACTION_BEGIN, true);
            }


        }
    }

    public void onConnectedCableEvent(boolean isConnected) {
        if (isConnected) {
            // 급속에서 사용자가 충전시작을 하게끔 한다. 수정.. 커넥터 체크 자동으로 할 때는 아래코드를 이용함
            if (flowState == UIFlowState.UI_CONNECTOR_WAIT) {
                setUIFlowState(UIFlowState.UI_RUN_CHECK);
                // 이미 Run이 된 상태이라면`
                if (isDspChargeRun) {
                    onDspChargingStartEvent();
                } else {
                    pageManager.showConwaitBox(channel);
                }
            }
        } else {
            if (flowState == UIFlowState.UI_UNPLUG) {
                onPageStartEvent();
            }

        }
    }

    public void onFinishChargingEvent() {
        if (flowState == UIFlowState.UI_CHARGING) {
            chargeData.chargeEndTime = new Date();
            pageManager.hideStopAskBox();
            setUIFlowState(UIFlowState.UI_FINISH_CHARGING);
            pageManager.changePage(PageID.FINISH_CHARGING, channel);

            if (!cpConfig.isAuthSkip) {
                Transceiver trans = ocppSessionManager.getOcppStack().getTransceiver();
                //transaciton monitor stat true
                trans.setTransactionMonitorStat(false);
                //충전중 모니터링시 생성된 트랜젝션 패킷 db에서 제거
                trans.removeSaveTransactionMessage(trans.getLastUniqeId_Metervalue());
                trans.removeSaveTransactionMessage(trans.getLastUniqeId_Stoptransaction());


                // 커넥터 상태를 충전 종료중으로 바꾼다.(Status 메시지 보냄)
                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.FINISHING);

                DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
                sendOcppMeterValues(rxData, SampledValue.Context.TRANSACTION_END, true);


                //통신으로 종료 패킷을 보낸다.

                if (isHardResetEvent) stopReason = StopTransaction.Reason.HARD_RESET;
                else if (isSoftResetEvent) stopReason = StopTransaction.Reason.SOFT_RESET;
                else if (isStopbySuspendedEVSE) stopReason = StopTransaction.Reason.EV_DISCONNECTED;
                else if (isRemoteStarted) stopReason = StopTransaction.Reason.REMOTE;
                else if (isStopByCard) stopReason = StopTransaction.Reason.LOCAL;
                else if (isStopByStartTransactionInvalid)
                    stopReason = StopTransaction.Reason.DE_AUTHORIZED;
                else if (isStopByCompliteCharging) stopReason = StopTransaction.Reason.OTHER;


                ocppSessionManager.stopCharging(chargeData.curConnectorId, (int) lastMeterValue, stopReason);
            }


            //DSP 종료신호 해제
            dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, false);

            if (!cpConfig.isAuthSkip) {
                //비회원일 경우 충전량에 대한 부분취소, 무카드취소 진행
                if (chargeData.isNomember) {
                    if (chargeData.chargingCost == 0) {
                        //충전금액이 0일경우 무카드 취소 진행
                        onSetPaymentRequestData("2", chargeData.approvalRequestPrice, chargeData.installmentMonth, "4",
                                chargeData.authCode, chargeData.paidDate, chargeData.paidTime, chargeData.pgTransactionSerialNo);

                        //취소 요청
                        String prepayDatetime = chargeData.paidDate + chargeData.paidTime;
                        tl3500s.cancelPay_Partial(channel, chargeData.approvalRequestPrice, 0, chargeData.approvalNumber, prepayDatetime, chargeData.pgTransactionSerialNo, 4);
                    } else if (chargeData.chargingCost < chargeData.approvalRequestPrice) {
                        //충전금액이 최초결제 금액보다 적을경우 차액 부분취소 진행
                        int cancelCost = chargeData.approvalRequestPrice - (int) chargeData.chargingCost;

                        onSetPaymentRequestData("3", cancelCost, chargeData.installmentMonth, "5",
                                chargeData.authCode, chargeData.paidDate, chargeData.paidTime, chargeData.pgTransactionSerialNo);

                        //취소(부분) 요청
                        String prepayDatetime = chargeData.paidDate + chargeData.paidTime;
                        tl3500s.cancelPay_Partial(channel, chargeData.approvalRequestPrice, 0, chargeData.approvalNumber, prepayDatetime, chargeData.pgTransactionSerialNo, 5);
                    }
                }

                String data = makeFinalChargingJsonObject();
                ocppSessionManager.sendDataTransferReq(TypeDefine.DATA_TRANSFER_MESSAGEID_FINALCHARGING, data);
            }

        }
        mainActivity.setRemoteStartedVisible(View.INVISIBLE);
    }

    /**
     * 최초결제 요청 및 취소(부분)요청시 요청data 저장
     *
     * @param payclass            : 결제구분(1:승인, 2:취소, 3:부분취소)
     * @param approvreqCost       : 승인요청금액(최초결제 혹은 취소금액)
     * @param installmonth        : 할부개월
     * @param cancel_classificode : 취소구분코드(4:PG무카드취소, 5:PG부분취소) - 취소시에만 해당
     * @param cancel_approvnum    : 최초결제 승인번호 - 취소시에만 해당
     * @param cancel_date         : 최초결제 일시 - 취소시에만 해당
     * @param cancel_time         : 최초결제 시간 - 취소시에만 해당
     * @param pgnum               : 최초결제 거래일련번호 - 취소시에만 해당
     */
    public void onSetPaymentRequestData(String payclass, int approvreqCost, String installmonth, String cancel_classificode, String cancel_approvnum,
                                        String cancel_date, String cancel_time, String pgnum) {
        chargeData.paymentClassification = payclass;
        chargeData.approvalRequestPrice = approvreqCost;
        chargeData.installmentMonth = installmonth;
        chargeData.cancelClassificationCode = cancel_classificode;
        chargeData.approvalNumber = cancel_approvnum;
        chargeData.paymentDate = cancel_date;
        chargeData.paymentTime = cancel_time;
        chargeData.approvalCancelKey = pgnum;
    }

    public void onChargingStop() {
        //DSP에 STOP 신호를 보낸다.
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, true);
    }

    public void onStopAsk() {
        pageManager.showStopAskBox();
    }

    public void powerLimitProcess() {
        double newPowerLimit = ocppSessionManager.getSmartChargerManager().getCurPowerLimit(chargeData.curConnectorId);

        if (newPowerLimit > 0 && powerLimit != newPowerLimit) {
            powerLimit = newPowerLimit;
            //TODO 전력 제한 관련 DSP 제어 통신
        }
    }

    /**
     * 1초에 한번씩 실행되면서 필요한 일을 수핸한다.
     */
    public void timerProcessSec() {
        //Fault 함수 수행
        onFaultEventProcess();

        //PowerLimit 함수 수행
        powerLimitProcess();

        //변동단가 적용위한 시간체크
        checkTransitionDay();

        //Reserve Check
        if (reserveInfo.expiryCheck() == true) mainActivity.setReservedVisible(View.INVISIBLE);

        //전력량계 모니터링
        getMeterValueProcess();

        DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
        dspVersion = rxData.version; // DSP 버전 정보 저장

        // 충전중일때 충전 시간을 계산한다.
        if (getUIFlowState() == UIFlowState.UI_CHARGING) {

            chargeData.chargingTime = (new Date()).getTime() - chargeData.chargeStartTime.getTime();
            int chargingTimeSec = (int) (chargeData.chargingTime / 1000);

            if (cpConfig.isFastCharger) {
                chargeData.soc = rxData.batterySOC;
                chargeData.remainTime = rxData.remainTime;
            }


            if (!cpConfig.isAuthSkip) {
                // 충전중 미터값을 주기에 따라 보낸다.
                int meterInterval = ocppSessionManager.getOcppConfiguration().MeterValueSampleInterval;
//                int meterInterval = 10; //for debug
                meterTimerCnt++;
                if (meterTimerCnt >= meterInterval && meterInterval > 0) {
                    sendOcppMeterValues(rxData, SampledValue.Context.SAMPLE_PERIODIC, true);
                    sendChargingTimeRptDatatrnsferReq();
                    meterTimerCnt = 0;
                }
            }

            if (!cpConfig.isAuthSkip) {
                //충전중 커넥터 제거시 종료사유 추가
                if (isDspPlug == false) {
                    if (chargeData.ocppStatus != StatusNotification.Status.SUSPENDED_EVSE && finishWaitCnt == 0) {
                        stopReason = StopTransaction.Reason.EV_DISCONNECTED;
                    }
                }

//                if (chargeData.serverStat)   onStopchargingMonitoring(rxData);       //add by si. 220318
            }

//            // 회원 FULL 충전옵션 -> 충전시간 제한
//            if (chargeData.approvalRequestPrice < 0 && chargingTimeSec > TypeDefine.MAX_CHARGING_TIME) {
//                isStopByCompliteCharging = true;
//                onChargingStop();
//            }


            nonChangeMeterStopFlag = getMeterChargingStopFlag();
            if (nonChangeMeterStopFlag) {
                LogWrapper.v("StopReason", "Meter value not Change");
                onChargingStop();     //충전량 변화 없을시 충전 종료
            }

        }

        //충전중 상태이고 플러그가 false이면 & stopReason이 EV_DISCONNECT이면 statusnoti SUSPENDED_EVSE상태 전송
        if (getUIFlowState() == UIFlowState.UI_CHARGING && !cpConfig.isAuthSkip) {
            if (isDspPlug == false && stopReason == StopTransaction.Reason.EV_DISCONNECTED) {
                if (finishWaitCnt == TypeDefine.SUSPENDED_EVSE_CHECK_TIMEOUT) {
                    setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.SUSPENDED_EVSE);
                    isStopbySuspendedEVSE = true;
                    isStopByCard = false;
                    isStopByStartTransactionInvalid = false;
                    isStopByCompliteCharging = false;

                    finishWaitCnt++;
                } else finishWaitCnt++;
            }
        }

        // ClockedAlign MeterValue 를 보낸다.
        if (!cpConfig.isAuthSkip) processClockAlignMeterValue(rxData);


        // Event에서 poll로 바꿈.
        if (rxData.get400Reg(DSPRxData2.STATUS400.STATE_DOOR) == false) {
            //도어 명령이 성공적으로 수행이 되면 Door Open을 더 이상 수행하지 않는다.
            dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, false);
        }

        // connect 체크 polling
        // Event에서 poll로 바꿈.
        if (getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT && rxData.get400Reg(DSPRxData2.STATUS400.STATE_PLUG) == true) {
            onConnectedCableEvent(true);
        }

        // Finish화면에서 일정시간 이상 지났을때 Unplug가 되면 초기화면
        if (getUIFlowState() == UIFlowState.UI_FINISH_CHARGING || getUIFlowState() == UIFlowState.UI_UNPLUG) {
            // 5초이상 Gap을 준다.(MC 융착을 피하기 위함)
            if (unplugTimerCnt++ > 5) {
                if (!isDspPlug) {
                    // 커넥터 상태를 충전 종료중으로 바꾼다.(Status 메시지 보냄)
                    if (!cpConfig.isAuthSkip)
                        if (isConnectorOperative)
                            setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.FINISHING);

                    onPageStartEvent();
                } else {
                    if (unplugTimerCnt > TypeDefine.MAX_UNPLUG_TIME) onPageStartEvent();
                }

            }
        }
    }

    int lastHourValue = 0;

    private static final String DATE_FORMAT_WITH_MS = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    protected void checkTransitionDay() {
        Calendar curTime = Calendar.getInstance();
        if (curTime.get(Calendar.HOUR_OF_DAY) != lastHourValue) {
            lastHourValue = curTime.get(Calendar.HOUR_OF_DAY);

            if (flowState == UIFlowState.UI_CHARGING ||
                    flowState == UIFlowState.UI_SELECT_CHARGING_OPTION ||
                    flowState == UIFlowState.UI_SET_CHARGING_OPTION ||
                    flowState == UIFlowState.UI_INSERT_CREDIT_CARD ||
                    flowState == UIFlowState.UI_CONNECTOR_WAIT ||
                    flowState == UIFlowState.UI_RUN_CHECK) {
                try {
                    if (authUnitPrice.length() == unitPriceIndex) return;
                    JSONObject obj = new JSONObject(authUnitPrice.getString(++unitPriceIndex));
                    String unitCost = obj.getString("price");
                    String startTime = obj.getString("startAt");
                    String endTime = obj.getString("endAt");

                    chargeData.chargingUnitCost = Double.parseDouble(unitCost);

                    LogWrapper.v(TAG, "CostInfo Received: " + chargeData.chargingUnitCost +
                            " / start:" + startTime + " ~ end:" + endTime);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void sendChargingTimeRptDatatrnsferReq() {
        String data = makeChargingTimeRptJsonObject();
        ocppSessionManager.sendDataTransferReq(TypeDefine.DATA_TRANSFER_MESSAGEID_CHARGINGTIME, data);
    }


    /**
     * chargingTimeRpt
     * DataTransfer
     *
     * @return
     */
    public String makeChargingTimeRptJsonObject() {
        String retstr = "";
        try {
            JSONObject obj = new JSONObject();
            //요청관련
            obj.put("SoC", chargeData.soc);
            obj.put("chargingCapacity", chargeData.measureWh);
            int elapsedTime = (int) (chargeData.chargingTime / 1000);
            obj.put("elapsedTime", elapsedTime);
            obj.put("remainingTime", chargeData.remainTime);
            obj.put("transactionId", chargeData.curTransactionID);
            obj.put("connectorId", chargeData.curConnectorId);

            retstr = obj.toString();

        } catch (Exception e) {
            LogWrapper.e(TAG, "DataTransfer(chargingTimeRpt) Json Make Err:" + e.toString());
        }

        return retstr;
    }


    void getMeterValueProcess() {
        //전력량계 값 가져오기
        // meterVal = 전력량계
        //meter volt
        if (mainActivity.getMeterService() != null) {
            try {
                long meterVal = mainActivity.getMeterService().readMeterCh(channel); //w 단위
                double meterVolt = mainActivity.getMeterService().readMeterVoltageCh(channel);
                double meterCurrent = mainActivity.getMeterService().readMeterCurrentCh(channel);
                int meterseq = mainActivity.getMeterService().readSeqNumber();

                //add by si - 21.12.09 - MeterReadError상태 감지 추가
                MeterStatusMonitoring(meterVal);

                //add by si - 211130 - meter view program seqnum 증가상태 감지(없을시 UI재부팅)
                MeterviewSeqnumMonitor(meterseq);

//                Log.d(TAG, "Meter:" + meterVal + ", Volt:" + meterVolt + ", current:" + meterCurrent + ", m_seqnum:" + meterseq);

                float fMeterVolt = (float) meterVolt;
                float fMeterCurrent = (float) meterCurrent;
                float fMeterVal = (float) meterVal;

                //DSP Write (전력량 관련)
                if (cpConfig.isFastCharger) {
                    dspControl.setOutputVoltageDC(chargeData.dspChannel, fMeterVolt);
                    dspControl.setOutputAmpareDC(chargeData.dspChannel, fMeterCurrent);
                    //load test용
                    chargeData.outputVoltage = Math.round(fMeterVolt * 100) / 100.0;
                    chargeData.outputCurr = Math.round(fMeterCurrent * 100) / 1000.0;
                } else {
                    dspControl.setOutputVoltageAC(chargeData.dspChannel, fMeterVolt);
                    dspControl.setOutputAmpareAC(chargeData.dspChannel, fMeterCurrent);
                    //load test용
                    chargeData.outputVoltage = Math.round(fMeterVolt * 100) / 100.0;
                    chargeData.outputCurr = Math.round(fMeterCurrent * 100) / 100.0;
                }


//                LogWrapper.d(TAG, "Meter:"+chargeData.meterVal+", Volt:"+chargeData.outputVoltage+", current:"+chargeData.outputCurr);
//                LogWrapper.d(TAG, "Meter:"+meterVal+", Volt:"+meterVolt+", current:"+meterCurrent);
                if (meterVal >= 0) {
                    if (cpConfig.isFastCharger)
                        dspControl.setMeterDC(chargeData.dspChannel, fMeterVal);
                    else dspControl.setMeterAC(chargeData.dspChannel, fMeterVal);
                    chargeData.meterVal = meterVal;
                    if (getUIFlowState() == UIFlowState.UI_CHARGING) {
                        if (lastMeterValue > 0) {
                            int gapMeter = (int) (meterVal - lastMeterValue);
                            if (gapMeter > 0) {
                                //gapMeter 최대 증가폭 1초당 0.5kw를 넘지못함. - add by si. 200831
                                if (gapMeter > 500) {
                                    gapMeter = 500;
                                }
                                chargeData.measureWh += gapMeter;
                                // 계량기 값의 차이를 계속 더한다.
                                // 시간별 과금을 위해서.(사용량 x 시간별 단가로 계산)  - 변동단가
                                chargeData.chargingCost += ((double) gapMeter / 1000.0) * (double) chargeData.chargingUnitCost;

                                //사용자 선택 충전옵션에 따른 종료상태 모니터링(비회원)
                                if (chargeData.approvalRequestPrice > 0) {  //금액
                                    if (chargeData.approvalRequestPrice <= chargeData.chargingCost) {
                                        chargeData.chargingCost = chargeData.approvalRequestPrice;
                                        isStopByCompliteCharging = true;
                                        LogWrapper.v("StopReason", "Charging Complete");
                                        onChargingStop();
                                    }
                                }
                                //서버로 값 전송
//                                minsuChargerInfo.curChargingKwh = (int)chargeData.measureWh;
//                                minsuChargerInfo.curChargingCost = (int)chargeData.chargingCost;
//                                minsuChargerInfo.curChargingCostUnit = chargeData.chargingUnitCost;
                            }
                        }
                    }
                    lastMeterValue = meterVal;
                } else {
                    // Meter Error !!!
                    if (lastMeterValue < 0) lastMeterValue = 0;
                    dspControl.setMeterAC(chargeData.dspChannel, lastMeterValue);
                }
            } catch (Exception e) {
                LogWrapper.e(TAG, "Meter Err:" + e.toString());
            }
        }

    }

    /***
     * 계량기 통신 오류시 200-7 bit로 계량기 통신 오류 폴트 알림
     */
    //add by si.201209 - 전력량계 오류상태 모니터링함수
    boolean isMeterCommErr = false;
    boolean isMeterCommErr_backup = false;
    FaultInfo meterfaulinfo = null;

    public void MeterStatusMonitoring(long m_meterVal) {
        try {
            if (m_meterVal == -1)
                isMeterCommErr = true;
            else isMeterCommErr = false;

            if (isMeterCommErr != isMeterCommErr_backup) {
                if (isMeterCommErr) {
                    chargeData.ismeterCommError = true;

                    meterfaulinfo = new FaultInfo(42313, 42313, "계량기 통신 에러", false);
                    faultList.add(meterfaulinfo);
                    pageManager.showFaultBox(channel);

                    setOcppError(chargeData.curConnectorId, "계량기 통신 에러");

                    //충전중 발생했을 경우 충전 중지.
                    if (getUIFlowState() == UIFlowState.UI_CHARGING) {
                        LogWrapper.v("StopReason", "Meter Comm Err");
                        onChargingStop();
                    } else {
                        if (getUIFlowState() != UIFlowState.UI_FINISH_CHARGING &&
                                ((getUIFlowState() != UIFlowState.UI_MAIN) || (getUIFlowState() != UIFlowState.UI_CARD_TAG))) {
                            onPageStartEvent();
                        }
                    }
                    //Meter Read error일 경우
                    //dsp로 에러신호 전송
                    dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.UI_FAULT, true);
                    LogWrapper.v(TAG, "MeterError occured!");


                } else if (!isMeterCommErr) {
                    chargeData.ismeterCommError = false;

                    if (meterfaulinfo != null) {
                        faultList.remove(meterfaulinfo);
                        meterfaulinfo = null;
                    }

                    setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);
                    //미터기 상태 정상일 경우
                    //dsp 미터에러신호 복구 및 기타변수 초기화
                    dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.UI_FAULT, false);
                    LogWrapper.v(TAG, "MeterError Restored!");
                    pageManager.hideFaultBox();
                }
                isMeterCommErr_backup = isMeterCommErr;
            }
        } catch (Exception e) {

        }
    }

    /**
     * 계량기 프로그램 동작여부 판단
     * seqnum 변화없을 경우 충전중이 아닐때 UI 재부팅
     *
     * @param seqnum 계량기프로그램에서 0~255까지 증가되는 값을 모니터링
     */
    int meter_seqnum_backup = 0;
    int meter_seqnum = 0;
    int meter_comm_errcnt = 0;

    public void MeterviewSeqnumMonitor(int seqnum) {
        //1초마다 실행됨
        meter_seqnum = seqnum;
        if ((meter_seqnum == meter_seqnum_backup) || (meter_seqnum == 0)) {
            meter_comm_errcnt++;
        } else {
            meter_seqnum_backup = meter_seqnum;
            meter_comm_errcnt = 0;
        }

        //err count 감지
        if (meter_comm_errcnt >= TypeDefine.COMM_METER_TIMEOUT) {
            //충전중이 아닐경우에 리셋 진행
            if (getUIFlowState() != UIFlowState.UI_CHARGING) {
                if (getUIFlowState() == UIFlowState.UI_MAIN ||
                        getUIFlowState() == UIFlowState.UI_CARD_TAG) {
                    //UI 리셋
                    meter_comm_errcnt = 0;
                    onResetRequest(true);
                }
            }
        }

    }

    /**
     * 1초마다 호출, Meter Interval 마다 Sample List들을 보냄
     */
    protected void processClockAlignMeterValue(DSPRxData2 rxData) {
        int clockAlignedDataInterval = ocppSessionManager.getOcppConfiguration().ClockAlignedDataInterval;
        if (clockAlignedDataInterval == 0) return;

        // 00:00:00 기준으로 Interval 계산함
        // 현재시간 초로 나타냄
        Calendar curTime = Calendar.getInstance();
        int secTime = curTime.get(Calendar.HOUR_OF_DAY) * 3600 + curTime.get(Calendar.MINUTE) * 60 + curTime.get(Calendar.SECOND);

        if ((secTime % clockAlignedDataInterval) == 0) {
            int gapMeter = 0;
            if (lastClockedMeterValue > 0) {
                gapMeter = (int) (lastMeterValue - lastClockedMeterValue);
            }
            lastClockedMeterValue = lastMeterValue;

            // TODO 전력제한 과 연동 필요!!
            int currentOffered = -1;
            int powerOffered = -1;
            int soc = -1;

            if (getUIFlowState() == UIFlowState.UI_CHARGING) {
                if (chargeData.connectorType == TypeDefine.ConnectorType.AC3) {
                    currentOffered = TypeDefine.CP_AC_CURRENT_OFFERED;
                    powerOffered = TypeDefine.CP_AC_POWER_OFFERED;
                } else {
                    currentOffered = TypeDefine.CP_DC_CURRENT_OFFERED;
                    powerOffered = TypeDefine.CP_DC_POWER_OFFERED;
                    soc = rxData.batterySOC;
                }
            }

            ocppSessionManager.sendMeterValueRequest(chargeData.curConnectorId, lastMeterValue, gapMeter, (int) chargeData.outputCurr, (int) (chargeData.outputCurr * chargeData.outputVoltage),
                    currentOffered, powerOffered, soc, SampledValue.Context.SAMPLE_CLOCK,
                    ocppSessionManager.getOcppConfiguration().MeterValuesAlignedData, getUIFlowState() == UIFlowState.UI_CHARGING);
        }
    }

    public long backupMeterval;
    long distanceTime;
    long initTime;
    long endTime;

    //add by si.201026 - 충전중 충전량 변화에 따른 충전종료 플래그 리턴 함수
    public boolean getMeterChargingStopFlag() {
        boolean retval = false;
        try {
            if (chargeData.measureWh != backupMeterval) {
                backupMeterval = chargeData.measureWh;
                initTime = System.currentTimeMillis();
                endTime = System.currentTimeMillis();
                distanceTime = (long) ((endTime - initTime) / 1000.0);       //초
            } else if (chargeData.measureWh == backupMeterval) {
                endTime = System.currentTimeMillis();
                distanceTime = (long) ((endTime - initTime) / 1000.0);       //초

                if (distanceTime >= TypeDefine.METERING_CHANGE_TIMEOUT) {
                    retval = true;
                } else retval = false;
            }
        } catch (Exception e) {

        }

        return retval;
    }

    /**
     * 충전중 전원이 강제로 꺼졌을 경우에 대비해 Stoptransaction 및 Metervalue END 패킷을  db에 저장
     *
     * @param rxData
     */

    public void onStopchargingMonitoring(DSPRxData2 rxData) {
        Transceiver trans = ocppSessionManager.getOcppStack().getTransceiver();

        //MeterValue
        OCPPMessage metervalueMsg = makeMetervalueFinishMsg();

        //StopTransaction
        OCPPMessage stoptransMsg = makeStopTransactionMsg();

        if (trans.getTransactionMonitorStat() == false) {
            stopReason = StopTransaction.Reason.POWER_LOSS;

            trans.setTransactionMonitorStat(true);
            trans.setLastUniqeId_Metervalue(metervalueMsg.getId());
            trans.setLastUniqeId_StopTransaction(stoptransMsg.getId());

            trans.saveTransactionMessage(metervalueMsg);
            trans.saveTransactionMessage(stoptransMsg);
        } else {
            //먼저 저장된 트랜젝션 패킷 지우고
            trans.removeSaveTransactionMessage(trans.getLastUniqeId_Metervalue());
            trans.removeSaveTransactionMessage(trans.getLastUniqeId_Stoptransaction());

            //새로운 트랜잭션 패킷 저장
            trans.setLastUniqeId_Metervalue(metervalueMsg.getId());
            trans.saveTransactionMessage(metervalueMsg);

            trans.setLastUniqeId_StopTransaction(stoptransMsg.getId());
            trans.saveTransactionMessage(stoptransMsg);
        }
    }

    public OCPPMessage makeMetervalueFinishMsg() {
        List<MeterValue> listValue = new ArrayList<MeterValue>();

        MeterValue meterValue = new MeterValue();
        meterValue.setTimestamp(Calendar.getInstance());

        List<SampledValue> listSample = new ArrayList<SampledValue>();
        SampledValue sampledValue = new SampledValue();
        sampledValue.setValue("" + lastMeterValue);
        sampledValue.setMeasurand(SampledValue.Measurand.ENERGY_ACTIVE_IMPORT_REGISTER);
        sampledValue.setUnit(SampledValue.Unit.W);
        sampledValue.setContext(SampledValue.Context.TRANSACTION_END);
        listSample.add(sampledValue);

//        if ( meterValInterval >= 0 && measurand.contains("Energy.Active.Import.Interval") ) {
//            sampledValue = new SampledValue();
//            sampledValue.setValue("" + meterValInterval);
//            sampledValue.setMeasurand(SampledValue.Measurand.ENERGY_ACTIVE_IMPORT_INTERVAL);
//            sampledValue.setUnit(SampledValue.Unit.W);
//            setContextSampleValue(sampledValue, context);
//            listSample.add(sampledValue);
//        }

//        if ( soc >= 0 && measurand.contains("SoC")) {
//            sampledValue = new SampledValue();
//            sampledValue.setValue("" + soc);
//            sampledValue.setMeasurand(SampledValue.Measurand.SO_C);
//            sampledValue.setUnit(SampledValue.Unit.PERCENT);
//            setContextSampleValue(sampledValue, context);
//            listSample.add(sampledValue);
//        }

        //(int)chargeData.outputCurr, (int)(chargeData.outputCurr*chargeData.outputVoltage

        String measurand = ocppSessionManager.getOcppConfiguration().MeterValuesSampledData;
        int current = (int) chargeData.outputCurr;

        if (current >= 0 && measurand.contains("Current.Import")) {
            sampledValue = new SampledValue();
            sampledValue.setValue("" + current);
            sampledValue.setMeasurand(SampledValue.Measurand.CURRENT_IMPORT);
            sampledValue.setUnit(SampledValue.Unit.A);
            sampledValue.setContext(SampledValue.Context.TRANSACTION_END);
            listSample.add(sampledValue);
        }

        int curPower = (int) (chargeData.outputCurr * chargeData.outputVoltage);
        if (curPower >= 0 && measurand.contains("Power.Active.Export")) {
            sampledValue = new SampledValue();
            sampledValue.setValue("" + curPower);
            sampledValue.setMeasurand(SampledValue.Measurand.POWER_ACTIVE_EXPORT);
            sampledValue.setUnit(SampledValue.Unit.W);
            sampledValue.setContext(SampledValue.Context.TRANSACTION_END);
            listSample.add(sampledValue);
        }

        int currentOffered = TypeDefine.CP_AC_CURRENT_OFFERED;
        int powerOffered = TypeDefine.CP_AC_POWER_OFFERED;

        if (currentOffered >= 0 && measurand.contains("Current.Offered")) {
            sampledValue = new SampledValue();
            sampledValue.setValue("" + currentOffered);
            sampledValue.setMeasurand(SampledValue.Measurand.CURRENT_OFFERED);
            sampledValue.setUnit(SampledValue.Unit.A);
            sampledValue.setContext(SampledValue.Context.TRANSACTION_END);
            listSample.add(sampledValue);
        }

        if (powerOffered >= 0 && measurand.contains("Power.Offered")) {
            sampledValue = new SampledValue();
            sampledValue.setValue("" + powerOffered);
            sampledValue.setMeasurand(SampledValue.Measurand.POWER_OFFERED);
            sampledValue.setUnit(SampledValue.Unit.W);
            sampledValue.setContext(SampledValue.Context.TRANSACTION_END);
            listSample.add(sampledValue);
        }

        meterValue.setSampledValue(listSample);
        listValue.add(meterValue);

        MeterValues meterValues = new MeterValues();
        meterValues.setConnectorId(chargeData.curConnectorId);
        meterValues.setMeterValue(listValue);

        OCPPSession session = ocppSessionManager.getOcppSesstion(chargeData.curConnectorId);
        boolean isInTransaction = true;
        if (isInTransaction) meterValues.setTransactionId(session.getCurTransactionId());

        OCPPMessage message = new OCPPMessage("MeterValues", meterValues);

        // 추후에 메시지 처리(재전송, ACK 처리등)을 위해 Connectorid를 저장한다.
        message.transactionConnectorId = chargeData.curConnectorId;

        Calendar startTime = session.getChargingStartTime();

        if (startTime != null) message.setTransactionStartTime(startTime);

        return message;
    }

    public OCPPMessage makeStopTransactionMsg() {
        StopTransaction stopTransaction = new StopTransaction();
        stopTransaction.setIdTag(lastCardNum);
        stopTransaction.setMeterStop((int) lastMeterValue);
        stopTransaction.setTimestamp(Calendar.getInstance());
        stopTransaction.setReason(stopReason);
        OCPPSession session = ocppSessionManager.getOcppSesstion(chargeData.curConnectorId);
        stopTransaction.setTransactionId(session.getCurTransactionId());

        OCPPMessage message = new OCPPMessage("StopTransaction", stopTransaction);

        // 추후에 메시지 처리(재전송, ACK 처리등)을 위해 Connectorid를 저장한다.
        message.transactionConnectorId = chargeData.curConnectorId;
        message.setTransactionStartTime(session.getChargingStartTime());

        return message;
    }

    public void doAuthComplete() {
        DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
        if (rxData.get400Reg(DSPRxData2.STATUS400.STATE_PLUG) == true) {
            // 이미 Connect 된 상태이라면
            onConnectedCableEvent(true);
        } else {
            // changePage가 하나의 함수에서 2번이상 불려지면. UI Thread로 인한 문제가 발생할 소지가 있음
            pageManager.changePage(PageID.CONNECTOR_WAIT, channel);
            // Ctype인 경우에는 도어를 오픈할 필요가 없음
            if (chargeData.connectorType != TypeDefine.ConnectorType.CTYPE) {
                dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);
            }
        }
    }

    /**
     * DSP에서 오는 이벤트를 처리한다.
     *
     * @param channel 해당 채널값
     * @param idx     상태값 Index
     * @param val
     */
    public void onDspStatusChange(int channel, DSPRxData2.STATUS400 idx, boolean val) {
        if (channel == chargeData.dspChannel) {
            LogWrapper.v(TAG, "DSP Status Change:" + idx.name() + " is " + val);
            switch (idx) {
                case READY:
                    isDspReady = val;
                    break;

                case AVAL_CHARGE:
                    isDspAvalCharge = val;
                    break;

                case STATE_PLUG:
                    isDspPlug = val;
                    if (isDspPlug && isConnectorOperative && getUIFlowState() == UIFlowState.UI_CARD_TAG)
                        setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.PREPARING);      //add by si. 220316
                    else {
                        if (getUIFlowState() == UIFlowState.UI_CARD_TAG)
                            setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);      //add by si. 220316
                    }
                    onConnectedCableEvent(val);
                    break;

                case STATE_DOOR:
                    isDspDoor = val;
                    if (isDspDoor == false) { // 도어 오픈
                        //도어 명령이 성공적으로 수행이 되면 Door Open을 더 이상 수행하지 않는다.
                        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, false);
                    }
                    break;

                case CHARGE_RUN:
                    isDspChargeRun = val;
                    if (val == true) {
                        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.START_CHARGE, false);
                        onDspChargingStartEvent();
                    }
                    break;

                case FINISH_CHARGE:
                    isDspChargeFinish = val;
                    if (val == true) onFinishChargingEvent();
                    break;

                case CG_STOP_BT:
                    isStopByCard = true;
                    break;

                case FAULT:
                    isDspFault = val;
                    onFaultEventProcess();
                    break;

                case STATE_RESET:
                    break;

                case CONNECTOR_LOCK_A:
                    break;
            }
        }
    }

    public void fillFaultMessage() {
        // 메시지 박스 내용 채움
        chargeData.faultBoxContent = "";
        for (FaultInfo fInfo : faultList) {
            if (fInfo.isRepair == false) {
                chargeData.faultBoxContent += "[" + fInfo.errorCode + "] " + fInfo.errorMsg;
                chargeData.faultBoxContent += "\r\n";
            }
        }
    }

    public synchronized void onFaultEventProcess() {
        Vector<FaultInfo> fList = faultManager.scanFaultV2(chargeData.dspChannel);
        boolean isEmergency = faultManager.isFaultEmergency(chargeData.dspChannel);

        if (fList.size() > 0) {
            Vector<FaultInfo> removed = new Vector<FaultInfo>();
            for (FaultInfo fInfo : fList) {
                for (FaultInfo fInfoCur : faultList) {
                    if (fInfoCur.id == fInfo.id) {
                        fInfoCur.isRepair = fInfo.isRepair;
                        if (fInfoCur.isRepair) {
                            removed.add(fInfoCur);
                        }
                    }
                }
                for (FaultInfo removeCur : removed) {
                    faultList.remove(removeCur);
                }

                if (!fInfo.isRepair) {
                    FaultInfo newInfo = new FaultInfo(fInfo.id, fInfo.errorCode, fInfo.errorMsg, fInfo.isRepair);
                    if (isDspFault) faultList.add(newInfo);
                }
                    fillFaultMessage();
                pageManager.refreshFaultBox();

            }

            if (isDspFault == true) {
                setOcppError(chargeData.curConnectorId, "");

                for (FaultInfo Info : faultList) {
                    String chann = "[connectorId:" + chargeData.curConnectorId + "] ";
                    String faultlog = "[" + Info.errorCode + "] " + Info.errorMsg;
                    LogWrapper.e(TAG, "[DSP Fault] " + chann + faultlog);     // 폴트로그 추가
                }
            }
        }


        if (isPreDspFault != isDspFault) {
            if (isDspFault == true) {
                // 충전충이라면 충전을 중지한다.
                if (getUIFlowState() == UIFlowState.UI_CHARGING) {
                    if (isEmergency == true) stopReason = StopTransaction.Reason.EMERGENCY_STOP;
                    else stopReason = StopTransaction.Reason.OTHER;
                    LogWrapper.v("StopReason", "DSP Fault Err");
                    onChargingStop();
                } else if (flowState == UIFlowState.UI_CONNECTOR_WAIT || flowState == UIFlowState.UI_RUN_CHECK) {
                    if (chargeData.isNomember) {
                        //충전금액이 0일경우 무카드 취소 진행
                        onSetPaymentRequestData("2", chargeData.approvalRequestPrice, chargeData.installmentMonth, "4",
                                chargeData.authCode, chargeData.paidDate, chargeData.paidTime, chargeData.pgTransactionSerialNo);

                        //취소 요청
                        String prepayDatetime = chargeData.paidDate + chargeData.paidTime;
                        tl3500s.cancelPay_Partial(channel, chargeData.approvalRequestPrice, 0,
                                chargeData.approvalNumber, prepayDatetime, chargeData.pgTransactionSerialNo, 4);
                    }
                    onPageStartEvent();
                } else {
                    if (getUIFlowState() != UIFlowState.UI_FINISH_CHARGING && getUIFlowState() != UIFlowState.UI_MAIN) {
                        onPageStartEvent();
                    }
                }
                pageManager.showFaultBox(channel);
            } else {
                pageManager.hideFaultBox();
                fList.clear();
                faultManager.setpreFaultinit();
            }
            isPreDspFault = isDspFault;
        }

        if (!chargeData.isdspCommError && !chargeData.ismeterCommError && !istl3500sCommError) {
            if (isDspFault == true) {
                if (chargeData.ocppStatus != StatusNotification.Status.FAULTED) {
                    setOcppError(chargeData.curConnectorId, "");
                }
            } else if (chargeData.ocppStatus == StatusNotification.Status.FAULTED) {
                if (isConnectorOperative) {
                    setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);
                }
            }
        }

        // 긴급버턴 이벤트 발생
        if (isEmergencyPressed != isEmergency) {
            if (isEmergency == true) {
                pageManager.showEmergencyBox();
            } else { // 긴급 버턴 해제
                pageManager.hideEmergencyBox();
            }
            isEmergencyPressed = isEmergency;
        }
    }


    // 시퀀스 진행 중 다른채널에서 에러 발생시 이벤트처리
    public void onOtherChannelFaultEvent() {
        if (flowState == UIFlowState.UI_CONNECTOR_WAIT || flowState == UIFlowState.UI_RUN_CHECK) {
            if (chargeData.isNomember) {
                //충전금액이 0일경우 무카드 취소 진행
                onSetPaymentRequestData("2", chargeData.approvalRequestPrice, chargeData.installmentMonth, "4",
                        chargeData.authCode, chargeData.paidDate, chargeData.paidTime, chargeData.pgTransactionSerialNo);

                //취소 요청
                String prepayDatetime = chargeData.paidDate + chargeData.paidTime;
                tl3500s.cancelPay_Partial(channel, chargeData.approvalRequestPrice, 0,
                        chargeData.approvalNumber, prepayDatetime, chargeData.pgTransactionSerialNo, 4);
            }
            onPageStartEvent();
        } else if (flowState == UIFlowState.UI_CHARGING || flowState == UIFlowState.UI_FINISH_CHARGING ||
                getUIFlowState() == UIFlowState.UI_UNPLUG) {
        } else {
            if (flowState == UIFlowState.UI_AUTH_WAIT) pageManager.hideAuthWaitView();
            else if (flowState == UIFlowState.UI_RUN_CHECK) pageManager.hideConwaitBox();
            onPageStartEvent();
        }
    }

    @Override
    public void onDspMeterChange(int channel, long meterVal) {
        // 계량기 값의 차이를 계속 더한다.
        // 추후 시간별 과금을 위해서.(사용량 x 시간별 단가로 계산)
        if (getUIFlowState() == UIFlowState.UI_CHARGING) {
            if (lastMeterValue < 0) lastMeterValue = meterVal;
            int gapMeter = (int) (meterVal - lastMeterValue);
            if (gapMeter < 0) gapMeter = 0;
            chargeData.measureWh += gapMeter;
            chargeData.chargingCost += ((double) gapMeter / 1000.0) * (double) chargeData.chargingUnitCost;
        }
        lastMeterValue = meterVal;
        //LogWrapper.v(TAG, "MeterVal : "+meterVal+", measure:"+chargeData.measureWh );
    }

    FaultInfo dspfaultInfo;

    @Override
    public void onDspCommErrorStatus(boolean isError) {
        chargeData.isdspCommError = isError;
        if (isError == true) {
            if (getUIFlowState() == UIFlowState.UI_CHARGING) {
                LogWrapper.v("StopReason", "DSP Comm Err");
                onChargingStop();
            } else if (getUIFlowState() != UIFlowState.UI_FINISH_CHARGING &&
                    (getUIFlowState() != UIFlowState.UI_CARD_TAG || getUIFlowState() != UIFlowState.UI_MAIN)) {
                onPageStartEvent();
            }


            dspfaultInfo = new FaultInfo(99999, 99999, mainActivity.getResources().getString(R.string.string_dsp_comm_err), false);
            faultList.add(dspfaultInfo);

            pageManager.showFaultBox(channel);
            //TODO 에러코드 수정
            setOcppError(chargeData.curConnectorId, "DSP통신에러");
            LogWrapper.e(TAG, "DSP-UI Comm Error!!");

        } else {
            faultList.remove(dspfaultInfo);
            dspfaultInfo = null;
            pageManager.hideFaultBox();
            setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);
            LogWrapper.e(TAG, "DSP-UI Comm Recovery.");
        }
    }

    //================================================
    // OCPP Event 송/수신, OCPP 처리
    //================================================


    public void setOcppStatus(int connectorId, StatusNotification.Status status) {
        if (isConnectorOperative) {
            chargeData.ocppStatus = status;
            chargeData.ocppStatusError = StatusNotification.ErrorCode.NO_ERROR;
            multiChannelUIManager.sendStatusNotificationStatusOfSystem(0);
        }
    }


    public void setOcppError(int connectorId, String vendorError) {
        if (isConnectorOperative) {
            chargeData.ocppVendorError = vendorError;
            chargeData.ocppStatus = StatusNotification.Status.FAULTED;
            chargeData.ocppStatusError = StatusNotification.ErrorCode.OTHER_ERROR;
            multiChannelUIManager.sendStatusNotificationStatusOfSystem(0);
        }

    }

    public void sendStatusNotificationStatusOfSystem() {
        if (chargeData.ocppStatus == StatusNotification.Status.FAULTED) {
            if (isDspFault || istl3500sCommError || chargeData.isdspCommError || isMeterCommErr) {
                for (FaultInfo fInfo : faultList) {
                    if (fInfo.isRepair == false) {
                        ocppSessionManager.sendStatusNotificationRequest(chargeData.curConnectorId,
                                chargeData.ocppStatus, chargeData.ocppStatusError, Integer.toString(fInfo.errorCode));
                    }
                }
            }
//            if(istl3500sCommError || chargeData.isdspCommError || isMeterCommErr){
//                ocppSessionManager.SendStatusNotificationRequest(chargeData.curConnectorId,
//                        chargeData.ocppStatus, chargeData.ocppStatusError, chargeData.ocppVendorError);
//            }

        } else {
            ocppSessionManager.sendStatusNotificationRequest(chargeData.curConnectorId,
                    chargeData.ocppStatus, chargeData.ocppStatusError, "");
        }
    }

    // 미터값, SOC 등 값을 전달한다.
    public void sendOcppMeterValues(DSPRxData2 rxData, SampledValue.Context context, boolean isInTransaction) {
        int currentOffered = TypeDefine.CP_AC_CURRENT_OFFERED;
        int powerOffered = TypeDefine.CP_AC_POWER_OFFERED;
        int soc = -1;

        ocppSessionManager.sendMeterValueRequest(chargeData.curConnectorId, chargeData.measureWh, -1,
                (int) chargeData.outputCurr, (int) (chargeData.outputCurr * chargeData.outputVoltage),
                currentOffered, powerOffered, soc,
                context, ocppSessionManager.getOcppConfiguration().MeterValuesSampledData, isInTransaction);
    }

    @Override
    public void onAuthSuccess(int connectorId) {
        if (flowState == UIFlowState.UI_AUTH_WAIT || flowState == UIFlowState.UI_CHARGING) {
            if (connectorId == chargeData.curConnectorId) {
                onAuthResultEvent(true, 1000);
            }
        }
    }

    @Override
    public void onAuthFailed(int connectorId) {
        if (flowState == UIFlowState.UI_AUTH_WAIT || flowState == UIFlowState.UI_CHARGING) {
            if (connectorId == chargeData.curConnectorId) {
                onAuthResultEvent(false, 1000);
            }
        }
    }

    @Override
    public void onChangeState(int connectorId, OCPPSession.SessionState state) {
        final int cid = connectorId - 1;
        final OCPPSession.SessionState chgState = state;
        /*
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvStatus[cid].setText(chgState.name());
            }
        });
        */
    }

    @Override
    public CancelReservationResponse.Status onCancelReservation(int reservationId) {
        CancelReservationResponse.Status ret = CancelReservationResponse.Status.REJECTED;
        if (reserveInfo.reservationId == reservationId) {
            reserveInfo.init();
            mainActivity.setReservedVisible(View.INVISIBLE);
            ret = CancelReservationResponse.Status.ACCEPTED;
            setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);

        }
        return ret;
    }

    @Override
    public void onBootNotificationResponse(boolean success) {

    }

    /**
     * 원격에서 충전을 중지시키는 이벤트가 발생했을때 처리
     *
     * @param connectorId
     */
    @Override
    public void onRemoteStopTransaction(int connectorId) {
        if (chargeData.curConnectorId == connectorId) {
            if (flowState == UIFlowState.UI_CHARGING) {
                stopReason = StopTransaction.Reason.REMOTE;
                LogWrapper.v("StopReason", "Remote Stop Accepted");
                onChargingStop();
                LogWrapper.e(TAG, "RemoteStopTransaction Received..");
            }
        }
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
        if (chargeData.isdspCommError || chargeData.ismeterCommError || isDspFault) {
            return false;
        } else {
            if (flowState == UIFlowState.UI_MAIN || flowState == UIFlowState.UI_SELECT ||
                    flowState == UIFlowState.UI_CARD_TAG) {

                // RemoteStart 후 authorize 진행 하기위하여 ocppsessionstate 변경
                ocppSessionManager.getOcppSesstion(connectorId).setSessionStateAuthStart();

                pageManager.showAuthWaitView(channel);
                setUIFlowState(UIFlowState.UI_AUTH_WAIT);

                mainActivity.setRemoteStartedVisible(View.VISIBLE);
                isRemoteStarted = true;
                isStopByCard = false;
                isStopByStartTransactionInvalid = false;
                isStopbySuspendedEVSE = false;
                isStopByCompliteCharging = false;
                lastCardNum = idTag;
                chargeData.isNomember = false;


//                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.PREPARING);
                LogWrapper.v(TAG, "RemoteStartTransaction Received..");

                ocppSessionManager.setLastAuthConnectorID(chargeData.curConnectorId);

//                // 이미 Connect 된 상태이라면
//                if ( isDspPlug ) {
//                    onConnectedCableEvent(true);
//                }

                multiChannelUIManager.rfidReaderRequest(channel);
                ocppSessionManager.authorizeRequest(chargeData.curConnectorId, idTag);
                return true;
            }
        }

        return false;
    }

    /**
     * StartTranscation의 IdTagInfo 상태를 받았을 때처리함.
     *
     * @param connectorId
     * @param tagInfo
     * @param transactionId
     */
    public void onStartTransactionResult(int connectorId, IdTagInfo tagInfo, int transactionId) {
        if (chargeData.curConnectorId == connectorId) {
            // 커넥터 상태를 충전중으로 바꾼다.(Status 메시지 보냄)
            setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.CHARGING);
            chargeData.startransaction_parentID = tagInfo.getParentIdTag();
            chargeData.curTransactionID = transactionId;

            if (flowState == UIFlowState.UI_CHARGING) {
                if (tagInfo.getStatus() == IdTagInfo.Status.ACCEPTED) {
                } else {
                    // 커넥터 상태를 SUSPENDED_EVSE 로 바꾸고 충전을 중지한다.
                    setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.SUSPENDED_EVSE);
                    OCPPConfiguration ocppConfiguration = ocppSessionManager.getOcppConfiguration();
                    // 만약 StopTransactionOnInvalidId 가 true 이면 충전을 중지한다.
                    if (ocppConfiguration.StopTransactionOnInvalidId == true) {
                        stopReason = StopTransaction.Reason.DE_AUTHORIZED;
                        isStopByStartTransactionInvalid = true;
                        isRemoteStarted = false;
                        isStopByCard = false;
                        isStopbySuspendedEVSE = false;
                        LogWrapper.v("StopReason", "StartTransaction Rejected");
                        onChargingStop();
                    }
                }
            }
        }
    }

    @Override
    public ReserveNowResponse.Status onReserveNow(int connectorId, Calendar expiryDate, String idTag, String parentIdTag, int reservationId) {
        ReserveNowResponse.Status ret = ReserveNowResponse.Status.REJECTED;

        /** 5.13 Reserve Now
         * If the reservationId does not match any reservation in the Charge Point, then the Charge Point SHALL
         return the status value ‘Accepted’ if it succeeds in reserving a connector. The Charge Point SHALL
         return ‘Occupied’ if the Charge Point or the specified connector are occupied. The Charge Point SHALL
         also return ‘Occupied’ when the Charge Point or connector has been reserved for the same or another
         idTag. The Charge Point SHALL return ‘Faulted’ if the Charge Point or the connector are in the Faulted
         state. The Charge Point SHALL return ‘Unavailable’ if the Charge Point or connector are in the
         Unavailable state. The Charge Point SHALL return ‘Rejected’ if it is configured not to accept
         reservations.
         */

        if (isConnectorOperative == false) ret = ReserveNowResponse.Status.UNAVAILABLE;
        else if (flowState != UIFlowState.UI_MAIN) ret = ReserveNowResponse.Status.OCCUPIED;
        else if (isDspFault) ret = ReserveNowResponse.Status.FAULTED;
        else if (reserveInfo.reservationId > 0 && reserveInfo.reservationId != reservationId)
            ret = ReserveNowResponse.Status.REJECTED;
        else {
            reserveInfo.setInfo(reservationId, idTag, parentIdTag, expiryDate);
            mainActivity.setReservedVisible(View.VISIBLE);
            ret = ReserveNowResponse.Status.ACCEPTED;
            setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.RESERVED);
        }
        return ret;
    }

    @Override
    public void onTriggerMessage(TriggerMessage message) {
        switch (message.getRequestedMessage().toString()) {
            case "MeterValues":
                DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
                sendOcppMeterValues(rxData, SampledValue.Context.TRIGGER, false);
                break;
        }
    }


    @Override
    public void onChangeAvailability(int connectorId, ChangeAvailability.Type type) {
        if (connectorId == chargeData.curConnectorId || connectorId == 0) {
            if (type == ChangeAvailability.Type.OPERATIVE) {
                isConnectorOperative = true;
                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);
                // 사용불가 화면 숨김
                pageManager.hideUnavailableConView();
            } else {
                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.UNAVAILABLE);
                isConnectorOperative = false;
                if (flowState != UIFlowState.UI_MAIN) onPageCommonEvent(PageEvent.GO_HOME);
            }
        }
    }

    @Override
    public void onResetRequest(boolean isHard) {
    }

    @Override
    public void onUpdateFirmwareRequest(URI location, int retry, Calendar retrieveDate, int retryInterval) {

    }

    @Override
    public void onTimeUpdate(Calendar syncTime) {
        if (!chargeData.serverStat) onOCPPTransportConnected();
    }

    @Override
    public void onDataTransferResponse(DataTransferResponse.Status status, String messageID, String data) {
        if (status == DataTransferResponse.Status.ACCEPTED) {
            if (messageID.equals(TypeDefine.DATA_TRANSFER_MESSAGEID_COST_UNIT)) {
                onCostUnitResponse(data);
            }
        }
    }

    void onCostUnitResponse(String data) {
        //회원 단가까지 받은 후 인증 완료 시퀀스 진행
        onRequestAuthUnitPriceResponse(data);

        if (flowState == UIFlowState.UI_AUTH_WAIT) {
            //충전 옵션설정 화면
            setUIFlowState(UIFlowState.UI_SELECT_CHARGING_OPTION);
            pageManager.changePage(PageID.SELECT_CHATGING_OPTION, channel);

            pageManager.hideAuthWaitView();
        }
    }

    JSONArray authUnitPrice;    // 변동단가 저장장
    int unitPriceIndex = 0;

    void onRequestAuthUnitPriceResponse(String data) {
        try {
            unitPriceIndex = 0;
            JSONObject obj = new JSONObject(data);
            authUnitPrice = obj.getJSONArray("tariff");
            obj = new JSONObject(authUnitPrice.getString(unitPriceIndex));
            String unitCost = obj.getString("price");
            String startTime = obj.getString("startAt");
            String endTime = obj.getString("endAt");

            chargeData.chargingUnitCost = Double.parseDouble(unitCost);

            LogWrapper.v(TAG, "CostInfo Received: " + chargeData.chargingUnitCost +
                    " / start:" + startTime + " ~ end:" + endTime);


        } catch (JSONException e) {
            LogWrapper.e(TAG, "Datatansfer Responese Json Parse Err:" + e.toString());
        }
    }


    @Override
    public void onDataTransferRequest(OCPPMessage message) {

    }

    @Override
    public boolean onCheckUIChargingStatus() {
        return false;
    }


    //================================================
    // RFID 이벤트 수신
    //================================================
    @Override
    public void onRfidDataReceive(String rfid, boolean success) {
        if (flowState == UIFlowState.UI_CARD_TAG || flowState == UIFlowState.UI_CHARGING) {
            onCardTagEvent(rfid, true);
        }
    }

    public void onAuthTimerOverEvent() {
        pageManager.getCardTagView().onPageDeactivate();
        chargeData.messageBoxTitle = "authtimeout";
        chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.timeover);
        setUIFlowState(UIFlowState.UI_AUTH_ALARM);
        pageManager.showAuthalarmBox(channel);
    }

    public void onServerconnctErrorEvent() {
        chargeData.messageBoxTitle = "serverconnecterror";
        chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.serverfail);
        setUIFlowState(UIFlowState.UI_AUTH_ALARM);
        pageManager.showAuthalarmBox(channel);
    }

    public void onRetryAuthclickEvent() {
        ocppSessionManager.startSession(chargeData.curConnectorId);
        if (chargeData.isNomember) {
            setUIFlowState(UIFlowState.UI_SET_CHARGING_OPTION);
            pageManager.changePage(PageID.SET_CHARING_OPTION, channel);
        } else {
            setUIFlowState(UIFlowState.UI_CARD_TAG);
            pageManager.getCardTagView().onPageActivate(channel);
        }
        pageManager.hideAuthalarmBox();

    }

    public void onMainPageStart() {
        if (cpConfig.isFastCharger) {
            pageManager.changePage(PageID.SELECT_FAST, channel);
        } else {
            pageManager.changePage(PageID.SELECT_SLOW, channel);
        }

        if (flowState == UIFlowState.UI_MAIN) {
            setUIFlowState(UIFlowState.UI_SELECT);
            initPaymentInfo();
        }
    }

    /**
     * 결제관련 변수 초기화
     */
    public void initPaymentInfo() {
        //거래요청관련(아래)
        chargeData.paymentClassification = "";       //결제구분(1:승인, 2:취소, 3:부분취소)
        chargeData.approvalRequestPrice = 0;            //승인요청금액(최초결제 혹은 취소금액)
        chargeData.requestPrice = 0;
        chargeData.installmentMonth = "";            //할부개월
        //결제 (부분)취소 시만 해당(아래)
        chargeData.cancelClassificationCode = "";    //취소구분코드
        chargeData.approvalNumber = "";              //승인번호
        chargeData.paymentDate = "";                 //원거래일
        chargeData.paymentTime = "";                 //원거래시간
        chargeData.approvalCancelKey = "";           //승인취소키(승인시 응답값 PG거래 일련번호 30자리)

        //거래응답관련(아래)
        chargeData.transactionTypeCode = "";         //거래구분코드
        chargeData.transactionMedium = "";           //거래매체
        chargeData.paycardNo = "";                   //카드번호
        chargeData.paidPrice = 0;                       //승인금액(최초결제 혹은 취소금액)
        chargeData.tax = 0;                             //세금
        chargeData.authCode = "";                    //승인번호
        chargeData.paidDate = "";                    //매출일자
        chargeData.paidTime = "";                    //매출기간
        chargeData.tid = "";                         //거래고유번호
        chargeData.pgTransactionSerialNo = "";       //pg거래일련번호
        chargeData.mrhstNo = "";                     //가맹점번호
        chargeData.terminalNo = "";                  //단말기번호
        chargeData.issureCode = "";                  //발급사코드
        chargeData.issureName = "";                  //발급사명
        chargeData.resCode = "";                     //응답코드
        chargeData.resMessage = "";                  //응답메시지

        chargeData.prePayTid = "";           // 선결제 tid

        cancelpayRetry = 0;

    }


    /**
     * 신용카드 선결제 금액 설정완료시
     */
    public void onSetPrepayCostOk() {
        if (chargeData.isNomember) {
            //최초결제 대기상태로 전환
            setUIFlowState(UIFlowState.UI_INSERT_CREDIT_CARD);
            //최초결제 대기 화면으로 전환
            pageManager.changePage(PageID.INSERT_CREDIT_CARD, channel);

            //요청정보 관련 데이터 세팅
            onSetPaymentRequestData("1", chargeData.approvalRequestPrice, "", "", "", "", "", "");

            //최초결제 요청
//            tl3500s.payReq_G(chargeData.approvalRequestPrice, 0, true, channel, cpConfig.chargerID);
            tl3500s.payReq_G(chargeData.approvalRequestPrice, 0, true, channel, "인천공항 전기차충전");

        } else {
            setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
            doAuthComplete();
        }
    }

    public void onSelectMember() {
        chargeData.isNomember = false;
        //카드태깅 화면
        setUIFlowState(UIFlowState.UI_CARD_TAG);
        pageManager.changePage(PageID.CARD_TAG, channel);
    }

    public void onSelectNomember() {
        chargeData.isNomember = true;

        multiChannelUIManager.rfidReaderRequest(channel);

        // 승인대기 화면 전환
        setUIFlowState(UIFlowState.UI_AUTH_WAIT);
        pageManager.showAuthWaitView(channel);

        String data = makeUnitpriceJsonObject(chargeData.isNomember);
        //단가정보 요청
        ocppSessionManager.sendDataTransferReq(TypeDefine.DATA_TRANSFER_MESSAGEID_COST_UNIT, data);
    }

    public void onFinishChargingHomeClick() {
        setUIFlowState(UIFlowState.UI_UNPLUG);
        pageManager.changePage(PageID.UNPLUG, channel);
    }

    public boolean isCostChargeOtion;

    public void onSelectChargingOption(boolean iscostcharging) {
        isCostChargeOtion = iscostcharging;
        setUIFlowState(UIFlowState.UI_SET_CHARGING_OPTION);
        pageManager.changePage(PageID.SET_CHARING_OPTION, channel);
    }

}
