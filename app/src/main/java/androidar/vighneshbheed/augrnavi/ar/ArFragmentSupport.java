package androidar.vighneshbheed.augrnavi.ar;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;

import com.beyondar.android.opengl.renderer.ARRenderer.FpsUpdatable;
import com.beyondar.android.sensor.BeyondarSensorManager;
import com.beyondar.android.util.math.geom.Ray;
import com.beyondar.android.view.OnClickBeyondarObjectListener;
import com.beyondar.android.view.OnTouchBeyondarViewListener;
import com.beyondar.android.world.BeyondarObject;
import com.beyondar.android.world.World;



public class ArFragmentSupport extends Fragment implements FpsUpdatable,OnClickListener,
        OnTouchListener{



    private static final int CORE_POOL_SIZE = 1;
    private static final int MAXIMUM_POOL_SIZE = 1;
    private static final long KEEP_ALIVE_TIME = 1000; private ArSurfaceView mBeyondarCameraView;
    private ArBeyondarGLSurfaceView mBeyondarGLSurface;
    private TextView mFpsTextView;
    private RelativeLayout mMainLayout;

    private Camera mCamera;
    private Camera.Parameters param;

    private World mWorld;

    private OnTouchBeyondarViewListenerMod mTouchListener;
    private OnClickBeyondarObjectListener mClickListener;

    private float mLastScreenTouchX, mLastScreenTouchY;

    private ThreadPoolExecutor mThreadPool;
    private BlockingQueue<Runnable> mBlockingQueue;

    private SensorManager mSensorManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBlockingQueue = new LinkedBlockingQueue<Runnable>();
        mThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME,
                TimeUnit.MILLISECONDS, mBlockingQueue);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
    }

    private void init() {
        ViewGroup.LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        mMainLayout = new RelativeLayout(getActivity());
        mBeyondarGLSurface = createBeyondarGLSurfaceView();
        mBeyondarGLSurface.setOnTouchListener(this);

        mBeyondarCameraView = createCameraView();

        mMainLayout.addView(mBeyondarCameraView, params);
        mMainLayout.addView(mBeyondarGLSurface, params);

        mBeyondarGLSurface.setMaxDistanceToRender(1000f);
        Log.d("ARFRAGG", "init: MaxDistRender"+mBeyondarGLSurface.getMaxDistanceToRender());
    }

    private void checkIfSensorsAvailable() {
        PackageManager pm = getActivity().getPackageManager();
        boolean compass = pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
        boolean accelerometer = pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);
        if (!compass && !accelerometer) {
            throw new IllegalStateException(getClass().getName()
                    + " can not run without the compass and the acelerometer sensors.");
        } else if (!compass) {
            throw new IllegalStateException(getClass().getName() + " can not run without the compass sensor.");
        } else if (!accelerometer) {
            throw new IllegalStateException(getClass().getName()
                    + " can not run without the acelerometer sensor.");
        }

    }

    
    protected ArBeyondarGLSurfaceView createBeyondarGLSurfaceView() {
        return new ArBeyondarGLSurfaceView(getActivity());
    }

    
    protected ArSurfaceView createCameraView() {
        mCamera=getCameraInstance();
        param=mCamera.getParameters();

        if(param.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
            param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCamera.setParameters(param);

        return new ArSurfaceView(getActivity(),mCamera);
    }

    
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); }
        catch (Exception e){
            Log.d("Main Activity", "getCameraInstance: ERROR"+e.getMessage());
            }
        return c; }

    
    public ArSurfaceView getCameraView() {
        return mBeyondarCameraView;
    }

    
    public androidar.vighneshbheed.augrnavi.ar.ArBeyondarGLSurfaceView getGLSurfaceView() {
        return mBeyondarGLSurface;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        init();
        startRenderingAR();
        return mMainLayout;
    }

    @Override
    public void onResume() {
        super.onResume();
        mBeyondarCameraView.startPreviewCamera();
        mBeyondarGLSurface.onResume();
        BeyondarSensorManager.resume(mSensorManager);
        if (mWorld != null) {
            mWorld.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mBeyondarCameraView.releaseCamera();
        mBeyondarGLSurface.onPause();
        BeyondarSensorManager.pause(mSensorManager);
        if (mWorld != null) {
            mWorld.onPause();
        }
    }

    
    public void setOnTouchBeyondarViewListener(OnTouchBeyondarViewListenerMod listener) {
        mTouchListener = listener;
    }

    
    public void setOnClickBeyondarObjectListener(OnClickBeyondarObjectListener listener) {
        mClickListener = listener;
        mMainLayout.setClickable(listener != null);
        mMainLayout.setOnClickListener(this);
    }

    @Override
    public boolean onTouch(View v, final MotionEvent event) {
        mLastScreenTouchX = event.getX();
        mLastScreenTouchY = event.getY();

        if (mWorld == null || mTouchListener == null || event == null) {
            return false;
        }
        mTouchListener.onTouchBeyondarView(event, mBeyondarGLSurface);
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v == mMainLayout) {
            if (mClickListener == null) {
                return;
            }
            final float lastX = mLastScreenTouchX;
            final float lastY = mLastScreenTouchY;

            mThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    final ArrayList<BeyondarObject> beyondarObjects = new ArrayList<BeyondarObject>();
                    mBeyondarGLSurface.getBeyondarObjectsOnScreenCoordinates(lastX, lastY, beyondarObjects);
                    if (beyondarObjects.size() == 0)
                        return;
                    mBeyondarGLSurface.post(new Runnable() {
                        @Override
                        public void run() {
                            OnClickBeyondarObjectListener listener = mClickListener;
                            if (listener != null) {
                                Log.d("ArFragment", "run: ListenerSet");
                                listener.onClickBeyondarObject(beyondarObjects);
                            }
                        }
                    });
                }
            });
        }
    }

    
    public World getWorld() {
        return mWorld;
    }

    
    public void setWorld(World world) {
        try {
            checkIfSensorsAvailable();
        } catch (IllegalStateException e) {
            throw e;
        }
        mWorld = world;
        mBeyondarGLSurface.setWorld(world);
    }

    
    public void setSensorDelay(int delay) {
        mBeyondarGLSurface.setSensorDelay(delay);
    }

    
    public int getSensorDelay() {
        return mBeyondarGLSurface.getSensorDelay();
    }

    
    public void setFpsUpdatable(FpsUpdatable fpsUpdatable) {
        mBeyondarGLSurface.setFpsUpdatable(fpsUpdatable);
    }

    
    public void stopRenderingAR() {
        mBeyondarGLSurface.setVisibility(View.INVISIBLE);
    }

    
    public void startRenderingAR() {
        mBeyondarGLSurface.setVisibility(View.VISIBLE);
    }

    
    public List<BeyondarObject> getBeyondarObjectsOnScreenCoordinates(float x, float y) {
        ArrayList<BeyondarObject> beyondarObjects = new ArrayList<BeyondarObject>();
        mBeyondarGLSurface.getBeyondarObjectsOnScreenCoordinates(x, y, beyondarObjects);
        return beyondarObjects;
    }

    
    public void getBeyondarObjectsOnScreenCoordinates(float x, float y,
                                                      ArrayList<BeyondarObject> beyondarObjects) {
        mBeyondarGLSurface.getBeyondarObjectsOnScreenCoordinates(x, y, beyondarObjects);
    }

    
    public void getBeyondarObjectsOnScreenCoordinates(float x, float y,
                                                      ArrayList<BeyondarObject> beyondarObjects, Ray ray) {
        mBeyondarGLSurface.getBeyondarObjectsOnScreenCoordinates(x, y, beyondarObjects, ray);

    }

    
    public void setPullCloserDistance(float maxDistanceSize) {
        mBeyondarGLSurface.setPullCloserDistance(maxDistanceSize);
    }

    
    public float getPullCloserDistance() {
        return mBeyondarGLSurface.getPullCloserDistance();
    }

    
    public void setPushAwayDistance(float minDistanceSize) {
        mBeyondarGLSurface.setPushAwayDistance(minDistanceSize);
    }

    
    public float getPushAwayDistance() {
        return mBeyondarGLSurface.getPushAwayDistance();
    }

    
    public void setMaxDistanceToRender(float meters) {
        mBeyondarGLSurface.setMaxDistanceToRender(meters);
    }

    
    public float getMaxDistanceToRender() {
        return mBeyondarGLSurface.getMaxDistanceToRender();
    }

    
    public void setDistanceFactor(float meters)
    {
        mBeyondarGLSurface.setDistanceFactor(meters);
    }

    
    public float getDistanceFactor(){
        return mBeyondarGLSurface.getDistanceFactor();
    }

    


    public void showFPS(boolean show) {
        if (show) {
            if (mFpsTextView == null) {
                mFpsTextView = new TextView(getActivity());
                mFpsTextView.setBackgroundResource(android.R.color.black);
                mFpsTextView.setTextColor(getResources().getColor(android.R.color.white));
                LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                mMainLayout.addView(mFpsTextView, params);
            }
            mFpsTextView.setVisibility(View.VISIBLE);
            setFpsUpdatable(this);
        } else if (mFpsTextView != null) {
            mFpsTextView.setVisibility(View.GONE);
            setFpsUpdatable(null);
        }
    }

    @Override
    public void onFpsUpdate(final float fps) {
        if (mFpsTextView != null) {
            mFpsTextView.post(new Runnable() {
                @Override
                public void run() {
                    mFpsTextView.setText("fps: " + fps);
                }
            });
        }
    }

    
    public void setBeyondarViewAdapter(BeyondarViewAdapter adapter) {
        mBeyondarGLSurface.setBeyondarViewAdapter(adapter, mMainLayout);
    }

    
    public void forceFillBeyondarObjectPositionsOnRendering(boolean fill) {
        mBeyondarGLSurface.forceFillBeyondarObjectPositionsOnRendering(fill);
    }

    
    public void fillBeyondarObjectPositions(BeyondarObject beyondarObject) {
        mBeyondarGLSurface.fillBeyondarObjectPositions(beyondarObject);
    }

    
    @Deprecated
    public void setMaxFarDistance(float maxDistanceSize) {
        setPullCloserDistance(maxDistanceSize);
    }

    
    @Deprecated
    public float getMaxDistanceSize() {
        return getPullCloserDistance();
    }

    
    @Deprecated
    public void setMinFarDistanceSize(float minDistanceSize) {
        setPushAwayDistance(minDistanceSize);
    }

    
    @Deprecated
    public float getMinDistanceSize() {
        return getPushAwayDistance();
    }
}

