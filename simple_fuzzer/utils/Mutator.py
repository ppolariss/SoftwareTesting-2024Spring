import math
import random
import struct
from typing import Any
import string


def insert_random_character(s: str, print_mutator=False) -> str:
    """
    向 s 中下标为 pos 的位置插入一个随机 byte
    pos 为随机生成，范围为 [0, len(s)]
    插入的 byte 为随机生成，范围为 [32, 127]
    """

    if len(s) == 0:
        return chr(random.randint(32, 127))

    # 随机生成 pos 和 byte
    pos = random.randint(0, len(s))
    random_char = chr(random.randint(32, 127))

    if print_mutator:
        print(f'\nInsert {random_char} at {pos}')

    return s[:pos] + random_char + s[pos:]


def flip_random_bits(s: str, print_mutator=False) -> str:
    """
    基于 AFL 变异算法策略中的 bitflip 与 random havoc 实现相邻 N 位翻转（N = 1, 2, 4），其中 N 为随机生成
    从 s 中随机挑选一个 bit，将其与其后面 N - 1 位翻转（翻转即 0 -> 1; 1 -> 0）
    注意：不要越界
    """

    if len(s) == 0:
        return s

    # 随机生成 N
    N = random.choice([1, 2, 4])
    if len(s) <= N:  #如果小于1
        return s  # 如果字符串为空，直接返回
    # 随机选择一个 bit
    random_index = random.randint(0, 8 * (len(s) - N))

    # 计算 bit 所在的字符索引和 bit 索引
    char_index = random_index // 8
    bit_index = random_index % 8

    # 获取 char_index 和 char_index + 1 处的字符
    char_1 = s[char_index]
    char_2 = s[char_index + 1] if char_index + 1 < len(s) else None
    ascii_char_1 = ord(char_1)
    ascii_char_2 = ord(char_2) if char_2 is not None else None

    # 将char_1 bit_index 及后的 N - 1 位进行翻转
    if bit_index + N < 8:
        for i in range(N):
            ascii_char_1 ^= 1 << (bit_index + i)
    else:
        for i in range(8 - bit_index):
            ascii_char_1 ^= 1 << (bit_index + i)

        if char_2 is not None:
            for i in range(N - 8 + bit_index):
                ascii_char_2 ^= 1 << i

    flipped_char_1 = chr(ascii_char_1)
    flipped_char_2 = chr(ascii_char_2) if char_2 is not None else None

    if flipped_char_2 is not None:
        result = s[:char_index] + flipped_char_1 + flipped_char_2
        if char_index + 2 < len(s):
            result += s[char_index + 2:]
    else:
        result = s[:char_index] + flipped_char_1
        if char_index + 1 < len(s):
            result += s[char_index + 1:]

    if print_mutator:
        print(f'\n{s[char_index:char_index + N]} -> {"".join(result[char_index:char_index + N])}')

    return result


def arithmetic_random_bytes(s: str, print_mutator=False) -> str:
    """
    基于 AFL 变异算法策略中的 arithmetic inc/dec 与 random havoc 实现相邻 N 字节随机增减（N = 1, 2, 4），其中 N 为随机生成
    字节随机增减：
        1. 取其中一个 byte，将其转换为数字 num1；
        2. 将 num1 加上一个 [-35, 35] 的随机数，得到 num2；
        3. 用 num2 所表示的 byte 替换该 byte
    从 s 中随机挑选一个 byte，将其与其后面 N - 1 个 bytes 进行字节随机增减
    注意：不要越界；如果出现单个字节在添加随机数之后，可以通过取模操作使该字节落在 [0, 255] 之间
    """

    if len(s) == 0:
        return s

    # 随机生成 N
    N = random.choice([1, 2, 4])
    if len(s) == 0:
        return s
    if len(s) < N:
        return s
    # 随机选择一个索引位置
    index = random.randint(0, len(s) - N)
    # 从字符串中获取 N 字节，并将其转换为数字
    bytes_to_modify = [ord(byte) for byte in s[index:index + N]]
    # 随机生成增减量
    delta = random.randint(-35, 35)
    # 对选中的字节进行增减操作
    modified_bytes = [(byte + delta) % 256 for byte in bytes_to_modify]
    # 将修改后的字节重新组合成字符串
    result = s[:index] + ''.join(chr(byte) for byte in modified_bytes) + s[index + N:]

    if print_mutator:
        print(f'\n{s[index:index + N]} -> {"".join(result[index:index + N])}')

    return result


