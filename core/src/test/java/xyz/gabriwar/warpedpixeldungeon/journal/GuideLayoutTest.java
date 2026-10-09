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

package xyz.gabriwar.warpedpixeldungeon.journal;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The guide's geometry: the whole tree opened out never puts two cards on top of each other in
 * either layout, a parent sits centred on its children, connectors are runs of whole pixels that
 * draw out from the parent, and the zoom steps are whole numbers around the interface's own.
 */
public class GuideLayoutTest {

	//the whole tree, every chapter open, cards of uneven heights
	private static GuideGraph.Node openTree(){
		GuideGraph.Node root = GuideGraph.build();
		int i = 0;
		for (GuideGraph.Node n : GuideGraph.all( root )){
			n.expanded = true;
			n.lw = 96;
			n.lh = 18 + (i++ * 7) % 13;
		}
		return root;
	}

	private static ArrayList<GuideGraph.Node> visible( GuideGraph.Node n, ArrayList<GuideGraph.Node> out ){
		out.add( n );
		if (GuideLayout.showsChildren( n )) for (GuideGraph.Node c : n.children) visible( c, out );
		return out;
	}

	private static void assertNoOverlap( GuideGraph.Node root ){
		ArrayList<GuideGraph.Node> all = visible( root, new ArrayList<>() );
		for (int a = 0; a < all.size(); a++){
			for (int b = a + 1; b < all.size(); b++){
				GuideGraph.Node p = all.get( a ), q = all.get( b );
				boolean overlap = p.lx < q.lx + q.lw && q.lx < p.lx + p.lw && p.ly < q.ly + q.lh && q.ly < p.ly + p.lh;
				assertFalse( p.id + " overlaps " + q.id, overlap );
			}
		}
	}

	@Test
	public void theTreeNeverStacksCards(){
		GuideGraph.Node root = openTree();
		GuideLayout.layout( root, false );
		assertNoOverlap( root );
		assertEquals( 0, root.ly );
		for (GuideGraph.Node n : visible( root, new ArrayList<>() )){
			if (!GuideLayout.showsChildren( n )) continue;
			GuideGraph.Node first = n.children.get( 0 ), last = n.children.get( n.children.size() - 1 );
			for (GuideGraph.Node c : n.children){
				assertTrue( c.id + " is not right of its parent", c.lx >= n.lx + n.lw + GuideLayout.GAP_X );
				assertEquals( n.level + 1, c.level );
			}
			int mid = (first.ly + first.lh / 2 + last.ly + last.lh / 2) / 2;
			//centred on its children, unless pushed down to clear the card above it
			assertTrue( n.id + " is off its children's centre", n.ly + n.lh / 2 >= mid - 1 );
		}
	}

	@Test
	public void theOutlineIsOneCardPerRow(){
		GuideGraph.Node root = openTree();
		GuideLayout.layout( root, true );
		assertNoOverlap( root );
		int y = Integer.MIN_VALUE;
		for (GuideGraph.Node n : visible( root, new ArrayList<>() )){
			assertTrue( n.id, n.ly > y );
			y = n.ly;
			assertEquals( n.level * GuideLayout.INDENT, n.lx );
		}
		//the deepest card still fits a phone held upright, at the interface's own zoom
		int widest = 0;
		for (GuideGraph.Node n : visible( root, new ArrayList<>() )) widest = Math.max( widest, n.lx + n.lw );
		assertTrue( "outline " + widest + " wide", widest <= 135 - 8 );
	}

	@Test
	public void aCollapsedChapterHidesItsPages(){
		GuideGraph.Node root = openTree();
		for (GuideGraph.Node c : root.children) c.expanded = false;
		assertEquals( 1 + root.children.size(), visible( root, new ArrayList<>() ).size() );
		GuideLayout.layout( root, false );
		assertNoOverlap( root );
	}

