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

package xyz.gabriwar.warpedpixeldungeon.homestead.animals;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;

/**
 * What holds a Leadable animal (Roping), saved with it: the hero's rope (LEASHED: it follows him;
 * TIED: it keeps by a post, the post's world cell here), and whether it stands in a closed pen
 * (penned, looked at again every Roping.PEN_LOOK turns). A rope on it is drawn (RopeLine) to the
 * hero or the post. An animal found in a pen carries one from then on; it holds it (Roping.held)
 * only while roped, tied or penned.
 *
 * It stays on the animal when the animal is only taken off the scheduler - parked by a window
 * moving off it, or carried by a waystone (travel.Travel.leashed asks holder()) - though
 * Actor.remove takes every buff off a char: see detach.
 */
public class Leashed extends Buff {

	{
		type = buffType.NEUTRAL;
	}

	public enum Mode { FREE, LEASHED, TIED }

	public Mode mode = Mode.FREE;
	/** TIED: the post's world cell. */
	public int postX, postY;
	/** In a closed pen, as last looked at (Actor.now() then). */
	public boolean penned;
	public float penLooked = Float.NEGATIVE_INFINITY;
	/** LEASHED: the hero's world cell, and the one he stood on before it (OverworldLevel.worldKey):
	 *  where an animal that cannot follow is pulled to. */
	public long leaderAt = Long.MIN_VALUE, leaderWas = Long.MIN_VALUE;

	private RopeLine rope;

	/** Is the animal the homestead's now: roped, tied or penned. */
	public boolean holds(){
		return mode != Mode.FREE || penned;
	}

	/** Whose hand the rope is in: the hero's while it is LEASHED, nobody's tied or penned. */
	public Char holder(){
		return mode == Mode.LEASHED ? Dungeon.hero : null;
	}

	/**
	 * Actor.remove (Char.onRemove) detaches every buff of a char taken off the scheduler. A living
	 * animal only parked or carried keeps its rope (only its drawing goes); a dead one's rope falls
	 * where it lay.
	 */
	@Override
	public void detach(){
		if (target != null && target.isAlive() && !Actor.chars().contains( target )){
			fx( false );
			return;
		}
		if (target != null && !target.isAlive() && mode != Mode.FREE && Dungeon.level != null
				&& target.pos >= 0 && target.pos < Dungeon.level.length()){
			mode = Mode.FREE;
			Heap h = Dungeon.level.drop( new Rope(), target.pos );
			if (h.sprite != null) h.sprite.drop();
		}
		super.detach();
	}

	/** The mode changed: the rope is drawn or taken away to match. */
	public void changed(){
		if (target != null && target.sprite != null) fx( true );
	}

	@Override
	public void fx( boolean on ){
		if (on && mode != Mode.FREE && target != null && target.sprite != null){
			if (rope == null || rope.parent == null){
				//among the effects over everyone: the rope is drawn over the beast and the hero
				com.watabou.noosa.Group layer = GameScene.layer( GameScene.Layer.EFFECTS );
				if (layer == null) return;
				rope = new RopeLine( this );
				layer.add( rope );
			}
		} else if (rope != null){
			rope.killAndErase();
			rope = null;
		}
	}

	private static final String MODE = "mode", POST_X = "post_x", POST_Y = "post_y", PENNED = "penned",
			LEADER_AT = "leader_at", LEADER_WAS = "leader_was";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( MODE, mode );
		bundle.put( POST_X, postX );
		bundle.put( POST_Y, postY );
		bundle.put( PENNED, penned );
		bundle.put( LEADER_AT, leaderAt );
		bundle.put( LEADER_WAS, leaderWas );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		mode = bundle.contains( MODE ) ? bundle.getEnum( MODE, Mode.class ) : Mode.FREE;
		postX = bundle.getInt( POST_X );
		postY = bundle.getInt( POST_Y );
		penned = bundle.getBoolean( PENNED );
		leaderAt = bundle.contains( LEADER_AT ) ? bundle.getLong( LEADER_AT ) : Long.MIN_VALUE;
		leaderWas = bundle.contains( LEADER_WAS ) ? bundle.getLong( LEADER_WAS ) : Long.MIN_VALUE;
	}
}
