package com.eteks.test;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.FurnitureCategory;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Wall;
import com.eteks.sweethome3d.plugin.Plugin;
import com.eteks.sweethome3d.plugin.PluginAction;


public class PhoenixPCS extends Plugin 
{
	public class RoomTestAction extends PluginAction 
	{		

		public Home home = null;
		public Room room = null;
		
		public List<String> furnIds = new ArrayList<String>();
		public List<float[][]> furnRects = new ArrayList<float[][]>();
		public List<float[][]> furnRectsBloated = new ArrayList<float[][]>();
		
		public List<String> markBoxName = new ArrayList<String>();
		
		public int MARKBOX_COUNT = 6;
		public HomePieceOfFurniture[] markBoxes = new HomePieceOfFurniture[MARKBOX_COUNT];		
		
		public int markerIndx = 0;

		
		public List<String> wallIds = new ArrayList<String>();
		public List<float[][]> wallRects = new ArrayList<float[][]>();
		public List<Float> wallThicks = new ArrayList<Float>();
		
		public float ROOM_TOLERANCE = 0.51f;
		public float WALL_TOLERANCE = 0.1f;
		
		public float FURNITURE_BLOAT_SIZE = 2.0f;	// 2cm
				
		// ======================= CLASSES ======================= //
		
		public class Points
		{
			float x;
			float y;
			
			public Points()
			{
				x = -10.0f;
				y = -10.0f;
			}
			
			public Points(float xCoord , float yCoord)
			{
				x = xCoord;
				y = yCoord;
			}
		}				
		
		public class LineSegement
		{
			Points startP;		// x, y
			Points endP;		// x, y
			
			public LineSegement(Points sP, Points eP)
			{
				startP = sP;
				endP = eP;
			}
		}	
		
		public class WallSegement
		{
			Points startP;		// x, y
			Points endP;		// x, y
			float len;
			
			public WallSegement(Points sP, Points eP, float l)
			{
				startP = sP;
				endP = eP;
				len = l;
			}
		}
		
		public class Intersect
		{
			Points p;
			float dist;
			
			public Intersect(Points inP, float inD)
			{
				p = inP;
				dist = inD;
			}
		}
		
		public class InterPoints
		{
			Points p;
			boolean bOrg;
			
			public InterPoints(Points inP, boolean inB)
			{
				p = inP;
				bOrg = inB;
			}
		}
		
		public class FurnLoc
		{
			float w;
			float h;
			float el;
			float ang;
			Points p;
			
			public FurnLoc(float wIn, float hIn, float elIn, float angIn, Points coord)
			{
				w = wIn;
				h = hIn;
				el = elIn;
				ang = angIn;
				p = coord;
			}
			
			public FurnLoc()
			{
				w = 0.0f;
				h = 0.0f;
				el = 0.0f;
				ang = 0.0f;
				p = new Points();
			}
		}
		
		
		public RoomTestAction() 
		{
			putPropertyValue(Property.NAME, "PhoenixPCS");
			putPropertyValue(Property.MENU, "Phoenix-Fresh");

			// Enables the action by default
			setEnabled(true);
		}	
		
		// ======================= CODE ======================= //
		
		@Override
		public void execute() 
		{	
			home = getHome();
			room = home.getRooms().get(0);
			
			try
			{
				init();
				
				storeAllFurnRects(home);
				storeAllWallRects(home);
				
				markBoxes = getMarkerBoxes();
				
				// 1. Get inner wall segments ----------- //
				List<WallSegement> innerWSList = getInnerWalls();

			}
			catch(Exception e)
			{
				JOptionPane.showMessageDialog(null," -x-x-x- EXCEPTION : " + e.getMessage()); 
				e.printStackTrace();
			}
		}
		
		public List<WallSegement> getInnerWalls()
		{
			String wsStr = "";
			List<WallSegement> wallSegList = new ArrayList<WallSegement>();
			
			for(int w = 0; w < wallIds.size(); w++)
			{
				List<Points> validPoints = new ArrayList<Points>();
				
				for(int ws = 0; ws < wallRects.get(w).length; ws++)
				{
					Points p = new Points(wallRects.get(w)[ws][0], wallRects.get(w)[ws][1]);
					
					if(room.containsPoint(p.x, p.y, (ROOM_TOLERANCE * wallThicks.get(w))))
						validPoints.add(p);
				}
				
				for(int i = 1; i < validPoints.size(); i++)
				{
					LineSegement ls = new LineSegement( (validPoints.get(i-1)), (validPoints.get(i)) );					
					
					float dist = calcDistance(ls.startP, ls.endP);
					
					if(dist >= (WALL_TOLERANCE + wallThicks.get(w)))
					{
						wallSegList.add(new WallSegement(ls.startP, ls.endP, dist));
						wsStr += (wallIds.get(w) + " : (" + ls.startP.x + "," + ls.startP.y + ") -> (" + ls.endP.x + "," + ls.endP.y + ")\n");
					}					
				}
				
				wsStr += ("------------------\n\n");
			}
			
			JOptionPane.showMessageDialog(null, wsStr);
			
			return wallSegList;
		}
				
		// ======================= INIT FUNCTIONS ======================= //
		
