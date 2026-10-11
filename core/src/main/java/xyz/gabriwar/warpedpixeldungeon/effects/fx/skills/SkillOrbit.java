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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.skills;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;

import java.util.ArrayList;

/**
 * What a buff that circles its bearer keeps on the scene: no picture of its own (the kit's FxOrbit
 * draws the motes, half behind the sprite and half in front, trailing as it moves) but the orbit's
 * keeper. Each frame it reads how many motes the buff holds and tells the orbit; when the bearer's
 * sprite is gone, or it is let go of (killed, erased, destroyed: Fx.dispose), the orbit goes too.
 * Subclasses add what a ring does besides circling (a glint, a mote flying off) in tick().
 *
 * Several keepers of one slot on one sprite (two stances held at once, two marks on a mob) share
 * its one orbit: it carries all their motes, each in its keeper's colour (tint()) in turn, set up
 * the way the first of them looks; the first one to update in a frame tells it so; and it is let
 * go of only with the last of them, so the others' motes never jump. A keeper taken off the scene
 * without being let go of drops out of its share the frame after it stops updating.
 * Made on any thread: it holds nothing of the GPU.
 */
public abstract class SkillOrbit extends Gizmo {

	protected final Char owner;
	protected final FxOrbit.Slot slot;
	protected FxOrbit.Ring ring;
	protected CharSprite sprite;
	protected float time;

	//the shared slots, each with its keepers in the order they came
	private static final ArrayList<Share> SHARES = new ArrayList<>();

	private static final class Share {
		final CharSprite sprite;
		final FxOrbit.Slot slot;
		final ArrayList<SkillOrbit> members = new ArrayList<>( 4 );
		final int[] colors = new int[FxOrbit.MOST];
		volatile FxOrbit.Ring ring;
		//when it was last told, and what it was told (its motes and colours, as one number)
		float toldAt = Float.NaN;
		volatile long told = Long.MIN_VALUE;
		boolean gone;

		Share( CharSprite sprite, FxOrbit.Slot slot ){
			this.sprite = sprite;
			this.slot = slot;
		}
	}

	private Share share;
	private float seenAt = Float.NaN;

	protected SkillOrbit( Char owner, FxOrbit.Slot slot ){
		this.owner = owner;
		this.slot = slot;
	}

	/** How many motes circle now. */
	protected abstract int count();

	/** The orbit set up for n motes (n > 0): motes(), trail(), breath(), speed(). When keepers
	 *  share a slot, the first one's look for all their motes together. */
	protected abstract void look( FxOrbit.Ring ring, int n );

	/** Its motes' colour when it shares its slot with another keeper; -1: none of its own. */
	protected int tint(){
		return -1;
	}

	/** Each frame, after the count is told: what else this ring does. */
	protected void tick( float dt ){}

	@Override
	public void update(){
		super.update();
		CharSprite s = owner.sprite;
		if (s == null || !s.exists){
			killAndErase();
			return;
		}
		if (s != sprite){
			//a new sprite (relinked): its own orbit
			release();
			sprite = s;
		}
		time += Game.elapsed;
		float now = Game.timeTotal;
		seenAt = now;
		Share sh;
		SkillOrbit first = null;
		int n = 0, k = 0;
		synchronized (SHARES){
			if (share == null || share.gone || !share.members.contains( this )) share = join( s, slot );
			sh = share;
			if (sh.toldAt != now){
				float last = sh.toldAt;
				sh.toldAt = now;
				//keepers that missed the last frame were taken off the scene: they drop out
				for (int i = sh.members.size() - 1; i >= 0; i--){
					SkillOrbit m = sh.members.get( i );
					if (m != this && !Float.isNaN( last ) && m.seenAt < last) sh.members.remove( i );
				}
				first = sh.members.get( 0 );
				for (SkillOrbit m : sh.members){
					n += Math.max( 0, m.count() );
					int t = m.tint();
					if (t >= 0 && k < sh.colors.length) sh.colors[k++] = t;
				}
			}
		}
		if (first != null){
			//this frame's teller
			if (sh.ring == null || sh.ring.isDetached()){
				sh.ring = FxOrbit.attach( s, slot );
				sh.told = Long.MIN_VALUE;
			}
			long said = n;
			if (k > 1) for (int i = 0; i < k; i++) said = said * 31 + sh.colors[i];
			if (said != sh.told){
				sh.told = said;
				if (n > 0) first.look( sh.ring, n );
				else sh.ring.motes( 0, FxFrames.MOTE_1, 0xFFFFFF, true );
				sh.ring.tints( sh.colors, k > 1 ? k : 0 );
			}
		}
		ring = sh.ring;
		tick( Game.elapsed );
	}

	//its slot's share on the sprite, joined (made if none). Under SHARES.
	private Share join( CharSprite s, FxOrbit.Slot slot ){
		Share found = null;
		for (int i = SHARES.size() - 1; i >= 0; i--){
			Share sh = SHARES.get( i );
			//a sprite gone with keepers that were never let go of: nothing will tell it again
			if (!sh.sprite.exists){
				sh.gone = true;
				SHARES.remove( i );
			} else if (sh.sprite == s && sh.slot == slot){
				found = sh;
			}
		}
		if (found == null){
			found = new Share( s, slot );
			SHARES.add( found );
		}
		found.members.add( this );
		return found;
	}

	/** Whether it is the first keeper of its slot (the one whose look the shared orbit wears):
	 *  what the orbit does once for all of them (a flare, a glint) it does. */
	protected final boolean leads(){
		synchronized (SHARES){
			return share == null || share.members.isEmpty() || share.members.get( 0 ) == this;
		}
	}

	/** The colour of the i-th keeper of its slot, round them in turn (its own when the i-th has
	 *  none, or it shares with no one). */
	protected final int mateTint( int i ){
		synchronized (SHARES){
			if (share == null || share.members.isEmpty()) return tint();
			int t = share.members.get( Math.floorMod( i, share.members.size() ) ).tint();
			return t >= 0 ? t : tint();
		}
	}

	private void release(){
		Share sh;
		boolean last;
		synchronized (SHARES){
			sh = share;
			share = null;
			last = false;
			if (sh != null){
				sh.members.remove( this );
				if (sh.members.isEmpty()){
					last = true;
					sh.gone = true;
					SHARES.remove( sh );
				} else {
					//the rest tell it again, without this one's motes
					sh.told = Long.MIN_VALUE;
				}
			}
		}
		if (last && sh.ring != null) sh.ring.detach();
		ring = null;
	}

	@Override
	public void kill(){
		release();
		super.kill();
	}

	@Override
	public void destroy(){
		release();
		super.destroy();
	}

	/** Where a mote at angle t of this slot sits about the sprite's middle (no lag), x. */
	protected float ellipseX( float t ){
		return SkillVfx.midX( sprite ) + slot.rx * (float) Math.cos( t );
	}

	protected float ellipseY( float t ){
		return SkillVfx.midY( sprite ) + slot.h + slot.ry * (float) Math.sin( t );
	}
}
