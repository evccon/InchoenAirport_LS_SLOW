/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 5. 2. 오후 4:29
 *
 */

package com.joas.ocppui_LS_2ch.webservice;
import androidx.annotation.NonNull;

import com.yanzhenjie.andserver.annotation.Interceptor;
import com.yanzhenjie.andserver.framework.HandlerInterceptor;
import com.yanzhenjie.andserver.framework.handler.RequestHandler;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;

@Interceptor
public class CORSInterceptor implements HandlerInterceptor {

    @Override
    public boolean onIntercept(@NonNull HttpRequest request, @NonNull HttpResponse response,
                               @NonNull RequestHandler handler) {
        /*
        String path = request.getPath();
        HttpMethod method = request.getMethod();
        MultiValueMap<String, String> valueMap = request.getParameter();
        Logger.i("Path: " + path);
        Logger.i("Method: " + method.value());
        Logger.i("Param: " + JsonUtils.toJsonString(valueMap));
        */

        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Acces-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
        return false;
    }
}