package ro.utcn.scd.mobile.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import ro.utcn.scd.mobile.R;
import ro.utcn.scd.mobile.data.ApiClient;
import ro.utcn.scd.mobile.data.Session;
import ro.utcn.scd.mobile.data.model.ChatMessage;

/**
 * Chatbot-ul pentru orar. Intrebarea este trimisa la backend, care adauga orarul
 * utilizatorului si o trimite modelului AI local (Ollama). Raspunsul apare treptat,
 * cuvant cu cuvant, iar butonul de trimitere devine "Stop" cat timp se scrie.
 */
public class ChatFragment extends Fragment {

    /** Cate mesaje anterioare se trimit ca istoric (pentru intrebari de tipul "si joi?"). */
    private static final int MAX_HISTORY = 6;

    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatAdapter adapter;
    private RecyclerView list;
    private View intro;
    private TextInputEditText input;
    private FloatingActionButton send;
    private Call currentCall;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        list = view.findViewById(R.id.messages);
        intro = view.findViewById(R.id.intro);
        input = view.findViewById(R.id.input);
        send = view.findViewById(R.id.send);

        adapter = new ChatAdapter(messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        list.setLayoutManager(layoutManager);
        list.setAdapter(adapter);
        // cand apare tastatura, lista ramane la ultimul mesaj
        list.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (b < ob) {
                scrollToEnd();
            }
        });

        ChipGroup suggestions = view.findViewById(R.id.suggestions);
        String[] questions = Session.get(requireContext()).isProfessor()
                ? new String[]{"Ce ore am azi?", "Ce predau mâine?", "În ce săli predau marți?", "Câte ore am pe săptămână?"}
                : new String[]{"Ce ore am azi?", "Când am ore miercuri?", "În ce sală e laboratorul de SCD?", "Am ore vineri?"};
        for (String q : questions) {
            Chip chip = new Chip(requireContext());
            chip.setText(q);
            chip.setOnClickListener(v -> ask(q));
            suggestions.addView(chip);
        }

        send.setOnClickListener(v -> {
            if (currentCall != null) {
                stop();
            } else {
                ask(text());
            }
        });
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                ask(text());
                return true;
            }
            return false;
        });
        updateState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stop();
    }

    private void ask(String question) {
        if (question.isEmpty() || currentCall != null) {
            return;
        }
        input.setText("");

        // istoricul conversatiei de pana acum (fara erori), pentru intrebari de continuare
        List<ChatMessage> history = new ArrayList<>();
        for (ChatMessage m : messages) {
            if (!m.error && !m.content.isEmpty()) {
                history.add(new ChatMessage(m.role, m.content));
            }
        }
        if (history.size() > MAX_HISTORY) {
            history = new ArrayList<>(history.subList(history.size() - MAX_HISTORY, history.size()));
        }

        messages.add(new ChatMessage("user", question));
        ChatMessage answer = new ChatMessage("assistant", "");
        answer.streaming = true;
        messages.add(answer);
        int answerPosition = messages.size() - 1;
        adapter.notifyItemRangeInserted(messages.size() - 2, 2);
        scrollToEnd();

        StringBuilder text = new StringBuilder();
        currentCall = ApiClient.get(requireContext()).chatStream(question, history, new ApiClient.StreamCallback() {
            @Override
            public void onToken(String token) {
                text.append(token);
                answer.content = text.toString();
                refresh(answerPosition);
            }

            @Override
            public void onDone(String model, long durationMs, Long firstTokenMs) {
                answer.content = text.toString().trim();
                answer.streaming = false;
                // raspunsurile instant (direct din orar) nu au nevoie de timpi detaliati
                answer.meta = durationMs < 1000 ? model : model
                        + (firstTokenMs != null ? " · primul cuvânt după " + seconds(firstTokenMs) : "")
                        + " · total " + seconds(durationMs);
                finish(answerPosition);
            }

            @Override
            public void onError(String message) {
                answer.streaming = false;
                if (text.length() > 0) {
                    // s-a intrerupt la jumatate: pastram ce a venit si aratam eroarea dedesubt
                    answer.content = text.toString().trim();
                    answer.meta = message;
                } else {
                    answer.content = message;
                    answer.error = true;
                }
                finish(answerPosition);
            }
        });
        updateState();
    }

    /** Opreste raspunsul in curs (modelul se opreste si pe server). */
    private void stop() {
        if (currentCall == null) {
            return;
        }
        currentCall.cancel();
        currentCall = null;
        if (!messages.isEmpty()) {
            ChatMessage last = messages.get(messages.size() - 1);
            if (last.streaming) {
                last.streaming = false;
                if (last.content.isEmpty()) {
                    last.content = "(răspuns oprit)";
                }
                last.meta = "oprit";
                if (adapter != null) {
                    adapter.notifyItemChanged(messages.size() - 1);
                }
            }
        }
        if (send != null) {
            updateState();
        }
    }

    private void finish(int position) {
        currentCall = null;
        if (!isAdded()) {
            return;
        }
        refresh(position);
        updateState();
    }

    private void refresh(int position) {
        if (!isAdded()) {
            return;
        }
        adapter.notifyItemChanged(position);
        scrollToEnd();
    }

    private void updateState() {
        boolean busy = currentCall != null;
        send.setImageResource(busy ? R.drawable.ic_stop : R.drawable.ic_send);
        send.setContentDescription(busy ? "Oprește răspunsul" : "Trimite");
        intro.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void scrollToEnd() {
        if (!messages.isEmpty()) {
            list.post(() -> list.scrollToPosition(messages.size() - 1));
        }
    }

    private String text() {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private static String seconds(long ms) {
        return String.format(Locale.US, "%.1f s", ms / 1000.0);
    }
}
