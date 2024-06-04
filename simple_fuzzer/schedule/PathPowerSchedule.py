from typing import Dict, Sequence, Set, Any
from utils.Coverage import Location
from schedule.PowerSchedule import PowerSchedule
from utils.Seed import Seed
import math
import hashlib
import pickle


def get_path_id(coverage: Any) -> str:
    """Returns a unique hash for the covered statements"""
    pickled = pickle.dumps(sorted(coverage))
    return hashlib.md5(pickled).hexdigest()


class PathPowerSchedule(PowerSchedule):

    def __init__(self, exponent: float) -> None:
        super().__init__()
        # 存储路径频率的字典，键为路径的字符串表示，值为该路径的访问次数
        self.path_frequencies: Dict[str, int] = {}
        self.exponent = exponent

    def update_path_frequencies(self, path_str: str) -> bool:
        if path_str in self.path_frequencies:
            self.path_frequencies[path_str] += 1
            return False
        else:
            self.path_frequencies[path_str] = 1
            return True


    def assign_energy(self, population: Sequence[Seed]) -> None:
        """Assign exponential energy inversely proportional to path frequency"""
        # TODO
        for seed in population:
            seed.energy = 1 / (
                self.path_frequencies[get_path_id(seed.coverage)] ** self.exponent
            )
        # for seed in population:
        #     seed.energy = 0
        #     for path in seed.coverage:
        #         seed.energy += self.path_frequencies[get_path_id(path)] ** self.exponent
        #     seed.energy = 1 / (
        #         seed.energy if seed.energy != 0 else 1
        #     )

#         seed.energy = math.exp(-self.path_frequencies[path_str] / total_paths)
