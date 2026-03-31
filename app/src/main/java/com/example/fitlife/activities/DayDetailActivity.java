package com.example.fitlife.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fitlife.R;
import com.example.fitlife.adapters.WorkoutAdapter;
import com.example.fitlife.models.Workout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DayDetailActivity extends AppCompatActivity implements WorkoutAdapter.OnWorkoutActionListener {

    public static final String EXTRA_YEAR = "EXTRA_YEAR";
    public static final String EXTRA_MONTH_ZERO_BASED = "EXTRA_MONTH_ZERO_BASED";
    public static final String EXTRA_DAY_OF_MONTH = "EXTRA_DAY_OF_MONTH";

    private ImageView ivBack;
    private TextView tvDayTitle;
    private TextView tvCreatedCount;
    private TextView tvCompletedCount;
    private TextView tvCompletionRate;
    private RecyclerView rvDayTasks;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private ListenerRegistration listenerRegistration;

    private long startOfDay;
    private long endOfDay;
    private final List<Workout> dayWorkouts = new ArrayList<>();
    private WorkoutAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_detail);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        ivBack = findViewById(R.id.ivBack);
        tvDayTitle = findViewById(R.id.tvDayTitle);
        tvCreatedCount = findViewById(R.id.tvCreatedCount);
        tvCompletedCount = findViewById(R.id.tvCompletedCount);
        tvCompletionRate = findViewById(R.id.tvCompletionRate);
        rvDayTasks = findViewById(R.id.rvDayTasks);

        int year = getIntent().getIntExtra(EXTRA_YEAR, -1);
        int month = getIntent().getIntExtra(EXTRA_MONTH_ZERO_BASED, -1);
        int day = getIntent().getIntExtra(EXTRA_DAY_OF_MONTH, -1);

        Calendar cal = Calendar.getInstance();
        if (year > 0 && month >= 0 && day > 0) {
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, day);
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        startOfDay = cal.getTimeInMillis();

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        endOfDay = cal.getTimeInMillis();

        SimpleDateFormat titleFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvDayTitle.setText(titleFormat.format(startOfDay));

        ivBack.setOnClickListener(v -> finish());

        adapter = new WorkoutAdapter(dayWorkouts, this);
        rvDayTasks.setLayoutManager(new LinearLayoutManager(this));
        rvDayTasks.setAdapter(adapter);

        startListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    private void startListener() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }

        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";

        listenerRegistration = firestore.collection("workouts")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) {
                        dayWorkouts.clear();
                        adapter.notifyDataSetChanged();
                        setSummary(0, 0);
                        return;
                    }

                    dayWorkouts.clear();
                    int created = 0;
                    int completed = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        Workout workout = doc.toObject(Workout.class);
                        if (workout == null) continue;

                        Long ts = doc.getLong("timestamp");
                        long timestamp = ts != null ? ts : workout.getTimestamp();
                        if (timestamp < startOfDay || timestamp > endOfDay) continue;

                        workout.setDocumentId(doc.getId());
                        workout.setTimestamp(timestamp);

                        Boolean completedBool = doc.getBoolean("completed");
                        workout.setCompleted(completedBool != null ? completedBool : workout.isCompleted());

                        dayWorkouts.add(workout);
                        created += 1;
                        if (workout.isCompleted()) {
                            completed += 1;
                        }
                    }

                    Collections.sort(dayWorkouts, Comparator.comparingLong(Workout::getTimestamp).reversed());
                    adapter.notifyDataSetChanged();
                    setSummary(created, completed);
                });
    }

    private void setSummary(int created, int completed) {
        tvCreatedCount.setText(String.valueOf(created));
        tvCompletedCount.setText(String.valueOf(completed));
        int percent = created > 0 ? (completed * 100 / created) : 0;
        tvCompletionRate.setText("Completion rate: " + percent + "%");
    }

    @Override
    public void onWorkoutClick(Workout workout) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("WORKOUT_DOC_ID", workout.getDocumentId());
        startActivity(intent);
    }

    @Override
    public void onWorkoutDelete(Workout workout, int position) {
    }

    @Override
    public void onWorkoutComplete(Workout workout, boolean isCompleted) {
        if (workout.getDocumentId() == null) return;
        firestore.collection("workouts").document(workout.getDocumentId())
                .update("completed", isCompleted);
    }
}

