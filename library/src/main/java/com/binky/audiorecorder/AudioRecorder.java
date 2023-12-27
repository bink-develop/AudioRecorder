package com.binky.audiorecorder;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Handler;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import com.binky.audiorecorder.file.AudioFileHelper;
import com.binky.audiorecorder.file.AudioFileListener;
import com.binky.audiorecorder.record.Recorder;
import com.binky.audiorecorder.record.RecorderCallback;
import com.binky.audiorecorder.utils.BytesTransUtil;
import com.binky.audiorecorder.utils.Log;

/**
 * 录音功能的核心类
 */

public class AudioRecorder implements RecorderCallback, AudioFileListener {
    private static final String TAG = "IdealRecorder";
    private Context mContext;
    private Handler mRecorderHandler;
    private RecordConfig mRecordConfig;

    private AudioFileHelper mAudioFileHelper;
    private boolean mIsAudioFileHelperInit;
    private Recorder mRecorder;
    private StatusListener mStatusListener;
    private long mMaxRecordTime = 6000L;
    private long mVolumeInterval = 200L;
    private int mCount;
    private ByteArrayOutputStream mByteArrayOutputStream = new ByteArrayOutputStream();
    private AtomicBoolean mIsStarted = new AtomicBoolean(false);

    private AudioRecorder() {
        mRecorderHandler = new Handler();
        mRecorder = new Recorder(mRecordConfig, this);
        mAudioFileHelper = new AudioFileHelper(this);
    }

    public static AudioRecorder getInstance() {
        return IdealRecorderHolder.instance;
    }

    /**
     * 初始化当前实例
     *
     * @param context 当前应用的application context
     */
    public void init(Context context) {
        if (null == context) {
            Log.e(TAG, "context can not be null");
            return;
        }
        this.mContext = context.getApplicationContext();
    }

    /**
     * 获取当前应用的context
     *
     * @return 当前应用的context
     */
    public Context getContext() {
        if (mContext== null) {
            throw new IllegalStateException("please call IdealRecorder.getInstance.init() first");
        }
        return mContext;
    }

    public AudioRecorder setRecordConfig(RecordConfig config) {
        this.mRecordConfig = config;
        mAudioFileHelper.setRecorderConfig(config);
        mRecorder.setRecordConfig(config);
        return this;
    }

    /**
     * 设置最长语音
     *
     * @param maxRecordTimeMillis 最长录音时间 单位 毫秒
     * @return
     */
    public AudioRecorder setMaxRecordTime(long maxRecordTimeMillis) {
        this.mMaxRecordTime = maxRecordTimeMillis;
        return this;
    }


    /**
     * 设置音量回调时长 单位毫秒 必须为100毫秒的整数倍
     *
     * @param intervalMillis 音量回调间隔时长
     * @return
     */
    public AudioRecorder setVolumeInterval(long intervalMillis) {
        if (intervalMillis < 100) {
            Log.e(TAG, "Volume interval should at least 100 Millisecond .Current set will not take effect, default interval is 200ms");
            return this;
        }
        if (intervalMillis % Recorder.TIMER_INTERVAL != 0) {
            intervalMillis = intervalMillis / Recorder.TIMER_INTERVAL * Recorder.TIMER_INTERVAL;
            Log.e(TAG, "Current interval is changed to " + intervalMillis);
        }
        this.mVolumeInterval = intervalMillis;
        return this;
    }


    /**
     * 设置录音保存路径 保存格式为wav
     *
     * @param path 文件保存绝对路径
     */
    public AudioRecorder setRecordFilePath(String path) {
        if (null ==mContext){
            Log.e(TAG, "context is null");
            return this;
        }
        String targetPath = mContext.getFilesDir().getPath();
        if (!TextUtils.isEmpty(path) && mAudioFileHelper!= null) {
            if (!path.contains(targetPath)){
                Log.e(TAG, "path["+path+"] is invalid, must start with["+targetPath+"]");
                return this;
            }
            mIsAudioFileHelperInit = true;
            mAudioFileHelper.setSavePath(path);
        } else {
            mIsAudioFileHelperInit = false;
            mAudioFileHelper.setSavePath(null);
        }
        return this;
    }

    /**
     * 设置录音保存的格式是否为wav 默认保存为wav格式 true 保存为wav格式 false 文件保存问pcm格式
     *
     * @param isWav 是否为wav格式 默认为true 保存为wav格式 ;false 文件保存问pcm格式
     * @return
     */
    public AudioRecorder setWavFormat(boolean isWav) {
        mAudioFileHelper.setWav(isWav);
        return this;
    }

    /**
     * 设置录音时各种状态的监听
     *
     * @param statusListener statusListener
     * @return
     */
    public AudioRecorder setStatusListener(StatusListener statusListener) {
        this.mStatusListener = statusListener;
        return this;
    }

