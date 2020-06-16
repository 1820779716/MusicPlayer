package com.gxun.musicplayer;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class MusicScanner {
    private static final File PATH = Environment.getExternalStorageDirectory();// 获取SD卡总目录
    private List<String> musicList; //存放找到的所有mp3的绝对路径(fileName.mp3)

    class MusicFilter implements FilenameFilter { //内部类，选择m4a、mp3为后缀的音频文件
        public boolean accept(File dir, String name) {
            return name.endsWith(".mp3"); //返回当前目录所有以.mp3结尾的文件
        }
    }

    public List<String> getMusicList() {
        musicList = new ArrayList<String>();//实例化一个List链表数组
        try {
            File MUSIC_PATH = new File(PATH, "TestMusic");//获取根目录的二级目录Music
            for (File file : MUSIC_PATH.listFiles(new MusicFilter())) {
                musicList.add(file.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.i("TAG", "读取文件异常");
        }
        return musicList;
    }
}
