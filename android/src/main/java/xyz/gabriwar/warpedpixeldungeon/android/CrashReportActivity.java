/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package xyz.gabriwar.warpedpixeldungeon.android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class CrashReportActivity extends Activity {

    public static final String EXTRA_CRASH_LOG = "crash_log";

    private static final String REPORT_EMAIL = "reportwpd@gabriwar.xyz";
    private static final String REPORT_SUBJECT = "Warped Pixel Dungeon Crash Report";

    // Warped PD in-game palette (see ui/Window.java)
    private static final int TITLE_COLOR = 0xFFFFFF44; // yellow, TITLE_COLOR
    private static final int ACCENT_COLOR = 0xFF33BB33; // green, SHPX_COLOR
    private static final int WINDOW_BG    = 0xFF000000; // game renders over black
    private static final int PANEL_BG     = 0xFF12121A; // chrome-ish dark panel
    private static final int CHROME_LINE  = 0xFF454A54; // window border grey
    private static final int TEXT_LIGHT   = 0xFFCCCCCC;
    private static final int TEXT_DIM      = 0xFF888888;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String crashLog = getIntent().getStringExtra(EXTRA_CRASH_LOG);
        if (crashLog == null) crashLog = "No crash log available.";

        // Build layout programmatically (no XML resources needed)
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(WINDOW_BG);
        root.setPadding(32, 48, 32, 32);

        TextView title = new TextView(this);
        title.setText("The game crashed :(");
        title.setTextColor(TITLE_COLOR);
        title.setTextSize(22f);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        title.setPadding(0, 0, 0, 16);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Warped ran into an error it couldn't recover from. " +
                "Send the report to help fix it — add a note below if you know what you were doing.");
        subtitle.setTextColor(TEXT_LIGHT);
        subtitle.setTextSize(14f);
        subtitle.setTypeface(Typeface.MONOSPACE);
        subtitle.setPadding(0, 0, 0, 20);
        root.addView(subtitle);

        // User comment field, prepended to the emailed report
        final EditText comment = new EditText(this);
        comment.setHint("Add a comment for the developer (optional)...");
        comment.setHintTextColor(TEXT_DIM);
        comment.setTextColor(Color.WHITE);
        comment.setTextSize(13f);
        comment.setTypeface(Typeface.MONOSPACE);
        comment.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        comment.setMinLines(2);
        comment.setGravity(Gravity.TOP | Gravity.START);
        comment.setPadding(20, 16, 20, 16);
        comment.setBackground(panel(PANEL_BG, CHROME_LINE));
        LinearLayout.LayoutParams commentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        commentParams.bottomMargin = 20;
        comment.setLayoutParams(commentParams);
        root.addView(comment);

        Button sendButton = new Button(this);
        sendButton.setText("Send Crash Report via Email");
        sendButton.setAllCaps(false);
        sendButton.setTextColor(Color.BLACK);
        sendButton.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        sendButton.setBackground(panel(ACCENT_COLOR, CHROME_LINE));
        sendButton.setPadding(32, 16, 32, 16);

        final String finalCrashLog = crashLog;
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String note = comment.getText().toString().trim();
                String body = note.isEmpty()
                        ? finalCrashLog
                        : "Player comment:\n" + note + "\n\n----- crash log -----\n\n" + finalCrashLog;
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:"));
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{REPORT_EMAIL});
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, REPORT_SUBJECT);
                emailIntent.putExtra(Intent.EXTRA_TEXT, body);
                try {
                    startActivity(Intent.createChooser(emailIntent, "Send crash report..."));
                } catch (android.content.ActivityNotFoundException e) {
                    // no email app available
                }
            }
        });
        root.addView(sendButton);

        Button closeButton = new Button(this);
        closeButton.setText("Close");
        closeButton.setAllCaps(false);
        closeButton.setTextColor(TEXT_LIGHT);
        closeButton.setTypeface(Typeface.MONOSPACE);
        closeButton.setBackground(panel(PANEL_BG, CHROME_LINE));
        closeButton.setPadding(32, 16, 32, 16);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        closeParams.topMargin = 12;
        closeButton.setLayoutParams(closeParams);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        root.addView(closeButton);

        TextView logLabel = new TextView(this);
        logLabel.setText("Crash log:");
        logLabel.setTextColor(TEXT_DIM);
        logLabel.setTextSize(12f);
        logLabel.setTypeface(Typeface.MONOSPACE);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = 28;
        logLabel.setLayoutParams(labelParams);
        root.addView(logLabel);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f);
        scrollParams.topMargin = 8;
        scrollView.setLayoutParams(scrollParams);

        TextView logView = new TextView(this);
        logView.setText(crashLog);
        logView.setTextColor(ACCENT_COLOR);
        logView.setTextSize(10f);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setPadding(16, 16, 16, 16);
        logView.setBackground(panel(0xFF07070D, CHROME_LINE));
        logView.setTextIsSelectable(true);
        scrollView.addView(logView);
        root.addView(scrollView);

        setContentView(root);
    }

    // Flat pixel-window-style panel: solid fill with a thin chrome border.
    private static GradientDrawable panel(int fill, int border) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setStroke(2, border);
        return d;
    }
}
