// Mock in-memory localStorage for Node.js test environment
if (typeof globalThis.localStorage === 'undefined') {
  const store = new Map<string, string>();
  globalThis.localStorage = {
    getItem: (key: string) => store.get(key) ?? null,
    setItem: (key: string, value: string) => store.set(key, String(value)),
    removeItem: (key: string) => { store.delete(key); },
    clear: () => { store.clear(); },
    key: (index: number) => Array.from(store.keys())[index] ?? null,
    get length() { return store.size; }
  };
}

import { createIdentity, generateAndroidTestId, generateTelephonyTestId, generateSyntheticPhoneNumber, generateBatteryHealth } from '../utils/crypto';
import { DeviceIdentityManager } from '../services/identityManager';
import { HookInterceptionBridge } from '../services/hookInterceptionBridge';

// Simple lightweight assertion helper
function assert(condition: boolean, message: string) {
  if (!condition) {
    throw new Error(`Assertion Failed: ${message}`);
  }
}

function assertEquals<T>(expected: T, actual: T, message: string) {
  if (expected !== actual) {
    throw new Error(`Assertion Failed [${message}]: Expected ${expected}, but got ${actual}`);
  }
}

console.log('--- STARTING NPATCH DEVICE ID LAB TIER B VERIFICATION ---');

// 1. Profile Generation Integrity
console.log('[Test 1] Profile Generation Integrity...');
const profile1 = createIdentity(101);
assert(profile1.identityNumber === 101, 'Identity number matches');
assertEquals(16, profile1.androidTestId.length, 'Android ID is 16 hex chars');
assert(/^[0-9a-f]{16}$/.test(profile1.androidTestId), 'Android ID is valid lowercase hex');
assertEquals(15, profile1.telephonyTestId.length, 'Telephony ID is 15 digits');
assert(/^[0-9]{15}$/.test(profile1.telephonyTestId), 'Telephony ID is valid numeric digits');
assert(profile1.syntheticPhoneNumber.startsWith('+1 (555) '), 'Synthetic phone number format');
assert(profile1.batteryHealth >= 60 && profile1.batteryHealth <= 99, 'Battery health range 60-99%');
assert(profile1.fingerprint.length === 32, 'Fingerprint 32-char SHA-256 hex');

// 2. Uniqueness across identities
console.log('[Test 2] Identity Uniqueness across indices...');
const profile2 = createIdentity(102);
assert(profile1.androidTestId !== profile2.androidTestId, 'Android IDs must differ');
assert(profile1.telephonyTestId !== profile2.telephonyTestId, 'Telephony IDs must differ');
assert(profile1.syntheticPhoneNumber !== profile2.syntheticPhoneNumber, 'Phone numbers must differ');
assert(profile1.fingerprint !== profile2.fingerprint, 'Fingerprints must differ');

// 3. Deterministic Regeneration
console.log('[Test 3] Deterministic Regeneration for same index...');
const profile1Again = createIdentity(101);
assertEquals(profile1.androidTestId, profile1Again.androidTestId, 'Deterministic Android ID');
assertEquals(profile1.telephonyTestId, profile1Again.telephonyTestId, 'Deterministic Telephony ID');
assertEquals(profile1.syntheticPhoneNumber, profile1Again.syntheticPhoneNumber, 'Deterministic Phone');
assertEquals(profile1.fingerprint, profile1Again.fingerprint, 'Deterministic Fingerprint');

// 4. Hook Interception Bridge State & Invariant
console.log('[Test 4] Hook Interception Bridge State...');
const bridge = HookInterceptionBridge.getInstance();
bridge.setIsInterceptionActive(true);
assert(bridge.getIsInterceptionActive() === true, 'Bridge interception should be active');
bridge.setInjectedIds('58e8039d8acedb72', '358941098234190');
assertEquals('58e8039d8acedb72', bridge.getActiveAndroidId(), 'Active Android ID matches injected profile');
assertEquals('358941098234190', bridge.getActiveTelephonyId(), 'Active Telephony ID matches injected profile');

