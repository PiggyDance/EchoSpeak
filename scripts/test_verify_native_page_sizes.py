import struct
import unittest

from verify_native_page_sizes import check_elf


def elf(alignments, offsets=None, addresses=None):
    data = bytearray(64 + 56 * len(alignments))
    data[:6] = b"\x7fELF\x02\x01"
    struct.pack_into("<Q", data, 32, 64)
    struct.pack_into("<HH", data, 54, 56, len(alignments))
    for i, alignment in enumerate(alignments):
        offset = offsets[i] if offsets else 0
        address = addresses[i] if addresses else 0
        struct.pack_into("<IIQQQQQQ", data, 64 + i * 56, 1, 5, offset, address, 0, 0, 0, alignment)
    return data


class NativePageSizeTest(unittest.TestCase):
    def test_checks_every_load_segment_not_only_the_first(self):
        result = check_elf(elf([0x4000, 0x1000]))
        self.assertFalse(result[0]["errors"])
        self.assertTrue(result[1]["errors"])

    def test_alignment_header_alone_does_not_make_offsets_compatible(self):
        result = check_elf(elf([0x4000], offsets=[0], addresses=[0x1000]))
        self.assertTrue(result[0]["errors"])

    def test_accepts_real_linker_alignment_and_rejects_non_power_of_two(self):
        self.assertFalse(check_elf(elf([0x4000, 0x10000]))[0]["errors"])
        self.assertTrue(check_elf(elf([0x5000]))[0]["errors"])

    def test_rejects_truncated_tables_instead_of_false_success(self):
        with self.assertRaises(ValueError):
            check_elf(elf([0x4000])[:-1])


if __name__ == "__main__":
    unittest.main()
