package com.example.fitlife.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.fitlife.R;
import com.example.fitlife.models.Workout;
import com.google.firebase.firestore.FirebaseFirestore;

public class DetailActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 101;
    private TextView detailTitle, tvDetailExercises, tvDetailEquipment, tvDetailInstructions, tvDetailDuration;
    private Button btnCompleteDetail, btnEditDetail, btnDeleteDetail, btnSendSMS;
    private LinearLayout smsInputLayout;
    private EditText etPhoneNumber;
    private ImageButton btnSendNow;
    private ImageView ivBack;
    private FirebaseFirestore firestore;
    private String workoutDocId;
    private Workout workout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        firestore = FirebaseFirestore.getInstance();
        workoutDocId = getIntent().getStringExtra("WORKOUT_DOC_ID");

        initViews();
        loadWorkoutFromFirestore();

        ivBack.setOnClickListener(v -> finish());
        
        btnCompleteDetail.setOnClickListener(v -> toggleCompleteInFirestore());
        
        btnEditDetail.setOnClickListener(v -> {
            Intent intent = new Intent(DetailActivity.this, AddWorkoutActivity.class);
            intent.putExtra("WORKOUT_DOC_ID", workoutDocId);
            startActivity(intent);
        });

        btnDeleteDetail.setOnClickListener(v -> showDeleteConfirmationFirestore());

        btnSendSMS.setOnClickListener(v -> {
            if (smsInputLayout.getVisibility() == View.VISIBLE) {
                smsInputLayout.setVisibility(View.GONE);
            } else {
                smsInputLayout.setVisibility(View.VISIBLE);
                etPhoneNumber.requestFocus();
            }
        });

        btnSendNow.setOnClickListener(v -> {
            if (checkSmsPermission()) {
                sendWorkoutSMS();
            } else {
                requestSmsPermission();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWorkoutFromFirestore(); // Refresh data if edited
    }

    private void initViews() {
        detailTitle = findViewById(R.id.detailTitle);
        tvDetailExercises = findViewById(R.id.tvDetailExercises);
        tvDetailEquipment = findViewById(R.id.tvDetailEquipment);
        tvDetailInstructions = findViewById(R.id.tvDetailInstructions);
        tvDetailDuration = findViewById(R.id.tvDetailDuration);
        btnCompleteDetail = findViewById(R.id.btnCompleteDetail);
        btnEditDetail = findViewById(R.id.btnEditDetail);
        btnDeleteDetail = findViewById(R.id.btnDeleteDetail);
        btnSendSMS = findViewById(R.id.btnSendSMS);
        smsInputLayout = findViewById(R.id.smsInputLayout);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnSendNow = findViewById(R.id.btnSendNow);
        ivBack = findViewById(R.id.ivBack);
    }

    private boolean checkSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendWorkoutSMS();
            } else {
                Toast.makeText(this, "Permission denied to send SMS", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendWorkoutSMS() {
        String phone = etPhoneNumber.getText().toString().trim();
        if (phone.isEmpty()) {
            etPhoneNumber.setError("Phone number required");
            return;
        }

        String fullNumber = "+95" + phone;
        String message = "Workout: " + workout.getTitle() + "\n" +
                "Exercises: " + workout.getExercises() + "\n" +
                "Instructions: " + workout.getInstructions();

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(fullNumber, null, message, null, null);
            Toast.makeText(this, "Workout sent to " + fullNumber, Toast.LENGTH_SHORT).show();
            smsInputLayout.setVisibility(View.GONE);
            etPhoneNumber.setText("");
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadWorkoutFromFirestore() {
        if (workoutDocId == null) return;
        
        firestore.collection("workouts").document(workoutDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    workout = documentSnapshot.toObject(Workout.class);
                    if (workout != null) {
                        detailTitle.setText(workout.getTitle());
                        tvDetailExercises.setText(workout.getExercises() != null && !workout.getExercises().isEmpty() ? workout.getExercises() : "No exercises listed.");
                        tvDetailEquipment.setText(workout.getEquipment() != null && !workout.getEquipment().isEmpty() ? workout.getEquipment() : "No equipment needed.");
                        tvDetailInstructions.setText(workout.getInstructions() != null && !workout.getInstructions().isEmpty() ? workout.getInstructions() : "No instructions provided.");
                        tvDetailDuration.setText("Duration: " + workout.getDuration() + " mins");
                        
                        updateCompleteButton(workout.isCompleted());
                    } else {
                        Toast.makeText(this, "Workout not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void toggleCompleteInFirestore() {
        boolean newStatus = !workout.isCompleted();
        firestore.collection("workouts").document(workoutDocId)
                .update("completed", newStatus)
                .addOnSuccessListener(aVoid -> {
                    workout.setCompleted(newStatus);
                    updateCompleteButton(newStatus);
                    Toast.makeText(this, newStatus ? "Workout completed!" : "Workout marked as pending", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateCompleteButton(boolean isCompleted) {
        if (isCompleted) {
            btnCompleteDetail.setText("Mark as Pending");
            btnCompleteDetail.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY));
        } else {
            btnCompleteDetail.setText("Mark as Completed");
            btnCompleteDetail.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.accent_orange)));
        }
    }

    private void showDeleteConfirmationFirestore() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Workout")
                .setMessage("Are you sure you want to delete this workout?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    firestore.collection("workouts").document(workoutDocId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Workout deleted from cloud", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
