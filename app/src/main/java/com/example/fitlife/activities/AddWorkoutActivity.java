package com.example.fitlife.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fitlife.R;
import com.example.fitlife.models.Workout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddWorkoutActivity extends AppCompatActivity {

    private EditText etWorkoutName, etExercises, etEquipment, etInstructions, etDuration;
    private Button btnSaveWorkout;
    private android.widget.ImageView ivBack;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private String workoutDocId = null;
    private long existingTimestamp = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_workout);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etWorkoutName = findViewById(R.id.etWorkoutName);
        etExercises = findViewById(R.id.etExercises);
        etEquipment = findViewById(R.id.etEquipment);
        etInstructions = findViewById(R.id.etInstructions);
        etDuration = findViewById(R.id.etDuration);
        btnSaveWorkout = findViewById(R.id.btnSaveWorkout);
        ivBack = findViewById(R.id.ivBack);

        // Check for edit mode
        if (getIntent().hasExtra("WORKOUT_DOC_ID")) {
            workoutDocId = getIntent().getStringExtra("WORKOUT_DOC_ID");
            loadWorkoutDataFromFirestore(workoutDocId);
            findViewById(R.id.addWorkoutTitle).setTag("Update Workout");
            ((android.widget.TextView)findViewById(R.id.addWorkoutTitle)).setText("Edit Workout");
            btnSaveWorkout.setText("Update Workout");
        }

        ivBack.setOnClickListener(v -> finish());
        btnSaveWorkout.setOnClickListener(v -> saveWorkoutToFirestore());
    }

    private void loadWorkoutDataFromFirestore(String docId) {
        firestore.collection("workouts").document(docId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Workout workout = documentSnapshot.toObject(Workout.class);
                    if (workout != null) {
                        existingTimestamp = workout.getTimestamp();
                        etWorkoutName.setText(workout.getTitle());
                        etExercises.setText(workout.getExercises());
                        etEquipment.setText(workout.getEquipment());
                        etInstructions.setText(workout.getInstructions());
                        etDuration.setText(String.valueOf(workout.getDuration()));
                    }
                });
    }

    private void saveWorkoutToFirestore() {
        String name = etWorkoutName.getText().toString().trim();
        String exercises = etExercises.getText().toString().trim();
        String equipment = etEquipment.getText().toString().trim();
        String instructions = etInstructions.getText().toString().trim();
        String durationStr = etDuration.getText().toString().trim();

        if (name.isEmpty()) {
            etWorkoutName.setError("Workout name is required");
            return;
        }

        int duration = 0;
        if (!durationStr.isEmpty()) {
            try {
                duration = Integer.parseInt(durationStr);
            } catch (NumberFormatException e) {
                etDuration.setError("Invalid duration");
                return;
            }
        }

        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";

        Workout workout = new Workout();
        workout.setTitle(name);
        workout.setExercises(exercises);
        workout.setEquipment(equipment);
        workout.setInstructions(instructions);
        workout.setDuration(duration);
        workout.setCompleted(false);
        workout.setUserId(userId); // Explicitly set the userId field
        workout.setDescription(""); // Clear the misused description field
        long now = System.currentTimeMillis();
        workout.setTimestamp(workoutDocId != null ? (existingTimestamp != 0L ? existingTimestamp : now) : now);

        btnSaveWorkout.setEnabled(false);

        if (workoutDocId != null) {
            // Update existing
            workout.setDocumentId(workoutDocId);
            firestore.collection("workouts").document(workoutDocId)
                    .set(workout)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Workout updated!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnSaveWorkout.setEnabled(true);
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Add new
            firestore.collection("workouts")
                    .add(workout)
                    .addOnSuccessListener(documentReference -> {
                        String id = documentReference.getId();
                        documentReference.update("documentId", id);
                        Toast.makeText(this, "Workout saved to cloud!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnSaveWorkout.setEnabled(true);
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
