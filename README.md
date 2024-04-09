Super Retrofit
========

This project fork from [Retrofit](https://github.com/square/retrofit),I refactor project to make custom protocol( base TCP) 、volley、UrlConnection support restful api

You can use some call factories(UrlConnectionCallFactory / OkHttpCallFactory / VolleyCallFactory /CronetCallFactory)) to create call , and add you custom protocol to this project.

## Get Started

replace `implementation com.squareup.retrofit2:retrofit:2.9.0` with `implementation 'io.github.jamesfchen:retrofit:1.0.0'`


### UrlConnection

`implementation 'io.github.jamesfchen:callfactory-urlconnection:1.0.0'`

```java
 Retrofit retrofit =
                new Retrofit.Builder()
                        .baseUrl(server.url("/"))
                        .addConverterFactory(new ToStringConverterFactory())
                        .callFactory(UrlConnectionCallFactory.create())
                        .build();
        Service example = retrofit.create(Service.class);

        server.enqueue(new MockResponse().setBody("Hi"));

        Response<String> response = example.getString().execute();
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body()).isEqualTo("Hi");
```

### Volley

`implementation 'io.github.jamesfchen:callfactory-volley:1.0.0'`

```kotlin
        val api = Retrofit.Builder()
                .baseUrl("....")
                .addConverterFactory(GsonConverterFactory.create())
                .callFactory(VolleyCallFactory.create(application))
                .build().create(LocationApi::class.java)
```

### Okhttp

1 case:

```java
Retrofit retrofit =
                new Retrofit.Builder()
                        .baseUrl(server.url("/"))
                        .addConverterFactory(new ToStringConverterFactory())
                        .callFactory(OkHttpCallFactory.create())
                        .build();
        Service example = retrofit.create(Service.class);

        server.enqueue(new MockResponse().setBody("Hi"));

        Response<String> response = example.getString().execute();
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body()).isEqualTo("Hi");
```

2 case:

```java
OkHttpClient okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .pingInterval(1, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .protocols(listOf(Protocol.HTTP_2))
                .build()
Retrofit retrofit =
                new Retrofit.Builder()
                        .baseUrl(server.url("/"))
                        .addConverterFactory(new ToStringConverterFactory())
                        .callFactory(OkHttpCallFactory.create(okHttpClient))
                        .build();
        Service example = retrofit.create(Service.class);

        server.enqueue(new MockResponse().setBody("Hi"));

        Response<String> response = example.getString().execute();
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body()).isEqualTo("Hi");
```

### Cronet

To Be Countinue...

### Mars

To Be Countinue...
