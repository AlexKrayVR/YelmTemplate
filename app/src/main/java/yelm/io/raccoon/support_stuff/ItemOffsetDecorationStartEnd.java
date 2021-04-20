package yelm.io.raccoon.support_stuff;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

public class ItemOffsetDecorationStartEnd extends RecyclerView.ItemDecoration {

    private int offsetStart;
    private int offsetEnd;

    public ItemOffsetDecorationStartEnd(int offsetStart, int offsetEnd) {
        this.offsetStart = offsetStart;
        this.offsetEnd = offsetEnd;
    }

    @Override
    public void getItemOffsets(@NotNull Rect outRect,@NotNull View view,
                               RecyclerView parent, RecyclerView.State state) {

        if (parent.getChildAdapterPosition(view) == (state.getItemCount()-1)) {
            outRect.right = offsetEnd;
        }

        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.left = offsetStart;
        }
    }
}