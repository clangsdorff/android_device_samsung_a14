package com.android.attention;

import android.app.Service;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.util.Size;

import java.nio.ByteBuffer;
import java.util.Collections;

public class CameraLightService extends Service {
    private static final String TAG = "CameraLightService";

    private static final int MSG_START = 1;
    private static final int MSG_STOP = 2;
    private static final int MSG_LUX = 3;

    private static final long SAMPLE_INTERVAL_MS = 4000;
    private static final long CAPTURE_TIMEOUT_MS = 2500;
    private static final int MAX_WARMUP_FRAMES = 5;
    private static final int GRID_SAMPLES = 16;

    private HandlerThread mThread;
    private Handler mHandler;
    private Messenger mIncoming;
    private Messenger mReplyTo;
    private CameraManager mCameraManager;
    private String mCameraId;
    private float mDefaultAperture = 2.0f;
    private boolean mCameraAvailable = true;
    private boolean mActive;
    private Capture mCapture;

    private final Runnable mSampleRunnable = this::startSample;

    private final CameraManager.AvailabilityCallback mAvailability =
            new CameraManager.AvailabilityCallback() {
        @Override
        public void onCameraAvailable(String cameraId) {
            if (cameraId.equals(mCameraId)) {
                mCameraAvailable = true;
            }
        }

        @Override
        public void onCameraUnavailable(String cameraId) {
            if (cameraId.equals(mCameraId) && mCapture == null) {
                mCameraAvailable = false;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mThread = new HandlerThread(TAG);
        mThread.start();
        mHandler = new Handler(mThread.getLooper(), this::handleMessage);
        mIncoming = new Messenger(mHandler);
        mCameraManager = getSystemService(CameraManager.class);
        initFrontCamera();
        if (mCameraId != null) {
            mCameraManager.registerAvailabilityCallback(mAvailability, mHandler);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mIncoming.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mHandler.post(this::stopSampling);
        return false;
    }

    @Override
    public void onDestroy() {
        mHandler.post(() -> {
            stopSampling();
            if (mCameraId != null) {
                mCameraManager.unregisterAvailabilityCallback(mAvailability);
            }
            mThread.quitSafely();
        });
        super.onDestroy();
    }

    private boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_START:
                mReplyTo = msg.replyTo;
                mActive = true;
                mHandler.removeCallbacks(mSampleRunnable);
                startSample();
                return true;
            case MSG_STOP:
                stopSampling();
                return true;
            default:
                return false;
        }
    }

    private void stopSampling() {
        mActive = false;
        mReplyTo = null;
        mHandler.removeCallbacks(mSampleRunnable);
        if (mCapture != null) {
            mCapture.close();
        }
    }

    private void scheduleNext() {
        mHandler.removeCallbacks(mSampleRunnable);
        if (mActive) {
            mHandler.postDelayed(mSampleRunnable, SAMPLE_INTERVAL_MS);
        }
    }

    private void startSample() {
        if (!mActive || mReplyTo == null) {
            return;
        }
        if (mCapture != null) {
            return;
        }
        if (mCameraId == null || !mCameraAvailable) {
            scheduleNext();
            return;
        }
        mCapture = new Capture();
        mCapture.start();
    }

    private void deliverLux(float lux) {
        if (!mActive || mReplyTo == null) {
            return;
        }
        try {
            Message msg = Message.obtain(null, MSG_LUX, Float.floatToRawIntBits(lux), 0);
            mReplyTo.send(msg);
        } catch (RemoteException e) {
            Log.w(TAG, "failed to deliver lux", e);
            stopSampling();
        }
    }

    private void initFrontCamera() {
        try {
            for (String id : mCameraManager.getCameraIdList()) {
                CameraCharacteristics c = mCameraManager.getCameraCharacteristics(id);
                Integer facing = c.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraMetadata.LENS_FACING_FRONT) {
                    mCameraId = id;
                    float[] apertures = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);
                    if (apertures != null && apertures.length > 0 && apertures[0] > 0) {
                        mDefaultAperture = apertures[0];
                    }
                    return;
                }
            }
        } catch (CameraAccessException | RuntimeException e) {
            Log.w(TAG, "front camera lookup failed", e);
        }
    }

    private Size pickSmallestSize() {
        try {
            CameraCharacteristics c = mCameraManager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap map = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) return null;
            Size[] sizes = map.getOutputSizes(ImageFormat.YUV_420_888);
            if (sizes == null || sizes.length == 0) return null;
            Size best = null;
            for (Size s : sizes) {
                if (s.getWidth() < 64 || s.getHeight() < 64) continue;
                if (best == null || area(s) < area(best)) {
                    best = s;
                }
            }
            return best != null ? best : sizes[sizes.length - 1];
        } catch (CameraAccessException | RuntimeException e) {
            Log.w(TAG, "size lookup failed", e);
            return null;
        }
    }

    private static long area(Size s) {
        return (long) s.getWidth() * s.getHeight();
    }

    private final class Capture {
        private ImageReader mReader;
        private CameraDevice mDevice;
        private boolean mDone;
        private int mFrames;
        private boolean mHaveAe;
        private long mExposureNs;
        private int mIso;
        private float mAperture;
        private final Runnable mTimeout = this::close;

        void start() {
            Size size = pickSmallestSize();
            if (size == null) {
                close();
                return;
            }
            mHandler.postDelayed(mTimeout, CAPTURE_TIMEOUT_MS);
            mReader = ImageReader.newInstance(size.getWidth(), size.getHeight(),
                    ImageFormat.YUV_420_888, 2);
            mReader.setOnImageAvailableListener(r -> onImage(), mHandler);
            try {
                mCameraManager.openCamera(mCameraId, mDeviceCallback, mHandler);
            } catch (CameraAccessException | RuntimeException e) {
                Log.w(TAG, "open failed", e);
                close();
            }
        }

        private final CameraDevice.StateCallback mDeviceCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice device) {
                if (mDone) {
                    device.close();
                    return;
                }
                mDevice = device;
                try {
                    OutputConfiguration out = new OutputConfiguration(mReader.getSurface());
                    device.createCaptureSession(new SessionConfiguration(
                            SessionConfiguration.SESSION_REGULAR,
                            Collections.singletonList(out),
                            mHandler::post,
                            mSessionCallback));
                } catch (CameraAccessException | RuntimeException e) {
                    Log.w(TAG, "session failed", e);
                    close();
                }
            }

            @Override
            public void onDisconnected(CameraDevice device) {
                device.close();
                close();
            }

            @Override
            public void onError(CameraDevice device, int error) {
                device.close();
                close();
            }
        };

        private final CameraCaptureSession.StateCallback mSessionCallback =
                new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                if (mDone) return;
                try {
                    CaptureRequest.Builder b =
                            mDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    b.addTarget(mReader.getSurface());
                    b.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    b.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                    b.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
                    b.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                    session.setRepeatingRequest(b.build(), mCaptureCallback, mHandler);
                } catch (CameraAccessException | RuntimeException e) {
                    Log.w(TAG, "request failed", e);
                    close();
                }
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {
                close();
            }
        };

        private final CameraCaptureSession.CaptureCallback mCaptureCallback =
                new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                    TotalCaptureResult result) {
                if (mDone) return;
                mFrames++;
                Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                boolean settled = (ae == null && mFrames >= 2)
                        || (ae != null && (ae == CaptureResult.CONTROL_AE_STATE_CONVERGED
                                || ae == CaptureResult.CONTROL_AE_STATE_LOCKED
                                || ae == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED))
                        || mFrames >= MAX_WARMUP_FRAMES;
                if (!settled) return;

                Long exp = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
                Integer iso = result.get(CaptureResult.SENSOR_SENSITIVITY);
                Float ap = result.get(CaptureResult.LENS_APERTURE);
                if (exp != null && exp > 0 && iso != null && iso > 0) {
                    mExposureNs = exp;
                    mIso = iso;
                    mAperture = (ap != null && ap > 0) ? ap : mDefaultAperture;
                    mHaveAe = true;
                }
            }
        };

        private void onImage() {
            Image image = mReader.acquireLatestImage();
            if (image == null) return;
            float lux = -1f;
            try {
                if (mDone || !mHaveAe) return;
                double meanY = sampleMeanLuma(image);
                lux = computeLux(mExposureNs, mIso, mAperture, meanY);
            } finally {
                image.close();
            }
            close();
            if (lux >= 0f) {
                deliverLux(lux);
            }
        }

        void close() {
            if (mDone) return;
            mDone = true;
            mHandler.removeCallbacks(mTimeout);
            if (mDevice != null) {
                mDevice.close();
                mDevice = null;
            }
            if (mReader != null) {
                mReader.close();
                mReader = null;
            }
            mCapture = null;
            scheduleNext();
        }
    }

    private static double sampleMeanLuma(Image image) {
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buf = plane.getBuffer();
        int w = image.getWidth();
        int h = image.getHeight();
        int rowStride = plane.getRowStride();
        int pixelStride = plane.getPixelStride();
        int stepX = Math.max(1, w / GRID_SAMPLES);
        int stepY = Math.max(1, h / GRID_SAMPLES);
        long sum = 0;
        int count = 0;
        for (int y = stepY / 2; y < h; y += stepY) {
            int row = y * rowStride;
            for (int x = stepX / 2; x < w; x += stepX) {
                sum += buf.get(row + x * pixelStride) & 0xff;
                count++;
            }
        }
        return count > 0 ? (double) sum / count : 0.0;
    }

    private static float computeLux(long exposureNs, int iso, float aperture, double meanY) {
        double t = exposureNs / 1_000_000_000.0;
        double ev100 = Math.log((aperture * aperture) / t) / Math.log(2.0)
                - Math.log(iso / 100.0) / Math.log(2.0);
        double lumaScale = Math.max(0.02, Math.min(2.5, Math.pow(meanY / 118.0, 2.2)));
        double lux = Math.pow(2.0, ev100) * 2.5 * lumaScale;
        return (float) Math.max(0.0, Math.min(10000.0, lux));
    }
}
