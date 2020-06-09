package com.gxun.musicplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ListView musicListLv;
    private SeekBar seekBar;
    private TextView musicCurrentTimeTv, musicTotalTimeTv, musicInfoTv;
    private Button startBtn, stopBtn, nextBtn, lastBtn;

    private Handler handler; // 处理改变进度条事件

    private final int RW_PERMISSION = 600; // 权限申请码

    //广播接收器
    ActivityReceiver activityReceiver;
    //定义控制动作、更新动作
    public static final String CTRL_ACTION = "com.gxun.musicplayer.CTRL_ACTION";
    public static final String UPDATE_ACTION = "com.gxun.musicplayer.UPDATE_ACTION";

    // 定义音乐的播放状态，0x11代表没有播放，0x12代表正在播放，0x13代表暂停
    int status = 0x11;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 申请文件读写权限
        if (!(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                && !(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                && !(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS) == PackageManager.PERMISSION_GRANTED)) {
            // 没有权限，申请权限
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS};
            // 申请权限，其中RW_PERMISSION是权限申请码，用来标志权限申请的
            ActivityCompat.requestPermissions(MainActivity.this, permissions, RW_PERMISSION);
            super.onCreate(savedInstanceState); // 授权后列表无内容，需重新初始化
        }

        // 初始化控件
        musicListLv = findViewById(R.id.music_list);
        seekBar = findViewById(R.id.seek_bar);
        musicCurrentTimeTv = findViewById(R.id.current_time);
        musicTotalTimeTv = findViewById(R.id.total_time);
        musicInfoTv = findViewById(R.id.music_info);
        startBtn = findViewById(R.id.btn_start);
        stopBtn = findViewById(R.id.btn_stop);
        nextBtn = findViewById(R.id.btn_next);
        lastBtn = findViewById(R.id.btn_last);

        // 设置点击事件
        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        nextBtn.setOnClickListener(this);
        lastBtn.setOnClickListener(this);

        initialMusicList(); // 初始化音乐列表
        initialSeekBar(); // 初始化进度条
        // 设置播放信息栏
        musicInfoTv.setText("当前无播放");

        activityReceiver = new ActivityReceiver();
        // 创建IntentFilter
        IntentFilter filter = new IntentFilter();
        // 指定BroadcastReceiver监听的Action
        filter.addAction(UPDATE_ACTION); // 更新动作，用于接收MusicService回传的Intent
        // 注册BroadcastReceiver
        registerReceiver(activityReceiver, filter);
        final Intent intent = new Intent(this, MusicService.class);
        // 启动后台Service
        startService(intent);

        // ListView点击事件
        musicListLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });

        // 进度条实现
