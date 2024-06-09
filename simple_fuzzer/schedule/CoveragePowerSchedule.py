from typing import List
from utils.Seed import Seed
from schedule.PowerSchedule import PowerSchedule

class CoveragePowerSchedule(PowerSchedule):

    def assign_energy(self, population: List[Seed]) -> None:
        """Assign energy based on the number of unique coverage points"""
        for seed in population:
            seed.energy = len(seed.coverage)  # Assign energy proportional to the coverage size