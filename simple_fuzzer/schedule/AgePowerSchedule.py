from typing import List
from utils.Seed import Seed
from schedule.PowerSchedule import PowerSchedule

class AgePowerSchedule(PowerSchedule):

    def __init__(self) -> None:
        super().__init__()
        self.seed_ages = {}  

    def assign_energy(self, population: List[Seed]) -> None:
        """Assign energy inversely proportional to the seed's age"""
        for seed in population:
            if seed not in self.seed_ages:
                self.seed_ages[seed] = 0
            self.seed_ages[seed] += 1
            seed.energy = 1 / self.seed_ages[seed]  # Assign energy inversely proportional to age