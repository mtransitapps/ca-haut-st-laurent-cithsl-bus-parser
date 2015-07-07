package org.mtransit.parser.ca_haut_st_laurent_cithsl_bus;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.mt.data.MTrip;

// https://www.amt.qc.ca/en/about/open-data
// http://www.amt.qc.ca/xdata/cithsl/google_transit.zip
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
		System.out.printf("\nGenerating CITHSL bus data...\n");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating CITHSL bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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

	private static final Pattern P1METRO = Pattern.compile("(\\(métro )", Pattern.CASE_INSENSITIVE);
	private static final String P1METRO_REPLACEMENT = "\\(";

	private static final Pattern SECTEUR = Pattern.compile("(secteur[s]? )", Pattern.CASE_INSENSITIVE);
	private static final String SECTEUR_REPLACEMENT = "";

	private static final Pattern DASH_DES = Pattern.compile("(\\- de[s]? )", Pattern.CASE_INSENSITIVE);
	private static final String DASH_DES_REPLACEMENT = "- ";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.route_long_name;
		routeLongName = CleanUtils.SAINT.matcher(routeLongName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		routeLongName = CleanUtils.POINT.matcher(routeLongName).replaceAll(CleanUtils.POINT_REPLACEMENT);
		routeLongName = CleanUtils.ET.matcher(routeLongName).replaceAll(CleanUtils.ET_REPLACEMENT);
		routeLongName = P1METRO.matcher(routeLongName).replaceAll(P1METRO_REPLACEMENT);
		routeLongName = SECTEUR.matcher(routeLongName).replaceAll(SECTEUR_REPLACEMENT);
		routeLongName = DASH_DES.matcher(routeLongName).replaceAll(DASH_DES_REPLACEMENT);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR = "40B54D";

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.trip_headsign), gTrip.direction_id);
	}

	private static final Pattern DIRECTION = Pattern.compile("(direction )", Pattern.CASE_INSENSITIVE);
	private static final String DIRECTION_REPLACEMENT = "";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = DIRECTION.matcher(tripHeadsign).replaceAll(DIRECTION_REPLACEMENT);
		tripHeadsign = SECTEUR.matcher(tripHeadsign).replaceAll(SECTEUR_REPLACEMENT);
		tripHeadsign = CleanUtils.POINT.matcher(tripHeadsign).replaceAll(CleanUtils.POINT_REPLACEMENT);
		return CleanUtils.cleanLabelFR(tripHeadsign);
	}

	private static final Pattern STATION_DE_METRO = Pattern.compile("(station de métro )", Pattern.CASE_INSENSITIVE);
	private static final String STATION_DE_METRO_REPLACEMENT = "station ";

	private static final Pattern START_WITH_FACE_A = Pattern.compile("^(face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE_AU = Pattern.compile("^(face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE = Pattern.compile("^(face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern SPACE_FACE_A = Pattern.compile("( face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
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
		return super.cleanStopNameFR(gStopName);
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public int getStopId(GStop gStop) {
		String stopCode = getStopCode(gStop);
		if (stopCode != null && stopCode.length() > 0) {
			return Integer.valueOf(stopCode); // using stop code as stop ID
		}
		Matcher matcher = DIGITS.matcher(gStop.stop_id);
		matcher.find();
		int digits = Integer.parseInt(matcher.group());
		int stopId;
		if (gStop.stop_id.startsWith("LSL")) {
			stopId = 100000;
		} else if (gStop.stop_id.startsWith("CHT")) {
			stopId = 200000;
		} else if (gStop.stop_id.startsWith("GOD")) {
			stopId = 300000;
		} else if (gStop.stop_id.startsWith("HOW")) {
			stopId = 400000;
		} else if (gStop.stop_id.startsWith("HUN")) {
			stopId = 500000;
		} else if (gStop.stop_id.startsWith("KAH")) {
			stopId = 600000;
		} else if (gStop.stop_id.startsWith("MER")) {
			stopId = 700000;
		} else if (gStop.stop_id.startsWith("MTL")) {
			stopId = 800000;
		} else if (gStop.stop_id.startsWith("ORM")) {
			stopId = 900000;
		} else if (gStop.stop_id.startsWith("SMN")) {
			stopId = 1000000;
		} else if (gStop.stop_id.startsWith("SPC")) {
			stopId = 1100000;
		} else if (gStop.stop_id.startsWith("TSS")) {
			stopId = 1200000;
		} else {
			System.out.println("Stop doesn't have an ID (start with)! " + gStop);
			System.exit(-1);
			stopId = -1;
		}
		if (gStop.stop_id.endsWith("A")) {
			stopId += 1000;
		} else if (gStop.stop_id.endsWith("B")) {
			stopId += 2000;
		} else if (gStop.stop_id.endsWith("C")) {
			stopId += 3000;
		} else if (gStop.stop_id.endsWith("D")) {
			stopId += 4000;
		} else {
			System.out.println("Stop doesn't have an ID (end with)! " + gStop);
			System.exit(-1);
		}
		return stopId + digits;
	}

	@Override
	public String getStopCode(GStop gStop) {
		if ("0".equals(gStop.stop_code)) {
			return null;
		}
		return super.getStopCode(gStop);
	}

}
