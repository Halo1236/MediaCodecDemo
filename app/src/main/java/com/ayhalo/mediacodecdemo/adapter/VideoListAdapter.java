package com.ayhalo.mediacodecdemo.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ayhalo.mediacodecdemo.R;
import com.ayhalo.mediacodecdemo.bean.VideoBean;

import java.util.ArrayList;

/**
 * MediaCodecDemo
 * Created by Halo on 2017/9/20.
 */

public class VideoListAdapter extends RecyclerView.Adapter<VideoListAdapter.ViewHolder>{

    private ArrayList<VideoBean> videoNameList;
    private LayoutInflater mlayoutInflater;
    private OnItemClickListener mOnItemClickListener;

    public VideoListAdapter(Context context, ArrayList<VideoBean> List){
        videoNameList = List;
        mlayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mlayoutInflater.inflate(R.layout.video_item_layout,parent,false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        VideoBean videoBean = videoNameList.get(position);
        holder.textView.setText(videoBean.getName());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnItemClickListener.onItemClick(v,position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return videoNameList.size();
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;
        private TextView textView;
        public ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView)itemView.findViewById(R.id.image_video);
            textView = (TextView)itemView.findViewById(R.id.video_name);
        }
    }

    public interface OnItemClickListener
    {
        void onItemClick(View view, int position);
        void onItemLongClick(View view,int position);
    }
    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener)
    {
        this.mOnItemClickListener = mOnItemClickListener;
    }
}
