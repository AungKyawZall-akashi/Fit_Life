package com.example.fitlife.adapters;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fitlife.R;
import com.example.fitlife.models.Workout;
import java.util.List;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder> {

    private List<Workout> workoutList;
    private OnWorkoutActionListener actionListener;

    public interface OnWorkoutActionListener {
        void onWorkoutClick(Workout workout);
        void onWorkoutDelete(Workout workout, int position);
        void onWorkoutComplete(Workout workout, boolean isCompleted);
    }

    public WorkoutAdapter(List<Workout> workoutList, OnWorkoutActionListener actionListener) {
        this.workoutList = workoutList;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout, parent, false);
        return new WorkoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        Workout workout = workoutList.get(position);
        holder.titleTextView.setText(workout.getTitle());
        holder.durationTextView.setText(workout.getDuration() + " mins");
        
        // Set dynamic icon
        setWorkoutIcon(holder.ivWorkoutIcon, workout.getTitle());

        // Handle completion status
        holder.cbCompleted.setChecked(workout.isCompleted());
        updateViewStyle(holder, workout.isCompleted());

        holder.cbCompleted.setOnClickListener(v -> {
            boolean isChecked = holder.cbCompleted.isChecked();
            workout.setCompleted(isChecked);
            updateViewStyle(holder, isChecked);
            if (actionListener != null) {
                actionListener.onWorkoutComplete(workout, isChecked);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onWorkoutClick(workout);
            }
        });
    }

    private void setWorkoutIcon(ImageView imageView, String title) {
        String lowerTitle = title.toLowerCase();
        if (lowerTitle.contains("yoga")) {
            imageView.setImageResource(R.drawable.ic_yoga);
        } else if (lowerTitle.contains("run")) {
            imageView.setImageResource(R.drawable.ic_run);
        } else if (lowerTitle.contains("weight") || lowerTitle.contains("lift")) {
            imageView.setImageResource(R.drawable.ic_weights);
        } else if (lowerTitle.contains("swim")) {
            imageView.setImageResource(R.drawable.ic_swim);
        } else {
            imageView.setImageResource(R.drawable.ic_run); // default
        }
    }

    private void updateViewStyle(WorkoutViewHolder holder, boolean isCompleted) {
        if (isCompleted) {
            holder.titleTextView.setPaintFlags(holder.titleTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.titleTextView.setTextColor(holder.itemView.getContext().getColor(R.color.text_secondary));
            holder.durationTextView.setTextColor(holder.itemView.getContext().getColor(R.color.text_secondary));
        } else {
            holder.titleTextView.setPaintFlags(holder.titleTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.titleTextView.setTextColor(holder.itemView.getContext().getColor(R.color.text_primary));
            holder.durationTextView.setTextColor(holder.itemView.getContext().getColor(R.color.text_secondary));
        }
    }

    @Override
    public int getItemCount() {
        return workoutList != null ? workoutList.size() : 0;
    }

    public static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView durationTextView;
        CheckBox cbCompleted;
        ImageView ivWorkoutIcon;

        public WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.textViewTitle);
            durationTextView = itemView.findViewById(R.id.textViewDuration);
            cbCompleted = itemView.findViewById(R.id.cbCompleted);
            ivWorkoutIcon = itemView.findViewById(R.id.ivWorkoutIcon);
        }
    }
}
