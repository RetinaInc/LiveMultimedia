package com.constantinnovationsinc.livemultimedia.utilities;

import android.os.MemoryFile;
import android.util.Log;
import java.io.IOException;

/**
 * Created by constantinnovationsinc on 8/17/14.
 */
public class SharedVideoMemory {
    public int sharedVideoFramesBuffer = 0;
    public MemoryFile mSharedMemFile = null;
    /**
     * MemoryFile is a wrapper for the Linux ashmem driver.
     * MemoryFiles are backed by shared memory, which can be optionally
     * set to be purgeable.
     * Purgeable files may have their contents reclaimed by the kernel
     * in low memory conditions (only if allowPurging is set to true).
     * After a file is purged, attempts to read or write the file will
     * cause an IOException to be thrown.
     */
    private static String TAG = "MemoryFile";
    private int mFrameCount = 0;
    private int mProcessedFrames = 0;
    private int mFrameSize = -1;

    /**
     * Allocates a new ashmem region. The region is initially not purgable.
     *
     * @param name      optional name for the file (can be null).
     * @param frameSize
     * @param length    of the memory file in bytes.
     * @throws IOException if the memory file could not be created.
     */
    public SharedVideoMemory(String name, int frameSize, int length) throws IOException {
        mSharedMemFile = new MemoryFile(name, length);
        mFrameSize = frameSize;
    }

    /**
     * Closes the memory file. If there are no other open references to the memory
     * file, it will be deleted.
     */
    public void close() {
        mSharedMemFile.close();
    }

    public boolean isEmpty() {
        return (mSharedMemFile.length() == 0) ? true : false;
    }

    public int getFrameCount() {
        return mFrameCount;
    }

    public int getProcessFramesCount() {
        return mProcessedFrames;
    }

    public int getFrameSize() {
        return mFrameSize;
    }

    public synchronized void lockMemory() {
        try {
            if (mSharedMemFile != null) {
                mSharedMemFile.allowPurging(false);
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public synchronized void clearMemory() {
        try {
            if (mSharedMemFile != null) {
                mSharedMemFile.allowPurging(true);
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public boolean getNextFrame(int frameNum, byte[] buffer) {
        Boolean mFlag = false;
        if (buffer == null) {
            Log.e(TAG, "Cannot return video frame into a null buffer that was passed to getNextFrame()");
            return mFlag;
        }
        if (frameNum == mFrameCount) {
            Log.e(TAG, "You have now read to the end of sharedMemory file");
            return mFlag;
        }
        try {
            Log.d(TAG, "About to read from shared file for frame:framesize is: " + frameNum + "," + mFrameSize);
            int readBytes = mSharedMemFile.readBytes(buffer, frameNum * mFrameSize, 0, mFrameSize);
            if (readBytes == mFrameSize) {
                mFlag = true;
                mProcessedFrames++;
            } else {
                Log.e(TAG, "bytes read are mot the same as frame size!");
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, e.getMessage());
            mFlag = false;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            mFlag = false;
        }
        return mFlag;
    }

    /**
     * Write bytes to the memory file.
     * Will throw an IOException if the file has been purged.
     *
     * @param buffer     byte array to write bytes from.
     * @param srcOffset  offset into the byte array buffer to write from.
     * @param destOffset offset  into the memory file to write to.
     * @param count      number of bytes to write.
     * @throws IOException if the memory file has been purged or deactivated.
     */
    public void writeBytes(byte[] buffer, int srcOffset, int destOffset, int count)
            throws IOException {
        try {
            mSharedMemFile.writeBytes(buffer, srcOffset, destOffset, count);
            mFrameCount++;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public Boolean isLastFrame() {
        return (mFrameCount == mProcessedFrames);
    }
}