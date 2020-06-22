package com.kyser.audiostreamdemo;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class SpaceItemDecoration extends RecyclerView.ItemDecoration {

    private final int verticalSpaceHeight;
    private final int horizontalSpaceWidth;

    public SpaceItemDecoration(int verticalSpaceHeight,int horizontalSpaceWidth) {
        this.verticalSpaceHeight = verticalSpaceHeight;
        this.horizontalSpaceWidth= horizontalSpaceWidth;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.top = verticalSpaceHeight;
        outRect.bottom = verticalSpaceHeight;
        outRect.left = horizontalSpaceWidth;
        outRect.right = horizontalSpaceWidth;
    }
}
