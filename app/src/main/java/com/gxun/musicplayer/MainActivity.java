package com.gxun.musicplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ListView musicListLv;
    private SeekBar seekBar;
    private TextView musicCurrentTimeTv, musicTotalTimeTv, musicInfoTv;
    private Button startBtn, stopBtn, nextBtn, lastBtn;

    private final int RW_PERMISSION = 600;//权限申请码

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //申请权限
        if (!(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                && !(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                && !(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS) == PackageManager.PERMISSION_GRANTED)) {
            //没有权限，申请权限
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS};
            //申请权限，其中RW_PERMISSION是权限申请码，用来标志权限申请的
            ActivityCompat.requestPermissions(MainActivity.this, permissions, RW_PERMISSION);
        }

        //初始化控件
        musicListLv = findViewById(R.id.music_list);
        seekBar = findViewById(R.id.seek_bar);
        musicCurrentTimeTv = findViewById(R.id.current_time);
        musicTotalTimeTv = findViewById(R.id.total_time);
        musicInfoTv = findViewById(R.id.music_info);
        startBtn = findViewById(R.id.btn_start);
        stopBtn = findViewById(R.id.btn_stop);
        nextBtn = findViewById(R.id.btn_next);
        lastBtn = findViewById(R.id.btn_last);

        //设置点击事件
        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        nextBtn.setOnClickListener(this);
        lastBtn.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_start:
                //startPlay();
                break;
            case R.id.btn_stop:
                //stopPlay();
                break;
            case R.id.btn_next:
                //nextSong();
                break;
            case R.id.btn_last:
                //lastSong();
                break;
        }
    }
}
