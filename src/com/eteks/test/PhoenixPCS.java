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
		public Room livingRoom = null;

		public List<String> furnIds = new ArrayList<String>();
		public List<float[][]> furnRects = new ArrayList<float[][]>();
		public List<float[][]> furnRectsBloated = new ArrayList<float[][]>();

		public List<Float> furnElevs = new ArrayList<Float>();
		public List<Float> furnHeights = new ArrayList<Float>();
		
		public List<String> markBoxName = new ArrayList<String>();

		public int MARKBOX_COUNT = 8;
		public HomePieceOfFurniture[] markBoxes = new HomePieceOfFurniture[MARKBOX_COUNT];		

		public int markerIndx = 0;

		public List<String> wallIds = new ArrayList<String>();
		public List<float[][]> wallRects = new ArrayList<float[][]>();
		public List<Float> wallThicks = new ArrayList<Float>();

		public float FURN_TOLERANCE = 0.51f;
		public float ROOM_TOLERANCE = 0.51f;
		public float WALL_TOLERANCE = 0.1f;

		public float FURNITURE_BLOAT_SIZE = 2.0f;	// 2cm

		public float VALID_INNERWALL_TOLERANCE = 0.5f;	// 5mm

		public float SLOPE_TOLERANCE = 0.01f;
		public float INFINITY = 10000.0f; 
		public float tolerance = 0.5f; 	// 5 mm

		public boolean bShowMarkerInter = true;
		public boolean bShowMarker = true;

		public float CONV_IN_CM = 2.54f;
		public float CONV_FT_CM = (12.0f * CONV_IN_CM);

		public float VALID_RS_LENGTH = (3.0f * CONV_FT_CM);

		public float DOOR_ELEVATION = (7.0f * CONV_FT_CM);

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

			public LineSegement(WallSegement ws)
			{
				startP = ws.startP;
				endP = ws.endP;
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

				storeAllFurnParams(home);
				storeAllWallRects(home);

				markBoxes = getMarkerBoxes();

				// 1. Get inner wall segments ----------- //
				//List<WallSegement> innerWSList = getInnerWalls();

				// ================================================== //

				// 2. Calculate distance (perpendicular) of a point from a line ----------- //				
				/*
				getLivingRoom();
				float[][] livinRect = livingRoom.getPoints();

				List<WallSegement> innerWSList = getInnerWalls();

				int count = 0;

				for(int l = 0; l < livinRect.length; l++)
				{
					count++;

					Points startP = new Points(livinRect[l][0], livinRect[l][1]);
					Points endP = null;

					if(l == (livinRect.length - 1))
						endP = new Points(livinRect[0][0], livinRect[0][1]);
					else
						endP = new Points(livinRect[l+1][0], livinRect[l+1][1]);

					Points midP = new Points(((startP.x + endP.x)/2.0f),((startP.y + endP.y)/2.0f));
					LineSegement rs = new LineSegement(startP, endP);

					for(WallSegement ws : innerWSList)
					{						
						LineSegement ls = new LineSegement(ws);

						if(isParallel(rs, ls))
						{							
							float dist = calcDistancePointLine(midP, ls);

							if(dist <= VALID_INNERWALL_TOLERANCE)
							{
								putMarkers(midP, (count % MARKBOX_COUNT));

								Points midWS = new Points(((ws.startP.x + ws.endP.x)/2.0f),((ws.startP.y + ws.endP.y)/2.0f));
								putMarkers(midWS, (count % MARKBOX_COUNT));
							}
						}
					}
				}			
				 */

				// ================================================== //

				// 3. Get inner wall segments of a room (Living) ----------- //	

				getLivingRoom();
				float[][] livinRect = livingRoom.getPoints();

				List<WallSegement> innerWSList = getInnerWalls();

				getValidInnerWallSegmentsOfRoom(innerWSList, livinRect, tolerance);			

				
				// ================================================== //

			}
			catch(Exception e)
			{
				JOptionPane.showMessageDialog(null," -x-x-x- EXCEPTION : " + e.getMessage()); 
				e.printStackTrace();
			}
		}

		public List<WallSegement> getValidInnerWallSegmentsOfRoom(List<WallSegement> innerWSList, float[][] roomRect, float tolr)
		{
			List<WallSegement> validRSWallList = new ArrayList<WallSegement>();
			
			for(int l = 0; l < roomRect.length; l++)
			{					
				Points startP = new Points(roomRect[l][0], roomRect[l][1]);
				Points endP = null;

				if(l == (roomRect.length - 1))
					endP = new Points(roomRect[0][0], roomRect[0][1]);
				else
					endP = new Points(roomRect[l+1][0], roomRect[l+1][1]);

				Points midP = new Points(((startP.x + endP.x)/2.0f),((startP.y + endP.y)/2.0f));
				LineSegement rs = new LineSegement(startP, endP);

				List<WallSegement> validList = new ArrayList<WallSegement>();

				for(WallSegement ws : innerWSList)
				{						
					LineSegement ls = new LineSegement(ws);
					boolean bIsParallel = isParallel(rs, ls, tolr);

					if(bIsParallel)
					{										
						float dist = calcDistancePointLine(midP, ls, tolr);

						if(dist <= VALID_INNERWALL_TOLERANCE)
						{
							validList.add(ws);

							if(bShowMarkerInter)
							{
								// Marker
								Points midWS = new Points(((ws.startP.x + ws.endP.x)/2.0f),((ws.startP.y + ws.endP.y)/2.0f));
								putMarkers(midWS, 0);
							}

						}
					}
				}

				float lenRS = calcDistance(rs.startP, rs.endP);

				for(WallSegement ws : validList)
				{
					if(lenRS > ws.len)
					{	
						List<WallSegement> validRSWallPieceList = new ArrayList<WallSegement>();

						boolean bWSInRS1 = checkPointInBetween(ws.startP, rs.startP, rs.endP, tolr);

						//JOptionPane.showMessageDialog(null, "ws : " + ws.startP.x + ", " + ws.startP.y + " [" + ws.len + "] -> " + lenRS);

						if(bWSInRS1)
						{
							boolean bWSInRS2 = checkPointInBetween(ws.endP, rs.startP, rs.endP, tolr);

							if(bWSInRS2)
							{											
								validRSWallPieceList.add(ws);

								if(bShowMarkerInter)
								{
									// Marker
									Points midWS = new Points(((ws.startP.x + ws.endP.x)/2.0f),((ws.startP.y + ws.endP.y)/2.0f));
									putMarkers(midWS, 3);
								}
							}
							else
							{
								boolean bWSOverlapRS = checkPointInBetween(rs.endP, ws.startP, ws.endP, tolr);

								if(bWSOverlapRS)
								{
									float len = calcDistance(ws.startP, rs.endP);

									WallSegement newRS = new WallSegement(ws.startP, rs.endP, len);
									validRSWallPieceList.add(newRS);

									if(bShowMarkerInter)
									{
										// Marker
										Points midWS = new Points(((newRS.startP.x + newRS.endP.x)/2.0f),((newRS.startP.y + newRS.endP.y)/2.0f));
										putMarkers(midWS, 5);
									}
								}
							}
						}
						else
						{
							boolean bRSInWS1 = checkPointInBetween(rs.startP, ws.startP, ws.endP, tolr);

							if(bRSInWS1)
							{
								Points sP = rs.startP;

								boolean bWSInRS2 = checkPointInBetween(ws.endP, rs.startP, rs.endP, tolr);

								if(bWSInRS2)
								{
									float len = calcDistance(sP, ws.endP);

									WallSegement newRS = new WallSegement(sP, ws.endP, len);
									validRSWallPieceList.add(newRS);

									if(bShowMarkerInter)
									{
										// Marker
										Points midWS = new Points(((newRS.startP.x + newRS.endP.x)/2.0f),((newRS.startP.y + newRS.endP.y)/2.0f));
										putMarkers(midWS, 4);
									}
								}
							}
						}

						// Concatenate valid pieces of RS
						for(WallSegement rsPiece : validRSWallPieceList)
						{
							validRSWallList.add(rsPiece);

							if(bShowMarker)
							{
								// Marker
								Points midRSp = new Points(((rsPiece.startP.x + rsPiece.endP.x)/2.0f),((rsPiece.startP.y + rsPiece.endP.y)/2.0f));
								putMarkers(midRSp, 2);
							}
						}							
					}
					else
					{
						WallSegement newWS = new WallSegement(rs.startP, rs.endP, lenRS);
						validRSWallList.add(newWS);

						if(bShowMarker)
						{
							// Marker
							Points midWS = new Points(((newWS.startP.x + newWS.endP.x)/2.0f),((newWS.startP.y + newWS.endP.y)/2.0f));
							putMarkers(midWS, 1);
						}
					}
				}
			}
			
					
			if(bShowMarker)
			{			
				for(WallSegement validWS : validRSWallList)
				{
					// Marker
					Points midWS = new Points(((validWS.startP.x + validWS.endP.x)/2.0f),((validWS.startP.y + validWS.endP.y)/2.0f));
					putMarkers(midWS, 6);
				}
			}
			
			return validRSWallList;
		}
		
		public List<WallSegement> getInnerWalls()
		{
			//String wsStr = "";
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
					wallSegList.add(new WallSegement(ls.startP, ls.endP, dist));

					//wsStr += (wallIds.get(w) + " : (" + ls.startP.x + "," + ls.startP.y + ") -> (" + ls.endP.x + "," + ls.endP.y + ")\n");			
				}

				//wsStr += ("------------------\n\n");
			}

			//JOptionPane.showMessageDialog(null, wsStr);

			return wallSegList;
		}

		public List<WallSegement> calcFurnIntersectionsBelowElev(List<WallSegement> validWSList, float elv, float tolr)
		{
			//String retStr = "";
			List<WallSegement> freeWallSegList = new ArrayList<WallSegement>();

			// Compare which furn obj have elevation less than "elv"
			// Take intersection points for objects whose elevation < "elv"

			try
			{									
				//int wallCounter  = 1;

				for(WallSegement ws : validWSList)
				{
					TreeMap<Float, Intersect> interMap = new TreeMap<Float, Intersect>();

					Intersect wallS = new Intersect(ws.startP, 0.0f);
					interMap.put(0.0f, wallS);

					Intersect wallE = new Intersect(ws.endP, ws.len);
					interMap.put(ws.len, wallE);

					for(int f = 0; f < furnElevs.size(); f++)
					{
						float furnElev = furnElevs.get(f);

						if(elv >= furnElev)
						{							
							LineSegement ref = new LineSegement(ws.startP, ws.endP);								
							List<Intersect> interList = checkIntersect(ref, furnIds.get(f));

							for(Intersect inter : interList)
							{
								if(checkPointInBetween(ws.startP, ws.endP, inter.p, tolr))
								{
									interMap.put(inter.dist, inter);
									putMarkers(inter.p, markerIndx);
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
					}					

					for(int k = 1; k < inList.size();)
					{
						Intersect inter1 = inList.get(k - 1);
						Intersect inter2 = inList.get(k);

						WallSegement fws = new WallSegement(inter1.p, inter2.p, (inter2.dist - inter1.dist));
						freeWallSegList.add(fws);

						k+= 2;
					}

					//wallCounter++;
				}				

				//retStr += "-----------------------------\n";
				//JOptionPane.showMessageDialog(null, fwsStr);

			}
			catch(Exception e) 
			{
				JOptionPane.showMessageDialog(null," -x-x-x- EXCEPTION [calcFurnIntersectionsAtElev]: " + e.getMessage()); 
				e.printStackTrace();
			}

			return freeWallSegList;
		}
		// ======================= INIT FUNCTIONS ======================= //

		public void getLivingRoom()
		{			
			for(Room r : home.getRooms())
			{			
				String roomName = r.getName();

				if((roomName != null) && (roomName.equalsIgnoreCase("living")))
				{
					livingRoom = r;
					break;
				}
			}
		}

		public void init()
		{
			furnIds = new ArrayList<String>();
			furnRects = new ArrayList<float[][]>();
			furnRectsBloated = new ArrayList<float[][]>();

			wallIds = new ArrayList<String>();
			wallRects = new ArrayList<float[][]>();			
			wallThicks = new ArrayList<Float>();
		}		

		public void storeAllFurnParams(Home h)
		{			
			for(HomePieceOfFurniture hp: h.getFurniture())
			{
				String fName = hp.getName();

				if(!fName.equals("boxred") && !fName.equals("boxgreen") )
				{					
					furnIds.add(hp.getName());
					furnRects.add(hp.getPoints());
					furnElevs.add(hp.getElevation());
					furnHeights.add(hp.getHeight());
					
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

		public void storeFurnParams(HomePieceOfFurniture hpf)
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

		public List<Intersect> checkIntersect(LineSegement r, String furnId)
		{
			List<Intersect> interList = new ArrayList<Intersect>();
			
			Intersect inter = null;
			int indx = -1;
			
			if((indx = furnIds.indexOf(furnId)) > -1)
			{ 				
				float[][] fRect = furnRects.get(indx);
				//float[][] fRect = furnRectsBloated.get(indx);
						
				if(fRect.length == 2)
				{
					LineSegement l1 = new LineSegement((new Points(fRect[0][0], fRect[0][1])) , (new Points(fRect[1][0], fRect[1][1])));
					
					inter = getIntersectPoint(r, l1);				
					if(inter.dist < INFINITY)
						interList.add(inter);
					
					//debug += ("1. " + inter.p.x + "," + inter.p.y + " -> " + inter.dist + "\n");
				}
				else if(fRect.length == 4)
				{
					LineSegement l1 = new LineSegement((new Points(fRect[0][0], fRect[0][1])) , (new Points(fRect[1][0], fRect[1][1])));
					LineSegement l2 = new LineSegement((new Points(fRect[1][0], fRect[1][1])) , (new Points(fRect[2][0], fRect[2][1])));
					LineSegement l3 = new LineSegement((new Points(fRect[2][0], fRect[2][1])) , (new Points(fRect[3][0], fRect[3][1])));
					LineSegement l4 = new LineSegement((new Points(fRect[3][0], fRect[3][1])) , (new Points(fRect[0][0], fRect[0][1])));
					
					inter = getIntersectPoint(r, l1);				
					if(inter.dist < INFINITY)
						interList.add(inter);
					
					//debug += ("1. " + inter.p.x + "," + inter.p.y + " -> " + inter.dist + "\n");
					
					inter = getIntersectPoint(r, l2);				
					if(inter.dist < INFINITY)
						interList.add(inter);
					
					//debug += ("2. " + inter.p.x + "," + inter.p.y + " -> " + inter.dist + "\n");
					
					inter = getIntersectPoint(r, l3);
					if(inter.dist < INFINITY)
						interList.add(inter);
					
					//debug += ("3. " + inter.p.x + "," + inter.p.y + " -> " + inter.dist + "\n");
					
					inter = getIntersectPoint(r, l4);
					if(inter.dist < INFINITY)
						interList.add(inter);
					
					//debug += ("4. " + inter.p.x + "," + inter.p.y + " -> " + inter.dist + "\n");
				}
				//JOptionPane.showMessageDialog(null, debug);					
			}
			
			return interList;
		}
		
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
				inter.p = new Points(2*INFINITY, 2*INFINITY);
				inter.dist = INFINITY;
			}
			else
			{
				inter.p = new Points((xNum/xDen), (yNum/yDen));				
				boolean bC1 = checkPointInBetween(inter.p, l.startP, l.endP, FURN_TOLERANCE);
				
				//JOptionPane.showMessageDialog(null, bC1 + " /  Intersection -> X : " + inter.p.x + ", Y : " + inter.p.y);
				
				if(bC1)
				{		
					inter.dist = calcDistance(inter.p, ref.startP);					
				}
				else
				{
					inter.p = new Points(INFINITY, INFINITY);
					inter.dist = INFINITY;
				}
			}
			
			return inter;			
		}
		
		public float calcDistance(Points p1, Points p2)
		{
			float d = (float) Math.sqrt(((p2.x - p1.x) * (p2.x - p1.x)) + ((p2.y - p1.y) * (p2.y - p1.y)));
			return d;
		}	

		public boolean isParallel(LineSegement ls1, LineSegement ls2, float tolr)
		{
			boolean isPara = false;

			float slope1 = 0.0f;
			float slope2 = 0.0f;

			if(Math.abs(ls1.endP.x - ls1.startP.x) <= tolr)
				slope1 = INFINITY;
			else
				slope1 = ((ls1.endP.y - ls1.startP.y) / (ls1.endP.x - ls1.startP.x));

			if(Math.abs(ls2.endP.x - ls2.startP.x) <= tolr)
				slope2 = INFINITY;
			else
				slope2 = ((ls2.endP.y - ls2.startP.y) / (ls2.endP.x - ls2.startP.x));

			isPara = (Math.abs(slope1 - slope2) < SLOPE_TOLERANCE) ? true : false;

			return isPara;
		}

		public float calcDistancePointLine(Points p, LineSegement ls, float tolr)
		{
			float dist = 0.0f;

			if(Math.abs(ls.endP.x - ls.startP.x) < tolr)
			{
				dist = Math.abs(ls.endP.x - p.x);
			}
			else if(Math.abs(ls.endP.y - ls.startP.y) < tolr)
			{
				dist = Math.abs(ls.endP.y - p.y);
			}
			else
			{
				float slope = ((ls.endP.y - ls.startP.y) / (ls.endP.x - ls.startP.x));

				float A = slope;
				float B = -1.0f;
				float C = (ls.startP.y - (slope * ls.startP.x));

				dist = ( Math.abs((A*p.x) + (B*p.y) + C) / ((float)Math.sqrt((A*A) + (B*B))) );
			}

			return dist;
		}	

		public boolean checkPointInBetween(Points test, Points start, Points end, float tolPercent)
		{
			boolean bRet = false;

			float distST = calcDistance(start, test);
			float distTE = calcDistance(test, end);
			float distSE = calcDistance(start, end);

			float distSEAbs = (float)(Math.abs(distST + distTE - distSE));

			if(distSEAbs <= tolPercent)
				bRet = true;

			return bRet;			
		}

		public List<Points> sortPList(List<Points> interPList, Points ref)
		{
			List<Points> retPList = new ArrayList<Points>();
			TreeMap<Float, Points> pMap = new TreeMap<Float, Points>();

			for(Points p : interPList)
			{
				float dist = calcDistance(p, ref);
				pMap.put(dist, p);
			}

			Set<Float> keys = pMap.keySet();

			for(Float d : keys)
			{
				retPList.add(pMap.get(d));
			}

			return retPList;
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
					else if(catPOF.get(p).getName().equals("boxpurp"))
					{
						markBoxes[6] = new HomePieceOfFurniture(catPOF.get(p));
						markBoxName.add("boxpurp");
						count++;
					}
					else if(catPOF.get(p).getName().equals("boxgray"))
					{
						markBoxes[7] = new HomePieceOfFurniture(catPOF.get(p));
						markBoxName.add("boxgray");
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
