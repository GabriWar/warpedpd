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

package xyz.gabriwar.warpedpixeldungeon.homestead.stations;

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.bags.BuildersSatchel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * One thing a station makes (StationRecipes lists them all): what goes in and how many of each,
 * what comes out, which station does it, how many turns of work one batch takes, and whether the
 * work needs the station's fire lit (a kiln, a forge, a hearth: HeatStation). Like the alchemy
 * pot's Recipe it takes items and gives items, but nothing here costs alchemical energy: a
 * station's price is its fuel and its time.
 *
 * The hero asks for a number of batches at once (a job, StationJob). Everything for all of them
 * is taken from him when he asks (take), the satchel's stacks first, and the station holds it;
 * each batch, as it finishes, uses up its own share of what is held (useBatch) and makes its
 * output from exactly what it used (an Output may look at it: a blandfruit takes the potion of the
 * seed cooked with it).
 *
 * Made with the builder: {@code StationRecipe.at( "planks_oak", Workbench.class ).in( Log.class, 1 )
 * .out( Plank.class, 3 ).turns( 10 ).done()}, then StationRecipes.register.
 */
public final class StationRecipe {

	/**
	 * What one batch takes of one ingredient: `quantity` items of any one of its kinds. A kind
	 * matches its own class only, or (`subclasses`) every class under it too; a `filter` may turn
	 * some of them away (a blandfruit already cooked). A CHOSEN input is picked by the hero when he
	 * crafts (which seed goes in with the blandfruit): the job then takes that item's class only.
	 */
	public static final class Input {
		private final Class<? extends Item>[] kinds;
		public final int quantity;
		public final boolean subclasses;
		public final boolean chosen;
		private final Predicate<Item> filter;
		private final String labelKey;
		//what its icon shows, when not its first kind (a seed of no kind in particular)
		private final Class<? extends Item> icon;

		private Input( Class<? extends Item>[] kinds, int quantity, boolean subclasses, boolean chosen,
				Predicate<Item> filter, String labelKey, Class<? extends Item> icon ){
			this.kinds = kinds;
			this.quantity = quantity;
			this.subclasses = subclasses;
			this.chosen = chosen;
			this.filter = filter;
			this.labelKey = labelKey;
			this.icon = icon;
		}

		/** The classes it takes (or, with subclasses, takes anything under). */
		public List<Class<? extends Item>> kinds(){
			ArrayList<Class<? extends Item>> out = new ArrayList<>();
			Collections.addAll( out, kinds );
			return out;
		}

		/** Does this item count for it? `only`, when not null, narrows it to one class (a chosen
		 *  input once the hero has chosen). */
		public boolean matches( Item item, Class<? extends Item> only ){
			if (item == null || item.quantity() <= 0) return false;
			if (only != null){
				if (item.getClass() != only) return false;
			} else {
				boolean kind = false;
				for (Class<? extends Item> k : kinds){
					if (subclasses ? k.isInstance( item ) : item.getClass() == k){
						kind = true;
						break;
					}
				}
				if (!kind) return false;
			}
			return filter == null || filter.test( item );
		}

		/** An item for its icon: of its own icon class, else of its first kind. */
		public Item sample(){
			Class<? extends Item> cls = icon != null ? icon : kinds[0];
			Item i = Reflection.newInstance( cls );
			if (i != null) i.quantity( quantity );
			return i;
		}

		/** Its name in a list of ingredients: its own label (an input of several kinds: "raw
		 *  meat", "any seed"), else its one kind's name. */
		public String label(){
			if (labelKey != null) return Messages.get( StationRecipe.class, labelKey );
			Item i = Reflection.newInstance( kinds[0] );
			return i == null ? kinds[0].getSimpleName() : i.name();
		}

		/** How many the hero carries of it, in the satchel and the pack alike. */
		public int count( Hero hero, Class<? extends Item> only ){
			int n = 0;
			for (Item i : stacks( hero, this, only )) n += i.quantity();
			return n;
		}
	}

