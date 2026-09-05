import { CityRecord, WORLD_CITIES } from '../data/worldCities';
import { WorldwideLocationProfile, CitySelectionMode } from '../types';

export class WorldLocationManager {
  private static instance: WorldLocationManager;

  private activeProfile: WorldwideLocationProfile;
  private previousProfile: WorldwideLocationProfile | null = null;
  private activeIps: Set<string> = new Set();
  private listeners: Array<() => void> = [];
  private seedCounter: number = 42;

  private constructor() {
    // Default to Tokyo, Japan
    const defaultCity = WORLD_CITIES.find(c => c.city === 'Tokyo') || WORLD_CITIES[0];
    this.activeProfile = this.createProfileForCity(defaultCity, '203.0.113.42', 'loc_tokyo_init', 'ACTIVE');
    this.activeIps.add(this.activeProfile.syntheticIp);
  }

  public static getInstance(): WorldLocationManager {
    if (!WorldLocationManager.instance) {
      WorldLocationManager.instance = new WorldLocationManager();
    }
    return WorldLocationManager.instance;
  }

  public getActiveProfile(): WorldwideLocationProfile {
    return { ...this.activeProfile };
  }

  public getPreviousProfile(): WorldwideLocationProfile | null {
    return this.previousProfile ? { ...this.previousProfile } : null;
  }

  public getAllCities(): CityRecord[] {
    return [...WORLD_CITIES];
  }

  public getAllCountries(): string[] {
    return Array.from(new Set(WORLD_CITIES.map(c => c.country))).sort();
  }

  public getAllRegions(): string[] {
    return Array.from(new Set(WORLD_CITIES.map(c => c.region))).sort();
  }

  public getCitiesInCountry(country: string): CityRecord[] {
    return WORLD_CITIES.filter(c => c.country.toLowerCase() === country.toLowerCase());
  }

  public getCitiesInRegion(region: string): CityRecord[] {
    return WORLD_CITIES.filter(c => c.region.toLowerCase() === region.toLowerCase());
  }

  public findCityById(id: string): CityRecord | undefined {
    return WORLD_CITIES.find(c => c.id === id);
  }

  public findCityByNameAndCountry(city: string, country: string): CityRecord | undefined {
    return WORLD_CITIES.find(
      c => c.city.toLowerCase() === city.toLowerCase() && c.country.toLowerCase() === country.toLowerCase()
    );
  }

  /**
   * Deterministic synthetic IP generator using RFC 5737 documentation test subnets:
   * TEST-NET-3 (203.0.113.0/24), TEST-NET-2 (198.51.100.0/24), TEST-NET-1 (192.0.2.0/24).
   */
  public generateSyntheticIp(seed?: number): string {
    const s = seed !== undefined ? seed : this.seedCounter++;
    // Use RFC 5737 203.0.113.0/24 range for synthetic testing
    const hostPart = (Math.abs(s) % 250) + 2; // Range 2..251
    const ip = `203.0.113.${hostPart}`;
    return ip;
  }

  /**
   * Generates a unique synthetic IP not currently assigned to an active profile.
   */
  public generateUniqueSyntheticIp(seed?: number): string {
    let s = seed !== undefined ? seed : this.seedCounter++;
    for (let attempt = 0; attempt < 250; attempt++) {
      const hostPart = ((Math.abs(s) + attempt) % 250) + 2;
      const ip = `203.0.113.${hostPart}`;
      if (!this.activeIps.has(ip) || ip === this.activeProfile.syntheticIp) {
        return ip;
      }
    }
    return `203.0.113.${(Math.abs(s) % 250) + 2}`;
  }

