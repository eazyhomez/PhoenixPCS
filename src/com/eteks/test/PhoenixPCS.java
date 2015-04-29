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
		
		// TODO : freeup this, design a class for furn info container 
		public List<HomePieceOfFurniture> furnList = new ArrayList<HomePieceOfFurniture>();
		
		public List<String> wallIds = new ArrayList<String>();
		public List<float[][]> wallRects = new ArrayList<float[][]>();
		public List<Float> wallThicks = new ArrayList<Float>();
		
		// TODO : Merge this into masterFreeWallSegList from start
		public List<List<WallSegement>> mastWallSegList = new ArrayList<List<WallSegement>>();
		
		public List<List<WallSegement>> masterFreeWallSegList = new ArrayList<List<WallSegement>>();
		
		public List<Design> designList = new ArrayList<Design>();
		
		public float ROOM_TOLERANCE = 0.51f;
		public float WALL_TOLERANCE = 0.1f;
		
		public float PARALLEL_COORDS = 10000.0f;
		public float TOLERANCE_PERCENT = 0.02f;		//0.05f;
		
		public float FURNITURE_BLOAT_SIZE = 2.0f;	// 2cm
		
		public float FURNITURE_PLACE_TOLERANCE = 0.0f;	// 0cm  // 5cm
		public float FURNITURE_INTERSECTION_PADDING = 60.0f;  // 2 feet
		
		public boolean bDebug = false;
		
		
		public float MEDIA_CAB_MIN = 122.0f; //355.7f;//461.6f;//122.0f;
		public float MEDIA_CAB_MAX = 244.0f; //375.0f;//
		public float MEDIA_CAB_CLEARANCE = 5.0f;
		
		public float MEDIA_CAB_TOLERANCE = 0.05f;
				
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
		
		public class Design
		{
			List<List<WallSegement>> mastFreeWSList;
			List<String> furnIds;
			List<float[][]> furnRects;
			List<float[][]> furnRectsBloated;
			// TBD
			
			public Design(List<List<WallSegement>> mFWSList, List<String> fIds, List<float[][]> fRects, List<float[][]> fRectsBl)
			{
				masterFreeWallSegList = mFWSList;
				furnIds = fIds;
				furnRects = fRects;
				furnRectsBloated = fRectsBl;
			}
		}
		
		
		public RoomTestAction() 
		{
			putPropertyValue(Property.NAME, "PhoenixPCS");
			putPropertyValue(Property.MENU, "Phoenix-Fresh");

			// Enables the action by default
			setEnabled(true);
		}	
		
		@Override
		public void execute() 
		{	
			home = getHome();
			room = home.getRooms().get(0);
			
			long startTime = System.currentTimeMillis();
			
			try
			{
				init();
				
				storeAllFurnRects(home);
				storeAllWallRects(home);
				
				checkWalls();
				
				float CURR_ELEVATION_LEVEL = 100.0f;				
				
				// For Floor Objects
				//String str = calcFurnIntersectionsAtElev(CURR_ELEVATION_LEVEL);  
				
				// masterFreeWallSegList - contains list of all free wall segements
				
				// For Wall Objects
				calcFurnIntersectionsAtElev(CURR_ELEVATION_LEVEL);  
				
				List<List<WallSegement>> currFWSeg = calculateFreeWallSeg(MEDIA_CAB_MIN);
				
				// Check for maximum and place accordingly
				chkFurnitureDimsAndPlace(currFWSeg, "Box345", MEDIA_CAB_MAX, MEDIA_CAB_CLEARANCE);
				
				long endTime = System.currentTimeMillis();
				
				JOptionPane.showMessageDialog(null, "No. of Designs generated : " + designList.size() + "\nTime : " + (endTime - startTime) + " ms");
				
			}
			catch(Exception e)
			{
				JOptionPane.showMessageDialog(null," -x-x-x- EXCEPTION : " + e.getMessage()); 
				e.printStackTrace();
			}
		}
		
		public void avoidIntersection(int wallIndx, List<List<WallSegement>> inFWSeg)
		{
			long startTime = System.currentTimeMillis();
			
			int wallCount = home.getWalls().size();
			
			int nextWallIndx = (wallIndx == 0) ? (wallCount - 2) : (wallIndx - 1);
			int prevWallIndx = (wallIndx == (wallCount - 1)) ? 0 : (wallIndx + 1);
			
			List<WallSegement> prevFreeWallSegList = inFWSeg.get(prevWallIndx);
			List<WallSegement> prevWSList = mastWallSegList.get(prevWallIndx);
			
			for(int p = (prevFreeWallSegList.size() - 1); p >= 0; p--)
			{
				WallSegement pWS = prevFreeWallSegList.get(p);
				float d = calcDistance(prevWSList.get(0).endP, pWS.startP);
				
				if(d > FURNITURE_INTERSECTION_PADDING)
				{					
					Points newP = getPointsAtDistOnLine(pWS.startP, pWS.endP, (d - FURNITURE_INTERSECTION_PADDING));
					WallSegement newWS = new WallSegement(pWS.startP, newP, calcDistance(pWS.startP, newP));
					prevFreeWallSegList.set(p, newWS);
					break;
				}
				else if(d < FURNITURE_INTERSECTION_PADDING)
				{
					//prevFreeWallSegList.remove(p);
					WallSegement newWS = new WallSegement(pWS.startP, pWS.startP, 0.0f);
					prevFreeWallSegList.add(p, newWS);
				}				
			}
			
			List<WallSegement> nextFreeWallSegList = inFWSeg.get(nextWallIndx);
			List<WallSegement> nextWSList = mastWallSegList.get(nextWallIndx);
			
			for(int n = 0; n < nextFreeWallSegList.size(); n++)
			{
				WallSegement nWS = nextFreeWallSegList.get(n);
				float d = calcDistance(nextWSList.get(0).startP, nWS.endP);
				
				//JOptionPane.showMessageDialog(null, nextWSList.get(0).startP.x + ", " + nextWSList.get(0).startP.y);

				if(d > FURNITURE_INTERSECTION_PADDING)
				{
					Points newP = getPointsAtDistOnLine(nWS.startP, nWS.endP, nWS.len - (d - FURNITURE_INTERSECTION_PADDING));
					//JOptionPane.showMessageDialog(null, nWS.startP.x + ", " + nWS.startP.y + " : " + newP.x + ", " + newP.y + " : " + nWS.endP.x + ", " + nWS.endP.y + " / " + d);
					WallSegement newWS = new WallSegement(newP, nWS.endP, calcDistance(newP, nWS.endP));
					nextFreeWallSegList.set(n, newWS);
					break;
				}	
				else
				{	
					//nextFreeWallSegList.remove(n);
					WallSegement newWS = new WallSegement(nWS.endP, nWS.endP, 0.0f);
					nextFreeWallSegList.set(n, newWS);
				}
			}
			
			long endTime = System.currentTimeMillis();
			
			//JOptionPane.showMessageDialog(null, prevWallIndx + " -> (" + wallIndx + ") -> " + nextWallIndx);
		}
		
		public void placeFurnItem(HomePieceOfFurniture inFurn, FurnLoc fLoc, int cnt)
		{
			HomePieceOfFurniture outFurn = inFurn.clone();
			outFurn.setName("PCS_C1_" + cnt);
			outFurn.setWidth(fLoc.w);
			outFurn.setAngle(fLoc.ang);
			outFurn.setX(fLoc.p.x);
			outFurn.setY(fLoc.p.y);
			
			home.addPieceOfFurniture(outFurn);
			
			//outFurn.setHeight(fLoc.h);
			//outFurn.setElevation(fLoc.el);
			//outFurn.setAngle(fLoc.ang);
		}
		
		public void chkFurnOrient(int mCount, WallSegement ws)
		{
			HomePieceOfFurniture furn = searchMatchFurn("PCS_C1_" + mCount);
			
			float[][] fRect = furn.getPoints();
			Points furnBottMid = new Points(((fRect[2][0] + fRect[3][0]) / 2),  ((fRect[2][1] + fRect[3][1]) / 2));
			
			Points wsMid = new Points(((ws.startP.x + ws.endP.x) / 2),  ((ws.startP.y + ws.endP.y) / 2));
			
			float dist = calcDistance(furnBottMid, wsMid);
			JOptionPane.showMessageDialog(null, "dist : " + dist);
			
			if(dist > MEDIA_CAB_TOLERANCE)
			{
				furn.setAngle((float)Math.PI);
				JOptionPane.showMessageDialog(null, "180 rotation");
			}
		}
		
		
		public HomePieceOfFurniture searchMatchFurn(String furnName)
		{
			HomePieceOfFurniture matchFurn = null;
			
			try 
			{				
				List<HomePieceOfFurniture> catPOF = home.getFurniture();

				for(int p = 0; p < catPOF.size(); p++ )
				{
					if(furnName.equalsIgnoreCase(catPOF.get(p).getName()))
					{
						matchFurn = catPOF.get(p);
						break;
					}
				}			
			}
			catch(Exception e){e.printStackTrace();}

			return matchFurn;
		}
		
		public void chkFurnitureDimsAndPlace(List<List<WallSegement>>inFWSegList, String furnName, float maxWidth, float clearance)
		{
			FurnLoc furnLoc = new FurnLoc();
			HomePieceOfFurniture newFurn = getFurnItem(furnName);
			
			int count = 1;
			
			try 
			{				
				for(int fw = 0; fw < inFWSegList.size(); fw++)
				{
					List<WallSegement> wallSegList = inFWSegList.get(fw);
					
					for(int w = 0; w < wallSegList.size(); w++)
					{
						WallSegement ws = wallSegList.get(w);
						
						if(ws.len > 0.0f)
						{
							float wsAngle = (float) Math.atan((ws.endP.y - ws.startP.y) / (ws.endP.x - ws.startP.x)); 
							
							if(ws.len > maxWidth)
							{
								furnLoc.w = (maxWidth - clearance);
								furnLoc.ang = wsAngle;
								
								furnLoc.p = calcFurnMids(ws.startP, ws.endP, (0.5f*newFurn.getDepth() + FURNITURE_PLACE_TOLERANCE));							
								
							}
							else if (ws.len > clearance)
							{
								furnLoc.w = (ws.len - clearance);
								furnLoc.ang = wsAngle;
								
								furnLoc.p = calcFurnMids(ws.startP, ws.endP, (0.5f*newFurn.getDepth() + FURNITURE_PLACE_TOLERANCE));
							}
							else
								continue;
							
							placeFurnItem(newFurn, furnLoc, count++);
							//avoidIntersection(fw, inFWSegList);							
							chkFurnOrient((count - 1), ws);
							
							saveDesign(inFWSegList, newFurn);
						}
					}
				}
			}
			catch(Exception e) 
			{
				JOptionPane.showMessageDialog(null," -x-x-x- EXCEPTION [chkFurnitureDimsAndPlace]: " + e.getMessage()); 
				e.printStackTrace();
			}
		}
		
		public void saveDesign(List<List<WallSegement>> inMasFWSList, HomePieceOfFurniture newFurn)
		{
			List<String> newFIds = new ArrayList<String>();
			newFIds.addAll(furnIds);
			newFIds.add(newFurn.getName());
			
			List<float[][]> newFRects = new ArrayList<float[][]>();
			newFRects.addAll(furnRects);
			
			List<float[][]> newFRectsBl = new ArrayList<float[][]>();
			newFRectsBl.addAll(furnRectsBloated);
			
			Design desg = new Design(inMasFWSList, newFIds, newFRects, newFRectsBl);
			designList.add(desg);
		}
		
		public HomePieceOfFurniture getFurnItem(String furnName)
		{
			HomePieceOfFurniture matchFurn = null;
			List<FurnitureCategory> fCatg = getUserPreferences().getFurnitureCatalog().getCategories();		

			try 
			{
				for(int c = 0; c < fCatg.size(); c++ )
				{
					List<CatalogPieceOfFurniture> catPOF = fCatg.get(c).getFurniture();

					for(int p = 0; p < catPOF.size(); p++ )
					{
						if(furnName.equalsIgnoreCase(catPOF.get(p).getName()))
						{
							matchFurn = new HomePieceOfFurniture(catPOF.get(p));
							//JOptionPane.showMessageDialog(null, "Found " + furnName);
							break;
						}
					}	
				}				
			}
			catch(Exception e){e.printStackTrace();}

			return matchFurn;
		}
		
		public void checkWalls()
		{
			String wsStr = "";
			
			for(int w = 0; w < wallIds.size(); w++)
			{
				List<Points> validPoints = new ArrayList<Points>();
				
				for(int ws = 0; ws < wallRects.get(w).length; ws++)
				{
					Points p = new Points(wallRects.get(w)[ws][0], wallRects.get(w)[ws][1]);
					
					if(room.containsPoint(p.x, p.y, (ROOM_TOLERANCE * wallThicks.get(w))))
						validPoints.add(p);
				}
				
				//JOptionPane.showMessageDialog(null, validPoints.size());
				
				List<WallSegement> wallSegList = new ArrayList<WallSegement>();
				
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
				
				mastWallSegList.add(w, wallSegList);
				
				wsStr += ("------------------\n\n");
			}			
			//JOptionPane.showMessageDialog(null, wsStr);
		}
		
		
		public List<List<WallSegement>> calculateFreeWallSeg(float minWidth)
		{
			List<List<WallSegement>> outMasterFreeWallSegList = new ArrayList<List<WallSegement>>();
			
			try 
			{				
				for(int fw = 0; fw < masterFreeWallSegList.size(); fw++)
				{
					List<WallSegement> wallSegList = masterFreeWallSegList.get(fw);
					List<WallSegement> outWallSegList = new ArrayList<WallSegement>();
					
					for(int w = 0; w < wallSegList.size(); w++)
					{
						if(wallSegList.get(w).len > minWidth)
							outWallSegList.add(wallSegList.get(w));
					}
					
					outMasterFreeWallSegList.add(outWallSegList);
				}
			}
			catch(Exception e) 
			{
				JOptionPane.showMessageDialog(null," -x-x-x- EXCEPTION [calculateFreeWallSeg]: " + e.getMessage()); 
				e.printStackTrace();
			}
			
			return outMasterFreeWallSegList;
		}
		
		
		public String printMasterFreeWallSegList(List<List<WallSegement>> inMastWallSegList)
		{
			String retStr = "";
			
			try 
			{				
				for(int fw = 0; fw < inMastWallSegList.size(); fw++)
				{
					List<WallSegement> wallSegList = inMastWallSegList.get(fw);
					
					for(int w = 0; w < wallSegList.size(); w++)
					{
						WallSegement ws = wallSegList.get(w);
						retStr += (fw + 1) + "." + (w + 1) + " : " + (ws.startP.x + "," + ws.startP.y + " -> " + ws.endP.x + "," + ws.endP.y + " >>> " + wallSegList.get(w).len + "\n");
					}
					
					retStr += "----------------\n";
				}
			}
			catch(Exception e) 
			{
				JOptionPane.showMessageDialog(null," -x-x-x- EXCEPTION [printMasterFreeWallSegList]: " + e.getMessage()); 
				e.printStackTrace();
			}
			
			return retStr;
		}
		
		
		public String calcFurnIntersectionsAtElev(float elv)
		{
			String retStr = "";
			
			// Compare which furn obj have elevation less than "elv"
			// Take intersection points for objects whose elevation + height is < "elv"
			
			try
			{				
				for(int m = 0; m < mastWallSegList.size(); m++)
				{
					List<WallSegement> wallSegList = mastWallSegList.get(m);			
					List<WallSegement> freeWallSegList = new ArrayList<WallSegement>();
					
					//JOptionPane.showMessageDialog(null, (m+1));
					
					int wallCounter  = 1;
					
					for(WallSegement ws : wallSegList)
					{
						TreeMap<Float, Intersect> interMap = new TreeMap<Float, Intersect>();
						
						Intersect wallS = new Intersect(ws.startP, 0.0f);
						interMap.put(0.0f, wallS);
						
						Intersect wallE = new Intersect(ws.endP, ws.len);
						interMap.put(ws.len, wallE);
						
						for(int f = 0; f < furnList.size(); f++)
						{
							float furnElev = furnList.get(f).getElevation();
							float furnH = furnList.get(f).getHeight();
							
							if((elv >= furnElev) && (elv <= (furnElev + furnH)))
							{							
								LineSegement ref = new LineSegement(ws.startP, ws.endP);								
								List<Intersect> interList = checkIntersect(ref, furnIds.get(f));
								
								for(Intersect inter : interList)
								{
									if(checkPointInBetween(ws.startP, ws.endP, inter.p, 0.0f, 0.0f, 0.0f))
									{
										interMap.put(inter.dist, inter);
										//JOptionPane.showMessageDialog(null, (m+1) + ", " + wallCounter + " -> Final Intersection (" + furnIds.get(f) + ") -> X : " + inter.p.x + ", Y : " + inter.p.y + ", Distance : " + inter.dist + " ----> " + ws.len);
									}
									
								}
							}
						}
						
						// Truncate the map so that end point is ws.endP	
						NavigableMap<Float, Intersect> interSet = interMap.headMap(ws.len, true);
						
						Set<Float> keys = interSet.keySet();
						List<Intersect> inList = new ArrayList<Intersect>();
						
						for(Float k : keys)
						{
							inList.add(interSet.get(k));
							//fwsStr += (wallCounter + " -> Intersection Points -> X : " + interSet.get(k).p.x + ", Y : " + interSet.get(k).p.y + ", Distance : " + interSet.get(k).dist + " ----> " + ws.len + "\n");
						}					
						
						for(int k = 1; k < inList.size();)
						{
							Intersect inter1 = inList.get(k - 1);
							Intersect inter2 = inList.get(k);
							
							WallSegement fws = new WallSegement(inter1.p, inter2.p, (inter2.dist - inter1.dist));
							freeWallSegList.add(fws);
							
							retStr += ((m+1) + "," + wallCounter + " -> freeWall_" + freeWallSegList.size() + " : " + fws.startP.x + "," + fws.startP.y  + " -> " + fws.endP.x  + "," + fws.endP.y + "\n");
							
							k+= 2;
						}
												
						wallCounter++;
					}				
					
					retStr += "-----------------------------\n";
					masterFreeWallSegList.add(m, freeWallSegList);
				}

				//JOptionPane.showMessageDialog(null, fwsStr);
				
			}
			catch(Exception e) 
			{
				JOptionPane.showMessageDialog(null," -x-x-x- EXCEPTION [calcFurnIntersections]: " + e.getMessage()); 
				e.printStackTrace();
			}
			
			return retStr;
		}
		
		
		// ================== calcFurnIntersections() ==================//
		
		public List<Intersect> checkIntersect(LineSegement r, String furn_id)
		{
			List<Intersect> interList = new ArrayList<Intersect>();
			
			int indx = -1;
			
			if((indx = furnIds.indexOf(furn_id)) > -1)
			{ 				
				float[][] fRect = furnRectsBloated.get(indx);
							
				LineSegement l1 = new LineSegement((new Points(fRect[0][0], fRect[0][1])) , (new Points(fRect[1][0], fRect[1][1])));
				LineSegement l2 = new LineSegement((new Points(fRect[1][0], fRect[1][1])) , (new Points(fRect[2][0], fRect[2][1])));
				LineSegement l3 = new LineSegement((new Points(fRect[2][0], fRect[2][1])) , (new Points(fRect[3][0], fRect[3][1])));
				LineSegement l4 = new LineSegement((new Points(fRect[3][0], fRect[3][1])) , (new Points(fRect[0][0], fRect[0][1])));
				
				
				/*
				String furnRect = ("furn : " + fRect[0][0] + "," + fRect[0][1] + " / " + fRect[1][0] + "," + fRect[1][1] + " / " + fRect[2][0] + "," + fRect[2][1] + " / " + fRect[3][0] + "," + fRect[3][1] + "\n\n");
				
				furnRect += ("l1 : " + l1.startP.x + "," + l1.startP.y  + " / " + l1.endP.x  + "," + l1.endP.y + "\n");
				furnRect += ("l2 : " + l2.startP.x + "," + l2.startP.y  + " / " + l2.endP.x  + "," + l2.endP.y + "\n");
				furnRect += ("l3 : " + l3.startP.x + "," + l3.startP.y  + " / " + l3.endP.x  + "," + l3.endP.y + "\n");
				furnRect += ("l4 : " + l4.startP.x + "," + l4.startP.y  + " / " + l4.endP.x  + "," + l4.endP.y + "\n");
						
				JOptionPane.showMessageDialog(null, furnRect);
				*/
				
				if(furn_id.equalsIgnoreCase("Armchair1"))
					bDebug = false; //true;
				else
					bDebug = false;
				
				String debug = "";
				
				Intersect inter = getIntersectPoint(r, l1);				
				if(inter.dist < PARALLEL_COORDS)
					interList.add(inter);
				
				debug += ("1. " + inter.p.x + "," + inter.p.y + " -> " + inter.dist + "\n");
				
				inter = getIntersectPoint(r, l2);				
				if(inter.dist < PARALLEL_COORDS)
					interList.add(inter);
				
				debug += ("2. " + inter.p.x + "," + inter.p.y + " -> " + inter.dist + "\n");
				
				inter = getIntersectPoint(r, l3);
				if(inter.dist < PARALLEL_COORDS)
					interList.add(inter);
				
				debug += ("3. " + inter.p.x + "," + inter.p.y + " -> " + inter.dist + "\n");
				
				inter = getIntersectPoint(r, l4);
				if(inter.dist < PARALLEL_COORDS)
					interList.add(inter);
				
				debug += ("4. " + inter.p.x + "," + inter.p.y + " -> " + inter.dist + "\n");
				
				if(bDebug)
					JOptionPane.showMessageDialog(null, debug);	
					
			}
			
			return interList;
		}
		
		
		// ================== checkIntersect() ==================//
		
		public Intersect getIntersectPoint(LineSegement ref, LineSegement l)
		{
			Intersect inter = new Intersect((new Points()), 0.0f);
			
			float A = (ref.endP.y - ref.startP.y);											// (y2 - y1)
			float B = (ref.startP.x - ref.endP.x);											// (x1 - x2)		
			float C = ((ref.endP.y * ref.startP.x) - (ref.startP.y * ref.endP.x));			// (y2x1 - y1x2)
			
			float P = (l.endP.y - l.startP.y);												// (y2' - y1')
			float Q = (l.startP.x - l.endP.x);												// (x1' - x2')		
			float R = ((l.endP.y * l.startP.x) - (l.startP.y * l.endP.x));					// (y2'x1' - y1'x2')
			
			float yNum = (P*C - R*A);
			float yDen = (P*B - Q*A);

			float xNum = (Q*C - R*B);
			float xDen = (Q*A - P*B);
			
			if((xDen == 0.0f) || (yDen == 0.0f))
			{
				inter.p = new Points(2*PARALLEL_COORDS, 2*PARALLEL_COORDS);
				inter.dist = PARALLEL_COORDS;
			}
			else
			{
				inter.p = new Points((xNum/xDen), (yNum/yDen));				
				boolean bC1 = checkPointInBetween(l.startP, l.endP, inter.p, P, Q, R);
				
				if(bDebug)
					JOptionPane.showMessageDialog(null, bC1 + " /  Intersection -> X : " + inter.p.x + ", Y : " + inter.p.y);
				
				if(bC1)
				{		
					inter.dist = calcDistance(inter.p, ref.startP);					
				}
				else
				{
					inter.p = new Points(PARALLEL_COORDS, PARALLEL_COORDS);
					inter.dist = PARALLEL_COORDS;
				}
			}
			
			return inter;			
		}		
		
		
		// ================== getIntersectPoint() ==================//
		
		public boolean checkPointInBetween(Points start, Points end, Points test, float P, float Q, float R)
		{
			boolean bRet = false;
			
			boolean bX = ((start.x <= test.x) && (test.x <= end.x)) || ((end.x <= test.x) && (test.x <= start.x));
			boolean bY = ((start.y <= test.y) && (test.y <= end.y)) || ((end.y <= test.y) && (test.y <= start.y));
			
			if(bX != bY)
			{				
				float distST = calcDistance(start, test);
				float distTE = calcDistance(test, end);
				float distSE = calcDistance(start, end);
				
				//float distSEAbs = (float)(Math.abs(distST + distTE - distSE));
				float distSEAbs = (distST + distTE - distSE);
				
				if(distSEAbs < 0.0f)
					distSEAbs = (0.0f - distSEAbs);
				
				if(distSEAbs <= (TOLERANCE_PERCENT * distSE))
					bRet = true;
			}
			else
				bRet = (bX && bY);
			
			//JOptionPane.showMessageDialog(null, "bX : " + bX + ", bY :" + bY + " , Final : " + bRet + " /  Intersection -> X : " + test.x + ", Y : " + test.y);
			
			return bRet;			
		}
		
		
		// ======================= INIT FUNCTIONS ======================= //
		
		public void init()
		{
			furnList = new ArrayList<HomePieceOfFurniture>();
			
			furnIds = new ArrayList<String>();
			furnRects = new ArrayList<float[][]>();
			furnRectsBloated = new ArrayList<float[][]>();
			
			wallIds = new ArrayList<String>();
			wallRects = new ArrayList<float[][]>();			
			wallThicks = new ArrayList<Float>();
			
			mastWallSegList = new ArrayList<List<WallSegement>>();
			masterFreeWallSegList = new ArrayList<List<WallSegement>>();
		}		
		
		public void storeAllFurnRects(Home h)
		{			
			for(HomePieceOfFurniture hp: h.getFurniture())
			{
				String fName = hp.getName();
				
				if(!fName.equals("boxred") && !fName.equals("boxgreen") )
				{
					furnList.add(hp);
					
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
				furnList.add(hpf);
				
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
		
		public Points calcFurnMids(Points p1, Points p2, float d)
		{
			Points retPoints = new Points();
			
			float l = calcDistance(p1,p2);
			float r = (float)Math.sqrt((d*d) + (0.25f*l*l));
			
			float e = (p2.x - p1.x);
			float f = (p2.y - p1.y);
			float p = (float)Math.sqrt((e*e + f*f));
			float k = (0.5f * p);
			
			float x1 = p1.x + (e*k/p) + (f/p)*((float)Math.sqrt((r*r - k*k)));
			float y1 = p1.y + (f*k/p) - (e/p)*((float)Math.sqrt((r*r - k*k)));
			
			float x2 = p1.x + (e*k/p) - (f/p)*((float)Math.sqrt((r*r - k*k)));
			float y2 = p1.y + (f*k/p) + (e/p)*((float)Math.sqrt((r*r - k*k)));
			
			// Check for inRoom
			if(room.containsPoint(x1, y1, 0.0f))
			{
				retPoints = new Points(x1, y1);
			}
			else if(room.containsPoint(x2, y2, 0.0f))
			{
				retPoints = new Points(x2, y2);
			}
			
			return retPoints;
					
			/*
			 	Let the centers be: (a,b), (c,d)
				Let the radii be: r, s
					
				  e = c - a                          [difference in x coordinates]
				  f = d - b                          [difference in y coordinates]
				  p = sqrt(e^2 + f^2)                [distance between centers]
				  k = (p^2 + r^2 - s^2)/(2p)         [distance from center 1 to line joining points of intersection]
				   
				                                      
				  x = a + ek/p + (f/p)sqrt(r^2 - k^2)
				  y = b + fk/p - (e/p)sqrt(r^2 - k^2)
				OR
				  x = a + ek/p - (f/p)sqrt(r^2 - k^2)
				  y = b + fk/p + (e/p)sqrt(r^2 - k^2)		
			*/
		}
		
		public Points getPointsAtDistOnLine(Points start, Points end, float dist)
		{
			Points newP = new Points();
			
			float len = calcDistance(start, end);
			
			float dx = (dist * (end.x - start.x) / len);
			float dy = (dist * (end.y - start.y) / len);
			
			newP = new Points((start.x + dx), (start.y + dy));
			
			return newP;
		}
	}
	
	
	
	@Override
	public PluginAction[] getActions() 
	{
		return new PluginAction [] {new RoomTestAction()}; 
	}
}
