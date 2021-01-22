package org.mtransit.parser.ca_haut_st_laurent_cithsl_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mtransit.parser.Constants.EMPTY;

// https://exo.quebec/en/about/open-data
// https://exo.quebec/xdata/cithsl/google_transit.zip
public class HautStLaurentCITHSLBusAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-haut-st-laurent-cithsl-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new HautStLaurentCITHSLBusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating CITHSL bus data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating CITHSL bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern P1METRO = Pattern.compile("(\\(métro )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final String P1METRO_REPLACEMENT = "\\(";

	private static final Pattern SECTEUR_ = Pattern.compile("(secteur[s]? )", Pattern.CASE_INSENSITIVE);

	private static final Pattern DASH_DES = Pattern.compile("(- de[s]? )", Pattern.CASE_INSENSITIVE);
	private static final String DASH_DES_REPLACEMENT = "- ";

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongNameOrDefault();
		routeLongName = CleanUtils.SAINT.matcher(routeLongName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		routeLongName = CleanUtils.POINT.matcher(routeLongName).replaceAll(CleanUtils.POINT_REPLACEMENT);
		routeLongName = CleanUtils.CLEAN_ET.matcher(routeLongName).replaceAll(CleanUtils.CLEAN_ET_REPLACEMENT);
		routeLongName = P1METRO.matcher(routeLongName).replaceAll(P1METRO_REPLACEMENT);
		routeLongName = SECTEUR_.matcher(routeLongName).replaceAll(EMPTY);
		routeLongName = DASH_DES.matcher(routeLongName).replaceAll(DASH_DES_REPLACEMENT);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR = "1F1F1F"; // DARK GRAY (from GTFS)

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String T = "T";

	private static final long RID_STARTS_WITH_T = 20_000L;

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		final String rsn = gRoute.getRouteShortName();
		if (!Utils.isDigitsOnly(rsn)) {
			Matcher matcher = DIGITS.matcher(rsn);
			if (matcher.find()) {
				int digits = Integer.parseInt(matcher.group());
				if (rsn.startsWith(T)) {
					return RID_STARTS_WITH_T + digits;
				}
			}
			throw new MTLog.Fatal("Unexpected route ID for %s!", gRoute);
		}
		return Long.parseLong(rsn);
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		mTrip.setHeadsignString(
				cleanTripHeadsign(gTrip.getTripHeadsignOrDefault()),
				gTrip.getDirectionIdOrDefault()
		);
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern DIRECTION_ = Pattern.compile("(direction )", Pattern.CASE_INSENSITIVE);

	private static final Pattern VIA_STE_DASH_ = Pattern.compile("((ste-)+)", Pattern.CASE_INSENSITIVE);
	private static final String VIA_STE_DASH_REPLACEMENT = "Ste ";

	private static final Pattern STARTS_WITH_VIA_DASH_ = Pattern.compile("(([^-]+-)+([^-]+)$)", Pattern.CASE_INSENSITIVE);
	private static final String STARTS_WITH_VIA_DASH_REPLACEMENT = "$3";

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = DIRECTION_.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = SECTEUR_.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = VIA_STE_DASH_.matcher(tripHeadsign).replaceAll(VIA_STE_DASH_REPLACEMENT);
		tripHeadsign = STARTS_WITH_VIA_DASH_.matcher(tripHeadsign).replaceAll(STARTS_WITH_VIA_DASH_REPLACEMENT);
		tripHeadsign = CleanUtils.POINT.matcher(tripHeadsign).replaceAll(CleanUtils.POINT_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return CleanUtils.cleanLabelFR(tripHeadsign);
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		throw new MTLog.Fatal("Unexpected trips to merge %s & %s!", mTrip, mTripToMerge);
	}

	private static final Pattern STATION_DE_METRO = Pattern.compile("(station de métro )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final String STATION_DE_METRO_REPLACEMENT = "station ";

	private static final Pattern START_WITH_FACE_A = Pattern.compile("^(face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final Pattern START_WITH_FACE_AU = Pattern.compile("^(face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE = Pattern.compile("^(face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern SPACE_FACE_A = Pattern.compile("( face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final Pattern SPACE_WITH_FACE_AU = Pattern.compile("( face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE = Pattern.compile("( face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern[] START_WITH_FACES = new Pattern[]{START_WITH_FACE_A, START_WITH_FACE_AU, START_WITH_FACE};

	private static final Pattern[] SPACE_FACES = new Pattern[]{SPACE_FACE_A, SPACE_WITH_FACE_AU, SPACE_WITH_FACE};

	private static final Pattern AVENUE = Pattern.compile("( avenue)", Pattern.CASE_INSENSITIVE);
	private static final String AVENUE_REPLACEMENT = " av.";

	private static final Pattern BOULEVARD = Pattern.compile("( boulevard)", Pattern.CASE_INSENSITIVE);
	private static final String BOULEVARD_REPLACEMENT = " boul.";

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = STATION_DE_METRO.matcher(gStopName).replaceAll(STATION_DE_METRO_REPLACEMENT);
		gStopName = AVENUE.matcher(gStopName).replaceAll(AVENUE_REPLACEMENT);
		gStopName = BOULEVARD.matcher(gStopName).replaceAll(BOULEVARD_REPLACEMENT);
		gStopName = Utils.replaceAll(gStopName, START_WITH_FACES, CleanUtils.SPACE);
		gStopName = Utils.replaceAll(gStopName, SPACE_FACES, CleanUtils.SPACE);
		return CleanUtils.cleanLabelFR(gStopName);
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public int getStopId(@NotNull GStop gStop) {
		String stopCode = getStopCode(gStop);
		if (stopCode.length() > 0) {
			return Integer.parseInt(stopCode); // using stop code as stop ID
		}
		//noinspection deprecation
		final String stopId1 = gStop.getStopId();
		Matcher matcher = DIGITS.matcher(stopId1);
		if (matcher.find()) {
			int digits = Integer.parseInt(matcher.group());
			int stopId;
			if (stopId1.startsWith("LSL")) {
				stopId = 100_000;
			} else if (stopId1.startsWith("CHT")) {
				stopId = 200_000;
			} else if (stopId1.startsWith("GOD")) {
				stopId = 300_000;
			} else if (stopId1.startsWith("HOW")) {
				stopId = 400_000;
			} else if (stopId1.startsWith("HUN")) {
				stopId = 500_000;
			} else if (stopId1.startsWith("KAH")) {
				stopId = 600_000;
			} else if (stopId1.startsWith("MER")) {
				stopId = 700_000;
			} else if (stopId1.startsWith("MTL")) {
				stopId = 800_000;
			} else if (stopId1.startsWith("ORM")) {
				stopId = 900_000;
			} else if (stopId1.startsWith("SMN")) {
				stopId = 1_000_000;
			} else if (stopId1.startsWith("SPC")) {
				stopId = 1_100_000;
			} else if (stopId1.startsWith("TSS")) {
				stopId = 1_200_000;
			} else {
				throw new MTLog.Fatal("Stop doesn't have an ID (start with)! " + gStop);
			}
			if (stopId1.endsWith("A")) {
				stopId += 1_000;
			} else if (stopId1.endsWith("B")) {
				stopId += 2_000;
			} else if (stopId1.endsWith("C")) {
				stopId += 3_000;
			} else if (stopId1.endsWith("D")) {
				stopId += 4_000;
			} else {
				throw new MTLog.Fatal("Stop doesn't have an ID (end with)! " + gStop);
			}
			return stopId + digits;
		}
		throw new MTLog.Fatal("Unexpected stop ID for %s!", gStop);
	}

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		if ("0".equals(gStop.getStopCode())) {
			return null;
		}
		return super.getStopCode(gStop);
	}

}
