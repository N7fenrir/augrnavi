
package androidar.vighneshbheed.augrnavi.ar;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.beyondar.android.opengl.renderer.ARRenderer;
import com.beyondar.android.opengl.renderer.ARRenderer.FpsUpdatable;
import com.beyondar.android.opengl.renderer.ARRenderer.GLSnapshotCallback;
import com.beyondar.android.opengl.renderer.OnBeyondarObjectRenderedListener;
import com.beyondar.android.opengl.util.MatrixTrackingGL;
import com.beyondar.android.sensor.BeyondarSensorManager;
import com.beyondar.android.util.Logger;
import com.beyondar.android.util.math.geom.Ray;
import com.beyondar.android.world.BeyondarObject;
import com.beyondar.android.world.World;

import androidar.vighneshbheed.augrnavi.ar.BeyondarViewAdapter;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL;


public class ArBeyondarGLSurfaceView extends GLSurfaceView implements OnBeyondarObjectRenderedListener {

	protected ARRenderer mRenderer;

	private BeyondarViewAdapter mViewAdapter;
	private ViewGroup mParent;

	private World mWorld;
	private int mSensorDelay;

	public ArBeyondarGLSurfaceView(Context context) {
		super(context);
		init(context);

	}

	public ArBeyondarGLSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	private void init(Context context) {
		mSensorDelay = SensorManager.SENSOR_DELAY_UI;

		if (Logger.DEBUG_OPENGL) {
			setDebugFlags(DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS);
		}

		setGLWrapper(new GLWrapper() {
			@Override
			public GL wrap(GL gl) {
				return new MatrixTrackingGL(gl);
			}
		});

		mRenderer = createRenderer();
		mRenderer.setOnBeyondarObjectRenderedListener(this);

		setEGLConfigChooser(8, 8, 8, 8, 16, 0);
		getHolder().setFormat(PixelFormat.TRANSLUCENT);
		setRenderer(mRenderer);

		requestFocus();
		setZOrderMediaOverlay(true);
		setFocusableInTouchMode(true);
	}

	
	public void tackePicture(GLSnapshotCallback callBack) {
		mRenderer.tackePicture(callBack);
	}

	
	protected ARRenderer createRenderer() {
		return new ARRenderer();
	}

	
	public void setFpsUpdatable(FpsUpdatable fpsUpdatable) {
		mRenderer.setFpsUpdatable(fpsUpdatable);
	}

	@Override
	public void setVisibility(int visibility) {
		if (visibility == VISIBLE) {
			mRenderer.setRendering(true);
		} else {
			mRenderer.setRendering(false);
		}
		super.setVisibility(visibility);
	}

	
	public void setSensorDelay(int delay) {
		mSensorDelay = delay;
		unregisterSensorListener();
		registerSensorListener(mSensorDelay);
	}

	
	public int getSensorDelay() {
		return mSensorDelay;
	}

	
	public void setWorld(World world) {
		if (null == mWorld) {unregisterSensorListener();
			registerSensorListener(mSensorDelay);
		}
		mWorld = world;
		mRenderer.setWorld(world);
	}

	private void unregisterSensorListener() {
		BeyondarSensorManager.unregisterSensorListener(mRenderer);
	}

	private void registerSensorListener(int sensorDealy) {
		BeyondarSensorManager.registerSensorListener(mRenderer);

	}

	@Override
	public void onPause() {
		unregisterSensorListener();
		super.onPause();
		mRenderer.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		registerSensorListener(mSensorDelay);
		if (mRenderer != null) {
			Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE))
					.getDefaultDisplay();
			mRenderer.rotateView(display.getRotation());
			mRenderer.onResume();
		}
	}

	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		if (mWorld == null || event == null) {
			return false;
		}

		return false;
	}

	private static final Ray sRay = new Ray(0, 0, 0);

	
	public synchronized void getBeyondarObjectsOnScreenCoordinates(float x, float y,
			ArrayList<BeyondarObject> beyondarObjects) {
		getBeyondarObjectsOnScreenCoordinates(x, y, beyondarObjects, sRay);

	}

	
	public synchronized void getBeyondarObjectsOnScreenCoordinates(float x, float y,
			ArrayList<BeyondarObject> beyondarObjects, Ray ray) {
		mRenderer.getViewRay(x, y, ray);
		mWorld.getBeyondarObjectsCollideRay(ray, beyondarObjects, getMaxDistanceToRender());
	}

	
	public void setPullCloserDistance(float maxDistanceSize) {
		mRenderer.setPullCloserDistance(maxDistanceSize);
	}

	
	public float getPullCloserDistance() {
		return mRenderer.getPullCloserDistance();
	}

	
	public void setPushAwayDistance(float minDistanceSize) {
		mRenderer.setPushAwayDistance(minDistanceSize);
	}

	
	public float getPushAwayDistance() {
		return mRenderer.getPushAwayDistance();
	}

	
	public void setMaxDistanceToRender(float meters) {
		mRenderer.setMaxDistanceToRender(meters);
	}

	
	public float getMaxDistanceToRender() {
		return mRenderer.getMaxDistanceToRender();
	}

	
	public void setDistanceFactor(float meters)
	{
		mRenderer.setDistanceFactor(meters);
	}

	
	public float getDistanceFactor(){
		return mRenderer.getDistanceFactor();
	}

	public void setBeyondarViewAdapter(BeyondarViewAdapter beyondarViewAdapter, ViewGroup parent) {
		mViewAdapter = beyondarViewAdapter;
		mParent = parent;
	}

	@Override
	public void onBeyondarObjectsRendered(List<BeyondarObject> renderedBeyondarObjects) {
		BeyondarViewAdapter tmpView = mViewAdapter;
		if (tmpView != null) {
			List<BeyondarObject> elements = World
					.sortGeoObjectByDistanceFromCenter(new ArrayList<BeyondarObject>(renderedBeyondarObjects));
			tmpView.processList(elements, mParent, this);
		}
	}

	
	public void forceFillBeyondarObjectPositionsOnRendering(boolean fill) {
		mRenderer.forceFillBeyondarObjectPositions(fill);
	}

	
	public void fillBeyondarObjectPositions(BeyondarObject beyondarObject) {
		mRenderer.fillBeyondarObjectScreenPositions(beyondarObject);
	}
}
