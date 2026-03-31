package com.example.fitlife.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fitlife.R;
import com.example.fitlife.activities.DetailActivity;
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

public class CalendarFragment extends Fragment {

    private CalendarView calendarView;
    private TextView tvSelectedDate;
    private TextView tvCreatedCount;
    private TextView tvCompletedCount;
    private TextView tvCompletionRate;
    private RecyclerView rvDayTasks;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private ListenerRegistration listenerRegistration;

    private long startOfDay;
    private long endOfDay;
    private final List<Workout> allWorkouts = new ArrayList<>();
    private final List<Workout> dayWorkouts = new ArrayList<>();
    private WorkoutAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        calendarView = view.findViewById(R.id.calendarView);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        tvCreatedCount = view.findViewById(R.id.tvCreatedCount);
        tvCompletedCount = view.findViewById(R.id.tvCompletedCount);
        tvCompletionRate = view.findViewById(R.id.tvCompletionRate);
        rvDayTasks = view.findViewById(R.id.rvDayTasks);

        adapter = new WorkoutAdapter(dayWorkouts, new WorkoutAdapter.OnWorkoutActionListener() {
            @Override
            public void onWorkoutClick(Workout workout) {
                if (getContext() == null) return;
                Intent intent = new Intent(getContext(), DetailActivity.class);
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
        });

        rvDayTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDayTasks.setAdapter(adapter);

        setSelectedDay(calendarView.getDate());
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            setSelectedDay(cal.getTimeInMillis());
            updateForSelectedDay();
        });

        startListener();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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
                    if (snap == null || err != null) {
                        allWorkouts.clear();
                        updateForSelectedDay();
                        return;
                    }

                    allWorkouts.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        Workout workout = doc.toObject(Workout.class);
                        if (workout == null) continue;

                        workout.setDocumentId(doc.getId());
                        Long ts = doc.getLong("timestamp");
                        if (ts != null) {
                            workout.setTimestamp(ts);
                        }
                        Boolean completedBool = doc.getBoolean("completed");
                        if (completedBool != null) {
                            workout.setCompleted(completedBool);
                        }
                        allWorkouts.add(workout);
                    }

                    updateForSelectedDay();
                });
    }

    private void setSelectedDay(long selectedMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedMillis);
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

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvSelectedDate.setText(sdf.format(startOfDay));
    }

    private void updateForSelectedDay() {
        dayWorkouts.clear();
        int created = 0;
        int completed = 0;

        for (Workout w : allWorkouts) {
            long ts = w.getTimestamp();
            if (ts < startOfDay || ts > endOfDay) continue;
            dayWorkouts.add(w);
            created += 1;
            if (w.isCompleted()) completed += 1;
        }

        Collections.sort(dayWorkouts, Comparator.comparingLong(Workout::getTimestamp).reversed());
        adapter.notifyDataSetChanged();

        tvCreatedCount.setText(String.valueOf(created));
        tvCompletedCount.setText(String.valueOf(completed));
        int percent = created > 0 ? (completed * 100 / created) : 0;
        tvCompletionRate.setText("Completion rate: " + percent + "%");
    }
}
