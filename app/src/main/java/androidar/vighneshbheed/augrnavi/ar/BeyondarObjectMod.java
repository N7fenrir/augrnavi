
package androidar.vighneshbheed.augrnavi.ar;

import com.beyondar.android.opengl.colision.MeshCollider;
import com.beyondar.android.opengl.colision.SquareMeshCollider;
import com.beyondar.android.opengl.renderable.Renderable;
import com.beyondar.android.opengl.renderable.SquareRenderable;
import com.beyondar.android.opengl.renderer.ARRenderer;
import com.beyondar.android.opengl.texture.Texture;
import com.beyondar.android.plugin.BeyondarObjectPlugin;
import com.beyondar.android.plugin.Plugable;
import com.beyondar.android.util.cache.BitmapCache;
import com.beyondar.android.util.math.geom.Point3;
import com.beyondar.android.world.World;

import java.util.ArrayList;
import java.util.List;


public class BeyondarObjectMod implements Plugable<BeyondarObjectPlugin> {

	private String mPlaceId;

	private Long mId;
	private int mTypeList;
	private Texture mTexture;
	private String mImageUri;
	private String mName;
	private boolean mVisible;
	private Renderable mRenderable;
	private Point3 mPosition;
	private Point3 mAngle;
	private boolean mFaceToCamera;
	private MeshCollider mMeshCollider;
	private double mDistanceFromUser;
	private Point3 mScreenPositionTopLeft, mScreenPositionTopRight, mScreenPositionBottomLeft,
			mScreenPositionBottomRight, mScreenPositionCenter;
	private Point3 mTopLeft, mBottomLeft, mBottomRight, mTopRight;

	
	protected List<BeyondarObjectPlugin> plugins;
	
	protected Object lockPlugins = new Object();

	
	public BeyondarObjectMod(long id) {
		mId = id;
		init();
	}

	public BeyondarObjectMod(long id,String placeId) {
		mId = id;
		mPlaceId=placeId;
		init();
	}

	
	public BeyondarObjectMod() {
		init();
	}

	private void init() {
		plugins = new ArrayList<BeyondarObjectPlugin>(DEFAULT_PLUGINS_CAPACITY);
		mPosition = new Point3();
		mAngle = new Point3();
		mTexture = new Texture();
		faceToCamera(true);
		setVisible(true);

		mTopLeft = new Point3();
		mBottomLeft = new Point3();
		mBottomRight = new Point3();
		mTopRight = new Point3();

		mScreenPositionTopLeft = new Point3();
		mScreenPositionTopRight = new Point3();
		mScreenPositionBottomLeft = new Point3();
		mScreenPositionBottomRight = new Point3();
		mScreenPositionCenter = new Point3();
	}

	
	public long getId() {
		if (mId == null) {
			mId = (long) hashCode();
		}
		return mId.longValue();
	}

	public void addPlugin(BeyondarObjectPlugin plugin) {
		synchronized (lockPlugins) {
			if (plugins.contains(plugin)) {
				return;
			}
			plugins.add(plugin);
		}
	}

	@Override
	public boolean removePlugin(BeyondarObjectPlugin plugin) {
		boolean removed = false;
		synchronized (lockPlugins) {
			removed = plugins.remove(plugin);
		}
		if (removed) {
			plugin.onDetached();
		}
		return removed;
	}

	@Override
	public void removeAllPlugins() {
		synchronized (lockPlugins) {
			plugins.clear();
		}
	}

	@Override
	public BeyondarObjectPlugin getFirstPlugin(Class<? extends BeyondarObjectPlugin> pluginClass) {
		synchronized (lockPlugins) {
			for (BeyondarObjectPlugin plugin : plugins) {
				if (pluginClass.isInstance(plugin)) {
					return plugin;
				}
			}
		}
		return null;
	}

	@Override
	public boolean containsAnyPlugin(Class<? extends BeyondarObjectPlugin> pluginClass) {
		return getFirstPlugin(pluginClass) != null;
	}

	@Override
	public boolean containsPlugin(BeyondarObjectPlugin plugin) {
		synchronized (lockPlugins) {
			return plugins.contains(plugin);
		}
	}

	@Override
	public List<BeyondarObjectPlugin> getAllPugins(Class<? extends BeyondarObjectPlugin> pluginClass) {
		ArrayList<BeyondarObjectPlugin> result = new ArrayList<BeyondarObjectPlugin>(5);
		return getAllPlugins(pluginClass, result);
	}

