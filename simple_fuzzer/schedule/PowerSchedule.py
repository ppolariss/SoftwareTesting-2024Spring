import random
from typing import List
import heapq
from utils.Seed import Seed

MAX_SEEDS = 100


class PowerSchedule:

    def assign_energy(self, population: List[Seed]) -> None:
        """Assigns each seed the same energy"""
        for seed in population:
            seed.energy = 1

    def normalized_energy(self, population: List[Seed]) -> List[float]:
        """Normalize energy"""
        energy = list(map(lambda seed: seed.energy, population))
        sum_energy = sum(energy)  # Add up all values in energy
        assert sum_energy != 0
        norm_energy = list(map(lambda nrg: nrg / sum_energy, energy))
        return norm_energy

    def choose(self, population: List[Seed]) -> Seed:
        """Choose weighted by normalized energy."""
        self.assign_energy(population)
        norm_energy = self.normalized_energy(population)
        while len(population) > MAX_SEEDS:
            min_index = norm_energy.index(min(norm_energy))
            del norm_energy[min_index]
            del population[min_index]
            # print("delete")
        # if len(population) > MAX_SEEDS:
        #     min_heap = list(zip(norm_energy, range(len(norm_energy))))
        #     heapq.heapify(min_heap)
        #     indices_to_remove = {heapq.heappop(min_heap)[1] for _ in range(len(population) - MAX_SEEDS)}
        #     list_indices_to_remove = list(indices_to_remove)
        #     list_indices_to_remove.sort(reverse=True)
        #     for index in list_indices_to_remove:
        #         del population[index]
        #         del norm_energy[index]
        seed: Seed = random.choices(population, weights=norm_energy)[0]
        return seed
