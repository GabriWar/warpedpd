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

import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import com.watabou.noosa.Camera;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import com.watabou.utils.Callback;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.ui.AttackIndicator;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//SPS-PD's trumpet: resonates on-hit, striking everything adjacent to the target
public class Trumpet extends MeleeWeapon {

	{
		image = ItemSpriteSheet.TRUMPET;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 0.8f;

		tier = 4;
		RCH = 2;    //extra reach
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//resonance: a third of weapon damage to every other char adjacent to the target
		int p = defender.pos;
		for (int n : PathFinder.NEIGHBOURS8) {
			Char ch = Actor.findChar(p + n);
			if (ch != null && ch != defender && ch != attacker && ch.isAlive()) {
				int dmg = Math.max(Random.NormalIntRange(min(), max()) - Random.IntRange(0, 1), 0);
				ch.damage(dmg / 3, this);
			}
		}
		return super.proc(attacker, defender, damage);
	}

	//SPS-PD weapon: no Duelist ability was ever designed for it

	@Override
	protected int baseChargeUse(Hero hero, Char target){
		return 2;
	}

	private int fanfareReach(){ return Math.min( 5, 3 + buffedLvl() / 3 ); }

	/** a fanfare: a blast that stops every enemy in earshot for a turn and heartens every ally */
	@Override
	protected void duelistAbility( Hero hero, Integer target ){
		beforeAbilityUsed( hero, null );
		int reach = fanfareReach();
		boolean any = false;
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (!m.isAlive() || !Dungeon.level.heroFOV[m.pos] || Dungeon.level.distance( hero.pos, m.pos ) > reach) continue;
			if (m.alignment == Char.Alignment.ENEMY){
				Buff.affect( m, Paralysis.class, 1f );
				if (m.sprite != null) m.sprite.emitter().burst( Speck.factory( Speck.SCREAM ), 3 );
				any = true;
			} else if (m.alignment == Char.Alignment.ALLY){
				Buff.prolong( m, Bless.class, 3f );
				if (m.sprite != null) m.sprite.emitter().burst( Speck.factory( Speck.STAR ), 3 );
			}
		}
		Buff.prolong( hero, Bless.class, 3f );
		if (hero.sprite != null){
			new Flare( 8, 12 + 4 * reach ).color( 0xFFD700, true ).show( hero.sprite, 0.9f ).angularSpeed = 45;
			hero.sprite.emitter().burst( Speck.factory( Speck.STAR ), 6 );
		}
		Camera.main.shake( any ? 2 : 1, 0.3f );
		Sample.INSTANCE.play( Assets.Sounds.CHALLENGE, 1f, 1.3f );
		Sample.INSTANCE.play( Assets.Sounds.BEACON, 0.8f, 1.5f );
		hero.sprite.operate( hero.pos );
		Invisibility.dispel();
		hero.spendAndNext( 1f );
		afterAbilityUsed( hero );
	}

	@Override
	public String abilityInfo() {
		return Messages.get(this, levelKnown ? "ability_desc" : "typical_ability_desc", fanfareReach());
	}

	@Override
	public String upgradeAbilityStat(int level) {
		return Integer.toString( Math.min( 5, 3 + level / 3 ) );
	}
}
