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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.debug.SkillDebug;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.SkillSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.HeroIcon;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * The debug window's skill picker: a tab per class, its skills laid out column by column as in its
 * tree, then each of its callings. A skill the hero has learned is ticked and green, with its level;
 * tapping another class's skill teaches it at level 1 (no points spent), tapping it again takes it
 * away. His own skills are greyed out and cannot be pressed.
 */
public class WndDebugSkills extends WndTabbed {

	//the columns in the order the tree shows them, left to right
	private static final CurrentSkills.BRANCHES[] COLUMNS = { CurrentSkills.BRANCHES.PASSIVEA,
			CurrentSkills.BRANCHES.ACTIVE, CurrentSkills.BRANCHES.PASSIVEB, CurrentSkills.BRANCHES.FOURTH };

	//the window comes back on the class it was left on (none yet: the first class not the hero's)
	private static int lastTab = -1;

	private final ArrayList<Page> pages = new ArrayList<>();
	private final RenderedTextBlock title;

	public WndDebugSkills() {
		super();

		Hero hero = Dungeon.hero;
		int width = WndDebug.width( chrome.marginHor() );

		title = PixelScene.renderTextBlock( 9 );
		title.hardlight( TITLE_COLOR );
		add( title );
		float top = title.height() + WndDebug.GAP * 2;

		float tallest = 0;
		for (HeroClass cls : HeroClass.values()){
			Page p = new Page( hero, cls, width );
			pages.add( p );
			tallest = Math.max( tallest, p.contentHeight );
		}

		int maxH = (int)(PixelScene.uiCamera.height - chrome.marginVer() - 20);
		float paneH = Math.max( WndDebug.BTN_HEIGHT, Math.min( tallest, maxH - top ) );

		//resize() BEFORE the panes go in: their cameras are placed from the window's
		resize( width, (int)Math.ceil( top + paneH ) );

		for (int i = 0; i < pages.size(); i++){
			final Page p = pages.get( i );
			final int index = i;
			add( p.pane );
			p.pane.setRect( 0, top, width, paneH );
			add( new IconTab( Icons.get( p.cls ) ){
				@Override
				protected void select( boolean value ){
					super.select( value );
					p.pane.visible = p.pane.active = value;
					if (!value) p.pane.releaseKeys();
					if (value){
						lastTab = index;
						p.sync();
						String cls = Messages.titleCase( p.cls.title() );
						title.text( "Skills: " + cls );
						if (title.width() > WndDebugSkills.this.width) title.text( cls );
						title.setPos( (WndDebugSkills.this.width - title.width()) / 2f, 0 );
						PixelScene.align( title );
					}
				}
			} );
		}

		layoutTabs();
		int first = lastTab;
		if (first < 0 || first >= pages.size()){
			first = hero != null && hero.heroClass == HeroClass.values()[0] ? 1 : 0;
		}
		select( first );
	}

	/** one class: a note, then a section per column and per calling, a button per skill */
	private class Page {

		final HeroClass cls;
		final ScrollPane pane;
		final ArrayList<Component> rows = new ArrayList<>();
		final ArrayList<SkillButton> buttons = new ArrayList<>();
		float contentHeight;

		Page( Hero hero, HeroClass cls, int width ) {
			this.cls = cls;
			//the pane before its buttons: pointer listeners fire newest first, so a pane made
			//after the buttons on it would take their presses
			pane = new ScrollPane( new Component() );

			RenderedTextBlock note = PixelScene.renderTextBlock(
					"Tap a skill to learn it at level 1 (no points spent); tap it again to forget it. Ticked: learned. "
							+ "Greyed out: the hero's own skills, which stay.", 5 );
			note.hardlight( 0xBBBBBB );
			note.maxWidth( width );
			rows.add( note );

			//each column under the emblem and the name it has in that class's tree
			CurrentSkills tree = new CurrentSkills( cls );
			Skill[] emblems = { tree.branchPA, tree.branchA, tree.branchPB, tree.branchD };
			for (int c = 0; c < COLUMNS.length; c++){
				section( hero, emblems[c].name(), new SkillSprite( emblems[c].image() ), COLUMNS[c], HeroSubClass.NONE );
			}
			for (HeroSubClass sub : cls.subClasses()){
				section( hero, sub.title(), new HeroIcon( sub ), CurrentSkills.BRANCHES.SUBCLASS, sub );
			}

			for (Component row : rows) pane.content().add( row );
			layout( width );
			//a page is all buttons: dragged from any of them, it scrolls
			pane.dragOverButtons();
		}

