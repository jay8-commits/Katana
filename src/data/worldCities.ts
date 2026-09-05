export interface CityRecord {
  id: string;
  city: string;
  country: string;
  countryCode: string;
  region: string;
  latitude: number;
  longitude: number;
  timezone: string;
}

export const WORLD_CITIES: CityRecord[] = [
  // 1. North America
  { id: 'na_nyc', city: 'New York', country: 'United States', countryCode: 'US', region: 'North America', latitude: 40.7128, longitude: -74.0060, timezone: 'America/New_York' },
  { id: 'na_lax', city: 'Los Angeles', country: 'United States', countryCode: 'US', region: 'North America', latitude: 34.0522, longitude: -118.2437, timezone: 'America/Los_Angeles' },
  { id: 'na_chi', city: 'Chicago', country: 'United States', countryCode: 'US', region: 'North America', latitude: 41.8781, longitude: -87.6298, timezone: 'America/Chicago' },
  { id: 'na_tor', city: 'Toronto', country: 'Canada', countryCode: 'CA', region: 'North America', latitude: 43.6532, longitude: -79.3832, timezone: 'America/Toronto' },
  { id: 'na_van', city: 'Vancouver', country: 'Canada', countryCode: 'CA', region: 'North America', latitude: 49.2827, longitude: -123.1207, timezone: 'America/Vancouver' },
  { id: 'na_mex', city: 'Mexico City', country: 'Mexico', countryCode: 'MX', region: 'North America', latitude: 19.4326, longitude: -99.1332, timezone: 'America/Mexico_City' },
  { id: 'na_mon', city: 'Montreal', country: 'Canada', countryCode: 'CA', region: 'North America', latitude: 45.5017, longitude: -73.5673, timezone: 'America/Toronto' },
  { id: 'na_gua', city: 'Guadalajara', country: 'Mexico', countryCode: 'MX', region: 'North America', latitude: 20.6597, longitude: -103.3496, timezone: 'America/Mexico_City' },

  // 2. South America
  { id: 'sa_sao', city: 'São Paulo', country: 'Brazil', countryCode: 'BR', region: 'South America', latitude: -23.5505, longitude: -46.6333, timezone: 'America/Sao_Paulo' },
  { id: 'sa_rio', city: 'Rio de Janeiro', country: 'Brazil', countryCode: 'BR', region: 'South America', latitude: -22.9068, longitude: -43.1729, timezone: 'America/Sao_Paulo' },
  { id: 'sa_bue', city: 'Buenos Aires', country: 'Argentina', countryCode: 'AR', region: 'South America', latitude: -34.6037, longitude: -58.3816, timezone: 'America/Argentina/Buenos_Aires' },
  { id: 'sa_bog', city: 'Bogotá', country: 'Colombia', countryCode: 'CO', region: 'South America', latitude: 4.7110, longitude: -74.0721, timezone: 'America/Bogota' },
  { id: 'sa_lim', city: 'Lima', country: 'Peru', countryCode: 'PE', region: 'South America', latitude: -12.0464, longitude: -77.0428, timezone: 'America/Lima' },
  { id: 'sa_san', city: 'Santiago', country: 'Chile', countryCode: 'CL', region: 'South America', latitude: -33.4489, longitude: -70.6693, timezone: 'America/Santiago' },
  { id: 'sa_car', city: 'Caracas', country: 'Venezuela', countryCode: 'VE', region: 'South America', latitude: 10.4806, longitude: -66.9036, timezone: 'America/Caracas' },
  { id: 'sa_qui', city: 'Quito', country: 'Ecuador', countryCode: 'EC', region: 'South America', latitude: -0.1807, longitude: -78.4678, timezone: 'America/Guayaquil' },
  { id: 'sa_mon', city: 'Montevideo', country: 'Uruguay', countryCode: 'UY', region: 'South America', latitude: -34.9011, longitude: -56.1645, timezone: 'America/Montevideo' },

  // 3. Europe
  { id: 'eu_lon', city: 'London', country: 'United Kingdom', countryCode: 'GB', region: 'Europe', latitude: 51.5074, longitude: -0.1278, timezone: 'Europe/London' },
  { id: 'eu_par', city: 'Paris', country: 'France', countryCode: 'FR', region: 'Europe', latitude: 48.8566, longitude: 2.3522, timezone: 'Europe/Paris' },
  { id: 'eu_ber', city: 'Berlin', country: 'Germany', countryCode: 'DE', region: 'Europe', latitude: 52.5200, longitude: 13.4050, timezone: 'Europe/Berlin' },
  { id: 'eu_rom', city: 'Rome', country: 'Italy', countryCode: 'IT', region: 'Europe', latitude: 41.9028, longitude: 12.4964, timezone: 'Europe/Rome' },
  { id: 'eu_mad', city: 'Madrid', country: 'Spain', countryCode: 'ES', region: 'Europe', latitude: 40.4168, longitude: -3.7038, timezone: 'Europe/Madrid' },
  { id: 'eu_ams', city: 'Amsterdam', country: 'Netherlands', countryCode: 'NL', region: 'Europe', latitude: 52.3676, longitude: 4.9041, timezone: 'Europe/Amsterdam' },
  { id: 'eu_sto', city: 'Stockholm', country: 'Sweden', countryCode: 'SE', region: 'Europe', latitude: 59.3293, longitude: 18.0686, timezone: 'Europe/Stockholm' },
  { id: 'eu_zur', city: 'Zurich', country: 'Switzerland', countryCode: 'CH', region: 'Europe', latitude: 47.3769, longitude: 8.5417, timezone: 'Europe/Zurich' },
  { id: 'eu_vie', city: 'Vienna', country: 'Austria', countryCode: 'AT', region: 'Europe', latitude: 48.2082, longitude: 16.3738, timezone: 'Europe/Vienna' },
  { id: 'eu_dub', city: 'Dublin', country: 'Ireland', countryCode: 'IE', region: 'Europe', latitude: 53.3498, longitude: -6.2603, timezone: 'Europe/Dublin' },
  { id: 'eu_lis', city: 'Lisbon', country: 'Portugal', countryCode: 'PT', region: 'Europe', latitude: 38.7223, longitude: -9.1393, timezone: 'Europe/Lisbon' },
  { id: 'eu_war', city: 'Warsaw', country: 'Poland', countryCode: 'PL', region: 'Europe', latitude: 52.2297, longitude: 21.0122, timezone: 'Europe/Warsaw' },

  // 4. Asia
  { id: 'as_tok', city: 'Tokyo', country: 'Japan', countryCode: 'JP', region: 'Asia', latitude: 35.6762, longitude: 139.6503, timezone: 'Asia/Tokyo' },
  { id: 'as_osa', city: 'Osaka', country: 'Japan', countryCode: 'JP', region: 'Asia', latitude: 34.6937, longitude: 135.5023, timezone: 'Asia/Tokyo' },
  { id: 'as_seo', city: 'Seoul', country: 'South Korea', countryCode: 'KR', region: 'Asia', latitude: 37.5665, longitude: 126.9780, timezone: 'Asia/Seoul' },
  { id: 'as_bei', city: 'Beijing', country: 'China', countryCode: 'CN', region: 'Asia', latitude: 39.9042, longitude: 116.4074, timezone: 'Asia/Shanghai' },
  { id: 'as_sha', city: 'Shanghai', country: 'China', countryCode: 'CN', region: 'Asia', latitude: 31.2304, longitude: 121.4737, timezone: 'Asia/Shanghai' },
  { id: 'as_sin', city: 'Singapore', country: 'Singapore', countryCode: 'SG', region: 'Asia', latitude: 1.3521, longitude: 103.8198, timezone: 'Asia/Singapore' },
  { id: 'as_mnl', city: 'Manila', country: 'Philippines', countryCode: 'PH', region: 'Asia', latitude: 14.5995, longitude: 120.9842, timezone: 'Asia/Manila' },
  { id: 'as_bkk', city: 'Bangkok', country: 'Thailand', countryCode: 'TH', region: 'Asia', latitude: 13.7563, longitude: 100.5018, timezone: 'Asia/Bangkok' },
  { id: 'as_mum', city: 'Mumbai', country: 'India', countryCode: 'IN', region: 'Asia', latitude: 19.0760, longitude: 72.8777, timezone: 'Asia/Kolkata' },
  { id: 'as_del', city: 'Delhi', country: 'India', countryCode: 'IN', region: 'Asia', latitude: 28.6139, longitude: 77.2090, timezone: 'Asia/Kolkata' },
  { id: 'as_jak', city: 'Jakarta', country: 'Indonesia', countryCode: 'ID', region: 'Asia', latitude: -6.2088, longitude: 106.8456, timezone: 'Asia/Jakarta' },
  { id: 'as_hcm', city: 'Ho Chi Minh City', country: 'Vietnam', countryCode: 'VN', region: 'Asia', latitude: 10.8231, longitude: 106.6297, timezone: 'Asia/Ho_Chi_Minh' },
  { id: 'as_kul', city: 'Kuala Lumpur', country: 'Malaysia', countryCode: 'MY', region: 'Asia', latitude: 3.1390, longitude: 101.6869, timezone: 'Asia/Kuala_Lumpur' },
  { id: 'as_tpe', city: 'Taipei', country: 'Taiwan', countryCode: 'TW', region: 'Asia', latitude: 25.0330, longitude: 121.5654, timezone: 'Asia/Taipei' },

  // 5. Africa
  { id: 'af_cai', city: 'Cairo', country: 'Egypt', countryCode: 'EG', region: 'Africa', latitude: 30.0444, longitude: 31.2357, timezone: 'Africa/Cairo' },
  { id: 'af_lag', city: 'Lagos', country: 'Nigeria', countryCode: 'NG', region: 'Africa', latitude: 6.5244, longitude: 3.3792, timezone: 'Africa/Lagos' },
  { id: 'af_nai', city: 'Nairobi', country: 'Kenya', countryCode: 'KE', region: 'Africa', latitude: -1.2921, longitude: 36.8219, timezone: 'Africa/Nairobi' },
  { id: 'af_jnb', city: 'Johannesburg', country: 'South Africa', countryCode: 'ZA', region: 'Africa', latitude: -26.2041, longitude: 28.0473, timezone: 'Africa/Johannesburg' },
  { id: 'af_cpt', city: 'Cape Town', country: 'South Africa', countryCode: 'ZA', region: 'Africa', latitude: -33.9249, longitude: 18.4241, timezone: 'Africa/Johannesburg' },
  { id: 'af_cas', city: 'Casablanca', country: 'Morocco', countryCode: 'MA', region: 'Africa', latitude: 33.5731, longitude: -7.5898, timezone: 'Africa/Casablanca' },
  { id: 'af_add', city: 'Addis Ababa', country: 'Ethiopia', countryCode: 'ET', region: 'Africa', latitude: 9.0320, longitude: 38.7469, timezone: 'Africa/Addis_Ababa' },
  { id: 'af_acc', city: 'Accra', country: 'Ghana', countryCode: 'GH', region: 'Africa', latitude: 5.6037, longitude: -0.1870, timezone: 'Africa/Accra' },
  { id: 'af_tun', city: 'Tunis', country: 'Tunisia', countryCode: 'TN', region: 'Africa', latitude: 36.8065, longitude: 10.1815, timezone: 'Africa/Tunis' },

  // 6. Oceania
  { id: 'oc_syd', city: 'Sydney', country: 'Australia', countryCode: 'AU', region: 'Oceania', latitude: -33.8688, longitude: 151.2093, timezone: 'Australia/Sydney' },
  { id: 'oc_mel', city: 'Melbourne', country: 'Australia', countryCode: 'AU', region: 'Oceania', latitude: -37.8136, longitude: 144.9631, timezone: 'Australia/Melbourne' },
  { id: 'oc_bri', city: 'Brisbane', country: 'Australia', countryCode: 'AU', region: 'Oceania', latitude: -27.4698, longitude: 153.0251, timezone: 'Australia/Brisbane' },
  { id: 'oc_per', city: 'Perth', country: 'Australia', countryCode: 'AU', region: 'Oceania', latitude: -31.9505, longitude: 115.8605, timezone: 'Australia/Perth' },
  { id: 'oc_akl', city: 'Auckland', country: 'New Zealand', countryCode: 'NZ', region: 'Oceania', latitude: -36.8485, longitude: 174.7633, timezone: 'Pacific/Auckland' },
  { id: 'oc_wlg', city: 'Wellington', country: 'New Zealand', countryCode: 'NZ', region: 'Oceania', latitude: -41.2865, longitude: 174.7762, timezone: 'Pacific/Auckland' },
  { id: 'oc_suv', city: 'Suva', country: 'Fiji', countryCode: 'FJ', region: 'Oceania', latitude: -18.1416, longitude: 178.4419, timezone: 'Pacific/Fiji' },

  // 7. Middle East
  { id: 'me_dxb', city: 'Dubai', country: 'United Arab Emirates', countryCode: 'AE', region: 'Middle East', latitude: 25.2048, longitude: 55.2708, timezone: 'Asia/Dubai' },
  { id: 'me_ruh', city: 'Riyadh', country: 'Saudi Arabia', countryCode: 'SA', region: 'Middle East', latitude: 24.7136, longitude: 46.6753, timezone: 'Asia/Riyadh' },
  { id: 'me_doh', city: 'Doha', country: 'Qatar', countryCode: 'QA', region: 'Middle East', latitude: 25.2854, longitude: 51.5310, timezone: 'Asia/Qatar' },
  { id: 'me_tlv', city: 'Tel Aviv', country: 'Israel', countryCode: 'IL', region: 'Middle East', latitude: 32.0853, longitude: 34.7818, timezone: 'Asia/Jerusalem' },
  { id: 'me_ist', city: 'Istanbul', country: 'Turkey', countryCode: 'TR', region: 'Middle East', latitude: 41.0082, longitude: 28.9784, timezone: 'Europe/Istanbul' },
  { id: 'me_mct', city: 'Muscat', country: 'Oman', countryCode: 'OM', region: 'Middle East', latitude: 23.5880, longitude: 58.3829, timezone: 'Asia/Muscat' },
  { id: 'me_kwi', city: 'Kuwait City', country: 'Kuwait', countryCode: 'KW', region: 'Middle East', latitude: 29.3759, longitude: 47.9774, timezone: 'Asia/Kuwait' },
  { id: 'me_amm', city: 'Amman', country: 'Jordan', countryCode: 'JO', region: 'Middle East', latitude: 31.9454, longitude: 35.9284, timezone: 'Asia/Amman' },

  // 8. Caribbean
  { id: 'cb_hav', city: 'Havana', country: 'Cuba', countryCode: 'CU', region: 'Caribbean', latitude: 23.1136, longitude: -82.3666, timezone: 'America/Havana' },
  { id: 'cb_kin', city: 'Kingston', country: 'Jamaica', countryCode: 'JM', region: 'Caribbean', latitude: 17.9712, longitude: -76.7936, timezone: 'America/Jamaica' },
  { id: 'cb_sju', city: 'San Juan', country: 'Puerto Rico', countryCode: 'PR', region: 'Caribbean', latitude: 18.4655, longitude: -66.1057, timezone: 'America/Puerto_Rico' },
  { id: 'cb_sdq', city: 'Santo Domingo', country: 'Dominican Republic', countryCode: 'DO', region: 'Caribbean', latitude: 18.4861, longitude: -69.9312, timezone: 'America/Santo_Domingo' },
  { id: 'cb_nas', city: 'Nassau', country: 'Bahamas', countryCode: 'BS', region: 'Caribbean', latitude: 25.0480, longitude: -77.3554, timezone: 'America/Nassau' },
  { id: 'cb_bgi', city: 'Bridgetown', country: 'Barbados', countryCode: 'BB', region: 'Caribbean', latitude: 13.0975, longitude: -59.6165, timezone: 'America/Barbados' },
  { id: 'cb_pos', city: 'Port of Spain', country: 'Trinidad and Tobago', countryCode: 'TT', region: 'Caribbean', latitude: 10.6549, longitude: -61.5019, timezone: 'America/Port_of_Spain' },

  // 9. Central America
  { id: 'ca_pty', city: 'Panama City', country: 'Panama', countryCode: 'PA', region: 'Central America', latitude: 8.9824, longitude: -79.5199, timezone: 'America/Panama' },
  { id: 'ca_sjo', city: 'San José', country: 'Costa Rica', countryCode: 'CR', region: 'Central America', latitude: 9.9281, longitude: -84.0907, timezone: 'America/Costa_Rica' },
  { id: 'ca_gua', city: 'Guatemala City', country: 'Guatemala', countryCode: 'GT', region: 'Central America', latitude: 14.6349, longitude: -90.5069, timezone: 'America/Guatemala' },
  { id: 'ca_sal', city: 'San Salvador', country: 'El Salvador', countryCode: 'SV', region: 'Central America', latitude: 13.6929, longitude: -89.2182, timezone: 'America/El_Salvador' },
  { id: 'ca_teg', city: 'Tegucigalpa', country: 'Honduras', countryCode: 'HN', region: 'Central America', latitude: 14.0723, longitude: -87.1921, timezone: 'America/Tegucigalpa' },
  { id: 'ca_mga', city: 'Managua', country: 'Nicaragua', countryCode: 'NI', region: 'Central America', latitude: 12.1364, longitude: -86.2514, timezone: 'America/Managua' },
  { id: 'ca_bze', city: 'Belize City', country: 'Belize', countryCode: 'BZ', region: 'Central America', latitude: 17.5046, longitude: -88.1962, timezone: 'America/Belize' }
];

export const REQUIRED_REGIONS = [
  'North America',
  'South America',
  'Europe',
  'Asia',
  'Africa',
  'Oceania',
  'Middle East',
  'Caribbean',
  'Central America'
] as const;
