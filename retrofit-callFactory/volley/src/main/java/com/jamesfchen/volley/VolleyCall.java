package com.jamesfchen.volley;

import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import kotlin.NotImplementedError;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Timeout;
import retrofit2.*;
import retrofit2.ExceptionCatchingResponseBody;
import retrofit2.NoContentResponseBody;

/**
 * Copyright ® $ 2021
 * All right reserved.
 *
 */
public final class VolleyCall<ResponseT> implements Call<ResponseT> {
  private final RequestFactory requestFactory;
  private final Object[] args;
  private Converter<ResponseBody, ResponseT> responseConverter;
  private RequestQueue internalQueue;

  VolleyCall(RequestFactory requestFactory, Object[] args, Converter<ResponseBody, ResponseT> responseConverter, RequestQueue internalQueue) {
    this.requestFactory = requestFactory;
    this.args = args;
    this.responseConverter = responseConverter;
    this.internalQueue = internalQueue;
  }

  @Override
  public Response<ResponseT> execute() throws IOException {
    throw new NotImplementedError("不支持execute方法");
  }

  private volatile boolean canceled;
  boolean executed = false;
  VolleyRequest<ResponseT> volleyRequest;

  @Override
  public void enqueue(@NotNull Callback<ResponseT> callback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already executed");
      executed = true;
      try {
        volleyRequest = getVolleyRequest(callback);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

    }
    if (canceled) {
      volleyRequest.cancel();
    }
    internalQueue.add(volleyRequest);
  }

  VolleyRequest<ResponseT> getVolleyRequest(Callback<ResponseT> callback) throws IOException  {
    if (volleyRequest != null) {
      return volleyRequest;
    }
    Request request  = requestFactory.create(args);
    int method = 0;
    if ("GET".equalsIgnoreCase(request.method())) {
      method = com.android.volley.Request.Method.GET;
    } else if ("POST".equalsIgnoreCase(request.method())) {
      method = com.android.volley.Request.Method.GET;
    }
    return volleyRequest = new VolleyRequest<>(method, request, this, responseConverter, callback);
  }


  @Override
  public boolean isExecuted() {
    return executed;
  }

  @Override
  public void cancel() {
    canceled = true;
    synchronized (this) {
      if (volleyRequest != null) {
        volleyRequest.cancel();
      }
    }
  }

  @Override
  public boolean isCanceled() {
    if (canceled) return true;
    synchronized (this) {
      return volleyRequest != null && volleyRequest.isCanceled();
    }
  }


  @Override
  public Call<ResponseT> clone() {
    return null;
  }

  @Override
  public Request request() {
    return null;
  }

  @Override
  public Timeout timeout() {
    return null;
  }

  static class VolleyRequest<ResponseT> extends com.android.volley.Request<byte[]> {
    private Callback<ResponseT> callback;
    private VolleyCall<ResponseT> call;
    private Request request;
    private Converter<ResponseBody, ResponseT> responseConverter;

    public VolleyRequest(int method, Request request, VolleyCall<ResponseT> call, Converter<ResponseBody, ResponseT> responseConverter, Callback<ResponseT> callback) {
      super(method, request.url().toString(), null);
      this.call = call;
      this.callback = callback;
      this.request = request;
      this.responseConverter = responseConverter;
    }

    public VolleyRequest(int method, String url, VolleyCall<ResponseT> call, Converter<ResponseBody, ResponseT> responseConverter) {
      super(method, url, null);
      this.call = call;
      this.responseConverter = responseConverter;
    }

    @Override
    protected com.android.volley.Response<byte[]> parseNetworkResponse(NetworkResponse response) {
//            int i = new Random().nextInt(4);
//            try {
//                Thread.sleep(i*1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
      if (!isCanceled() && callback != null) {
        Response<ResponseT> r;
        try {
          r = parseResponse(response);
        } catch (IOException e) {
//          Utils.throwIfFatal(e);
          callback.onFailure(call, e);
          return null;
        }
        callback.onResponse(call, r);
      }
      return com.android.volley.Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(byte[] response) {
    }

    @Override
    protected VolleyError parseNetworkError(VolleyError volleyError) {
      if (!isCanceled() && callback != null) {
        callback.onFailure(call, volleyError);
      }
      return super.parseNetworkError(volleyError);
    }

    Response<ResponseT> parseResponse(NetworkResponse response) throws IOException {
      okhttp3.Response rawRespons = new okhttp3.Response.Builder().body(new NoContentResponseBody(MediaType.parse(response.headers.get("content-type")), response.data.length)).code(response.statusCode).protocol(Protocol.HTTP_1_1).request(request).message("成功").build();
      if (response.statusCode < 200 || response.statusCode >= 300) {
        rawRespons = rawRespons.newBuilder().message("Response.error()").build();
        try (ResponseBody respBody = ResponseBody.create(MediaType.parse(response.headers.get("content-type")), response.data)) {
          Response.error(respBody, rawRespons);
        }
      }
      //204 没有内容  / 205 reset content
      if (response.statusCode == 204 || response.statusCode == 205) {
        return Response.success(null, rawRespons);
      }
      ResponseBody respBody = ResponseBody.create(MediaType.parse(response.headers.get("content-type")), response.data);
      ExceptionCatchingResponseBody catchingBody = new ExceptionCatchingResponseBody(respBody);
      try {
        ResponseT body = responseConverter.convert(catchingBody);
        return Response.success(body, rawRespons);
      } catch (IOException e) {
        catchingBody.throwIfCaught();
        throw e;
      }
    }
  }

}
