package org.mtransit.parser.ca_haut_st_laurent_cithsl_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.Utils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTripStop;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.mt.data.MTrip;

// https://exo.quebec/en/about/open-data
// https://exo.quebec/xdata/cithsl/google_transit.zip
public class HautStLaurentCITHSLBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-haut-st-laurent-cithsl-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new HautStLaurentCITHSLBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		MTLog.log("Generating CITHSL bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		MTLog.log("Generating CITHSL bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern P1METRO = Pattern.compile("(\\(métro )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final String P1METRO_REPLACEMENT = "\\(";

	private static final Pattern SECTEUR = Pattern.compile("(secteur[s]? )", Pattern.CASE_INSENSITIVE);
	private static final String SECTEUR_REPLACEMENT = "";

	private static final Pattern DASH_DES = Pattern.compile("(\\- de[s]? )", Pattern.CASE_INSENSITIVE);
	private static final String DASH_DES_REPLACEMENT = "- ";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = CleanUtils.SAINT.matcher(routeLongName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		routeLongName = CleanUtils.POINT.matcher(routeLongName).replaceAll(CleanUtils.POINT_REPLACEMENT);
		routeLongName = CleanUtils.CLEAN_ET.matcher(routeLongName).replaceAll(CleanUtils.CLEAN_ET_REPLACEMENT);
		routeLongName = P1METRO.matcher(routeLongName).replaceAll(P1METRO_REPLACEMENT);
		routeLongName = SECTEUR.matcher(routeLongName).replaceAll(SECTEUR_REPLACEMENT);
		routeLongName = DASH_DES.matcher(routeLongName).replaceAll(DASH_DES_REPLACEMENT);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR = "1F1F1F"; // DARK GRAY (from GTFS)

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		// https://exo.quebec/fr/planifier-trajet/autobus/CITHSL/140/
		// https://exo.quebec/Media/Default/pdf/section4/Horaires-bus/haut-saint-laurent-horaire-140.pdf
		map2.put(140L, new RouteTripSpec(140L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Châteauguay", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Mercier") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
						"79132", // Hébert / Saint-Jean-Baptiste #Mercier
								"79152", // ++ Des Ormes / des Noyers
								"79023", // == Mercier (route 138 / Josime-Pelletier)
								"79182", // <> !=
								"79194", // == <> Clinique Médicale Mercier
								"79024", // != Parc Mercier (rue Côté / route 138)
								"79174", // != Beaupré / Beaulac
								"79184", // <> Centre régional de Châteauguay
								"79185", // <> ++ Anjou / Maple
								"79186", // <> CLSC de Châteauguay
								"79187", // <> Hôpital Anna-Laberge
								"79191", // <> ++ École secondaire Louis-Philippe-Paré
								"79188" // <> Wal-Mart Châteauguay
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
						"79184", // <> Centre régional de Châteauguay
								"79185", // <> ++ Anjou / Maple
								"79186", // <> CLSC de Châteauguay
								"79187", // <> Hôpital Anna-Laberge
								"79191", // <> ++ École secondaire Louis-Philippe-Paré
								"79188", // <> Wal-Mart Châteauguay
								"79051", // !== Parc Mercier (rue Beauchemin / rue Beaupré)
								"79178", // !== Batiscan / Édouard-Laberge
								"79192", // !== Édouard-Laberge / Henri-Ladouceur
								"79193", // !== Édouard-Laberge / Beaupré
								"79180", // != == Édouard-Laberge / Beauchemin
								"79181", // ++ Beauchemin / Beloeil
								"79049", // == !=
								"79182", // != <>
								"79183", // != Josime-Pelletier / Saint-Jean-Baptiste #Mercier =>
								"79194", // != <> Clinique Médicale Mercier
								"79182", // <>
								"79057", // !=
								"79132" // != Hébert / Saint-Jean-Baptiste #Mercier =>
						)) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		mTrip.setHeadsignString(
			cleanTripHeadsign(gTrip.getTripHeadsign()),
			gTrip.getDirectionIdOrDefault()
		);
	}

	private static final Pattern DIRECTION = Pattern.compile("(direction )", Pattern.CASE_INSENSITIVE);
	private static final String DIRECTION_REPLACEMENT = "";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = DIRECTION.matcher(tripHeadsign).replaceAll(DIRECTION_REPLACEMENT);
		tripHeadsign = SECTEUR.matcher(tripHeadsign).replaceAll(SECTEUR_REPLACEMENT);
		tripHeadsign = CleanUtils.POINT.matcher(tripHeadsign).replaceAll(CleanUtils.POINT_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return CleanUtils.cleanLabelFR(tripHeadsign);
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 1L) {
			if (Arrays.asList( //
					"Ste-Martine", //
					"Ormstown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Ormstown", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 111L) {
			if (Arrays.asList( //
					"Mercier-Ste-Martine", //
					"Ste-Martine", //
					"Mercier-Ste-Martine-Howick-Ormstown", //
					"Ormstown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Ormstown", mTrip.getHeadsignId());
				return true;
			}
		}
		throw new MTLog.Fatal("Unepected trips to merge %s & %s!", mTrip, mTripToMerge);
	}

	private static final Pattern STATION_DE_METRO = Pattern.compile("(station de métro )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final String STATION_DE_METRO_REPLACEMENT = "station ";

	private static final Pattern START_WITH_FACE_A = Pattern.compile("^(face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final Pattern START_WITH_FACE_AU = Pattern.compile("^(face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE = Pattern.compile("^(face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern SPACE_FACE_A = Pattern.compile("( face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final Pattern SPACE_WITH_FACE_AU = Pattern.compile("( face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE = Pattern.compile("( face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern[] START_WITH_FACES = new Pattern[] { START_WITH_FACE_A, START_WITH_FACE_AU, START_WITH_FACE };

	private static final Pattern[] SPACE_FACES = new Pattern[] { SPACE_FACE_A, SPACE_WITH_FACE_AU, SPACE_WITH_FACE };

	private static final Pattern AVENUE = Pattern.compile("( avenue)", Pattern.CASE_INSENSITIVE);
	private static final String AVENUE_REPLACEMENT = " av.";

	private static final Pattern BOULEVARD = Pattern.compile("( boulevard)", Pattern.CASE_INSENSITIVE);
	private static final String BOULEVARD_REPLACEMENT = " boul.";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = STATION_DE_METRO.matcher(gStopName).replaceAll(STATION_DE_METRO_REPLACEMENT);
		gStopName = AVENUE.matcher(gStopName).replaceAll(AVENUE_REPLACEMENT);
		gStopName = BOULEVARD.matcher(gStopName).replaceAll(BOULEVARD_REPLACEMENT);
		gStopName = Utils.replaceAll(gStopName, START_WITH_FACES, CleanUtils.SPACE);
		gStopName = Utils.replaceAll(gStopName, SPACE_FACES, CleanUtils.SPACE);
		return CleanUtils.cleanLabelFR(gStopName);
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public int getStopId(GStop gStop) {
		String stopCode = getStopCode(gStop);
		if (stopCode != null && stopCode.length() > 0) {
			return Integer.valueOf(stopCode); // using stop code as stop ID
		}
		Matcher matcher = DIGITS.matcher(gStop.getStopId());
		if (matcher.find()) {
			int digits = Integer.parseInt(matcher.group());
			int stopId;
			if (gStop.getStopId().startsWith("LSL")) {
				stopId = 100000;
			} else if (gStop.getStopId().startsWith("CHT")) {
				stopId = 200000;
			} else if (gStop.getStopId().startsWith("GOD")) {
				stopId = 300000;
			} else if (gStop.getStopId().startsWith("HOW")) {
				stopId = 400000;
			} else if (gStop.getStopId().startsWith("HUN")) {
				stopId = 500000;
			} else if (gStop.getStopId().startsWith("KAH")) {
				stopId = 600000;
			} else if (gStop.getStopId().startsWith("MER")) {
				stopId = 700000;
			} else if (gStop.getStopId().startsWith("MTL")) {
				stopId = 800000;
			} else if (gStop.getStopId().startsWith("ORM")) {
				stopId = 900000;
			} else if (gStop.getStopId().startsWith("SMN")) {
				stopId = 1000000;
			} else if (gStop.getStopId().startsWith("SPC")) {
				stopId = 1100000;
			} else if (gStop.getStopId().startsWith("TSS")) {
				stopId = 1200000;
			} else {
				throw new MTLog.Fatal("Stop doesn't have an ID (start with)! " + gStop);
			}
			if (gStop.getStopId().endsWith("A")) {
				stopId += 1000;
			} else if (gStop.getStopId().endsWith("B")) {
				stopId += 2000;
			} else if (gStop.getStopId().endsWith("C")) {
				stopId += 3000;
			} else if (gStop.getStopId().endsWith("D")) {
				stopId += 4000;
			} else {
				throw new MTLog.Fatal("Stop doesn't have an ID (end with)! " + gStop);
			}
			return stopId + digits;
		}
		throw new MTLog.Fatal("Unexpected stop ID for %s!", gStop);
	}

	@Override
	public String getStopCode(GStop gStop) {
		if ("0".equals(gStop.getStopCode())) {
			return null;
		}
		return super.getStopCode(gStop);
	}

}