	/** What a batch makes. */
	public interface Output {
		/** The items one batch makes from what it used up (its inputs' share, in their order). */
		List<Item> make( List<Item> used );
		/** What the station's window shows it as. */
		Item sample();
	}

	/** An Output of so many of one item, whatever went in. */
	public static Output of( Class<? extends Item> cls, int quantity ){
		return new Output(){
			@Override
			public List<Item> make( List<Item> used ){
				Item i = Reflection.newInstance( cls );
				if (i == null) return Collections.emptyList();
				i.quantity( quantity );
				return Collections.singletonList( i );
			}

			@Override
			public Item sample(){
				Item i = Reflection.newInstance( cls );
				if (i != null) i.quantity( quantity );
				return i;
			}
		};
	}

	public final String id;
	/** The station that makes it (any subclass of it does too). */
	public final Class<? extends HomesteadStation> station;
	public final List<Input> inputs;
	public final Output output;
	/** Turns of work for one batch, at the plain rate (a workshop's stations work faster). */
	public final int turns;
	/** Only while the station's fire burns (HeatStation.lit). */
	public final boolean needsHeat;
	private final String nameKey;

	private StationRecipe( Builder b ){
		id = b.id;
		station = b.station;
		inputs = Collections.unmodifiableList( new ArrayList<>( b.inputs ) );
		output = b.output;
		turns = b.turns;
		needsHeat = b.heat;
		nameKey = b.nameKey;
	}

	/** What it is called in the station's window: its own name, else what it makes, "plank x3". */
	public String name(){
		if (nameKey != null) return Messages.get( StationRecipe.class, nameKey );
		Item s = output.sample();
		if (s == null) return id;
		return s.quantity() > 1 ? Messages.get( StationRecipe.class, "makes", s.name(), s.quantity() ) : s.name();
	}

	/** Its first chosen input, or null when the hero has nothing to choose. */
	public Input chosenInput(){
		for (Input in : inputs) if (in.chosen) return in;
		return null;
	}

	/** How many batches the hero can pay for now; `chosen` is the class he picked for the chosen
	 *  input (null: any of its kinds). */
	public int affordable( Hero hero, Class<? extends Item> chosen ){
		int n = Integer.MAX_VALUE;
		for (Input in : inputs){
			n = Math.min( n, in.count( hero, in.chosen ? chosen : null ) / in.quantity );
		}
		return n == Integer.MAX_VALUE ? 0 : n;
	}

	/**
	 * Takes all that `batches` batches need from the hero, the satchel's stacks first: the items,
	 * or null (nothing taken) when he has too few.
	 */
	public ArrayList<Item> take( Hero hero, int batches, Class<? extends Item> chosen ){
		if (batches <= 0 || affordable( hero, chosen ) < batches) return null;
		ArrayList<Item> out = new ArrayList<>();
		for (Input in : inputs){
			int need = in.quantity * batches;
			for (Item stack : stacks( hero, in, in.chosen ? chosen : null )){
				if (need <= 0) break;
				int take = Math.min( need, stack.quantity() );
				if (take == stack.quantity()){
					stack.detachAll( hero.belongings.backpack );
					out.add( stack );
				} else {
					Item part = stack.split( take );
					stack.updateQuickslot();
					if (part != null) out.add( part );
				}
				need -= take;
			}
		}
		return out;
	}

	/**
	 * One batch's share of what a job holds: taken out of `held` (stacks split as needed) and
	 * handed back in the inputs' order, or null - and `held` left as it was - when it holds too
	 * little.
	 */
	public ArrayList<Item> useBatch( ArrayList<Item> held ){
		//first that there is enough of everything, each item counted for one input only
		ArrayList<Item> claimed = new ArrayList<>();
		for (Input in : inputs){
			int need = in.quantity;
			for (Item i : held){
				if (need <= 0) break;
				if (claimed.contains( i ) || !in.matches( i, null )) continue;
				claimed.add( i );
				need -= i.quantity();
			}
			if (need > 0) return null;
		}
		ArrayList<Item> used = new ArrayList<>();
		for (Input in : inputs){
			int need = in.quantity;
			for (Item i : new ArrayList<>( held )){
				if (need <= 0) break;
				if (!in.matches( i, null )) continue;
				int take = Math.min( need, i.quantity() );
				if (take == i.quantity()){
					held.remove( i );
					used.add( i );
				} else {
					Item part = i.split( take );
					if (part != null) used.add( part );
				}
				need -= take;
			}
		}
		return used;
	}

