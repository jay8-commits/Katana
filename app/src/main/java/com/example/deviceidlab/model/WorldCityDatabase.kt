package com.example.deviceidlab.model

import java.io.Serializable
import kotlin.random.Random

data class CityRecord(
    val id: String,
    val city: String,
    val country: String,
    val countryCode: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String
) : Serializable

object WorldCityDatabase {

    val REQUIRED_REGIONS = listOf(
        "North America",
        "South America",
        "Europe",
        "Asia",
        "Africa",
        "Oceania",
        "Middle East",
        "Caribbean",
        "Central America"
    )

    val CITIES = listOf(
        // 1. North America
        CityRecord("na_nyc", "New York", "United States", "US", "North America", 40.7128, -74.0060, "America/New_York"),
        CityRecord("na_lax", "Los Angeles", "United States", "US", "North America", 34.0522, -118.2437, "America/Los_Angeles"),
        CityRecord("na_chi", "Chicago", "United States", "US", "North America", 41.8781, -87.6298, "America/Chicago"),
        CityRecord("na_tor", "Toronto", "Canada", "CA", "North America", 43.6532, -79.3832, "America/Toronto"),
        CityRecord("na_van", "Vancouver", "Canada", "CA", "North America", 49.2827, -123.1207, "America/Vancouver"),
        CityRecord("na_mex", "Mexico City", "Mexico", "MX", "North America", 19.4326, -99.1332, "America/Mexico_City"),
        CityRecord("na_mon", "Montreal", "Canada", "CA", "North America", 45.5017, -73.5673, "America/Toronto"),
        CityRecord("na_gua", "Guadalajara", "Mexico", "MX", "North America", 20.6597, -103.3496, "America/Mexico_City"),

        // 2. South America
        CityRecord("sa_sao", "São Paulo", "Brazil", "BR", "South America", -23.5505, -46.6333, "America/Sao_Paulo"),
        CityRecord("sa_rio", "Rio de Janeiro", "Brazil", "BR", "South America", -22.9068, -43.1729, "America/Sao_Paulo"),
        CityRecord("sa_bue", "Buenos Aires", "Argentina", "AR", "South America", -34.6037, -58.3816, "America/Argentina/Buenos_Aires"),
        CityRecord("sa_bog", "Bogotá", "Colombia", "CO", "South America", 4.7110, -74.0721, "America/Bogota"),
        CityRecord("sa_lim", "Lima", "Peru", "PE", "South America", -12.0464, -77.0428, "America/Lima"),
        CityRecord("sa_san", "Santiago", "Chile", "CL", "South America", -33.4489, -70.6693, "America/Santiago"),
        CityRecord("sa_car", "Caracas", "Venezuela", "VE", "South America", 10.4806, -66.9036, "America/Caracas"),
        CityRecord("sa_qui", "Quito", "Ecuador", "EC", "South America", -0.1807, -78.4678, "America/Guayaquil"),
        CityRecord("sa_mon", "Montevideo", "Uruguay", "UY", "South America", -34.9011, -56.1645, "America/Montevideo"),

        // 3. Europe
        CityRecord("eu_lon", "London", "United Kingdom", "GB", "Europe", 51.5074, -0.1278, "Europe/London"),
        CityRecord("eu_par", "Paris", "France", "FR", "Europe", 48.8566, 2.3522, "Europe/Paris"),
        CityRecord("eu_ber", "Berlin", "Germany", "DE", "Europe", 52.5200, 13.4050, "Europe/Berlin"),
        CityRecord("eu_rom", "Rome", "Italy", "IT", "Europe", 41.9028, 12.4964, "Europe/Rome"),
        CityRecord("eu_mad", "Madrid", "Spain", "ES", "Europe", 40.4168, -3.7038, "Europe/Madrid"),
        CityRecord("eu_ams", "Amsterdam", "Netherlands", "NL", "Europe", 52.3676, 4.9041, "Europe/Amsterdam"),
        CityRecord("eu_sto", "Stockholm", "Sweden", "SE", "Europe", 59.3293, 18.0686, "Europe/Stockholm"),
        CityRecord("eu_zur", "Zurich", "Switzerland", "CH", "Europe", 47.3769, 8.5417, "Europe/Zurich"),
        CityRecord("eu_vie", "Vienna", "Austria", "AT", "Europe", 48.2082, 16.3738, "Europe/Vienna"),
        CityRecord("eu_dub", "Dublin", "Ireland", "IE", "Europe", 53.3498, -6.2603, "Europe/Dublin"),
        CityRecord("eu_lis", "Lisbon", "Portugal", "PT", "Europe", 38.7223, -9.1393, "Europe/Lisbon"),
        CityRecord("eu_war", "Warsaw", "Poland", "PL", "Europe", 52.2297, 21.0122, "Europe/Warsaw"),

        // 4. Asia
        CityRecord("as_tok", "Tokyo", "Japan", "JP", "Asia", 35.6762, 139.6503, "Asia/Tokyo"),
        CityRecord("as_osa", "Osaka", "Japan", "JP", "Asia", 34.6937, 135.5023, "Asia/Tokyo"),
        CityRecord("as_seo", "Seoul", "South Korea", "KR", "Asia", 37.5665, 126.9780, "Asia/Seoul"),
        CityRecord("as_bei", "Beijing", "China", "CN", "Asia", 39.9042, 116.4074, "Asia/Shanghai"),
        CityRecord("as_sha", "Shanghai", "China", "CN", "Asia", 31.2304, 121.4737, "Asia/Shanghai"),
        CityRecord("as_sin", "Singapore", "Singapore", "SG", "Asia", 1.3521, 103.8198, "Asia/Singapore"),
        CityRecord("as_mnl", "Manila", "Philippines", "PH", "Asia", 14.5995, 120.9842, "Asia/Manila"),
        CityRecord("as_bkk", "Bangkok", "Thailand", "TH", "Asia", 13.7563, 100.5018, "Asia/Bangkok"),
        CityRecord("as_mum", "Mumbai", "India", "IN", "Asia", 19.0760, 72.8777, "Asia/Kolkata"),
        CityRecord("as_del", "Delhi", "India", "IN", "Asia", 28.6139, 77.2090, "Asia/Kolkata"),
        CityRecord("as_jak", "Jakarta", "Indonesia", "ID", "Asia", -6.2088, 106.8456, "Asia/Jakarta"),
        CityRecord("as_hcm", "Ho Chi Minh City", "Vietnam", "VN", "Asia", 10.8231, 106.6297, "Asia/Ho_Chi_Minh"),
        CityRecord("as_kul", "Kuala Lumpur", "Malaysia", "MY", "Asia", 3.1390, 101.6869, "Asia/Kuala_Lumpur"),
        CityRecord("as_tpe", "Taipei", "Taiwan", "TW", "Asia", 25.0330, 121.5654, "Asia/Taipei"),

        // 5. Africa
        CityRecord("af_cai", "Cairo", "Egypt", "EG", "Africa", 30.0444, 31.2357, "Africa/Cairo"),
        CityRecord("af_lag", "Lagos", "Nigeria", "NG", "Africa", 6.5244, 3.3792, "Africa/Lagos"),
        CityRecord("af_nai", "Nairobi", "Kenya", "KE", "Africa", -1.2921, 36.8219, "Africa/Nairobi"),
        CityRecord("af_jnb", "Johannesburg", "South Africa", "ZA", "Africa", -26.2041, 28.0473, "Africa/Johannesburg"),
        CityRecord("af_cpt", "Cape Town", "South Africa", "ZA", "Africa", -33.9249, 18.4241, "Africa/Johannesburg"),
        CityRecord("af_cas", "Casablanca", "Morocco", "MA", "Africa", 33.5731, -7.5898, "Africa/Casablanca"),
        CityRecord("af_add", "Addis Ababa", "Ethiopia", "ET", "Africa", 9.0320, 38.7469, "Africa/Addis_Ababa"),
        CityRecord("af_acc", "Accra", "Ghana", "GH", "Africa", 5.6037, -0.1870, "Africa/Accra"),
        CityRecord("af_tun", "Tunis", "Tunisia", "TN", "Africa", 36.8065, 10.1815, "Africa/Tunis"),

        // 6. Oceania
        CityRecord("oc_syd", "Sydney", "Australia", "AU", "Oceania", -33.8688, 151.2093, "Australia/Sydney"),
        CityRecord("oc_mel", "Melbourne", "Australia", "AU", "Oceania", -37.8136, 144.9631, "Australia/Melbourne"),
        CityRecord("oc_bri", "Brisbane", "Australia", "AU", "Oceania", -27.4698, 153.0251, "Australia/Brisbane"),
        CityRecord("oc_per", "Perth", "Australia", "AU", "Oceania", -31.9505, 115.8605, "Australia/Perth"),
        CityRecord("oc_akl", "Auckland", "New Zealand", "NZ", "Oceania", -36.8485, 174.7633, "Pacific/Auckland"),
        CityRecord("oc_wlg", "Wellington", "New Zealand", "NZ", "Oceania", -41.2865, 174.7762, "Pacific/Auckland"),
        CityRecord("oc_suv", "Suva", "Fiji", "FJ", "Oceania", -18.1416, 178.4419, "Pacific/Fiji"),

        // 7. Middle East
        CityRecord("me_dxb", "Dubai", "United Arab Emirates", "AE", "Middle East", 25.2048, 55.2708, "Asia/Dubai"),
        CityRecord("me_ruh", "Riyadh", "Saudi Arabia", "SA", "Middle East", 24.7136, 46.6753, "Asia/Riyadh"),
        CityRecord("me_doh", "Doha", "Qatar", "QA", "Middle East", 25.2854, 51.5310, "Asia/Qatar"),
        CityRecord("me_tlv", "Tel Aviv", "Israel", "IL", "Middle East", 32.0853, 34.7818, "Asia/Jerusalem"),
        CityRecord("me_ist", "Istanbul", "Turkey", "TR", "Middle East", 41.0082, 28.9784, "Europe/Istanbul"),
        CityRecord("me_mct", "Muscat", "Oman", "OM", "Middle East", 23.5880, 58.3829, "Asia/Muscat"),
        CityRecord("me_kwi", "Kuwait City", "Kuwait", "KW", "Middle East", 29.3759, 47.9774, "Asia/Kuwait"),
        CityRecord("me_amm", "Amman", "Jordan", "JO", "Middle East", 31.9454, 35.9284, "Asia/Amman"),

        // 8. Caribbean
        CityRecord("cb_hav", "Havana", "Cuba", "CU", "Caribbean", 23.1136, -82.3666, "America/Havana"),
        CityRecord("cb_kin", "Kingston", "Jamaica", "JM", "Caribbean", 17.9712, -76.7936, "America/Jamaica"),
        CityRecord("cb_sju", "San Juan", "Puerto Rico", "PR", "Caribbean", 18.4655, -66.1057, "America/Puerto_Rico"),
        CityRecord("cb_sdq", "Santo Domingo", "Dominican Republic", "DO", "Caribbean", 18.4861, -69.9312, "America/Santo_Domingo"),
        CityRecord("cb_nas", "Nassau", "Bahamas", "BS", "Caribbean", 25.0480, -77.3554, "America/Nassau"),
        CityRecord("cb_bgi", "Bridgetown", "Barbados", "BB", "Caribbean", 13.0975, -59.6165, "America/Barbados"),
        CityRecord("cb_pos", "Port of Spain", "Trinidad and Tobago", "TT", "Caribbean", 10.6549, -61.5019, "America/Port_of_Spain"),

        // 9. Central America
        CityRecord("ca_pty", "Panama City", "Panama", "PA", "Central America", 8.9824, -79.5199, "America/Panama"),
        CityRecord("ca_sjo", "San José", "Costa Rica", "CR", "Central America", 9.9281, -84.0907, "America/Costa_Rica"),
        CityRecord("ca_gua", "Guatemala City", "Guatemala", "GT", "Central America", 14.6349, -90.5069, "America/Guatemala"),
        CityRecord("ca_sal", "San Salvador", "El Salvador", "SV", "Central America", 13.6929, -89.2182, "America/El_Salvador"),
        CityRecord("ca_teg", "Tegucigalpa", "Honduras", "HN", "Central America", 14.0723, -87.1921, "America/Tegucigalpa"),
        CityRecord("ca_mga", "Managua", "Nicaragua", "NI", "Central America", 12.1364, -86.2514, "America/Managua"),
        CityRecord("ca_bze", "Belize City", "Belize", "BZ", "Central America", 17.5046, -88.1962, "America/Belize")
    )

