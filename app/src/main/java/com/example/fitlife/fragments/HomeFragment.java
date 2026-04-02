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
import com.example.fitlife.activities.PlanItemsActivity;
import com.example.fitlife.activities.PlanItemDetailActivity;
import com.example.fitlife.models.PlanEntry;
import com.example.fitlife.adapters.PlanItemAdapter;
import com.example.fitlife.adapters.WorkoutAdapter;
import com.example.fitlife.models.PackagePurchase;
import com.example.fitlife.models.PlanItem;
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

    private RecyclerView rvPackages;
    private RecyclerView rvRoutines;
    private TextView tvManagePackages;
    private TextView tvManageRoutines;
    private TextView tvPackagesEmpty;
    private TextView tvRoutinesEmpty;
    private final List<PlanItem> packageItems = new ArrayList<>();
    private final List<PlanItem> routineItems = new ArrayList<>();
    private PlanItemAdapter packagesAdapter;
    private PlanItemAdapter routinesAdapter;

    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private ListenerRegistration workoutListener;
    private ListenerRegistration planItemsListener;
    private ListenerRegistration purchasesListener;

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

        rvPackages = view.findViewById(R.id.rvPackages);
        rvRoutines = view.findViewById(R.id.rvRoutines);
        tvManagePackages = view.findViewById(R.id.tvManagePackages);
        tvManageRoutines = view.findViewById(R.id.tvManageRoutines);
        tvPackagesEmpty = view.findViewById(R.id.tvPackagesEmpty);
        tvRoutinesEmpty = view.findViewById(R.id.tvRoutinesEmpty);
        
        // Stats views
        pbStats = view.findViewById(R.id.pbStats);
        tvStatsPercent = view.findViewById(R.id.tvStatsPercent);
        tvStatsCount = view.findViewById(R.id.tvStatsCount);

        workoutList = new ArrayList<>();
        setupRecyclerView();
        setupSwipeGestures();
        setupPlanSections();
        loadUserProfile();
        ensureDefaultPackages();
        startWorkoutListener(); // Start listening for real-time updates
        startPlanItemsListener();

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
        if (planItemsListener != null) {
            planItemsListener.remove();
            planItemsListener = null;
        }
        if (purchasesListener != null) {
            purchasesListener.remove();
            purchasesListener = null;
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

    private void setupPlanSections() {
        packagesAdapter = new PlanItemAdapter(packageItems, new PlanItemAdapter.Listener() {
            @Override
            public void onItemClick(PlanItem item) {
                if (getContext() == null) return;
                if (item.getKey() != null) {
                    Intent intent = new Intent(getContext(), PlanItemDetailActivity.class);
                    intent.putExtra(PlanItemDetailActivity.EXTRA_PACKAGE_KEY, item.getKey());
                    startActivity(intent);
                }
            }

            @Override
            public void onItemLongClick(PlanItem item) {
            }

            @Override
            public void onToggleAdded(PlanItem item, boolean added) {
            }
        }, false);

        routinesAdapter = new PlanItemAdapter(routineItems, new PlanItemAdapter.Listener() {
            @Override
            public void onItemClick(PlanItem item) {
                if (getContext() == null) return;
                if (item.getDocumentId() == null) return;
                Intent intent = new Intent(getContext(), PlanItemsActivity.class);
                intent.putExtra(PlanItemsActivity.EXTRA_TYPE, "routine");
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(PlanItem item) {
            }

            @Override
            public void onToggleAdded(PlanItem item, boolean added) {
            }
        }, false);

        if (rvPackages != null) {
            rvPackages.setLayoutManager(new LinearLayoutManager(getContext()));
            rvPackages.setAdapter(packagesAdapter);
        }
        if (rvRoutines != null) {
            rvRoutines.setLayoutManager(new LinearLayoutManager(getContext()));
            rvRoutines.setAdapter(routinesAdapter);
        }

        if (tvManagePackages != null) {
            tvManagePackages.setOnClickListener(v -> {
                if (getContext() == null) return;
                Intent intent = new Intent(getContext(), PlanItemsActivity.class);
                intent.putExtra(PlanItemsActivity.EXTRA_TYPE, "package");
                startActivity(intent);
            });
        }
        if (tvManageRoutines != null) {
            tvManageRoutines.setOnClickListener(v -> {
                if (getContext() == null) return;
                Intent intent = new Intent(getContext(), PlanItemsActivity.class);
                intent.putExtra(PlanItemsActivity.EXTRA_TYPE, "routine");
                startActivity(intent);
            });
        }
    }

    private void startPlanItemsListener() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";

        planItemsListener = firestore.collection("plan_items")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    // Packages are handled by purchasesListener; only maintain routines here
                    routineItems.clear();

                    for (QueryDocumentSnapshot doc : value) {
                        PlanItem item = doc.toObject(PlanItem.class);
                        if (item == null) continue;
                        item.setDocumentId(doc.getId());
                        if (!item.isAddedToWeeklyPlan()) continue;

                        if ("routine".equalsIgnoreCase(item.getType())) {
                            routineItems.add(item);
                        }
                    }

                    // Only routines adapter updates here
                    if (routinesAdapter != null) routinesAdapter.notifyDataSetChanged();

                    if (tvRoutinesEmpty != null) {
                        tvRoutinesEmpty.setVisibility(routineItems.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void ensureDefaultPackages() {
        startPurchasesListener();
    }

    private void startPurchasesListener() {
        if (purchasesListener != null) {
            purchasesListener.remove();
            purchasesListener = null;
        }

        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";
        purchasesListener = firestore.collection("package_purchases")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) return;

                    long now = System.currentTimeMillis();
                    packageItems.clear();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        PackagePurchase p = doc.toObject(PackagePurchase.class);
                        if (p == null) continue;
                        if (p.getPackageKey() == null) continue;
                        if (p.getExpiresAt() <= now) continue;

                        PlanItem item;
                        if ("diet_plan".equalsIgnoreCase(p.getPackageKey())) {
                            item = buildDietPlan(userId);
                            item.setKey("diet_plan");
                            item.setDocumentId(userId + "_package_diet_plan");
                        } else if ("gym".equalsIgnoreCase(p.getPackageKey())) {
                            item = buildGymPlan(userId);
                            item.setKey("gym");
                            item.setDocumentId(userId + "_package_gym");
                        } else if ("yoga".equalsIgnoreCase(p.getPackageKey())) {
                            item = buildYogaPlan(userId);
                            item.setKey("yoga");
                            item.setDocumentId(userId + "_package_yoga");
                        } else {
                            continue;
                        }

                        item.setType("package");
                        item.setPurchased(true);
                        item.setExpiresAt(p.getExpiresAt());
                        packageItems.add(item);
                    }

                    if (packagesAdapter != null) packagesAdapter.notifyDataSetChanged();
                    if (tvPackagesEmpty != null) {
                        tvPackagesEmpty.setVisibility(packageItems.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private PlanItem buildDietPlan(String userId) {
        PlanItem item = new PlanItem();
        item.setUserId(userId);
        item.setType("package");
        item.setTitle("Diet Plan");
        item.setNotes("Simple daily nutrition plan");
        item.setCreatedAt(System.currentTimeMillis());

        List<PlanEntry> entries = new ArrayList<>();
        entries.add(new PlanEntry("Breakfast", "Oats or eggs + fruit. Drink 1 glass of water."));
        entries.add(new PlanEntry("Lunch", "Lean protein + rice/potato + vegetables. Avoid sugary drinks."));
        entries.add(new PlanEntry("Snack", "Nuts or yogurt. Keep portion small."));
        entries.add(new PlanEntry("Dinner", "Light meal: vegetables + protein. Stop eating 2 hours before sleep."));
        entries.add(new PlanEntry("Hydration", "Drink 6-8 glasses of water across the day."));
        item.setEntries(entries);
        return item;
    }

    private PlanItem buildGymPlan(String userId) {
        PlanItem item = new PlanItem();
        item.setUserId(userId);
        item.setType("package");
        item.setTitle("Gym");
        item.setNotes("Beginner full-body routine");
        item.setCreatedAt(System.currentTimeMillis());

        List<PlanEntry> entries = new ArrayList<>();
        entries.add(new PlanEntry("Warm-up (5-10 min)", "Brisk walk + dynamic stretches."));
        entries.add(new PlanEntry("Squats (3x10)", "Keep back straight, go to comfortable depth."));
        entries.add(new PlanEntry("Push-ups (3x8-12)", "Knees down if needed, control the movement."));
        entries.add(new PlanEntry("Rows (3x10)", "Use dumbbells/band, squeeze shoulder blades."));
        entries.add(new PlanEntry("Plank (3x30-45s)", "Keep core tight, avoid sagging."));
        entries.add(new PlanEntry("Cool down", "Slow walk + stretching 5 minutes."));
        item.setEntries(entries);
        return item;
    }

    private PlanItem buildYogaPlan(String userId) {
        PlanItem item = new PlanItem();
        item.setUserId(userId);
        item.setType("package");
        item.setTitle("Yoga");
        item.setNotes("Daily flexibility & breathing");
        item.setCreatedAt(System.currentTimeMillis());

        List<PlanEntry> entries = new ArrayList<>();
        entries.add(new PlanEntry("Breathing (2 min)", "Inhale 4s, exhale 6s. Relax shoulders."));
        entries.add(new PlanEntry("Sun Salutation (3 rounds)", "Move slowly, keep steady breathing."));
        entries.add(new PlanEntry("Warrior I/II (2x each side)", "Stack knee over ankle, open chest."));
        entries.add(new PlanEntry("Triangle Pose (2x each side)", "Lengthen spine, avoid collapsing."));
        entries.add(new PlanEntry("Seated Forward Fold (2 min)", "Hinge at hips, keep knees soft."));
        entries.add(new PlanEntry("Savasana (2-3 min)", "Lie down, fully relax body."));
        item.setEntries(entries);
        return item;
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
