package com.example.fitlife.fragments;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fitlife.R;
import com.example.fitlife.activities.AddWorkoutActivity;
import com.example.fitlife.activities.DetailActivity;
import com.example.fitlife.adapters.WorkoutAdapter;
import com.example.fitlife.models.Workout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements WorkoutAdapter.OnWorkoutActionListener {

    private RecyclerView rvWorkouts;
    private TextView tvEmptyState, tvAddWorkout, tvWelcomeName;
    private TextView tvStatsPercent, tvStatsCount;
    private ProgressBar pbStats;
    private WorkoutAdapter adapter;
    private List<Workout> workoutList;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private ListenerRegistration workoutListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvWorkouts = view.findViewById(R.id.rvWorkouts);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        tvAddWorkout = view.findViewById(R.id.tvAddWorkout);
        tvWelcomeName = view.findViewById(R.id.tvWelcomeName);
        
        // Stats views
        pbStats = view.findViewById(R.id.pbStats);
        tvStatsPercent = view.findViewById(R.id.tvStatsPercent);
        tvStatsCount = view.findViewById(R.id.tvStatsCount);

        workoutList = new ArrayList<>();
        setupRecyclerView();
        setupSwipeGestures();
        loadUserProfile();
        startWorkoutListener(); // Start listening for real-time updates

        tvAddWorkout.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddWorkoutActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (workoutListener != null) {
            workoutListener.remove();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        filterWorkoutsToToday();
        checkEmptyState();
    }

    private void startWorkoutListener() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";
        
        workoutListener = firestore.collection("workouts")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    if (value != null) {
                        long[] todayRange = getTodayRangeMillis();
                        long startOfDay = todayRange[0];
                        long endOfDay = todayRange[1];

                        workoutList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Workout workout = doc.toObject(Workout.class);
                            workout.setDocumentId(doc.getId());
                            Long timestamp = doc.getLong("timestamp");
                            if (timestamp != null) {
                                workout.setTimestamp(timestamp);
                            }
                            long ts = workout.getTimestamp();
                            if (ts >= startOfDay && ts <= endOfDay) {
                                workoutList.add(workout);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        checkEmptyState();
                    }
                });
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            firestore.collection("users").document(user.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String username = document.getString("username");
                                if (username != null && !username.isEmpty()) {
                                    if (!isAdded()) return;
                                    
                                    String fullText = "Hello, " + username + "!";
                                    SpannableString spannable = new SpannableString(fullText);
                                    int start = fullText.indexOf(username);
                                    if (start >= 0) {
                                        int end = start + username.length();
                                        if (getContext() != null) {
                                            spannable.setSpan(new ForegroundColorSpan(getContext().getColor(R.color.accent_orange)), 
                                                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                        }
                                    }
                                    tvWelcomeName.setText(spannable);
                                }
                            }
                        }
                    });
        }
    }

    private void setupSwipeGestures() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Workout workout = workoutList.get(position);

                if (direction == ItemTouchHelper.RIGHT) {
                    showDeleteConfirmation(workout, position);
                } else if (direction == ItemTouchHelper.LEFT) {
                    boolean newStatus = !workout.isCompleted();
                    onWorkoutComplete(workout, newStatus);
                    workout.setCompleted(newStatus);
                    adapter.notifyItemChanged(position);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                    return;
                }

                View itemView = viewHolder.itemView;
                int itemHeight = itemView.getBottom() - itemView.getTop();
                Paint paint = new Paint();

                if (dX > 0) {
                    paint.setColor(Color.parseColor("#EF4444"));
                    RectF background = new RectF(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + dX, itemView.getBottom());
                    c.drawRect(background, paint);

                    android.graphics.drawable.Drawable icon = ContextCompat.getDrawable(itemView.getContext(), android.R.drawable.ic_menu_delete);
                    if (icon != null) {
                        icon.setTint(Color.WHITE);
                        int iconMargin = (itemHeight - icon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + iconMargin;
                        int iconLeft = itemView.getLeft() + iconMargin;
                        int iconRight = iconLeft + icon.getIntrinsicWidth();
                        int iconBottom = iconTop + icon.getIntrinsicHeight();
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        icon.draw(c);
                    }
                } else if (dX < 0) {
                    paint.setColor(Color.parseColor("#10B981"));
                    RectF background = new RectF(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    c.drawRect(background, paint);

                    android.graphics.drawable.Drawable icon = ContextCompat.getDrawable(itemView.getContext(), android.R.drawable.checkbox_on_background);
                    if (icon != null) {
                        icon.setTint(Color.WHITE);
                        int iconMargin = (itemHeight - icon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + iconMargin;
                        int iconRight = itemView.getRight() - iconMargin;
                        int iconLeft = iconRight - icon.getIntrinsicWidth();
                        int iconBottom = iconTop + icon.getIntrinsicHeight();
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        icon.draw(c);
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rvWorkouts);
    }

    private void showDeleteConfirmation(Workout workout, int position) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Workout")
                .setMessage("Are you sure you want to delete '" + workout.getTitle() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    onWorkoutDelete(workout, position);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    adapter.notifyItemChanged(position);
                })
                .setOnCancelListener(dialog -> {
                    adapter.notifyItemChanged(position);
                })
                .show();
    }

    private void checkEmptyState() {
        if (workoutList.isEmpty()) {
            rvWorkouts.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvWorkouts.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
        updateStatistics();
    }

    private void updateStatistics() {
        if (workoutList == null) return;
        int totalToday = workoutList.size();
        int completedToday = 0;

        for (Workout w : workoutList) {
            if (w.isCompleted()) {
                completedToday++;
            }
        }

        int percent = (totalToday > 0) ? (completedToday * 100 / totalToday) : 0;
        
        if (pbStats != null) {
            pbStats.setProgress(percent);
        }
        if (tvStatsPercent != null) {
            tvStatsPercent.setText(percent + "%");
        }
        if (tvStatsCount != null) {
            tvStatsCount.setText(completedToday + " of " + totalToday + " workouts today");
        }
    }

    private void filterWorkoutsToToday() {
        if (workoutList == null || workoutList.isEmpty() || adapter == null) return;

        long[] todayRange = getTodayRangeMillis();
        long startOfDay = todayRange[0];
        long endOfDay = todayRange[1];

        boolean changed = false;
        for (int i = workoutList.size() - 1; i >= 0; i--) {
            long ts = workoutList.get(i).getTimestamp();
            if (ts < startOfDay || ts > endOfDay) {
                workoutList.remove(i);
                changed = true;
            }
        }

        if (changed) {
            adapter.notifyDataSetChanged();
        }
    }

    private long[] getTodayRangeMillis() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();

        cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
        cal.set(java.util.Calendar.MINUTE, 59);
        cal.set(java.util.Calendar.SECOND, 59);
        cal.set(java.util.Calendar.MILLISECOND, 999);
        long endOfDay = cal.getTimeInMillis();

        return new long[]{startOfDay, endOfDay};
    }

    private void setupRecyclerView() {
        adapter = new WorkoutAdapter(workoutList, this);
        rvWorkouts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvWorkouts.setAdapter(adapter);
    }

    @Override
    public void onWorkoutClick(Workout workout) {
        Intent intent = new Intent(getActivity(), DetailActivity.class);
        intent.putExtra("WORKOUT_DOC_ID", workout.getDocumentId());
        startActivity(intent);
    }

    @Override
    public void onWorkoutDelete(Workout workout, int position) {
        if (!isAdded() || getContext() == null) return;
        
        firestore.collection("workouts").document(workout.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    workoutList.remove(position);
                    adapter.notifyItemRemoved(position);
                    checkEmptyState();
                    Toast.makeText(getContext(), "Workout deleted from cloud", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onWorkoutComplete(Workout workout, boolean isCompleted) {
        if (!isAdded() || getContext() == null) return;

        firestore.collection("workouts").document(workout.getDocumentId())
                .update("completed", isCompleted)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    String status = isCompleted ? "completed" : "pending";
                    Toast.makeText(getContext(), workout.getTitle() + " marked as " + status, Toast.LENGTH_SHORT).show();
                });
    }
}
