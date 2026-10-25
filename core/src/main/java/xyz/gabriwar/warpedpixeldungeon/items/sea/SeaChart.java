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

package xyz.gabriwar.warpedpixeldungeon.items.sea;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.SeaWorld;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sea.Seafaring;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * A lighthouse keeper's chart of the waters round his light (actors/mobs/npcs/LighthouseKeeper):
 * read under the open sky, it is copied onto the hero's world map - every island within REACH of the
 * light it was drawn at goes on it, with the wreck or the lighthouse that stands on it (levels/
 * overworld/SeaWorld.chart) - and is used up.
 */
public class SeaChart extends Item {

	public static final String AC_READ = "READ";
	/** How far round its light a chart shows the isles, cells. */
	public static final int REACH = 400;

	//the light it was drawn at, MIN_VALUE when unknown (it charts round its reader then)
	public int originX = Integer.MIN_VALUE, originY = Integer.MIN_VALUE;
	//the light's own name (SeaSites.lightName), for its description
	public String lightName = "";

	{
		image = ItemSpriteSheet.SEA_CHART;
		stackable = false;
		defaultAction = AC_READ;
	}

	/** A chart of the waters round a light. */
	public static SeaChart of( int x, int y, String lightName ){
		SeaChart c = new SeaChart();
		c.originX = x;
		c.originY = y;
		c.lightName = lightName;
		return c;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_READ );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute( hero, action );
		if (!action.equals( AC_READ )) return;
		OverworldLevel ow = Seafaring.surface( Dungeon.level );
		if (ow == null || ow.isMirror()){
			NetManager.heroLog( hero, GLog.WARNING + Messages.get( this, "no_surface" ) );
			return;
		}
		int x = originX, y = originY;
		if (x == Integer.MIN_VALUE){
			x = ow.worldX + hero.pos % ow.width();
			y = ow.worldY + hero.pos / ow.width();
		}
		int marked = ow.sea().chart( x, y, REACH );
		detach( hero.belongings.backpack );
		hero.spend( 1f );
		hero.busy();
		if (hero.sprite != null) hero.sprite.operate( hero.pos );
		SpatialSound.play( Assets.Sounds.READ, hero );
		NetManager.heroLog( hero, marked > 0 ? GLog.POSITIVE + Messages.get( this, "charted", marked )
				: Messages.get( this, "nothing_new" ) );
	}

	@Override
	public String desc(){
		return lightName.isEmpty() ? Messages.get( this, "desc_plain" )
				: Messages.get( this, "desc", Messages.get( SeaWorld.class, "light_name", lightName ) );
	}

	@Override
	public boolean isUpgradable(){
		return false;
	}

	@Override
	public boolean isIdentified(){
		return true;
	}

	@Override
	public int value(){
		return 50;
	}

	private static final String ORIGIN_X = "origin_x";
	private static final String ORIGIN_Y = "origin_y";
	private static final String LIGHT = "light_name";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( ORIGIN_X, originX );
		bundle.put( ORIGIN_Y, originY );
		bundle.put( LIGHT, lightName );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		originX = bundle.contains( ORIGIN_X ) ? bundle.getInt( ORIGIN_X ) : Integer.MIN_VALUE;
		originY = bundle.contains( ORIGIN_Y ) ? bundle.getInt( ORIGIN_Y ) : Integer.MIN_VALUE;
		lightName = bundle.contains( LIGHT ) ? bundle.getString( LIGHT ) : "";
	}
}
