package com.joas.hw.fwUpdater;

import android.util.Log;

import com.joas.hw.dsp2.DSPControl2;
import com.joas.utils.LogWrapper;
import com.joas.utils.Xmodem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FWUpdater extends Thread {
    public static final String TAG = "FWUpdater";

    DSPControl2 dspControl;
    byte[] uploadedFWData;

    String fwFilePath;
    FWUpdaterListener fwUpdaterListener;

    public FWUpdater(DSPControl2 _dspControl, String filePath, FWUpdaterListener _fwUpdaterListener) {
        dspControl = _dspControl;
        fwFilePath = filePath;
        fwUpdaterListener = _fwUpdaterListener;
    }

    @Override
    public void run() {
        if (fwUpdaterListener != null) fwUpdaterListener.onFWUpdateStart();
        LogWrapper.v(TAG, "FW UPDATE START!!!!!");

        //.bin 파일을 uploadedFWData 바이트 배열에 저장
        File file = new File(fwFilePath);
        uploadedFWData = new byte[(int) file.length()];
        Log.v(TAG, "File Length :" + (int) file.length());
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            fileInputStream.read(uploadedFWData);
        } catch (IOException e) {
            LogWrapper.e(TAG, "File input Exception : " + e);
        }


        // DSP 통신 정지
        dspControl.stopThread();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        byte[] slowOTAData = {
                0x01, 0x10, 0x00, (byte) 0xC8, 0x00, 0x1D, 0x3A, (byte) 0x80, 0x01, 0x00, 0x07,
                0x00, 0x07, 0x09, (byte) 0x91, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                (byte) 0xA1, 0x48, 0x43, 0x58, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x5B, (byte) 0xF5};


        // DSP 200번지 15BIT SET(raw Data)
        dspControl.sendRawData(slowOTAData);
        LogWrapper.v(TAG, "FW UPDATE BIT SET");

        while (true) {
            String receiveData = dspControl.read2bytesRawData();

            // DSP에서 응답 신호 "FF" 올 때까지 대기
            if (receiveData.equals("FF")) {
                LogWrapper.v(TAG, "DSP response OK: " + receiveData);
                break;
            }
        }

        // DSP 통신 포트 연결 해재
        dspControl.stopSerialPort();
        LogWrapper.v(TAG, "DSP RS485 Disconnected");

        // 기존 DSP포트에 xmodem 시리얼 포트 연결
        Xmodem xmodem = new Xmodem(dspControl.getSerialDev());
        LogWrapper.v(TAG, "Xmodem connecting");

        // 펌웨어 업데이트 진행
        if (xmodem.send(uploadedFWData)) {
            if (fwUpdaterListener != null) fwUpdaterListener.onFWUpdateComplete();
            LogWrapper.v(TAG, "FW UPDATE SUCCESS");
        } else {
            if (fwUpdaterListener != null) fwUpdaterListener.onFWUpdateFailed();
            LogWrapper.v(TAG, "FW UPDATE FAILED");

        }

        // xmodem 포트 연결 해재
        xmodem.stopSerialPort();


    }

}
