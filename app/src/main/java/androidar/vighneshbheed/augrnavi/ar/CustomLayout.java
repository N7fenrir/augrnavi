
package androidar.vighneshbheed.augrnavi.ar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews.RemoteView;


@SuppressLint("NewApi")
@RemoteView
class CustomLayout extends ViewGroup {
	
	private int mLeftWidth;

	
	private int mRightWidth;

	
	private final Rect mTmpContainerRect = new Rect();
	private final Rect mTmpChildRect = new Rect();

	private int xPos, yPos;

	CustomLayout(Context context) {
		super(context);
	}

	
	@Override
	public boolean shouldDelayChildPressedState() {
		return false;
	}

	public void setPosition(int x, int y) {
		if (Build.VERSION.SDK_INT >= 11) {
			setTranslationX(x);
			setTranslationY(y);
			return;
		}

		if (xPos == x && yPos == y) {
			return;
		}
		xPos = x;
		yPos = y;
		invalidate();
	}

	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int count = getChildCount();

		mLeftWidth = 0;
		mRightWidth = 0;

		int maxHeight = 0;
		int maxWidth = 0;
		int childState = 0;

		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != GONE) {
				measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);

				final LayoutParams lp = (LayoutParams) child.getLayoutParams();
				if (lp.position == LayoutParams.POSITION_LEFT) {
					mLeftWidth += Math.max(maxWidth, child.getMeasuredWidth() + lp.leftMargin
							+ lp.rightMargin);
				} else if (lp.position == LayoutParams.POSITION_RIGHT) {
					mRightWidth += Math.max(maxWidth, child.getMeasuredWidth() + lp.leftMargin
							+ lp.rightMargin);
				} else {
					maxWidth = Math.max(maxWidth, child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
				}
				maxHeight = Math.max(maxHeight, child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
				if (Build.VERSION.SDK_INT >= 11) {
					childState = combineMeasuredStates(childState, child.getMeasuredState());
				} else {
					supportCombineMeasuredStates(childState, 0);
				}
			}
		}

		maxWidth += mLeftWidth + mRightWidth;

		maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
		maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

		if (Build.VERSION.SDK_INT >= 11) {
			setMeasuredDimension(
					resolveSizeAndState(maxWidth, widthMeasureSpec, childState) + xPos,
					resolveSizeAndState(maxHeight, heightMeasureSpec,
							childState << MEASURED_HEIGHT_STATE_SHIFT) + yPos);
		} else {
			setMeasuredDimension(
					resolveSizeAndStateSupport(maxWidth, widthMeasureSpec, childState) + xPos,
					resolveSizeAndStateSupport(maxHeight, heightMeasureSpec,
							childState << MEASURED_HEIGHT_STATE_SHIFT) + yPos);
		}

	}

	public static int resolveSizeAndStateSupport(int size, int measureSpec, int childMeasuredState) {
		int result = size;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);
		switch (specMode) {
		case MeasureSpec.UNSPECIFIED:
			result = size;
			break;
		case MeasureSpec.AT_MOST:
			if (specSize < size) {
				result = specSize | MEASURED_STATE_TOO_SMALL;
			} else {
				result = size;
			}
			break;
		case MeasureSpec.EXACTLY:
			result = specSize;
			break;
		}
		return result | (childMeasuredState & MEASURED_STATE_MASK);
	}

	private static int supportCombineMeasuredStates(int curState, int newState) {
		return curState | newState;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		final int count = getChildCount();

		int leftPos = getPaddingLeft();
		int rightPos = right - left - getPaddingRight();

		final int middleLeft = leftPos + mLeftWidth;
		final int middleRight = rightPos - mRightWidth;

		final int parentTop = getPaddingTop();
		final int parentBottom = bottom - top - getPaddingBottom();

		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != GONE) {
				final LayoutParams lp = (LayoutParams) child.getLayoutParams();

				final int width = child.getMeasuredWidth();
				final int height = child.getMeasuredHeight();

				if (lp.position == LayoutParams.POSITION_LEFT) {
					mTmpContainerRect.left = leftPos + lp.leftMargin;
					mTmpContainerRect.right = leftPos + width + lp.rightMargin;
					leftPos = mTmpContainerRect.right;
				} else if (lp.position == LayoutParams.POSITION_RIGHT) {
					mTmpContainerRect.right = rightPos - lp.rightMargin;
					mTmpContainerRect.left = rightPos - width - lp.leftMargin;
					rightPos = mTmpContainerRect.left;
				} else {
					mTmpContainerRect.left = middleLeft + lp.leftMargin;
					mTmpContainerRect.right = middleRight - lp.rightMargin;
				}
				mTmpContainerRect.top = parentTop + lp.topMargin;
				mTmpContainerRect.bottom = parentBottom - lp.bottomMargin;

				Gravity.apply(lp.gravity, width, height, mTmpContainerRect, mTmpChildRect);

				child.layout(mTmpChildRect.left + xPos, mTmpChildRect.top + yPos, mTmpChildRect.right + xPos,
						mTmpChildRect.bottom + yPos);
			}
		}
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
	}

	@Override
	protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		return new LayoutParams(p);
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof LayoutParams;
	}

	
	static class LayoutParams extends MarginLayoutParams {
		
		public int gravity = Gravity.TOP | Gravity.START;

		public static int POSITION_MIDDLE = 0;
		public static int POSITION_LEFT = 1;
		public static int POSITION_RIGHT = 2;

		public int position = POSITION_MIDDLE;

		public LayoutParams(int width, int height) {
			super(width, height);
		}

		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
		}
	}
}