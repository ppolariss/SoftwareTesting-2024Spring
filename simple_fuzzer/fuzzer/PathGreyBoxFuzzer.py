import time
from typing import List, Tuple, Any, Set

from fuzzer.GreyBoxFuzzer import GreyBoxFuzzer
from schedule.PathPowerSchedule import PathPowerSchedule, get_path_id
from runner.FunctionCoverageRunner import FunctionCoverageRunner
from utils.Coverage import Location
from utils.Mutator import Mutator
from runner.Runner import Runner
from utils.Seed import Seed


class PathGreyBoxFuzzer(GreyBoxFuzzer):
    """Count how often individual paths are exercised."""

    def __init__(self, seeds: List[str], schedule: PathPowerSchedule, is_print: bool):
        super().__init__(seeds, schedule, False)
        self.start_time = time.time()
        self.last_crash_time = self.start_time
        self.population = []
        # self.file_map = {}
        self.covered_line: Set[Location] = set()
        self.seed_index = 0
        self.crash_map = dict()
        self.seeds = seeds
        self.mutator = Mutator()
        self.schedule = schedule
        self.is_print = is_print
        self.total_execs = 0
        self.total_path = 0
        self.last_path_time = self.start_time
        self.schedule.path_frequencies = {}

        # TODO
        if is_print:
            print(
                """
┌───────────────────────┬───────────────────────┬───────────────────────┬───────────────────┬───────────────────┬────────────────┬───────────────────┐
│        Run Time       │     Last New Path     │    Last Uniq Crash    │    Total Execs    │    Total Paths    │  Uniq Crashes  │   Covered Lines   │
├───────────────────────┼───────────────────────┼───────────────────────┼───────────────────┼───────────────────┼────────────────┼───────────────────┤"""
            )

    def print_stats(self):
        if not self.is_print:
            return

        def format_seconds(seconds):
            hours = int(seconds) // 3600
            minutes = int(seconds % 3600) // 60
            remaining_seconds = int(seconds) % 60
            return f"{hours:02d}:{minutes:02d}:{remaining_seconds:02d}"

        template = """│{runtime}│{path_time}│{crash_time}│{total_exec}│{total_path}│{uniq_crash}│{covered_line}│
├───────────────────────┼───────────────────────┼───────────────────────┼───────────────────┼───────────────────┼────────────────┼───────────────────┤"""
        template = template.format(
            runtime=format_seconds(time.time() - self.start_time).center(23),
            # path_time="".center(23),
            path_time=format_seconds(self.last_path_time - self.start_time).center(23),
            crash_time=format_seconds(self.last_crash_time - self.start_time).center(
                23
            ),
            total_exec=str(self.total_execs).center(19),
            # total_path="".center(19),
            # total_path=str(self.total_path).center(19),
            total_path=str(len(self.schedule.path_frequencies)).center(19),
            uniq_crash=str(len(set(self.crash_map.values()))).center(16),
            covered_line=str(len(self.covered_line)).center(19),
        )
        print(template)

    def run(self, runner: FunctionCoverageRunner) -> Tuple[Any, str]:  # type: ignore
        """Inform scheduler about path frequency"""
        result, outcome = super().run(runner)

        path_id = get_path_id(runner.coverage())
        if path_id not in self.schedule.path_frequencies:
            self.schedule.path_frequencies[path_id] = 1
            self.last_path_time = time.time()
        else:
            self.schedule.path_frequencies[path_id] += 1
        # TODO
        self.total_execs += 1

        if len(self.covered_line) != len(runner.all_coverage):
            self.covered_line |= runner.all_coverage
            if outcome == Runner.PASS:
                # We have new coverage
                seed = Seed(self.inp, runner.coverage())
                self.population.append(seed)
                print(self.population)
                # self.schedule.update_path_frequencies(seed)
                self.last_path_time = time.time()
                self.total_path += 1
        if outcome == Runner.FAIL:
            self.crash_map[self.inp] = result
            self.last_crash_time = time.time()


        self.schedule.assign_energy(self.population)

        #     self.print_stats()

        return result, outcome