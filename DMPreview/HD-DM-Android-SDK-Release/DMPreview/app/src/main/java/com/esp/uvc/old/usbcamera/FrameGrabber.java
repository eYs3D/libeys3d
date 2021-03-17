package com.esp.uvc.old.usbcamera;

import android.util.Log;

import com.esp.android.usb.camera.core.IFrameCallback;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FrameGrabber{
    String TAG = "FrameGrabber";
    final private GrabberCallback mColorCallback;
    final private GrabberCallback mDepthCallback;

    private ByteBuffer mLatestFrameColor;
    private ByteBuffer mLatestFrameDepth;

    public FrameGrabber(){
        mColorCallback = new GrabberCallback();
        mDepthCallback = new GrabberCallback();
    }

    public ByteBuffer getLatestFrameColor(){
        return  mLatestFrameColor;
    }

    public GrabberCallback getCallbackDepth(){
        return  mDepthCallback;
    }
    public GrabberCallback getCallbackColor(){
        return  mColorCallback;
    }

    public ByteBuffer getLatestFrameDepth(){
        return  mLatestFrameDepth;
    }
    public void grabLatestFrame(){
        try {
            mColorCallback.grab();
            mDepthCallback.grab();
            mLatestFrameColor = mColorCallback.getFrame();
            mLatestFrameDepth = mDepthCallback.getFrame();
        } catch (InterruptedException e) {
            Log.e(TAG,"GetFrame() fail!");
        }

    }

    static class GrabberCallback implements IFrameCallback {
        private final ReentrantLock lock;

        enum State {
            CONSUMER_BUSY, CONSUMER_GRABBING, FRAME_AVAILABLE
        }

        private State state;

        // A buffer containing a copy of the latest frame received.
        // Usable by the consumer when in state CONSUMER_BUSY,
        // and by the producer when in state CONSUMER_GRABBING.
        private ByteBuffer storedFrame;

        private final Condition stateChanged;

        public GrabberCallback() {
            lock = new ReentrantLock();
            stateChanged = lock.newCondition();
            storedFrame = null;
            state = State.CONSUMER_BUSY;
        }

        public void grab() {
            lock.lock();
            if (state != State.CONSUMER_BUSY) {
                throw new IllegalStateException();
            }
            state = State.CONSUMER_GRABBING;
            lock.unlock();
        }

        /* package */ ByteBuffer getFrame() throws InterruptedException {
            lock.lock();
            while (state != State.FRAME_AVAILABLE) {
                stateChanged.await();
            }
            state = State.CONSUMER_BUSY;
            lock.unlock();
            return storedFrame;
        }

        @Override
        public void onFrame(ByteBuffer imageData, int frameCount) {
            //Log.i("onFrame","onFrame(ByteBuffer imageData):"+imageData);
            //Log.d("benaco", "onFrame");

            lock.lock();
            // If the native side isn't waiting, drop the frame.
            if (state == State.CONSUMER_GRABBING) {
                // Reallocate the buffer if necessary.
                if (storedFrame == null || storedFrame.capacity() < imageData.capacity()) {
                    storedFrame = ByteBuffer.allocateDirect(imageData.capacity());
                }
                storedFrame.rewind();
                storedFrame.put(imageData);
                // Notify the native side that a frame is ready.
                state = State.FRAME_AVAILABLE;
                stateChanged.signal();
            }
            lock.unlock();
            //Log.i("onFrame","onFrame(ByteBuffer imageData): end"+imageData+" end");
        }
    }
}
