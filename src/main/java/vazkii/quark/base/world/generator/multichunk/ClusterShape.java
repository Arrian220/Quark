package vazkii.quark.base.world.generator.multichunk;

import java.util.Random;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.PerlinNoiseGenerator;
import vazkii.quark.base.world.config.ClusterSizeConfig;

public class ClusterShape {
	
	private final BlockPos src;
	private final Vec3d radius;
	private final PerlinNoiseGenerator noiseGenerator;
	
	public ClusterShape(BlockPos src, Vec3d radius, PerlinNoiseGenerator noiseGenerator) {
		this.src = src;
		this.radius = radius;
		this.noiseGenerator = noiseGenerator;
	}
	
	public boolean isInside(BlockPos pos) {
		// normalize distances by the radius 
		double dx = (double) (pos.getX() - src.getX()) / radius.x;
		double dy = (double) (pos.getY() - src.getY()) / radius.y;
		double dz = (double) (pos.getZ() - src.getZ()) / radius.z;
		
		// convert to spherical
		double r = Math.sqrt(dx * dx + dy * dy + dz * dz);
		double phi = Math.atan2(dz, dx);
		double theta = r == 0 ? 0 : Math.acos(dy / r);
		
		// use phi, theta + the src pos to get noisemap uv
		double xn = phi + src.getX();
		double yn = theta + src.getZ();
		double noise = noiseGenerator.getValue(xn, yn);
		
		// when nearing the end of the loop, lerp back to the start to prevent it cutting off
		double cutoff = 0.75 * Math.PI;
		if(phi > cutoff) {
			double noise0 = noiseGenerator.getValue(-Math.PI + src.getX(), yn);
			noise = MathHelper.lerp((phi - cutoff) / (Math.PI - cutoff), noise, noise0);
		}
		
		// accept if within constrains
		double maxR = (noise / 16.0) + 0.5;
		return r < maxR;
	}

	public int getUpperBound() {
		return (int) Math.ceil(src.getY() + radius.getY());
	}
	
	public int getLowerBound() {
		return (int) Math.floor(src.getY() - radius.getY());
	}
	
	public static class Provider {
		
		private final ClusterSizeConfig sizeProvider;
		private final PerlinNoiseGenerator noiseGenerator;
		
		public Provider(ClusterSizeConfig provider, long seed) {
			this.sizeProvider = provider;
			noiseGenerator = new PerlinNoiseGenerator(new Random(seed), 4);
		}
		
		public ClusterShape around(BlockPos src) {
			Random rand = randAroundBlockPos(src);
			
			int radiusX = sizeProvider.horizontalSize + rand.nextInt(sizeProvider.horizontalVariation);
			int radiusY = sizeProvider.verticalSize + rand.nextInt(sizeProvider.verticalVariation);
			int radiusZ = sizeProvider.horizontalSize + rand.nextInt(sizeProvider.horizontalVariation);
					
			return new ClusterShape(src, new Vec3d(radiusX, radiusY, radiusZ), noiseGenerator);
		}
		
		public int getRadius() {
			return sizeProvider.horizontalSize + sizeProvider.horizontalVariation;
		}
		
		public Random randAroundBlockPos(BlockPos pos) {
			return new Random(31 * (31 * (31 + pos.getX()) + pos.getY()) + pos.getZ()); 
		}
		
	}
	
}