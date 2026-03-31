package com.example.fitlife.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.fitlife.R;

public class CalendarFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        TextView textView = new TextView(getContext());
        textView.setText("Calendar coming soon!");
        textView.setTextSize(24);
        textView.setGravity(android.view.Gravity.CENTER);
        if (getContext() != null) {
            textView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.main_background));
            textView.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        }
        return textView;
    }
}
