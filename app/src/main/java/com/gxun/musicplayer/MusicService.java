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

public class MusicService extends Service {

    private List<String> musicList;// 存放找到的所有mp3的绝对路径
    private MediaPlayer player; // 定义多媒体对象
    private int musicNum; // 当前播放的歌曲在List中的下标
    private String musicName; // 当前播放的歌曲名

    MusicServiceReceiver musicServiceReceiver; // 此类中的广播接收器

    int status = 0x11; // 多媒体对象初始状态

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化BroadcastReceiver对象，注册BroadcastReceiver
        musicServiceReceiver = new MusicServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MainActivity.CTRL_ACTION); // 控制动作
        registerReceiver(musicServiceReceiver, filter);

        musicList = new MusicScanner().getMusicList(); // 获得mp3文件路径
        player = new MediaPlayer(); // 初始化多媒体对象
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // 创建广播子类接收消息
    public class MusicServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int control = intent.getIntExtra("control",-1);
            switch (control){ // 控制状态：播放、暂停、停止、上一曲、下一曲
                case 1:
                    if(status == 0x11){ // 原来处于没有播放状态,准备并播放音乐
                        play();
                        status = 0x12; // 状态改为正在播放
                    } else if(status == 0x12){ // 原来处于播放状态，转为暂停
                        pause();
                        status = 0x13; // 状态改为暂停
                    } else if(status == 0x13){ // 原来处于暂停状态
                        goPlay();
                        status = 0x12; // 状态改为正在播放
                    }
                    break;
                case 2:
                    stop(); // 停止播放
                    status = 0x11; // 状态设置为无播放状态
                    break;
                case 3:
                    last(); // 上一曲
                    status = 0x12; // 状态改为正在播放
                    break;
                case 4:
                    next(); // 下一曲
                    status = 0x12; // 状态改为正在播放
                    break;
            }
            // 广播通知Activity更改图标、文本框
            Intent updateIntent = new Intent(MainActivity.UPDATE_ACTION); // 指定更新动作
            updateIntent.putExtra("newStatus",status); // 发送新状态
            updateIntent.putExtra("musicName", musicName); // 发送音乐文件名
            updateIntent.putExtra("maxTime", player.getDuration()); // 该首歌曲最长秒数
            // 发送广播，将被MainActivity组件中的BroadcastReceiver接收
            sendBroadcast(updateIntent);
        }
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
            String dataSource = musicList.get(musicNum);// 得到当前播放音乐的路径
            setPlayName(dataSource); // 截取歌名
            // 指定参数为音频文件
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setDataSource(dataSource);// 为多媒体对象设置播放路径
            player.prepare(); // 准备播放
            player.start(); // 开始播放
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer arg0) {
                    next(); // 如果当前歌曲播放完毕,自动播放下一首
                }
            });
        } catch (Exception e) {
            Log.i("MusicService", e.getMessage());
        }
    }

    public void goPlay(){ //继续播放
        int position = getCurrentProgress();
        player.seekTo(position); // 设置当前MediaPlayer的播放位置，单位是毫秒。
        try {
            player.prepare(); // 同步的方式装载流媒体文件。
        } catch (Exception e) {
            e.printStackTrace();
        }
        player.start();
    }

    public int getCurrentProgress() { // 获取当前进度
        if (player != null & player.isPlaying()) { //当前正在播放歌曲，返回当前进度
            return player.getCurrentPosition();
        } else if (player != null & (!player.isPlaying())) { //当前歌曲处于暂停状态，返回当前进度
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
        if (player != null && player.isPlaying()){
            player.pause();
        }
    }

    public void stop() { //停止播放
        if (player != null && player.isPlaying()) {
            player.stop(); //停止播放
            musicNum = 0; //重置歌曲下标
            player.reset(); //重置多媒体对象
        }
    }
}