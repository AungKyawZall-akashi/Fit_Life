package com.example.fitlife.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fitlife.R;
import com.example.fitlife.models.PurchaseHistoryRecord;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PurchaseHistoryAdapter extends RecyclerView.Adapter<PurchaseHistoryAdapter.VH> {

    private final List<PurchaseHistoryRecord> items;

    public PurchaseHistoryAdapter(List<PurchaseHistoryRecord> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_purchase_history, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PurchaseHistoryRecord r = items.get(position);
        String key = r.getPackageKey() != null ? r.getPackageKey() : "";

        if ("diet_plan".equalsIgnoreCase(key)) {
            holder.tvTitle.setText("Diet Plan");
            holder.ivIcon.setImageResource(R.drawable.ic_diet);
        } else if ("gym".equalsIgnoreCase(key)) {
            holder.tvTitle.setText("Gym");
            holder.ivIcon.setImageResource(R.drawable.ic_weights);
        } else if ("yoga".equalsIgnoreCase(key)) {
            holder.tvTitle.setText("Yoga");
            holder.ivIcon.setImageResource(R.drawable.ic_yoga);
        } else {
            holder.tvTitle.setText("Package");
            holder.ivIcon.setImageResource(R.drawable.ic_statistics);
        }

        String status = r.getStatus() != null && !r.getStatus().trim().isEmpty() ? r.getStatus().trim() : "purchased";
        status = status.substring(0, 1).toUpperCase(Locale.getDefault()) + status.substring(1);

        int months = r.getPlanMonths();
        String monthLabel = months == 1 ? "1 month" : months + " months";
        DecimalFormat df = new DecimalFormat("0.00");

        String method = r.getPaymentMethod() != null && !r.getPaymentMethod().trim().isEmpty() ? r.getPaymentMethod().trim() : "Premium";
        holder.tvMeta.setText(status + " • " + monthLabel + " • $" + df.format(r.getPrice()) + " • " + method);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        long eventTime = r.getCanceledAt() > 0 ? r.getCanceledAt() : r.getPurchasedAt();
        holder.tvPurchasedAt.setText(sdf.format(new Date(eventTime)));
        holder.tvExpiresAt.setText("Expire: " + sdf.format(new Date(r.getExpiresAt())));
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView ivIcon;
        final TextView tvTitle;
        final TextView tvMeta;
        final TextView tvPurchasedAt;
        final TextView tvExpiresAt;

        VH(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvPurchasedAt = itemView.findViewById(R.id.tvPurchasedAt);
            tvExpiresAt = itemView.findViewById(R.id.tvExpiresAt);
        }
    }
}
