package androidar.vighneshbheed.augrnavi.ar;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ListAdapter;

import com.beyondar.android.util.math.geom.Point2;
import com.beyondar.android.world.BeyondarObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public abstract class BeyondarViewAdapter {

	Queue<ViewGroup> mReusedViews;
	Queue<ViewGroup> mNewViews;

	ViewGroup mParentView;
	Point2 mNewPosition;
	Context mContext;

	final LayoutParams mLayoutParams;

	public BeyondarViewAdapter(Context context) {
		mReusedViews = new LinkedList<ViewGroup>();
		mNewViews = new LinkedList<ViewGroup>();
		mContext = context;
		mLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	}

	void processList(final List<BeyondarObject> list, ViewGroup parent, final ArBeyondarGLSurfaceView glSurface) {
		mParentView = parent;

		mParentView.post(new Runnable() {
			@Override
			public void run() {
				for (BeyondarObject beyondarObject : list) {

					if (beyondarObject.getScreenPositionCenter().z > 1) {
						continue;
					}
					CustomLayout recycledParent = (CustomLayout) mReusedViews.poll();
					glSurface.fillBeyondarObjectPositions(beyondarObject);
					View toRecycle = null;

					if (recycledParent != null && recycledParent.getChildCount() > 0) {
						toRecycle = recycledParent.getChildAt(0);
					}

					View view = getView(beyondarObject, toRecycle, mParentView);

					boolean added = false;
					if ((toRecycle != view || view == null) && toRecycle != null) {
						mReusedViews.add(recycledParent);
						added = true;
					}

					if (view != null && (recycledParent == null || view.getParent() != recycledParent)) {
						CustomLayout parentLayout = new CustomLayout(mContext);
						parentLayout.addView(view, mLayoutParams);
						if (!added) {
							mReusedViews.add(recycledParent);
						}
						recycledParent = parentLayout;
					}

					if (view != null) {
						mNewViews.add(recycledParent);
						if (recycledParent.getParent() == null) {
							mParentView.addView(recycledParent, mLayoutParams);
						}
						recycledParent.setPosition((int) mNewPosition.x, (int) mNewPosition.y);
					}
				}

				removeUnusedViews();
				Queue<ViewGroup> tmp = mNewViews;
				mNewViews = mReusedViews;
				mReusedViews = tmp;
				mNewPosition = null;
			}
		});
	}

	
	protected Context getContext() {
		return mContext;
	}

	
	protected void setPosition(Point2 position) {
		mNewPosition = position;
	}

	private void removeUnusedViews() {
		while (!mReusedViews.isEmpty()) {
			View view = mReusedViews.poll();
			mParentView.removeView(view);
		}
	}

	
	public abstract View getView(BeyondarObject beyondarObject, View recycledView, ViewGroup parent);
}