    /**
     * 判断是否有录音权限
     *
     * @return
     */
    public boolean isRecordAudioPermissionGranted() {
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 判断是否有读写存储权限
     *
     * @return
     */
    public boolean isWriteExternalStoragePermissionGranted() {
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 开始录音
     *
     * @return
     */
    public boolean start() {
        if (isRecordAudioPermissionGranted()) {
            Log.e(TAG, "Start failed , Because lack RECORD_AUDIO permission");
            return false;
        }
        if (mIsStarted.compareAndSet(false, true)) {
            mRecorder.start();
            Log.d(TAG, "Ideal Recorder Started");
            return true;
        } else {
            Log.e(TAG, "Start failed , Because the Ideal Recorder already started");
            return false;
        }
    }

    /**
     * 停止录音
     */
    public void stop() {
        Log.d(TAG, "Stop Ideal Recorder is called");
        if (this.mIsStarted.get()) {
            this.mIsStarted.set(false);
            this.mRecorder.immediateStop();

        } else if (this.mRecorder!= null) {
            this.mRecorder.immediateStop();
        }
    }

    /**
     * 在UI线程执行
     *
     * @param runnable 需要执行的runnable
     */
    private void runOnUi(Runnable runnable) {
        mRecorderHandler.post(runnable);
    }


    @Override
    public boolean onRecorderReady() {
        if (!isRecordAudioPermissionGranted()) {
            Log.e(TAG, "set recorder failed,because no RECORD_AUDIO permission was granted");
            onRecordedFail(AudioConst.RecorderErrorCode.RECORDER_PERMISSION_ERROR);
        }
        return isRecordAudioPermissionGranted();
    }

    @Override
    public boolean onRecorderStart() {
        if (mIsAudioFileHelperInit) {
            mAudioFileHelper.start();
        }
        mCount = 0;
        mByteArrayOutputStream.reset();
        runOnUi(new Runnable() {
            public void run() {
                if (mStatusListener!= null) {
                    mStatusListener.onStartRecording();
                }
                Log.d(TAG, "onRecorderStart");
            }
        });
        return true;
    }

    @Override
    public void onRecorded(final short[] wave) {
        mCount++;
        final byte[] bytes = BytesTransUtil.getInstance().Shorts2Bytes(wave);
        if (mIsAudioFileHelperInit) {

            mAudioFileHelper.save(bytes, 0, bytes.length);
        }
        mByteArrayOutputStream.write(bytes, 0, bytes.length);
        if (mStatusListener!= null) {
            mStatusListener.onRecordDataOnWorkerThread(wave, wave == null ? 0 : wave.length);
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                if (mStatusListener!= null) {
                    mStatusListener.onRecordData(wave, wave == null ? 0 : wave.length);
                }
            }
        });

        long recordedTime = mCount * Recorder.TIMER_INTERVAL;
        if (recordedTime >= mVolumeInterval && recordedTime % mVolumeInterval== 0) {
            onRecorderVolume(calculateVolume(wave));
        }
        if (recordedTime >= mMaxRecordTime) {
            mRecorder.stop();
            mIsStarted.set(false);
        }

    }

    private void onRecorderVolume(final int volume) {

        runOnUi(new Runnable() {
            public void run() {
                if (mStatusListener!= null) {
                    mStatusListener.onVoiceVolume(volume);
                }
            }
        });


    }

    @Override
    public void onRecordedFail(final int paramInt) {
        if (mIsAudioFileHelperInit) {

            mAudioFileHelper.cancel();
        }
        runOnUi(new Runnable() {
            public void run() {
                String errorMsg = "";
                switch (paramInt) {
                    case AudioConst.RecorderErrorCode.RECORDER_EXCEPTION_OCCUR:
                        errorMsg = "启动或录音时抛出异常Exception";
                        break;
                    case AudioConst.RecorderErrorCode.RECORDER_READ_ERROR:
                        errorMsg = "Recorder.read() 过程中发生错误";
                        break;
                    case AudioConst.RecorderErrorCode.RECORDER_PERMISSION_ERROR:
                        errorMsg = "当前应用没有录音权限或者录音功能被占用";
                        break;
                    default:
                        errorMsg = "未知错误";
                }
                if (mStatusListener!= null) {
                    mStatusListener.onRecordError(paramInt, errorMsg);
                }
            }
        });
    }

    @Override
    public void onRecorderStop() {
        if (mIsAudioFileHelperInit) {
            mAudioFileHelper.finish();
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                if (mStatusListener!= null) {
                    mStatusListener.onRecordedAllData(mByteArrayOutputStream.toByteArray());
                    mStatusListener.onStopRecording();
                }
            }
        });
