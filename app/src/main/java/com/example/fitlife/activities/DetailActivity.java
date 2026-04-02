package com.example.fitlife.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fitlife.R;
import com.example.fitlife.models.Workout;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class DetailActivity extends AppCompatActivity {

    private TextView detailTitle, tvDetailExercises, tvDetailEquipment, tvDetailInstructions, tvDetailDuration;
    private Button btnCompleteDetail, btnEditDetail, btnDeleteDetail, btnSendEmail;
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

        btnSendEmail.setOnClickListener(v -> {
            if (workout == null) {
                Toast.makeText(this, "Workout not loaded yet", Toast.LENGTH_SHORT).show();
                return;
            }
            showEmailShareDialog();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWorkoutFromFirestore(); // Refresh data if edited
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
        btnSendEmail = findViewById(R.id.btnSendEmail);
        ivBack = findViewById(R.id.ivBack);
    }

    private void showEmailShareDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_email_share, null, false);
        RadioGroup rgFormat = view.findViewById(R.id.rgFormat);
        android.widget.CheckBox cbEquipment = view.findViewById(R.id.cbEquipment);
        android.widget.CheckBox cbInstructions = view.findViewById(R.id.cbInstructions);
        android.widget.CheckBox cbDuration = view.findViewById(R.id.cbDuration);
        EditText etEmail = view.findViewById(R.id.etEmail);

        rgFormat.check(R.id.rbText);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Send Workout Plan")
                .setView(view)
                .setPositiveButton("Send", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int checked = rgFormat.getCheckedRadioButtonId();
            boolean checklist = checked == R.id.rbChecklist;

            String recipient = etEmail.getText().toString().trim();
            if (!recipient.isEmpty() && !isValidEmail(recipient)) {
                etEmail.setError("Valid email required");
                return;
            }
            String subject = "Workout Plan: " + safe(workout.getTitle());
            String body = buildWorkoutText(checklist, cbEquipment.isChecked(), cbInstructions.isChecked(), cbDuration.isChecked());
            openGmailCompose(recipient, subject, body);
            dialog.dismiss();
        }));

        dialog.show();
    }

    private void openGmailCompose(String recipient, String subject, String body) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        if (recipient != null && !recipient.trim().isEmpty()) {
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{recipient.trim()});
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        Intent gmailIntent = new Intent(intent);
        gmailIntent.setPackage("com.google.android.gm");
        if (gmailIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(gmailIntent);
            return;
        }

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(intent, "Send email"));
        } else {
            Toast.makeText(this, "No email app found. Please install Gmail.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        String e = email.trim();
        if (e.isEmpty()) return false;
        return android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches();
    }

    private String buildWorkoutText(boolean checklist, boolean includeEquipment, boolean includeInstructions, boolean includeDuration) {
        StringBuilder sb = new StringBuilder();
        sb.append("Workout: ").append(safe(workout.getTitle())).append("\n");
        if (includeDuration) {
            sb.append("Duration: ").append(workout.getDuration()).append(" mins").append("\n");
        }
        sb.append("\n");

        String exercises = safe(workout.getExercises());
        if (!exercises.isEmpty()) {
            sb.append("Exercises:\n");
            if (checklist) {
                for (String line : splitExercises(exercises)) {
                    sb.append("- [ ] ").append(line).append("\n");
                }
            } else {
                sb.append(exercises).append("\n");
            }
            sb.append("\n");
        }

        if (includeEquipment) {
            String equipment = safe(workout.getEquipment());
            if (!equipment.isEmpty()) {
                sb.append("Equipment:\n").append(equipment).append("\n\n");
            }
        }

        if (includeInstructions) {
            String instructions = safe(workout.getInstructions());
            if (!instructions.isEmpty()) {
                sb.append("Instructions:\n").append(instructions).append("\n");
            }
        }

        return sb.toString().trim();
    }

    private List<String> splitExercises(String exercises) {
        List<String> out = new ArrayList<>();
        for (String part : exercises.split("\\r?\\n|,")) {
            String s = part.trim();
            if (!s.isEmpty()) out.add(s);
        }
        if (out.isEmpty()) {
            out.add(exercises.trim());
        }
        return out;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
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