		public void init()
		{
			furnIds = new ArrayList<String>();
			furnRects = new ArrayList<float[][]>();
			furnRectsBloated = new ArrayList<float[][]>();
			
			wallIds = new ArrayList<String>();
			wallRects = new ArrayList<float[][]>();			
			wallThicks = new ArrayList<Float>();
		}		
		
		public void storeAllFurnRects(Home h)
		{			
			for(HomePieceOfFurniture hp: h.getFurniture())
			{
				String fName = hp.getName();
				
				if(!fName.equals("boxred") && !fName.equals("boxgreen") )
				{					
					furnIds.add(hp.getName());
					furnRects.add(hp.getPoints());
					
					HomePieceOfFurniture hClone = hp.clone();
					float d = hp.getDepth();
					float w = hp.getWidth();
					
					hClone.setDepth(d + FURNITURE_BLOAT_SIZE);
					hClone.setWidth(w + FURNITURE_BLOAT_SIZE);
					hClone.setElevation(0.0f);
					
					furnRectsBloated.add(hClone.getPoints());
				}
			}
		}
		
		public void storeAllFurnRects(HomePieceOfFurniture hpf)
		{			
			String fName = hpf.getName();
			
			if(!fName.equals("boxred") && !fName.equals("boxgreen") )
			{
				furnIds.add(hpf.getName());
				furnRects.add(hpf.getPoints());
				
				HomePieceOfFurniture hClone = hpf.clone();
				float d = hpf.getDepth();
				float w = hpf.getWidth();
				
				hClone.setDepth(d + FURNITURE_BLOAT_SIZE);
				hClone.setWidth(w + FURNITURE_BLOAT_SIZE);
				hClone.setElevation(0.0f);
				
				furnRectsBloated.add(hClone.getPoints());
			}
		}
		
		public void storeAllWallRects(Home h)
		{
			int wallCount = 1;
			
			String furnRect = "";
			
			for(Wall w: h.getWalls())
			{
				wallIds.add("wall_" + wallCount);
				
				float[][] wRect = w.getPoints();
				wallRects.add(wRect);
				wallThicks.add(w.getThickness());		
				
				furnRect += ("Wall_"+ wallCount +" : " + wRect[0][0] + "," + wRect[0][1] + " / " + wRect[1][0] + "," + wRect[1][1] + " / " + wRect[2][0] + "," + wRect[2][1] + " / " + wRect[3][0] + "," + wRect[3][1] + "\n\n");
				
				wallCount++;
			}
			
			//JOptionPane.showMessageDialog(null, furnRect);
		}
				
		// ======================= UTIL FUNCTIONS ======================= //
				
		public float calcDistance(Points p1, Points p2)
		{
			float d = (float) Math.sqrt(((p2.x - p1.x) * (p2.x - p1.x)) + ((p2.y - p1.y) * (p2.y - p1.y)));
			return d;
		}		
		
		// ======================= DEBUG FUNCTIONS ======================= //

		public void putMarkers(Points p, int indx)
		{
			HomePieceOfFurniture box = null;

			box = markBoxes[indx].clone();			
			box.setX(p.x);
			box.setY(p.y);
			home.addPieceOfFurniture(box);
		}

		public HomePieceOfFurniture[] getMarkerBoxes()
		{
			HomePieceOfFurniture[] markBoxes = new HomePieceOfFurniture[MARKBOX_COUNT];
			int count = 0;

			List<FurnitureCategory> fCatg = getUserPreferences().getFurnitureCatalog().getCategories();

			for(int c = 0; c < fCatg.size(); c++ )
			{
				if(count >= MARKBOX_COUNT)
					break;

				List<CatalogPieceOfFurniture> catPOF = fCatg.get(c).getFurniture();

				for(int p = 0; p < catPOF.size(); p++ )
				{
					if(catPOF.get(p).getName().equals("boxred"))
					{
						markBoxes[0] = new HomePieceOfFurniture(catPOF.get(p));
						markBoxName.add("boxred");
						count++;
					}
					else if(catPOF.get(p).getName().equals("boxgreen"))
					{
						markBoxes[1] = new HomePieceOfFurniture(catPOF.get(p));
						markBoxName.add("boxgreen");
						count++;
					}
					else if(catPOF.get(p).getName().equals("boxblue"))
					{
						markBoxes[2] = new HomePieceOfFurniture(catPOF.get(p));
						markBoxName.add("boxblue");
						count++;
					}
					else if(catPOF.get(p).getName().equals("boxyellow"))
					{
						markBoxes[3] = new HomePieceOfFurniture(catPOF.get(p));
						markBoxName.add("boxyellow");
						count++;
					}
					else if(catPOF.get(p).getName().equals("boxteal"))
					{
						markBoxes[4] = new HomePieceOfFurniture(catPOF.get(p));
						markBoxName.add("boxteal");
						count++;
					}
					else if(catPOF.get(p).getName().equals("boxblack"))
					{
						markBoxes[5] = new HomePieceOfFurniture(catPOF.get(p));
						markBoxName.add("boxblack");
						count++;
					}

					if(count >= MARKBOX_COUNT)
						break;
				}	
			}

			return markBoxes;
		}
	}
	
	
	
	@Override
	public PluginAction[] getActions() 
	{
		return new PluginAction [] {new RoomTestAction()}; 
	}
}
