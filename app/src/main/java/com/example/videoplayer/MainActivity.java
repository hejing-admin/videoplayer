package com.example.videoplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;
import android.widget.VideoView;

import com.example.videoplayer.Utils.PixelUtil;

import java.io.File;
import java.net.URI;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // 播放view
    private VideoView videoView;

    // 整个播放页面(包括播放view 和 操作栏)
    private RelativeLayout videoLayout;

    // 播放view下面的操作栏布局
    private LinearLayout controllerLayout;

    // 播放/暂停 控制按钮
    private ImageView play_controller_iv;

    // 当前时间
    private TextView current_time_tv;

    // 总时间
    private TextView total_time_tv;

    // 控制播放内容的进度条
    private SeekBar play_seek;

    // 控制音量的进度条
    private SeekBar volume_seek;

    // 半屏和全屏的切换imageview
    private ImageView screen_iv;

    // UIHandler的标识
    public static final int UPDATE_UI = 1;

    // 当前屏幕的宽
    private int screen_width;

    // 当前屏幕的高
    private int screen_height;

    // 音频管理器
    private AudioManager audioManager;
    private ImageView volume_iv;

    // 默认为半屏状态
    private boolean isFullScreen = false;

    // 设定触摸 默认为不合法
    private boolean isAdjust = false;

    private int threshold = 54;  // 防误触阈值

    private float lastX = 0, lastY = 0;

    private float brightness;  // 亮度

    private ImageView operation_bg;

    private ImageView operation_percent;
    private FrameLayout progress_frameLayout;


    private ImageView video_start_iv;
    private EditText video_website_or_local_tv;

    private String path;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 音频管理器获取系统的音频服务
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // 初始化UI布局
        initUI();

        // 设置播放事件
        setPlayEvent();

        // start播放点击监听
        video_start_iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initVideoResource();
            }
        });
    }

    /**
     * 初始化视频源
     */
    private void initVideoResource() {
        String path_local_name = video_website_or_local_tv.getText().toString().trim();
        if (!TextUtils.isEmpty(path_local_name)) {
            // 判断输入的是本地视频名称还是网络视频地址
            if (path_local_name.startsWith("http")) {
                // 输入内容是网络视频地址
                Log.d("ting", "进来了!!!");
                path = path_local_name;
            } else {
                // 输入内容是本地视频名称
                // 路径1: /storage/emulated/0/Android/data/com.example.videoplayer/files/视频文件名
                String temp1 = getExternalFilesDir("").toString() + "/" + path_local_name;

                if (new File(temp1).exists()) {
                    path = temp1;
                } else {
                    Toast.makeText(this, "视频不存在", Toast.LENGTH_SHORT).show();
                }
            }
            if (path != null) {
                videoView.setVideoPath(path);

                // 默认为开始播放
                videoView.start();
                // 开启UIHandler的同步刷新
                UIHandler.sendEmptyMessage(UPDATE_UI);
            }

        } else {
            // 没有输入，提示用户需要输入视频源
            Toast.makeText(this, "你需要输入一个视频源", Toast.LENGTH_SHORT).show();
            // 播放界面提示无视频可以播放
        }

    }

    /**
     * 时间字符串格式化函数
     * 功能:
     *      传入显示控件，传入时间，进行显示
     * @param textView
     * @param millisecond
     */
    private void updateTextViewWithTimeFormat(TextView textView, int millisecond)  {
        int all_seconds = millisecond / 1000;
        // 时
        int hours = all_seconds / 3600;
        // 分
        int minutes = all_seconds % 3600 / 60;
        // 秒
        int seconds = all_seconds % 60;

        String str = null;

        if (hours != 0) {
            str = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            str = String.format("%02d:%02d", minutes, seconds);
        }
        textView.setText(str);
    }

    // 在视频播放时执行 UI刷新
    private Handler UIHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == UPDATE_UI) {  // 设置刷新标识
                // 刷新UI

                // 获取到当前视频的播放时间
                int currentPosition = videoView.getCurrentPosition();
                // 获取到当前视频播放的总时间
                int totalDuration = videoView.getDuration();

                // 格式化视频播放时间
                updateTextViewWithTimeFormat(current_time_tv, currentPosition);
                updateTextViewWithTimeFormat(total_time_tv, totalDuration);

                // 格式化播放进度
                // 指定视频播放总进度
                play_seek.setMax(totalDuration);
                // 设置当前播放进度
                play_seek.setProgress(currentPosition);

                // 完成自刷新的效果 (???)
                UIHandler.sendEmptyMessageDelayed(UPDATE_UI, 500);
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        // 停止UIHandler的继续刷新
        UIHandler.removeMessages(UPDATE_UI);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setPlayEvent() {
        // 播放/暂停点击事件
        play_controller_iv.setOnClickListener(this);

        // 切换横竖屏的点击事件
        screen_iv.setOnClickListener(this);

        // 内容进度条拖动事件
        play_seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 实时刷新播放时间变化情况
                updateTextViewWithTimeFormat(current_time_tv, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 开始拖动时，需要停止UIHandler的刷新
                UIHandler.removeMessages(UPDATE_UI);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 停止拖动时，我们能拿到当前的进度
                int progress = seekBar.getProgress();
                // 令视频的播放进度遵循seekBar停止拖动时的这一刻的进度
                videoView.seekTo(progress);
                // 重新进行UIHandler的刷新
                UIHandler.sendEmptyMessage(UPDATE_UI);
            }
        });

        // 音量进度条拖动事件
        volume_seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                /**
                 * 设置当前设备的音量
                 * 参数1: 类型
                 * 参数2: 具体音量
                 * 参数3: 标记
                 */
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        /**
         * 控制VideoView的手势事件 （控制音量和亮度）
         */
        videoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // 获取触摸时当前的x轴和y轴的滑动距离
                float x = motionEvent.getX();
                float y = motionEvent.getY();
                switch (motionEvent.getAction()) {
                    /**
                     * 手指落下屏幕的那一刻(只会调用一次)
                     */
                    case MotionEvent.ACTION_DOWN:
                        // 储存移动之前的x值、y值
                        lastX = x;
                        lastY = y;
                        break;
                    /**
                     * 手指在屏幕上移动(会调用多次)
                     */
                    case MotionEvent.ACTION_MOVE:
                        float delatX = x - lastX;
                        float delatY = y - lastY;
                        float absDelatX = Math.abs(delatX);
                        float absDelatY = Math.abs(delatY);
                        /**
                         * 手势合法性的验证
                         */
                        if (absDelatX > threshold && absDelatY > threshold) {  // 斜着滑动
                            if (absDelatX < absDelatY) {
                                // y轴变化量更大，以纵向为主
                                isAdjust = true;
                            } else {
                                isAdjust = false;
                            }
                        } else if (absDelatX < threshold && absDelatY > threshold) {
                            isAdjust = true;
                        } else if (absDelatX > threshold && absDelatY < threshold) {
                            isAdjust = false;
                        }

                        // 如果手势合法的话:
                        if (isAdjust) {
                            /**
                             * 在判断当前手势已经合法的前提下，区分此时手势应该调节亮度还是声音
                             * 左半屏调节亮度，右半屏调节声音
                             */
                            if (x < screen_width / 2) {
                                /**
                                 * 左半屏调节亮度
                                 */
                                if (delatY > 0) {
                                    /**
                                     * 左屏向下滑动降低亮度
                                     */
                                } else {
                                    /**
                                     * 左屏向上滑动升高亮度
                                     */
                                }
                                changeBrightness(-delatY);
                            } else {
                                /**
                                 * 右半屏调节声音
                                 */
                                if (delatY > 0) {
                                    /**
                                     * 右屏向下滑动减少声音
                                     */
                                    Log.i("ting", "减少声音: " + delatY);

                                } else {
                                    /**
                                     * 右屏向上滑动增大声音
                                     */
                                    Log.i("ting", "增大声音: " + (-delatY));
                                }
                                changeVolume(-delatY);
                            }
                        } else {
                            Log.i("ting", "没进去, delatX = " + delatX + "  delatY=" + delatY);
                        }
                        lastX = x;
                        lastY = y;

                        break;
                    /**
                     * 手指离开屏幕的那一刻(只会调用一次)
                     */
                    case MotionEvent.ACTION_UP:
                        progress_frameLayout.setVisibility(View.GONE);
                        break;
                }
                return true;
            }
        });

        /**
         * 设置循环播放事件
         */
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
                mediaPlayer.setLooping(true);
            }
        });

        /**
         * videoView的页面点击事件
         * setOnClickListener不好用，不推荐。
         */
        /*videoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (videoView.isPlaying()) {
                    // 如果视频正在播放，点击之后暂停播放
                    Log.i("ting", "执行暂停操作");
                    videoView.pause();
                } else {
                    // 如果视频是暂停播放状态，点击之后恢复播放
                    Log.i("ting", "执行播放操作");
                    videoView.resume();
                }
            }
        });*/
    }

    /**
     * 改变音量
     */
    private void changeVolume(float delatY) {
        // 获取最大声音
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        // 获取当前声音
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int index = (int) (delatY / screen_height * max * 3);  // 音量偏移
        // 音量设置目标值
        int volume = Math.max(current + index, 0);
        // 设置音量
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);

        if (progress_frameLayout.getVisibility() == View.GONE) {
            progress_frameLayout.setVisibility(View.VISIBLE);
        }

        operation_bg.setImageResource(R.mipmap.volume_white);
        ViewGroup.LayoutParams layoutParams = operation_percent.getLayoutParams();
        layoutParams.width = (int) (PixelUtil.dp2px(94) * (float)volume / max);
        operation_percent.setLayoutParams(layoutParams);

        // 更新音量进度条
        volume_seek.setProgress(volume);
    }

    /**
     * 调节亮度
     */
    private void changeBrightness(float delatY) {
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        brightness = attributes.screenBrightness;

        float index = delatY / screen_height / 3;  // 亮度偏移量
        brightness += index;
        // 临界值判断
        if (brightness > 1.0f) {
            brightness = 1.0f;
        }
        if (brightness < 0.01f) {
            brightness = 0.01f;
        }
        if (progress_frameLayout.getVisibility() == View.GONE) {
            progress_frameLayout.setVisibility(View.VISIBLE);
        }
        attributes.screenBrightness = brightness;

        operation_bg.setImageResource(R.mipmap.brightness_white);
        ViewGroup.LayoutParams layoutParams = operation_percent.getLayoutParams();
        layoutParams.width = (int) (PixelUtil.dp2px(94)*brightness);
        operation_percent.setLayoutParams(layoutParams);

        getWindow().setAttributes(attributes);
    }

    /**
     * 初始化UI布局
     */
    private void initUI() {
        // 初始化工具类的上下文
        PixelUtil.initContext(this);

        videoLayout = findViewById(R.id.videoLayout);
        videoView = findViewById(R.id.videoView);
        controllerLayout = findViewById(R.id.controllerbar_layout);
        play_controller_iv = findViewById(R.id.pause_iv);
        current_time_tv = findViewById(R.id.time_current_tv);
        total_time_tv = findViewById(R.id.time_total_tv);
        play_seek = findViewById(R.id.play_seek);
        volume_seek = findViewById(R.id.volume_seek);
        screen_iv = findViewById(R.id.screen_iv);
        volume_iv = findViewById(R.id.volume_iv);

        operation_bg = findViewById(R.id.operation_bg);
        operation_percent = findViewById(R.id.operation_percent);

        progress_frameLayout = findViewById(R.id.frameLayout);

        // 输入网址
        video_website_or_local_tv = findViewById(R.id.video_website_or_local_tv);
        video_start_iv = findViewById(R.id.video_start_iv);


        // 获取当前屏幕的宽和高
        screen_width = getResources().getDisplayMetrics().widthPixels;
        screen_height = getResources().getDisplayMetrics().heightPixels;

        // 设置音量进度条的当前刻度和最大刻度
        /**
         * 获取设备的最大音量
         */
        int streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        /**
         * 获取设备的当前音量
         */
        int streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        volume_seek.setMax(streamMaxVolume);
        volume_seek.setProgress(streamVolume);


    }

    /**
     * 设置VideoView的宽和高
     */
    private void setVideoViewScale(int width, int height) {
        // 给VideoView设置参数
        ViewGroup.LayoutParams layoutParams = videoView.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        videoView.setLayoutParams(layoutParams);

        // 给VideoLayout设置参数
        ViewGroup.LayoutParams layoutParams1 = videoLayout.getLayoutParams();
        layoutParams1.width = width;
        layoutParams1.height = height;
        videoLayout.setLayoutParams(layoutParams1);

    }

    /**
     * 监听屏幕方向的改变
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        /**
         * 当屏幕方向为横屏时
         */
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 将整个屏幕进行拉伸
            setVideoViewScale(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            // 横屏状态下进行显示音量控制条
            volume_iv.setVisibility(View.VISIBLE);
            volume_seek.setVisibility(View.VISIBLE);
            isFullScreen = true;

            // 移除掉半屏状态
            // getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            // 设置全屏
            // getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        /**
         * 当屏幕方向为竖屏的时候
         */
        else {
            setVideoViewScale(ViewGroup.LayoutParams.MATCH_PARENT, PixelUtil.dp2px(275));
            // 竖屏状态下隐藏音量控制条
            volume_seek.setVisibility(View.GONE);
            volume_iv.setVisibility(View.GONE);
            isFullScreen = false;

            // 移除掉全屏状态
            // getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            // 设置半屏
            // getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.pause_iv) {  // 播放/暂停 按钮监听
            // 控制视频的播放和暂停
            if (videoView.isPlaying()) {
                // 如果正在播放，点击此按钮之后(视频变为暂停状态)，图片需设置为播放(提醒用户点击就播放)
                play_controller_iv.setImageResource(R.drawable.play_btn_style);
                // 状态变成 暂停播放
                videoView.pause();
                // 视频播放暂停之后需要停止UI刷新。
                UIHandler.removeMessages(UPDATE_UI);

            } else {
                // 如果是暂停状态，点击此按钮之后(视频变为播放状态)，图片需设置为暂停(提醒用户点击就暂停)
                play_controller_iv.setImageResource(R.drawable.pause_btn_style);
                // 状态变为 继续播放
                videoView.start();
                // UIHandler恢复刷新
                UIHandler.sendEmptyMessage(UPDATE_UI);
            }
        } else if (view.getId() == R.id.screen_iv) {  // 横竖屏切换按钮
            if (isFullScreen) {
                // 如果当前是全屏，点击之后切换为半屏
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            } else {
                // 如果当前是半屏，点击之后切换为全屏
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
    }
}




























