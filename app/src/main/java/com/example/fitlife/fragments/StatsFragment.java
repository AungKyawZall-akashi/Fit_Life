package com.example.fitlife.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fitlife.R;
import com.example.fitlife.activities.DayDetailActivity;
import com.example.fitlife.models.Workout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StatsFragment extends Fragment {

    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    private TextView tvMonthTitle;
    private TextView tvCreatedCount;
    private TextView tvCompletedCount;
    private TextView tvCompletionRate;
    private TextView tvUntracked;
    private RecyclerView rvMonthlyStats;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private ListenerRegistration listenerRegistration;

    private int currentYear;
    private int currentMonthZeroBased;
    private DayStatsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        tvMonthTitle = view.findViewById(R.id.tvMonthTitle);
        tvCreatedCount = view.findViewById(R.id.tvCreatedCount);
        tvCompletedCount = view.findViewById(R.id.tvCompletedCount);
        tvCompletionRate = view.findViewById(R.id.tvCompletionRate);
        tvUntracked = view.findViewById(R.id.tvUntracked);
        rvMonthlyStats = view.findViewById(R.id.rvMonthlyStats);

        Calendar cal = Calendar.getInstance();
        currentYear = cal.get(Calendar.YEAR);
        currentMonthZeroBased = cal.get(Calendar.MONTH);

        adapter = new DayStatsAdapter();
        rvMonthlyStats.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMonthlyStats.setAdapter(adapter);

        btnPrevMonth.setOnClickListener(v -> moveMonth(-1));
        btnNextMonth.setOnClickListener(v -> moveMonth(1));

        refreshMonth();

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

    private void moveMonth(int delta) {
        int newMonth = currentMonthZeroBased + delta;
        int newYear = currentYear;
        if (newMonth < 0) {
            newMonth = 11;
            newYear -= 1;
        } else if (newMonth > 11) {
            newMonth = 0;
            newYear += 1;
        }
        currentMonthZeroBased = newMonth;
        currentYear = newYear;
        refreshMonth();
    }

    private void refreshMonth() {
        updateMonthTitle();
        loadMonthlyStats();
    }

    private void updateMonthTitle() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, currentYear);
        cal.set(Calendar.MONTH, currentMonthZeroBased);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthTitle.setText(sdf.format(cal.getTime()));
    }

    private void loadMonthlyStats() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }

        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";

        Calendar startCal = Calendar.getInstance();
        startCal.set(Calendar.YEAR, currentYear);
        startCal.set(Calendar.MONTH, currentMonthZeroBased);
        startCal.set(Calendar.DAY_OF_MONTH, 1);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        long startOfMonth = startCal.getTimeInMillis();

        Calendar endCal = (Calendar) startCal.clone();
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        long endOfMonth = endCal.getTimeInMillis();

        int daysInMonth = endCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        listenerRegistration = firestore.collection("workouts")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) {
                        setSummary(0, 0, 0);
                        adapter.setItems(new ArrayList<>(), 0, null);
                        return;
                    }

                    int[] createdByDay = new int[daysInMonth];
                    int[] completedByDay = new int[daysInMonth];
                    int totalCreated = 0;
                    int totalCompleted = 0;
                    int untracked = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        Workout workout = doc.toObject(Workout.class);
                        if (workout == null) continue;

                        Long ts = doc.getLong("timestamp");
                        long timestamp = ts != null ? ts : workout.getTimestamp();
                        if (timestamp <= 0) {
                            untracked += 1;
                            continue;
                        }
                        if (timestamp < startOfMonth || timestamp > endOfMonth) continue;

                        Calendar dayCal = Calendar.getInstance();
                        dayCal.setTimeInMillis(timestamp);
                        int day = dayCal.get(Calendar.DAY_OF_MONTH);
                        int index = day - 1;
                        if (index < 0 || index >= daysInMonth) continue;

                        createdByDay[index] += 1;
                        totalCreated += 1;

                        Boolean completedBool = doc.getBoolean("completed");
                        boolean completed = completedBool != null ? completedBool : workout.isCompleted();

                        if (completed) {
                            completedByDay[index] += 1;
                            totalCompleted += 1;
                        }
                    }

                    List<DayStatItem> items = new ArrayList<>(daysInMonth);
                    int maxCreated = 0;
                    for (int i = 0; i < daysInMonth; i++) {
                        maxCreated = Math.max(maxCreated, createdByDay[i]);
                    }
                    int maxScale = Math.max(1, maxCreated);

                    for (int i = 0; i < daysInMonth; i++) {
                        if (createdByDay[i] > 0) {
                            items.add(new DayStatItem(i + 1, createdByDay[i], completedByDay[i]));
                        }
                    }

                    setSummary(totalCreated, totalCompleted, untracked);
                    adapter.setItems(items, maxScale, day -> {
                        if (getContext() == null) return;
                        Intent intent = new Intent(getContext(), DayDetailActivity.class);
                        intent.putExtra(DayDetailActivity.EXTRA_YEAR, currentYear);
                        intent.putExtra(DayDetailActivity.EXTRA_MONTH_ZERO_BASED, currentMonthZeroBased);
                        intent.putExtra(DayDetailActivity.EXTRA_DAY_OF_MONTH, day);
                        startActivity(intent);
                    });
                });
    }

    private void setSummary(int created, int completed, int untracked) {
        tvCreatedCount.setText(String.valueOf(created));
        tvCompletedCount.setText(String.valueOf(completed));
        int percent = created > 0 ? (completed * 100 / created) : 0;
        tvCompletionRate.setText("Completion rate: " + percent + "%");
        tvUntracked.setText("Untracked (no date): " + untracked);
    }

    private static class DayStatItem {
        final int day;
        final int created;
        final int completed;

        DayStatItem(int day, int created, int completed) {
            this.day = day;
            this.created = created;
            this.completed = completed;
        }
    }

    private static class DayStatsAdapter extends RecyclerView.Adapter<DayStatsAdapter.VH> {
        private final List<DayStatItem> items = new ArrayList<>();
        private int maxScale = 1;
        private OnDayClickListener onDayClickListener;

        void setItems(List<DayStatItem> newItems, int maxScale, OnDayClickListener onDayClickListener) {
            items.clear();
            items.addAll(newItems);
            this.maxScale = Math.max(1, maxScale);
            this.onDayClickListener = onDayClickListener;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_month_day_stat, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DayStatItem item = items.get(position);

            holder.tvDay.setText(String.valueOf(item.day));
            holder.tvCreated.setText(String.valueOf(item.created));
            holder.tvCompleted.setText(String.valueOf(item.completed));

            holder.createdBarContainer.setWeightSum(maxScale);
            holder.completedBarContainer.setWeightSum(maxScale);

            LinearLayout.LayoutParams createdLp = (LinearLayout.LayoutParams) holder.createdBar.getLayoutParams();
            createdLp.weight = item.created;
            holder.createdBar.setLayoutParams(createdLp);

            LinearLayout.LayoutParams completedLp = (LinearLayout.LayoutParams) holder.completedBar.getLayoutParams();
            completedLp.weight = item.completed;
            holder.completedBar.setLayoutParams(completedLp);

            holder.itemView.setOnClickListener(v -> {
                if (onDayClickListener != null) {
                    onDayClickListener.onDayClick(item.day);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView tvDay;
            final TextView tvCreated;
            final TextView tvCompleted;
            final LinearLayout createdBarContainer;
            final LinearLayout completedBarContainer;
            final View createdBar;
            final View completedBar;

            VH(@NonNull View itemView) {
                super(itemView);
                tvDay = itemView.findViewById(R.id.tvDay);
                tvCreated = itemView.findViewById(R.id.tvCreated);
                tvCompleted = itemView.findViewById(R.id.tvCompleted);
                createdBarContainer = itemView.findViewById(R.id.createdBarContainer);
                completedBarContainer = itemView.findViewById(R.id.completedBarContainer);
                createdBar = itemView.findViewById(R.id.createdBar);
                completedBar = itemView.findViewById(R.id.completedBar);
            }
        }
    }

    private interface OnDayClickListener {
        void onDayClick(int day);
    }
}
