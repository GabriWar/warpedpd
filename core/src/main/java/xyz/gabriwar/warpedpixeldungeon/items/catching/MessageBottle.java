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

package xyz.gabriwar.warpedpixeldungeon.items.catching;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.items.EmptyBottle;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTitledMessage;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * A bottle with a message in it, out of the water on a fishing line (FishingRules.Junk.BOTTLE),
 * mostly out of the sea and the rivers. READ under the surface's open sky, its note tells of a cache
 * buried some way off (BottleCaches.place, measured from where it is read) and the world map marks
 * the spot (BottleMarks); the bottle is left empty (EmptyBottle). A spade turns the cache up. Where
 * no open ground in reach matches its sketch (out at sea, deep in the peaks) it stays unread.
 */
public class MessageBottle extends Item {

	public static final String AC_READ = "READ";

	//how many hands the notes are written in (note_N)
	public static final int VOICES = 4;

	{
		image = ItemSpriteSheet.MESSAGE_BOTTLE;
		defaultAction = AC_READ;
		stackable = true;
		bones = false;
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
		if (!action.equals( AC_READ ) || !Catching.mayUse( hero )) return;
		if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != 0){
			GLog.i( Messages.get( MessageBottle.class, "no_surface" ) );
			return;
		}
		CatchWorld world = CatchWorld.get();
		if (world == null) return;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		int w = ow.width();
		int hx = ow.worldX() + hero.pos % w, hy = ow.worldY() + hero.pos / w;
		int[] spot = BottleCaches.place( ow.worldSeed(), hx, hy, world.readNote() );
		//out at sea, or deep in the peaks or the snow, no open ground in reach matches the sketch:
		//the note is kept, to be read again somewhere else
		if (spot == null){
			GLog.i( Messages.get( MessageBottle.class, "no_match" ) );
			return;
		}
		if (detach( hero.belongings.backpack ) == null) return;
		EmptyBottle.giveBack( hero );
		world.addCache( spot[0], spot[1] );
		String text = Messages.get( MessageBottle.class, "note_" + Random.Int( VOICES ) ) + "\n\n"
				+ Messages.get( MessageBottle.class, "where", paces( hx, hy, spot[0], spot[1] ),
						Messages.get( MessageBottle.class, compass( spot[0] - hx, spot[1] - hy ) ) );
		GLog.p( Messages.get( MessageBottle.class, "marked" ) );
		SpatialSound.play( Assets.Sounds.READ, hero );
		GameScene.show( new WndTitledMessage( new ItemSprite( ItemSpriteSheet.MESSAGE_BOTTLE ),
				Messages.titleCase( Messages.get( MessageBottle.class, "note_title" ) ), text ) );
		hero.sprite.operate( hero.pos );
		hero.spendAndNext( 1f );
	}

	/** The distance in tens of paces, as a note gives it. */
	static int paces( int fx, int fy, int tx, int ty ){
		double d = Math.hypot( tx - fx, ty - fy );
		return Math.max( 10, (int)(Math.round( d / 10.0 ) * 10) );
	}

	/** The way from here to there as a compass point's key (y grows southwards). */
	static String compass( int dx, int dy ){
		double a = Math.toDegrees( Math.atan2( dx, -dy ) );
		if (a < 0) a += 360;
		String[] points = { "north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest" };
		return points[(int)Math.round( a / 45.0 ) % 8];
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
		return 15 * quantity;
	}
}