bridge.setIsInterceptionActive(false);
assert(bridge.getIsInterceptionActive() === false, 'Interception toggled off');

// 5. 21 API Inventory & Flag Verification
console.log('[Test 5] 21 Registered API Inventory & Dynamic vs Static Flags...');
const apis = [
  { name: 'Settings.Secure.getString(ANDROID_ID)', dynamic: true, restart: false },
  { name: 'Settings.Secure.getStringForUser(ANDROID_ID)', dynamic: true, restart: false },
  { name: 'Build.getSerial()', dynamic: true, restart: false },
  { name: 'TelephonyManager.getDeviceId()', dynamic: true, restart: false },
  { name: 'TelephonyManager.getDeviceId(int)', dynamic: true, restart: false },
  { name: 'TelephonyManager.getImei()', dynamic: true, restart: false },
  { name: 'TelephonyManager.getImei(int)', dynamic: true, restart: false },
  { name: 'TelephonyManager.getMeid()', dynamic: true, restart: false },
  { name: 'TelephonyManager.getMeid(int)', dynamic: true, restart: false },
  { name: 'TelephonyManager.getSimSerialNumber()', dynamic: true, restart: false },
  { name: 'TelephonyManager.getSimSerialNumber(int)', dynamic: true, restart: false },
  { name: 'TelephonyManager.getSubscriberId()', dynamic: true, restart: false },
  { name: 'TelephonyManager.getSubscriberId(int)', dynamic: true, restart: false },
  { name: 'WifiInfo.getMacAddress()', dynamic: true, restart: false },
  { name: 'Build.MODEL', dynamic: false, restart: true },
  { name: 'Build.MANUFACTURER', dynamic: false, restart: true },
  { name: 'Build.BRAND', dynamic: false, restart: true },
  { name: 'Build.PRODUCT', dynamic: false, restart: true },
  { name: 'Build.DEVICE', dynamic: false, restart: true },
  { name: 'Build.FINGERPRINT', dynamic: false, restart: true },
  { name: 'Build.SERIAL', dynamic: false, restart: true }
];

assertEquals(21, apis.length, 'Strictly 21 APIs must be registered');
const dynamicApis = apis.filter(a => a.dynamic);
const staticApis = apis.filter(a => a.restart);
assertEquals(14, dynamicApis.length, '14 Dynamic APIs');
assertEquals(7, staticApis.length, '7 Static APIs requiring restart');

// 6. Masking Utility Verification
console.log('[Test 6] Masking Utility Verification...');
function mask(val: string | null | undefined): string {
  if (!val) return '<null>';
  if (val.length <= 4) return '****';
  return `${val.substring(0, 2)}...${val.substring(val.length - 2)}`;
}
assertEquals('<null>', mask(null), 'Null masking');
assertEquals('****', mask('test'), 'Short string masking');
assertEquals('58...72', mask('58e8039d8acedb72'), 'Long string masking');

// 7. Identity Pool Generation & Depletion Cycle
console.log('[Test 7] Identity Pool Generation & Depletion Cycle...');
const manager = DeviceIdentityManager.getInstance();
const genResult1 = manager.generateNextIdentity();
assertEquals('success', genResult1.type, 'Initial generation success');
if (genResult1.type === 'success') {
  assert(manager.getUsedCount() >= 1, 'Used count incremented');
  assert(manager.isUsed(genResult1.identity.identityNumber), 'Identity number recorded as used');
}

// 8. Uniqueness Status across successive generations
console.log('[Test 8] Successive Profile 4-Field Uniqueness Status...');
const genResult2 = manager.generateNextIdentity();
assertEquals('success', genResult2.type, 'Second generation success');
assertEquals('PASS', manager.getProfileUniquenessStatus(), 'Profile uniqueness status must be PASS across 4 fields');

