package com.example.fitlife.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fitlife.R;
import com.example.fitlife.models.PlanEntry;
import java.util.List;

public class PlanEntryAdapter extends RecyclerView.Adapter<PlanEntryAdapter.VH> {

    private final List<PlanEntry> entries;

    public PlanEntryAdapter(List<PlanEntry> entries) {
        this.entries = entries;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plan_entry, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PlanEntry entry = entries.get(position);
        holder.tvName.setText(entry.getName());
        holder.tvInstructions.setText(entry.getInstructions());
    }

    @Override
    public int getItemCount() {
        return entries != null ? entries.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvInstructions;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvInstructions = itemView.findViewById(R.id.tvInstructions);
        }
    }
}

