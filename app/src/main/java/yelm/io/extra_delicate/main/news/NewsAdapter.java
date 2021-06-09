package yelm.io.extra_delicate.main.news;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

import yelm.io.extra_delicate.databinding.NewsItemBinding;
import yelm.io.extra_delicate.rest.query.RestMethods;
import yelm.io.extra_delicate.support_stuff.GradientTransformation;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsHolder> {

    private Context context;
    private List<News> news;
    //boolean isMoving = false;

    public NewsAdapter(Context context, List<News> news) {
        this.context = context;
        this.news = news;
    }

    @NonNull
    @Override
    public NewsAdapter.NewsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NewsHolder(NewsItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull NewsAdapter.NewsHolder holder, int position) {
        News currentNews = news.get(position);
        //holder.binding.name.setText(currentNews.getTitle());
       // if (currentNews.getTitle().trim().isEmpty()) {
            Picasso.get()
                    .load(currentNews.getPreviewImage())
                    .noPlaceholder()
                    .centerCrop()
                    .resize(400, 0)
                    .into(holder.binding.image);
//        } else {
//            Picasso.get()
//                    .load(currentNews.getPreviewImage())
//                    .noPlaceholder()
//                    .centerCrop()
//                    .resize(400, 0)
//                    .transform(new GradientTransformation(context))
//                    .into(holder.binding.image);
//        }

        holder.binding.image.setOnClickListener(v -> {
            RestMethods.sendStatistic("open_news");
            Intent intent = new Intent(context, NewsActivity.class);
            intent.putExtra("news", currentNews);
            context.startActivity(intent);
        });




//        AnimatorSet animationSet = new AnimatorSet();
//        ObjectAnimator scaleY = ObjectAnimator.ofFloat(holder.binding.news, "scaleY", 1f, 0.9f);
//        ObjectAnimator scaleX = ObjectAnimator.ofFloat(holder.binding.news, "scaleX", 1f, 0.9f);
//        animationSet.setDuration(200).playTogether(scaleX, scaleY);

//
//        holder.binding.image.setOnTouchListener((view, motionEvent) -> {
//            Log.d(Logging.debug, "onTouch");
//            switch (motionEvent.getAction()) {
//                case MotionEvent.ACTION_DOWN:
//                    Log.d(Logging.debug, "down");
//                    animationSet.start();
//                    isMoving = false;
//                    return true;
//                case MotionEvent.ACTION_MOVE:
//                    Log.d(Logging.debug, "move");
//                    isMoving = true;
//                    break;
//                case MotionEvent.ACTION_UP:
//                    Log.d(Logging.debug, "up");
//                    animationSet.cancel();
//                    if (!isMoving) {
//                        RestMethods.sendStatistic("open_news");
//                        Intent intent = new Intent(context, NewsActivity.class);
//                        intent.putExtra("news", currentNews);
//                        context.startActivity(intent);
//                    }
//                    AnimatorSet animationSet1 = new AnimatorSet();
//                    ObjectAnimator scaleY1 = ObjectAnimator.ofFloat(holder.binding.news, "scaleY", holder.binding.news.getScaleY(), 1.0f);
//                    ObjectAnimator scaleX1 = ObjectAnimator.ofFloat(holder.binding.news, "scaleX", holder.binding.news.getScaleX(), 1.0f);
//                    animationSet1.setDuration(200).playTogether(scaleX1, scaleY1);
//                    animationSet1.start();
//                    break;
//                case MotionEvent.ACTION_CANCEL:
//                    Log.d(Logging.debug, "cancel");
//                    animationSet.cancel();
////                        if (!isMoving) {
////                            RestMethods.sendStatistic("open_news");
////                            Intent intent = new Intent(context, NewsActivity.class);
////                            intent.putExtra("news", currentNews);
////                            intent.putParcelableArrayListExtra("items", (ArrayList<? extends Parcelable>) currentNews.getItems());
////                            context.startActivity(intent);
////                        }
//                    AnimatorSet animationSet2 = new AnimatorSet();
//                    ObjectAnimator scaleY2 = ObjectAnimator.ofFloat(holder.binding.news, "scaleY", holder.binding.news.getScaleY(), 1.0f);
//                    ObjectAnimator scaleX2 = ObjectAnimator.ofFloat(holder.binding.news, "scaleX", holder.binding.news.getScaleX(), 1.0f);
//                    animationSet2.setDuration(200).playTogether(scaleX2, scaleY2);
//                    animationSet2.start();
//                    break;
//            }
//            return false;
//        });
//
//

    }

    @Override
    public int getItemCount() {
        return news.size();
    }

    public static class NewsHolder extends RecyclerView.ViewHolder {
        private NewsItemBinding binding;

        public NewsHolder(NewsItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}