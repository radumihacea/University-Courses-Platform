package ro.utcn.scd.mobile.ui.chat;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ro.utcn.scd.mobile.data.model.ChatMessage;

/** Bulele conversatiei: intrebarile utilizatorului (dreapta) si raspunsurile asistentului (stanga). */
class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.Holder> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_ASSISTANT = 1;

    private final List<ChatMessage> messages;

    ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? TYPE_USER : TYPE_ASSISTANT;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_USER
                ? ro.utcn.scd.mobile.R.layout.item_chat_user
                : ro.utcn.scd.mobile.R.layout.item_chat_assistant;
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(messages.get(position));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView text;
        final TextView meta;
        /** culoarea normala a textului (din layout), ca sa o putem reface dupa o eroare */
        final ColorStateList normalColor;

        Holder(View view) {
            super(view);
            text = view.findViewById(ro.utcn.scd.mobile.R.id.text);
            meta = view.findViewById(ro.utcn.scd.mobile.R.id.meta);
            normalColor = text.getTextColors();
        }

        void bind(ChatMessage m) {
            if (m.isUser()) {
                text.setText(m.content);
                return;
            }
            boolean waiting = m.streaming && m.content.isEmpty();
            // cat timp asteptam primul cuvant; apoi textul creste pe masura ce soseste, cu un cursor
            text.setText(waiting ? "citește orarul..." : m.streaming ? m.content + " ▍" : m.content);
            text.setTypeface(null, waiting ? Typeface.ITALIC : Typeface.NORMAL);
            if (m.error) {
                text.setTextColor(ContextCompat.getColor(text.getContext(), ro.utcn.scd.mobile.R.color.error_text));
            } else {
                text.setTextColor(normalColor);
            }

            if (meta != null) {
                meta.setVisibility(m.meta != null ? View.VISIBLE : View.GONE);
                meta.setText(m.meta);
            }
        }
    }
}
