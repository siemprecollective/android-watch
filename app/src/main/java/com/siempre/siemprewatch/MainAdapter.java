package com.siempre.siemprewatch;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Wearable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class MainAdapter extends RecyclerView.Adapter<MainAdapter.MyViewHolder> {
    private static final String TAG = "MainAdapter";
    private static final String MAKE_CALL = "/make_call";
    private static final String DISCONNECT = "/disconnect";
    private ArrayList<DataModel> mDataset;
    private MainActivity mMainActivity;

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public View view;
        public MyViewHolder (View v) {
            super(v);
            view = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MainAdapter(ArrayList<DataModel> myDataset, MainActivity homeActivity) {
        mDataset = myDataset;
        mMainActivity = homeActivity;
    }

    @Override
    public int getItemViewType(int position) {
        return mDataset.get(position).type;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MainAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
        Log.d(TAG, "in onCreateViewHolder with viewType: " + viewType);
        // create a new view
        View v;
        /*
        if (viewType == 0) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.friend_listview, parent, false);
        }*/

        v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.friend_listview, parent, false);

        final MyViewHolder vh = new MyViewHolder(v);
        if (viewType == 0) {
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mMainActivity.vibrate(2);
                    final int position = vh.getAdapterPosition();
                    String toCallId = mDataset.get(position).id;
                    mMainActivity.onClickFriend(toCallId);
                }
            });
        }
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Log.d(TAG, "in onBindViewHolder with name: " + mDataset.get(position).name);
        Log.d(TAG, "in onBindViewHolder with type: " + mDataset.get(position).type);
        Log.d(TAG, "in onBindViewHolder with photoURL: " + ((User)mDataset.get(position)).photoURL);

        if (getItemViewType(position) == 0) {
            User user = (User) mDataset.get(position);
            FloatingActionButton myImageButton = holder.view.findViewById(R.id.imageButton);
            if (user.photoURL != null) {
                Picasso.get().load(user.photoURL).into(myImageButton);
            }
            if (user.inCall) {
                myImageButton.setBackgroundTintList(ColorStateList.valueOf(Constants.colors[3]));
            } else {
                myImageButton.setBackgroundTintList(ColorStateList.valueOf(Constants.colors[((User) mDataset.get(position)).status]));
            }
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
