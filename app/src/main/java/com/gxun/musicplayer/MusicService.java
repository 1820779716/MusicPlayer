package com.gxun.musicplayer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MusicService extends Service {

    private List<String> musicList; // 存放找到的所有mp3的绝对路径
    public MediaPlayer player; // 定义多媒体对象
    private int musicNum; // 当前播放的歌曲在List中的下标
    private String musicName; // 当前播放的歌曲名

    MusicServiceReceiver musicServiceReceiver; // 此类中的广播接收器

    String status = "stop"; // 多媒体对象初始状态

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化BroadcastReceiver对象，注册BroadcastReceiver
        musicServiceReceiver = new MusicServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MainActivity.CTRL_ACTION); // 控制动作
        filter.addAction(MainActivity.SEEKBAR_PROGRESS_ACTION); // 进度条动作
        filter.addAction(MainActivity.CLICKITEM_ACTION); // 点击ListView的item
        registerReceiver(musicServiceReceiver, filter);

        musicList = new MusicScanner().getMusicList(); // 获得mp3文件路径
        player = new MediaPlayer(); // 初始化多媒体对象
        musicNum = 0; // 歌曲下标默认为0

        // 定时发送进度条信息
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (player != null && player.isPlaying()) {
                    sendCurrentPosition();
                }
            }
        }, 0, 30);
    }

    // 创建广播子类接收消息
    public class MusicServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case MainActivity.CTRL_ACTION:
                    receiveCtrl(intent);
                    break;
                case MainActivity.SEEKBAR_PROGRESS_ACTION:
                    setProgress(intent);
                    break;
                case MainActivity.CLICKITEM_ACTION:
                    musicNum = intent.getIntExtra("musicNum", -1); // 获取ListView点击的Item的位置
                    play();
                    sendUpdateIntent();
                    break;
            }
        }

        public void receiveCtrl(Intent intent) {
            int control = intent.getIntExtra("control", -1);
            switch (control) { // 控制状态：播放、暂停、停止、上一曲、下一曲
                case 1:
                    if (status.equals("stop")) { // 原来处于没有播放状态,准备并播放音乐
                        play();
                    } else if (status.equals("playing")) { // 原来处于播放状态，转为暂停
                        pause();
                    } else if (status.equals("pause")) { // 原来处于暂停状态
                        goPlay();
                    }
                    break;
                case 2:
                    stop(); // 停止播放
                    break;
                case 3:
                    last(); // 上一曲
                    break;
                case 4:
                    next(); // 下一曲
                    break;
            }
            sendUpdateIntent();
        }

        public void setProgress(Intent intent) {
            if (!status.equals("stop")) {
                int progress = intent.getIntExtra("Progress", -1);
                int seekBarMax = intent.getIntExtra("SeekBarMax", -1);
                int musicMax = player.getDuration();
                player.seekTo(musicMax * progress / seekBarMax);
                goPlay(); // 拖动进度条后会自动播放
                sendUpdateIntent(); // 自动播放后状态改变，发送当前播放信息、状态信息
                sendCurrentPosition(); // 发送当前进度
            }
        }
    }

    public void sendUpdateIntent() {
        // 广播通知MainActivity音乐信息
        Intent updateIntent = new Intent(MainActivity.UPDATE_ACTION); // 指定更新动作
        int musicTotalTime = 0; // 歌曲时长默认为0
        updateIntent.putExtra("newStatus", status); // 发送新状态
        updateIntent.putExtra("musicName", musicName); // 发送音乐文件名
        if(!status.equals("stop")){ // 当前播放器不为停止状态，获取当前音乐总时长
            musicTotalTime = player.getDuration();
        }
        updateIntent.putExtra("musicTotalTime", musicTotalTime); // 发送歌曲时长，时间为ms
        // 发送广播，将被MainActivity组件中的BroadcastReceiver接收
        sendBroadcast(updateIntent);
    }

    public void sendCurrentPosition() {
        // 广播通知MainActivity当前进度
        Intent positionIntent = new Intent(MainActivity.CURRENT_POSITION_ACTION);
        positionIntent.putExtra("currentPosition", getCurrentProgress());
        sendBroadcast(positionIntent);
    }

    public void setPlayName(String dataSource) {
        File file = new File(dataSource); // 获取文件
        String name = file.getName(); // 获取文件名
        int index = name.lastIndexOf("."); // 找到最后一个'.'
        musicName = name.substring(0, index); // 截取后缀名前面的字符串做歌名
    }

    // Service实现暂停、播放、停止、上一曲、下一曲
    public void play() {
        try {
            player.reset(); // 重置多媒体
            String dataSource = musicList.get(musicNum); // 得到当前播放音乐的路径
            setPlayName(dataSource); // 截取歌名
            // 指定参数为音频文件
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setDataSource(dataSource);// 为多媒体对象设置播放路径
            player.prepare(); // 准备播放
            player.start(); // 开始播放
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer arg0) {
                    next(); // 如果当前歌曲播放完毕,自动播放下一首
                    sendUpdateIntent(); // 广播通知Activity
                }
            });
            status = "playing"; // 状态改为正在播放
        } catch (Exception e) {
            Log.i("MusicService", e.getMessage());
        }
    }

    public void goPlay() { //继续播放
        int position = getCurrentProgress();
        player.seekTo(position); // 设置当前MediaPlayer的播放位置，单位是毫秒。
        try {
            player.prepare(); // 同步的方式装载流媒体文件。
        } catch (Exception e) {
            e.printStackTrace();
        }
        player.start();
        status = "playing"; // 状态改为正在播放
    }

    public int getCurrentProgress() { // 获取当前进度
        if (status.equals("playing")) { // 当前正在播放歌曲，返回当前进度
            return player.getCurrentPosition();
        } else if (status.equals("pause")) { // 当前歌曲处于暂停状态，返回当前进度
            return player.getCurrentPosition();
        }
        //无播放，进度为0
        return 0;
    }

    public void last() { //上一曲，歌曲下标前移一位，当下标等于0则把下标移至倒数第一首歌曲
        musicNum = musicNum == 0 ? musicList.size() - 1 : musicNum - 1;
        play();
    }

    public void next() { //下一曲，歌曲下标后移一位，当下标等于最后一首歌曲的下标则把下标移至第一首歌曲
        musicNum = musicNum == musicList.size() - 1 ? 0 : musicNum + 1;
        play();
    }

    public void pause() { // 暂停播放
        if (!status.equals("pause")) {
            player.pause();
            status = "pause"; // 状态改为暂停
        }
    }

    public void stop() { // 停止播放
        if (!status.equals("stop")) {
            player.stop(); // 停止播放
            player.reset(); // 重置多媒体对象
            musicNum = 0; // 重置歌曲下标
            status = "stop"; // 状态设置为无播放状态
        }
    }
}