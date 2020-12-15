package com.h2004c.upload;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.ResourceSubscriber;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

/**
 * 登录
 * https://www.liulongbin.top:8888/api/private/v1/login
 * post
 * 参数:
 * username(admin),password(123456)
 * <p>
 * 上传图片
 * https://www.liulongbin.top:8888/api/private/v1/upload
 * post
 * 请求头 :authorization : Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOjUwMCwicmlkIjowLCJpYXQiOjE2MDcwNjY0NDcsImV4cCI6MTYwNzE1Mjg0N30.jeHnyLxcu_enp_ka1kS7YwkC-nzer7UfG0-iJgOCCyE
 * 参数: file -- 文件
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private Button mBtn;
    private Button mBtnRetrofit;
    private Button mBtnHttp;
    private Button mBtnStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPers();
        initView();
    }

    private void initPers() {
        String[] pers = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        ActivityCompat.requestPermissions(this, pers, 100);
    }

    private void initView() {
        mBtn = (Button) findViewById(R.id.btn);
        mBtnRetrofit = (Button) findViewById(R.id.btn_Retrofit);
        mBtnHttp = (Button) findViewById(R.id.btn_http);

        mBtn.setOnClickListener(this);
        mBtnRetrofit.setOnClickListener(this);
        mBtnHttp.setOnClickListener(this);
        mBtnStart = (Button) findViewById(R.id.btn_start);
        mBtnStart.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn:
                okUpload();
                break;
            case R.id.btn_Retrofit:
                retrofitUpload();
                break;
            case R.id.btn_http:
                final File file = new File("/storage/emulated/0/test1.jpg");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            uploadForm(null, "file", file,
                                    file.getName(), "https://www.liulongbin.top:8888/api/private/v1/upload");

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                break;
            case R.id.btn_start:
                startActivity(new Intent(this,CameraActivity.class));
                break;
        }
    }

    /**
     * HttpUrlConnection　实现文件上传
     *
     * @param params       普通参数
     * @param fileFormName 文件在表单中的键
     * @param uploadFile   上传的文件
     * @param newFileName  文件在表单中的值（服务端获取到的文件名）
     * @param urlStr       url
     * @throws IOException
     */
    public void uploadForm(Map<String, String> params, String fileFormName,
                           File uploadFile, String newFileName, String urlStr)
            throws IOException {

        if (newFileName == null || newFileName.trim().equals("")) {
            newFileName = uploadFile.getName();
        }
        //string stringbuffer,stringBuilder
        //线程不安全
        StringBuilder sb = new StringBuilder();
        /**
         * 普通的表单数据
         */
        if (params != null) {
            for (String key : params.keySet()) {
                sb.append("--" + BOUNDARY + "\r\n");
                sb.append("Content-Disposition: form-data; name=\"" + key + "\"" + "\r\n");
                sb.append("\r\n");
                sb.append(params.get(key) + "\r\n");
            }
        }

        /**
         * 上传文件的头
         */
        sb.append("--" + BOUNDARY + "\r\n");

        sb.append("Content-Disposition: form-data; name=\"" + fileFormName + "\"; " +
                "filename=\"" + newFileName + "\""
                + "\r\n");
        sb.append("Content-Type: application/octet-stream" + "\r\n");// 如果服务器端有文件类型的校验，必须明确指定ContentType
        sb.append("\r\n");

        byte[] headerInfo = sb.toString().getBytes("UTF-8");
        byte[] endInfo = ("\r\n--" + BOUNDARY + "--\r\n").getBytes("UTF-8");


        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        // 设置传输内容的格式，以及长度

        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
        conn.setRequestProperty("Content-Length",
                String.valueOf(headerInfo.length + uploadFile.length() + endInfo.length));
        //设置请求头
        conn.setRequestProperty("authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOjUwMCwicmlkIjowLCJpYXQiOjE2MDcwNjY0NDcsImV4cCI6MTYwNzE1Mjg0N30.jeHnyLxcu_enp_ka1kS7YwkC-nzer7UfG0-iJgOCCyE");
        conn.setDoOutput(true);

        OutputStream out = conn.getOutputStream();
        InputStream in = new FileInputStream(uploadFile);

        //写入的文件长度
        int count = 0;
        //文件的总长度
        int available = in.available();
        // 写入头部 （包含了普通的参数，以及文件的标示等）
        out.write(headerInfo);
        // 写入文件
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
            count += len;
            int progress = count * 100 / available;
            Log.d(TAG, "上传进度: " + progress + " %");

        }
        // 写入尾部
        out.write(endInfo);
        in.close();
        out.close();
        if (conn.getResponseCode() == 200) {
            System.out.println("文件上传成功");
            String s = stream2String(conn.getInputStream());
            Log.d(TAG, "uploadForm: " + s);
        }
    }

    // 分割符,自己定义即可
    private static final String BOUNDARY = "----WebKitFormBoundaryT1HoybnYeFOGFlBR";

    public String stream2String(InputStream is) {
        int len;
        byte[] bytes = new byte[1024];
        StringBuffer sb = new StringBuffer();
        try {
            while ((len = is.read(bytes)) != -1) {
                sb.append(new String(bytes, 0, len));
            }

            is.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    private void retrofitUpload() {
        MediaType type = MediaType.parse("application/octet-stream");
        File file = new File("/storage/emulated/0/test1.jpg");
        if (file.exists()) {
            RequestBody fileBody = RequestBody.create(
                    type, file
            );
            MultipartBody.Part filePart =
                    MultipartBody.Part.createFormData("file", "11.jpg", fileBody);
            new Retrofit.Builder()
                    .baseUrl(ApiService.sUrl)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
                    .create(ApiService.class)
                    .upload("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOjUwMCwicmlkIjowLCJpYXQiOjE2MDcwNjY0NDcsImV4cCI6MTYwNzE1Mjg0N30.jeHnyLxcu_enp_ka1kS7YwkC-nzer7UfG0-iJgOCCyE",
                            filePart)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new ResourceSubscriber<ResponseBody>() {
                        @Override
                        public void onNext(ResponseBody responseBody) {
                            try {
                                Log.d(TAG, "onNext: " + responseBody.string());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(Throwable t) {
                            Log.d(TAG, "onError: " + t.toString());
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } else {
            showToast("文件不存在");
        }


    }

    private void okUpload() {
        //client.newCall().enqueue();
        File file = new File("/storage/emulated/0/test1.jpg");
        if (file.exists()) {
            //媒体类型
            //MediaType type = MediaType.parse("image/*");
            //二级制流的形式上传
            MediaType type = MediaType.parse("application/octet-stream");
            //放置文件的请求体
            RequestBody fileBody = RequestBody.create(type, file);
            //请求体
            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)//form表单的形式
                    .addFormDataPart("file", "aa.jpg", fileBody)
                    .build();


            Request request = new Request.Builder()
                    .url("https://www.liulongbin.top:8888/api/private/v1/upload")
                    ////添加请求头
                    .addHeader("authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOjUwMCwicmlkIjowLCJpYXQiOjE2MDcwNjY0NDcsImV4cCI6MTYwNzE1Mjg0N30.jeHnyLxcu_enp_ka1kS7YwkC-nzer7UfG0-iJgOCCyE")
                    .post(body)
                    .build();
            new OkHttpClient.Builder()
                    .build()
                    .newCall(request)
                    .enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Log.d(TAG, "onFailure: " + e.toString());
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            Log.d(TAG, "onResponse: " + response.body().string());
                        }
                    });
        } else {
            showToast("文件不存在");
        }

    }

    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
