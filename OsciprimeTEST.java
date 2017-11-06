package ch.serverbox.android.osciprime.audio;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import ch.serverbox.android.osciprime.sources.AudioSource;

public class AudioAdapter {
    public static final int NEW_BYTE_SAMPLES = 99;
    public static final int NEW_SHORT_SAMPLES = 100;
    private Runnable mAudioLoop;
    private Thread mAudioSamplingThread;
    private final AudioSource mAudioSource;
    private Handler mHandler;
    private final Object mLock;
    private Looper mLooper;
    private boolean mStop;
    private boolean mStopped;

    /* renamed from: ch.serverbox.android.osciprime.audio.AudioAdapter.1 */
    class C00261 implements Runnable {
        C00261() {
        }

        public void run() {
            Looper.prepare();
            synchronized (AudioAdapter.this.mLock) {
                AudioAdapter.this.mHandler = new Handler();
                AudioAdapter.this.mLooper = Looper.myLooper();
                AudioAdapter.this.mLock.notifyAll();
            }
            Looper.loop();
        }
    }

    /* renamed from: ch.serverbox.android.osciprime.audio.AudioAdapter.2 */
    class C00272 implements Runnable {
        C00272() {
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            /*
            r10 = this;
            r3 = 2;
            r1 = 1;
            r2 = ch.serverbox.android.osciprime.audio.AudioAdapter.this;
            r2 = r2.mAudioSource;
            r6 = r2.cBlocksize();
            r2 = ch.serverbox.android.osciprime.audio.AudioAdapter.this;
            r4 = new java.lang.StringBuilder;
            r5 = "Block size: ";
            r4.<init>(r5);
            r4 = r4.append(r6);
            r4 = r4.toString();
            r2.m16l(r4);
            r7 = new short[r6];
            r0 = new android.media.AudioRecord;
            r2 = 44100; // 0xac44 float:6.1797E-41 double:2.17883E-319;
            r5 = r6 * 16;
            r4 = r3;
            r0.<init>(r1, r2, r3, r4, r5);
            r1 = r0.getState();
            if (r1 != 0) goto L_0x0044;
        L_0x0033:
            r1 = ch.serverbox.android.osciprime.audio.AudioAdapter.this;
            r2 = "cannot initialzie audio";
            r1.m15e(r2);
            r1 = ch.serverbox.android.osciprime.audio.AudioAdapter.this;
            r1 = r1.mAudioSource;
            r1.unavailable();
        L_0x0043:
            return;
        L_0x0044:
            r0.startRecording();
            r1 = ch.serverbox.android.osciprime.audio.AudioAdapter.this;
            r1 = r1.mAudioSource;
            r1 = r1.cBlocksize();
            r9 = new int[r1];
        L_0x0053:
            r1 = ch.serverbox.android.osciprime.audio.AudioAdapter.this;
            r2 = r1.mLock;
            monitor-enter(r2);
            r1 = ch.serverbox.android.osciprime.audio.AudioAdapter.this;	 Catch:{ all -> 0x0076 }
            r1 = r1.mStop;	 Catch:{ all -> 0x0076 }
            if (r1 == 0) goto L_0x0079;
        L_0x0062:
            r1 = ch.serverbox.android.osciprime.audio.AudioAdapter.this;	 Catch:{ all -> 0x0076 }
            r3 = 1;
            r1.mStopped = r3;	 Catch:{ all -> 0x0076 }
            r0.stop();	 Catch:{ all -> 0x0076 }
            r1 = ch.serverbox.android.osciprime.audio.AudioAdapter.this;	 Catch:{ all -> 0x0076 }
            r1 = r1.mLock;	 Catch:{ all -> 0x0076 }
            r1.notifyAll();	 Catch:{ all -> 0x0076 }
            monitor-exit(r2);	 Catch:{ all -> 0x0076 }
            goto L_0x0043;
        L_0x0076:
            r1 = move-exception;
            monitor-exit(r2);	 Catch:{ all -> 0x0076 }
            throw r1;
        L_0x0079:
            monitor-exit(r2);	 Catch:{ all -> 0x0076 }
            r1 = 0;
            r1 = r0.read(r7, r1, r6);
            r2 = -2;
            if (r1 != r2) goto L_0x008d;
        L_0x0082:
            r1 = ch.serverbox.android.osciprime.audio.AudioAdapter.this;
            r2 = "read: bad value given";
            r1.m15e(r2);
            r0.stop();
            goto L_0x0043;
        L_0x008d:
            r8 = 0;
        L_0x008e:
            r1 = r7.length;
            if (r8 < r1) goto L_0x009d;
        L_0x0091:
            r1 = ch.serverbox.android.osciprime.audio.AudioAdapter.this;
            r1 = r1.mAudioSource;
            r2 = new int[r6];
            r1.onNewSamples(r9, r2);
            goto L_0x0053;
        L_0x009d:
            r1 = r7[r8];
            r9[r8] = r1;
            r8 = r8 + 1;
            goto L_0x008e;
            */
            throw new UnsupportedOperationException("Method not decompiled: ch.serverbox.android.osciprime.audio.AudioAdapter.2.run():void");
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public AudioAdapter(ch.serverbox.android.osciprime.sources.AudioSource r4) {
        /*
        r3 = this;
        r2 = 0;
        r1 = 0;
        r3.<init>();
        r3.mLooper = r2;
        r3.mHandler = r2;
        r3.mStop = r1;
        r3.mStopped = r1;
        r1 = new java.lang.Object;
        r1.<init>();
        r3.mLock = r1;
        r1 = new java.lang.Thread;
        r2 = new ch.serverbox.android.osciprime.audio.AudioAdapter$1;
        r2.<init>();
        r1.<init>(r2);
        r3.mAudioSamplingThread = r1;
        r1 = new ch.serverbox.android.osciprime.audio.AudioAdapter$2;
        r1.<init>();
        r3.mAudioLoop = r1;
        r3.mAudioSource = r4;
        r1 = r3.mAudioSamplingThread;
        r1.start();
        r2 = r3.mLock;
        monitor-enter(r2);
    L_0x0031:
        r1 = r3.mHandler;	 Catch:{ all -> 0x0047 }
        if (r1 == 0) goto L_0x0037;
    L_0x0035:
        monitor-exit(r2);	 Catch:{ all -> 0x0047 }
        return;
    L_0x0037:
        r1 = r3.mLock;	 Catch:{ InterruptedException -> 0x003d }
        r1.wait();	 Catch:{ InterruptedException -> 0x003d }
        goto L_0x0031;
    L_0x003d:
        r0 = move-exception;
        r1 = "can't wait for lock, interrupted";
        r3.m15e(r1);	 Catch:{ all -> 0x0047 }
        r0.printStackTrace();	 Catch:{ all -> 0x0047 }
        goto L_0x0031;
    L_0x0047:
        r1 = move-exception;
        monitor-exit(r2);	 Catch:{ all -> 0x0047 }
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: ch.serverbox.android.osciprime.audio.AudioAdapter.<init>(ch.serverbox.android.osciprime.sources.AudioSource):void");
    }

    public void startSampling() {
        this.mHandler.post(this.mAudioLoop);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void stopSampling() {
        /*
        r4 = this;
        r3 = 0;
        r1 = 1;
        r4.mStop = r1;
        r2 = r4.mLock;
        monitor-enter(r2);
    L_0x0007:
        r1 = r4.mStopped;	 Catch:{ all -> 0x0026 }
        if (r1 == 0) goto L_0x0016;
    L_0x000b:
        monitor-exit(r2);	 Catch:{ all -> 0x0026 }
        r4.mStopped = r3;
        r4.mStop = r3;
        r1 = "stopped";
        r4.m16l(r1);
        return;
    L_0x0016:
        r1 = r4.mLock;	 Catch:{ InterruptedException -> 0x001c }
        r1.wait();	 Catch:{ InterruptedException -> 0x001c }
        goto L_0x0007;
    L_0x001c:
        r0 = move-exception;
        r1 = "can't wait for lock";
        r4.m15e(r1);	 Catch:{ all -> 0x0026 }
        r0.printStackTrace();	 Catch:{ all -> 0x0026 }
        goto L_0x0007;
    L_0x0026:
        r1 = move-exception;
        monitor-exit(r2);	 Catch:{ all -> 0x0026 }
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: ch.serverbox.android.osciprime.audio.AudioAdapter.stopSampling():void");
    }

    public void quit() {
        m16l("quitting ...");
        if (this.mLooper != null) {
            this.mLooper.quit();
            try {
                this.mAudioSamplingThread.join();
            } catch (InterruptedException e) {
                m15e("could not join AudioSamplingthread, interrupted");
                e.printStackTrace();
            }
            m16l("threads joined ...");
        }
    }

    private void m15e(String msg) {
        Log.e("AudioAdapter", ">==< " + msg + " >==<");
    }

    private void m16l(String msg) {
        Log.d("AudioAdapter", ">==< " + msg + " >==<");
    }
}
