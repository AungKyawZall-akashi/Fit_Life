package com.example.fitlife.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fitlife.R;
import com.example.fitlife.adapters.PlanEntryAdapter;
import com.example.fitlife.models.PlanEntry;
import com.example.fitlife.models.PlanItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

public class PlanItemDetailActivity extends AppCompatActivity {

    public static final String EXTRA_DOC_ID = "EXTRA_DOC_ID";
    public static final String EXTRA_PACKAGE_KEY = "EXTRA_PACKAGE_KEY";

    private ImageView ivBack;
    private TextView tvTitle;
    private RecyclerView rvEntries;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private ListenerRegistration listenerRegistration;

    private final List<PlanEntry> entries = new ArrayList<>();
    private PlanEntryAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_item_detail);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);
        rvEntries = findViewById(R.id.rvEntries);

        adapter = new PlanEntryAdapter(entries);
        rvEntries.setLayoutManager(new LinearLayoutManager(this));
        rvEntries.setAdapter(adapter);

        ivBack.setOnClickListener(v -> finish());

        String packageKey = getIntent().getStringExtra(EXTRA_PACKAGE_KEY);
        if (packageKey != null && !packageKey.trim().isEmpty()) {
            applyFallback("anonymous_package_" + packageKey);
            return;
        }

        String docId = getIntent().getStringExtra(EXTRA_DOC_ID);
        if (docId != null && !docId.trim().isEmpty()) {
            startListener(docId);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    private void startListener(String docId) {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }

        listenerRegistration = firestore.collection("plan_items")
                .document(docId)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null || !snap.exists()) {
                        applyFallback(docId);
                        return;
                    }

                    PlanItem item = snap.toObject(PlanItem.class);
                    if (item == null) return;

                    tvTitle.setText(item.getTitle());

                    entries.clear();
                    List<PlanEntry> list = item.getEntries();
                    if (list != null) {
                        entries.addAll(list);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void applyFallback(String docId) {
        entries.clear();
        if (docId != null) {
            if (docId.contains("_package_diet_plan")) {
                tvTitle.setText("Diet Plan");
                entries.add(new PlanEntry("Breakfast", "Oats or eggs + fruit. Drink 1 glass of water."));
                entries.add(new PlanEntry("Lunch", "Lean protein + rice/potato + vegetables. Avoid sugary drinks."));
                entries.add(new PlanEntry("Snack", "Nuts or yogurt. Keep portion small."));
                entries.add(new PlanEntry("Dinner", "Light meal: vegetables + protein. Stop eating 2 hours before sleep."));
                entries.add(new PlanEntry("Hydration", "Drink 6-8 glasses of water across the day."));
            } else if (docId.contains("_package_gym")) {
                tvTitle.setText("Gym");
                entries.add(new PlanEntry("Warm-up (5-10 min)", "Brisk walk + dynamic stretches."));
                entries.add(new PlanEntry("Squats (3x10)", "Keep back straight, go to comfortable depth."));
                entries.add(new PlanEntry("Push-ups (3x8-12)", "Knees down if needed, control the movement."));
                entries.add(new PlanEntry("Rows (3x10)", "Use dumbbells/band, squeeze shoulder blades."));
                entries.add(new PlanEntry("Plank (3x30-45s)", "Keep core tight, avoid sagging."));
                entries.add(new PlanEntry("Cool down", "Slow walk + stretching 5 minutes."));
            } else if (docId.contains("_package_yoga")) {
                tvTitle.setText("Yoga");
                entries.add(new PlanEntry("Breathing (2 min)", "Inhale 4s, exhale 6s. Relax shoulders."));
                entries.add(new PlanEntry("Sun Salutation (3 rounds)", "Move slowly, keep steady breathing."));
                entries.add(new PlanEntry("Warrior I/II (2x each side)", "Stack knee over ankle, open chest."));
                entries.add(new PlanEntry("Triangle Pose (2x each side)", "Lengthen spine, avoid collapsing."));
                entries.add(new PlanEntry("Seated Forward Fold (2 min)", "Hinge at hips, keep knees soft."));
                entries.add(new PlanEntry("Savasana (2-3 min)", "Lie down, fully relax body."));
            }
        }
        adapter.notifyDataSetChanged();
    }
}
