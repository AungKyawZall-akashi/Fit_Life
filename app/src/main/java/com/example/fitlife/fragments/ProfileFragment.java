package com.example.fitlife.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fitlife.R;
import com.example.fitlife.activities.MainActivity;
import com.example.fitlife.adapters.PurchaseAdapter;
import com.example.fitlife.adapters.PurchaseHistoryAdapter;
import com.example.fitlife.models.PackagePurchase;
import com.example.fitlife.models.PurchaseHistoryRecord;
import com.example.fitlife.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private ImageView ivProfilePicture;
    private TextView tvProfileName, tvProfileEmail, tvProfileDOB;
    private Button btnLogout;
    private View rlEditProfile;
    private SwitchCompat switchProfileTheme;
    private View btnUploadProfilePicture;
    private RecyclerView rvPurchases;
    private TextView tvPurchasesEmpty;
    private RecyclerView rvPurchaseHistory;
    private TextView tvPurchaseHistoryEmpty;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ListenerRegistration purchasesListener;
    private ListenerRegistration purchaseHistoryListener;
    private final List<PackagePurchase> purchases = new ArrayList<>();
    private PurchaseAdapter purchaseAdapter;
    private final List<PurchaseHistoryRecord> purchaseHistory = new ArrayList<>();
    private PurchaseHistoryAdapter purchaseHistoryAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        ivProfilePicture = view.findViewById(R.id.ivProfilePicture);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfileDOB = view.findViewById(R.id.tvProfileDOB);
        btnLogout = view.findViewById(R.id.btnLogout);
        rlEditProfile = view.findViewById(R.id.rlEditProfile);
        switchProfileTheme = view.findViewById(R.id.switchProfileTheme);
        btnUploadProfilePicture = view.findViewById(R.id.btnUploadProfilePicture);
        rvPurchases = view.findViewById(R.id.rvPurchases);
        tvPurchasesEmpty = view.findViewById(R.id.tvPurchasesEmpty);
        rvPurchaseHistory = view.findViewById(R.id.rvPurchaseHistory);
        tvPurchaseHistoryEmpty = view.findViewById(R.id.tvPurchaseHistoryEmpty);

        purchaseAdapter = new PurchaseAdapter(purchases, purchase -> showStopPurchaseDialog(purchase));
        rvPurchases.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPurchases.setAdapter(purchaseAdapter);

        purchaseHistoryAdapter = new PurchaseHistoryAdapter(purchaseHistory);
        rvPurchaseHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPurchaseHistory.setAdapter(purchaseHistoryAdapter);

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                uploadProfilePicture(uri);
            }
        });

        loadUserProfile();
        startPurchasesListener();
        startPurchaseHistoryListener();
        setupThemeSwitch();

        rlEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        ivProfilePicture.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        if (btnUploadProfilePicture != null) {
            btnUploadProfilePicture.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (purchasesListener != null) {
            purchasesListener.remove();
            purchasesListener = null;
        }
        if (purchaseHistoryListener != null) {
            purchaseHistoryListener.remove();
            purchaseHistoryListener = null;
        }
    }

    private void setupThemeSwitch() {
        if (getActivity() == null || switchProfileTheme == null) return;

        SharedPreferences sharedPref = getActivity().getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPref.getBoolean("isDarkMode", false);
        switchProfileTheme.setChecked(isDarkMode);

        switchProfileTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("isDarkMode", isChecked);
            editor.apply();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }

    private void showEditProfileDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Profile");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText etName = new EditText(getContext());
        etName.setHint("Full Name");
        etName.setText(tvProfileName.getText().toString());
        layout.addView(etName);

        final EditText etDOB = new EditText(getContext());
        etDOB.setHint("Date of Birth (DD/MM/YYYY)");
        etDOB.setText(tvProfileDOB.getText().toString().equals("Not Set") ? "" : tvProfileDOB.getText().toString());
        layout.addView(etDOB);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = etName.getText().toString().trim();
            String newDOB = etDOB.getText().toString().trim();
            updateProfileInFirestore(newName, newDOB);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateProfileInFirestore(String name, String dob) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("username", name);
            updates.put("dob", dob);

            firestore.collection("users").document(user.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        if (isAdded()) {
                            tvProfileName.setText(name);
                            tvProfileDOB.setText(dob.isEmpty() ? "Not Set" : dob);
                            Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvProfileEmail.setText(user.getEmail());
            
            firestore.collection("users").document(user.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                if (!isAdded()) return;
                                User userModel = document.toObject(User.class);
                                if (userModel != null) {
                                    tvProfileName.setText(userModel.getUsername());
                                    if (userModel.getDob() != null && !userModel.getDob().isEmpty()) {
                                        tvProfileDOB.setText(userModel.getDob());
                                    } else {
                                        tvProfileDOB.setText("Not Set");
                                    }
                                    showProfileImage(userModel.getProfileImageUrl());
                                }
                            }
                        }
                    });
        }
    }

    private void startPurchasesListener() {
        if (purchasesListener != null) {
            purchasesListener.remove();
            purchasesListener = null;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            purchases.clear();
            if (purchaseAdapter != null) purchaseAdapter.notifyDataSetChanged();
            if (tvPurchasesEmpty != null) tvPurchasesEmpty.setVisibility(View.VISIBLE);
            return;
        }

        purchasesListener = firestore.collection("package_purchases")
                .whereEqualTo("userId", user.getUid())
                .addSnapshotListener((snap, err) -> {
                    if (!isAdded() || snap == null || err != null) return;

                    purchases.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        PackagePurchase p = doc.toObject(PackagePurchase.class);
                        if (p == null) continue;
                        purchases.add(p);
                    }

                    syncHistoryFromPurchases(user.getUid(), purchases);

                    Collections.sort(purchases, Comparator.comparingLong(PackagePurchase::getExpiresAt).reversed());
                    if (purchaseAdapter != null) purchaseAdapter.notifyDataSetChanged();
                    if (tvPurchasesEmpty != null) {
                        tvPurchasesEmpty.setVisibility(purchases.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void syncHistoryFromPurchases(String userId, List<PackagePurchase> list) {
        if (userId == null || list == null) return;
        for (PackagePurchase p : list) {
            if (p == null || p.getPackageKey() == null) continue;
            long purchasedAt = p.getPurchasedAt();
            if (purchasedAt <= 0) continue;

            String historyDocId = userId + "_" + p.getPackageKey() + "_" + purchasedAt + "_purchased";
            Map<String, Object> history = new HashMap<>();
            history.put("userId", userId);
            history.put("packageKey", p.getPackageKey());
            history.put("purchasedAt", purchasedAt);
            history.put("expiresAt", p.getExpiresAt());
            history.put("planMonths", p.getPlanMonths());
            history.put("price", p.getPrice());
            history.put("paymentMethod", p.getPaymentMethod());
            history.put("phoneNumberMasked", p.getPhoneNumberMasked());
            history.put("status", "purchased");
            history.put("canceledAt", p.getCanceledAt());
            firestore.collection("package_purchase_history").document(historyDocId).set(history);
        }
    }

    private void startPurchaseHistoryListener() {
        if (purchaseHistoryListener != null) {
            purchaseHistoryListener.remove();
            purchaseHistoryListener = null;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            purchaseHistory.clear();
            if (purchaseHistoryAdapter != null) purchaseHistoryAdapter.notifyDataSetChanged();
            if (tvPurchaseHistoryEmpty != null) tvPurchaseHistoryEmpty.setVisibility(View.VISIBLE);
            return;
        }

        purchaseHistoryListener = firestore.collection("package_purchase_history")
                .whereEqualTo("userId", user.getUid())
                .addSnapshotListener((snap, err) -> {
                    if (!isAdded() || snap == null || err != null) return;

                    purchaseHistory.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        PurchaseHistoryRecord r = doc.toObject(PurchaseHistoryRecord.class);
                        if (r == null) continue;
                        purchaseHistory.add(r);
                    }

                    Collections.sort(purchaseHistory, Comparator.comparingLong(PurchaseHistoryRecord::getPurchasedAt).reversed());
                    if (purchaseHistoryAdapter != null) purchaseHistoryAdapter.notifyDataSetChanged();
                    if (tvPurchaseHistoryEmpty != null) {
                        tvPurchaseHistoryEmpty.setVisibility(purchaseHistory.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void showStopPurchaseDialog(PackagePurchase purchase) {
        if (!isAdded() || getContext() == null) return;
        if (purchase == null || purchase.getPackageKey() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Stop Package")
                .setMessage("Do you want to unsubscribe and stop this package now?")
                .setPositiveButton("Stop", (dialog, which) -> stopPurchase(purchase))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void stopPurchase(PackagePurchase purchase) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        if (purchase == null || purchase.getPackageKey() == null) return;
        String packageKey = purchase.getPackageKey();
        long now = System.currentTimeMillis();
        String docId = user.getUid() + "_" + packageKey;

        Map<String, Object> updates = new HashMap<>();
        updates.put("expiresAt", now);
        updates.put("canceledAt", now);
        updates.put("status", "canceled");

        firestore.collection("package_purchases").document(docId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> history = new HashMap<>();
                    history.put("userId", user.getUid());
                    history.put("packageKey", packageKey);
                    history.put("purchasedAt", purchase.getPurchasedAt());
                    history.put("expiresAt", now);
                    history.put("planMonths", purchase.getPlanMonths());
                    history.put("price", purchase.getPrice());
                    history.put("paymentMethod", purchase.getPaymentMethod());
                    history.put("phoneNumberMasked", purchase.getPhoneNumberMasked());
                    history.put("status", "canceled");
                    history.put("canceledAt", now);
                    String historyDocId = user.getUid() + "_" + packageKey + "_" + now + "_canceled";
                    firestore.collection("package_purchase_history").document(historyDocId).set(history);

                    if (isAdded()) {
                        Toast.makeText(getContext(), "Package stopped", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to stop: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadProfilePicture(Uri uri) {
        if (getContext() == null) return;

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        applyProfileImage(uri);

        StorageReference ref = FirebaseStorage.getInstance()
                .getReference()
                .child("profile_images")
                .child(user.getUid() + ".jpg");

        ref.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("profileImageUrl", downloadUri.toString());
                            firestore.collection("users").document(user.getUid())
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        if (isAdded()) {
                                            Toast.makeText(getContext(), "Profile picture updated", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        if (isAdded()) {
                                            Toast.makeText(getContext(), "Failed to save profile picture: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        })
                        .addOnFailureListener(e -> {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }))
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showProfileImage(String profileImageUrl) {
        if (!isAdded() || getContext() == null) return;
        if (profileImageUrl == null || profileImageUrl.trim().isEmpty()) {
            applyDefaultProfileIcon();
            return;
        }

        try {
            StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(profileImageUrl);
            long maxBytes = 2L * 1024L * 1024L;
            ref.getBytes(maxBytes)
                    .addOnSuccessListener(bytes -> {
                        if (!isAdded()) return;
                        ivProfilePicture.setImageTintList(null);
                        ivProfilePicture.setPadding(0, 0, 0, 0);
                        ivProfilePicture.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        ivProfilePicture.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            applyDefaultProfileIcon();
                        }
                    });
        } catch (Exception e) {
            applyDefaultProfileIcon();
        }
    }

    private void applyProfileImage(Uri uri) {
        if (!isAdded()) return;
        ivProfilePicture.setImageTintList(null);
        ivProfilePicture.setPadding(0, 0, 0, 0);
        ivProfilePicture.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivProfilePicture.setImageURI(uri);
    }

    private void applyDefaultProfileIcon() {
        if (!isAdded() || getContext() == null) return;
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        ivProfilePicture.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ivProfilePicture.setPadding(padding, padding, padding, padding);
        ivProfilePicture.setImageResource(R.drawable.ic_profile);
        ivProfilePicture.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
    }

    private void showLogoutConfirmation() {
        if (!isAdded() || getContext() == null) return;
        
        new AlertDialog.Builder(getContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    if (!isAdded()) return;
                    mAuth.signOut();
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