		private void section( Hero hero, String heading, Image icon, CurrentSkills.BRANCHES branch, HeroSubClass sub ) {
			rows.add( new WndDebug.Header( Messages.titleCase( heading ), icon ) );
			for (CurrentSkills.Origin o : CurrentSkills.catalog()){
				if (o.heroClass != cls || o.branch != branch || o.subClass != sub) continue;
				SkillButton b = new SkillButton( hero, o.cls );
				buttons.add( b );
				rows.add( b );
			}
		}

		//full width in portrait; at the debug window's wide size the skills sit two to a row
		private void layout( int width ) {
			boolean wide = width >= WndDebug.WIDE;
			float half = (width - WndDebug.GAP) / 2f;
			float pos = 0;
			boolean left = true;
			for (Component row : rows){
				if (row instanceof SkillButton){
					if (wide){
						row.setRect( left ? 0 : half + WndDebug.GAP, pos, half, WndDebug.BTN_HEIGHT );
						if (!left) pos += WndDebug.BTN_HEIGHT + WndDebug.GAP;
						left = !left;
					} else {
						row.setRect( 0, pos, width, WndDebug.BTN_HEIGHT );
						pos += WndDebug.BTN_HEIGHT + WndDebug.GAP;
					}
					continue;
				}
				//a section starts on a fresh row, with a little air above it
				if (!left){
					pos += WndDebug.BTN_HEIGHT + WndDebug.GAP;
					left = true;
				}
				if (pos > 0) pos += WndDebug.GAP * 2;
				if (row instanceof RenderedTextBlock){
					((RenderedTextBlock) row).setPos( 0, pos );
					PixelScene.align( (RenderedTextBlock) row );
				} else {
					row.setRect( 0, pos, width, 0 );
				}
				pos += row.height() + WndDebug.GAP;
			}
			if (!left) pos += WndDebug.BTN_HEIGHT + WndDebug.GAP;
			contentHeight = Math.max( 0, pos - WndDebug.GAP );
			pane.content().setSize( width, contentHeight );
		}

		void sync() {
			for (SkillButton b : buttons) b.sync();
		}
	}

	private void syncAll() {
		for (Page p : pages) p.sync();
	}

	/** a skill: ticked and green once the hero has learned it; his own are greyed out, they cannot go */
	private class SkillButton extends WndDebug.DebugButton {

		private final Hero hero;
		private final Class<? extends Skill> skill;
		private final String name;

		SkillButton( Hero hero, Class<? extends Skill> skill ) {
			this( hero, skill, Reflection.newInstance( skill ) );
		}

		private SkillButton( Hero hero, Class<? extends Skill> skill, Skill sample ) {
			super( Messages.titleCase( sample.name() ), new SkillSprite( sample.image() ), true );
			this.hero = hero;
			this.skill = skill;
			this.name = Messages.titleCase( sample.name() );
		}

		@Override
		protected void onClick() {
			if (hero == null || hero != Dungeon.hero) return;
			if (SkillDebug.revoke( hero, skill )){
				GLog.i( "Forgot " + name + "." );
			} else if (SkillDebug.grant( hero, skill )){
				GLog.p( "Learned " + name + " (level 1)." );
			}
			syncAll();
		}

		void sync() {
			Skill held = hero == null ? null : hero.heroSkills.exact( skill );
			boolean own = held != null && !hero.heroSkills.isForeign( held );
			//the hero's whole tree is "held", learned or not: only a level makes it learned
			boolean learned = held != null && held.level > 0;
			on( learned );
			label( learned ? name + " (lv " + held.level + ")" : name );
			if (active == own) enable( !own );
		}
	}
}