//        byteArrayOutputStream.reset();
    }

    private int calculateVolume(short[] wave) {
        long v = 0;
        // 将 buffer 内容取出，进行平方和运算
        for (int i = 0; i < wave.length; i++) {
            v += wave[i] * wave[i];
        }
        // 平方和除以数据总长度，得到音量大小。
        double mean = v / (double) wave.length;
        double volume = 10 * Math.log10(mean);
        return (int) volume;
    }


    /**
     * 保存文件失败
     */
    @Override
    public void onFailure(final String reason) {

        Log.d(TAG, "save record file failure, this reason is " + reason);

        runOnUi(new Runnable() {
            public void run() {
                if (mStatusListener!= null) {
                    mStatusListener.onFileSaveFailed(reason);
                }
            }
        });
    }

    /**
     * 保存文件成功
     */
    @Override
    public void onSuccess(final String savePath) {
        Log.d(TAG, "save record file success, the file path is" + savePath);
        runOnUi(new Runnable() {
            public void run() {
                if (mStatusListener!= null) {
                    mStatusListener.onFileSaveSuccess(savePath);
                }
            }
        });

    }

    /**
     * 录音的配置信息  默认配置为16K采样率 单通道 16位
     * <pre>
     *      audioSource = MediaRecorder.AudioSource.MIC;
     *      sampleRate = SAMPLE_RATE_16K_HZ;
     *      channelConfig = AudioFormat.CHANNEL_IN_MONO;
     *      audioFormat = AudioFormat.ENCODING_PCM_16BIT;
     * </pre>
     */
    public static class RecordConfig {
        public static final int SAMPLE_RATE_44K_HZ = 44100;
        public static final int SAMPLE_RATE_22K_HZ = 22050;
        public static final int SAMPLE_RATE_16K_HZ = 16000;
        public static final int SAMPLE_RATE_11K_HZ = 11025;
        public static final int SAMPLE_RATE_8K_HZ = 8000;
        private int audioSource = MediaRecorder.AudioSource.MIC;
        private int sampleRate = SAMPLE_RATE_16K_HZ;
        private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

        /**
         * 录音配置的构造方法
         *
         * @param audioSource   the recording source.
         *                      See {@link MediaRecorder.AudioSource} for the recording source definitions.
         *                      recommend {@link MediaRecorder.AudioSource#MIC}
         * @param sampleRate    the sample rate expressed in Hertz. {@link RecordConfig#SAMPLE_RATE_44K_HZ} is Recommended ,
         *                      {@link RecordConfig#SAMPLE_RATE_22K_HZ},{@link RecordConfig#SAMPLE_RATE_16K_HZ},
         *                      {@link RecordConfig#SAMPLE_RATE_11K_HZ},{@link RecordConfig#SAMPLE_RATE_8K_HZ}
         * @param channelConfig describes the configuration of the audio channels.
         *                      See {@link AudioFormat#CHANNEL_IN_MONO} and
         *                      {@link AudioFormat#CHANNEL_IN_STEREO}.  {@link AudioFormat#CHANNEL_IN_MONO} is guaranteed
         *                      to work on all devices.
         * @param audioFormat   the format in which the audio data is to be returned.
         *                      See {@link AudioFormat#ENCODING_PCM_8BIT}, {@link AudioFormat#ENCODING_PCM_16BIT},
         *                      and {@link AudioFormat#ENCODING_PCM_FLOAT}.
         */
        public RecordConfig(int audioSource, int sampleRate, int channelConfig, int audioFormat) {
            this.audioSource = audioSource;
            this.sampleRate = sampleRate;
            this.channelConfig = channelConfig;
            this.audioFormat = audioFormat;
        }

        /**
         * 录音配置的构造方法
         */
        public RecordConfig() {

        }

        public int getAudioSource() {
            return audioSource;
        }

        /**
         * @param audioSource the recording source.
         *                    See {@link MediaRecorder.AudioSource} for the recording source definitions.
         *                    recommend {@link MediaRecorder.AudioSource#MIC}
         */
        public RecordConfig setAudioSource(int audioSource) {
            this.audioSource = audioSource;
            return this;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        /**
         * @param sampleRate the sample rate expressed in Hertz. {@link RecordConfig#SAMPLE_RATE_44K_HZ} is Recommended ,
         * @link RecordConfig#SAMPLE_RATE_22K_HZ},{@link RecordConfig#SAMPLE_RATE_16K_HZ},{@link RecordConfig#SAMPLE_RATE_11K_HZ},{@link RecordConfig#SAMPLE_RATE_8K_HZ}
         * which is usually the sample rate of the source.
         */
        public RecordConfig setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        public int getChannelConfig() {
            return channelConfig;
        }

        /**
         * @param channelConfig describes the configuration of the audio channels.
         *                      See {@link AudioFormat#CHANNEL_IN_MONO} and
         *                      {@link AudioFormat#CHANNEL_IN_STEREO}.  {@link AudioFormat#CHANNEL_IN_MONO} is guaranteed
         *                      to work on all devices.
         */
        public RecordConfig setChannelConfig(int channelConfig) {
            this.channelConfig = channelConfig;
            return this;
        }

        public int getAudioFormat() {
            return audioFormat;
        }

        /**
         * @param audioFormat the format in which the audio data is to be returned.
         *                    See {@link AudioFormat#ENCODING_PCM_8BIT}, {@link AudioFormat#ENCODING_PCM_16BIT},
         *                    and {@link AudioFormat#ENCODING_PCM_FLOAT}.
         */
        public RecordConfig setAudioFormat(int audioFormat) {
            this.audioFormat = audioFormat;
            return this;
        }


    }


    /**
     * idealRecorder的holder 用来初始化
     */
    private static class IdealRecorderHolder {
        private final static AudioRecorder instance = new AudioRecorder();
    }
}