	@Test
	public void connectorsAreWholePixelRuns(){
		for (boolean outline : new boolean[]{ false, true }){
			int[] path = outline
					? GuideLayout.connector( 0, 0, 96, 20, 9, 30, 96, 22, true )
					: GuideLayout.connector( 0, 40, 96, 20, 114, 0, 96, 22, false );
			//every leg straight along one axis
			for (int i = 2; i < path.length; i += 2){
				assertTrue( path[i] == path[i-2] || path[i+1] == path[i-1] );
			}
			int len = GuideLayout.length( path );
			int[] at = new int[2];
			GuideLayout.pointAt( path, 0, at );
			assertArrayEquals( new int[]{ path[0], path[1] }, at );
			GuideLayout.pointAt( path, len + 5, at );
			assertArrayEquals( new int[]{ path[path.length - 2], path[path.length - 1] }, at );

			//nothing drawn yet, then a part, then all of it: the legs only ever grow
			int[][] none = GuideLayout.legs( path, 0 );
			for (int[] r : none) assertTrue( r[2] == 0 || r[3] == 0 );
			int prev = 0;
			for (int d = 1; d <= len; d++){
				int area = 0;
				for (int[] r : GuideLayout.legs( path, d )){
					if (r[2] > 0 && r[3] > 0){
						assertTrue( r[2] == 1 || r[3] == 1 );
						area += r[2] * r[3];
					}
				}
				assertTrue( area >= prev );
				prev = area;
			}
			//drawn out, the legs reach the child: one pixel left of its card
			int[][] full = GuideLayout.legs( path, len );
			int[] lastLeg = full[full.length - 1];
			assertEquals( path[path.length - 2], lastLeg[0] + lastLeg[2] - 1 );
		}
	}

	@Test
	public void zoomStepsAreWholeAndAroundTheInterface(){
		for (int dz = 1; dz <= 12; dz++){
			int[] z = GuideLayout.zoomLevels( dz );
			assertTrue( z.length >= 2 );
			assertTrue( z[0] >= 1 );
			boolean has = false;
			for (int i = 0; i < z.length; i++){
				if (i > 0) assertTrue( z[i] > z[i-1] );
				if (z[i] == dz) has = true;
			}
			assertTrue( "no step at the interface's zoom " + dz, has );
			assertTrue( z.length <= 8 );
			assertEquals( Math.max( z[0] + 1, dz * 2 ), z[z.length - 1] );
		}
		int[] z = GuideLayout.zoomLevels( 7 );
		assertEquals( 7, GuideLayout.step( z, 5, +1 ) );
		assertEquals( 5, GuideLayout.step( z, 7, -1 ) );
		assertEquals( z[z.length - 1], GuideLayout.step( z, 99, +1 ) );
		assertEquals( z[0], GuideLayout.step( z, 0, -1 ) );
		assertEquals( 7, GuideLayout.step( z, 6.2f, +1 ) );
		assertEquals( 7, GuideLayout.nearest( z, 7.4f ) );
		//a box fits at the largest step at most the cap, or the smallest when nothing fits
		assertEquals( 7, GuideLayout.fit( z, 100, 100, 0, 2000, 2000, 7 ) );
		assertEquals( 5, GuideLayout.fit( z, 100, 100, 0, 600, 600, 7 ) );
		assertEquals( z[0], GuideLayout.fit( z, 1000, 1000, 0, 600, 600, 7 ) );
	}

	@Test
	public void theWheelZoomsByWholeNotches(){
		float[] acc = new float[1];
		//a sideways swipe or a tilted wheel sends nothing up or down: no zoom
		assertEquals( 0, GuideLayout.wheelSteps( acc, 0f ) );
		//a smooth wheel's small amounts add up to one step, not a step each
		assertEquals( 0, GuideLayout.wheelSteps( acc, 0.25f ) );
		assertEquals( 0, GuideLayout.wheelSteps( acc, 0.25f ) );
		assertEquals( 0, GuideLayout.wheelSteps( acc, 0.25f ) );
		assertEquals( -1, GuideLayout.wheelSteps( acc, 0.25f ) );
		//a plain wheel's notch is a step: down zooms out, up zooms in
		assertEquals( -1, GuideLayout.wheelSteps( acc, 1f ) );
		assertEquals( +1, GuideLayout.wheelSteps( acc, -1f ) );
		assertEquals( -2, GuideLayout.wheelSteps( acc, 2f ) );
		//turning back drops what was left over
		assertEquals( 0, GuideLayout.wheelSteps( acc, 0.5f ) );
		assertEquals( 0, GuideLayout.wheelSteps( acc, -0.75f ) );
		assertEquals( +1, GuideLayout.wheelSteps( acc, -0.25f ) );
	}

	@Test
	public void theViewKeepsSomeOfTheGraph(){
		//content from 0 to 500, a view 200 wide, 40 kept in view
		assertEquals( 100f, GuideLayout.clamp( 100, 200, 0, 500, 40 ), 0 );
		assertEquals( -160f, GuideLayout.clamp( -900, 200, 0, 500, 40 ), 0 );
		assertEquals( 460f, GuideLayout.clamp( 900, 200, 0, 500, 40 ), 0 );
		assertEquals( 0f, GuideLayout.ease( -1 ), 0 );
		assertEquals( 1f, GuideLayout.ease( 2 ), 0 );
		assertEquals( 1f, GuideLayout.easeOutBack( 1 ), 1e-5 );
	}
}
