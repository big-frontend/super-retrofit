package com.jamesfchen.urlconnection;


import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.util.Objects;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.Timeout;
import retrofit2.*;
import retrofit2.ExceptionCatchingResponseBody;

/**
 * Copyright ® $ 2020
 * All right reserved.
 */
public final class UrlConnectionCallFactory implements Call.Factory {
  /*
              String method = request.method();
              connection.setRequestMethod(method);
              for (int i = 0, size = request.headers().size(); i < size; ++i) {
                  String name = request.headers().name(i);
                  String value = request.headers().value(i);
                  connection.addRequestProperty(name, value);
              }
              if ("GET".equals(method)) {
                  connection.setDoInput(true);
                  connection.setInstanceFollowRedirects(false);
              } else if ("POST".equals(method)) {
                  connection.setDoOutput(true);
                  connection.setUseCaches(false);
                  OutputStream outputStream = connection.getOutputStream();
                  Buffer buffer = new Buffer();
                  assert request.body() != null;
                  request.body().writeTo(buffer);
                  outputStream.write(buffer.readByteArray());
                  outputStream.flush();
                  outputStream.close();
              }
//                connection.setConnectTimeout(timeout);
//                connection.setReadTimeout(timeout);
//                connection.setHostnameVerifier();
//                connection.setSSLSocketFactory();
//                connection.setAllowUserInteraction();
//                connection.setChunkedStreamingMode();
//                connection.setDefaultUseCaches();
//                connection.setFixedLengthStreamingMode();
//                connection.setIfModifiedSince();
              connection.connect();
   */
  public static UrlConnectionCallFactory create() {
    return new UrlConnectionCallFactory();
  }

  @Override
  public <T> Call<T> newCall(RequestFactory requestFactory, Object[] args, Converter<ResponseBody, T> responseConverter) {
    return new UrlConnectionCall<>(requestFactory, args, responseConverter);
  }

  static final class UrlConnectionCall<T> implements Call<T> {
    private final RequestFactory requestFactory;
    private final Object[] args;
    private HttpURLConnection rawConnection;
    private Converter<ResponseBody, T> responseConverter;
    private boolean executed;
    private volatile boolean canceled;

    UrlConnectionCall(RequestFactory requestFactory, Object[] args, Converter<ResponseBody, T> responseConverter) {
      this.requestFactory = requestFactory;
      this.args = args;
      this.responseConverter = responseConverter;
    }

    private HttpURLConnection createConnection(Request request) throws IOException {
      HttpURLConnection connection;

      if (request.url().isHttps()) {
        connection = (HttpsURLConnection) request.url().url().openConnection();
      } else {
        connection = (HttpURLConnection) request.url().url().openConnection();
      }
      return connection;
    }

    private void configConnection(Request request, HttpURLConnection connection) throws ProtocolException {
      for (int i = 0, size = request.headers().size(); i < size; ++i) {
        String name = request.headers().name(i);
        String value = request.headers().value(i);
        connection.addRequestProperty(name, value);
      }
      String method = request.method();
      connection.setRequestMethod(method);
      connection.setDoInput(true);
//                connection.setConnectTimeout(timeout);
//                connection.setReadTimeout(timeout);
//                connection.setHostnameVerifier();
//                connection.setSSLSocketFactory();
//                connection.setAllowUserInteraction();
//                connection.setChunkedStreamingMode();
//                connection.setDefaultUseCaches();
//                connection.setFixedLengthStreamingMode();
//                connection.setIfModifiedSince();
      rawConnection = connection;
    }

    @Override
    public Response<T> execute() throws IOException {
      HttpURLConnection connection;
      Request request;
      synchronized (this) {
        if (executed) throw new IllegalStateException("Already executed.");
        executed = true;
        request = requestFactory.create(args);
        connection = createConnection(request);
      }
      if (canceled) {
        connection.disconnect();
        throw new IOException("Canceled");
      }

      try {
        int i = 0;
        while (true) {
          try {
            synchronized (this) {
              connection = createConnection(request);
            }
            if (i <= 0) {
              break;
            }
            connection.setRequestProperty("Collection", "close");
            break;
          } catch (EOFException e) {
            i++;
            connection.disconnect();
          }
        }
        configConnection(request, connection);
        RequestBody rawbody = request.body();
        String contentType = "text/plain";
        if (rawbody != null) {
          contentType = Objects.requireNonNull(rawbody.contentType()).type();
          connection.setDoOutput(true);
          connection.addRequestProperty("Content-Type", contentType);
          long contentLegth = rawbody.contentLength();
          if (contentLegth != -1) {
            connection.setFixedLengthStreamingMode((int) contentLegth);
            connection.addRequestProperty("Content-Length", String.valueOf(contentLegth));
          } else {
            connection.setChunkedStreamingMode(4096);
          }
//                    body.writeTo(connection.getOutputStream())
          Buffer buffer = new Buffer();
          OutputStream outputStream = connection.getOutputStream();
          rawbody.writeTo(buffer);
          outputStream.write(buffer.readByteArray());
          outputStream.flush();
          outputStream.close();
        }
        InputStream inputStream = connection.getInputStream();
        byte[] rawResponseBody = readInputStream(inputStream);
        int responseCode = connection.getResponseCode();
        if (responseCode == 200 && rawResponseBody.length > 0) {
//                    int contentLength = connection.getContentLength();
//                    String responseMessage = connection.getResponseMessage();
//          String contentType = connection.getContentType();
          ResponseBody responseBody = ResponseBody.create(MediaType.parse(contentType), rawResponseBody);
          ExceptionCatchingResponseBody catchingBody = new ExceptionCatchingResponseBody(responseBody);
          try {
            T body = responseConverter.convert(catchingBody);
            connection.disconnect();
            return Response.success(body);
          } catch (RuntimeException e) {
            // If the underlying source threw an exception, propagate that rather than indicating it was
            // a runtime exception.
            catchingBody.throwIfCaught();
            throw e;
          }
//                    return  Response.success(responseBody)
        }

        return null;
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }

    public static byte[] readInputStream(InputStream inputStream) {
      int count = 0;
      byte[] buff = new byte[4096];
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try {
        while ((count = inputStream.read(buff, 0, buff.length)) != -1) {
          baos.write(buff, 0, count);
        }
        inputStream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return baos.toByteArray();
    }

    @Override
    public void enqueue(Callback<T> callback) {
      Executors.newCachedThreadPool().execute(() -> {
        try {
          Response<T> execute = execute();
          callback.onResponse(this, execute);
        } catch (IOException e) {
          callback.onFailure(this, e);
        }
      });
    }

    @Override
    public boolean isExecuted() {
      return executed;
    }

    @Override
    public void cancel() {
      canceled = true;

      HttpURLConnection connection;
      synchronized (this) {
        connection = rawConnection;
      }
      if (connection != null) {
        connection.disconnect();
      }
    }

    @Override
    public boolean isCanceled() {
      return canceled;
    }

    @Override
    public Call<T> clone() {
      return new UrlConnectionCall<>(requestFactory, args, responseConverter);
    }

    @Override
    public Request request() {
      try {
        return requestFactory.create(args);
      } catch (IOException e) {
        throw new RuntimeException("Unable to create request.", e);
      }
    }

    @Override
    public Timeout timeout() {
      return null;
    }


  }
}