	@Override
	public List<BeyondarObjectPlugin> getAllPlugins(Class<? extends BeyondarObjectPlugin> pluginClass,
			List<BeyondarObjectPlugin> result) {
		synchronized (lockPlugins) {
			for (BeyondarObjectPlugin plugin : plugins) {
				if (pluginClass.isInstance(plugin)) {
					result.add(plugin);
				}
			}
		}
		return result;
	}

	
	@Override
	public List<BeyondarObjectPlugin> getAllPlugins() {
		synchronized (lockPlugins) {
			return new ArrayList<BeyondarObjectPlugin>(plugins);
		}
	}

	void onRemoved() {
		synchronized (lockPlugins) {
			for (BeyondarObjectPlugin plugin : plugins) {
				plugin.onDetached();
			}
		}
	}

	
	public Point3 getAngle() {
		return mAngle;
	}

	
	public void setAngle(float x, float y, float z) {
		if (mAngle.x == x && mAngle.y == y && mAngle.z == z)
			return;
		mAngle.x = x;
		mAngle.y = y;
		mAngle.z = z;
		synchronized (lockPlugins) {
			for (BeyondarObjectPlugin plugin : plugins) {
				plugin.onAngleChanged(mAngle);
			}
		}
	}

	
	public void setAngle(Point3 newAngle) {
		if (newAngle == mAngle)
			return;
		mAngle = newAngle;
		synchronized (lockPlugins) {
			for (BeyondarObjectPlugin plugin : plugins) {
				plugin.onAngleChanged(mAngle);
			}
		}
	}

	
	public Point3 getPosition() {
		return mPosition;
	}

	
	public void setPosition(Point3 newPos) {
		if (newPos == mPosition)
			return;
		mPosition = newPos;
		synchronized (lockPlugins) {
			for (BeyondarObjectPlugin plugin : plugins) {
				plugin.onPositionChanged(mPosition);
			}
		}
	}


	public void setPosition(float x, float y, float z) {
		if (mPosition.x == x && mPosition.y == y && mPosition.z == z)
			return;
		mPosition.x = x;
		mPosition.y = y;
		mPosition.z = z;
		synchronized (lockPlugins) {
			for (BeyondarObjectPlugin plugin : plugins) {
				plugin.onPositionChanged(mPosition);
			}
		}
	}

	
	protected Renderable createRenderable() {
		return SquareRenderable.getInstance();
	}

	
	public Texture getTexture() {
		return mTexture;
	}

	
	public void setTexturePointer(int texturePointer) {
		if (texturePointer == mTexture.getTexturePointer())
			return;
		mTexture.setTexturePointer(texturePointer);
		synchronized (lockPlugins) {
			for (BeyondarObjectPlugin plugin : plugins) {
				plugin.onTextureChanged(mTexture);
			}
		}
	}

	
	public void setTexture(Texture texture) {
		if (texture == this.mTexture) {
			return;
		}
		if (texture == null) {
			texture = new Texture();
		}
		this.mTexture = texture;
		synchronized (lockPlugins) {
			for (BeyondarObjectPlugin plugin : plugins) {
				plugin.onTextureChanged(this.mTexture);
			}
		}
	}

	
	public Renderable getOpenGLObject() {
		if (null == mRenderable) {
			mRenderable = createRenderable();
		}
		return mRenderable;
	}

	
	public void setRenderable(Renderable renderable) {
		if (renderable == this.mRenderable)
			return;
		this.mRenderable = renderable;
		synchronized (lockPlugins) {
			for (BeyondarObjectPlugin plugin : plugins) {
				plugin.onRenderableChanged(this.mRenderable);
			}
		}
	}

	
	public String getImageUri() {
		return mImageUri;
	}

	
	public void faceToCamera(boolean faceToCamera) {
		if (faceToCamera == this.mFaceToCamera)
			return;
		this.mFaceToCamera = faceToCamera;
		synchronized (lockPlugins) {
			for (BeyondarObjectPlugin plugin : plugins) {
				plugin.onFaceToCameraChanged(this.mFaceToCamera);
			}
		}
	}

	
	public boolean isFacingToCamera() {
		return mFaceToCamera;
	}

	
	public void setVisible(boolean visible) {
		if (visible == this.mVisible)
			return;
		this.mVisible = visible;
		synchronized (lockPlugins) {
			for (BeyondarObjectPlugin plugin : plugins) {
				plugin.onVisibilityChanged(this.mVisible);
			}
		}
	}

	
	public boolean isVisible() {
		return mVisible;
	}

	
	public void setName(String name) {
		if (name == this.mName)
			return;
		this.mName = name;
		synchronized (lockPlugins) {
			for (BeyondarObjectPlugin plugin : plugins) {
				plugin.onNameChanged(this.mName);
			}
		}
	}

	
	public String getName() {
		return mName;
	}

