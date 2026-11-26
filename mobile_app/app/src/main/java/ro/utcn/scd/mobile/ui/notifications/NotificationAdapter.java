package ro.utcn.scd.mobile.ui.notifications;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ro.utcn.scd.mobile.R;
import ro.utcn.scd.mobile.data.model.AppNotification;
import ro.utcn.scd.mobile.ui.common.Labels;

/** Lista de notificari: cele necitite au bulina si text ingrosat. Click = marcheaza ca citita. */
class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.Holder> {

    interface OnClick {
        void onClick(AppNotification notification, int position);
    }

    private final List<AppNotification> items;
    private final OnClick onClick;

    NotificationAdapter(List<AppNotification> items, OnClick onClick) {
        this.items = items;
        this.onClick = onClick;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        AppNotification n = items.get(position);
        holder.message.setText(n.message);
        holder.message.setTypeface(null, n.isRead() ? Typeface.NORMAL : Typeface.BOLD);
        holder.time.setText(Labels.relativeTime(n.createdAt));
        holder.dot.setVisibility(n.isRead() ? View.INVISIBLE : View.VISIBLE);
        holder.itemView.setOnClickListener(v -> onClick.onClick(n, holder.getBindingAdapterPosition()));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView message;
        final TextView time;
        final View dot;

        Holder(View view) {
            super(view);
            message = view.findViewById(R.id.message);
            time = view.findViewById(R.id.time);
            dot = view.findViewById(R.id.unread_dot);
        }
    }
}
