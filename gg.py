#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import re

CHINESE_PATTERN = re.compile(r'[\u4e00-\u9fff\u3000-\u303f\uff00-\uffef]')

TEXT_EXTENSIONS = {
    '.py', '.pyw', '.js', '.ts', '.jsx', '.tsx', '.java', '.c', '.cpp',
    '.h', '.hpp', '.cs', '.go', '.rs', '.swift', '.kt', '.scala',
    '.html', '.htm', '.css', '.scss', '.less', '.json', '.xml', '.yaml',
    '.yml', '.toml', '.ini', '.cfg', '.conf', '.sh', '.bash', '.zsh',
    '.txt', '.md', '.rst', '.tex'
}

def has_chinese(file_path):
    """检测文件是否包含中文字符，成功读取则返回True/False，否则返回False"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            for line in f:
                if CHINESE_PATTERN.search(line):
                    return True
    except UnicodeDecodeError:
        try:
            with open(file_path, 'r', encoding='gbk', errors='ignore') as f:
                for line in f:
                    if CHINESE_PATTERN.search(line):
                        return True
        except Exception:
            pass
    except Exception:
        pass
    return False

def scan_src():
    src_dir = os.path.join(os.getcwd(), 'src')
    if not os.path.isdir(src_dir):
        print(f"错误：目录 '{src_dir}' 不存在")
        return

    found_files = []
    for root, _, files in os.walk(src_dir):
        for file in files:
            ext = os.path.splitext(file)[1].lower()
            if ext in TEXT_EXTENSIONS:
                full_path = os.path.join(root, file)
                if has_chinese(full_path):
                    found_files.append(full_path)

    if found_files:
        print("发现含有中文的文件：")
        for f in found_files:
            print(f"  {f}")
    else:
        print("所有文本文件中均未检测到中文字符。")

if __name__ == "__main__":
    scan_src()