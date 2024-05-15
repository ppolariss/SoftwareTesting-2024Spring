import os
import time
from typing import List, Any, Tuple, Set

import random

from fuzzer.Fuzzer import Fuzzer
from runner.Runner import Runner
from utils.Coverage import Location
from utils.Mutator import Mutator
from runner.FunctionCoverageRunner import FunctionCoverageRunner
from schedule.PowerSchedule import PowerSchedule

from utils.Seed import Seed


class GreyBoxFuzzer(Fuzzer):

    def __init__(self, seeds: List[str], schedule: PowerSchedule, is_print: bool) -> None:
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
            # Still seeding
            self.inp = self.seeds[self.seed_index]
            self.seed_index += 1
        else:
            # Mutating
            self.inp = self.create_candidate()

        return self.inp

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
            if outcome == Runner.PASS:
                # We have new coverage
                seed = Seed(self.inp, runner.coverage())
                self.population.append(seed)
        if outcome == Runner.FAIL:
            self.last_crash_time = time.time()
            self.crash_map[self.inp] = result

        return result, outcome