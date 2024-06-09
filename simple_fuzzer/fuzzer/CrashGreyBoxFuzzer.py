from fuzzer.GreyBoxFuzzer import GreyBoxFuzzer
from runner.Runner import Runner
from runner.FunctionCoverageRunner import FunctionCoverageRunner
from schedule.CrashPowerSchedule import CrashPowerSchedule
from utils.Seed import Seed
import time
from typing import List, Tuple, Any, Set

class CrashGreyBoxFuzzer(GreyBoxFuzzer):

    def __init__(self, seeds: List[str], schedule: CrashPowerSchedule, is_print: bool) -> None:
        super().__init__(seeds, schedule, is_print)

    def run(self, runner: FunctionCoverageRunner) -> Tuple[Any, str]:  # type: ignore
        """Run function(inp) while tracking coverage.
           If we reach new coverage,
           add inp to population and its coverage to population_coverage
        """
        result, outcome = super().run(runner)
        
        # print(f"result = {result}, outcome = {outcome}")
        
        # if len(self.covered_line) != len(runner.all_coverage):
        #     self.covered_line |= runner.all_coverage
        #     if outcome == Runner.PASS:
        #         # We have new coverage
        #         seed = Seed(self.inp, runner.coverage())
        #         self.population.append(seed)
        if outcome == Runner.FAIL:
            # uniq_crash_num = len(set(self.crash_map.values()))
            # self.crash_map[self.inp] = result
            # if len(set(self.crash_map.values())) != uniq_crash_num:
            #     self.last_crash_time = time.time()
            # NOTE: Increment crash count in the schedule
            # print(f"self.inp = {self.inp}")
            self.schedule.increment_crash_count(Seed(self.inp, runner.coverage()))

        return result, outcome
