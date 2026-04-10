package com.example.fitlife.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fitlife.R;
import com.example.fitlife.models.PlanItem;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PlanItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onItemClick(PlanItem item);
        void onItemLongClick(PlanItem item);
        void onToggleAdded(PlanItem item, boolean added);
    }

    private static final int VIEW_TYPE_ROUTINE = 0;
    private static final int VIEW_TYPE_PACKAGE = 1;

    private final List<PlanItem> items;
    private final Listener listener;
    private final boolean showToggle;

    public PlanItemAdapter(List<PlanItem> items, Listener listener, boolean showToggle) {
        this.items = items;
        this.listener = listener;
        this.showToggle = showToggle;
    }

    @Override
    public int getItemViewType(int position) {
        PlanItem item = items.get(position);
        return "package".equalsIgnoreCase(item.getType()) ? VIEW_TYPE_PACKAGE : VIEW_TYPE_ROUTINE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_PACKAGE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_package, parent, false);
            return new PackageVH(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plan_item, parent, false);
        return new RoutineVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        PlanItem item = items.get(position);
        if (holder instanceof PackageVH) {
            bindPackage((PackageVH) holder, item);
        } else if (holder instanceof RoutineVH) {
            bindRoutine((RoutineVH) holder, item);
        }
    }

    private void bindRoutine(@NonNull RoutineVH holder, @NonNull PlanItem item) {
        holder.tvTitle.setText(getDisplayTitle(item));

        if (item.getCreatedAt() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.tvCreatedAt.setText(sdf.format(new Date(item.getCreatedAt())));
            holder.tvCreatedAt.setVisibility(View.VISIBLE);
        } else {
            holder.tvCreatedAt.setVisibility(View.GONE);
        }

        if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
            holder.tvNotes.setText(item.getNotes());
            holder.tvNotes.setVisibility(View.VISIBLE);
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }

        holder.switchAdded.setOnCheckedChangeListener(null);
        holder.switchAdded.setChecked(item.isAddedToWeeklyPlan());
        holder.switchAdded.setVisibility(showToggle ? View.VISIBLE : View.GONE);

        holder.switchAdded.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onToggleAdded(item, isChecked);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onItemLongClick(item);
                return true;
            }
            return false;
        });

        holder.ivIcon.setImageResource(android.R.drawable.ic_menu_my_calendar);
    }

    @NonNull
    private String getDisplayTitle(@NonNull PlanItem item) {
        String title = item.getTitle();
        if (title != null) {
            title = title.trim();
        }
        if (title != null && !title.isEmpty()) {
            return title;
        }

        String key = item.getKey();
        if (key != null) {
            key = key.trim();
        }
        if (key != null && !key.isEmpty()) {
            String spaced = key.replace('_', ' ');
            if (!spaced.isEmpty()) {
                return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
            }
        }

        return "Untitled Routine";
    }

    private void bindPackage(@NonNull PackageVH holder, @NonNull PlanItem item) {
        holder.tvTitle.setText(item.getTitle());
        holder.tvSubtitle.setText(item.getNotes() != null ? item.getNotes() : "");

        String key = item.getKey() != null ? item.getKey() : "";
        int iconRes;
        if ("diet_plan".equalsIgnoreCase(key)) {
            iconRes = R.drawable.ic_diet;
        } else if ("gym".equalsIgnoreCase(key)) {
            iconRes = R.drawable.ic_weights;
        } else if ("yoga".equalsIgnoreCase(key)) {
            iconRes = R.drawable.ic_yoga;
        } else {
            iconRes = R.drawable.ic_statistics;
        }
        holder.ivPackageIcon.setImageResource(iconRes);

        boolean active = item.isPurchased() && item.getExpiresAt() > System.currentTimeMillis();
        if (active) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.tvStatus.setText("Expire: " + sdf.format(new Date(item.getExpiresAt())));
            holder.ivStatusIcon.setImageResource(android.R.drawable.checkbox_on_background);
            holder.ivStatusIcon.setColorFilter(holder.itemView.getResources().getColor(R.color.accent_orange));
            holder.itemView.setAlpha(1f);
        } else {
            if (item.isPurchased() && item.getExpiresAt() > 0) {
                holder.tvStatus.setText("Expired • Buy again");
            } else {
                holder.tvStatus.setText("Locked • Need to buy");
            }
            holder.ivStatusIcon.setImageResource(android.R.drawable.ic_lock_lock);
            holder.ivStatusIcon.setColorFilter(holder.itemView.getResources().getColor(R.color.text_secondary));
            holder.itemView.setAlpha(0.9f);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onItemLongClick(item);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class RoutineVH extends RecyclerView.ViewHolder {
        final ImageView ivIcon;
        final TextView tvTitle;
        final TextView tvCreatedAt;
        final TextView tvNotes;
        final SwitchCompat switchAdded;

        RoutineVH(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            switchAdded = itemView.findViewById(R.id.switchAdded);
        }
    }

    static class PackageVH extends RecyclerView.ViewHolder {
        final ImageView ivPackageIcon;
        final TextView tvTitle;
        final TextView tvSubtitle;
        final ImageView ivStatusIcon;
        final TextView tvStatus;

        PackageVH(@NonNull View itemView) {
            super(itemView);
            ivPackageIcon = itemView.findViewById(R.id.ivPackageIcon);
            tvTitle = itemView.findViewById(R.id.tvPackageTitle);
            tvSubtitle = itemView.findViewById(R.id.tvPackageSubtitle);
            ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
