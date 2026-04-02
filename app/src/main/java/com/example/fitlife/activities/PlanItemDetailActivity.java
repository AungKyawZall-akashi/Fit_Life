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
import com.google.firebase.firestore.Source;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class PlanItemDetailActivity extends AppCompatActivity {

    public static final String EXTRA_DOC_ID = "EXTRA_DOC_ID";
    public static final String EXTRA_PACKAGE_KEY = "EXTRA_PACKAGE_KEY";
    public static final String EXTRA_DIET_MODE = "EXTRA_DIET_MODE";
    public static final String DIET_MODE_STATIC = "static";
    public static final String DIET_MODE_PERSONAL = "personal";
    public static final String EXTRA_PERSONAL_PLAN_JSON = "EXTRA_PERSONAL_PLAN_JSON";

    private ImageView ivBack;
    private TextView tvTitle;
    private ImageView ivBannerIcon;
    private TextView tvBannerTitle;
    private TextView tvBannerSubtitle;
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
        ivBannerIcon = findViewById(R.id.ivBannerIcon);
        tvBannerTitle = findViewById(R.id.tvBannerTitle);
        tvBannerSubtitle = findViewById(R.id.tvBannerSubtitle);
        rvEntries = findViewById(R.id.rvEntries);

        adapter = new PlanEntryAdapter(entries);
        rvEntries.setLayoutManager(new LinearLayoutManager(this));
        rvEntries.setAdapter(adapter);

        ivBack.setOnClickListener(v -> finish());

        String packageKey = getIntent().getStringExtra(EXTRA_PACKAGE_KEY);
        if (packageKey != null && !packageKey.trim().isEmpty()) {
            if ("diet_plan".equalsIgnoreCase(packageKey)) {
                String mode = getIntent().getStringExtra(EXTRA_DIET_MODE);
                if (DIET_MODE_PERSONAL.equalsIgnoreCase(mode)) {
                    String json = getIntent().getStringExtra(EXTRA_PERSONAL_PLAN_JSON);
                    if (json != null && !json.trim().isEmpty()) {
                        applyPersonalFromJson(json);
                        return;
                    }
                    loadPersonalDietPlan();
                    return;
                }
                applyFallback("anonymous_package_" + packageKey);
                return;
            }
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
                    updateBanner(item.getKey() != null ? item.getKey() : docId, entries.size());
                    adapter.notifyDataSetChanged();
                });
    }

    private void applyFallback(String docId) {
        entries.clear();
        if (docId != null) {
            if (docId.contains("_package_diet_plan")) {
                tvTitle.setText("Diet Plan");
                entries.add(new PlanEntry("Breakfast", "Oats or eggs + fruit. Drink 1 glass of water.", "Morning • Balanced", "diet"));
                entries.add(new PlanEntry("Lunch", "Lean protein + rice/potato + vegetables. Avoid sugary drinks.", "Midday • Protein + Veg", "diet"));
                entries.add(new PlanEntry("Snack", "Nuts or yogurt. Keep portion small.", "Afternoon • Light", "diet"));
                entries.add(new PlanEntry("Dinner", "Light meal: vegetables + protein. Stop eating 2 hours before sleep.", "Evening • Light meal", "diet"));
                entries.add(new PlanEntry("Hydration", "Drink 6-8 glasses of water across the day.", "All day • 6-8 glasses", "water"));
            } else if (docId.contains("_package_gym")) {
                tvTitle.setText("Gym");
                entries.add(new PlanEntry("Warm-up", "Brisk walk + dynamic stretches.", "5-10 min", "run"));
                entries.add(new PlanEntry("Squats", "Keep back straight, go to comfortable depth.", "3 sets × 10 reps", "weights"));
                entries.add(new PlanEntry("Push-ups", "Knees down if needed, control the movement.", "3 sets × 8-12 reps", "weights"));
                entries.add(new PlanEntry("Rows", "Use dumbbells/band, squeeze shoulder blades.", "3 sets × 10 reps", "weights"));
                entries.add(new PlanEntry("Plank", "Keep core tight, avoid sagging.", "3 sets × 30-45 sec", "weights"));
                entries.add(new PlanEntry("Cool down", "Slow walk + stretching 5 minutes.", "5 min", "run"));
            } else if (docId.contains("_package_yoga")) {
                tvTitle.setText("Yoga");
                entries.add(new PlanEntry("Breathing", "Inhale 4s, exhale 6s. Relax shoulders.", "2 min", "yoga"));
                entries.add(new PlanEntry("Sun Salutation", "Move slowly, keep steady breathing.", "3 rounds", "yoga"));
                entries.add(new PlanEntry("Warrior I/II", "Stack knee over ankle, open chest.", "2× each side", "yoga"));
                entries.add(new PlanEntry("Triangle Pose", "Lengthen spine, avoid collapsing.", "2× each side", "yoga"));
                entries.add(new PlanEntry("Seated Forward Fold", "Hinge at hips, keep knees soft.", "2 min", "yoga"));
                entries.add(new PlanEntry("Savasana", "Lie down, fully relax body.", "2-3 min", "yoga"));
            }
        }
        updateBanner(docId, entries.size());
        adapter.notifyDataSetChanged();
    }

    private void updateBanner(String keyOrDocId, int count) {
        if (ivBannerIcon == null || tvBannerTitle == null || tvBannerSubtitle == null) return;
        String k = keyOrDocId != null ? keyOrDocId : "";

        int iconRes = R.drawable.ic_statistics;
        String subtitle = count + " items";

        if (k.contains("diet_plan") || "diet_plan".equalsIgnoreCase(k)) {
            iconRes = R.drawable.ic_diet;
            subtitle = count + " items • Daily nutrition";
        } else if (k.contains("_package_gym") || "gym".equalsIgnoreCase(k)) {
            iconRes = R.drawable.ic_weights;
            subtitle = count + " items • Strength training";
        } else if (k.contains("_package_yoga") || "yoga".equalsIgnoreCase(k)) {
            iconRes = R.drawable.ic_yoga;
            subtitle = count + " items • Flexibility";
        }

        ivBannerIcon.setImageResource(iconRes);
        tvBannerTitle.setText("What's inside");
        tvBannerSubtitle.setText(subtitle);
    }

    private void loadPersonalDietPlan() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            applyFallback("anonymous_package_diet_plan");
            return;
        }

        firestore.collection("personal_diet_plans").document(userId)
                .get(Source.CACHE)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        applyPersonalFromDoc(task.getResult());
                        return;
                    }

                    firestore.collection("personal_diet_plans").document(userId)
                            .get(Source.SERVER)
                            .addOnSuccessListener(doc -> {
                                if (!doc.exists()) {
                                    android.widget.Toast.makeText(this, "No personal plan yet. Please generate one.", android.widget.Toast.LENGTH_SHORT).show();
                                    android.content.Intent intent = new android.content.Intent(this, PersonalDietPlanActivity.class);
                                    startActivity(intent);
                                    finish();
                                    return;
                                }
                                applyPersonalFromDoc(doc);
                            })
                            .addOnFailureListener(e -> {
                                android.widget.Toast.makeText(this, "Cannot load personal plan. Please try again.", android.widget.Toast.LENGTH_SHORT).show();
                                android.content.Intent intent = new android.content.Intent(this, PersonalDietPlanActivity.class);
                                startActivity(intent);
                                finish();
                            });
                });
    }

    private void applyPersonalFromDoc(com.google.firebase.firestore.DocumentSnapshot doc) {
        tvTitle.setText("Personal Diet Plan");
        entries.clear();
        java.util.List<java.util.Map<String, Object>> list = (java.util.List<java.util.Map<String, Object>>) doc.get("entries");
        if (list != null) {
            for (java.util.Map<String, Object> m : list) {
                if (m == null) continue;
                String name = m.get("name") != null ? String.valueOf(m.get("name")) : "";
                String ins = m.get("instructions") != null ? String.valueOf(m.get("instructions")) : "";
                String meta = m.get("meta") != null ? String.valueOf(m.get("meta")) : "";
                String imageKey = m.get("imageKey") != null ? String.valueOf(m.get("imageKey")) : "";
                entries.add(new PlanEntry(name, ins, meta, imageKey));
            }
        }
        updateBanner("diet_plan", entries.size());
        adapter.notifyDataSetChanged();
    }

    private void applyPersonalFromJson(String json) {
        tvTitle.setText("Personal Diet Plan");
        entries.clear();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String name = obj.optString("name", "");
                String ins = obj.optString("instructions", "");
                String meta = obj.optString("meta", "");
                String imageKey = obj.optString("imageKey", "");
                entries.add(new PlanEntry(name, ins, meta, imageKey));
            }
        } catch (Exception e) {
        }
        updateBanner("diet_plan", entries.size());
        adapter.notifyDataSetChanged();
    }
}
