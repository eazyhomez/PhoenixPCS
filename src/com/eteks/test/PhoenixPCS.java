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
		public List<float[][]> furnRectsAccess = new ArrayList<float[][]>();
		
		public List<HomePieceOfFurniture> furnList = new ArrayList<HomePieceOfFurniture>();

		public List<String> wallIds = new ArrayList<String>();
		public List<float[][]> wallRects = new ArrayList<float[][]>();
		public List<Float> wallThicks = new ArrayList<Float>();
		
		public int MARKBOX_COUNT = 8;
		public List<String> markBoxName = new ArrayList<String>();
		public HomePieceOfFurniture[] markBoxes = new HomePieceOfFurniture[MARKBOX_COUNT];		

		public int markerIndx = 0;

		public float FURN_TOLERANCE = 0.51f;
		public float ROOM_TOLERANCE = 0.51f;
		public float WALL_TOLERANCE = 0.1f;
		public float ORIENTATION_TOLERANCE = 0.05f;
		public float SLOPE_TOLERANCE = 0.01f;
		public float tolerance = 0.5f; 	// 5 mm
		public float VALID_INNERWALL_TOLERANCE = 0.5f;	// 5mm
		
		public float FURNITURE_BLOAT_SIZE = 5.0f;	// 2cm

		public float INFINITY = 10000.0f; 

		public float CONV_IN_CM = 2.54f;
		public float CONV_FT_CM = (12.0f * CONV_IN_CM);

		public float VALID_RS_LENGTH = (3.0f * CONV_FT_CM);
		public float DOOR_ELEVATION = (7.0f * CONV_FT_CM);

		public boolean bShowMarkerInter = false;
		public boolean bShowMarker = true;
		
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

				// A. Get all FWS in room (Living) --------- //
				
				getLivingRoom();
				float[][] livinRect = livingRoom.getPoints();
				List<WallSegement> innerWSList = getInnerWalls();
				
				List<WallSegement> validWSList = getValidInnerWallSegmentsOfRoom(innerWSList, livinRect, tolerance);	
				
				List<WallSegement> fWSList = calcFreeWallIntersectionsBelowElev(validWSList, DOOR_ELEVATION, livingRoom, 1.0f);
				
				for(WallSegement freeWS : fWSList)
				{
					if(freeWS.len >= VALID_RS_LENGTH)
					{
						// Marker
						Points midWS = new Points(((freeWS.startP.x + freeWS.endP.x)/2.0f),((freeWS.startP.y + freeWS.endP.y)/2.0f));
						putMarkers(midWS, 1);
					}
				}
				
				// ================================================== //
				
				// 6. Place furniture parallel to the wall --------- //
				// 7. Get quadrant info --------- //
				// 8. Get wall angles --------- //
				// 9. Check for intersection with all furns/fixtures/walls --------- //
				// 10. Check orientation --------- //
				
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
							boolean bWSEInRS1 = checkPointInBetween(ws.endP, rs.startP, rs.endP, tolr);
									
							if(bWSEInRS1)
							{
								boolean bWSEInRS2 = checkPointInBetween(ws.startP, rs.startP, rs.endP, tolr);

								if(bWSEInRS2)
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
									boolean bWSOverlapRS = checkPointInBetween(rs.endP, ws.endP, ws.endP, tolr);

									if(bWSOverlapRS)
									{
										float len = calcDistance(ws.endP, rs.endP);

										WallSegement newRS = new WallSegement(ws.endP, rs.endP, len);
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
						}

						// Concatenate valid pieces of RS
						for(WallSegement rsPiece : validRSWallPieceList)
						{
							validRSWallList.add(rsPiece);

							if(bShowMarkerInter)
							{
								// Marker
								Points midRSp = new Points(((rsPiece.startP.x + rsPiece.endP.x)/2.0f),((rsPiece.startP.y + rsPiece.endP.y)/2.0f));
								putMarkers(midRSp, 2);
							}
						}							
					}
					else
					{
						boolean bRSSInWS = checkPointInBetween(rs.startP, ws.startP, ws.endP, tolr);
						boolean bRSEInWS = checkPointInBetween(rs.endP, ws.startP, ws.endP, tolr);
						
						if(bRSSInWS && bRSEInWS)
						{
							WallSegement newWS = new WallSegement(rs.startP, rs.endP, lenRS);
							validRSWallList.add(newWS);

							if(bShowMarkerInter)
							{
								// Marker
								Points midWS = new Points(((newWS.startP.x + newWS.endP.x)/2.0f),((newWS.startP.y + newWS.endP.y)/2.0f));
								putMarkers(midWS, 1);
							}
						}
						else if(!bRSSInWS)
						{
							boolean bWSSInRS = checkPointInBetween(ws.startP, rs.startP, rs.endP, tolr);
							
							if(bWSSInRS)
							{
								float len = calcDistance(ws.startP, rs.endP);
								
								WallSegement newWS = new WallSegement(ws.startP, rs.endP, len);
								validRSWallList.add(newWS);

								if(bShowMarkerInter)
								{
									// Marker
									Points midWS = new Points(((newWS.startP.x + newWS.endP.x)/2.0f),((newWS.startP.y + newWS.endP.y)/2.0f));
									putMarkers(midWS, 3);
								}
							}
							else
							{
								float len = calcDistance(ws.endP, rs.endP);
								
								WallSegement newWS = new WallSegement(ws.endP, rs.endP, len);
								validRSWallList.add(newWS);

								if(bShowMarkerInter)
								{
									// Marker
									Points midWS = new Points(((newWS.startP.x + newWS.endP.x)/2.0f),((newWS.startP.y + newWS.endP.y)/2.0f));
									putMarkers(midWS, 3);
								}
							}
						}
						else if(!bRSEInWS)
						{
							boolean bWSSInRS = checkPointInBetween(ws.startP, rs.startP, rs.endP, tolr);
							
							if(bWSSInRS)
							{
								float len = calcDistance(ws.startP, rs.startP);
								
								WallSegement newWS = new WallSegement(ws.startP, rs.startP, len);
								validRSWallList.add(newWS);

								if(bShowMarkerInter)
								{
									// Marker
									Points midWS = new Points(((newWS.startP.x + newWS.endP.x)/2.0f),((newWS.startP.y + newWS.endP.y)/2.0f));
									putMarkers(midWS, 3);
								}
							}
							else
							{
								float len = calcDistance(ws.endP, rs.startP);
								
								WallSegement newWS = new WallSegement(ws.endP, rs.startP, len);
								validRSWallList.add(newWS);

								if(bShowMarkerInter)
								{
									// Marker
									Points midWS = new Points(((newWS.startP.x + newWS.endP.x)/2.0f),((newWS.startP.y + newWS.endP.y)/2.0f));
									putMarkers(midWS, 3);
								}
							}
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
					//putMarkers(midWS, 6);
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

					//putMarkers(ls.startP, 6);
					//putMarkers(ls.endP, 5);
					//wsStr += (wallIds.get(w) + " : (" + ls.startP.x + "," + ls.startP.y + ") -> (" + ls.endP.x + "," + ls.endP.y + ")\n");			
				}

				//wsStr += ("------------------\n\n");
			}

			//JOptionPane.showMessageDialog(null, wsStr);

			return wallSegList;
		}

		public List<WallSegement> calcFreeWallIntersectionsBelowElev(List<WallSegement> validWSList, float elv, Room r, float tolr)
		{
			List<WallSegement> freeWallSegList = new ArrayList<WallSegement>();

			// Compare which furn obj have elevation less than "elv"
			// Take intersection points for objects whose elevation < "elv"

			try
			{
				for(WallSegement ws : validWSList)
				{
					TreeMap<Float, Intersect> interMap = new TreeMap<Float, Intersect>();

					Intersect wallS = new Intersect(ws.startP, 0.0f);
					interMap.put(0.0f, wallS);

					Intersect wallE = new Intersect(ws.endP, ws.len);
					interMap.put(ws.len, wallE);

					// Debug
					Points midPWS = new Points(((ws.startP.x + ws.endP.x)/2.0f),((ws.startP.y + ws.endP.y)/2.0f));
					putMarkers(midPWS, 5);
					
					for(int f = 0; f < furnElevs.size(); f++)
					{
						float furnElev = furnElevs.get(f);
						
						if(elv >= furnElev)
						{							
							LineSegement ref = new LineSegement(ws.startP, ws.endP);								
							List<Intersect> interList = checkIntersect(ref, furnIds.get(f));
							
							int interCount = 0;
							
							for(Intersect inter : interList)
							{
								//if(r.containsPoint(inter.p.x, inter.p.y, tolr))
								if(checkPointInBetween(inter.p, ws.startP, ws.endP, tolr))
								{			
									interCount++;
									
									interMap.put(inter.dist, inter);
									
									//if(bShowMarkerInter)
										putMarkers(inter.p, 3);
								}
							}
							
							if(interCount == 1)
							{
								float X = furnList.get(f).getX();
								float Y = furnList.get(f).getY();
								
								Points midP = new Points(X, Y);
								
								float calcDS = calcDistance(midP, ws.startP);
								float calcDE = calcDistance(midP, ws.endP);
								
								Intersect inter;

								//if((calcDS <= calcDE) && (calcDS <= tolr))
								if(calcDS <= calcDE)
								{
									inter = new Intersect(ws.startP, 0.5f);
									interMap.put(inter.dist, inter);
									
									//if(bShowMarkerInter)
										putMarkers(inter.p, 4);
								}
								//else if(calcDE <= tolr)
								else
								{
									inter = new Intersect(ws.endP, (ws.len - 0.5f));
									interMap.put(inter.dist, inter);
									
									//if(bShowMarkerInter)
										putMarkers(inter.p, 4);
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
						
						if(bShowMarker)
						{
							//putMarkers(fws.startP, 1);
							//putMarkers(fws.endP, 0);
						}
					}
				}
			}
			catch(Exception e) 
			{
				JOptionPane.showMessageDialog(null," -x-x-x- EXCEPTION [calcFreeWallIntersectionsBelowElev]: " + e.getMessage()); 
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

				if(!markBoxName.contains(fName))
				{					
					furnIds.add(hp.getName());
					furnRects.add(hp.getPoints());
					furnElevs.add(hp.getElevation());
					furnHeights.add(hp.getHeight());
					furnList.add(hp);
					
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

		public void placeFurnParallelToWall(LineSegement ws, HomePieceOfFurniture furn, Points furnCoords)
		{
			FurnLoc furnLoc = new FurnLoc();
			float furnAngle =  calcWallAngles(ws);
			
			furnLoc.w = furn.getWidth();
			furnLoc.ang = furnAngle;			
			furnLoc.p = furnCoords;	
			
			placeFurnItem(furn, furnLoc);
		}
		
		public void placeFurnItem(HomePieceOfFurniture inFurn, FurnLoc fLoc)
		{
			HomePieceOfFurniture outFurn = inFurn;
			outFurn.setName(inFurn.getName());
			outFurn.setWidth(fLoc.w);
			outFurn.setAngle(fLoc.ang);
			outFurn.setX(fLoc.p.x);
			outFurn.setY(fLoc.p.y);
			
			home.addPieceOfFurniture(outFurn);
		}
		
		public float calcWallAngles(LineSegement ws)
		{
			float retAngle = 0.0f;
			
			float wsAngle =  (float) Math.atan((Math.abs(ws.endP.y - ws.startP.y)) / (Math.abs(ws.endP.x - ws.startP.x))); 
			
			Points p = new Points((ws.startP.x - ws.endP.x), (ws.startP.y - ws.endP.y));
			int qIndx = getQuadrantInfo(p);
			
			if(qIndx == 1)
				retAngle = wsAngle;
			else if(qIndx == 2)
				retAngle = (float)(Math.PI) - wsAngle;
			else if(qIndx == 3)
				retAngle = (float)(Math.PI) + wsAngle;
			else if(qIndx == 4)
				retAngle = (float)(2.0f*Math.PI) - wsAngle;
			
			return retAngle;
		}
		
		public int getQuadrantInfo(Points p)
		{
			int qIndx = 0;
			
			if((p.x > 0.0f) && (p.y > 0.0f))
				qIndx = 1;
			else if((p.x < 0.0f) && (p.y > 0.0f))
				qIndx = 2;
			else if((p.x < 0.0f) && (p.y < 0.0f))
				qIndx = 3;
			else if((p.x > 0.0f) && (p.y < 0.0f))
				qIndx = 4;
			
			return qIndx;
		}
		
		public void chkFurnOrient(HomePieceOfFurniture furn, LineSegement ws)
		{			
			float[][] fRect = furn.getPoints();
			Points furnBottMid = new Points(((fRect[2][0] + fRect[3][0]) / 2),  ((fRect[2][1] + fRect[3][1]) / 2));
			
			Points wsMid = new Points(((ws.startP.x + ws.endP.x) / 2),  ((ws.startP.y + ws.endP.y) / 2));
			
			float dist = calcDistance(furnBottMid, wsMid);
			//JOptionPane.showMessageDialog(null, "dist : " + dist);
			
			if(dist > ORIENTATION_TOLERANCE)
			{
				furn.setAngle((float)Math.PI);
				//JOptionPane.showMessageDialog(null, "180 rotation");
			}
		}
		
		public boolean checkIntersectWithAllFurns(HomePieceOfFurniture hpf, boolean bAddAccessibility)
		{
			boolean bIntersects = false;
			
			for(int x = 0 ; x < furnIds.size(); x++)
			{
				if(!hpf.getName().equalsIgnoreCase(furnIds.get(x)) && !(furnIds.get(x).toLowerCase().startsWith("diningrect")))
				{	
					float[][] refFurnRect = furnRects.get(x);
					
					for(int f = 0; f < refFurnRect.length; f++)
					{
						Points startLine = new Points(refFurnRect[f][0], refFurnRect[f][1]);
						
						Points endLine = null;
						
						if(f == (refFurnRect.length - 1))
							endLine = new Points(refFurnRect[0][0], refFurnRect[0][1]);
						else
							endLine = new Points(refFurnRect[f+1][0], refFurnRect[f+1][1]);				
						
						LineSegement ls = new LineSegement(startLine, endLine);
						
						// For Accessibility check
						List<Intersect> interList = new ArrayList<Intersect>();
						
						if(bAddAccessibility)
							interList = checkIntersectAccessibility(ls, hpf.getName());
						else
							interList = checkIntersect(ls, hpf.getName());
						
						for(Intersect inter : interList)
						{
							if(inter != null)
							{
								bIntersects = checkPointInBetween(inter.p, ls.startP, ls.endP, FURN_TOLERANCE);
								
								if(bIntersects)
									break;
							}
							//putMarkers(inter.p, 3);
						}
					}
					
					if(bIntersects)
						break;
				}
				
				if(bIntersects)
					break;
			}
			
			return bIntersects;
		}
		
		public List<Intersect> checkIntersectAccessibility(LineSegement r, String furnId)
		{
			List<Intersect> interList = new ArrayList<Intersect>();
			
			Intersect inter = null;
			int indx = -1;
			
			if((indx = furnIds.indexOf(furnId)) > -1)
			{ 				
				float[][] fRect = furnRectsAccess.get(indx);
						
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
		
		public List<Intersect> checkIntersect(LineSegement r, String furnId)
		{
			List<Intersect> interList = new ArrayList<Intersect>();
			
			Intersect inter = null;
			int indx = -1;
			
			if((indx = furnIds.indexOf(furnId)) > -1)
			{ 				
				//float[][] fRect = furnRects.get(indx);
				float[][] fRect = furnRectsBloated.get(indx);
						
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

		public boolean checkPointInBetween2(Points test, Points start, Points end, float tolPercent)
		{
			boolean bRet = false;

			float distST = calcDistance(start, test);
			float distTE = calcDistance(test, end);
			float distSE = calcDistance(start, end);

			float distSEAbs = (float)(Math.abs(distST + distTE - distSE));
			float perc = Math.abs(distSEAbs / distSE);
			
			if(perc <= tolPercent)
				bRet = true;
			
			JOptionPane.showMessageDialog(null, bRet + " -> " + perc + ", " + tolPercent);
			
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
