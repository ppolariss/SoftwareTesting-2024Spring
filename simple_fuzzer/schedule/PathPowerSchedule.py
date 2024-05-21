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

    # def update_path_frequencies(self, seed: Seed) -> None:
    #     """更新给定种子覆盖的路径的频率"""
    #     path_str = self.path_to_string(seed.coverage)
    #     if path_str in self.path_frequencies:
    #         self.path_frequencies[path_str] += 1
    #     else:
    #         self.path_frequencies[path_str] = 1

    # def path_to_string(self, coverage: Set[Location]) -> str:
    #     """将覆盖集合转换为字符串表示，用于在字典中作为键"""
    #     # 这里我们简单地将覆盖集合的元素排序并连接起来作为路径的唯一标识
    #     return "-".join(sorted(["{}:{}".format(loc[0], loc[1]) for loc in coverage]))

    def assign_energy(self, population: Sequence[Seed]) -> None:
        """Assign exponential energy inversely proportional to path frequency"""
        # TODO
        for seed in population:
            seed.energy = 1 / (
                self.path_frequencies[get_path_id(seed.coverage)] ** self.exponent
            )
        # # 首先更新路径频率
        # for seed in population:
        #     self.update_path_frequencies(seed)

        # # 计算总路径数，用于能量的归一化
        # total_paths = sum(self.path_frequencies.values())

        # # 为每个种子分配能量
        # for seed in population:
        #     path_str = self.path_to_string(seed.coverage)
        #     if path_str in self.path_frequencies:
        #         # 使用路径频率的倒数作为能量值，这里使用1/x的指数关系
        #         seed.energy = math.exp(-self.path_frequencies[path_str] / total_paths)
        #     else:
        #         # 如果路径没有被访问过，可以分配一个默认的能量值
        #         seed.energy = 1.0
