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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.SkillSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Image;
import xyz.gabriwar.warpedpixeldungeon.ui.Button;

import java.util.List;

/**
 * The quick panel behind the HUD tag: every skill you can actually use this
 * turn, and nothing else. Tap one to toggle it or to start its cast - no trip
 * through the tree.
 */
public class WndQuickSkills extends Window {

	private static final int SLOT   = 20;
	private static final int MARGIN = 2;
	private static final int COLS   = 4;
	private static final int TITLE_H = 14;

	public WndQuickSkills(){

		super();

		Hero hero = Dungeon.hero;
		List<Skill> usable = hero.heroSkills.usableNow( hero );

		int width = COLS * SLOT + (COLS - 1) * MARGIN;

		RenderedTextBlock title = PixelScene.renderTextBlock( "Skills", 9 );
		title.hardlight( TITLE_COLOR );
		title.setPos( (width - title.width()) / 2f, (TITLE_H - title.height()) / 2f );
		add( title );

		if (usable.isEmpty()){
			RenderedTextBlock none = PixelScene.renderTextBlock(
					"You have not learned a skill you can use yet.", 6 );
			none.maxWidth( width );
			none.setPos( 0, TITLE_H );
			add( none );
			resize( width, (int)(none.bottom() + 2) );
			return;
		}

		int rows = (usable.size() + COLS - 1) / COLS;
		for (int i = 0; i < usable.size(); i++){
			SkillButton btn = new SkillButton( usable.get(i) );
			btn.setRect( (i % COLS) * (SLOT + MARGIN),
					TITLE_H + (i / COLS) * (SLOT + MARGIN), SLOT, SLOT );
			add( btn );
		}

		resize( width, TITLE_H + rows * SLOT + (rows - 1) * MARGIN );
	}

	private class SkillButton extends Button {

		private Skill skill;

		private ColorBlock bg;
		private Image icon;
		private BitmapText cost;

		public SkillButton( Skill skill ){
			super();
			this.skill = skill;

			icon = new SkillSprite( skill.image() );
			add( icon );

			boolean ready = !skill.actions( Dungeon.hero ).isEmpty();
			icon.alpha( ready ? 1f : 0.35f );

			if (skill.active){
				bg.hardlight( 0x1d3a1d );
			} else if (!ready){
				bg.hardlight( 0x2a1414 );
			}

			if (skill.getManaCost() > 0){
				cost.text( Integer.toString( skill.getManaCost() ) );
				cost.hardlight( ready ? 0x8ac0ff : 0xcc6666 );
				cost.measure();
			}
		}

		@Override
		protected void createChildren(){
			super.createChildren();

			bg = new ColorBlock( 1, 1, 0xFF1f1f1f );
			add( bg );

			cost = new BitmapText( PixelScene.pixelFont );
			add( cost );
		}

		@Override
		protected void layout(){
			super.layout();

			bg.size( width, height );
			bg.x = x;
			bg.y = y;

			if (icon != null){
				icon.x = x + (width - icon.width()) / 2;
				icon.y = y + (height - icon.height()) / 2;
				PixelScene.align( icon );
			}

			cost.x = x + width - cost.width() - 1;
			cost.y = y + height - cost.baseLine() - 1;
			PixelScene.align( cost );
		}

		@Override
		protected void onClick(){

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
				hide();
				GameScene.show( new WndQuickSkills() );
			}
		}

		@Override
		protected boolean onLongClick(){
			GameScene.show( new WndSkill( WndQuickSkills.this, skill ) );
			return true;
		}
	}
}
