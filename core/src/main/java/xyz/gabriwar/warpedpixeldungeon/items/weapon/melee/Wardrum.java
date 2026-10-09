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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import com.watabou.noosa.Camera;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Haste;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import com.watabou.utils.Callback;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.ui.AttackIndicator;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//SPS-PD's war drum: resonates on-hit, striking and unnerving everything adjacent to the target
public class Wardrum extends MeleeWeapon {

	{
		image = ItemSpriteSheet.WARDRUM;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 0.8f;

		tier = 3;
		RCH = 2;    //extra reach
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//resonance: half weapon damage, brief vertigo, and brief terror
		// to every other char adjacent to the target
		int p = defender.pos;
		for (int n : PathFinder.NEIGHBOURS8) {
			Char ch = Actor.findChar(p + n);
			if (ch != null && ch != defender && ch != attacker && ch.isAlive()) {
				int dmg = Math.max(Random.NormalIntRange(min(), max()) - Random.IntRange(0, 1), 0);
				ch.damage(dmg / 2, this);

				if (ch.isAlive()) {
					Buff.prolong(ch, Vertigo.class, 3f);
					Buff.prolong(ch, Terror.class, 3f).object = attacker.id();
				}
			}
		}
		return super.proc(attacker, defender, damage);
	}

	//SPS-PD weapon: no Duelist ability was ever designed for it

	private int beatTurns(){ return Math.min( 4, 2 + buffedLvl() / 3 ); }

	/** a war beat: every ally in sight is hasted, every enemy beside the drummer is terrified, and the ground shakes */
	@Override
	protected void duelistAbility( Hero hero, Integer target ){
		beforeAbilityUsed( hero, null );
		int turns = beatTurns();
		Buff.prolong( hero, Haste.class, turns );
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (!m.isAlive() || !Dungeon.level.heroFOV[m.pos]) continue;
			if (m.alignment == Char.Alignment.ALLY){
				Buff.prolong( m, Haste.class, turns );
				if (m.sprite != null) m.sprite.emitter().burst( Speck.factory( Speck.STAR ), 3 );
			} else if (m.alignment == Char.Alignment.ENEMY && Dungeon.level.distance( hero.pos, m.pos ) <= 2){
				Buff.prolong( m, Terror.class, 2f ).object = hero.id();
				if (m.sprite != null) m.sprite.emitter().burst( Speck.factory( Speck.SCREAM ), 2 );
			}
		}
		for (int n : PathFinder.NEIGHBOURS8){
			if (Dungeon.level.heroFOV[hero.pos + n]) CellEmitter.bottom( hero.pos + n ).burst( Speck.factory( Speck.DUST ), 2 );
		}
		Camera.main.shake( 3, 0.4f );
		SpatialSound.play( Assets.Sounds.HIT_CRUSH, hero, 1f, 0.6f );
		SpatialSound.play( Assets.Sounds.CHALLENGE, hero, 1f, 0.9f );
		hero.sprite.operate( hero.pos );
		Invisibility.dispel();
		hero.spendAndNext( 1f );
		afterAbilityUsed( hero );
	}

	@Override
	public String abilityInfo() {
		return Messages.get(this, levelKnown ? "ability_desc" : "typical_ability_desc", beatTurns());
	}

	@Override
	public String upgradeAbilityStat(int level) {
		return Integer.toString( Math.min( 4, 2 + level / 3 ) );
	}
}