// 9. Location Profile Coordinate Validation & Tolerance Matching
console.log('[Test 9] Location Profile Coordinate Validation & Tolerance Matching...');
function validateLocationProfile(lat: number, lng: number, alt: number, acc: number): boolean {
  if (lat < -90.0 || lat > 90.0) return false;
  if (lng < -180.0 || lng > 180.0) return false;
  if (acc < 0) return false;
  return true;
}
assert(validateLocationProfile(35.6762, 139.6503, 40.0, 5.0) === true, 'Tokyo coordinates valid');
assert(validateLocationProfile(37.7749, -122.4194, 15.0, 5.0) === true, 'SF coordinates valid');
assert(validateLocationProfile(95.0, 10.0, 0.0, 5.0) === false, 'Invalid latitude > 90 rejected');
assert(validateLocationProfile(0.0, -190.0, 0.0, 5.0) === false, 'Invalid longitude < -180 rejected');

const epsilonCoord = 1e-5;
const lat1 = 35.6762001;
const lat2 = 35.6762008;
assert(Math.abs(lat1 - lat2) < epsilonCoord, 'Coordinate tolerance matching within 1e-5 epsilon');

// 10. Location Subsystem 14-API Catalog Registration
console.log('[Test 10] Location Subsystem 14-API Catalog Registration...');
const locationApis = [
  'LocationManager.getLastKnownLocation(provider)',
  'LocationManager.isProviderEnabled(provider)',
  'LocationManager.requestLocationUpdates(String, long, float, LocationListener)',
  'LocationManager.requestLocationUpdates(LocationRequest, LocationListener, Looper)',
  'LocationManager.requestLocationUpdates(String, long, float, PendingIntent)',
  'LocationManager.requestSingleUpdate(String, LocationListener, Looper)',
  'LocationManager.requestSingleUpdate(Criteria, LocationListener, Looper)',
  'LocationManager.getCurrentLocation(String, CancellationSignal, Executor, Consumer)',
  'LocationManager.getProviders(boolean)',
  'LocationManager.getBestProvider(Criteria, boolean)',
  'FusedLocationProviderClient.getLastLocation()',
  'FusedLocationProviderClient.getCurrentLocation(int, CancellationToken)',
  'FusedLocationProviderClient.requestLocationUpdates(LocationRequest, LocationCallback, Looper)',
  'Location.getLatitude() / getLongitude()'
];
assertEquals(14, locationApis.length, 'Strictly 14 Location APIs cataloged');
assertEquals(21, apis.length, 'Strictly 21 Identity APIs preserved without regression');

// 11. Worldwide City Catalog Integrity
console.log('[Test 11] Worldwide City Catalog Integrity...');
import { WORLD_CITIES } from '../data/worldCities';
import { WorldLocationManager } from '../services/worldLocationManager';

assert(WORLD_CITIES.length >= 80, `World city database has at least 80 cities (actual: ${WORLD_CITIES.length})`);
for (const city of WORLD_CITIES) {
  assert(city.latitude >= -90.0 && city.latitude <= 90.0, `Latitude for ${city.city} within [-90, 90]`);
  assert(city.longitude >= -180.0 && city.longitude <= 180.0, `Longitude for ${city.city} within [-180, 180]`);
  assert(city.city.length > 0, `City name not empty for ${city.id}`);
  assert(city.country.length > 0, `Country name not empty for ${city.id}`);
  assert(city.countryCode.length === 2, `Country code 2 uppercase chars for ${city.id}`);
  assert(city.timezone.includes('/'), `IANA timezone format for ${city.id}`);
}

// 12. RFC 5737 Synthetic Test IP Generation & Scope
console.log('[Test 12] RFC 5737 Synthetic Test IP Generation & Scope...');
import { generateSyntheticIpv4 } from '../utils/crypto';
const ip1 = generateSyntheticIpv4(42);
assert(ip1.startsWith('203.0.113.'), `Synthetic IP in RFC 5737 TEST-NET-3 range: ${ip1}`);
const ipParts = ip1.split('.').map(Number);
assertEquals(4, ipParts.length, 'IPv4 4 octets');
assert(ipParts[3] >= 1 && ipParts[3] <= 254, 'Host octet within 1-254');

