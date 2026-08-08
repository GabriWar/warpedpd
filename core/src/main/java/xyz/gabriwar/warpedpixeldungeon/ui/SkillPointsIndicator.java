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

package xyz.gabriwar.warpedpixeldungeon.ui;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.windows.WndHero;
import xyz.gabriwar.warpedpixeldungeon.windows.WndQuickSkills;
import com.watabou.noosa.Image;

/**
 * The quick skill tag. It shows whichever skills the hero can actually use
 * right now - toggles and casts - and opens them for one-tap use. When there
 * are unspent points it says so too, and a long press goes to the tree.
 */
public class SkillPointsIndicator extends Tag {

	private Image icon;

	public SkillPointsIndicator() {
		super( 0x4a8ad8 );
		setSize( SIZE, SIZE );
		visible = false;
	}

	@Override
	protected void createChildren() {
		super.createChildren();
		icon = Icons.get( Icons.TALENT );
		add( icon );
	}

	@Override
	protected void layout() {
		super.layout();
		if (!flipped)   icon.x = x + (SIZE - icon.width()) / 2f + 1;
		else            icon.x = x + width - (SIZE + icon.width()) / 2f - 1;
		icon.y = y + (height - icon.height()) / 2f;
		PixelScene.align( icon );
	}

	@Override
	public void update() {
		visible = Dungeon.hero != null && Dungeon.hero.isAlive()
				&& (Skill.availableSkill > 0
					|| !Dungeon.hero.heroSkills.usableNow( Dungeon.hero ).isEmpty());
		super.update();
	}

	@Override
	protected void onClick() {
		super.onClick();
		GameScene.show( new WndQuickSkills() );
	}

	@Override
	protected boolean onLongClick() {
		//the full tree, for spending points
		WndHero.lastIdx = 1;
		GameScene.show( new WndHero() );
		return true;
	}

	@Override
	protected String hoverText() {
		return Skill.availableSkill > 0
				? "Skills (" + Skill.availableSkill + " points to spend)"
				: "Skills";
	}
}
