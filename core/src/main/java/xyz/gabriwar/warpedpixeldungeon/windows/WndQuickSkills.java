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
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
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

import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import com.watabou.noosa.Game;
import com.watabou.glwrap.Blending;
import xyz.gabriwar.warpedpixeldungeon.ui.SkillTreeArt;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.SkillSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;
import xyz.gabriwar.warpedpixeldungeon.ui.Button;

import java.util.List;

/**
 * The quick panel behind the HUD tag: every skill you can actually use this
 * turn, and nothing else. Tap one to toggle it or to start its cast - no trip
 * through the tree.
 */
public class WndQuickSkills extends Window {

	private static final int SLOT    = SkillTreeArt.SOCKET;   //20
	private static final int PIPS    = 3;
	private static final int CELL_W  = SLOT + 4;
	private static final int CELL_H  = SLOT + PIPS + 4;
	private static final int COLS    = 4;
	private static final int TITLE_H = 14;
	private static final int CLEAR_H = 18;

	private final java.util.function.Consumer<Skill> selection;
	//only when the skills are more than the screen holds (every class's, from the debug window)
	private ScrollPane pane;

	public WndQuickSkills(){
		this(null);
	}

	public WndQuickSkills(java.util.function.Consumer<Skill> selection){
		super();
		this.selection = selection;

		Hero hero = Dungeon.hero;
		List<Skill> usable = hero.heroSkills.usableNow( hero );

		int width = COLS * CELL_W;

		//the same strip as the tree: icon, title, and the points waiting if any
		ColorBlock strip = new ColorBlock( width, TITLE_H, 0xFF000000 );
		strip.alpha( 0.35f );
		add( strip );
		Image icon = Icons.get( Icons.TALENT );
		icon.x = 2;
		icon.y = (TITLE_H - icon.height()) / 2f;
		PixelScene.align( icon );
		add( icon );
		RenderedTextBlock title = PixelScene.renderTextBlock( Messages.get( this, selection == null ? "title" : "assign" ), 8 );
		title.hardlight( TITLE_COLOR );
		title.setPos( icon.x + icon.width() + 3, (TITLE_H - title.height()) / 2f );
		PixelScene.align( title );
		add( title );
		if (selection == null && Dungeon.hero.heroSkills.availableSkill > 0){
			RenderedTextBlock pts = PixelScene.renderTextBlock( Messages.get( this, "points", Dungeon.hero.heroSkills.availableSkill ), 6 );
			pts.hardlight( 0x8ce08c );
			pts.setPos( width - pts.width() - 2, (TITLE_H - pts.height()) / 2f );
			PixelScene.align( pts );
			add( pts );
		}
		ColorBlock line = new ColorBlock( width, 1, 0xFF3a3a44 );
		line.y = TITLE_H;
		add( line );

		if (usable.isEmpty()){
			RenderedTextBlock none = PixelScene.renderTextBlock( Messages.get( this, "none" ), 6 );
			none.maxWidth( width - 4 );
			none.setPos( 2, TITLE_H + 3 );
			add( none );
			resize( width, (int)(none.bottom() + 3) );
			addClearButton(width);
			return;
		}

		int rows = (usable.size() + COLS - 1) / COLS;
		int room = (int)PixelScene.uiCamera.height - chrome.marginVer() - 20 - TITLE_H - 3
				- (selection == null ? 0 : CLEAR_H + 2);
		if (rows * CELL_H <= room){
			for (int i = 0; i < usable.size(); i++){
				SkillButton btn = new SkillButton( usable.get(i) );
				btn.setRect( (i % COLS) * CELL_W + 2, TITLE_H + 3 + (i / COLS) * CELL_H, SLOT, SLOT );
				add( btn );
			}
			resize( width, TITLE_H + 3 + rows * CELL_H );
		} else {
			//the pane before its buttons: pointer listeners fire newest first, so a pane made
			//after the buttons on it would take their presses
			pane = new ScrollPane( new Component() );
			for (int i = 0; i < usable.size(); i++){
				SkillButton btn = new SkillButton( usable.get(i) );
				btn.setRect( (i % COLS) * CELL_W + 2, 2 + (i / COLS) * CELL_H, SLOT, SLOT );
				pane.content().add( btn );
			}
			pane.content().setSize( width, 2 + rows * CELL_H );
			//resize() before the pane goes in: its camera is placed from the window's
			resize( width, TITLE_H + 3 + room );
			add( pane );
			pane.setRect( 0, TITLE_H + 1, width, room + 2 );
			pane.dragOverButtons();
		}
		addClearButton(width);
	}

	private void addClearButton(int width){
		if (selection == null) return;
		xyz.gabriwar.warpedpixeldungeon.ui.RedButton clear = new xyz.gabriwar.warpedpixeldungeon.ui.RedButton(Messages.get(this, "clear")){
			@Override protected void onClick(){
				hide();
				selection.accept(null);
			}
		};
		clear.setRect(0, height + 2, width, CLEAR_H);
		add(clear);
		resize(width, (int)clear.bottom());
	}

