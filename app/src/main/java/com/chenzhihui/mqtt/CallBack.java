/*
 *  Copyright (c) 2016 Meituan Inc.
 *
 *     The right to copy, distribute, modify, or otherwise make use
 *     of this software may be licensed only pursuant to the terms
 *     of an applicable Meituan license agreement.
 *
 */

package com.chenzhihui.mqtt;

public interface CallBack {
    void onResponse(String response);
    void onErrorResponse(String error);
}