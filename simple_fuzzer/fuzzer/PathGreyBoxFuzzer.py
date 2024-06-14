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

    def __init__(self, seeds: List[str], schedule: PathPowerSchedule, is_print: bool,from_disk: bool):
        super().__init__(seeds, schedule,is_print,from_disk)
        self.is_print = is_print
        self.total_path = 0
        self.last_path_time = self.start_time
        self.total_crash = 0
        self.from_disk = from_disk

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
        # print(self.total_crash)
        # print(len({k: v for k, v in self.schedule.path_frequencies.items() if v > 2}))

    def run(self, runner: FunctionCoverageRunner) -> Tuple[Any, str]:  # type: ignore
        """Inform scheduler about path frequency"""
        result, outcome = super().run(runner)

        path_id = get_path_id(runner.coverage())
        if self.schedule.update_path_frequencies(path_id):
            self.total_path += 1
            self.last_path_time = time.time()
            if outcome == Runner.FAIL:
                self.total_crash += 1
                # if self.total_crash % 5 == 0:
                #     seed = Seed(self.inp, runner.coverage())
                #     self.population.append(seed)
        assert self.total_path == len(self.schedule.path_frequencies)
        # assert self.total_path == runner.cumulative_coverage[-1]
        # print(len(self.schedule.path_frequencies))
        # TODO
        # self.schedule.assign_energy(self.population)

        #     self.print_stats()

        return result, outcome