package ro.utcn.scd.mobile.ui.optionals;

import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ro.utcn.scd.mobile.data.ApiClient;
import ro.utcn.scd.mobile.data.model.OptionalCourse;
import ro.utcn.scd.mobile.ui.common.CardListActivity;
import ro.utcn.scd.mobile.ui.common.Labels;

/**
 * Disciplinele optionale ale studentului. Dupa inscriere, orele apar automat
 * in orarul personal (Azi / Saptamana).
 */
public class OptionalsActivity extends CardListActivity {

    @Override
    protected String screenTitle() {
        return "Discipline opționale";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setIntro("Alege opționalele pe care le urmezi. După înscriere, orele lor apar în orarul tău.");
    }

    @Override
    protected void load() {
        ApiClient.get(this).optionals(callback(null));
    }

    private ApiClient.Callback<List<OptionalCourse>> callback(String confirmation) {
        return new ApiClient.Callback<List<OptionalCourse>>() {
            @Override
            public void onSuccess(List<OptionalCourse> result) {
                if (confirmation != null) {
                    Toast.makeText(OptionalsActivity.this, confirmation, Toast.LENGTH_SHORT).show();
                }
                List<Card> cards = new ArrayList<>();
                for (OptionalCourse o : result) {
                    Card c = new Card();
                    c.title = o.disciplineCode + " · " + Labels.activityType(o.type);
                    c.badge = o.enrolled ? "Înscris" : null;
                    c.subtitle = o.disciplineName + (o.credits != null ? " · " + o.credits + " credite" : "");
                    StringBuilder body = new StringBuilder(o.professorName);
                    if (o.schedule == null || o.schedule.isEmpty()) {
                        body.append("\nÎncă nu apare în orarul publicat.");
                    } else {
                        for (String s : o.schedule) {
                            body.append("\n").append(Labels.pretty(s));
                        }
                    }
                    c.body = body.toString();
                    c.actionText = o.enrolled ? "Renunță" : "Înscrie-te";
                    c.action = () -> {
                        refresh.setRefreshing(true);
                        ApiClient.get(OptionalsActivity.this).setEnrolled(o.assignmentId, !o.enrolled,
                                callback(o.enrolled ? "Te-ai retras de la " + o.disciplineCode + "."
                                        : "Te-ai înscris la " + o.disciplineCode + ". Orele apar în orarul tău."));
                    };
                    cards.add(c);
                }
                showCards(cards, "Nu există discipline opționale pentru semigrupa ta.");
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        };
    }
}