  public createProfileForCity(
    cityRecord: CityRecord,
    customIp?: string,
    profileId?: string,
    initialState: 'AVAILABLE' | 'ACTIVE' | 'CONSUMED' = 'AVAILABLE'
  ): WorldwideLocationProfile {
    // Coordinate validation
    if (cityRecord.latitude < -90 || cityRecord.latitude > 90) {
      throw new Error(`Invalid latitude: ${cityRecord.latitude}. Must be between -90 and 90.`);
    }
    if (cityRecord.longitude < -180 || cityRecord.longitude > 180) {
      throw new Error(`Invalid longitude: ${cityRecord.longitude}. Must be between -180 and 180.`);
    }

    const ip = customIp || this.generateUniqueSyntheticIp();
    const pid = profileId || `loc_prof_${cityRecord.id}_${Date.now().toString(36)}`;

    return {
      profileId: pid,
      city: cityRecord.city,
      country: cityRecord.country,
      countryCode: cityRecord.countryCode,
      region: cityRecord.region,
      latitude: cityRecord.latitude,
      longitude: cityRecord.longitude,
      timezone: cityRecord.timezone,
      provider: 'gps',
      altitude: Math.round((Math.abs(cityRecord.latitude * 1.5) % 50 + 10) * 10) / 10,
      accuracy: 3.5,
      speed: 0.0,
      bearing: 0.0,
      timestamp: Date.now(),
      elapsedRealtimeNanos: Date.now() * 1_000_000,
      syntheticIp: ip,
      state: initialState
    };
  }

  /**
   * Mode 1: Manual City Selection
   */
  public selectManualCity(cityId: string): WorldwideLocationProfile {
    const cityRecord = this.findCityById(cityId);
    if (!cityRecord) {
      throw new Error(`City with ID '${cityId}' not found in WorldCityDatabase.`);
    }
    return this.createProfileForCity(cityRecord);
  }

  /**
   * Mode 2: Random World City Selection
   * Supports deterministic pseudo-random selection if seed is provided.
   */
  public selectRandomWorldCity(seed?: number): WorldwideLocationProfile {
    let index: number;
    if (seed !== undefined) {
      index = Math.abs(seed) % WORLD_CITIES.length;
    } else {
      index = Math.floor(Math.random() * WORLD_CITIES.length);
    }
    const cityRecord = WORLD_CITIES[index];
    const ip = this.generateSyntheticIp(seed !== undefined ? seed : index);
    return this.createProfileForCity(cityRecord, ip);
  }

  /**
   * Mode 3: Random Country -> City Selection
   * Optionally select country, then choose a random city in that country.
   */
  public selectRandomCityInCountry(country: string, seed?: number): WorldwideLocationProfile {
    const citiesInCountry = this.getCitiesInCountry(country);
    if (citiesInCountry.length === 0) {
      throw new Error(`No cities found for country '${country}'.`);
    }
    let index: number;
    if (seed !== undefined) {
      index = Math.abs(seed) % citiesInCountry.length;
    } else {
      index = Math.floor(Math.random() * citiesInCountry.length);
    }
    const cityRecord = citiesInCountry[index];
    const ip = this.generateSyntheticIp(seed !== undefined ? seed : index);
    return this.createProfileForCity(cityRecord, ip);
  }

  /**
   * Activates a newly generated or selected profile.
   * Advances the current profile from ACTIVE to CONSUMED.
   * Moves newly activated profile to ACTIVE.
   */
  public activateProfile(profile: WorldwideLocationProfile): WorldwideLocationProfile {
    // Mark previous as consumed
    if (this.activeProfile) {
      this.previousProfile = {
        ...this.activeProfile,
        state: 'CONSUMED'
      };
      this.activeIps.delete(this.activeProfile.syntheticIp);
    }

    const activated: WorldwideLocationProfile = {
      ...profile,
      state: 'ACTIVE',
      timestamp: Date.now(),
      elapsedRealtimeNanos: Date.now() * 1_000_000
    };

    this.activeProfile = activated;
    this.activeIps.add(activated.syntheticIp);

    this.notify();
    return { ...activated };
  }

  /**
   * Clear active profile back to default preset.
   */
  public clearProfile(): WorldwideLocationProfile {
    const defaultCity = WORLD_CITIES.find(c => c.city === 'Tokyo') || WORLD_CITIES[0];
    const resetProfile = this.createProfileForCity(defaultCity, '203.0.113.42', 'loc_tokyo_reset', 'ACTIVE');
    return this.activateProfile(resetProfile);
  }

