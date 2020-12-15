
from PIL import Image
from argparse import ArgumentParser
from typing import List

import os
import re
import math


def main():
    parser = ArgumentParser()
    parser.add_argument('--pattern', type=str, default='.*', help='A regex to match against file names')
    parser.add_argument('--recursive', default=False, action='store_true', dest='recursive', help='Should search subfolders recursively for images')
    parser.add_argument('--dir', type=str, default='', help='Search directory, relative to root')
    parser.add_argument('--root', type=str, default='../src/main/resources/assets/tfc/textures/', help='Root directory to search for (Default, when ran in ./resources/, looks under /tfc/textures/')
    parser.add_argument('--scale', type=int, default=10, help='Amount to scale each image up by (nearest neighbor)')
    parser.add_argument('--shape', choices=('grid', 'x', 'y'), default='x', help='Output stitching shape. grid = square, x = horizontal, y = vertical')
    parser.add_argument('--result', type=str, default='./stitched.png', help='Output file name')

    args = parser.parse_args()
    print('Running with', args)

    files = []
    find_files(files, os.path.join(args.root, args.dir), args.pattern, args.recursive)
    if len(files) == 0:
        print('No files found, exiting')
        return

    files = sorted(files)
    side = 1 + math.isqrt(len(files))
    if args.shape == 'x':
        width, height = len(files), 1
    elif args.shape == 'y':
        width, height = 1, len(files)
    else:
        width, height = side, side
    print('Found %d files' % len(files))

    stitched = Image.new('RGBA', (width * args.scale * 16, height * args.scale * 16), (150, 150, 150, 255))

    for i, f in enumerate(files):
        if args.shape == 'x':
            x, y = i, 0
        elif args.shape == 'y':
            x, y = 0, i
        else:
            x, y = i % side, i // side

        try:
            part = Image.open(f).convert('RGBA')
            part = part.resize((args.scale * part.width, args.scale * part.height), Image.NEAREST)
            stitched.paste(part, (x * args.scale * 16, y * args.scale * 16))
        except Exception as e:
            print('Failed on %s: %s' % (str(f), str(e)))

    stitched.save(args.result)


def find_files(files: List[str], current_dir: str, pattern: str, recursive: bool):
    for f in os.listdir(current_dir):
        path = os.path.join(current_dir, f)
        if os.path.isfile(path):
            if re.search(pattern, f) and f.endswith('.png'):
                files.append(path)
        elif recursive and os.path.isdir(path):
            find_files(files, os.path.join(current_dir, f), pattern, recursive)


if __name__ == '__main__':
    main()