//        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { //进度条
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { // 用于监听SeekBar进度值的改变
//
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) { // 用于监听SeekBar开始拖动
//
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) { // 用于监听SeekBar停止拖动  SeekBar停止拖动后的事件
//                int progress = seekBar.getProgress();
//                Log.i("TAG:", "" + progress + "");
//                int musicMaxTime = intent.getIntExtra("maxTime",-1); // 得到该首歌曲最长秒数
//                int seekBarMax = seekBar.getMax();
//                double currentProcess = musicMaxTime * progress / seekBarMax;
//                musicService.player.seekTo(); // 跳到该曲该秒
//            }
//        });
//        Thread t = new Thread() { // 自动改变进度条的线程
//            @Override
//            public void run() {
//                int position, mMax, sMax;
//                while (!Thread.currentThread().isInterrupted()) { // 线程未结束会一直返回false，此时可以一直刷新进度条
//                    if (musicService.player != null && musicService.player.isPlaying()) {
//                        position = musicService.getCurrentProgress(); // 得到当前歌曲播放进度(秒)
//                        mMax = musicService.player.getDuration(); // 最大秒数
//                        sMax = seekBar.getMax();// seekBar最大值，算百分比
//                        Message m = handler.obtainMessage(); // 获取一个Message
//                        m.arg1 = position * sMax / mMax; // seekBar进度条的百分比
//                        m.arg2 = position; // 当前位置
//                        m.what = status; // 状态
//                        handler.sendMessage(m);
//                        try {
//                            Thread.sleep(1000);// 每间隔1秒发送一次更新消息
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }
//        };
//        // 实例化一个handler对象
//        // 通过 Handler 更新 UI 上的组件状
//        handler = new Handler() {
//            @Override
//            public void handleMessage(Message msg) {
//                super.handleMessage(msg);
//                int mMax = musicService.player.getDuration(); // 最大秒数
//                if (msg.what == status) {
//                    try {
//                        seekBar.setProgress(msg.arg1);
//                        setPlayTime(msg.arg2 / 1000, mMax / 1000);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    seekBar.setProgress(0);
//                    musicInfoTv.setText("播放已经停止");
//                }
//            }
//        };
//        t.start(); //启动线程
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(CTRL_ACTION);
        //设定控制指令，向MusicService发送，达到控制播放效果
        switch (v.getId()) {
            case R.id.btn_start:
                intent.putExtra("control", 1);
                break;
            case R.id.btn_stop:
                intent.putExtra("control", 2);
                break;
            case R.id.btn_last:
                intent.putExtra("control", 3);
                break;
            case R.id.btn_next:
                intent.putExtra("control", 4);
                break;
        }
        sendBroadcast(intent); // 发送广播
    }

    // 初始化音乐列表向列表添加mp3名字
    void initialMusicList() {
        List<String> musicList = new MusicScanner().getMusicList();
        String[] str = new String[musicList.size()];
        int i = 0;
        for (String path : musicList) {
            File file = new File(path);
            str[i++] = file.getName();
        }
        /**
         * 创建数组适配器，作为数据源和列表控件联系的桥梁
         * parameter1:上下文环境
         * parameter2: 当前列表项加载的布局文件
         * parameter3: 数据源
         */
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, str);
        musicListLv.setAdapter(adapter);
    }

    // 初始化进度条
    void initialSeekBar() {
        musicCurrentTimeTv.setText("00:00");
        musicTotalTimeTv.setText("00:00");
        seekBar.setProgress(0); // 进度条置0
    }

    // 设置当前播放的信息
    void setPlayTime(int position, int max) {

        int pMinutes = 0;
        while (position >= 60) {
            pMinutes++;
            position -= 60;
        }

        String currentTime = (pMinutes < 10 ? "0" + pMinutes : pMinutes) + ":"
                + (position < 10 ? "0" + position : position);
        musicCurrentTimeTv.setText(currentTime);

        int mMinutes = 0;
        while (max >= 60) {
            mMinutes++;
            max -= 60;
        }

        String totalTime = (mMinutes < 10 ? "0" + mMinutes : mMinutes) + ":"
                + (max < 10 ? "0" + max : max);
        musicTotalTimeTv.setText(totalTime);
    }

    //自定义BroadcastReceiver，负责监听从Service传回来的广播
    public class ActivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 获取Intent中的newStatus（新的播放状态）
            int newStatus = intent.getIntExtra("newStatus", -1);
            // 获取Intent中的musicName（当前播放的歌曲名）
            String musicName = intent.getStringExtra("musicName");
            String musicInfo = "";
            switch (newStatus) {
                case 0x11: // 无播放状态
                    startBtn.setText("开始");
                    musicInfo = "当前无播放";
                    initialSeekBar();
                    status = 0x11;
                    break;
                case 0x12: // 正在播放状态
                    startBtn.setText("暂停");
                    musicInfo = "正在播放: " + musicName;
                    status = 0x12;
                    break;
                case 0x13: // 暂停状态
                    startBtn.setText("开始");
                    musicInfo = "播放暂停: " + musicName;
                    status = 0x13;
                    break;
            }
            musicInfoTv.setText(musicInfo);
        }
    }
}