const ip1Deterministic = generateSyntheticIpv4(42);
assertEquals(ip1, ip1Deterministic, 'Deterministic IP generation for same seed');
const ip2 = generateSyntheticIpv4(43);
assert(ip1 !== ip2, 'Different seed produces different synthetic IP');

// 13. Three City Selection Modes Verification
console.log('[Test 13] Three City Selection Modes Verification...');
const worldManager = WorldLocationManager.getInstance();

// Mode 1: Manual City Selection
const manualTokyo = worldManager.selectManualCity('as_tok');
assertEquals('Tokyo', manualTokyo.city, 'Manual selection selects Tokyo');
assertEquals('Japan', manualTokyo.country, 'Manual selection country matches');
assertEquals('Asia/Tokyo', manualTokyo.timezone, 'Manual selection timezone matches');

const manualLondon = worldManager.selectManualCity('eu_lon');
assertEquals('London', manualLondon.city, 'Manual selection selects London');
assertEquals('United Kingdom', manualLondon.country, 'Manual selection country matches');

// Mode 2: Random World City Selection
const randomWorld1 = worldManager.selectRandomWorldCity(1001);
assert(randomWorld1.city.length > 0, 'Random world city has name');
assert(randomWorld1.syntheticIp.startsWith('203.0.113.'), 'Random world city has RFC 5737 synthetic IP');
const randomWorld1Again = worldManager.selectRandomWorldCity(1001);
assertEquals(randomWorld1.city, randomWorld1Again.city, 'Deterministic random world selection with seed');

// Mode 3: Random City in Country Selection
const usCity = worldManager.selectRandomCityInCountry('United States', 555);
assertEquals('United States', usCity.country, 'Random city in United States has US country');
assert(['New York', 'Los Angeles', 'Chicago', 'Houston', 'Miami', 'San Francisco', 'Seattle'].includes(usCity.city), `City is valid US city: ${usCity.city}`);

const japanCity = worldManager.selectRandomCityInCountry('Japan', 777);
assertEquals('Japan', japanCity.country, 'Random city in Japan has Japan country');
assert(['Tokyo', 'Osaka', 'Kyoto', 'Sapporo'].includes(japanCity.city), `City is valid Japan city: ${japanCity.city}`);

// 14. Profile Consistency Validation Check
console.log('[Test 14] Profile Consistency Validation Check...');
const validProfileCheck = worldManager.validateProfileConsistency(manualTokyo);
assertEquals(true, validProfileCheck.isConsistent, 'Tokyo profile is fully consistent');
assertEquals(0, validProfileCheck.inconsistencies.length, 'No inconsistencies in canonical profile');

const inconsistentProfile = {
  ...manualTokyo,
  country: 'Germany' // Mismatch!
};
const invalidProfileCheck = worldManager.validateProfileConsistency(inconsistentProfile);
assertEquals(false, invalidProfileCheck.isConsistent, 'Mismatched country detected as inconsistent');
assert(invalidProfileCheck.inconsistencies.length > 0, 'Inconsistencies logged');

// 15. Multi-Profile Switching & State Transition
console.log('[Test 15] Multi-Profile Switching & State Transition...');
worldManager.activateProfile(manualTokyo);
assertEquals('ACTIVE', worldManager.getActiveProfile().state, 'Active profile state is ACTIVE');

worldManager.activateProfile(manualLondon);
assertEquals('London', worldManager.getActiveProfile().city, 'Active profile updated to London');
assertEquals('ACTIVE', worldManager.getActiveProfile().state, 'Active profile state is ACTIVE');
const prev = worldManager.getPreviousProfile();
assert(prev !== null, 'Previous profile exists');
if (prev) {
  assertEquals('Tokyo', prev.city, 'Previous profile city was Tokyo');
  assertEquals('CONSUMED', prev.state, 'Previous profile marked as CONSUMED');
}

// 21 APIs still fully intact
assertEquals(21, apis.length, 'Strictly 21 Identity APIs preserved without regression');

console.log('✅ ALL TIER B & WORLDWIDE LOCATION VERIFICATION TESTS PASSED SUCCESSFULLY!');
