package yelm.io.extra_delicate.support_stuff;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

public class ItemOffsetDecorationRight extends RecyclerView.ItemDecoration {

    private int offset;

    public ItemOffsetDecorationRight(int offset) {
        this.offset = offset;
    }

    @Override
    public void getItemOffsets(@NotNull Rect outRect,@NotNull View view,
                               RecyclerView parent, RecyclerView.State state) {
        if (parent.getChildAdapterPosition(view) == (state.getItemCount()-1)) {
            outRect.right = offset;
        }
    }
}