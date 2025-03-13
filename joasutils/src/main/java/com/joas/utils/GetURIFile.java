/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 1. 15 오후 3:41
 *
 */

package com.joas.utils;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Async Task to download file from URL
 */
public class GetURIFile extends AsyncTask<String, String, String> {
    public final static String TAG = "GetURIFile";
    private static final int BUFFER_SIZE = 4096;

    private String fileName;
    private String m_localPath;
    boolean endFlag = false;
    GetURIFileListener listener;

    /**
     * new GetURIFile(path).execute(url);
     */

    public GetURIFile(String filename, GetURIFileListener getURIFileListener) {
        this.fileName = filename;               //filename : /storage/emulated/0/Update/update.zip
        this.listener = getURIFileListener;
    }

    /**
     * Downloading file in background thread
     */
    @Override
    protected String doInBackground(String... uri) {
        URL url;
        int Read;
        int totalReadLen = 0;

        try {
            url = new URL(uri[0]);
            if (url.getProtocol().equals("http") || url.getProtocol().equals("https")) {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setConnectTimeout(1000*10); // 10초 타임아웃
//            conn.setReadTimeout(1000*10); // 10초 타임아웃
//            conn.setRequestProperty("Accept-Encoding", "identity");
//            Log.i("Net", "length = " + conn.getContentLength());
//            Log.i("Net", "respCode = " + conn.getResponseCode());
//            Log.i("Net", "contentType = " + conn.getContentType());
//            Log.i("Net", "content = " + conn.getContent());

                int len = conn.getContentLength();
                byte[] tmpByte;
                if (len > 0) {
                    tmpByte = new byte[len];
                } else {
                    tmpByte = new byte[20000000];
                }

                InputStream is = conn.getInputStream();

                File file = null;
                file = new File(this.fileName);

                FileOutputStream fos = new FileOutputStream(file);
                for (; ; ) {
                    Read = is.read(tmpByte);
                    if (Read <= 0) {
                        break;
                    }
                    fos.write(tmpByte, 0, Read);
//                totalReadLen += Read;
//                msg_downloadstat = "" + ((int) totalReadLen * 100 / len) + "%(" + "" + len + "/" + "" + totalReadLen + ")";
//                LogWrapper.d(TAG, " " + msg_downloadstat);
                }
                is.close();
                fos.close();
                conn.disconnect();

                if (this.listener != null) this.listener.onGetURIFileFinished();
            } else if (url.getProtocol().equals("ftp")) {
//                URLConnection conn = url.openConnection();
//                InputStream inputStream = conn.getInputStream();
//
//                File file = null;
//                file = new File(this.fileName);
//                FileOutputStream outputStream = new FileOutputStream(file);
//
//                byte[] buffer = new byte[4096];
//                int bytesRead;
//                while ((bytesRead = inputStream.read(buffer)) != -1) {
//                    outputStream.write(buffer, 0, bytesRead);
//                }
//
//                outputStream.close();
//                inputStream.close();


                FTPClient ftpClient = new FTPClient();
                ftpClient.connect(url.getHost(), url.getPort() == -1 ? url.getDefaultPort() : url.getPort());

                int reply = ftpClient.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    ftpClient.disconnect();
                    Log.e(TAG, "FTP server refused connection.");
                    if (this.listener != null) this.listener.onGetURIFileError("");
                    return "";
                }

                String[] userInfo = url.getUserInfo().split(":");

                boolean loginSuccess = ftpClient.login(userInfo[0], userInfo[1]);
                if (!loginSuccess) {
                    ftpClient.disconnect();
                    Log.e(TAG, "FTP login failed.");
                    if (this.listener != null) this.listener.onGetURIFileError("");
                    return "";
                }

                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                ftpClient.enterLocalPassiveMode();

                OutputStream outputStream = new FileOutputStream(this.fileName);
                boolean downloadSuccess = ftpClient.retrieveFile(url.getPath(), outputStream);
                outputStream.close();

                if (downloadSuccess) {
                    Log.d(TAG, "File downloaded successfully.");
                } else {
                    Log.e(TAG, "File download failed.");
                }

                ftpClient.logout();
                ftpClient.disconnect();


                if (this.listener != null) this.listener.onGetURIFileFinished();

            } else {
                if (this.listener != null)
                    this.listener.onGetURIFileError("Not Supported Protocol");
            }

        } catch (Exception e) {
            if (this.listener != null) this.listener.onGetURIFileError(e.toString());
        }


        return "";
    }
}