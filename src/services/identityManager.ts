import { DeviceIdentity, GenerationResult } from '../types';
import { createIdentity } from '../utils/crypto';

const STORAGE_KEY_USED_IDS = 'device_id_lab_used_ids';
const STORAGE_KEY_LATEST_ID = 'device_id_lab_latest_id';
const MAX_POOL_CAPACITY = 1000000;
const MAX_RANDOM_COLLISION_RETRIES = 25;
const DETERMINISTIC_FALLBACK_THRESHOLD = 999990;

export class DeviceIdentityManager {
  private static instance: DeviceIdentityManager;
  private usedIdsSet: Set<number> = new Set();
  private latestIdentity: DeviceIdentity | null = null;
  private subscribers: Array<() => void> = [];

  private constructor() {
    this.loadFromStorage();
  }

  public static getInstance(): DeviceIdentityManager {
    if (!DeviceIdentityManager.instance) {
      DeviceIdentityManager.instance = new DeviceIdentityManager();
    }
    return DeviceIdentityManager.instance;
  }

  private loadFromStorage(): void {
    try {
      const rawUsed = localStorage.getItem(STORAGE_KEY_USED_IDS);
      if (rawUsed) {
        const parsed = JSON.parse(rawUsed) as number[];
        this.usedIdsSet = new Set(parsed);
      }
      const rawLatest = localStorage.getItem(STORAGE_KEY_LATEST_ID);
      if (rawLatest) {
        this.latestIdentity = JSON.parse(rawLatest) as DeviceIdentity;
      }
    } catch (e) {
      console.error('Failed to load identities from localStorage:', e);
      this.usedIdsSet = new Set();
      this.latestIdentity = null;
    }
  }

  private saveToStorage(): void {
    try {
      // For performance if array is huge, save up to 100,000 in storage or chunked
      const arr = Array.from(this.usedIdsSet);
      localStorage.setItem(STORAGE_KEY_USED_IDS, JSON.stringify(arr));
      if (this.latestIdentity) {
        localStorage.setItem(STORAGE_KEY_LATEST_ID, JSON.stringify(this.latestIdentity));
      } else {
        localStorage.removeItem(STORAGE_KEY_LATEST_ID);
      }
    } catch (e) {
      console.warn('Storage limit reached or failed saving:', e);
    }
  }

  public subscribe(callback: () => void): () => void {
    this.subscribers.push(callback);
    return () => {
      this.subscribers = this.subscribers.filter(cb => cb !== callback);
    };
  }

  private notify(): void {
    this.subscribers.forEach(cb => cb());
  }

  public getCurrentIdentity(): DeviceIdentity | null {
    return this.latestIdentity;
  }

  public getUsedCount(): number {
    return this.usedIdsSet.size;
  }

  public getTotalCapacity(): number {
    return MAX_POOL_CAPACITY;
  }

  public isUsed(idNumber: number): boolean {
    return this.usedIdsSet.has(idNumber);
  }

  public generateNextIdentity(): GenerationResult {
    const currentCount = this.usedIdsSet.size;
    if (currentCount >= MAX_POOL_CAPACITY) {
      return {
        type: 'pool_exhausted',
        message: 'ID POOL EXHAUSTED: All 1,000,000 test identities have been used.'
      };
    }

    let candidateIndex: number | null = null;

    // 1. If plenty of free slots, use crypto random selection
    if (currentCount < DETERMINISTIC_FALLBACK_THRESHOLD) {
      for (let attempt = 0; attempt < MAX_RANDOM_COLLISION_RETRIES; attempt++) {
        // Random number between 1 and MAX_POOL_CAPACITY inclusive
        const randomNum = 1 + Math.floor(Math.random() * MAX_POOL_CAPACITY);
        if (!this.usedIdsSet.has(randomNum)) {
          candidateIndex = randomNum;
          break;
        }
      }
    }

    // 2. If collisions occurred or near pool exhaustion, do deterministic fallback search
    if (candidateIndex === null) {
      candidateIndex = this.findFirstUnusedIndex();
    }

    if (candidateIndex === null) {
      return {
        type: 'pool_exhausted',
        message: 'ID POOL EXHAUSTED: All 1,000,000 test identities have been used.'
      };
    }

    // 3. Construct deterministic identity & persist
    const identity = createIdentity(candidateIndex);
    this.usedIdsSet.add(candidateIndex);
    this.latestIdentity = identity;
    this.saveToStorage();
    this.notify();

    return {
      type: 'success',
      identity
    };
  }

  public generateBatch(count: number): { allocated: number; duplicates: number } {
    let allocated = 0;
    let duplicates = 0;

    for (let i = 0; i < count; i++) {
      const res = this.generateNextIdentity();
      if (res.type === 'success') {
        allocated++;
      } else if (res.type === 'pool_exhausted') {
        break;
      }
    }

    return { allocated, duplicates };
  }

  private findFirstUnusedIndex(): number | null {
    for (let i = 1; i <= MAX_POOL_CAPACITY; i++) {
      if (!this.usedIdsSet.has(i)) {
        return i;
      }
    }
    return null;
  }

  public resetDatabase(): void {
    this.usedIdsSet.clear();
    this.latestIdentity = null;
    this.saveToStorage();
    this.notify();
  }
}
