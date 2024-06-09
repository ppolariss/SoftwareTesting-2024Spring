from typing import List, Dict
from utils.Seed import Seed
from schedule.PowerSchedule import PowerSchedule

class CrashPowerSchedule(PowerSchedule):

    def __init__(self) -> None:
        super().__init__()
        self.crash_counts: Dict[str, int] = {}  # 记录 seed 触发的 crash 次数

    def assign_energy(self, population: List[Seed]) -> None:
        """Assign higher energy to seeds that caused more crashes"""
        for seed in population:
            seed_data = seed.data
            if seed_data in self.crash_counts:
                seed.energy = self.crash_counts[seed_data]
            else:
                seed.energy = 1  

    def increment_crash_count(self, seed: Seed) -> None:
        """Increment the crash count for the given seed"""
        seed_data = seed.data
        if seed_data in self.crash_counts:
            self.crash_counts[seed_data] += 1
        else:
            self.crash_counts[seed_data] = 1
        # print(f"new crash")
        # print(f"new crash \n self.crash_counts = {self.crash_counts.values()}")
        # print(f"len(self.crash_counts) = {len(self.crash_counts)}")
