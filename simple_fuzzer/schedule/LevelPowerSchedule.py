from typing import Dict, Sequence, Set, Union, Any
from schedule.PowerSchedule import PowerSchedule

from utils.Coverage import Location
from utils.Seed import Seed

 # NOTE: 不使用这种调度方式，因为 distance 需要根据函数调用图计算，没太看懂
class LevelPowerSchedule(PowerSchedule):
    """Assign high energy to seeds close to some target"""

    def __init__(self, distance: Dict[str, int], exponent: float) -> None:
        self.distance = distance
        self.exponent = exponent

    def __getFunctions__(self, coverage: Set[Location]) -> Set[str]:
        functions = set()
        for f, _ in set(coverage):
            functions.add(f)
        return functions

    def assignEnergy(self, population: Sequence[Seed]):
        """Assigns each seed energy inversely proportional
           to the average function-level distance to target."""
        min_dist: Union[int, float] = 0xFFFF
        max_dist: Union[int, float] = 0

        for seed in population:
            if seed.distance < 0: 
                num_dist = 0
                sum_dist = 0
                for f in self.__getFunctions__(seed.coverage):
                    if f in list(self.distance):
                        sum_dist += self.distance[f]
                        num_dist += 1
                seed.distance = sum_dist / num_dist
            if seed.distance < min_dist:
                min_dist = seed.distance
            if seed.distance > max_dist:
                max_dist = seed.distance

        for seed in population:
            if seed.distance == min_dist:
                if min_dist == max_dist:
                    seed.energy = 1
                else:
                    seed.energy = max_dist - min_dist
            else:
                seed.energy = (max_dist - min_dist) / (seed.distance - min_dist)
    
    # def assignEnergy(self, population: Sequence[Seed]) -> None:
    #     """Assigns each seed energy inversely proportional
    #        to the average function-level distance to target."""
    #     for seed in population:
    #         if seed.distance < 0:
    #             num_dist = 0
    #             sum_dist = 0
    #             for f in self.__getFunctions__(seed.coverage):
    #                 if f in list(self.distance):
    #                     sum_dist += self.distance[f]
    #                     num_dist += 1
    #             seed.distance = sum_dist / num_dist
    #             seed.energy = (1 / seed.distance) ** self.exponent