	public String getPlaceId() {
		return mPlaceId;
	}

	
	public void setImageUri(String uri) {
		if (uri == mImageUri)
			return;
		mImageUri = uri;
		synchronized (lockPlugins) {
			for (BeyondarObjectPlugin plugin : plugins) {
				plugin.onImageUriChanged(mImageUri);
			}
		}
		setTexture(null);
	}

	
	public void setImageResource(int resId) {
		setImageUri(BitmapCache.generateUri(resId));
	}

	
	void setWorldListType(int worldListType) {
		mTypeList = worldListType;
	}

	
	public int getWorldListType() {
		return mTypeList;
	}

	
	public double getDistanceFromUser() {
		return mDistanceFromUser;
	}

	
	public void setDistanceFromUser(double distance) {
		mDistanceFromUser = distance;
	}

	
	public Point3 getScreenPositionBottomLeft() {
		return mScreenPositionBottomLeft;
	}

	
	public Point3 getScreenPositionTopLeft() {
		return mScreenPositionTopLeft;
	}

	
	public Point3 getScreenPositionTopRight() {
		return mScreenPositionTopRight;
	}

	
	public Point3 getScreenPositionBottomRight() {
		return mScreenPositionBottomRight;
	}

	
	public Point3 getScreenPositionCenter() {
		return mScreenPositionCenter;
	}

	
	public Point3 getTopLeft() {
		mTopLeft.x = mPosition.x + mTexture.getVertices()[3];
		mTopLeft.y = mPosition.y + mTexture.getVertices()[4];
		mTopLeft.z = mPosition.z + mTexture.getVertices()[5];

		mTopLeft.rotatePointDegrees_x(mAngle.x, mPosition);
		mTopLeft.rotatePointDegrees_y(mAngle.y, mPosition);
		mTopLeft.rotatePointDegrees_z(mAngle.z, mPosition);
		return mTopLeft;
	}

	
	public Point3 getBottomLeft() {
		mBottomLeft.x = mPosition.x + mTexture.getVertices()[0];
		mBottomLeft.y = mPosition.y + mTexture.getVertices()[1];
		mBottomLeft.z = mPosition.z + mTexture.getVertices()[2];

		mBottomLeft.rotatePointDegrees_x(mAngle.x, mPosition);
		mBottomLeft.rotatePointDegrees_y(mAngle.y, mPosition);
		mBottomLeft.rotatePointDegrees_z(mAngle.z, mPosition);
		return mBottomLeft;
	}

	
	public Point3 getBottomRight() {
		mBottomRight.x = mPosition.x + mTexture.getVertices()[6];
		mBottomRight.y = mPosition.y + mTexture.getVertices()[7];
		mBottomRight.z = mPosition.z + mTexture.getVertices()[8];

		mBottomRight.rotatePointDegrees_x(mAngle.x, mPosition);
		mBottomRight.rotatePointDegrees_y(mAngle.y, mPosition);
		mBottomRight.rotatePointDegrees_z(mAngle.z, mPosition);
		return mBottomRight;
	}

	
	public Point3 getTopRight() {
		mTopRight.x = mPosition.x + mTexture.getVertices()[9];
		mTopRight.y = mPosition.y + mTexture.getVertices()[10];
		mTopRight.z = mPosition.z + mTexture.getVertices()[11];

		mTopRight.rotatePointDegrees_x(mAngle.x, mPosition);
		mTopRight.rotatePointDegrees_y(mAngle.y, mPosition);
		mTopRight.rotatePointDegrees_z(mAngle.z, mPosition);
		return mTopRight;
	}

	
	public MeshCollider getMeshCollider() {
		Point3 topLeft = getTopLeft();
		Point3 bottomLeft = getBottomLeft();
		Point3 bottomRight = getBottomRight();
		Point3 topRight = getTopRight();

		mMeshCollider = new SquareMeshCollider(topLeft, bottomLeft, bottomRight, topRight);
		return mMeshCollider;
	}
}
