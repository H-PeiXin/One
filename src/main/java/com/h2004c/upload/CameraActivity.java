package com.h2004c.upload;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;

import cn.jzvd.Jzvd;
import cn.jzvd.JzvdStd;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int CAMERA_CODE = 0;
    private static final String TAG = "CameraActivity";
    private static final int ALBUM_CODE = 1;
    private Button mBtnCamera;
    private Button mBtnAlbum;
    private File mFile;
    private Uri mImageUri;
    private Button mBtnJiaozi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        initPers();
        initView();
    }

    private void initPers() {
        String[] pers = {
                Manifest.permission.CAMERA
        };
        ActivityCompat.requestPermissions(this, pers, 100);
    }

    private void initView() {
        mBtnCamera = (Button) findViewById(R.id.btn_camera);
        mBtnAlbum = (Button) findViewById(R.id.btn_album);

        mBtnCamera.setOnClickListener(this);
        mBtnAlbum.setOnClickListener(this);
        mBtnJiaozi = (Button) findViewById(R.id.btn_jiaozi);
        mBtnJiaozi.setOnClickListener(this);

        mJzvdStd = (JzvdStd) findViewById(R.id.jz_video);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_camera:

                openCamera();
                break;
            case R.id.btn_album:
                openAlbum();
                break;
            case R.id.btn_jiaozi:
               break;
        }
    }

    //饺子播放器
    @Override
    public void onBackPressed() {
        if (Jzvd.backPress()) {
            return;
        }
        super.onBackPressed();
    }
    @Override
    protected void onPause() {
        super.onPause();
        Jzvd.releaseAllVideos();
    }

    //开启相册上传
    private void openAlbum() {

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, ALBUM_CODE);
    }

    //拍照上传
    private void openCamera() {
        //创建文件用于保存图片
        //android/data/包名/cache/
        //System.currentTimeMillis():当前时间的毫秒值
        mFile = new File(getExternalCacheDir(), System.currentTimeMillis() + ".jpg");
        if (!mFile.exists()) {
            try {
                mFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //适配7.0
        //私有目录访问受限
        //FileUriExposedException: file:///storage/emulated/0/Android/data/com.h2004c.upload/cache/1607148649359.jpg exposed
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            mImageUri = Uri.fromFile(mFile);
        } else {
            //第二个参数要和清单文件中的配置保持一致
            mImageUri = FileProvider.getUriForFile(this, "com.h2004c.upload.provider", mFile);
        }

        //启动相机
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);//将拍照图片存入mImageUri
        startActivityForResult(intent, CAMERA_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    //Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(mImageUri));
                    //处理照相之后的结果并上传
                    uploadOk(mFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (requestCode == ALBUM_CODE) {
            //相册
            if (resultCode == RESULT_OK) {
                Uri imageUri = data.getData();
                //处理uri,7.0以后的fileProvider 把URI 以content provider 方式 对外提供的解析方法
                File file = getFileFromUri(imageUri, this);

                if (file.exists()) {
                    uploadOk(file);
                }
            }
        }
    }

    public File getFileFromUri(Uri uri, Context context) {
        if (uri == null) {
            return null;
        }
        switch (uri.getScheme()) {
            case "content":
                //代表系统是7.0及其以上
                return getFileFromContentUri(uri, context);
            case "file":
                //7.0以下
                return new File(uri.getPath());
            default:
                return null;
        }
    }

    /**
     * 通过内容解析中查询uri中的文件路径
     */
    private File getFileFromContentUri(Uri contentUri, Context context) {
        if (contentUri == null) {
            return null;
        }
        File file = null;
        String filePath;
        String[] filePathColumn = {MediaStore.MediaColumns.DATA};
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(contentUri, filePathColumn, null,
                null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            filePath = cursor.getString(cursor.getColumnIndex(filePathColumn[0]));
            cursor.close();

            if (!TextUtils.isEmpty(filePath)) {
                file = new File(filePath);
            }
        }
        return file;
    }

    private void uploadOk(File file) {
        //client.newCall().enqueue();
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
        }

    }
}
