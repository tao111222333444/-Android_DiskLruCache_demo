package com.example.disklrucache.Activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

import com.example.disklrucache.R;
import com.example.disklrucache.Util.MD5Util;
import com.example.disklrucache.Util.Util_download_image;
import com.example.disklrucache.libcore.io.DiskLruCache;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by 涛 on 2015/6/12.
 */
public class ImageAcivity extends Activity {
    private String TAG = "ImageAcivity";
    private DiskLruCache mDiskLruCache;
    private String key;
    private String imageUrl = "http://image.baidu.com/i?tn=download&ipn=dwnl&word=download&ie=utf8&fr" +
            "=result&url=http%3A%2F%2Fpic1.ooopic.com%2Fup" +
            "loadfilepic%2Fsheji%2F2009-08-29%2FOOOPIC_wenneng837_20090829e618c617f6cd3dc6.jpg";
    private String image1Url = "http://image.baidu.com/i?tn=download&ipn=dwnl&word=download&ie=utf8&f" +
            "r=result&url=http%3A%2F%2Fpic1.nipic.com%2F2008-09-08%2F200898163242920_2.jpg";
    private Bitmap bitmap;
    private ImageView image,image1;
    private HashMap<String , Bitmap> bitmap_map = new HashMap<>();
    /**
     * 记录所有正在下载或等待下载的任务。
     */
    private Set<BitmapWorkerTask> taskCollection;
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
        setContentView(R.layout.activity_image);
        image = (ImageView) findViewById(R.id.image);
        image1  = (ImageView) findViewById(R.id.image1);

        taskCollection = new HashSet<BitmapWorkerTask>();
        mDiskLruCache = Util_download_image.getDiskLruCache(ImageAcivity.this,"bitmap");
        for(int i=0;i<2;i++){
            try {
                if(i==0){
                }else{
                    imageUrl = image1Url;
                }
                key = MD5Util.MD5(imageUrl);//获取存储本地的文件名
                Log.e(TAG,i+"     "+key);
                DiskLruCache.Snapshot snapShot = null;//获取存储的文件
                snapShot = mDiskLruCache.get(key);
                if (snapShot != null) {//判断是否获取到该文件
                    InputStream is = snapShot.getInputStream(0);//
                    bitmap = BitmapFactory.decodeStream(is);
                    if(i==0){
                    image.setImageBitmap(bitmap);
                    }else{
                        image1.setImageBitmap(bitmap);
                    }
                    Log.e(TAG, "未访问网络  从缓存获取");
                }else{
                    Log.e(TAG, "访问网络 下载");
                    BitmapWorkerTask task = new BitmapWorkerTask();
                    taskCollection.add(task);
                    task.execute(imageUrl);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        fluchCache();
    }
    /**
     * 将缓存记录同步到journal文件中。
     */
    public void fluchCache() {
        if (mDiskLruCache != null) {
            try {
                mDiskLruCache.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 异步下载图片的任务。
     *
     * @author guolin
     */
    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

        /**
         * 图片的URL地址
         */
        private String imageUrl;

        @Override
        protected Bitmap doInBackground(String... params) {
            imageUrl = params[0];
            FileDescriptor fileDescriptor = null;
            FileInputStream fileInputStream = null;
            DiskLruCache.Snapshot snapShot = null;
            try {
                // 生成图片URL对应的key
                final String key = MD5Util.MD5(imageUrl);
                // 查找key对应的缓存
                snapShot = mDiskLruCache.get(key);
                if (snapShot == null) {
                    // 如果没有找到对应的缓存，则准备从网络上请求数据，并写入缓存
                    DiskLruCache.Editor editor = mDiskLruCache.edit(key);
                    if (editor != null) {
                        OutputStream outputStream = editor.newOutputStream(0);
                        if (Util_download_image.downloadUrlToStream(imageUrl, outputStream)) {
                            editor.commit();
                        } else {
                            editor.abort();
                        }
                    }
                    // 缓存被写入后，再次查找key对应的缓存
                    snapShot = mDiskLruCache.get(key);
                }
                if (snapShot != null) {
                    fileInputStream = (FileInputStream) snapShot.getInputStream(0);
                    fileDescriptor = fileInputStream.getFD();
                }
                // 将缓存数据解析成Bitmap对象
                Bitmap bitmap = null;
                if (fileDescriptor != null) {
                    bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                }
                if (bitmap != null) {
                    // 将Bitmap对象添加到内存缓存当中
                    bitmap_map.put(key,bitmap);
//                    addBitmapToMemoryCache(params[0], bitmap);
                }
                return bitmap;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fileDescriptor == null && fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e) {
                    }
                }
            }
            return null;
        }
    }
    }
