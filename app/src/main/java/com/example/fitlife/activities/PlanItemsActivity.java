package com.example.fitlife.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fitlife.R;
import com.example.fitlife.adapters.PlanItemAdapter;
import com.example.fitlife.models.PlanEntry;
import com.example.fitlife.models.PackagePurchase;
import com.example.fitlife.models.PlanItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlanItemsActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "EXTRA_TYPE";

    private ImageView ivBack;
    private ImageButton btnAdd;
    private TextView tvTitle;
    private TextView tvHint;
    private RecyclerView rvItems;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private ListenerRegistration listenerRegistration;
    private ListenerRegistration purchasesListenerRegistration;

    private final List<PlanItem> items = new ArrayList<>();
    private PlanItemAdapter adapter;
    private String type;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_items);

        type = getIntent().getStringExtra(EXTRA_TYPE);
        if (type == null || type.trim().isEmpty()) {
            type = "routine";
        }

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        ivBack = findViewById(R.id.ivBack);
        btnAdd = findViewById(R.id.btnAdd);
        tvTitle = findViewById(R.id.tvTitle);
        tvHint = findViewById(R.id.tvHint);
        rvItems = findViewById(R.id.rvItems);

        tvTitle.setText("package".equalsIgnoreCase(type) ? "My Packages" : "My Routine");
        tvHint.setText("package".equalsIgnoreCase(type) ? "Locked packages need premium to unlock" : "Toggle to add/remove from weekly plan");

        boolean isPackages = "package".equalsIgnoreCase(type);
        btnAdd.setVisibility(isPackages ? View.GONE : View.VISIBLE);

        adapter = new PlanItemAdapter(items, new PlanItemAdapter.Listener() {
            @Override
            public void onItemClick(PlanItem item) {
                if ("package".equalsIgnoreCase(type)) {
                    String key = item.getKey();
                    if (key == null || key.trim().isEmpty()) return;
                    if (!item.isPurchased() || item.getExpiresAt() <= System.currentTimeMillis()) {
                        showBuyDialog(key);
                        return;
                    }
                    if ("diet_plan".equalsIgnoreCase(key)) {
                        Intent intent = new Intent(PlanItemsActivity.this, DietPlanOptionsActivity.class);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(PlanItemsActivity.this, PlanItemDetailActivity.class);
                        intent.putExtra(PlanItemDetailActivity.EXTRA_PACKAGE_KEY, key);
                        startActivity(intent);
                    }
                    return;
                }

                String docId = item.getDocumentId();
                if (docId == null || docId.trim().isEmpty()) return;
                Intent intent = new Intent(PlanItemsActivity.this, PlanItemDetailActivity.class);
                intent.putExtra(PlanItemDetailActivity.EXTRA_DOC_ID, docId);
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(PlanItem item) {
                if (item.isSystemDefault()) return;
                showItemMenu(item);
            }

            @Override
            public void onToggleAdded(PlanItem item, boolean added) {
                if (item.getDocumentId() == null) return;
                firestore.collection("plan_items").document(item.getDocumentId())
                        .update("addedToWeeklyPlan", added);
            }
        }, !isPackages);

        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(adapter);

        ivBack.setOnClickListener(v -> finish());
        btnAdd.setOnClickListener(v -> showCreateDialog());

        if (isPackages) {
            setupDefaultPackages();
            startPurchasesListener();
            ensureDefaultPackages();
        }
        startListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
        if (purchasesListenerRegistration != null) {
            purchasesListenerRegistration.remove();
            purchasesListenerRegistration = null;
        }
    }

    private void startListener() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }

        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";

        listenerRegistration = firestore.collection("plan_items")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", type)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) {
                        return;
                    }

                    if ("package".equalsIgnoreCase(type)) {
                        return;
                    }

                    items.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        PlanItem item = doc.toObject(PlanItem.class);
                        if (item == null) continue;
                        item.setDocumentId(doc.getId());
                        if (item.getTitle() == null || item.getTitle().trim().isEmpty()) {
                            String fallback = item.getKey() != null && !item.getKey().trim().isEmpty()
                                    ? item.getKey().trim().replace('_', ' ')
                                    : "Untitled Routine";
                            if (!fallback.isEmpty()) {
                                fallback = Character.toUpperCase(fallback.charAt(0)) + fallback.substring(1);
                            }
                            item.setTitle(fallback);
                        }
                        items.add(item);
                    }

                    Collections.sort(items, Comparator.comparingLong(PlanItem::getCreatedAt).reversed());
                    adapter.notifyDataSetChanged();
                });
    }

    private void setupDefaultPackages() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";
        items.clear();
        PlanItem diet = buildDietPlan(userId, userId + "_package_diet_plan");
        diet.setSystemDefault(true);
        items.add(diet);
        PlanItem gym = buildGymPlan(userId, userId + "_package_gym");
        gym.setSystemDefault(true);
        items.add(gym);
        PlanItem yoga = buildYogaPlan(userId, userId + "_package_yoga");
        yoga.setSystemDefault(true);
        items.add(yoga);
        adapter.notifyDataSetChanged();
    }

    private void startPurchasesListener() {
        if (purchasesListenerRegistration != null) {
            purchasesListenerRegistration.remove();
            purchasesListenerRegistration = null;
        }

        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";
        purchasesListenerRegistration = firestore.collection("package_purchases")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) return;

                    java.util.Map<String, PackagePurchase> map = new java.util.HashMap<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        PackagePurchase p = doc.toObject(PackagePurchase.class);
                        if (p == null) continue;
                        if (p.getPackageKey() == null) continue;
                        map.put(p.getPackageKey(), p);
                    }

                    for (PlanItem item : items) {
                        String key = item.getKey();
                        PackagePurchase p = key != null ? map.get(key) : null;
                        if (p != null) {
                            item.setPurchased(true);
                            item.setExpiresAt(p.getExpiresAt());
                        } else {
                            item.setPurchased(false);
                            item.setExpiresAt(0L);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showBuyDialog(String packageKey) {
        new AlertDialog.Builder(this)
                .setTitle("Premium Required")
                .setMessage("This package is locked. You need to buy premium to unlock it.")
                .setPositiveButton("Buy Premium", (dialog, which) -> {
                    Intent intent = new Intent(this, PackagePurchaseActivity.class);
                    intent.putExtra(PackagePurchaseActivity.EXTRA_PACKAGE_KEY, packageKey);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void ensureDefaultPackages() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";

        Map<String, PlanItem> defaults = new LinkedHashMap<>();
        defaults.put("diet_plan", buildDietPlan(userId, null));
        defaults.put("gym", buildGymPlan(userId, null));
        defaults.put("yoga", buildYogaPlan(userId, null));

        for (Map.Entry<String, PlanItem> entry : defaults.entrySet()) {
            String key = entry.getKey();
            PlanItem item = entry.getValue();
            String docId = userId + "_package_" + key;

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("type", "package");
            data.put("key", key);
            data.put("title", item.getTitle());
            data.put("notes", item.getNotes());
            data.put("addedToWeeklyPlan", true);
            data.put("createdAt", item.getCreatedAt());
            data.put("systemDefault", true);
            data.put("entries", item.getEntries());

            firestore.runTransaction(transaction -> {
                        com.google.firebase.firestore.DocumentReference ref = firestore.collection("plan_items").document(docId);
                        com.google.firebase.firestore.DocumentSnapshot snap = transaction.get(ref);
                        if (!snap.exists()) {
                            transaction.set(ref, data);
                        }
                        return null;
                    })
                    .addOnFailureListener(e -> {
                        if (e instanceof FirebaseFirestoreException) {
                            FirebaseFirestoreException ex = (FirebaseFirestoreException) e;
                            if (ex.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                return;
                            }
                        }
                    });
        }
    }

    private PlanItem buildDietPlan(String userId, String documentId) {
        PlanItem item = new PlanItem();
        item.setDocumentId(documentId);
        item.setUserId(userId);
        item.setType("package");
        item.setKey("diet_plan");
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

    private PlanItem buildGymPlan(String userId, String documentId) {
        PlanItem item = new PlanItem();
        item.setDocumentId(documentId);
        item.setUserId(userId);
        item.setType("package");
        item.setKey("gym");
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

    private PlanItem buildYogaPlan(String userId, String documentId) {
        PlanItem item = new PlanItem();
        item.setDocumentId(documentId);
        item.setUserId(userId);
        item.setType("package");
        item.setKey("yoga");
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

    private void showCreateDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Title");
        layout.addView(etTitle);

        EditText etNotes = new EditText(this);
        etNotes.setHint("Notes (optional)");
        layout.addView(etNotes);

        new AlertDialog.Builder(this)
                .setTitle("package".equalsIgnoreCase(type) ? "New Package" : "New Routine")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String notes = etNotes.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    createItem(title, notes);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createItem(String title, String notes) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("type", type);
        data.put("title", title);
        data.put("notes", notes);
        data.put("addedToWeeklyPlan", true);
        data.put("createdAt", System.currentTimeMillis());

        firestore.collection("plan_items")
                .add(data)
                .addOnSuccessListener(ref -> Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showItemMenu(PlanItem item) {
        String[] options = new String[]{"Edit", "Delete"};
        new AlertDialog.Builder(this)
                .setTitle(item.getTitle())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditDialog(item);
                    } else if (which == 1) {
                        deleteItem(item);
                    }
                })
                .show();
    }

    private void showEditDialog(PlanItem item) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Title");
        etTitle.setText(item.getTitle());
        layout.addView(etTitle);

        EditText etNotes = new EditText(this);
        etNotes.setHint("Notes (optional)");
        etNotes.setText(item.getNotes());
        layout.addView(etNotes);

        new AlertDialog.Builder(this)
                .setTitle("Edit")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String notes = etNotes.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateItem(item, title, notes);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateItem(PlanItem item, String title, String notes) {
        if (item.getDocumentId() == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("notes", notes);

        firestore.collection("plan_items").document(item.getDocumentId())
                .update(updates)
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void deleteItem(PlanItem item) {
        if (item.getDocumentId() == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> firestore.collection("plan_items").document(item.getDocumentId()).delete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showViewDialog(PlanItem item) {
        StringBuilder message = new StringBuilder();
        if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
            message.append(item.getNotes().trim());
        } else {
            message.append("No notes");
        }

        new AlertDialog.Builder(this)
                .setTitle(item.getTitle())
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .show();
    }
}
