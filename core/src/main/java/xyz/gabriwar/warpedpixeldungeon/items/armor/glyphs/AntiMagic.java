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

package xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Degrade;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hex;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicalSleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.duelist.ElementalStrike;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.mage.ElementalBlast;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.mage.WarpBeacon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.GuidingLight;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.HolyLance;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.HolyWeapon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.Judgement;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.Smite;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.Sunray;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrystalWisp;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DM100;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eye;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Shaman;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Warlock;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.YogFist;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.ChaliceOfBlood;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.ArcaneBomb;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.HolyBomb;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRetribution;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast;
import xyz.gabriwar.warpedpixeldungeon.items.wands.CursedWand;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfDisintegration;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFireblast;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFrost;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfLightning;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfLivingEarth;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfMagicMissile;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfPrismaticLight;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfTransfusion;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfWarding;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.HolyDart;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.DisintegrationTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GrimTrap;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Random;

import java.util.HashSet;

public class AntiMagic extends Armor.Glyph {

	private static ItemSprite.Glowing TEAL = new ItemSprite.Glowing( 0x88EEFF );
	
	public static final HashSet<Class> RESISTS = new HashSet<>();
	static {
		RESISTS.add( MagicalSleep.class );
		RESISTS.add( Charm.class );
		RESISTS.add( Weakness.class );
		RESISTS.add( Vulnerable.class );
		RESISTS.add( Hex.class );
		RESISTS.add( Degrade.class );
		
		RESISTS.add( DisintegrationTrap.class );
		RESISTS.add( GrimTrap.class );

		RESISTS.add( ArcaneBomb.class );
		RESISTS.add( HolyBomb.HolyDamage.class );
		RESISTS.add( ScrollOfRetribution.class );
		RESISTS.add( ScrollOfPsionicBlast.class );
		RESISTS.add( ScrollOfTeleportation.class );
		RESISTS.add( HolyDart.class );

		RESISTS.add( GuidingLight.class );
		RESISTS.add( HolyWeapon.class );
		RESISTS.add( Sunray.class );
		RESISTS.add( HolyLance.class );
		RESISTS.add( Smite.class );
		RESISTS.add( Judgement.class );

		RESISTS.add( ElementalBlast.class );
		RESISTS.add( CursedWand.class );
		RESISTS.add( WandOfBlastWave.class );
		RESISTS.add( WandOfDisintegration.class );
		RESISTS.add( WandOfFireblast.class );
		RESISTS.add( WandOfFrost.class );
		RESISTS.add( WandOfLightning.class );
		RESISTS.add( WandOfLivingEarth.class );
		RESISTS.add( WandOfMagicMissile.class );
		RESISTS.add( WandOfPrismaticLight.class );
		RESISTS.add( WandOfTransfusion.class );
		RESISTS.add( WandOfWarding.Ward.class );

		RESISTS.add( ChaliceOfBlood.class );

		RESISTS.add( ElementalStrike.class );
		RESISTS.add( Blazing.class );
		RESISTS.add( Shocking.class );
		RESISTS.add( Grim.class );

		RESISTS.add( WarpBeacon.class );
		
		RESISTS.add( DM100.LightningBolt.class );
		RESISTS.add( Shaman.EarthenBolt.class );
		RESISTS.add( CrystalWisp.LightBeam.class );
		RESISTS.add( Warlock.DarkBolt.class );
		RESISTS.add( Eye.DeathGaze.class );
		RESISTS.add( YogFist.BrightFist.LightBeam.class );
		RESISTS.add( YogFist.DarkFist.DarkBolt.class );
	}
	
	@Override
	public int proc(Armor armor, Char attacker, Char defender, int damage) {
		//no proc effect, triggers in Char.damage
		return damage;
	}
	
	public static int drRoll( Char owner, int level ){
		if (level == -1){
			return 0;
		} else {
			//scales with glyph level
			float multi = genericProcChanceMultiplier(owner) * owner.glyphPower(AntiMagic.class);
			return Random.NormalIntRange(
					Math.round(level * multi),
					Math.round((3 + (level * 1.5f)) * multi));
		}
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return TEAL;
	}

}