// Pure TypeScript implementation of standard SHA-256
// Compatible with standard RFC 6234 / NIST FIPS 180-4 and Java MessageDigest.getInstance("SHA-256")

function sha256(ascii: string): Uint8Array {
  const lengthProperty = 'length';
  let i = 0;
  let j = 0;

  const words: number[] = [];
  const asciiBitLength = ascii[lengthProperty] * 8;

  // Initial hash value: first 32 bits of the fractional parts of the square roots of the first 8 primes
  let hash: number[] = [
    0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
    0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
  ];

  // Constants: first 32 bits of the fractional parts of the cube roots of the first 64 primes
  const k: number[] = [
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
  ];

  for (i = 0; i < ascii[lengthProperty]; i++) {
    j = ascii.charCodeAt(i);
    if (j >> 8) return new Uint8Array(32); // UTF-8 fallback
    words[i >> 2] |= j << ((3 - (i % 4)) * 8);
  }

  words[asciiBitLength >> 5] |= 0x80 << (24 - (asciiBitLength % 32));
  words[(((asciiBitLength + 64) >> 9) << 4) + 15] = asciiBitLength;

  const w = new Array(64);

  for (i = 0; i < words[lengthProperty]; i += 16) {
    let a = hash[0];
    let b = hash[1];
    let c = hash[2];
    let d = hash[3];
    let e = hash[4];
    let f = hash[5];
    let g = hash[6];
    let h = hash[7];

    for (j = 0; j < 64; j++) {
      if (j < 16) {
        w[j] = words[j + i] | 0;
      } else {
        const gamma0 =
          ((w[j - 15] >>> 7) | (w[j - 15] << 25)) ^
          ((w[j - 15] >>> 18) | (w[j - 15] << 14)) ^
          (w[j - 15] >>> 3);
        const gamma1 =
          ((w[j - 2] >>> 17) | (w[j - 2] << 15)) ^
          ((w[j - 2] >>> 19) | (w[j - 2] << 13)) ^
          (w[j - 2] >>> 10);
        w[j] = (w[j - 16] + gamma0 + w[j - 7] + gamma1) | 0;
      }

      const ch = (e & f) ^ (~e & g);
      const maj = (a & b) ^ (a & c) ^ (b & c);
      const sigma0 =
        ((a >>> 2) | (a << 30)) ^
        ((a >>> 13) | (a << 19)) ^
        ((a >>> 22) | (a << 10));
      const sigma1 =
        ((e >>> 6) | (e << 26)) ^
        ((e >>> 11) | (e << 21)) ^
        ((e >>> 25) | (e << 7));

      const temp1 = (h + sigma1 + ch + k[j] + w[j]) | 0;
      const temp2 = (sigma0 + maj) | 0;

      h = g;
      g = f;
      f = e;
      e = (d + temp1) | 0;
      d = c;
      c = b;
      b = a;
      a = (temp1 + temp2) | 0;
    }

    hash[0] = (hash[0] + a) | 0;
    hash[1] = (hash[1] + b) | 0;
    hash[2] = (hash[2] + c) | 0;
    hash[3] = (hash[3] + d) | 0;
    hash[4] = (hash[4] + e) | 0;
    hash[5] = (hash[5] + f) | 0;
    hash[6] = (hash[6] + g) | 0;
    hash[7] = (hash[7] + h) | 0;
  }

  const out = new Uint8Array(32);
  for (i = 0; i < 8; i++) {
    out[i * 4] = (hash[i] >>> 24) & 0xff;
    out[i * 4 + 1] = (hash[i] >>> 16) & 0xff;
    out[i * 4 + 2] = (hash[i] >>> 8) & 0xff;
    out[i * 4 + 3] = hash[i] & 0xff;
  }
  return out;
}

const ANDROID_ID_SALT = "AndroidID_Deterministic_Salt_Lab_v1_";
const TELEPHONY_ID_SALT = "Telephony_Deterministic_Salt_Lab_v1_";

/**
 * Generates a deterministic 16-hexadecimal character test Android ID
 * for the given identity index (1..1,000,000).
 * Matches Kotlin's RandomIdGenerator.generateAndroidTestId.
 */
export function generateAndroidTestId(identityNumber: number): string {
  const input = `${ANDROID_ID_SALT}${identityNumber}`;
  const digest = sha256(input);
  // Take first 8 bytes = 16 hex characters
  let hex = '';
  for (let i = 0; i < 8; i++) {
    hex += digest[i].toString(16).padStart(2, '0');
  }
  return hex.toLowerCase();
}