def interesting_random_bytes(s: str, print_mutator=False) -> str:
    """
    基于 AFL 变异算法策略中的 interesting values 与 random havoc 实现相邻 N 字节随机替换为 interesting_value（N = 1, 2, 4），其中 N 为随机生成
    interesting_value 替换：
        1. 构建分别针对于 1, 2, 4 bytes 的 interesting_value 数组；
        2. 随机挑选 s 中相邻连续的 1, 2, 4 bytes，将其替换为相应 interesting_value 数组中的随机元素；
    注意：不要越界
    """
    interesting_values = {
        1: [0, 1, 127, 128, 255],  # 对于1 byte
        2: [0, 1, 256, 65535],  # 对于2 bytes
        4: [0, 1, 65536, 4294967295]  # 对于4 bytes
    }

    if len(s) == 0:
        return s

    # 随机生成 N
    N = random.choice([1, 2, 4])
    if len(s) < N:
        return s  # If the string is not long enough, return it as is

    # 随机选择一个索引位置
    index = random.randint(0, len(s) - N)

    # 随机替换相邻的 N 字节为 interesting_value 数组中的随机元素
    interesting_value = random.choice(interesting_values[N])

    result = list(s)
    for i in range(N):
        interesting_byte = interesting_value % 256
        result[index + i] = chr(interesting_byte)

        interesting_value >>= 8

    if print_mutator:
        print(f'\n{s[index:index + N]} -> {"".join(result[index:index + N])}')

    return ''.join(result)


def havoc_random_insert(s: str, print_mutator=False):
    """
    基于 AFL 变异算法策略中的 random havoc 实现随机插入
    随机选取一个位置，插入一段的内容，其中 75% 的概率是插入原文中的任意一段随机长度的内容，25% 的概率是插入一段随机长度的 bytes
    """
    index = random.randint(0, len(s))

    if len(s) == 0:
        if random.random() < 0.75:
            return s
        else:
            length = random.randint(1, 10)
            insert_content = ''.join(random.choices(string.ascii_letters + string.digits, k=length))
            return insert_content

    # 75% 的概率插入原文中的任意一段随机长度的内容
    if random.random() < 0.75:
        # 随机选择插入内容的起始位置和长度
        start = random.randint(0, len(s) - 1)
        length = random.randint(1, len(s) - start)
        insert_content = s[start:start + length]
    else:
        # 25% 的概率插入一段随机长度的字节
        length = random.randint(1, len(s))  # 长度范围为 1 到 原文长度
        insert_content = ''.join(random.choices(string.ascii_letters + string.digits, k=length))

    # 将插入内容插入到原字符串中
    result = s[:index] + insert_content + s[index:]

    if print_mutator:
        print(f'\nInsert {insert_content} at {index}')

    return result


def havoc_random_replace(s: str, print_mutator=False):
    """
    基于 AFL 变异算法策略中的 random havoc 实现随机替换
    随机选取一个位置，替换随后一段随机长度的内容，其中 75% 的概率是替换为原文中的任意一段随机长度的内容，25% 的概率是替换为一段随机长度的 bytes
    """

    if len(s) == 0:
        return s

    replace_len = random.randint(1, len(s))
    index = random.randint(0, len(s) - replace_len)

    # 75% 的概率替换为原文中的任意一段随机长度的内容
    if random.random() < 0.75:
        # 随机选择替换内容的起始位置和长度
        replace_choice_len = random.randint(1, len(s))
        start = random.randint(0, len(s) - replace_choice_len)
        replace_content = s[start:start + replace_choice_len]
    else:
        # 25% 的概率替换为一段随机长度的字节
        replace_choice_len = random.randint(1, len(s))
        replace_content = ''.join(random.choices(string.ascii_letters + string.digits, k=replace_choice_len))

    # 将替换内容替换原字符串中的一段内容
    result = s[:index] + replace_content + s[index + replace_len:]

    if print_mutator:
        print(f'\n{s[index:index + replace_len]} -> {replace_content}')

    return result


