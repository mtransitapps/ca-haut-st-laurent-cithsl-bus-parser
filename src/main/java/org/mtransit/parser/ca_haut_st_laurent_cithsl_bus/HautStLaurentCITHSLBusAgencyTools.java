package org.mtransit.parser.ca_haut_st_laurent_cithsl_bus;

import static org.mtransit.commons.Constants.EMPTY;
import static org.mtransit.commons.RegexUtils.DIGITS;

import org.jetbrains.annotations.NotNull;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.RegexUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.mt.data.MAgency;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://exo.quebec/en/about/open-data
public class HautStLaurentCITHSLBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new HautStLaurentCITHSLBusAgencyTools().start(args);
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "exo HSL";
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern P1METRO = Pattern.compile("(\\(métro )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final String P1METRO_REPLACEMENT = "\\(";

	private static final Pattern SECTEUR_ = Pattern.compile("(secteurs? )", Pattern.CASE_INSENSITIVE);

	private static final Pattern DASH_DES = Pattern.compile("(- des? )", Pattern.CASE_INSENSITIVE);
	private static final String DASH_DES_REPLACEMENT = "- ";

	@Override
	public boolean defaultRouteLongNameEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String routeLongName) {
		routeLongName = CleanUtils.SAINT.matcher(routeLongName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		routeLongName = CleanUtils.POINT.matcher(routeLongName).replaceAll(CleanUtils.POINT_REPLACEMENT);
		routeLongName = CleanUtils.CLEAN_ET.matcher(routeLongName).replaceAll(CleanUtils.CLEAN_ET_REPLACEMENT);
		routeLongName = P1METRO.matcher(routeLongName).replaceAll(P1METRO_REPLACEMENT);
		routeLongName = SECTEUR_.matcher(routeLongName).replaceAll(EMPTY);
		routeLongName = DASH_DES.matcher(routeLongName).replaceAll(DASH_DES_REPLACEMENT);
		return CleanUtils.cleanLabel(routeLongName);
	}

	@Override
	public boolean defaultAgencyColorEnabled() {
		return true;
	}

	@Override
	public boolean defaultRouteIdEnabled() {
		return true;
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return true;
	}

	@NotNull
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		return gRoute.getRouteShortName(); // used by GTFS-RT
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern VIA_STE_DASH_ = Pattern.compile("((ste-)+)", Pattern.CASE_INSENSITIVE);
	private static final String VIA_STE_DASH_REPLACEMENT = "Ste ";

	private static final Pattern STARTS_WITH_VIA_DASH_ = Pattern.compile("(([^-]+-)+([^-]+)$)", Pattern.CASE_INSENSITIVE);
	private static final String STARTS_WITH_VIA_DASH_REPLACEMENT = "$3";

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.keepToFR(tripHeadsign);
		tripHeadsign = SECTEUR_.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = VIA_STE_DASH_.matcher(tripHeadsign).replaceAll(VIA_STE_DASH_REPLACEMENT);
		tripHeadsign = STARTS_WITH_VIA_DASH_.matcher(tripHeadsign).replaceAll(STARTS_WITH_VIA_DASH_REPLACEMENT);
		tripHeadsign = CleanUtils.POINT.matcher(tripHeadsign).replaceAll(CleanUtils.POINT_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return CleanUtils.cleanLabelFR(tripHeadsign);
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

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = STATION_DE_METRO.matcher(gStopName).replaceAll(STATION_DE_METRO_REPLACEMENT);
		gStopName = RegexUtils.replaceAllNN(gStopName, START_WITH_FACES, CleanUtils.SPACE);
		gStopName = RegexUtils.replaceAllNN(gStopName, SPACE_FACES, CleanUtils.SPACE);
		gStopName = CleanUtils.cleanStreetTypesFRCA(gStopName);
		return CleanUtils.cleanLabelFR(gStopName);
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		final String stopCode = getStopCode(gStop);
		if (stopCode.length() > 0) {
			return Integer.parseInt(stopCode); // using stop code as stop ID
		}
		//noinspection deprecation
		final String stopId1 = gStop.getStopId();
		final Matcher matcher = DIGITS.matcher(stopId1);
		if (matcher.find()) {
			final int digits = Integer.parseInt(matcher.group());
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
			return EMPTY;
		}
		//noinspection deprecation
		return gStop.getStopId(); // used by GTFS-RT
	}
}
