package com.binky.audiorecorder.file;

import android.media.AudioFormat;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.binky.audiorecorder.AudioRecorder;
import com.binky.audiorecorder.utils.Log;


/**
 * 语音文件帮助类 用户保存相关文件
 */

public class AudioFileHelper {

    public static final String TAG = "AudioFileHelper";
    private AudioFileListener mAudioFileListener;
    private String mSavePath;
    private RandomAccessFile mRandomAccessFile;
    private File mTargetFile;
    private AudioRecorder.RecordConfig mRecordConfig;
    private boolean mIsWav = true;

    public AudioFileHelper(AudioFileListener listener) {
        this.mAudioFileListener = listener;
    }

    public void setSavePath(String savePath) {
        this.mSavePath = savePath;
    }

    public void setRecorderConfig(AudioRecorder.RecordConfig config) {
        this.mRecordConfig = config;
    }

    public void setWav(boolean wav) {
        this.mIsWav = wav;
    }

    public void start() {
        try {
            open(mSavePath);
        } catch (IOException e) {
            e.printStackTrace();
            if (mAudioFileListener!= null) {
                mAudioFileListener.onFailure(e.toString());
            }
        }
    }

    public void save(byte[] data, int offset, int size) {
        if (null == mRandomAccessFile) {
            return;
        }
        try {
            write(mRandomAccessFile, data, offset, size);
        } catch (IOException e) {
            e.printStackTrace();
            if (mAudioFileListener!= null) {
                mAudioFileListener.onFailure(e.toString());
            }

        }
    }

    public void finish() {
        try {
            close();

        } catch (IOException e) {
            e.printStackTrace();
            if (mAudioFileListener!= null) {
                mAudioFileListener.onFailure(e.toString());
            }

        }
    }


    private void open(String path) throws IOException {
        if (TextUtils.isEmpty(path)) {
            Log.d(TAG, "Path not set , data will not save");
            return;
        }
        if (null == this.mRecordConfig) {
            Log.d(TAG, "RecordConfig not set , data will not save");
            return;
        }
        mTargetFile = new File(path);

        if (mTargetFile.exists()) {
            mTargetFile.delete();
        } else {
            File parentDir = mTargetFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
        }
        short bSamples;
        short nChannels;
        int sRate;
        if (mRecordConfig.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT) {
            bSamples = 16;
        } else {
            bSamples = 8;
        }

        if (mRecordConfig.getChannelConfig() == AudioFormat.CHANNEL_IN_MONO) {
            nChannels = 1;
        } else {
            nChannels = 2;
        }
        sRate = mRecordConfig.getSampleRate();
        mRandomAccessFile = new RandomAccessFile(mTargetFile, "rw");
        mRandomAccessFile.setLength(0);
        if (mIsWav) {
            // Set file length to
            // 0, to prevent unexpected behavior in case the file already existed
            // 16K、16bit、单声道
            /* RIFF header */
            // riff id
            mRandomAccessFile.writeBytes("RIFF");
            // riff chunk size *PLACEHOLDER*
            mRandomAccessFile.writeInt(0);
            // wave type
            mRandomAccessFile.writeBytes("WAVE");

            /* fmt chunk */
            // fmt id
            mRandomAccessFile.writeBytes("fmt ");
            // fmt chunk size
            mRandomAccessFile.writeInt(Integer.reverseBytes(16));
            // AudioFormat,1 for PCM
            mRandomAccessFile.writeShort(Short.reverseBytes((short) 1));
            // Number of channels, 1 for mono, 2 for stereo
            mRandomAccessFile.writeShort(Short.reverseBytes(nChannels));
            // Sample rate
            mRandomAccessFile.writeInt(Integer.reverseBytes(sRate));
            // Byte rate,SampleRate*NumberOfChannels*BitsPerSample/8
            mRandomAccessFile.writeInt(Integer.reverseBytes(sRate * bSamples * nChannels / 8));
            // Block align, NumberOfChannels*BitsPerSample/8
            mRandomAccessFile.writeShort(Short.reverseBytes((short) (nChannels * bSamples / 8)));
            // Bits per sample
            mRandomAccessFile.writeShort(Short.reverseBytes(bSamples));

            /* data chunk */
            // data id
            mRandomAccessFile.writeBytes("data");
            // data chunk size *PLACEHOLDER*
            mRandomAccessFile.writeInt(0);
        }
        Log.d(TAG, "saved file path: " + path);

    }

    private void write(RandomAccessFile file, byte[] data, int offset, int size) throws IOException {
        file.write(data, offset, size);
    }

    private void close() throws IOException {
        try {
            if (mRandomAccessFile== null) {
                if (mAudioFileListener!= null) {
                    mAudioFileListener.onFailure("File save error exception occurs");
                }
                return;
            }
            if (mIsWav) {
                mRandomAccessFile.seek(4); // riff chunk size
                mRandomAccessFile.writeInt(Integer.reverseBytes((int) (mRandomAccessFile.length() - 8)));
                mRandomAccessFile.seek(40); // data chunk size
                mRandomAccessFile.writeInt(Integer.reverseBytes((int) (mRandomAccessFile.length() - 44)));
            }

            Log.d(TAG, "file size: " + mRandomAccessFile.length());
            if (mAudioFileListener!= null) {
                mAudioFileListener.onSuccess(mSavePath);
            }

        } finally {
            if (mRandomAccessFile!= null) {
                mRandomAccessFile.close();
                mRandomAccessFile = null;
            }

        }
    }

    public void cancel() {
        if (null == mRandomAccessFile || null == mTargetFile) {
            return;
        }
        if (mTargetFile.exists()) {
            mTargetFile.delete();
        }
        mRandomAccessFile = null;
        mTargetFile = null;
    }


}
