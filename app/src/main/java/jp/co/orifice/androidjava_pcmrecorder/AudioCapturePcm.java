package jp.co.orifice.androidjava_pcmrecorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioCapturePcm {
    final String TAG = "AudioCapturePcm";

    // Config....
    private final int RecordingBufferTimeLen = 3;               // sec.
    private final float RecordingSamplingRate = 48000.0f;
    private final int RingBufferSize = (int) RecordingSamplingRate * RecordingBufferTimeLen;

    private RingBuffer ringBuffer = new RingBuffer();

    private Thread threadCapture;
    private int recordBufSize;
    private AudioRecord audioRecorder;

    public AudioCapturePcm() {
        recordBufSize = AudioRecord.getMinBufferSize(
                (int)RecordingSamplingRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT) * 2;    // 1280Byte.

        audioRecorder = new AudioRecord(
//                MediaRecorder.AudioSource.REMOTE_SUBMIX,
//                MediaRecorder.AudioSource.VOICE_DOWNLINK,
                MediaRecorder.AudioSource.MIC,                  // TODO: Capture Audio OUT.
                (int)RecordingSamplingRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                recordBufSize);
    }

    public void recStart() {
        if (threadCapture != null)
            return;

        threadCapture = new Thread() {
            @Override
            public void run() {
                int samples = recordBufSize / 2;
                short[] shortData = new short[samples];
                ringBuffer.clear();
                audioRecorder.startRecording();

                while (!isInterrupted()) {
                    audioRecorder.read(shortData, 0, samples);
                    int ret = ringBuffer.push(shortData, samples);
                    if (ret < 0) {
                        Log.e(TAG, "ERROR! Rec buffer Overflow !!!!");
                        break;
                    }
                }
                audioRecorder.stop();
                Log.d(TAG, "Thread Terminated.");
            }
        };
        threadCapture.start();
    }

    public void recStop() {
        if (threadCapture == null)
            return;

        try {
            threadCapture.interrupt();
            threadCapture.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            threadCapture = null;
        }
        ringBuffer.clear();
    }

    public int popData(short[] buf, int len) {
        return ringBuffer.popBlocking(buf, 0, len, 1.0);
    }

    /*
     Ring Buffer.
    */
    private class RingBuffer {
        private short[] ringBuf = new short[RingBufferSize];
        private long pushCount = 0;
        private long popCount = 0;

        public int push(short[] sdata, int len) {
            if ( ringBuf.length < (pushCount - popCount) + len ){
                Log.e(TAG, "[RingBuffer] Overflow !!!!");
                return -1;		// Over flow!
            }

            int srcIdx = 0;
            int dstIdx = (int)(pushCount % ringBuf.length);
            int vacant = ringBuf.length - dstIdx;
            int size = len;
            if (vacant < size) {
                for (int i=0; i<vacant; i++) {
                    ringBuf[dstIdx++] = sdata[srcIdx++];
                }
                size -= vacant;
                dstIdx = 0;
            }
            for (int i=0; i<size; i++) {
                ringBuf[dstIdx++] = sdata[srcIdx++];
            }

            pushCount += len;
            return 0;
        }

        private void clear() {
            popCount = 0;
            pushCount = 0;
        }

        private int popBlocking(short[] popBuf, int popIdx, int len, double waitTime) {
            // config...
            final int waitTics = 2;         // polling time (msec).

            while (len > (pushCount - popCount)) {
                try {
                    Thread.sleep(waitTics);
                }catch(InterruptedException e){
                    return -1;
                }

                waitTime -= waitTics/1000.0;
                if (waitTime < 0) {
                    return -1;		// internal error!
                }
            }

            int sidx = (int)(popCount % ringBuf.length);
            int rsize = len;
            int tailLength = ringBuf.length - sidx;
            if (len > tailLength) {
                System.arraycopy(ringBuf, sidx, popBuf, popIdx, tailLength);
                len -= tailLength;
                popIdx  += tailLength;
                popCount += tailLength;
                sidx = (int)(pushCount % ringBuf.length);
            }
            System.arraycopy(ringBuf, sidx, popBuf, popIdx, len);
            popCount += len;

            return rsize;
        }
    }
}
