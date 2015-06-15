package com.example.disklrucache.Activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.example.disklrucache.R;
import com.example.disklrucache.Util.MD5Util;
import com.example.disklrucache.Util.Util_download_image;
import com.example.disklrucache.libcore.io.DiskLruCache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class MainActivity extends Activity {
    private String TAG = "MainActivity";
    private DiskLruCache mDiskLruCache;
    private String key;
    private String imageUrl = "http://image.baidu.com/i?tn=download&ipn=dwnl&word=download&" +
            "ie=utf8&fr=result&url=http%3A%2F%2Fpic1.ooopic.com%2Fuploadfilepic%2F" +
            "sheji%2F2009-08-09%2FOOOPIC_SHIJUNHONG_20090809ad6104071d324dda.jpg";
    private Bitmap bitmap;
    private ImageView image;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            DiskLruCache.Snapshot snapShot = null;//获取存储的文件
            try {
                snapShot = mDiskLruCache.get(key);
            } catch (IOException e) {
                e.printStackTrace();
            }
            InputStream is = snapShot.getInputStream(0);//
            bitmap = BitmapFactory.decodeStream(is);
            image.setImageBitmap(bitmap);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDiskLruCache = Util_download_image.getDiskLruCache(MainActivity.this,"bitmap");
        image = (ImageView) findViewById(R.id.image);
        try {
            key = MD5Util.MD5(imageUrl);//获取存储本地的文件名
            DiskLruCache.Snapshot snapShot = mDiskLruCache.get(key);//获取存储的文件
            if (snapShot != null) {//判断是否获取到该文件
                InputStream is = snapShot.getInputStream(0);//
                bitmap = BitmapFactory.decodeStream(is);
                image.setImageBitmap(bitmap);
                Log.e(TAG,"未访问网络  从缓存获取");
            }else{
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            DiskLruCache.Editor editor = mDiskLruCache.edit(key);
                            if (editor != null) {
                                OutputStream outputStream = editor.newOutputStream(0);
                                if (Util_download_image.downloadUrlToStream(imageUrl, outputStream)) {
                                    editor.commit();
                                    handler.sendEmptyMessage(1);
                                } else {
                                    editor.abort();
                                }
                            }
                            mDiskLruCache.flush();//操作记录同步到日志文件（也就是journal文件）
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this,ImageAcivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
