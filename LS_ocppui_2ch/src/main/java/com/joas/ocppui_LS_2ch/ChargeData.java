/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 5. 2. 오후 4:29
 *
 */

package com.joas.ocppui_LS_2ch;

import com.joas.ocppls.msg.StatusNotification;

import java.util.Date;

public class ChargeData {
    public int dspChannel = TypeDefine.DEFAULT_CHANNEL;

    /**
     * 동시에 사용 가능한 총 커넥처(채널) 수
     */
    public int ocppConnectorCnt = TypeDefine.OCPP_CONNECTOR_CNT_1CH_3MODE;

    /**
     * 커넥터 종류, UI에서 사용자 선택에 의해서 바뀜
     */
    public TypeDefine.ConnectorType connectorType = TypeDefine.ConnectorType.CTYPE;

    /**
     * 현재 커넥터 번호(현재 채널), 1개인 경우에는 항상 1이 됨
     */
    public int curConnectorId = TypeDefine.OCPP_DEFAULT_CONNECTOR_ID ;

    /**
     * UI에서 인증 타임아웃 시간, 기본 60초
     */
    public int authTimeout = TypeDefine.DEFAULT_AUTH_TIMEOUT;
    public int connectCarTimeout = TypeDefine.DEFAULT_CONNECT_CAR_TIMEOUT ;
    public int authnumInputTimeout = TypeDefine.NOMEM_INPUT_AUTH_NUM_TIMEOUT;

    // 충전진행시 필요로 하는 공유 변수들(충전량, 시간등)


    public long measureWh  = 0;
    public long meterVal = 0;
    public Date chargeStartTime = new Date();
    public Date chargeEndTime = new Date();
    public long chargingTime = 0;
    public double chargingCost = 0;
    public double chargingUnitCost = TypeDefine.DEFAULT_UNIT_COST;
    public int soc = 0;             // SOC (완속X)
    public int remainTime = 0;      // 충전 남은 시간(완속x)
    public double outputVoltage = 0;
    public double outputCurr = 0;

    //==============================
    // OCPP 통신에 필요한 값들
    //=============================

    // 현재 커넥터에 대한 상태값
    public StatusNotification.Status ocppStatus = StatusNotification.Status.AVAILABLE;
    public StatusNotification.ErrorCode ocppStatusError = StatusNotification.ErrorCode.NO_ERROR;
    public String ocppVendorError;

    /**
     * 메시지 박스 타이틀
     */
    public String messageBoxTitle = "";

    /**
     * 메시지 박스 내용(멀티라인)
     */
    public String messageBoxContent = "";

    /**
     * 메시지 박스 시간(초)
     */
    public int messageBoxTimeout = TypeDefine.MESSAGEBOX_TIMEOUT_SHORT;

    public String faultBoxContent = "";

    public boolean isdspCommError = false;
    public boolean ismeterCommError = false;
    public boolean serverStat = false;
    public String pidNum = "";
    public String startransaction_parentID = "";
    public int curTransactionID = 0;

    public boolean isNomember = false;

    //현장결제 관련 변수
    //거래요청관련(아래)
    public String paymentClassification = "";       //결제구분(1:승인, 2:취소, 3:부분취소)
    public int approvalRequestPrice = 0;            //승인요청금액(최초결제 혹은 취소금액)
    public int requestPrice = 0;                    // 최초결제 승인요청금액
    public String installmentMonth = "";            //할부개월
    //결제 (부분)취소 시만 해당(아래)
    public String cancelClassificationCode = "";    //취소구분코드
    public String approvalNumber = "";              //승인번호
    public String paymentDate = "";                 //원거래일
    public String paymentTime = "";                 //원거래시간
    public String approvalCancelKey = "";           //승인취소키(승인시 응답값 PG거래 일련번호 30자리)

    //거래응답관련(아래)
    public String transactionTypeCode = "";         //거래구분코드
    public String transactionMedium = "";           //거래매체
    public String paycardNo = "";                   //카드번호
    public int paidPrice = 0;                       //승인금액(최초결제 혹은 취소금액)
    public int tax = 0;                             //세금
    public String authCode = "";                    //승인번호
    public String paidDate = "";                    //매출일자
    public String paidTime = "";                    //매출기간
    public String tid = "";                         //거래고유번호
    public String pgTransactionSerialNo = "";       //pg거래일련번호
    public String mrhstNo = "";                     //가맹점번호
    public String terminalNo = "";                  //단말기번호
    public String issureCode = "";                  //발급사코드
    public String issureName = "";                  //발급사명
    public String resCode = "";                     //응답코드
    public String resMessage = "";                  //응답메시지

    //부분취소 DATATRANSFER 전문 관련
    public String prePayTid = "";       // 선결제 거래번호
}
