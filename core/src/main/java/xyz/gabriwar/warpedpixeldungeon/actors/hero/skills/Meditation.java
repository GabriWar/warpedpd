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


import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummonedPet;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

public class Meditation extends PassiveSkillA2 {

	{
		name = "Meditation";
		image = 26;
		tier = 2;
	}

	//turns of calm stillness between two pulses
	private static final int PULSE_TURNS = 5;
	private static final float SUMMON_MEND = 0.10f;

	//not bundled: a reload just starts the count again
	private int stillTurns = 0;
	private int lastPos = -1;
	private float lastTick = -1f;

	@Override
	protected boolean upgrade(){
		return true;
	}

	//ManaRegen asks once a turn while the pool is not full: that call is the meditation clock.
	//Standing still (or resting) with no enemy in sight builds towards a pulse
	@Override
	public int manaRegenerationBonus(){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || lastTick == Actor.now()) return 0;
		//a gap means the pool was full or the hero was elsewhere: the calm starts over
		if (Actor.now() - lastTick > 1f) stillTurns = 0;
		lastTick = Actor.now();

		boolean still = (hero.resting || hero.pos == lastPos) && hero.visibleEnemies() == 0;
		lastPos = hero.pos;
		if (!still){
			stillTurns = 0;
			return 0;
		}
		if (++stillTurns >= PULSE_TURNS){
			stillTurns = 0;
			pulse( hero );
		}
		return 0;
	}

	private void pulse( Hero hero ){
		int effectiveMT = hero.MT + RingOfMagic.manaBonus( hero );
		int gain = Math.min( 1 + level, effectiveMT - hero.MP );
		hero.MP += gain;
		if (hero.sprite != null){
			//the pulse: a flare, motes rising, and a second, wider ring of light a breath later
			new Flare( 5, 18 ).color( 0x66CCFF, true ).show( hero.sprite, 1f );
			hero.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 4 + level );
			hero.sprite.showStatus( CharSprite.POSITIVE, Messages.get( this, "pulse", gain ) );
			xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StaggerFX.after( 0.35f, () -> {
				if (hero.sprite != null) new Flare( 8, 30 ).color( 0x99DDFF, true ).show( hero.sprite, 0.8f );
			} );
		}
		Sample.INSTANCE.play( Assets.Sounds.MELD, 0.6f, 1.2f );

		if (level < MAX_LEVEL) return;
		//the calm reaches the servants in sight and mends them
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (!(m instanceof SummonedPet) || m.alignment != Char.Alignment.ALLY
					|| !m.isAlive() || m.HP >= m.HT || !Dungeon.level.heroFOV[m.pos]) continue;
			int mend = Math.min( SkillInteractions.ofHealth( m.HT, SUMMON_MEND ), m.HT - m.HP );
			m.HP += mend;
			if (m.sprite != null){
				m.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 3 );
				m.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( mend ), FloatingText.HEALING );
			}
		}
	}
}
