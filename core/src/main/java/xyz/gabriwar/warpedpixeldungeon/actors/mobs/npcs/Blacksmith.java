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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AscensionChallenge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.SanChikarah;
import xyz.gabriwar.warpedpixeldungeon.items.SanChikarahDeath;
import xyz.gabriwar.warpedpixeldungeon.items.SanChikarahLife;
import xyz.gabriwar.warpedpixeldungeon.items.SanChikarahTranscend;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.quest.DarkGold;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Pickaxe;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.ParchmentScrap;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.BlacksmithRoom;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.BlacksmithSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBlacksmith;
import xyz.gabriwar.warpedpixeldungeon.windows.WndQuest;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collection;

public class Blacksmith extends NPC {
	
	{
		spriteClass = BlacksmithSprite.class;

		properties.add(Property.IMMOVABLE);
	}

	@Override
	public Notes.Landmark landmark() {
		return (!Quest.completed() || Quest.rewardsAvailable()) ? Notes.Landmark.TROLL : null;
	}

	@Override
	protected boolean act() {
		if (Dungeon.hero.buff(AscensionChallenge.class) != null){
			die(null);
			Notes.remove( landmark() );
			return true;
		} else if (!Quest.rewardsAvailable() && Quest.completed()){
			Notes.remove( landmark() );
		}
		return super.act();
	}
	
	@Override
	public boolean interact(Char c) {

		sprite.turnTo( pos, c.pos );

		if (!(c instanceof Hero)) return true;
		final Hero h = (Hero) c;

		if (!Dungeon.sanchikarah && checkSanChikarah(h)) {
			return true;
		}

		Quest.HeroProgress hp = Quest.progressFor( h.id() );

		if (hp == null || !hp.given) {
			showIntro( h );
		} else if (!hp.completed) {
			String msg = Messages.get(this, "reminder") + "\n\n";
			switch (Quest.type){
				case Quest.CRYSTAL: msg += Messages.get(Blacksmith.this, "reminder_crystal"); break;
				case Quest.GNOLL:   msg += Messages.get(Blacksmith.this, "reminder_gnoll"); break;
				case Quest.FUNGI:   msg += Messages.get(Blacksmith.this, "reminder_fungi"); break;
			}
			tell( h, msg );
		} else if (Quest.rewardsAvailable( h )) {
			showServices( h );
		} else {
			tell( h, Messages.get(this, "get_lost") );
		}

		return true;
	}

