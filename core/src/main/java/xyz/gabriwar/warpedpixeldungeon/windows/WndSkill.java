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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.SkillSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;

public class WndSkill extends Window {

	private static final float BUTTON_HEIGHT = 16;
	private static final float GAP = 2;

	private static final int WIDTH_MIN = 120;
	private static final int WIDTH_MAX = 220;

	public WndSkill( final Window host, final Skill skill ){
		this( host, skill, false );
	}

	public WndSkill( final Window host, final Skill skill, final boolean upgradable ){

		super();

		int width = WIDTH_MIN;

		//title: icon + name + level
		IconTitle titlebar = new IconTitle();
		titlebar.icon( new SkillSprite( skill.image() ) );
		titlebar.label( skill.name() + " (lvl " + skill.level + ")" );
		titlebar.setRect( 0, 0, width, 0 );
		add( titlebar );

		RenderedTextBlock info = PixelScene.renderTextBlock( skill.info(), 6 );
		info.maxWidth( width );
		info.setPos( titlebar.left(), titlebar.bottom() + GAP );
		add( info );

		float y = info.bottom() + GAP;

		if (upgradable){
			RedButton btnUp = new RedButton( "Upgrade (" + skill.upgradeCost() + (skill.upgradeCost() == 1 ? " point)" : " points)") ) {
				@Override
				protected void onClick() {
					hide();
					skill.requestUpgrade();
					//keep the flow going: reopen with fresh numbers
					GameScene.show( new WndSkill( host, skill,
							skill.level < Skill.MAX_LEVEL
									&& Skill.availableSkill >= skill.upgradeCost() ) );
				}
			};
			btnUp.setRect( 0, y + GAP, width, BUTTON_HEIGHT );
			add( btnUp );
			y = btnUp.bottom();
		}

		for (final String action : skill.actions( Dungeon.hero )){
			RedButton btn = new RedButton( action ) {
				@Override
				protected void onClick() {
					hide();
					if (action.equals(Skill.AC_CAST) || action.equals(Skill.AC_SUMMON)){
						//game actions need the board - close the hosting window too
						if (host != null) host.hide();
						skill.execute( Dungeon.hero, action );
					} else {
						skill.execute( Dungeon.hero, action );
						//advancing keeps the flow going: reopen fresh numbers
						if (action.equals(Skill.AC_ADVANCE)){
							GameScene.show( new WndSkill( host, skill ) );
						}
					}
				}
			};
			btn.setRect( 0, y + GAP, width, BUTTON_HEIGHT );
			add( btn );
			y = btn.bottom();
		}

		resize( width, (int)(y + GAP) );
	}
}