	/** a glow is added to what is under it, so it reads as light rather than paint */
	private static class Glow extends Image {
		Glow( int[] frame ){
			super( SkillTreeArt.get() );
			SkillTreeArt.frame( this, frame );
		}
		@Override
		public void draw() {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}

	/**
	 * A socket off the tree, in the state the skill is in right now: the rim is the
	 * metal of its points, the fill runs green while a toggle is on, a green ring
	 * breathes round it while it runs, and the whole thing goes dark when it cannot be
	 * used this turn. Pips under it count the points; the mana price sits top-left.
	 */
	private class SkillButton extends Button {

		private Skill skill;
		private Glow ring;
		private Image socket;
		private Image icon;
		private Image pips;
		private Image plate;
		private BitmapText cost;

		public SkillButton( Skill skill ){
			super();
			this.skill = skill;

			icon = skill.quickslotIcon();
			add( icon );
			bringToFront( plate );
			bringToFront( cost );

			boolean ready = !skill.actions( Dungeon.hero ).isEmpty();
			int level = Math.max( 1, Math.min( Skill.MAX_LEVEL, skill.level ) );
			int metal = level >= Skill.MAX_LEVEL ? 2 : level - 1;

			int frame;
			if (!ready && !skill.active){
				frame = SkillTreeArt.LOCKED;
			} else {
				frame = (skill.active ? SkillTreeArt.L1A : SkillTreeArt.L1) + metal;
			}
			SkillTreeArt.frame( socket, SkillTreeArt.socket( frame ) );
			SkillTreeArt.frame( pips, SkillTreeArt.pips( Skill.MAX_LEVEL, skill.level ) );
			pips.visible = !(skill instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CrownSkill);

			icon.alpha( ready || skill.active ? 1f : 0.35f );
			//same rule as the tree: a switchable skill keeps its colour only while it runs
			if (skill.toggleable() && icon instanceof SkillSprite){
				((SkillSprite) icon).grey( !skill.active );
			}

			ring.visible = skill.active;

			if (!skill.quickslotStatus().isEmpty()){
				cost.text( skill.quickslotStatus() );
				cost.hardlight( ready ? 0x8ac0ff : 0xdd8877 );
				cost.measure();
				plate.visible = cost.visible = true;
			} else {
				plate.visible = cost.visible = false;
			}
		}

		@Override
		protected void createChildren(){
			super.createChildren();
			ring = new Glow( SkillTreeArt.GLOW );
			ring.hardlight( 0x66ff66 );
			add( ring );
			socket = new Image( SkillTreeArt.get() );
			add( socket );
			pips = new Image( SkillTreeArt.get() );
			add( pips );
			plate = new Image( SkillTreeArt.get() );
			SkillTreeArt.frame( plate, SkillTreeArt.PLATE );
			add( plate );
			cost = new BitmapText( PixelScene.pixelFont );
			add( cost );
		}

		@Override
		public synchronized void update() {
			super.update();
			if (ring.visible){
				ring.alpha( 0.5f + 0.5f * (float)Math.sin( Game.timeTotal * 4 ) );
			}
		}

		@Override
		protected void layout(){
			super.layout();
			socket.x = x;
			socket.y = y;
			ring.x = x - 3;
			ring.y = y - 3;
			if (icon != null){
				icon.x = x + (width - icon.width()) / 2;
				icon.y = y + (height - icon.height()) / 2;
				PixelScene.align( icon );
			}
			pips.x = x + (width - pips.width()) / 2;
			pips.y = y + height + 1;
			PixelScene.align( pips );
			plate.x = x - 1;
			plate.y = y - 1;
			cost.x = plate.x + (plate.width() - cost.width()) / 2;
			cost.y = plate.y + 1;
			PixelScene.align( cost );
		}

		@Override
		protected String hoverText() {
			return Messages.titleCase( skill.name() ) + (skill instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CrownSkill ? "" : " " + skill.level + "/" + Skill.MAX_LEVEL);
		}

		@Override
		protected void onClick(){
			if (selection != null){
				hide();
				selection.accept(skill);
				return;
			}
			java.util.ArrayList<String> actions = skill.actions( Dungeon.hero );
			if (actions.isEmpty()){
				//not enough mana, or nothing to do with it right now
				GameScene.show( new WndSkill( WndQuickSkills.this, skill ) );
				return;
			}
			String action = actions.get( 0 );
			boolean leavesTheWindow = action.equals( Skill.AC_CAST )
					|| action.equals( Skill.AC_SUMMON );
			if (leavesTheWindow){
				hide();
				skill.execute( Dungeon.hero, action );
			} else {
				//a toggle: flip it and stay put so several can be set in a row
				skill.execute( Dungeon.hero, action );
				//a list long enough to scroll comes back scrolled where it was
				float scrolled = pane == null ? 0 : pane.content().camera.scroll.y;
				hide();
				WndQuickSkills again = new WndQuickSkills();
				if (again.pane != null) again.pane.scrollTo( 0, scrolled );
				GameScene.show( again );
			}
		}

		@Override
		protected boolean onLongClick(){
			GameScene.show( new WndSkill( WndQuickSkills.this, skill ) );
			return true;
		}
	}
}
