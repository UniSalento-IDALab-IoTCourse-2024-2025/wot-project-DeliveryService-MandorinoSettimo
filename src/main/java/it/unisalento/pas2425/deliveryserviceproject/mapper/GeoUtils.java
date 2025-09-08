package it.unisalento.pas2425.deliveryserviceproject.mapper;

public final class GeoUtils {
    private static final double R = 6371000.0;

    public static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*
                        Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    /** 0..360 (0=N) */
    public static double bearingDegrees(double lat1, double lon1, double lat2, double lon2) {
        double φ1 = Math.toRadians(lat1), φ2 = Math.toRadians(lat2);
        double λ1 = Math.toRadians(lon1), λ2 = Math.toRadians(lon2);
        double y = Math.sin(λ2-λ1)*Math.cos(φ2);
        double x = Math.cos(φ1)*Math.sin(φ2) - Math.sin(φ1)*Math.cos(φ2)*Math.cos(λ2-λ1);
        double deg = Math.toDegrees(Math.atan2(y, x));
        return (deg + 360) % 360;
    }

    /** km/h da due punti con timestamp ISO8601 */
    public static Double speedKmh(String t1Iso, double lat1, double lon1, String t2Iso, double lat2, double lon2) {
        long t1 = java.time.Instant.parse(t1Iso).toEpochMilli();
        long t2 = java.time.Instant.parse(t2Iso).toEpochMilli();
        if (t2 <= t1) return null;
        double distM = haversineMeters(lat1, lon1, lat2, lon2);
        double sec = (t2 - t1)/1000.0;
        return (distM / sec) * 3.6;
    }
}

