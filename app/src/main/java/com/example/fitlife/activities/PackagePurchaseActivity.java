package com.example.fitlife.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.fitlife.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;
import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
        plan1.setOnClickListener(v -> showPaymentDialog(1));
        plan3.setOnClickListener(v -> showPaymentDialog(3));
        plan12.setOnClickListener(v -> showPaymentDialog(12));
    }

    private void bindPackageInfo(String key) {
        int iconRes;
        String title;
        String subtitle;

        if ("diet_plan".equalsIgnoreCase(key)) {
            iconRes = R.drawable.ic_diet;
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

    private void showPaymentDialog(int months) {
        double price = months == 1 ? 4.99 : months == 3 ? 12.99 : 39.99;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_payment, null, false);
        TextView tvAmount = view.findViewById(R.id.tvAmount);
        RadioGroup rgPayment = view.findViewById(R.id.rgPayment);
        EditText etPhone = view.findViewById(R.id.etPhone);

        tvAmount.setText("Amount: $" + String.format(Locale.getDefault(), "%.2f", price));
        rgPayment.check(R.id.rbKBZ);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirm Payment")
                .setView(view)
                .setPositiveButton("Pay", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int checkedId = rgPayment.getCheckedRadioButtonId();
            String method = checkedId == R.id.rbMBU ? "MBU" : "KBZ Pay";
            String phone = etPhone.getText().toString().trim();
            if (phone.isEmpty()) {
                etPhone.setError("Phone number required");
                return;
            }
            String digits = phone.replaceAll("[^0-9]", "");
            if (digits.length() < 7) {
                etPhone.setError("Invalid phone number");
                return;
            }
            dialog.dismiss();
            buyMonths(months, price, method, maskPhone(digits));
        }));

        dialog.show();
    }

    private String maskPhone(String digits) {
        if (digits == null) return "";
        String d = digits.trim();
        if (d.length() <= 4) return d;
        String last4 = d.substring(d.length() - 4);
        StringBuilder sb = new StringBuilder();
        sb.append(d.charAt(0));
        for (int i = 1; i < d.length() - 4; i++) {
            sb.append("*");
        }
        sb.append(last4);
        return sb.toString();
    }

    private void buyMonths(int months, double price, String paymentMethod, String phoneMasked) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";
        long now = System.currentTimeMillis();
        String docId = userId + "_" + packageKey;

        firestore.collection("package_purchases").document(docId)
                .get(Source.CACHE)
                .addOnCompleteListener(task -> {
                    long base = now;
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        Long existingExpires = task.getResult().getLong("expiresAt");
                        if (existingExpires != null && existingExpires > now) {
                            base = existingExpires;
                        }
                    }

                    long expiresAt = base + (long) months * 30L * 24L * 60L * 60L * 1000L;

                    Map<String, Object> data = new HashMap<>();
                    data.put("userId", userId);
                    data.put("packageKey", packageKey);
                    data.put("purchasedAt", now);
                    data.put("expiresAt", expiresAt);
                    data.put("planMonths", months);
                    data.put("price", price);
                    data.put("paymentMethod", paymentMethod);
                    data.put("phoneNumberMasked", phoneMasked);

                    firestore.collection("package_purchases").document(docId)
                            .set(data, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Map<String, Object> history = new HashMap<>();
                                history.putAll(data);
                                history.put("status", "purchased");
                                history.put("canceledAt", 0L);
                                String historyDocId = userId + "_" + packageKey + "_" + now + "_purchased";
                                firestore.collection("package_purchase_history").document(historyDocId).set(history, SetOptions.merge());

                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                String expireText = sdf.format(new Date(expiresAt));
                                new AlertDialog.Builder(this)
                                        .setTitle("Thank you for purchasing!")
                                        .setMessage("Your package is unlocked until " + expireText)
                                        .setPositiveButton("OK", (dialog, which) -> goToDashboard())
                                        .show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Purchase failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                });
    }

    private void goToDashboard() {
        android.content.Intent intent = new android.content.Intent(this, DashboardActivity.class);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
