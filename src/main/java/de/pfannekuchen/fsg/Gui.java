package de.pfannekuchen.fsg;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.dselent.bigarraylist.BigArrayList;

import kaptainwutax.biomeutils.source.OverworldBiomeSource;
import kaptainwutax.featureutils.structure.DesertPyramid;
import kaptainwutax.featureutils.structure.Village;
import kaptainwutax.seedutils.mc.ChunkRand;
import kaptainwutax.seedutils.mc.MCVersion;
import kaptainwutax.seedutils.mc.pos.CPos;
import kaptainwutax.seedutils.mc.pos.RPos;
import kaptainwutax.seedutils.util.math.DistanceMetric;

public class Gui {

	private static volatile AtomicReference<BigInteger> seedsChecked = new AtomicReference<BigInteger>(BigInteger.ZERO);
	private static final String maxSeeds = BigInteger.valueOf(1L << 16L).multiply(BigInteger.valueOf(1L << 48L)).toString();
	private static final BigInteger biomeSeeds = BigInteger.valueOf(1L << 16L);
	private static volatile BigArrayList<Long> seeds = new BigArrayList<>();
	
	public static void main(String[] args) throws InterruptedException {
		for (int i = 0; i < 12; i++) {
			final long limit = (1L << 48L) / 12;
			final long offset = limit * i;
			final long newLimit = limit + offset;
			Thread seedfind_thread = new Thread(new Runnable() {
				@Override
				public void run() {
					DesertPyramid pyramidStructure = new DesertPyramid(MCVersion.v1_8);
					Village villageStructure = new Village(MCVersion.v1_8);
					
					RPos playerSpawnRPos;
					ChunkRand rand = new ChunkRand();
					OverworldBiomeSource obs;
					CPos[][] pyramidCPos;
					CPos[][] villageCPos;
					CPos spawnCPos;
					int pX;
					int pZ;
					long seed;
					for (long structureSeed = offset; structureSeed < newLimit; structureSeed++) {
						pyramidCPos = new CPos[2][2];
						villageCPos = new CPos[2][2];
						for (long biomeSeed = 0; biomeSeed < 1L << 16L; biomeSeed++) {
							seed = biomeSeed<<48|structureSeed;
							
							obs = new OverworldBiomeSource(MCVersion.v1_7_2, seed);
							
							playerSpawnRPos = obs.getSpawnPoint().toRegionPos(512);
							pX = playerSpawnRPos.getX() + 1;
							pZ = playerSpawnRPos.getZ() + 1;
							if (pX > 1 || pX < 0) break;
							if (pZ > 1 || pZ < 0) break;
							if (pyramidCPos[pX][pZ] == null) {
								pyramidCPos[pX][pZ] = pyramidStructure.getInRegion(structureSeed, playerSpawnRPos.getX(), playerSpawnRPos.getZ(), rand);
								villageCPos[pX][pZ] = villageStructure.getInRegion(structureSeed, playerSpawnRPos.getX(), playerSpawnRPos.getZ(), rand);
							}
							
							if (!pyramidStructure.canSpawn(pyramidCPos[pX][pZ].getX(), pyramidCPos[pX][pZ].getZ(), obs)) continue;
							if (!villageStructure.canSpawn(villageCPos[pX][pZ].getX(), villageCPos[pX][pZ].getZ(), obs)) continue;
							
							spawnCPos = obs.getSpawnPoint().toChunkPos();
							
							if (spawnCPos.distanceTo(villageCPos[pX][pZ], DistanceMetric.EUCLIDEAN) > 16) continue;
							if (spawnCPos.distanceTo(pyramidCPos[pX][pZ], DistanceMetric.EUCLIDEAN) > 16) continue;
							if (pyramidCPos[pX][pZ].distanceTo(villageCPos[pX][pZ], DistanceMetric.EUCLIDEAN) > 16) break;
							seeds.add(seed);
						}
						seedsChecked.set(seedsChecked.get().add(biomeSeeds));
						System.out.println(seedsChecked.toString() + " out of \n" + maxSeeds + " seeds were checked and " + seeds.size() + " have been found.");
					}
				}
			});
			seedfind_thread.setName("Seed-Finding Thread-" + i);
			seedfind_thread.start();
		}
	}
	
}
