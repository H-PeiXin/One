package com.h2004c.upload;

import io.reactivex.Flowable;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {
    String sUrl = "https://www.liulongbin.top:8888/";
    //请求头
    //file

    /**
     *  * 请求头 :authorization : Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOjUwMCwicmlkIjowLCJpYXQiOjE2MDcwNjY0NDcsImV4cCI6MTYwNzE1Mjg0N30.jeHnyLxcu_enp_ka1kS7YwkC-nzer7UfG0-iJgOCCyE
     *  * 参数: file -- 文件
     * @return
     */
    //@FormUrlEncoded post请求提交参数Field/FieldMap
    //Multipart 上传文件使用
    @Multipart
    @POST("api/private/v1/upload")
    Flowable<ResponseBody> upload(@Header("authorization") String token,
                                  @Part MultipartBody.Part file);
}
