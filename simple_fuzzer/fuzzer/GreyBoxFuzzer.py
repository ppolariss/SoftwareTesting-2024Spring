import os
import time
from typing import List, Any, Tuple, Set, Dict
import json
import random

from fuzzer.Fuzzer import Fuzzer
from runner.Runner import Runner
from utils.Coverage import Location
from utils.Mutator import Mutator
from runner.FunctionCoverageRunner import FunctionCoverageRunner
from schedule.PowerSchedule import PowerSchedule
from utils.Seed import Seed

SEED_DIR = "seed_inputs"

class GreyBoxFuzzer(Fuzzer):
    def __init__(self, seeds: List[str], schedule: PowerSchedule, is_print: bool, from_disk: bool) -> None:
        """Constructor.
        `seeds` - a list of (input) strings to mutate.
        `mutator` - the mutator to apply.
        `schedule` - the power schedule to apply.
        """
        super().__init__()
        self.last_crash_time = self.start_time
        self.population = []
        self.file_map = {}
        self.covered_line: Set[Location] = set()
        self.seed_index = 0
        self.crash_map = dict()
        self.seeds = seeds
        self.mutator = Mutator()
        self.schedule = schedule

        self.from_disk = from_disk
        if not self.from_disk:
            print("我已经清理")
            self.clear_seed_dir()  

        if is_print:
            print("""
┌───────────────────────┬───────────────────────┬───────────────────┬────────────────┬───────────────────┐
│        Run Time       │    Last Uniq Crash    │    Total Execs    │  Uniq Crashes  │   Covered Lines   │
├───────────────────────┼───────────────────────┼───────────────────┼────────────────┼───────────────────┤""")

    def create_candidate(self) -> str:
        """Returns an input generated by fuzzing a seed in the population"""
        seed = self.schedule.choose(self.population)

        # Stacking: Apply multiple mutations to generate the candidate
        candidate = seed.data
        trials = min(len(candidate), 1 << random.randint(1, 5))
        for i in range(trials):
            candidate = self.mutator.mutate(candidate)
        return candidate

    def fuzz(self) -> str:
        """Returns first each seed once and then generates new inputs"""
        if self.seed_index < len(self.seeds):
            if self.from_disk:
                # 从磁盘读取指定序号的种子
                self.inp = self.load_seed(self.seed_index)
            else:
                # 使用预定义的种子
                self.inp = self.seeds[self.seed_index]
            self.seed_index += 1
        else:
            # Mutating
            self.inp = self.create_candidate()
        return self.inp

    def load_seed(self, index: int) -> str:
        """Loads a specific seed from a JSON file"""
        seed_file = os.path.join(SEED_DIR, f"{index}.json")
        if os.path.exists(seed_file):
            with open(seed_file, 'r') as f:
                seed = json.load(f)
            return seed["data"]
        else:
            return ""

    def print_stats(self):
        def format_seconds(seconds):
            hours = int(seconds) // 3600
            minutes = int(seconds % 3600) // 60
            remaining_seconds = int(seconds) % 60
            return f"{hours:02d}:{minutes:02d}:{remaining_seconds:02d}"

        template = """│{runtime}│{crash_time}│{total_exec}│{uniq_crash}│{covered_line}│
├───────────────────────┼───────────────────────┼───────────────────┼────────────────┼───────────────────┤"""

        template = template.format(runtime=format_seconds(time.time() - self.start_time).center(23),
                                   crash_time=format_seconds(self.last_crash_time - self.start_time).center(23),
                                   total_exec=str(self.total_execs).center(19),
                                   uniq_crash=str(len(set(self.crash_map.values()))).center(16),
                                   covered_line=str(len(self.covered_line)).center(19))
        print(template)

    def run(self, runner: FunctionCoverageRunner) -> Tuple[Any, str]:  # type: ignore
        """Run function(inp) while tracking coverage.
           If we reach new coverage,
           add inp to population and its coverage to population_coverage
        """
        result, outcome = super().run(runner)
        if len(self.covered_line) != len(runner.all_coverage):
            self.covered_line |= runner.all_coverage
            if outcome == Runner.PASS:  # NOTE
                # We have new coverage
                seed = Seed(self.inp, runner.coverage())
                self.population.append(seed)
                self.save_seed(seed)
        if outcome == Runner.FAIL:
            seed = Seed(self.inp, runner.coverage())
            self.save_seed(seed)
            uniq_crash_num = len(set(self.crash_map.values()))
            self.crash_map[self.inp] = result
            if len(set(self.crash_map.values())) != uniq_crash_num:
                self.last_crash_time = time.time()

        return result, outcome

    def save_seed(self, seed: Seed):
        seed_data = {
            "data": seed.data
        }

        # Ensure the SEED_DIR exists
        if not os.path.exists(SEED_DIR):
            os.makedirs(SEED_DIR)

        seed_index = len(os.listdir(SEED_DIR))
        seed_file = os.path.join(SEED_DIR, f"{seed_index}.json")

        with open(seed_file, 'w') as f:
            json.dump(seed_data, f, indent=2)

    def clear_seed_dir(self):
        """Clear all seeds in the SEED_DIR"""
        if os.path.exists(SEED_DIR):
            for file in os.listdir(SEED_DIR):
                file_path = os.path.join(SEED_DIR, file)
                if os.path.isfile(file_path):
                    os.unlink(file_path)