    fun getAllCities(): List<CityRecord> = CITIES

    fun getAllCountries(): List<String> = CITIES.map { it.country }.distinct().sorted()

    fun getAllRegions(): List<String> = CITIES.map { it.region }.distinct().sorted()

    fun getCitiesInCountry(country: String): List<CityRecord> =
        CITIES.filter { it.country.equals(country, ignoreCase = true) }

    fun getCitiesInRegion(region: String): List<CityRecord> =
        CITIES.filter { it.region.equals(region, ignoreCase = true) }

    fun findById(id: String): CityRecord? =
        CITIES.firstOrNull { it.id.equals(id, ignoreCase = true) }

    fun findByCityAndCountry(city: String, country: String): CityRecord? =
        CITIES.firstOrNull { it.city.equals(city, ignoreCase = true) && it.country.equals(country, ignoreCase = true) }

    fun getRandomCity(seed: Long? = null): CityRecord {
        val rand = if (seed != null) Random(seed) else Random.Default
        val index = rand.nextInt(CITIES.size)
        return CITIES[index]
    }

    fun getRandomCityInCountry(country: String, seed: Long? = null): CityRecord? {
        val inCountry = getCitiesInCountry(country)
        if (inCountry.isEmpty()) return null
        val rand = if (seed != null) Random(seed) else Random.Default
        val index = rand.nextInt(inCountry.size)
        return inCountry[index]
    }
}
