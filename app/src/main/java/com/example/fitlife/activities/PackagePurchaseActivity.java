package com.example.fitlife.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.fitlife.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class PackagePurchaseActivity extends AppCompatActivity {

    public static final String EXTRA_PACKAGE_KEY = "EXTRA_PACKAGE_KEY";

    private ImageView ivBack;
    private ImageView ivPackageIcon;
    private TextView tvPackageTitle;
    private TextView tvPackageSubtitle;
    private CardView plan1;
    private CardView plan3;
    private CardView plan12;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private String packageKey;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_package_purchase);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        packageKey = getIntent().getStringExtra(EXTRA_PACKAGE_KEY);
        if (packageKey == null) packageKey = "";

        ivBack = findViewById(R.id.ivBack);
        ivPackageIcon = findViewById(R.id.ivPackageIcon);
        tvPackageTitle = findViewById(R.id.tvPackageTitle);
        tvPackageSubtitle = findViewById(R.id.tvPackageSubtitle);
        plan1 = findViewById(R.id.plan1);
        plan3 = findViewById(R.id.plan3);
        plan12 = findViewById(R.id.plan12);

        bindPackageInfo(packageKey);

        ivBack.setOnClickListener(v -> finish());
        plan1.setOnClickListener(v -> buyMonths(1));
        plan3.setOnClickListener(v -> buyMonths(3));
        plan12.setOnClickListener(v -> buyMonths(12));
    }

    private void bindPackageInfo(String key) {
        int iconRes;
        String title;
        String subtitle;

        if ("diet_plan".equalsIgnoreCase(key)) {
            iconRes = R.drawable.ic_notifications;
            title = "Diet Plan";
            subtitle = "Simple daily nutrition plan";
        } else if ("gym".equalsIgnoreCase(key)) {
            iconRes = R.drawable.ic_weights;
            title = "Gym";
            subtitle = "Beginner full-body routine";
        } else if ("yoga".equalsIgnoreCase(key)) {
            iconRes = R.drawable.ic_yoga;
            title = "Yoga";
            subtitle = "Daily flexibility & breathing";
        } else {
            iconRes = R.drawable.ic_statistics;
            title = "Package";
            subtitle = "";
        }

        ivPackageIcon.setImageResource(iconRes);
        tvPackageTitle.setText(title);
        tvPackageSubtitle.setText(subtitle);
    }

    private void buyMonths(int months) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";
        long now = System.currentTimeMillis();
        String docId = userId + "_" + packageKey;

        firestore.collection("package_purchases").document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    long base = now;
                    Long existingExpires = doc.getLong("expiresAt");
                    if (existingExpires != null && existingExpires > now) {
                        base = existingExpires;
                    }

                    long expiresAt = base + (long) months * 30L * 24L * 60L * 60L * 1000L;

                    Map<String, Object> data = new HashMap<>();
                    data.put("userId", userId);
                    data.put("packageKey", packageKey);
                    data.put("purchasedAt", now);
                    data.put("expiresAt", expiresAt);

                    firestore.collection("package_purchases").document(docId)
                            .set(data)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Purchased", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Purchase failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Purchase failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}

