package com.example.fitlife.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fitlife.R;
import com.example.fitlife.models.PackagePurchase;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PurchaseAdapter extends RecyclerView.Adapter<PurchaseAdapter.VH> {

    public interface Listener {
        void onStop(PackagePurchase purchase);
    }

    private final List<PackagePurchase> purchases;
    private final Listener listener;

    public PurchaseAdapter(List<PackagePurchase> purchases, Listener listener) {
        this.purchases = purchases;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_purchase, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PackagePurchase p = purchases.get(position);
        String key = p.getPackageKey() != null ? p.getPackageKey() : "";

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

        int months = p.getPlanMonths();
        double price = p.getPrice();
        String monthLabel = months == 1 ? "1 month" : months + " months";
        DecimalFormat df = new DecimalFormat("0.00");
        String method = p.getPaymentMethod() != null && !p.getPaymentMethod().trim().isEmpty() ? p.getPaymentMethod().trim() : "Premium";
        String phone = p.getPhoneNumberMasked() != null && !p.getPhoneNumberMasked().trim().isEmpty() ? p.getPhoneNumberMasked().trim() : "";
        String phonePart = phone.isEmpty() ? "" : " • " + phone;
        holder.tvPlan.setText("Plan: " + monthLabel + " • $" + df.format(price) + " • " + method + phonePart);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        holder.tvExpires.setText("Expire: " + sdf.format(new Date(p.getExpiresAt())));

        boolean active = p.getExpiresAt() > System.currentTimeMillis();
        holder.tvStop.setVisibility(active ? View.VISIBLE : View.GONE);
        holder.tvStop.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStop(p);
            }
        });
    }

    @Override
    public int getItemCount() {
        return purchases != null ? purchases.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView ivIcon;
        final TextView tvTitle;
        final TextView tvPlan;
        final TextView tvExpires;
        final TextView tvStop;

        VH(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvPlan = itemView.findViewById(R.id.tvPlan);
            tvExpires = itemView.findViewById(R.id.tvExpires);
            tvStop = itemView.findViewById(R.id.tvStop);
        }
    }
}
