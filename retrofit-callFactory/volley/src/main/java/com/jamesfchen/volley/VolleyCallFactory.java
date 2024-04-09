package com.jamesfchen.volley;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import okhttp3.Request;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.RequestFactory;

/**
 * Copyright ® $ 2020
 * All right reserved.
 */
public final class VolleyCallFactory implements Call.Factory {
  public Application application;

  public VolleyCallFactory(Application application) {
    this.application = application;
  }

  public static VolleyCallFactory create(Application application) {
    return new VolleyCallFactory(application);
  }

  @NotNull
  @Override
  public <T> Call<T> newCall(RequestFactory requestFactory, Object[] args, Converter<ResponseBody, T> responseConverter) {
    return new VolleyCall<T>(requestFactory, args, responseConverter, getInternalQueue(application.getApplicationContext()));
  }

  static volatile RequestQueue internalQueue;

  private static RequestQueue getInternalQueue(Context context) {
    if (internalQueue == null) {
      synchronized (VolleyCallFactory.class) {
        if (internalQueue == null) {
          internalQueue = Volley.newRequestQueue(context);
          internalQueue.addRequestEventListener(new RequestQueue.RequestEventListener() {
            @Override
            public void onRequestEvent(com.android.volley.Request<?> request, int event) {
              Log.d("cjf", "request id:" + request.getSequence() + " event:" + event2String(event));
            }
          });
        }
      }
    }
    return internalQueue;
  }

  private static String event2String(int event) {
    switch (event) {
      case RequestQueue.RequestEvent.REQUEST_QUEUED:
        return "add queue";
      case RequestQueue.RequestEvent.REQUEST_CACHE_LOOKUP_STARTED:
        return "start lookup request";
      case RequestQueue.RequestEvent.REQUEST_CACHE_LOOKUP_FINISHED:
        return "finish lookup request";
      case RequestQueue.RequestEvent.REQUEST_NETWORK_DISPATCH_STARTED:
        return "start dispatch request";
      case RequestQueue.RequestEvent.REQUEST_NETWORK_DISPATCH_FINISHED:
        return "finish dispatch request";
      case RequestQueue.RequestEvent.REQUEST_FINISHED:
        return "pop queue";
      default:
        return "";
    }
  }
}