  /**
   * Validates application-level consistency of a profile.
   * Ensures City ↔ Country ↔ Coordinates ↔ Timezone ↔ Synthetic IP match catalog.
   */
  public validateProfileConsistency(profile: WorldwideLocationProfile): {
    isConsistent: boolean;
    cityMatch: boolean;
    countryMatch: boolean;
    coordMatch: boolean;
    timezoneMatch: boolean;
    ipFormatValid: boolean;
    ipInTestRange: boolean;
    diagnostics: string;
    inconsistencies: string[];
  } {
    const inconsistencies: string[] = [];
    const cityRecord = this.findCityByNameAndCountry(profile.city, profile.country);
    if (!cityRecord) {
      inconsistencies.push(`City '${profile.city}' in '${profile.country}' not found in canonical World City Database.`);
      return {
        isConsistent: false,
        cityMatch: false,
        countryMatch: false,
        coordMatch: false,
        timezoneMatch: false,
        ipFormatValid: this.isValidIpv4(profile.syntheticIp),
        ipInTestRange: this.isRfc5737TestIp(profile.syntheticIp),
        diagnostics: `City '${profile.city}' in '${profile.country}' not found in canonical World City Database.`,
        inconsistencies
      };
    }

    const epsilon = 1e-4;
    const coordMatch =
      Math.abs(profile.latitude - cityRecord.latitude) < epsilon &&
      Math.abs(profile.longitude - cityRecord.longitude) < epsilon;
    if (!coordMatch) {
      inconsistencies.push(`Coordinates mismatch: expected (${cityRecord.latitude}, ${cityRecord.longitude}), got (${profile.latitude}, ${profile.longitude}).`);
    }

    const timezoneMatch = profile.timezone === cityRecord.timezone;
    if (!timezoneMatch) {
      inconsistencies.push(`Timezone mismatch: expected '${cityRecord.timezone}', got '${profile.timezone}'.`);
    }

    const ipFormatValid = this.isValidIpv4(profile.syntheticIp);
    if (!ipFormatValid) {
      inconsistencies.push(`Invalid IPv4 format: '${profile.syntheticIp}'.`);
    }

    const ipInTestRange = this.isRfc5737TestIp(profile.syntheticIp);
    if (!ipInTestRange) {
      inconsistencies.push(`Synthetic IP '${profile.syntheticIp}' is not in RFC 5737 test range.`);
    }

    const isConsistent = inconsistencies.length === 0;

    let diagnostics = 'PROFILE CONSISTENCY: PASS. City, coordinates, timezone, and synthetic IP are coherently bound.';
    if (inconsistencies.length > 0) {
      diagnostics = inconsistencies.join(' ');
    }

    return {
      isConsistent,
      cityMatch: true,
      countryMatch: true,
      coordMatch,
      timezoneMatch,
      ipFormatValid,
      ipInTestRange,
      diagnostics,
      inconsistencies
    };
  }

  public isValidIpv4(ip: string): boolean {
    const parts = ip.split('.');
    if (parts.length !== 4) return false;
    for (const p of parts) {
      const n = Number(p);
      if (isNaN(n) || n < 0 || n > 255 || (p.length > 1 && p.startsWith('0'))) {
        return false;
      }
    }
    return true;
  }

  public isRfc5737TestIp(ip: string): boolean {
    if (!this.isValidIpv4(ip)) return false;
    // TEST-NET-3 (203.0.113.0/24), TEST-NET-2 (198.51.100.0/24), TEST-NET-1 (192.0.2.0/24)
    return ip.startsWith('203.0.113.') || ip.startsWith('198.51.100.') || ip.startsWith('192.0.2.');
  }

  public subscribe(listener: () => void): () => void {
    this.listeners.push(listener);
    return () => {
      this.listeners = this.listeners.filter(l => l !== listener);
    };
  }

  private notify(): void {
    this.listeners.forEach(l => l());
  }
}
