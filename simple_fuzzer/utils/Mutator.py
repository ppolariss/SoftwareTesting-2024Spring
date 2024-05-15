import math
import random
import struct
from typing import Any
import string


def insert_random_character(s: str) -> str:
    """
    向 s 中下标为 pos 的位置插入一个随机 byte
    pos 为随机生成，范围为 [0, len(s)]
    插入的 byte 为随机生成，范围为 [32, 127]
    """
    pos = random.randint(0, len(s))
    random_char = chr(random.randint(32, 127))
    return s[:pos] + random_char + s[pos:]

def flip_random_bits(s: str) -> str:
    """
    基于 AFL 变异算法策略中的 bitflip 与 random havoc 实现相邻 N 位翻转（N = 1, 2, 4），其中 N 为随机生成
    从 s 中随机挑选一个 bit，将其与其后面 N - 1 位翻转（翻转即 0 -> 1; 1 -> 0）
    注意：不要越界
    """
    N = random.choice([1, 2, 4])
    # 随机选择一个索引位置
    index = random.randint(0, len(s) - N)
    # 对索引位置及其后面的 N - 1 位进行翻转
    result = list(s)
    for i in range(N):
        result[index + i] = '1' if result[index + i] == '0' else '0'
    return ''.join(result)


def arithmetic_random_bytes(s: str) -> str:
    """
    基于 AFL 变异算法策略中的 arithmetic inc/dec 与 random havoc 实现相邻 N 字节随机增减（N = 1, 2, 4），其中 N 为随机生成
    字节随机增减：
        1. 取其中一个 byte，将其转换为数字 num1；
        2. 将 num1 加上一个 [-35, 35] 的随机数，得到 num2；
        3. 用 num2 所表示的 byte 替换该 byte
    从 s 中随机挑选一个 byte，将其与其后面 N - 1 个 bytes 进行字节随机增减
    注意：不要越界；如果出现单个字节在添加随机数之后，可以通过取模操作使该字节落在 [0, 255] 之间
    """
    # TODO
    # 随机生成 N
    N = random.choice([1, 2, 4])
    # 随机选择一个索引位置
    index = random.randint(0, len(s) - N)
    # 从字符串中获取 N 字节，并将其转换为数字
    bytes_to_modify = [ord(byte) for byte in s[index:index+N]]
    # 随机生成增减量
    delta = random.randint(-35, 35)
    # 对选中的字节进行增减操作
    modified_bytes = [(byte + delta) % 256 for byte in bytes_to_modify]
    # 将修改后的字节重新组合成字符串
    result = s[:index] + ''.join(chr(byte) for byte in modified_bytes) + s[index+N:]
    return result


def interesting_random_bytes(s: str) -> str:
    """
    基于 AFL 变异算法策略中的 interesting values 与 random havoc 实现相邻 N 字节随机替换为 interesting_value（N = 1, 2, 4），其中 N 为随机生成
    interesting_value 替换：
        1. 构建分别针对于 1, 2, 4 bytes 的 interesting_value 数组；
        2. 随机挑选 s 中相邻连续的 1, 2, 4 bytes，将其替换为相应 interesting_value 数组中的随机元素；
    注意：不要越界
    """
    # TODO
    interesting_values = {
        1: [0, 1, 127, 128, 255],  # 对于1 byte
        2: [0, 1, 256, 65535],      # 对于2 bytes
        4: [0, 1, 65536, 4294967295]# 对于4 bytes
    }
    
    # 随机生成 N
    N = random.choice([1, 2, 4])
    
    # 随机选择一个索引位置
    index = random.randint(0, len(s) - N)
    
    # 随机替换相邻的 N 字节为 interesting_value 数组中的随机元素
    result = list(s)
    for i in range(N):
        interesting_value = random.choice(interesting_values[N])
        result[index + i] = chr(interesting_value)
    return ''.join(result)


def havoc_random_insert(s: str):
    """
    基于 AFL 变异算法策略中的 random havoc 实现随机插入
    随机选取一个位置，插入一段的内容，其中 75% 的概率是插入原文中的任意一段随机长度的内容，25% 的概率是插入一段随机长度的 bytes
    """
    # TODO
    index = random.randint(0, len(s))
    
    # 75% 的概率插入原文中的任意一段随机长度的内容
    if random.random() < 0.75:
        # 随机选择插入内容的起始位置和长度
        start = random.randint(0, len(s))
        length = random.randint(1, len(s) - start)
        insert_content = s[start:start+length]
    else:
        # 25% 的概率插入一段随机长度的字节
        length = random.randint(1, 10)  # 假设长度范围为 1 到 10
        insert_content = ''.join(random.choices(string.ascii_letters + string.digits, k=length))
    
    # 将插入内容插入到原字符串中
    result = s[:index] + insert_content + s[index:]
    return result



def havoc_random_replace(s: str):
    """
    基于 AFL 变异算法策略中的 random havoc 实现随机替换
    随机选取一个位置，替换随后一段随机长度的内容，其中 75% 的概率是替换为原文中的任意一段随机长度的内容，25% 的概率是替换为一段随机长度的 bytes
    """
    # TODO
    index = random.randint(0, len(s))
    
    # 75% 的概率替换为原文中的任意一段随机长度的内容
    if random.random() < 0.75:
        # 随机选择替换内容的起始位置和长度
        start = random.randint(0, len(s))
        length = random.randint(1, len(s) - start)
        replace_content = s[start:start+length]
    else:
        # 25% 的概率替换为一段随机长度的字节
        length = random.randint(1, 10)  # 假设长度范围为 1 到 10
        replace_content = ''.join(random.choices(string.ascii_letters + string.digits, k=length))
    
    # 将替换内容替换原字符串中的一段内容
    result = s[:index] + replace_content + s[index+len(replace_content):]
    return result


class Mutator:

    def __init__(self) -> None:
        """Constructor"""
        self.mutators = [
            insert_random_character,
            flip_random_bits,
            arithmetic_random_bytes,
            interesting_random_bytes,
            havoc_random_insert,
            havoc_random_replace
        ]

    def mutate(self, inp: Any) -> Any:
        mutator = random.choice(self.mutators)
        return mutator(inp)