	/** What a batch that used these makes. */
	public List<Item> produce( List<Item> used ){
		return output.make( used );
	}

	//every stack the hero carries that counts for an input, those in a builder's satchel first
	private static ArrayList<Item> stacks( Hero hero, Input in, Class<? extends Item> only ){
		ArrayList<Item> satchel = new ArrayList<>(), rest = new ArrayList<>();
		if (hero != null && hero.belongings != null) collect( hero.belongings.backpack, in, only, false, satchel, rest );
		satchel.addAll( rest );
		return satchel;
	}

	private static void collect( Bag bag, Input in, Class<? extends Item> only, boolean inSatchel,
			ArrayList<Item> satchel, ArrayList<Item> rest ){
		for (Item i : bag.items){
			if (i instanceof Bag){
				collect( (Bag) i, in, only, inSatchel || i instanceof BuildersSatchel, satchel, rest );
			} else if (in.matches( i, only )){
				(inSatchel ? satchel : rest).add( i );
			}
		}
	}

	// ----------------------------------------------------------------- the builder

	public static Builder at( String id, Class<? extends HomesteadStation> station ){
		return new Builder( id, station );
	}

	public static final class Builder {
		private final String id;
		private final Class<? extends HomesteadStation> station;
		private final ArrayList<Input> inputs = new ArrayList<>();
		private Output output;
		private int turns = 1;
		private boolean heat;
		private String nameKey;

		private Builder( String id, Class<? extends HomesteadStation> station ){
			this.id = id;
			this.station = station;
		}

		/** `quantity` of one item a batch. */
		@SuppressWarnings("unchecked")
		public Builder in( Class<? extends Item> kind, int quantity ){
			inputs.add( new Input( new Class[]{ kind }, quantity, false, false, null, null, null ) );
			return this;
		}

		/** `quantity` of any of several items a batch, named by its own label (StationRecipe's
		 *  message `labelKey`); the satchel's and the pack's first stacks go first. */
		@SafeVarargs
		public final Builder inAny( int quantity, String labelKey, Class<? extends Item>... kinds ){
			inputs.add( new Input( kinds, quantity, false, false, null, labelKey, null ) );
			return this;
		}

		/** `quantity` of one item of any class under `base` that passes `filter` (null for all),
		 *  the hero choosing which when he crafts; shown by an item of `icon`. */
		@SuppressWarnings("unchecked")
		public Builder inChosen( Class<? extends Item> base, int quantity, String labelKey, Predicate<Item> filter,
				Class<? extends Item> icon ){
			inputs.add( new Input( new Class[]{ base }, quantity, true, true, filter, labelKey, icon ) );
			return this;
		}

		/** `quantity` of one item that passes `filter`, of that one class. */
		@SuppressWarnings("unchecked")
		public Builder inWhere( Class<? extends Item> kind, int quantity, Predicate<Item> filter ){
			inputs.add( new Input( new Class[]{ kind }, quantity, false, false, filter, null, null ) );
			return this;
		}

		public Builder out( Class<? extends Item> cls, int quantity ){
			output = of( cls, quantity );
			return this;
		}

		public Builder out( Output output ){
			this.output = output;
			return this;
		}

		public Builder turns( int turns ){
			this.turns = turns;
			return this;
		}

		/** Works only while the station's fire is lit. */
		public Builder heat(){
			heat = true;
			return this;
		}

		/** Named in the window by StationRecipe's message `key` instead of by its output. */
		public Builder named( String key ){
			nameKey = key;
			return this;
		}

		public StationRecipe done(){
			return new StationRecipe( this );
		}
	}
}
