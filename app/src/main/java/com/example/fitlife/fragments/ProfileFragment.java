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
import com.example.fitlife.R;
import com.example.fitlife.activities.MainActivity;
import com.example.fitlife.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private ImageView ivProfilePicture;
    private TextView tvProfileName, tvProfileEmail, tvProfileDOB;
    private Button btnLogout;
    private View rlEditProfile;
    private SwitchCompat switchProfileTheme;
    private View btnUploadProfilePicture;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private ActivityResultLauncher<String> pickImageLauncher;

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

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                uploadProfilePicture(uri);
            }
        });

        loadUserProfile();
        setupThemeSwitch();

        rlEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        ivProfilePicture.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        if (btnUploadProfilePicture != null) {
            btnUploadProfilePicture.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        }

        return view;
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
