package androidar.vighneshbheed.augrnavi.ar;



import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.beyondar.android.util.Logger;

import java.io.IOException;


public class ArSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private boolean mIsPreviewing;

    private static final String TAG="CamSurfaceView";

public ArSurfaceView(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mHolder.getSurface() == null){
            return;
        }

        try {
            mCamera.stopPreview();
        } catch (Exception e){
            }

        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    public void startPreviewCamera() {
if(mCamera != null && !this.mIsPreviewing) {
            this.mIsPreviewing = true;

            try {
                mCamera.setPreviewDisplay(this.mHolder);
                mCamera.startPreview();
            } catch (Exception var2) {
                Logger.w("camera", "Cannot start preview.", var2);
                this.mIsPreviewing = false;
            }

        }
    }

    public void releaseCamera() {
        this.stopPreviewCamera();
        if(mCamera != null) {
            mCamera.release();
            mCamera = null;
        }

    }

    public void stopPreviewCamera() {
        if(mCamera != null && this.mIsPreviewing) {
            mIsPreviewing = false;
            mCamera.stopPreview();
        }
    }

}