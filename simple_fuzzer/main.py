import os
import time

from fuzzer.GreyBoxFuzzer import GreyBoxFuzzer
from fuzzer.PathGreyBoxFuzzer import PathGreyBoxFuzzer
from fuzzer.CrashGreyBoxFuzzer import CrashGreyBoxFuzzer

from runner.FunctionCoverageRunner import FunctionCoverageRunner

from schedule.PowerSchedule import PowerSchedule
from schedule.PathPowerSchedule import PathPowerSchedule
from schedule.LevelPowerSchedule import LevelPowerSchedule
from schedule.CrashPowerSchedule import CrashPowerSchedule
from schedule.CoveragePowerSchedule import CoveragePowerSchedule
from schedule.AgePowerSchedule import AgePowerSchedule

from samples.Samples import sample1, sample2, sample3, sample4
from utils.ObjectUtils import dump_object, load_object


class Result:
    def __init__(self, coverage, crashes, start_time, end_time):
        self.covered_line = coverage
        self.crashes = crashes
        self.start_time = start_time
        self.end_time = end_time

    def __str__(self):
        return "Covered Lines: " + str(self.covered_line) + ", Crashes Num: " + str(self.crashes) + ", Start Time: " + str(self.start_time) + ", End Time: " + str(self.end_time)


if __name__ == "__main__":
    f_runner = FunctionCoverageRunner(sample4)
    seeds = load_object("corpus/corpus_4")

    # grey_fuzzer = GreyBoxFuzzer(seeds=seeds, schedule=PowerSchedule(), is_print=True)
    grey_fuzzer = PathGreyBoxFuzzer(seeds=seeds, schedule=PathPowerSchedule(5), is_print=True)
    # grey_fuzzer = CrashGreyBoxFuzzer(seeds=seeds, schedule=CrashPowerSchedule(), is_print=True)
    # grey_fuzzer = GreyBoxFuzzer(seeds=seeds, schedule=CoveragePowerSchedule(), is_print=True)
    # grey_fuzzer = GreyBoxFuzzer(seeds=seeds, schedule=AgePowerSchedule(), is_print=True)
    
    start_time = time.time()
    grey_fuzzer.runs(f_runner, run_time=7200)
    grey_fuzzer.save_seed_input()
    res = Result(grey_fuzzer.covered_line, set(grey_fuzzer.crash_map.values()), start_time, time.time())
    with open('result.txt', 'w') as f:
        # 将数据转换成字符串，并逐行写入文件
        for item in set(grey_fuzzer.crash_map.values()):
            f.write(str(item) + '\n')
            f.write("------------------------------------------------")
    #dump_object("_result" + os.sep + "Sample-1.pkl", res)
    #print(load_object("_result" + os.sep + "Sample-1.pkl"))