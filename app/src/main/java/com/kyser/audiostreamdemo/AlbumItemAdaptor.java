package com.kyser.audiostreamdemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.spotify.protocol.types.ListItem;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class AlbumItemAdaptor extends RecyclerView.Adapter<AlbumItemAdaptor.ViewHolder> {
    private Context mContext;
    ListItem[] mMediaList;
    private ItemSelection mItemSelection;
    public interface ItemSelection{ void onItemSelection(ListItem MediaList, int position);}

    public AlbumItemAdaptor(Context mContext, ItemSelection mItemSelection) {
        this.mContext = mContext;
        this.mItemSelection = mItemSelection;
    }
    public void setAlbumList( ListItem[] mediaLis ){
        this.mMediaList = mediaLis;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.listing_album_item,parent,false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.title.setText(mMediaList[position].title);
        String b_url = mMediaList[position].imageUri.raw;
        Glide.with(holder.itemView)
                .load(b_url)
                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .into(holder.poster);

        holder.itemView.setOnClickListener(v -> mItemSelection.onItemSelection(mMediaList[position],position));
    }

    @Override
    public int getItemCount() {
        return mMediaList!=null?mMediaList.length:0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView title ;
        ImageView poster;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.album_title);
            poster = (ImageView) itemView.findViewById(R.id.album_poster);
        }
    }
}
