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

package xyz.gabriwar.warpedpixeldungeon.windows;

import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.ui.Component;

import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.ChangesScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.services.updates.AvailableUpdateData;
import xyz.gabriwar.warpedpixeldungeon.services.updates.Updates;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The update prompt on the title screen. Two moments share it: a newer build exists
 * (GitHub releases, or Play reporting one for the account's track) and, on Play only,
 * the flexible download finished and the game just needs a restart to install it.
 */
public class WndUpdate extends Window {

	private static final int WIDTH_P = 120;
	private static final int WIDTH_L = 180;
	private static final int MARGIN = 4;
	private static final int BTN_HEIGHT = 18;

	public WndUpdate(){
		super();

		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;
		boolean installable = Updates.isInstallable();
		AvailableUpdateData update = Updates.updateData();

		Image icon = Icons.get(Icons.WPD);
		IconTitle title = new IconTitle(icon, Messages.get(this, installable ? "title_install" : "title"));
		title.setRect(0, 0, width, 0);
		add(title);
		//behind the icon, so the burst reads as coming from it
		new Flare(6, 24).color(SHPX_COLOR, true).show(icon, 4f).angularSpeed = 40;

		//"7.5.1 > 7.6.0": what is installed, an arrow, what is waiting
		float y = title.bottom() + MARGIN;
		String want = update != null ? update.versionName : null;
		if (want != null && want.matches("v\\d.*")) want = want.substring(1);

		RenderedTextBlock have = PixelScene.renderTextBlock(Game.version, 7);
		have.hardlight(0xBBBBBB);
		have.setPos(MARGIN, y);
		add(have);
		float rowBottom = have.bottom();
		if (want != null){
			Image arrow = Icons.get(Icons.RIGHTARROW);
			arrow.x = have.right() + 3;
			arrow.y = y + (have.height() - arrow.height()) / 2f;
			PixelScene.align(arrow);
			add(arrow);
			RenderedTextBlock next = PixelScene.renderTextBlock(want, 7);
			next.hardlight(TITLE_COLOR);
			next.setPos(arrow.x + arrow.width() + 3, y);
			add(next);
			rowBottom = Math.max(rowBottom, next.bottom());
		}
		y = rowBottom + MARGIN;

		//the notes sit on a toast panel like the changes screen, and scroll when a
		//release wrote more than fits on a phone
		String notes;
		if (installable){
			notes = Messages.get(this, "notes_install");
		} else if (update != null && update.desc != null && !update.desc.trim().isEmpty()){
			notes = prettify(update.desc);
		} else {
			notes = Messages.get(this, "notes");
		}
		NinePatch panel = Chrome.get(Chrome.Type.TOAST);
		add(panel);

		Component content = new Component();
		RenderedTextBlock body = PixelScene.renderTextBlock(notes, 6);
		body.maxWidth(width - 2*MARGIN - panel.marginHor() - 2);
		body.setPos(panel.marginLeft() + 1, panel.marginTop() + 1);
		content.add(body);
		content.setSize(width - 2*MARGIN, body.bottom() + panel.marginBottom() + 1);

		float maxNotes = PixelScene.landscape() ? 70 : 90;
		float notesHeight = Math.min(maxNotes, content.height());
		panel.size(width - 2*MARGIN, notesHeight);
		panel.x = MARGIN;
		panel.y = y;
		ScrollPane pane = new ScrollPane(content);
		add(pane);
		y += notesHeight + MARGIN;

		RedButton go = new RedButton(Messages.get(this, installable ? "install" : "update")){
			@Override
			protected void onClick() {
				super.onClick();
				hide();
				if (Updates.isInstallable()){
					Updates.launchInstall();
				} else if (Updates.updateAvailable()){
					Updates.launchUpdate(Updates.updateData());
				}
			}
		};
		go.icon(Icons.get(Icons.CHANGES));
		go.textColor(TITLE_COLOR);
		go.setRect(MARGIN, y, width - 2*MARGIN, BTN_HEIGHT);
		add(go);

		float half = (width - 2*MARGIN - 2) / 2f;
		RedButton later = new RedButton(Messages.get(this, "later")){
			@Override
			protected void onClick() {
				super.onClick();
				hide();
			}
		};
		later.setRect(MARGIN, go.bottom() + 2, half, BTN_HEIGHT);
		add(later);

		RedButton changes = new RedButton(Messages.get(this, "changes")){
			@Override
			protected void onClick() {
				super.onClick();
				hide();
				ChangesScene.changesSelected = 0;
				WarpedPixelDungeon.switchNoFade(ChangesScene.class);
			}
		};
		changes.setRect(later.right() + 2, later.top(), half, BTN_HEIGHT);
		add(changes);

		resize(width, (int)changes.bottom() + MARGIN);
		//a scroll pane places its own camera from the window's, so it is laid out only
		//once resize() has put the window where it will stay
		pane.setRect(panel.x, panel.y, panel.width(), panel.height());
	}

	private static final Pattern COMMIT_LINE = Pattern.compile("^[-*]\\s*(\\w+)(?:\\([^)]*\\))?!?:\\s*(.+)$");

	/**
	 * Release bodies come from CI as conventional commit lines. Players get the kind of
	 * change as a highlighted label and the message after it; any other line stays as
	 * it was written.
	 */
	private static String prettify( String desc ){
		StringBuilder out = new StringBuilder();
		for (String line : desc.split("\\r?\\n")){
			Matcher m = COMMIT_LINE.matcher(line.trim());
			if (m.matches()){
				String label;
				switch (m.group(1).toLowerCase()){
					case "feat":    label = "New";      break;
					case "fix":     label = "Fixed";    break;
					case "balance": label = "Balance";  break;
					case "perf":    label = "Faster";   break;
					case "art":     label = "Art";      break;
					case "ui":      label = "UI";       break;
					case "polish":  label = "Polish";   break;
					case "revert":  label = "Reverted"; break;
					default:        label = m.group(1);
				}
				String msg = m.group(2).trim();
				if (!msg.isEmpty()) msg = Character.toUpperCase(msg.charAt(0)) + msg.substring(1);
				line = "_" + label + "_ " + msg;
			}
			if (out.length() > 0) out.append("\n");
			out.append(line);
		}
		return out.toString();
	}
}
