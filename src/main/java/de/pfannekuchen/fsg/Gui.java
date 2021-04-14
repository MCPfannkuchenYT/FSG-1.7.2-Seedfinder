package de.pfannekuchen.fsg;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import kaptainwutax.biomeutils.source.NetherBiomeSource;
import kaptainwutax.biomeutils.source.OverworldBiomeSource;
import kaptainwutax.featureutils.structure.DesertPyramid;
import kaptainwutax.featureutils.structure.Fortress;
import kaptainwutax.featureutils.structure.Village;
import kaptainwutax.seedutils.mc.ChunkRand;
import kaptainwutax.seedutils.mc.MCVersion;
import kaptainwutax.seedutils.mc.pos.BPos;
import kaptainwutax.seedutils.mc.pos.CPos;
import kaptainwutax.seedutils.mc.pos.RPos;
import kaptainwutax.seedutils.util.math.DistanceMetric;

public class Gui extends JFrame {

	private static final long serialVersionUID = -40442317103116380L;
	private static Vector<String> list = new Vector<>();
	
	public Gui(JList<String> field) {
		super("FSG 1.7.2");
		setResizable(false);
		setSize(200, 400);
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();	
		}
		
		setLayout(new BorderLayout());
		
		JButton c = new JButton("Copy");
		c.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (list.size() < field.getSelectedIndex() || field.getSelectedIndex() < 0) return;
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(list.get(field.getSelectedIndex())), null);
			}
		});
		
		add(c, BorderLayout.SOUTH);
		field.setFont(new Font(Font.MONOSPACED, 0, 16));
		((DefaultListCellRenderer) field.getCellRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
		field.setBackground(getBackground());
		
		add(field, BorderLayout.NORTH);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final AtomicLong t = new AtomicLong(System.currentTimeMillis());
		addMouseWheelListener(new MouseWheelListener() {
			
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (t.get() < 1) return;
				t.set(System.currentTimeMillis());
				if (list.size() > 14 && e.getUnitsToScroll() > 1) {
					int selected = field.getSelectedIndex();
					Gui.list.remove(0);
					field.setListData(Gui.list);
					if (selected > 0) {
						selected -= 1;
						field.setSelectedIndex(selected);
					}
				}
			}
		});
		
		setVisible(true);
	}
	
	public static void main(String[] args) throws InterruptedException {
		final JList<String> field = new JList<String>(list);
		Thread gui_thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				new Gui(field);
			}
		});
		gui_thread.setName("Gui Thread");
		gui_thread.start();
		for (int i = 0; i < 8; i++) {
			final long limit = (1L << 48L) / 8;
			final long offset = limit * i;
			final long newLimit = limit + offset;
			Thread seedfind_thread = new Thread(new Runnable() {
				
				@Override
				public void run() {
					DesertPyramid pyramidStructure = new DesertPyramid(MCVersion.v1_8);
					Village villageStructure = new Village(MCVersion.v1_8);
					Fortress fortress = new Fortress(MCVersion.v1_8);
					
					RPos playerSpawnRPos;
					ChunkRand rand = new ChunkRand();
					OverworldBiomeSource obs;
					CPos pyramidCPos;
					CPos villageCPos;
					CPos spawnCPos;
					CPos cpos2;
					CPos fortressPos = null;
					NetherBiomeSource nbs;
					for (long structureSeed = offset; structureSeed < newLimit; structureSeed++) {
						
						for (long biomeSeed = 0; biomeSeed < 1L << 16L; biomeSeed++) {
							
							final long seed = biomeSeed<<48|structureSeed;
							
							obs = new OverworldBiomeSource(MCVersion.v1_7_2, seed);
							
							playerSpawnRPos = obs.getSpawnPoint().toRegionPos(512);
							
							pyramidCPos = pyramidStructure.getInRegion(structureSeed, playerSpawnRPos.getX(), playerSpawnRPos.getZ(), rand);
							villageCPos = villageStructure.getInRegion(structureSeed, playerSpawnRPos.getX(), playerSpawnRPos.getZ(), rand);
							
							if (!pyramidStructure.canSpawn(pyramidCPos.getX(), pyramidCPos.getZ(), obs)) continue;
							if (!villageStructure.canSpawn(villageCPos.getX(), villageCPos.getZ(), obs)) continue;
							
							spawnCPos = obs.getSpawnPoint().toChunkPos();
							
							if (spawnCPos.distanceTo(villageCPos, DistanceMetric.EUCLIDEAN) > 12) continue;
							if (spawnCPos.distanceTo(pyramidCPos, DistanceMetric.EUCLIDEAN) > 12) continue;
							if (pyramidCPos.distanceTo(villageCPos, DistanceMetric.EUCLIDEAN) > 6) continue;
							
							nbs = new NetherBiomeSource(MCVersion.v1_7_2, seed);
							
							cpos2 = new BPos(obs.getSpawnPoint().getX() / 8, 0, obs.getSpawnPoint().getZ() / 8).toChunkPos();
							boolean successful = false;
							for (int x = -1; x < 2; x++) {
								for (int y = -1; y < 2; y++) {
									fortressPos = fortress.getInRegion(structureSeed, cpos2.getX() + x, cpos2.getY() + y, rand);
									
									if (fortressPos == null) continue;
									if (fortress.canSpawn(fortressPos.getX(), fortressPos.getZ(), nbs) && !successful) {
										successful = true;
										break;
									}
								}
								if (successful) break;
							}
							if (!successful) continue;
							
							list.add((seed) + "");
							int x = field.getSelectedIndex();
							field.setListData(list);
							field.setSelectedIndex(x);
							System.out.println("new seed found: " + list.get(list.size() - 1) + " - " + pyramidCPos + ":" + villageCPos + ":" + fortressPos);
							break;
						}
					}
					System.out.println("Seedfinding done...");
				}
			});
			seedfind_thread.setName("Seed-Finding Thread-" + i);
			seedfind_thread.start();
		}
	}
	
}
