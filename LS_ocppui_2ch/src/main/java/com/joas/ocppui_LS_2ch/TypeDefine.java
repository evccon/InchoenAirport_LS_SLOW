/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 5. 2. 오후 4:29
 *
 */

package com.joas.ocppui_LS_2ch;

public class TypeDefine {
    public enum ConnectorType {
        AC3,
        CHADEMO,
        DCCOMBO,
        BTYPE,
        CTYPE
    }
    /** 개정 이력
     * 2.X.4 2023.06.02     : DataTransfer 의 Value값들의 변수 타입, 변수명 서버와 동기화 작업
     *
     * 2.X.5 2023.07.26     : 신용카드 결제 시 상품명에 "인천공항 전기차충전 " 들어가도록 수정
     *
     * 2.A.6 2023.08.28     : 내역 누락..
     */
    public static final String SW_VER = "v2.A.6"; //  V chanel . 예비 . 배포버전
    public static final String SW_RELEASE_DATE = "2023.08.28";

    public static final int MAX_CHANNEL = 2;

    public static final int CP_TYPE_SLOW_AC = 0x02;
    public static final int CP_TYPE_FAST_3MODE = 0x06;

    public static final int CP_AC_POWER_OFFERED = 7040; // w
    public static final int CP_DC_POWER_OFFERED = 20 * 1000; //w
    public static final int CP_AC_CURRENT_OFFERED = 32;
    public static final int CP_DC_CURRENT_OFFERED = 100;

    // 미터프로그램 동작 모니터링 타임아웃
    public static final int COMM_METER_TIMEOUT = 60; // 1 min

    public static final int DEFAULT_CHANNEL = 0;

    public static final int DEFAULT_UNIT_COST = 100; // 100원

    public static final int OCPP_CONNECTOR_CNT_1CH_3MODE = 1;
    public static final int OCPP_DEFAULT_CONNECTOR_ID = 1;

    // Timer 값 정의
    public static final int DEFAULT_AUTH_TIMEOUT = 60; // sec
    public static final int DEFAULT_CONNECT_CAR_TIMEOUT = 60; // sec
    public static final int MESSAGEBOX_TIMEOUT_SHORT = 10; // sec
    public static final int NOMEM_INPUT_AUTH_NUM_TIMEOUT = 180;     //sec
    public static final int GO_SELECT_PAGE_TIMEOUT = 10; // sec

    public static final int FIRMWARE_UPDATE_COUNTER = 30; // 펌웨어 다운로드 후 업데이트 카운트

    public static final int METERING_CHANGE_TIMEOUT = 10 * 60;     //10 * 60초 :  10분

    // 충전중 상태 정보 주기
    public static final int COMM_CHARGE_STATUS_PERIOD = 300; // 5 min

    // 시간 오차값 허용 범위 정의
    public static final int TIME_SYNC_GAP_MS = 10 * 1000; // 10 sec

    //프로그램 시작후 WatchDog 타이머 시작까지 시간
    public static final int WATCHDOG_START_TIMEOUT = 10; // 5 min

    public static final String REPOSITORY_BASE_PATH = "/SmartChargerData";
    public static final String FORCE_CLOSE_LOG_PATH = REPOSITORY_BASE_PATH + "/ForceCloseLog";
    public static final String CP_CONFIG_PATH = REPOSITORY_BASE_PATH + "/CPConfig";
    //add by si. 210330
    public static final String METERCONFIG_BASE_PATH = "/MeterViewConfig";
    public static final String METER_CONFIG_FILENAME = "MeterViewConfig.txt";

    public static final int DISP_CHARGING_CHARLCD_PERIOD = 10; // 8초
    public static final int DISP_CHARLCD_BACKLIGHT_OFF_TIMEOUT = 60; //초

    public static final int SUSPENDED_EVSE_CHECK_TIMEOUT = 1;      //초

    public static final String DATA_TRANSFER_MESSAGEID_TIMEOFFSET = "timeOffset";
    public static final String DATA_TRANSFER_MESSAGEID_GATEWAY_INFO = "gatewayInfo";
    public static final String DATA_TRANSFER_MESSAGEID_CP_CHARGING_PARAM = "cpChargingParameterRpt";
    public static final String DATA_TRANSFER_MESSAGEID_COST_UNIT = "tariff";
    public static final String DATA_TRANSFER_MESSAGEID_PAYMENT_INFO = "guestAdvancePayment";
    public static final String DATA_TRANSFER_MESSAGEID_CANCEL_PAYMENT_INFO = "partialCancellation";
    public static final String DATA_TRANSFER_MESSAGEID_CHARGINGTIME = "chargingTimeRpt";
    public static final String DATA_TRANSFER_MESSAGEID_FINALCHARGING = "finalChargingRpt";


    public static final int MAX_CHARGING_TIME = 10 * 60 * 60 ; // 10시간
    public static final int CANCELPAY_MAX_RETRY_ATTEMPTS = 3;  // 결제취소 실패 시 재시도 횟수
    public static final int MAX_UNPLUG_TIME = 5 * 60; // 충전완료화면에서 메인으로 넘어가는시간(초)

}
