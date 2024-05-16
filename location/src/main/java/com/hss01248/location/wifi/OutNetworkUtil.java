package com.hss01248.location.wifi;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @Despciption todo
 * @Author hss
 * @Date 5/16/24 3:42 PM
 * @Version 1.0
 */
public class OutNetworkUtil {

    public static boolean canUseGoogle = false;
    public interface NetworkCheckListener {
        void onNetworkStatusChecked(boolean isConnected);
    }

    public static void checkIfCanReachGoogle(final NetworkCheckListener listener) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://www.google.com";

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 网络请求失败，可能是网络问题或请求问题
                listener.onNetworkStatusChecked(false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 网络请求成功，通过状态码判断是否真的能连接到Google
                if (response.isSuccessful()) {
                    listener.onNetworkStatusChecked(true);
                } else {
                    listener.onNetworkStatusChecked(false);
                }
                response.close();
            }
        });
    }
}
