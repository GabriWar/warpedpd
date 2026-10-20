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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillMarkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LinkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.utils.PointF;


import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Callback;

import java.util.ArrayList;

public class BladeBind extends Skill {

	{
		tag = "CD";
		name = "Blade Bind";
		castText = "Bound!";
		image = 137;
		tier = 4;
		mana = 7;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && hero.MP >= getManaCost())
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public void execute( final Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){
			GameScene.selectCell( new CellSelector.Listener(){
				@Override
				public void onSelect( Integer target ){
					if (target != null){
						bind( hero, target );
					}
				}

				@Override
				public String prompt(){
					return Messages.get(BladeBind.class, "prompt");
				}
			} );
		}
	}

	//blades lock with an adjacent enemy: its blows are weakened
	private void bind( Hero hero, int target ){
		//the selector stays live across other casts, so re-check before spending
		if (level <= 0 || hero.MP < getManaCost()) return;

		final Char ch = Actor.findChar( target );
		if (ch == null || ch.alignment != Char.Alignment.ENEMY || !ch.isAlive()
				|| !Dungeon.level.adjacent( hero.pos, target )){
			GLog.w( Messages.get(this, "no_target") );
			return;
		}

		hero.MP -= getManaCost();
		castTextYell();
		Buff.prolong( ch, Weakness.class, 2 + 2 * level );
		//+3: you wrench it off balance: its next attack goes wide and leaves it open
		if (level >= MAX_LEVEL) Buff.affect( ch, Wrenched.class );
		SpatialSound.play( Assets.Sounds.HIT_PARRY, ch, 1f, 1.0f );
		SpatialSound.play( Assets.Sounds.HIT_PARRY, ch, 0.8f, 0.7f );
		//two tethers of steel light snap from her blade onto it and lock (never the Ethereal Chains)
		if (hero.sprite != null && hero.sprite.parent != null && ch.sprite != null){
			PointF a = hero.sprite.center(), b = ch.sprite.center();
			SkillMarkFX.tether( hero, ch, STEEL, TETHER_LIFE, -2 );
			SkillMarkFX.tether( hero, ch, STEEL, TETHER_LIFE, 2 );
			//the bite the second chain's landing gave, on the frame it landed (Chains: dist/320 + 0.2 s)
			PointF biteFrom = new PointF( a.x + 4, a.y + 2 ), biteTo = new PointF( b.x - 3, b.y + 3 );
			hero.sprite.parent.add( new Bite( biteFrom, biteTo, () -> {
				if (ch.sprite != null){
					ch.sprite.flash();
					ch.sprite.emitter().burst( SkillBursts.METAL_SPARK, 5 );
				}
				SpatialSound.play( Assets.Sounds.CHAINS, ch, 0.9f, 1.3f );
			} ) );
			//the blades drawn into the bind from both ends: steel glints pulled off her blade and off
			//it to a knot between them, snapping on the bite's own frame (the same two points, the
			//same clock; it bites nothing itself: the Bite above does)
			LinkFX.bind( hero.sprite.parent, biteFrom, biteTo, LinkFX.STEEL, null );
		}
		if (ch.sprite != null){
			BladeBind.this.chipOn( ch );
			//it is weakened, not dazed: steel sparks, never stun stars
			ch.sprite.emitter().burst( SkillBursts.METAL_SPARK, 6 );
			ch.sprite.flash();
		}
		xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter.center( ch.pos ).burst( SkillBursts.METAL_SPARK, 8 );
		Dungeon.hero.heroSkills.lastUsed = this;
		hero.sprite.operate( ch.pos );
		hero.spendAndNext( TIME_TO_USE );
	}

	/** The tethers' steel and how long they hold, s. */
	public static final int STEEL = 0xB9DEEE;
	public static final float TETHER_LIFE = 0.45f;

	/** What the second of the two chains that once drew the bind did when it landed, on its frame:
	 *  Chains' clock (dist/320 + 0.2 s from the first frame it was updated, passed by more than
	 *  that), without the chain. */
	public static final class Bite extends Gizmo {

		final float duration;
		private final com.watabou.utils.Callback then;
		private float spent;

		public Bite( PointF from, PointF to, com.watabou.utils.Callback then ){
			float dx = to.x - from.x;
			float dy = to.y - from.y;
			float distance = (float)Math.hypot( dx, dy );
			duration = distance/320f + 0.2f;
			this.then = then;
		}

		@Override
		public void update(){
			super.update();
			if ((spent += Game.elapsed) > duration){
				killAndErase();
				then.call();
			}
		}
	}

	//+3: the wrenched enemy's next attack on you misses outright
	@Override
	public boolean dodgeChance( Char attacker ){
		return level >= MAX_LEVEL && attacker != null && attacker.buff( Wrenched.class ) != null;
	}

	@Override
	public void onDodge( Char attacker ){
		if (attacker == null) return;
		Buff.detach( attacker, Wrenched.class );
		SkillInteractions.affectAfterHit( attacker, Vulnerable.class, 3f );
		if (attacker.sprite != null) attacker.sprite.emitter().burst( SkillBursts.METAL_SPARK, 4 );
		Sample.INSTANCE.play( Assets.Sounds.HIT_PARRY, 1f, 1.4f );
	}

	/** its weapon was wrenched aside: the next attack it makes goes wide */
	public static class Wrenched extends Buff {
		{
			type = buffType.NEGATIVE;
		}

		@Override
		public int icon(){ return xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator.NONE; }
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}
