package com.ayhalo.mediacodecdemo;

import android.content.Intent;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.ayhalo.mediacodecdemo.adapter.VideoListAdapter;
import com.ayhalo.mediacodecdemo.bean.VideoBean;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Halo";
    private String rootPath;
    private RecyclerView recyclerView;
    private ArrayList<VideoBean> beanList = new ArrayList<>();
    private LinearLayout empty_rel;
    private VideoListAdapter videoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        //rootPath = "/udisk2";
        Log.d(TAG, rootPath);
        //checkMediaDecoder();
        new MyTask(rootPath).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    protected void initView() {
        empty_rel = (LinearLayout) findViewById(R.id.empty_rel);
        recyclerView = (RecyclerView) findViewById(R.id.video_list);
        videoAdapter = new VideoListAdapter(this, beanList);
        LinearLayoutManager layoutmanager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutmanager);
        recyclerView.setAdapter(videoAdapter);
        videoAdapter.setOnItemClickListener(new VideoListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Intent intent = new Intent(getApplicationContext(), FileAVC1Thread.class);
                intent.putExtra("path", beanList.get(position).getPath());
                Log.d(TAG, "onItemClick: " + beanList.get(position).getPath());
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(View view, int position) {
                // TODO: 2017/9/20
            }
        });
    }

    class MyTask extends AsyncTask<Void, Void, Void> {
        String path;

        MyTask(String path) {
            this.path = path;
        }

        @Override
        protected Void doInBackground(Void... params) {
            getAllVideo(path);
            return null;
        }

        @Override
        protected void onPostExecute(Void params) {
            if (beanList.size() > 0) {
                empty_rel.setVisibility(View.GONE);
            } else {
                empty_rel.setVisibility(View.VISIBLE);
                Log.d(TAG, "onPostExecute: " + beanList.size());
            }
            Log.d(TAG, "onPostExecute: " + beanList.size());
            videoAdapter.notifyDataSetChanged();
        }
    }

    public void getAllVideo(String path) {
        File souFile = new File(path);
        if (souFile.isDirectory()) {
            File[] file = souFile.listFiles();
            if (file != null) {
                for (File mFile : file) {
                    if (mFile.isFile()) {
                        if (mFile.isHidden()) continue;
                        int extPos = mFile.getName().lastIndexOf('.');
                        if (extPos >= 0) {
                            String ext = mFile.getName().substring(extPos + 1);
                            if (!TextUtils.isEmpty(ext) && sMediaExtSet.contains(ext)) {
                                VideoBean videoBean = new VideoBean();
                                videoBean.setName(mFile.getName());
                                videoBean.setPath(mFile.getAbsolutePath());
                                beanList.add(videoBean);
                            }
                        }
                    }
                    if (mFile.isDirectory()) {
                        getAllVideo(path + File.separator + mFile.getName());
                    }
                }
            }
        }
    }

    private static Set<String> sMediaExtSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    static {
        sMediaExtSet.add("flv");
        sMediaExtSet.add("mp4");
        sMediaExtSet.add("h264");
        sMediaExtSet.add("avi");
        sMediaExtSet.add("rmvb");
    }

    private void checkMediaDecoder() {
        int numCodecs = MediaCodecList.getCodecCount();
        MediaCodecInfo mediaCodecInfo = null;
        for (int i = 0; i < numCodecs && mediaCodecInfo == null; i++) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            if (info.isEncoder()) {
                continue;
            }
            String[] types = info.getSupportedTypes();
            boolean found = false;
            for (int j = 0; j < types.length && !found; j++) {
                Log.d(TAG, "checkMediaDecoder: " + types[j]);
            }
        }
    }
}
