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
    public static final String CURRENT_POSITION_ACTION = "com.gxun.musicplayer.DURATION_ACTION";
    public static final String SEEKBAR_PROGRESS_ACTION = "com.gxun.musicplayer.SEEKBAR_PROGRESS_ACTION";
    public static final String CLICKITEM_ACTION = "com.gxun.musicplayer.CLICKITEM_ACTION";

    // 定义音乐的播放状态，stop代表没有播放，playing代表正在播放，pause代表暂停
    String status = "stop";

    int currentPosition, musicMaxTime;

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

        musicInfoTv.setText("当前无播放"); // 设置播放信息

        activityReceiver = new ActivityReceiver();
        // 创建IntentFilter
        IntentFilter filter = new IntentFilter();
        // 指定BroadcastReceiver监听的Action
        filter.addAction(UPDATE_ACTION); // 更新动作，用于接收MusicService回传的音乐信息的Intent
        filter.addAction(CURRENT_POSITION_ACTION); // 更新动作，用于接收MusicService回传的进度条信息的Intent
        // 注册BroadcastReceiver
        registerReceiver(activityReceiver, filter);
        final Intent intent = new Intent(this, MusicService.class);
        // 启动后台Service
        startService(intent);

        // ListView点击事件
        musicListLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent itemIntent = new Intent(CLICKITEM_ACTION);
                itemIntent.putExtra("musicNum", position);
                sendBroadcast(itemIntent);
            }
        });

        // 进度条实现
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { //进度条
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { // 用于监听SeekBar进度值的改变
                if (fromUser){
                    int seekBarMax = seekBar.getMax();
                    sendProgress(progress, seekBarMax);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { // 用于监听SeekBar开始拖动
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { // 用于监听SeekBar停止拖动  SeekBar停止拖动后的事件
            }
        });

    }

    private void sendProgress(int progress, int seekBarMax) {
        Intent progressIntent = new Intent(SEEKBAR_PROGRESS_ACTION);
        progressIntent.putExtra("Progress", progress);
        progressIntent.putExtra("SeekBarMax", seekBarMax);
        sendBroadcast(progressIntent);
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

    // 设置总播放时间
    void setTotalTime(int max){
        int mMinutes = 0;
        while (max >= 60) {
            mMinutes++;
            max -= 60;
        }
        String totalTime = (mMinutes < 10 ? "0" + mMinutes : mMinutes) + ":"
                + (max < 10 ? "0" + max : max);
        musicTotalTimeTv.setText(totalTime);
    }
    //设置当前时间及进度条
    void setCurrentTime(int position) {
        int pMinutes = 0;
        while (position >= 60) {
            pMinutes++;
            position -= 60;
        }
        String currentTime = (pMinutes < 10 ? "0" + pMinutes : pMinutes) + ":"
                + (position < 10 ? "0" + position : position);
        musicCurrentTimeTv.setText(currentTime);
        seekBar.setProgress(currentPosition * seekBar.getMax() / musicMaxTime);
    }

    //自定义BroadcastReceiver，负责监听从Service传回来的广播
    public class ActivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case UPDATE_ACTION:
                    updateStatus(intent);
                    break;
                case CURRENT_POSITION_ACTION:
                    updateSeekBar(intent);
                    break;
            }
        }
        public void updateStatus(Intent intent){
            // 获取Intent中的newStatus（新的播放状态）
            String newStatus = intent.getStringExtra("newStatus");
            // 获取Intent中的musicName（当前播放的歌曲名）
            String musicName = intent.getStringExtra("musicName");
            musicMaxTime = intent.getIntExtra("maxTime", 0);
            String musicInfo = "";
            switch (newStatus) {
                case "stop": // 无播放状态
                    startBtn.setText("开始");
                    musicInfo = "当前无播放";
                    initialSeekBar();
                    status = newStatus;
                    break;
                case "playing": // 正在播放状态
                    startBtn.setText("暂停");
                    musicInfo = "正在播放: " + musicName;
                    status = newStatus;
                    break;
                case "pause": // 暂停状态
                    startBtn.setText("开始");
                    musicInfo = "播放暂停: " + musicName;
                    status = newStatus;
                    break;
            }
            musicInfoTv.setText(musicInfo);
            setTotalTime(musicMaxTime/1000); // 广播返回的为ms，需转换为s
        }

        public void updateSeekBar(Intent intent){
            currentPosition = intent.getIntExtra("currentPosition", 0);
            setCurrentTime(currentPosition/1000);
        }
    }
}