def block_swap(input_str):
    """
    基于AFL的块交换方法，每次随机交换str的两块内容，交换次数随机产生
    """
    num_swaps = random.randint(1, 5);
    input_list = list(input_str)
    if len(input_list) <= 1:
        return input_str

    for _ in range(num_swaps):
        block_size = random.randint(1, len(input_list) // 2)

        # 将原字符串分为两部分，从两部分中随机产生一个block起始的index
        start_idx1 = random.randint(0, len(input_list) // 2 - block_size)
        start_idx2 = random.randint(len(input_list) // 2, len(input_list) - block_size)

        # 提取两个块
        block1 = input_list[start_idx1:start_idx1 + block_size]
        block2 = input_list[start_idx2:start_idx2 + block_size]

        # 交换两个块
        input_list[start_idx1:start_idx1 + block_size] = block2
        input_list[start_idx2:start_idx2 + block_size] = block1

    mutated_input = ''.join(input_list)
    return mutated_input


def shuffle(input_str):
    """随机打乱输入字符串的顺序"""
    if len(input_str) <= 1:
        return input_str
    input_list = list(input_str)  # 将字符串转换为字符列表
    random.shuffle(input_list)  # 随机打乱字符列表的顺序
    return ''.join(input_list)  # 将字符列表重新组合成字符串


def insert_special_characters(s):
    """插入一些特殊字符或HTML转义字符"""
    special_chars = ['&lt;', '&gt;', '&amp;', '&quot;', '&apos;']
    insert_pos = random.randint(0, len(s))
    special_char = random.choice(special_chars)
    return s[:insert_pos] + special_char + s[insert_pos:]


def insert_nonesense(s):
    """插入一些可能无意义但可能出发错误的"""
    """插入不以'<!'或'</'开头的字符串以触发断言失败"""
    non_assert_strings = ['<a', '<b', '<c','@', '#', '$', '%', '^', '&', '*','&#','（','\123','nct127','HENG']
    insert_pos = random.randint(0, len(s))
    assert_string = random.choice(non_assert_strings)
    return s[:insert_pos] + assert_string + s[insert_pos:]


def insert_unclosed_tag(s):
    """添加一些未闭合的HTML标签"""
    tags = ['<div', '<span', '<p', '<a href="','</','<!--','<!', '<?','<![']
    insert_pos = random.randint(0, len(s))
    unclosed_tag = random.choice(tags)
    return s[:insert_pos] + unclosed_tag + s[insert_pos:]


def insert_confused_tag(s):
    """插入一些不完整或混淆的HTML标签"""
    confused_tags = ['<div/>', '<span>', '<p></', '</p><', '<a href="example.com"></a><','<div attr="value', '<span attr=\'value', '<p attr=value', '<a attr="value']
    insert_pos = random.randint(0, len(s))

    confused_tag = random.choice(confused_tags)
    return s[:insert_pos] + confused_tag + s[insert_pos:]


def delete_random_character(s: str) -> str:
    if s == "":
        return s
    index1 = random.randint(0, len(s))
    index2 = random.randint(0, len(s))
    return s[:min(index1, index2)] + s[max(index1, index2):]


class Mutator:

    def __init__(self) -> None:
        """Constructor"""
        self.mutators = [
            insert_random_character,
            flip_random_bits,
            arithmetic_random_bytes,
            interesting_random_bytes,
            havoc_random_insert,
            havoc_random_replace,
            block_swap,
            shuffle,
            insert_special_characters,
            insert_unclosed_tag,
            insert_confused_tag,
            insert_nonesense,
            delete_random_character
        ]

    def mutate(self, inp: Any) -> Any:
        mutator = random.choice(self.mutators)
        return mutator(inp)


# for test
def test_base_mutator():
    origin = "abcdefgh..."
    # 可增加测试次数以验证健壮性
    test_num = 100

    for i in range(len(origin)):
        origin_slice = origin[0:i]
        for j in range(test_num):
            print("\n====================================================")
            print("* Origin:                    ", origin_slice)
            print("* Insert Random Character:   ", insert_random_character(origin_slice, True))
            print("* Flip Random Bits:          ", flip_random_bits(origin_slice, True))
            print("* Arithmetic Random Bytes:   ", arithmetic_random_bytes(origin_slice, True))
            print("* Interesting Random Bytes:  ", interesting_random_bytes(origin_slice, True))
            print("* Havoc Random Insert:       ", havoc_random_insert(origin_slice, True))
            print("* Havoc Random Replace:      ", havoc_random_replace(origin_slice, True))
            print("====================================================")


if __name__ == '__main__':
    test_base_mutator()
