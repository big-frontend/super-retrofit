package com.jamesfchen.cronet;


import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.RequestFactory;

import javax.annotation.Nullable;

/**
 * Copyright ® $ 2021
 * All right reserved.
 *
 * @author jamesfchen
 * @since 9月/28/2021  周二
 */
public class CronetCallFactory implements Call.Factory {

  @Override
  public <T> Call<T> newCall(RequestFactory requestFactory, Object[] args, Converter<ResponseBody, T> responseConverter) {
    return null;
  }
}
