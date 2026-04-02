package com.example.fitlife.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

        if (entry.getMeta() != null && !entry.getMeta().trim().isEmpty()) {
            holder.tvMeta.setText(entry.getMeta());
            holder.tvMeta.setVisibility(View.VISIBLE);
        } else {
            holder.tvMeta.setVisibility(View.GONE);
        }

        int iconRes = resolveIcon(entry.getImageKey());
        holder.ivEntryIcon.setImageResource(iconRes);
    }

    private int resolveIcon(String imageKey) {
        if (imageKey == null) return R.drawable.ic_statistics;
        String k = imageKey.trim().toLowerCase();
        if ("diet".equals(k)) return R.drawable.ic_diet;
        if ("water".equals(k)) return R.drawable.ic_swim;
        if ("run".equals(k)) return R.drawable.ic_run;
        if ("weights".equals(k)) return R.drawable.ic_weights;
        if ("yoga".equals(k)) return R.drawable.ic_yoga;
        return R.drawable.ic_statistics;
    }

    @Override
    public int getItemCount() {
        return entries != null ? entries.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView ivEntryIcon;
        final TextView tvName;
        final TextView tvMeta;
        final TextView tvInstructions;

        VH(@NonNull View itemView) {
            super(itemView);
            ivEntryIcon = itemView.findViewById(R.id.ivEntryIcon);
            tvName = itemView.findViewById(R.id.tvName);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvInstructions = itemView.findViewById(R.id.tvInstructions);
        }
    }
}