/**
 * Generates a deterministic 15-numeric character test Telephony/IMEI identifier
 * for the given identity index.
 * Matches Kotlin's RandomIdGenerator.generateTelephonyTestId.
 */
export function generateTelephonyTestId(identityNumber: number): string {
  const input = `${TELEPHONY_ID_SALT}${identityNumber}`;
  const digest = sha256(input);
  
  // Convert 32-byte digest to BigInt (positive)
  let hexStr = '';
  for (let i = 0; i < digest.length; i++) {
    hexStr += digest[i].toString(16).padStart(2, '0');
  }
  const bigInt = BigInt('0x' + hexStr);
  const modulus = BigInt('1000000000000000'); // 10^15
  let rawNum = (bigInt % modulus).toString().padStart(15, '0');

  // Ensure non-zero leading digit (e.g., standard cellular TAC prefix)
  if (rawNum.startsWith('0')) {
    rawNum = '3' + rawNum.substring(1);
  }
  return rawNum;
}

const PHONE_NUMBER_SALT = 'PhoneNumber_Deterministic_Salt_Lab_v1_';
const BATTERY_HEALTH_SALT = 'BatteryHealth_Deterministic_Salt_Lab_v1_';

/**
 * Generates a deterministic synthetic phone number (+1 (555) XXX-XXXX)
 * for the given identity index.
 */
export function generateSyntheticPhoneNumber(identityNumber: number): string {
  const input = `${PHONE_NUMBER_SALT}${identityNumber}`;
  const digest = sha256(input);
  const prefix = 100 + ((digest[0] << 8 | digest[1]) % 900);
  const lineNum = 1000 + ((digest[2] << 8 | digest[3]) % 9000);
  return `+1 (555) ${prefix}-${lineNum}`;
}

/**
 * Generates a realistic battery health percentage (60% - 99%)
 * for the given identity index, ensuring difference from previous if provided.
 */
export function generateBatteryHealth(identityNumber: number, previousHealth?: number): number {
  const input = `${BATTERY_HEALTH_SALT}${identityNumber}`;
  const digest = sha256(input);
  let health = 60 + (digest[0] % 40); // 60%..99%
  if (previousHealth !== undefined && health === previousHealth) {
    health = health >= 99 ? 75 : health + 1;
  }
  return health;
}

const SYNTHETIC_IP_SALT = 'SyntheticIp_Deterministic_Salt_RFC5737_v1_';

/**
 * Generates a deterministic synthetic test IPv4 address in RFC 5737 TEST-NET-3
 * range (203.0.113.0/24, specifically 203.0.113.1 - 203.0.113.254).
 */
export function generateSyntheticIpv4(seed: number): string {
  const input = `${SYNTHETIC_IP_SALT}${seed}`;
  const digest = sha256(input);
  const host = 1 + (digest[0] % 254);
  return `203.0.113.${host}`;
}

/**
 * Creates a full DeviceIdentity for the specified identity number,
 * binding Fingerprint, Android ID, Phone Number, and Battery Health together.
 */
export function createIdentity(
  identityNumber: number,
  createdAt: number = Date.now(),
  previousHealth?: number
): {
  identityNumber: number;
  androidTestId: string;
  telephonyTestId: string;
  syntheticPhoneNumber: string;
  batteryHealth: number;
  fingerprint: string;
  createdAt: number;
} {
  const androidTestId = generateAndroidTestId(identityNumber);
  const telephonyTestId = generateTelephonyTestId(identityNumber);
  const syntheticPhoneNumber = generateSyntheticPhoneNumber(identityNumber);
  const batteryHealth = generateBatteryHealth(identityNumber, previousHealth);

  // Compute profile fingerprint binding all 4 fields into the SAME profile
  const rawData = `${identityNumber}:${androidTestId}:${telephonyTestId}:${syntheticPhoneNumber}:${batteryHealth}`;
  const digest = sha256(rawData);
  let fingerprint = '';
  for (let i = 0; i < 16; i++) {
    fingerprint += digest[i].toString(16).padStart(2, '0');
  }

  return {
    identityNumber,
    androidTestId,
    telephonyTestId,
    syntheticPhoneNumber,
    batteryHealth,
    fingerprint,
    createdAt
  };
}
