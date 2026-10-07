/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantArmor;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantRing;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantWand;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.StarFragment;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Ore;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore;
import xyz.gabriwar.warpedpixeldungeon.items.quest.DarkGold;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.Ores;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.BlacksmithSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBlacksmith2;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import xyz.gabriwar.warpedpixeldungeon.windows.WndQuest;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

public class Blacksmith2 extends NPC {

	{
		spriteClass = BlacksmithSprite.class;

		properties.add( Property.IMMOVABLE );
	}

	//lives in the town: sleeps at the inn (TownCommute)
	@Override
	public boolean sleepsAtInn() { return true; }

	@Override
	protected int bedtime() { return 140; }

	@Override
	public boolean interact( Char c ) {

		sprite.turnTo( pos, c.pos );

		if (xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.handleNetHero(c, description())) return true;
		if (c != Dungeon.hero) {
			return true;
		}
		if (Shopkeeper.closedForNight()) {
			tell( Messages.get(this, "asleep") );
			return true;
		}

		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show( new WndOptions( sprite(), Messages.titleCase( name() ),
						Messages.get( Blacksmith2.this, "menu" ),
						MasterworkCore.menuOptions( Messages.get( Blacksmith2.this, "opt_reinforce" ),
								Messages.get( Blacksmith2.this, "opt_starforge" ),
								Messages.get( Blacksmith2.this, "opt_smelt" ) ) ){
					@Override
					protected void onSelect( int index ){
						if (index == 0) coreWork( true );
						else if (index == 1) coreWork( false );
						else if (index == 2) reinforce();
						else if (index == 3) starForge();
						else if (index == 4) smelt();
					}
				} );
			}
		});

		return true;
	}

	//masterwork cores worked into an item: a masterwork step, or a new rarity
	private void coreWork( final boolean masterwork ){
		if (Dungeon.hero.belongings.getItem( MasterworkCore.class ) == null){
			tell( Messages.get( this, "no_cores" ) );
			return;
		}
		MasterworkCore.choose( Dungeon.hero, masterwork );
	}

	//the adamantite reinforcing he always did: it needs the metal and 50 dark gold
	private void reinforce(){
		DarkGold gold = Dungeon.hero.belongings.getItem( DarkGold.class );
		if (!checkAdamant()) {
			tell( Messages.get(this, "no_adamant") );
		} else if (gold == null || gold.quantity() < 50) {
			tell( Messages.get(this, "need_gold") );
		} else {
			GameScene.show( new WndBlacksmith2( Blacksmith2.this, Dungeon.hero ) );
		}
	}

	//two fragments of a fallen star folded together into a masterwork core: star-metal takes the
	//work like nothing dug out of the ground (items/StarFragment, levels/overworld/WorldEvents)
	private void starForge(){
		StarFragment shards = Dungeon.hero.belongings.getItem( StarFragment.class );
		if (shards == null || shards.quantity() < 2){
			tell( Messages.get( this, "no_fragments" ) );
			return;
		}
		if (shards.quantity() == 2) shards.detachAll( Dungeon.hero.belongings.backpack );
		else shards.quantity( shards.quantity() - 2 );
		Item.updateQuickslot();
		MasterworkCore core = new MasterworkCore();
		if (!core.collect( Dungeon.hero.belongings.backpack )){
			Dungeon.level.drop( core, Dungeon.hero.pos ).sprite.drop();
		}
		Sample.INSTANCE.play( Assets.Sounds.EVOKE );
		Dungeon.hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 8 );
		GLog.p( Messages.get( this, "starforged" ) );
		Dungeon.hero.spendAndNext( 2f );
	}

	//ore dug out of the world's rock (items/ore, levels/overworld/Ores) poured into masterwork cores,
	//one crucible a day; the rarest metals pay for their filings on top (the rates and why they are
	//what they are: Ores.Kind)
	private void smelt(){
		if (Dungeon.hero.belongings.getItem( Ore.class ) == null){
			tell( Messages.get( this, "no_ore" ) );
			return;
		}
		if (firedToday( Dungeon.hero )){
			tell( Messages.get( this, "smelt_tomorrow" ) );
			return;
		}
		GameScene.selectItem( new WndBag.ItemSelector(){
			@Override
			public String textPrompt(){
				return Messages.get( Blacksmith2.class, "smelt_prompt" );
			}
			@Override
			public Class<? extends Bag> preferredBag(){
				return xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings.Backpack.class;
			}
			@Override
			public boolean itemSelectable( Item item ){
				return item instanceof Ore;
			}
			@Override
			public void onSelect( Item item ){
				if (item instanceof Ore) smeltStack( (Ore) item );
			}
		} );
	}

	private void smeltStack( Ore ore ){
		int have = ore.quantity();
		String name = ore.name();
		Ores.Kind k = ore.kind();
		if (!pourCrucible( Dungeon.hero, ore, false )){
			tell( Messages.get( this, "smelt_short", k.batch, name, have ) );
			return;
		}
		Sample.INSTANCE.play( Assets.Sounds.EVOKE );
		Dungeon.hero.sprite.emitter().burst( Speck.factory( Speck.FORGE ), 8 );
		GLog.p( Messages.get( this, "smelted", k.batch, name ) );
		if (k.bonus > 0) GLog.p( Messages.get( this, "smelted_bonus", k.bonus ) );
		Dungeon.hero.spendAndNext( 2f );
	}

	/** Has this hero had his crucible today? One a day whichever forge pours it: the day is kept
	 *  on the hero (Hero.smeltDay), so neither a deep forge of every rift nor the copy of the troll
	 *  his commute makes each morning (TownCommute) is a second one. */
	public static boolean firedToday( Hero hero ){
		return hero.smeltDay == WorldClock.day();
	}

	/** Today's crucible from this stack, at the troll's forge or a deep one: whether it was poured
	 *  (not when the hero had his today, nor when the stack is short of a batch - then nothing is
	 *  taken). */
	public static boolean pourCrucible( Hero hero, Ore ore, boolean deepForge ){
		if (firedToday( hero ) || !smeltInto( hero, ore, deepForge )) return false;
		hero.smeltDay = WorldClock.day();
		return true;
	}

	/** The forge's side of smelting: one batch taken from the stack, the core into the hero's pack
	 *  (or at his feet), the filings paid for. Whether it was poured: not when the stack is short of
	 *  a batch, and then nothing is taken. */
	public static boolean smeltInto( Hero hero, Ore ore ){
		return smeltInto( hero, ore, false );
	}

	/** ...at the troll's forge, or at the deep forge by the burning rift (CaveSites, DwarvenForge):
	 *  its heat takes the rarest metals in smaller batches (Ores.Kind.forgeBatch), and no one there
	 *  buys the filings. One crucible a day either way (pourCrucible). */
	public static boolean smeltInto( Hero hero, Ore ore, boolean deepForge ){
		Ores.Kind k = ore.kind();
		int batch = deepForge ? k.forgeBatch() : k.batch;
		if (ore.quantity() < batch) return false;
		castCore( hero, ore, batch, deepForge ? 0 : k.bonus );
		return true;
	}

	//a batch taken from the stack, the core into the hero's pack (or at his feet), the filings' gold paid
	private static void castCore( Hero hero, Ore ore, int batch, int bonus ){
		int have = ore.quantity();
		if (batch == have) ore.detachAll( hero.belongings.backpack );
		else ore.quantity( have - batch );
		Item.updateQuickslot();
		Item core = new MasterworkCore();
		if (!core.collect( hero.belongings.backpack )){
			Heap h = Dungeon.level.drop( core, hero.pos );
			if (h.sprite != null) h.sprite.drop();
		}
		//straight into the purse, like a sale (Shopkeeper.paySale)
		if (bonus > 0) Shopkeeper.paySale( hero, bonus );
	}

	public static String verify( Item item1, Item item2 ) {

		if (item1 == item2) {
			return Messages.get(Blacksmith2.class, "same_item");
		}

		if (!item1.isIdentified()) {
			return Messages.get(Blacksmith2.class, "not_identified");
		}

		if (item1.cursed) {
			return Messages.get(Blacksmith2.class, "cursed");
		}

		if (item1.reinforced) {
			return Messages.get(Blacksmith2.class, "already_reinforced");
		}

		if (item1.level() < 0) {
			return Messages.get(Blacksmith2.class, "too_poor");
		}

		if (!item1.isUpgradable()) {
			return Messages.get(Blacksmith2.class, "cant_reinforce");
		}

		if (item1 instanceof Armor && item2 instanceof AdamantArmor) {
			return null;
		}

		if (item1 instanceof MeleeWeapon && item2 instanceof AdamantWeapon) {
			return null;
		}

		if (item1 instanceof Wand && item2 instanceof AdamantWand) {
			return null;
		}

		if (item1 instanceof Ring && item2 instanceof AdamantRing) {
			return null;
		}

		return Messages.get(Blacksmith2.class, "wrong_match");
	}

	public static void upgrade( Item item1, Item item2 ) {

		item1.reinforced = true;
		item2.detach( Dungeon.hero.belongings.backpack );

		DarkGold gold = Dungeon.hero.belongings.getItem( DarkGold.class );
		if (gold != null && gold.quantity() >= 50) {
			if (gold.quantity() == 50) {
				gold.detachAll( Dungeon.hero.belongings.backpack );
			} else {
				gold.quantity( gold.quantity() - 50 );
			}
		}

		GLog.p( Messages.get(Blacksmith2.class, "looks_better", item1.name()) );
		Dungeon.hero.spendAndNext( 2f );
		Badges.validateItemLevelAquired( item1 );
	}

	private void tell( String text ) {
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show( new WndQuest( Blacksmith2.this, text ) );
			}
		});
	}

	public static boolean checkAdamant() {
		AdamantArmor armor = Dungeon.hero.belongings.getItem( AdamantArmor.class );
		AdamantWeapon weapon = Dungeon.hero.belongings.getItem( AdamantWeapon.class );
		AdamantRing ring = Dungeon.hero.belongings.getItem( AdamantRing.class );
		AdamantWand wand = Dungeon.hero.belongings.getItem( AdamantWand.class );

		return armor != null || weapon != null || ring != null || wand != null;
	}

	@Override
	public int defenseSkill( Char enemy ) {
		return INFINITE_EVASION;
	}

	@Override
	public void damage( int dmg, Object src ) {
	}

	@Override
	public boolean add( Buff buff ) {
		return false;
	}

	@Override
	public boolean reset() {
		return true;
	}
}