	private void showIntro( final Hero h ) {
		String msg1 = "";
		switch (h.heroClass){
			case WARRIOR:   msg1 += Messages.get(Blacksmith.this, "intro_quest_warrior"); break;
			case MAGE:      msg1 += Messages.get(Blacksmith.this, "intro_quest_mage"); break;
			case ROGUE:     msg1 += Messages.get(Blacksmith.this, "intro_quest_rogue"); break;
			case HUNTRESS:  msg1 += Messages.get(Blacksmith.this, "intro_quest_huntress"); break;
			case DUELIST:   msg1 += Messages.get(Blacksmith.this, "intro_quest_duelist"); break;
			case CLERIC:    msg1 += Messages.get(Blacksmith.this, "intro_quest_cleric"); break;
		}
		msg1 += "\n\n" + Messages.get(Blacksmith.this, "intro_quest_start");

		String msg2 = "";
		switch (Quest.type){
			case Quest.CRYSTAL: msg2 += Messages.get(Blacksmith.this, "intro_quest_crystal"); break;
			case Quest.GNOLL:   msg2 += Messages.get(Blacksmith.this, "intro_quest_gnoll"); break;
			case Quest.FUNGI:   msg2 += Messages.get(Blacksmith.this, "intro_quest_fungi"); break;
		}

		final String msg1Final = msg1;
		final String msg2Final = msg2;

		if (h.isRemote) {
			Quest.give( h ); // hands the pickaxe + rolls this hero's rewards
			QuestSupport.sendInfo( this, h, msg1Final + (msg2Final.isEmpty() ? "" : "\n\n" + msg2Final) );
		} else {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show(new WndQuest(Blacksmith.this, msg1Final) {
						@Override
						public void hide() {
							super.hide();
							Quest.give( h );
							if (!msg2Final.isEmpty()){
								GameScene.show(new WndQuest(Blacksmith.this, msg2Final));
							}
						}
					} );
				}
			});
		}
	}

	private void showServices( final Hero h ) {
		if (h.isRemote) {
			Quest.sendServices( h );
		} else {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					Quest.HeroProgress hp = Quest.progressFor( h.id() );
					//in case game was closed during smith reward selection
					if (hp != null && hp.smithRewards != null && hp.smiths > 0){
						GameScene.show( new WndBlacksmith.WndSmith( Blacksmith.this, h ) );
					} else {
						GameScene.show(new WndBlacksmith(Blacksmith.this, h));
					}
				}
			});
		}
	}

	private void tell( final Hero h, final String text ) {
		if (h.isRemote) {
			QuestSupport.sendInfo( this, h, text );
		} else {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show( new WndQuest( Blacksmith.this, text ) );
				}
			});
		}
	}

	private boolean checkSanChikarah( Hero h ) {
		SanChikarahDeath death = h.belongings.getItem(SanChikarahDeath.class);
		SanChikarahLife life = h.belongings.getItem(SanChikarahLife.class);
		SanChikarahTranscend transcend = h.belongings.getItem(SanChikarahTranscend.class);

		if (death != null && life != null && transcend != null) {
			death.detach(h.belongings.backpack);
			life.detach(h.belongings.backpack);
			transcend.detach(h.belongings.backpack);

			SanChikarah sanChikarah = new SanChikarah();
			if (!sanChikarah.doPickUp(h)) {
				Dungeon.level.drop(sanChikarah, h.pos).sprite.drop();
			}

			Dungeon.sanchikarah = true;
			//the levels stop dropping the pieces once they are forged
			Dungeon.sanchikarahdeath = true;
			Dungeon.sanchikarahlife = true;
			Dungeon.sanchikarahtranscend = true;
			Sample.INSTANCE.play(Assets.Sounds.EVOKE);
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.heroLog(h, GLog.HIGHLIGHT + Messages.get(this, "sanchikarah"));
			tell(h, Messages.get(this, "sanchikarah"));
			return true;
		}
		return false;
	}

	@Override
	public int defenseSkill( Char enemy ) {
		return INFINITE_EVASION;
	}
	
	@Override
	public void damage( int dmg, Object src ) {
		//do nothing
	}

	@Override
	public boolean add( Buff buff ) {
		return false;
	}
	
	@Override
	public boolean reset() {
		return true;
	}

	public static class Quest {

		private static int type = 0;
		public static final int CRYSTAL = 1;
		public static final int GNOLL = 2;
		public static final int FUNGI = 3; //The fungi quest is not implemented, only exists partially in code

		//world state — the mine is shared geometry, so these stay global
		private static boolean spawned;
		private static boolean started;      // a hero has entered the mine
		private static boolean bossBeaten;   // any hero beat the mine boss
		private static boolean worldCompleted; // the mine quest has been finished by at least one hero

		// Per-hero reward economy: favor pool, pickaxe, service counts and the smith reward
		// pool are all tracked per hero (each hero mines into their own backpack -> own favor).
		public static class HeroProgress implements PerHeroProgress {
			public int heroId;
			public boolean given;       // this hero accepted the quest + received a pickaxe
			public boolean completed;   // this hero exited the mine and cashed ore into favor
			public int favor;
			public Item pickaxe;        // parked pickaxe between phases (null while held)
			public boolean freePickaxe;
			public int reforges, hardens, upgrades, smiths;
			public ArrayList<Item> smithRewards;
			public Weapon.Enchantment smithEnchant;
			public Armor.Glyph smithGlyph;

			@Override public int heroId() { return heroId; }

			@Override
			public void storeInBundle( Bundle b ) {
				b.put( HERO_ID, heroId );
				b.put( GIVEN, given );
				b.put( COMPLETED, completed );
				b.put( FAVOR, favor );
				if (pickaxe != null) b.put( PICKAXE, pickaxe );
				b.put( FREE_PICKAXE, freePickaxe );
				b.put( REFORGES, reforges );
				b.put( HARDENS, hardens );
				b.put( UPGRADES, upgrades );
				b.put( SMITHS, smiths );
				if (smithRewards != null) {
					b.put( SMITH_REWARDS, smithRewards );
					if (smithEnchant != null) { b.put( ENCHANT, smithEnchant ); b.put( GLYPH, smithGlyph ); }
				}
			}

			@Override
			public void restoreFromBundle( Bundle b ) {
				heroId = b.getInt( HERO_ID );
				given = b.getBoolean( GIVEN );
				completed = b.getBoolean( COMPLETED );
				favor = b.getInt( FAVOR );
				pickaxe = b.contains( PICKAXE ) ? (Item) b.get( PICKAXE ) : null;
				freePickaxe = b.getBoolean( FREE_PICKAXE );
				reforges = b.getInt( REFORGES );
				hardens = b.getInt( HARDENS );
				upgrades = b.getInt( UPGRADES );
				smiths = b.getInt( SMITHS );
				if (b.contains( SMITH_REWARDS )) {
					smithRewards = new ArrayList<>((Collection<Item>) ((Collection<?>) b.getCollection( SMITH_REWARDS )));
					if (b.contains( ENCHANT )) {
						smithEnchant = (Weapon.Enchantment) b.get( ENCHANT );
						smithGlyph   = (Armor.Glyph) b.get( GLYPH );
					}
				}
			}
		}

		private static final PerHeroStore<HeroProgress> progress = new PerHeroStore<>();

		public static void reset() {
			type        = 0;
			spawned		= false;
			started     = false;
			bossBeaten  = false;
			worldCompleted = false;
			progress.clear();
		}

		public static HeroProgress progressFor( int heroId ) { return progress.get( heroId ); }

		private static final String NODE	= "blacksmith";

		private static final String TYPE    	= "type";
		private static final String SPAWNED		= "spawned";
		private static final String STARTED		= "started";
		private static final String BOSS_BEATEN	= "boss_beaten";
		private static final String WORLD_COMPLETED	= "world_completed";
		private static final String PROGRESS	= "progress";

		private static final String HERO_ID		= "hero_id";
		private static final String GIVEN		= "given";
		private static final String COMPLETED	= "completed";
		private static final String FAVOR	    = "favor";
		private static final String PICKAXE	    = "pickaxe";
		private static final String FREE_PICKAXE= "free_pickaxe";
		private static final String REFORGES	= "reforges";
		private static final String HARDENS	    = "hardens";
		private static final String UPGRADES	= "upgrades";
		private static final String SMITHS	    = "smiths";
		private static final String SMITH_REWARDS = "smith_rewards";
		private static final String ENCHANT		= "enchant";
		private static final String GLYPH		= "glyph";

		public static void storeInBundle( Bundle bundle ) {

			Bundle node = new Bundle();

			node.put( SPAWNED, spawned );

			if (spawned) {
				node.put( TYPE, type );
				node.put( STARTED, started );
				node.put( BOSS_BEATEN, bossBeaten );
				node.put( WORLD_COMPLETED, worldCompleted );
				progress.store( node, PROGRESS );
			}

			bundle.put( NODE, node );
		}

		public static void restoreFromBundle( Bundle bundle ) {

			Bundle node = bundle.getBundle( NODE );

			progress.clear();

			if (!node.isNull() && (spawned = node.getBoolean( SPAWNED ))) {
				type = node.getInt(TYPE);
				started = node.getBoolean( STARTED );
				bossBeaten = node.getBoolean( BOSS_BEATEN );
				worldCompleted = node.contains( WORLD_COMPLETED ) && node.getBoolean( WORLD_COMPLETED );
				progress.restore( node, PROGRESS );
			} else {
				reset();
			}
		}

		public static ArrayList<Room> spawn( ArrayList<Room> rooms ) {
			if (!spawned && Dungeon.depth > 11 && Random.Int( 15 - Dungeon.depth ) == 0) {

				rooms.add(new BlacksmithRoom());
				spawned = true;

				//Currently cannot roll the fungi quest, as it is not fully implemented
				type = Random.IntRange(1, 2);
			}
			return rooms;
		}

		/** Register the quest for a hero, roll their personal smith-reward pool, and hand
		 *  them a pickaxe. */
		public static void give( Hero h ) {
			HeroProgress hp = progress.get( h.id() );
			if (hp == null) {
				hp = new HeroProgress();
				hp.heroId = h.id();
				progress.put( hp );
			}
			hp.given = true;
			hp.completed = false;
			generateRewards( hp, true );

			Item pick = hp.pickaxe != null ? hp.pickaxe : new Pickaxe().identify(false);
			hp.pickaxe = null;
			if (pick.doPickUp( h )) {
				xyz.gabriwar.warpedpixeldungeon.net.NetManager.heroLog( h,
						Messages.capitalize(Messages.get(h, "you_now_have", pick.name())) );
			} else {
				Dungeon.level.drop( pick, h.pos ).sprite.drop();
			}
		}

		public static void generateRewards( HeroProgress hp, boolean useDecks ){
			ArrayList<Item> smithRewards = new ArrayList<>();
			smithRewards.add(Generator.randomWeapon(3, useDecks));
			smithRewards.add(Generator.randomWeapon(3, useDecks));
			ArrayList<Item> toUndo = new ArrayList<>();
			while (smithRewards.get(0).getClass() == smithRewards.get(1).getClass()) {
				if (useDecks)   toUndo.add(smithRewards.get(1));
				smithRewards.remove(1);
				smithRewards.add(Generator.randomWeapon(3, useDecks));
			}
			for (Item i : toUndo){
				Generator.undoDrop(i);
			}
			smithRewards.add(Generator.randomMissile(3, useDecks));
			smithRewards.add(Generator.randomArmor(3));

			//30%:+0, 45%:+1, 20%:+2, 5%:+3
			int rewardLevel;
			float itemLevelRoll = Random.Float();
			if (itemLevelRoll < 0.3f){
				rewardLevel = 0;
			} else if (itemLevelRoll < 0.75f){
				rewardLevel = 1;
			} else if (itemLevelRoll < 0.95f){
				rewardLevel = 2;
			} else {
				rewardLevel = 3;
			}

			for (Item i : smithRewards){
				i.level(rewardLevel);
				if (i instanceof Weapon) {
					((Weapon) i).enchant(null);
				} else if (i instanceof Armor){
					((Armor) i).inscribe(null);
				}
				i.cursed = false;
			}

			hp.smithRewards = smithRewards;

			// 30% base chance to be enchanted, stored separately so status isn't revealed early
			hp.smithEnchant = Weapon.Enchantment.random();
			hp.smithGlyph = Armor.Glyph.random();

			float enchantRoll = Random.Float();
			if (enchantRoll > 0.3f * ParchmentScrap.enchantChanceMultiplier()){
				hp.smithEnchant = null;
				hp.smithGlyph = null;
			}
		}

		public static int Type(){
			return type;
		}

		// World gate: has any hero accepted the quest (used to open the shared mine).
		public static boolean given(){
			for (HeroProgress hp : progress.values()) if (hp.given) return true;
			return false;
		}

		public static boolean given( Hero h ) {
			HeroProgress hp = progress.get( h.id() );
			return hp != null && hp.given;
		}

		public static boolean started(){
			return started;
		}

		public static void start(){
			started = true;
		}

		public static boolean beatBoss(){
			return bossBeaten = true;
		}

		public static boolean bossBeaten(){
			return bossBeaten;
		}

		// World gate: has the mine quest been finished by at least one hero.
		public static boolean completed(){
			return worldCompleted;
		}

		public static boolean completedBy( Hero h ) {
			HeroProgress hp = progress.get( h.id() );
			return hp != null && hp.completed;
		}

		/** A hero exited the mine — cash their ore into their own favor. */
		public static void complete( Hero h ){
			HeroProgress hp = progress.get( h.id() );
			if (hp == null || hp.completed) return;
			hp.completed = true;
			worldCompleted = true;

			hp.favor = 0;
			DarkGold gold = h.belongings.getItem(DarkGold.class);
			if (gold != null){
				hp.favor += Math.min(2000, gold.quantity()*50);
				gold.detachAll(h.belongings.backpack);
			}

			Pickaxe pick = h.belongings.getItem(Pickaxe.class);
			if (pick != null) {
				if (pick.isEquipped(h)) {
					boolean wasCursed = pick.cursed;
					pick.cursed = false; //so that it can always be removed
					pick.doUnequip(h, false);
					pick.cursed = wasCursed;
				}
				pick.detach(h.belongings.backpack);
				hp.pickaxe = pick;
			}

			if (bossBeaten) hp.favor += 1000;

			Statistics.questScores[2] += hp.favor;

			if (hp.favor >= 2500){
				hp.freePickaxe = true;
			}
		}

		public static boolean rewardsAvailable( Hero h ){
			HeroProgress hp = progress.get( h.id() );
			return hp != null && (hp.favor > 0
					|| (hp.smithRewards != null && hp.smiths > 0)
					|| (hp.pickaxe != null && hp.freePickaxe));
		}

		// Route the service menu to a remote hero's client (KIND_BLACKSMITH).
		static void sendServices( Hero h ) {
			xyz.gabriwar.warpedpixeldungeon.net.BlacksmithService.sendMenu( h );
		}

		// True if ANY present hero still has rewards waiting (drives the global TROLL landmark).
		public static boolean rewardsAvailable(){
			for (HeroProgress hp : progress.values()) {
				if (hp.favor > 0
						|| (hp.smithRewards != null && hp.smiths > 0)
						|| (hp.pickaxe != null && hp.freePickaxe)) return true;
			}
			return false;
		}

	}
}
