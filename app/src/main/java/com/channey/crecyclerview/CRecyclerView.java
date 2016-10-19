package com.channey.crecyclerview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by channey on 2016/10/18.
 */

public class CRecyclerView extends RecyclerView {
    private static final String TAG = "CRecyclerView";
    private CAdapter mAdapter;
    private int headerRefreshHeight;
    private float startY;
    private final int STATE_PULL_UP_REFRESH = 0;
    private final int STATE_LOADING = 1;
    private final int STATE_DROP_DOWN_REFRESH = 2;
    private int currentState = STATE_PULL_UP_REFRESH;
    private float ranY = 1.5f;
    private int currentDist = 0;
    private ValueAnimator animator_hide_header;
    private int firstCompletelyVisibleItemPosition;
    private OnRefreshListener mOnRefreshListener;
    boolean hasInit = false;

    public void setOnRefreshListener(OnRefreshListener listener) {
        this.mOnRefreshListener = listener;
    }


    public void setRefreshAble(boolean refreshAble) {
        this.refreshAble = refreshAble;
    }

    private boolean refreshAble = true;

    public CRecyclerView(Context context) {
        this(context, null, 0);
    }

    public CRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Adapter adapter = getAdapter();
        if (!(adapter instanceof CAdapter)) {
            throw new IllegalArgumentException("the adapter must extents BaseRefreshRecyclerViewAdapter");
        }
        mAdapter = (CAdapter) adapter;
        mAdapter.setFooterClickListener(new CAdapter.FooterClickListener() {
            @Override
            public void onFooterClick() {
                onLoadMore();
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!refreshAble) {
            return super.onTouchEvent(e);
        }
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = e.getY();
                headerRefreshHeight = mAdapter.getHeaderRefreshHeight();
                break;
            case MotionEvent.ACTION_MOVE:
                if (currentState == STATE_LOADING) {
                    break;
                }
                float tmpY = e.getY();
                if (currentState == STATE_PULL_UP_REFRESH) {
                    if ((tmpY - startY) / ranY <= this.headerRefreshHeight) {
                        currentDist = (int) ((tmpY - startY) / ranY);
                        mAdapter.setHeaderPadding((int) ((tmpY - startY) / ranY - this.headerRefreshHeight));
                        initAnimationHideHeader();
                    } else if (firstCompletelyVisibleItemPosition >= 0 && firstCompletelyVisibleItemPosition <= 1) {
                        currentState = STATE_DROP_DOWN_REFRESH;
                        changeWightState();
                    }
                }
                if (currentState == STATE_DROP_DOWN_REFRESH) {
                    changeWightState();
                    currentDist = (int) ((tmpY - startY) / ranY - this.headerRefreshHeight);
                    mAdapter.setHeaderPadding(currentDist);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (currentState == STATE_LOADING) {
                    break;
                }
                if (currentState == STATE_PULL_UP_REFRESH) {
                    if (animator_hide_header == null) {
                        initAnimationHideHeader();
                    }
                    animator_hide_header.start();
                }
                if (currentState == STATE_DROP_DOWN_REFRESH) {
                    currentState = STATE_LOADING;
                    changeWightState();
                    View view = getLayoutManager().getChildAt(0);
                    if (view.getTop() <= 5) {
                        onRefresh();
                        initAnimaionRelasetoRefresh();
                    } else {
                        currentDist = -view.getTop();
                        animator_hide_header.start();
                        currentState = STATE_PULL_UP_REFRESH;
                    }

                }
                break;
        }
        return super.onTouchEvent(e);

    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        LayoutManager layoutManager = getLayoutManager();
        int lastVisibleItemPosition = 0;
        if (layoutManager instanceof LinearLayoutManager) {
            lastVisibleItemPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
            firstCompletelyVisibleItemPosition = ((LinearLayoutManager) layoutManager).findFirstCompletelyVisibleItemPosition();
        }
        if (layoutManager instanceof GridLayoutManager) {
            lastVisibleItemPosition = ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
            firstCompletelyVisibleItemPosition = ((GridLayoutManager) layoutManager).findFirstCompletelyVisibleItemPosition();
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            int[] last = null;
            int[] first = null;
            if (!hasInit) {
                last = new int[((StaggeredGridLayoutManager) layoutManager).getSpanCount()];
                first = new int[last.length];
                hasInit = true;
            }
            int[] lastVisibleItemPositions = ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(last);
            int[] firstCompletelyVisibleItemPositions = ((StaggeredGridLayoutManager) layoutManager).findFirstCompletelyVisibleItemPositions(first);
            firstCompletelyVisibleItemPosition = firstCompletelyVisibleItemPositions[0];
            for (int i : lastVisibleItemPositions) {
                lastVisibleItemPosition = i > lastVisibleItemPosition ? i : lastVisibleItemPosition;
            }
        }
        if (lastVisibleItemPosition == mAdapter.getItemCount() - 1) {
            mAdapter.setFooterVisible(true);
            layoutManager.scrollToPosition(mAdapter.getItemCount());
            onLoadMore();
        }
    }


    private void initAnimaionRelasetoRefresh() {
        ValueAnimator animator_relase_torefresh = ValueAnimator.ofInt(currentDist, 0);
        animator_relase_torefresh.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mAdapter.setHeaderPadding((Integer) valueAnimator.getAnimatedValue());
            }
        });
        animator_relase_torefresh.setDuration(400);
        animator_relase_torefresh.start();
    }

    private void initAnimationRefreshOver() {
        ValueAnimator animator_refresh_over = ValueAnimator.ofInt(0, -headerRefreshHeight);
        animator_refresh_over.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mAdapter.setHeaderPadding((Integer) valueAnimator.getAnimatedValue());
            }
        });
        animator_refresh_over.setDuration(200);
        animator_refresh_over.start();
    }

    private void initAnimationHideHeader() {
        animator_hide_header = ValueAnimator.ofInt(-currentDist, -headerRefreshHeight);
        animator_hide_header.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mAdapter.setHeaderPadding((Integer) valueAnimator.getAnimatedValue());
            }
        });
        animator_hide_header.setDuration(100);
    }

    private void onRefresh() {
        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
    }

    public void completeRefresh() {
        changeWightState();
        initAnimationRefreshOver();
        currentState = STATE_PULL_UP_REFRESH;
        mAdapter.setHeaderState(0);
    }

    public void completeLoadMore() {
        mAdapter.setFooterVisible(false);
    }

    private void onLoadMore() {
        if (mOnRefreshListener != null) {
            mOnRefreshListener.onLoadMore();
        }
    }

    private void changeWightState() {
        switch (currentState) {
            case STATE_PULL_UP_REFRESH:
                mAdapter.setHeaderState(0);
                break;
            case STATE_DROP_DOWN_REFRESH:
                mAdapter.setHeaderState(1);
                break;
            case STATE_LOADING:
                mAdapter.setHeaderState(2);
                break;
        }
    }

    public interface OnRefreshListener {
        void onRefresh();

        void onLoadMore();
    }
}
