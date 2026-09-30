"""Validate the actual bundled model/labels without a platform-specific inference runtime."""

import hashlib
import io
from pathlib import Path
import struct
import unittest
import zipfile

ASSETS = Path(__file__).resolve().parents[1] / "composeApp/src/androidMain/assets"


class ModelTables:
    """Read the few FlatBuffer fields needed for the TFLite model tensor contract."""

    def __init__(self, data):
        self.data = data

    def field(self, table, index):
        vtable = table - struct.unpack_from("<i", self.data, table)[0]
        size = struct.unpack_from("<H", self.data, vtable)[0]
        if 4 + index * 2 >= size:
            return None
        offset = struct.unpack_from("<H", self.data, vtable + 4 + index * 2)[0]
        return table + offset if offset else None

    def pointer(self, offset):
        return offset + struct.unpack_from("<I", self.data, offset)[0]

    def vector(self, table, index):
        offset = self.pointer(self.field(table, index))
        return offset + 4, struct.unpack_from("<I", self.data, offset)[0]

    def tables(self, table, index):
        offset, count = self.vector(table, index)
        return [self.pointer(offset + i * 4) for i in range(count)]

    def ints(self, table, index):
        offset, count = self.vector(table, index)
        return list(struct.unpack_from("<" + "i" * count, self.data, offset))


class YamnetModelAssetTest(unittest.TestCase):
    def setUp(self):
        self.model = (ASSETS / "yamnet.tflite").read_bytes()

    def test_model_is_identical_to_upstream_and_original_release(self):
        self.assertEqual(hashlib.sha256(self.model).hexdigest(),
                         "10c95ea3eb9a7bb4cb8bddf6feb023250381008177ac162ce169694d05c317de")

    def test_labels_match_the_model_metadata_and_speech_class(self):
        labels = (ASSETS / "yamnet_labels.txt").read_bytes()
        with zipfile.ZipFile(io.BytesIO(self.model)) as archive:
            self.assertEqual(labels, archive.read("yamnet_label_list.txt"))
        lines = labels.decode().splitlines()
        self.assertEqual(len(lines), 521)
        self.assertEqual(lines[0], "Speech")

    def test_model_has_the_required_float_input_and_classification_output(self):
        self.assertEqual(self.model[4:8], b"TFL3")
        tables = ModelTables(self.model)
        root = struct.unpack_from("<I", self.model, 0)[0]
        graphs = tables.tables(root, 2)
        self.assertEqual(len(graphs), 1)
        tensors = tables.tables(graphs[0], 0)
        inputs = tables.ints(graphs[0], 1)
        outputs = tables.ints(graphs[0], 2)
        self.assertEqual(len(inputs), 1)
        self.assertEqual(len(outputs), 1)
        for index, shape in [(inputs[0], [15600]), (outputs[0], [1, 521])]:
            tensor = tensors[index]
            self.assertEqual(tables.ints(tensor, 0), shape)
            dtype = tables.field(tensor, 1)
            self.assertEqual(self.model[dtype] if dtype else 0, 0)  # FLOAT32


if __name__ == "__main__":
    unittest.main()
