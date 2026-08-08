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

package xyz.gabriwar.warpedpixeldungeon.ui;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.Image;

/**
 * Shown while a map waypoint exists but the march is paused (the player
 * tapped). Tapping it puts the hero back on the road.
 */
public class WaypointIndicator extends Tag {

	private Image icon;

	public WaypointIndicator() {
		super( 0x4a6fa8 );
		setSize( SIZE, SIZE );
		visible = false;
	}

	@Override
	protected void createChildren() {
		super.createChildren();
		icon = new ItemSprite( ItemSpriteSheet.WORLD_MAP, null );
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
		visible = Dungeon.level instanceof OverworldLevel
				&& ((OverworldLevel) Dungeon.level).waypointActive
				&& !((OverworldLevel) Dungeon.level).waypointMarching;
		super.update();
	}

	@Override
	protected void onClick() {
		super.onClick();
		if (Dungeon.level instanceof OverworldLevel && Dungeon.hero.ready){
			((OverworldLevel) Dungeon.level).resumeMarch();
		}
	}

	@Override
	protected String hoverText() {
		return Messages.get( this, "hover" );
	}
}
