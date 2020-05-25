package com.projectkorra.pathfinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class Pathfinder {
	private final PathNode start, finish;
	private List<Material> traversables;

	public Pathfinder(Location start, Location finish) throws IllegalArgumentException {
		this(start, finish, new ArrayList<Material>());
	}

	public Pathfinder(Location start, Location finish, Material... traversables) throws IllegalArgumentException {
		this(start, finish, Arrays.asList((traversables != null) ? traversables : new Material[0]));
	}

	public Pathfinder(Location start, Location finish, List<Material> traversables) throws IllegalArgumentException {
		if (!start.getWorld().equals(finish.getWorld())) {
			throw new IllegalArgumentException("The specified start and finish Locations are not in the same World!");
		}

		this.start = new PathNode(start);
		this.finish = new PathNode(finish);
		this.traversables = traversables;
	}

	public Path calculate() {
		return calculate(-1);
	}

	public Path calculate(int maxNodes) {
		return calculate(maxNodes, true);
	}

	public Path calculate(boolean diagonal) {
		return calculate(-1, diagonal);
	}

	public Path calculate(int maxNodes, boolean diagonal) {
		List<PathNode> open = new ArrayList<PathNode>();
		List<PathNode> closed = new ArrayList<PathNode>();
		List<PathNode> navigated = new ArrayList<PathNode>();

		open.add(start);

		while (!open.isEmpty()) {
			PathNode current = null;
			for (PathNode node : open) {
				if (current == null || node.getH() < current.getH()) {
					current = node;
				}
			}

			if (current.getDistanceFrom(finish) < 1 || (navigated.size() >= maxNodes && maxNodes != -1)) {
				if (navigated.size() < maxNodes) {
					navigated.add(finish);
				} else {
					navigated.add(current);
				}

				if (!navigated.contains(finish)) {
					return null;
				}

				return reconstruct(navigated);
			}

			open.remove(current);
			closed.add(current);

			for (PathNode node : current.getNeighbors(diagonal)) {

				if (!isTraversable(node.getLocation().getBlock().getType())) {
					continue;
				}

				if (closed.contains(node)) {
					continue;
				}

				double tentG = current.getG() + current.getDistanceFrom(node);
				if (!open.contains(node) || tentG < node.getG()) {
					if (!navigated.contains(current)) {
						navigated.add(current);
					}

					node.setG(tentG);
					node.setH(node.getDistanceFrom(finish));

					if (!open.contains(node)) {
						open.add(node);
					}
				}
			}
		}

		return null;
	}

	private Path reconstruct(List<PathNode> navigated) {
		Path path = new Path(new ArrayList<Location>());

		for (int i = 0 ; i < navigated.size(); i++) {
			final PathNode current = navigated.get(i);
			path.append(current.getLocation());
		}

		return path;
	}

	public Location getStart() {
		return start.getLocation();
	}

	public Location getFinish() {
		return finish.getLocation();
	}

	public boolean isTraversable(Material material) {
		if (this.traversables == null || this.traversables.isEmpty()) {
			return true;
		}

		return this.traversables.contains(material);
	}

	public static class Path {
		private List<Location> locations;

		public Path(List<Location> locations) {
			this.locations = locations;
		}

		public Path append(Location other) {
			if (other == null) {
				return this;
			}

			this.locations.add(other);
			return this;
		}

		public Path append(Path other) {
			if (other == null) {
				return this;
			}

			this.locations.addAll(other.getLocations());
			return this;
		}

		public List<Location> getLocations() {
			return this.locations;
		}

		@Override
		public boolean equals(final Object object) {
			if (!(object instanceof Path)) {
				return false;
			}

			Path other = (Path) object;
			return this.locations.equals(other.locations);
		}
	}

	private class PathNode {
		private Location location;
		private double G, H, F;

		public PathNode(final Location location) {
			this(location, start, finish);
		}

		public PathNode(final int x, final int y, final int z, final World world) {
			this(new Location(world, x, y, z), start, finish);
		}

		public PathNode(final int x, final int y, final int z, final World world, final PathNode start, final PathNode target) {
			this(new Location(world, x, y, z), start, target);
		}

		public PathNode(final Location location, final PathNode start, final PathNode target) {
			this.location = location;
			this.G = this.getDistanceFrom(start);
			this.H = this.getDistanceFrom(target);
			this.F = this.G + this.H;
		}

		public double getDistanceFrom(final Location location) {
			if (location == null || !this.location.getWorld().equals(location.getWorld())) {
				return Double.POSITIVE_INFINITY;
			}

			return Math.abs(this.getX() - location.getBlockX()) + Math.abs(this.getY() - location.getBlockY())
					+ Math.abs(this.getZ() - location.getBlockZ());
		}

		public double getDistanceFrom(final PathNode other) {
			if (other == null) {
				return Double.POSITIVE_INFINITY;
			}
			return this.getDistanceFrom(other.getLocation());
		}

		public Location getLocation() {
			return this.location;
		}

		public int getX() {
			return this.location.getBlockX();
		}

		public int getY() {
			return this.location.getBlockY();
		}

		public int getZ() {
			return this.location.getBlockZ();
		}

		public double getG() {
			return this.G;
		}

		public void setG(double G) {
			this.G = G;
			this.F = this.G + this.H;
		}

		public double getH() {
			return this.H;
		}

		public void setH(double H) {
			this.H = H;
			this.F = this.G + this.H;
		}

		public double getF() {
			return this.F;
		}

		public List<PathNode> getNeighbors() {
			return getNeighbors(true);
		}

		public List<PathNode> getNeighbors(boolean diagonal) {
			List<PathNode> neighbors = new ArrayList<PathNode>();
			for (int x = -1; x <= 1; x++) {
				for (int y = -1; y <= 1; y++) {
					for (int z = -1; z <= 1; z++) {
						if(x == 0 && y == 0 && z == 0) {
							continue;
						}
						
						if (!diagonal && isDiagonalCoord(x, y, z)) {
							continue;
						}
						
						
						neighbors.add(new PathNode(new Location(this.location.getWorld(), this.getX() + x,
								this.getY() + y, this.getZ() + z)));
					}
				}
			}

			return neighbors;
		}

		private boolean isDiagonalCoord(int x, int y, int z) {
			if(x == 0 && z == 0) {
				return false;
			} else if (Math.abs(x) == Math.abs(z)) {
				return true;
			} else if (Math.abs(y) == 1 && (x != z)) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public boolean equals(final Object object) {
			if (!(object instanceof PathNode)) {
				return false;
			}
			PathNode other = (PathNode) object;
			return this.location.equals(other.location);
		}
	}
}
