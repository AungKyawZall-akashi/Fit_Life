package com.example.fitlife.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fitlife.R;
import com.example.fitlife.models.PlanEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PersonalDietPlanActivity extends AppCompatActivity {

    private Spinner spGoal;
    private Spinner spMeals;
    private Spinner spPreference;
    private EditText etAllergies;
    private Button btnGenerate;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_diet_plan);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        ImageView ivBack = findViewById(R.id.ivBack);
        spGoal = findViewById(R.id.spGoal);
        spMeals = findViewById(R.id.spMeals);
        spPreference = findViewById(R.id.spPreference);
        etAllergies = findViewById(R.id.etAllergies);
        btnGenerate = findViewById(R.id.btnGenerate);

        ivBack.setOnClickListener(v -> finish());

        ArrayAdapter<String> goalAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Weight loss", "Maintain", "Weight gain"});
        spGoal.setAdapter(goalAdapter);

        ArrayAdapter<String> mealsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"3", "4", "5"});
        spMeals.setAdapter(mealsAdapter);

        ArrayAdapter<String> prefAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"No preference", "Vegetarian", "Vegan", "Halal"});
        spPreference.setAdapter(prefAdapter);

        btnGenerate.setOnClickListener(v -> savePersonalPlan());
    }

    private void savePersonalPlan() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        String goal = String.valueOf(spGoal.getSelectedItem());
        String preference = String.valueOf(spPreference.getSelectedItem());
        String allergies = etAllergies.getText().toString().trim();

        List<PlanEntry> entries = buildPlan(goal, preference);

        openPersonalPlan(entries, "Plan generated");

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId != null ? userId : "anonymous");
        data.put("goal", goal);
        data.put("mealsPerDay", 5);
        data.put("preference", preference);
        data.put("allergies", allergies);
        data.put("updatedAt", System.currentTimeMillis());
        data.put("entries", toMapList(entries));

        if (userId != null) {
            firestore.collection("personal_diet_plans").document(userId)
                    .set(data);
        }
    }

    private void openPersonalPlan(List<PlanEntry> entries, String toast) {
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, PlanItemDetailActivity.class);
        intent.putExtra(PlanItemDetailActivity.EXTRA_PACKAGE_KEY, "diet_plan");
        intent.putExtra(PlanItemDetailActivity.EXTRA_DIET_MODE, PlanItemDetailActivity.DIET_MODE_PERSONAL);
        intent.putExtra(PlanItemDetailActivity.EXTRA_PERSONAL_PLAN_JSON, buildJson(entries));
        startActivity(intent);
        finish();
    }

    private String buildJson(List<PlanEntry> entries) {
        try {
            JSONArray arr = new JSONArray();
            for (PlanEntry e : entries) {
                JSONObject obj = new JSONObject();
                obj.put("name", e.getName());
                obj.put("instructions", e.getInstructions());
                obj.put("meta", e.getMeta());
                obj.put("imageKey", e.getImageKey());
                arr.put(obj);
            }
            return arr.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<Map<String, Object>> toMapList(List<PlanEntry> entries) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (PlanEntry e : entries) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", e.getName());
            m.put("instructions", e.getInstructions());
            m.put("meta", e.getMeta());
            m.put("imageKey", e.getImageKey());
            list.add(m);
        }
        return list;
    }

    private List<PlanEntry> buildPlan(String goal, String preference) {
        String g = goal != null ? goal.toLowerCase(Locale.getDefault()) : "";
        String pref = preference != null ? preference.toLowerCase(Locale.getDefault()) : "";

        List<PlanEntry> list = new ArrayList<>();
        String protein = pref.contains("vegan") ? "tofu/beans" : pref.contains("vegetarian") ? "eggs/beans" : "chicken/fish";

        if (g.contains("loss")) {
            list.add(new PlanEntry("Breakfast", "High-protein: " + protein + " + fruit.", "Meal 1 • Morning", "diet"));
            list.add(new PlanEntry("Snack 1", "Nuts or yogurt (small portion).", "Meal 2 • Mid-morning", "diet"));
            list.add(new PlanEntry("Lunch", "Balanced plate: " + protein + " + vegetables + small carbs.", "Meal 3 • Midday", "diet"));
            list.add(new PlanEntry("Snack 2", "Fruit or salad.", "Meal 4 • Afternoon", "diet"));
            list.add(new PlanEntry("Dinner", "Low-carb: vegetables + " + protein + ".", "Meal 5 • Evening", "diet"));
        } else if (g.contains("gain")) {
            list.add(new PlanEntry("Breakfast", "Oats + " + protein + " + banana.", "Meal 1 • Morning", "diet"));
            list.add(new PlanEntry("Snack 1", "Peanut butter toast or nuts.", "Meal 2 • Mid-morning", "diet"));
            list.add(new PlanEntry("Lunch", "Rice/pasta + " + protein + " + vegetables.", "Meal 3 • Midday", "diet"));
            list.add(new PlanEntry("Snack 2", "Milk/yogurt smoothie.", "Meal 4 • Afternoon", "diet"));
            list.add(new PlanEntry("Dinner", protein + " + potatoes + vegetables.", "Meal 5 • Evening", "diet"));
        } else {
            list.add(new PlanEntry("Breakfast", "Eggs/oats + fruit.", "Meal 1 • Morning", "diet"));
            list.add(new PlanEntry("Snack 1", "Nuts or yogurt.", "Meal 2 • Mid-morning", "diet"));
            list.add(new PlanEntry("Lunch", protein + " + vegetables + rice.", "Meal 3 • Midday", "diet"));
            list.add(new PlanEntry("Snack 2", "Fruit.", "Meal 4 • Afternoon", "diet"));
            list.add(new PlanEntry("Dinner", "Light meal: vegetables + " + protein + ".", "Meal 5 • Evening", "diet"));
        }

        list.add(new PlanEntry("Hydration", "Drink water regularly throughout the day.", "All day • 6-8 glasses", "water"));
        return list;
    }
}
