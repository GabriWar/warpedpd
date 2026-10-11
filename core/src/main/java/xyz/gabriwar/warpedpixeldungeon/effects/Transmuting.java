/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.Arc;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.ArcBitmask;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.TalentIcon;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.RectF;

//one thing becoming another over its owner's head: the old one's ghost fading in (never scaled
//up from nothing), then dissolving - motes leaving its painted pixels and swirling off it - a
//white bloom at the swap, and the new one forming as motes gather onto its pixels; then it fades.
//At most MOTES motes each way, from the effects' own random numbers; let go of when done.
public class Transmuting extends Component {

	private enum Phase {
		FADE_IN, TRANSMUTING, FADE_OUT
	}

	private static final float FADE_IN_TIME		= 0.2f;
	private static final float TRANSMUTING_TIME	= 1.4f;
	private static final float FADE_OUT_TIME	= 0.4f;

	private static final float ALPHA	= 0.6f;

	/** Motes leaving the old one, and gathering into the new; the swap's moment into the change
	 *  and its bloom's alpha. */
	public static final int MOTES = 30;
	public static final float SWAP_AT = 0.7f, BLOOM = 0.6f;
	/** How long a gathering mote takes to land, s. */
	public static final float GATHER = 0.4f;

	Image oldSprite;
	Image newSprite;

	private Char target;

	private Phase phase;
	private float duration;
	private float passed;

	private RectF oldUv, newUv;
	private final float[] at = new float[2];
	private int left, gathered;
	private float moteIn;
	private boolean swapped;

	public Transmuting( Item oldItem, Item newItem ){
		oldSprite = new ItemSprite(oldItem);
		oldSprite.originToCenter();
		add(oldSprite);
		newSprite = new ItemSprite(newItem);
		newSprite.originToCenter();
		add(newSprite);

		oldSprite.alpha(0f);
		newSprite.alpha(0f);

		phase = Phase.FADE_IN;
		duration = FADE_IN_TIME;
		passed = 0;
	}

	public Transmuting( Talent oldTalent, Talent newTalent ){
		oldSprite = new TalentIcon(oldTalent);
		oldSprite.originToCenter();
		add(oldSprite);
		newSprite = new TalentIcon(newTalent);
		newSprite.originToCenter();
		add(newSprite);

		oldSprite.alpha(0f);
		newSprite.alpha(0f);

		phase = Phase.FADE_IN;
		duration = FADE_IN_TIME;
		passed = 0;
	}

	@Override
	public void update() {
		super.update();

		if (target == null || target.sprite == null) {
			Fx.dispose( this );
			return;
		}

		if (passed == 0) {
			float cx = target.sprite.x + target.sprite.width() / 2f;
			oldSprite.x = cx - oldSprite.width() / 2;
			oldSprite.y = target.sprite.y - oldSprite.height();

			newSprite.x = cx - newSprite.width() / 2;
			newSprite.y = target.sprite.y - newSprite.height();
			if (oldUv == null) oldUv = oldSprite.frame();
			if (newUv == null) newUv = newSprite.frame();
		}

		switch (phase) {
			case FADE_IN:
				oldSprite.alpha( passed / duration * ALPHA );
				break;
			case TRANSMUTING:
				oldSprite.alpha( Math.max( 0f, 1f - passed / SWAP_AT ) * ALPHA );
				newSprite.alpha( Math.max( 0f, (passed - SWAP_AT) / (duration - SWAP_AT) ) * ALPHA );
				motes();
				break;
			case FADE_OUT:
				newSprite.alpha( (1 - passed / duration) * ALPHA );
				break;
		}

		if ((passed += Game.elapsed) > duration) {
			switch (phase) {
				case FADE_IN:
					phase = Phase.TRANSMUTING;
					duration = TRANSMUTING_TIME;
					left = MOTES;
					gathered = MOTES;
					moteIn = 0;
					break;
				case TRANSMUTING:
					phase = Phase.FADE_OUT;
					duration = FADE_OUT_TIME;
					break;
				case FADE_OUT:
					Fx.dispose( this );
					return;
			}

			passed = 0;
		}
	}

	//the change: motes off the old one's pixels till the swap, a bloom, motes onto the new one's
	private void motes(){
		if (!Arc.onScene()) return;
		float cx = oldSprite.x + oldSprite.width() / 2f, cy = oldSprite.y + oldSprite.height() / 2f;
		if (!swapped && passed >= SWAP_AT){
			swapped = true;
			FxLight l = Arc.glow( cx, cy, 23, 0xFFFFFF, BLOOM );
			if (l != null) l.life( 0.03f, 0.05f, 0.3f );
		}
		if ((moteIn -= Game.elapsed) > 0) return;
		moteIn += SWAP_AT / MOTES;
		if (!swapped && left > 0){
			//leaving: a pixel of the old one swirling off round its middle, rising
			left--;
			ArcBitmask.pick( oldSprite, oldUv, at );
			FxParticle p = Arc.light( at[0], at[1], FxBudget.P2 );
			if (p == null) return;
			float dx = at[0] - cx, dy = at[1] - cy;
			p.look( FxFrames.MOTE_1 ).paint( Element.ARCANE.ramp.stop( 1 ) ).vel( -dy * 3f + dx, dx * 3f - 8f ).drag( 1.5f )
					.life( FxRandom.Float( 0.4f, 0.6f ) ).alpha( 1f, 0, 0.25f );
		} else if (swapped && gathered > 0 && passed < duration - GATHER * 0.5f){
			//gathering: onto a pixel of the new one from 8-12 px off it
			gathered--;
			ArcBitmask.pick( newSprite, newUv, at );
			float a = FxRandom.angle(), r = FxRandom.Float( 8f, 12f );
			float sx = at[0] + (float)Math.cos( a ) * r, sy = at[1] + (float)Math.sin( a ) * r;
			FxParticle p = Arc.light( sx, sy, FxBudget.P2 );
			if (p == null) return;
			p.look( FxFrames.MOTE_1 ).paint( Element.ARCANE.ramp.stop( 0 ) ).vel( (at[0] - sx) / GATHER, (at[1] - sy) / GATHER )
					.life( GATHER ).alpha( 1f, 0.1f, 0.05f );
		}
	}

	public static void show( Char ch, Item oldItem, Item newItem ) {

		if (!ch.sprite.visible) {
			return;
		}

		Transmuting sprite = new Transmuting( oldItem, newItem );
		sprite.target = ch;
		ch.sprite.parent.add( sprite );
	}

	public static void show( Char ch, Talent oldTalent, Talent newTalent ) {

		if (!ch.sprite.visible) {
			return;
		}

		Transmuting sprite = new Transmuting( oldTalent, newTalent );
		sprite.target = ch;
		ch.sprite.parent.add( sprite );
	}

}
