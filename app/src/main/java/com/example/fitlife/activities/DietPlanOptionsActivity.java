package com.example.fitlife.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fitlife.R;

public class DietPlanOptionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diet_plan_options);

        ImageView ivBack = findViewById(R.id.ivBack);
        android.view.View cardStatic = findViewById(R.id.cardStatic);
        android.view.View cardPersonal = findViewById(R.id.cardPersonal);

        ivBack.setOnClickListener(v -> finish());

        cardStatic.setOnClickListener(v -> {
            Intent intent = new Intent(this, PlanItemDetailActivity.class);
            intent.putExtra(PlanItemDetailActivity.EXTRA_PACKAGE_KEY, "diet_plan");
            intent.putExtra(PlanItemDetailActivity.EXTRA_DIET_MODE, PlanItemDetailActivity.DIET_MODE_STATIC);
            startActivity(intent);
        });

        cardPersonal.setOnClickListener(v -> {
            Intent intent = new Intent(this, PersonalDietPlanActivity.class);
            startActivity(intent);
        });
    }
}

