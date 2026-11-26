package ro.utcn.scd.mobile.ui.common;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Aplicatia se deseneaza pe tot ecranul (edge-to-edge, obligatoriu pe Android 15+),
 * asa ca adaugam spatiu pentru bara de stare, bara de navigare si tastatura.
 */
public final class InsetsHelper {

    private InsetsHelper() {
    }

    /** Spatiu pe toate laturile: bare de sistem + tastatura (pentru ecranele simple, ex. login). */
    public static void padForSystemBarsAndKeyboard(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
