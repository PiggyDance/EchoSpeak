#!/usr/bin/env python3
"""Verify every 64-bit ELF PT_LOAD segment in APK/AAB/AAR files, plus APK ZIP alignment.

Uses only Python's standard library. AAB ZIP entries are not APK installation offsets;
check generated APKs as well before release. No binary headers are modified.
"""

import argparse
import json
from pathlib import Path
import struct
import sys
import zipfile

PAGE_SIZE = 16384


def check_elf(data):
    if len(data) < 64 or data[:4] != b"\x7fELF":
        raise ValueError("Not an ELF file")
    if data[4] != 2:
        return None  # Android's 16 KB requirement applies to 64-bit native libraries.
    if data[5] not in (1, 2):
        raise ValueError("Unsupported ELF byte order")
    endian = "<" if data[5] == 1 else ">"
    table_offset = struct.unpack_from(endian + "Q", data, 32)[0]
    entry_size, count = struct.unpack_from(endian + "HH", data, 54)
    if entry_size < 56 or not count or table_offset + entry_size * count > len(data):
        raise ValueError("Invalid ELF program header table")
    segments = []
    for i in range(count):
        header = struct.unpack_from(endian + "IIQQQQQQ", data, table_offset + i * entry_size)
        kind, _, offset, address, _, file_size, _, alignment = header
        if kind != 1:  # PT_LOAD
            continue
        errors = []
        if alignment < PAGE_SIZE or alignment & (alignment - 1):
            errors.append("PT_LOAD alignment must be a power of two >= 16384")
        if offset % PAGE_SIZE != address % PAGE_SIZE:
            errors.append("PT_LOAD file offset and virtual address are not congruent modulo 16384")
        if offset + file_size > len(data):
            errors.append("PT_LOAD extends beyond the ELF file")
        segments.append({"alignment": alignment, "offset": offset, "address": address, "errors": errors})
    if not segments:
        raise ValueError("ELF has no PT_LOAD segments")
    return segments


def check_archive(path):
    results = []
    with zipfile.ZipFile(path) as archive, path.open("rb") as raw:
        for entry in archive.infolist():
            if not entry.filename.endswith(".so"):
                continue
            try:
                segments = check_elf(archive.read(entry))
                if segments is None:
                    continue
                errors = [error for segment in segments for error in segment["errors"]]
                zip_offset = None
                if path.suffix.lower() == ".apk" and entry.compress_type == zipfile.ZIP_STORED:
                    raw.seek(entry.header_offset)
                    header = raw.read(30)
                    if header[:4] != b"PK\x03\x04":
                        errors.append("Invalid ZIP local header")
                    else:
                        name_size, extra_size = struct.unpack_from("<HH", header, 26)
                        zip_offset = entry.header_offset + 30 + name_size + extra_size
                        if zip_offset % PAGE_SIZE:
                            errors.append("Uncompressed APK library is not ZIP aligned to 16384")
                results.append({"library": entry.filename, "segments": segments,
                                "apk_data_offset": zip_offset, "errors": errors})
            except (ValueError, struct.error) as error:
                results.append({"library": entry.filename, "errors": [str(error)]})
    return results


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("artifacts", nargs="+", type=Path)
    parser.add_argument("--json", type=Path, help="Save a machine-readable report")
    args = parser.parse_args()
    report = []
    failed = False
    for path in args.artifacts:
        try:
            results = check_archive(path)
            errors = [] if results else ["No 64-bit native libraries found"]
        except (OSError, ValueError, zipfile.BadZipFile) as error:
            results, errors = [], [str(error)]
        report.append({"artifact": str(path.resolve()), "libraries": results, "errors": errors})
        print(path)
        for result in results:
            status = "FAIL" if result["errors"] else "PASS"
            alignments = ", ".join(hex(s["alignment"]) for s in result.get("segments", []))
            print(f"  {status} {result['library']} PT_LOAD=[{alignments}]")
            for error in result["errors"]:
                print("    " + error)
        for error in errors:
            print("  FAIL " + error)
        failed |= bool(errors) or any(result["errors"] for result in results)
    if args.json:
        args.json.parent.mkdir(parents=True, exist_ok=True)
        args.json.write_text(json.dumps(report, indent=2) + "\n")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
